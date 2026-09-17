package com.techcity.techcityassist

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.tasks.await

/**
 * Central place for everything auth: sign-in/out, the username -> email
 * convention, and the session guard that detects an account deleted or
 * disabled in the Firebase Console and returns the user to the login screen.
 *
 * Access model: the owner pre-creates accounts in the Firebase Console
 * (Authentication -> Users) as <username>@techcity.app. Deleting or disabling
 * an account there revokes access; the app itself has no user management.
 */
object Authmanager {

    private const val TAG = "Authmanager"

    /** Accounts must be created in the console with exactly this domain. */
    private const val USERNAME_DOMAIN = "techcity.app"

    /** How often the session guard re-validates the account while foregrounded. */
    private const val SESSION_CHECK_INTERVAL_MS = 60_000L

    /** Intent extra telling LoginActivity to show the "access removed" message. */
    const val EXTRA_ACCESS_REMOVED = "ACCESS_REMOVED"

    private val auth: FirebaseAuth
        get() = FirebaseAuth.getInstance()

    private val handler = Handler(Looper.getMainLooper())
    private var appContext: Context? = null
    private var currentActivity: Activity? = null
    private var startedActivities = 0
    private var guardStarted = false

    /** Set while routing to LoginActivity so concurrent failures don't re-redirect. */
    @Volatile
    private var redirecting = false

    /**
     * Purely local check — Firebase Auth persists the session on-device, so
     * returning users skip the login screen with no network round-trip.
     */
    fun isSignedIn(): Boolean = auth.currentUser != null

    fun usernameToEmail(input: String): String {
        val trimmed = input.trim()
        return if (trimmed.contains("@")) trimmed else "$trimmed@$USERNAME_DOMAIN"
    }

    /**
     * Sign in with console-created credentials.
     * Returns null on success, otherwise a user-facing error message.
     */
    suspend fun signIn(username: String, password: String): String? {
        return try {
            auth.signInWithEmailAndPassword(usernameToEmail(username), password).await()
            null
        } catch (e: FirebaseAuthInvalidUserException) {
            if (e.errorCode == "ERROR_USER_DISABLED") {
                "This account has been disabled. Contact the administrator."
            } else {
                "Wrong username or password"
            }
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            "Wrong username or password"
        } catch (e: FirebaseNetworkException) {
            "No connection. Check your network and try again."
        } catch (e: Exception) {
            Log.e(TAG, "Sign-in failed", e)
            "Sign-in failed. Please try again."
        }
    }

    /**
     * Sign out and clear locally cached inventory so a revoked device
     * doesn't keep readable data.
     */
    fun signOut(context: Context) {
        PhoneListHolder.clearCache()
        SyncDataManager.clearLocalData(context)
        auth.signOut()
    }

    /** Sign out and return to the login screen, clearing the back stack. */
    fun forceSignOut(context: Context, accessRemoved: Boolean) {
        // Route first so the AuthStateListener fired by signOut() no-ops.
        routeToLogin(context, accessRemoved)
        signOut(context)
    }

    /**
     * Called from Firestore error branches. PERMISSION_DENIED means the
     * account's token is no longer accepted by the security rules (revoked
     * and past the ID-token lifetime) — treat it as revocation.
     * Returns true when the error was handled with sign-out + redirect.
     */
    fun handleFirestoreError(context: Context, error: Throwable?): Boolean {
        val code = (error as? FirebaseFirestoreException)?.code
        if (code != FirebaseFirestoreException.Code.PERMISSION_DENIED) return false
        Log.w(TAG, "PERMISSION_DENIED from Firestore — treating as revoked access")
        forceSignOut(context, accessRemoved = true)
        return true
    }

    /**
     * Wire the session guard into every activity via lifecycle callbacks.
     * Called once from TechCityApplication.onCreate.
     *
     * reload() fails immediately with FirebaseAuthInvalidUserException once
     * the account is deleted or disabled in the console (the refresh token
     * is revoked even though the current ID token stays valid up to an hour)
     * — that is what makes revocation near-real-time.
     */
    fun startSessionGuard(app: Application) {
        if (guardStarted) return
        guardStarted = true
        appContext = app.applicationContext

        // Belt-and-suspenders: any path that ends the session while a data
        // screen is visible routes back to login.
        auth.addAuthStateListener { firebaseAuth ->
            if (firebaseAuth.currentUser == null) {
                val activity = currentActivity
                if (activity != null && activity !is LoginActivity) {
                    routeToLogin(activity, accessRemoved = false)
                }
            }
        }

        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                startedActivities++
                if (startedActivities == 1) {
                    handler.removeCallbacks(sessionTick)
                    handler.postDelayed(sessionTick, SESSION_CHECK_INTERVAL_MS)
                }
            }

            override fun onActivityStopped(activity: Activity) {
                startedActivities--
                if (startedActivities <= 0) handler.removeCallbacks(sessionTick)
            }

            override fun onActivityResumed(activity: Activity) {
                currentActivity = activity
                if (activity is LoginActivity) {
                    redirecting = false
                } else {
                    checkSession()
                }
            }

            override fun onActivityPaused(activity: Activity) {
                if (currentActivity === activity) currentActivity = null
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    private val sessionTick = object : Runnable {
        override fun run() {
            checkSession()
            handler.postDelayed(this, SESSION_CHECK_INTERVAL_MS)
        }
    }

    private fun checkSession() {
        if (redirecting) return
        if (currentActivity is LoginActivity) return
        val user = auth.currentUser ?: return
        user.reload().addOnFailureListener { e ->
            if (e is FirebaseAuthInvalidUserException) {
                Log.w(TAG, "Account deleted or disabled in console — signing out", e)
                val context = currentActivity ?: appContext ?: return@addOnFailureListener
                forceSignOut(context, accessRemoved = true)
            }
            // Any other failure (typically no network) keeps the session;
            // the next tick retries.
        }
    }

    private fun routeToLogin(context: Context, accessRemoved: Boolean) {
        if (redirecting) return
        redirecting = true
        val intent = Intent(context, LoginActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            if (accessRemoved) putExtra(EXTRA_ACCESS_REMOVED, true)
        }
        context.startActivity(intent)
    }
}
