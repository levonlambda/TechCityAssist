package com.techcity.techcityassist

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.view.WindowManager

class TechCityApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Revocation detector: re-validates the signed-in account on every
        // activity resume and on a periodic tick while foregrounded.
        Authmanager.startSessionGuard(this)
        // Restore the persisted store location before any screen reads it.
        LocationManager.init(this)
        // Store display tablets must never dim or time out while the app is in
        // front. Every activity window gets FLAG_KEEP_SCREEN_ON as it is created;
        // the system honors the flag only while the window is visible, so it is
        // released automatically on Home, app switch or finish and needs no
        // cleanup. See _specs/keep-screen-awake-on-display-screens.md.
        registerActivityLifecycleCallbacks(KeepScreenOnCallbacks)
    }
}

/**
 * Adds [WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON] to every activity
 * window as it is created, so any current or future screen keeps the display
 * on without per-activity code. The flag survives activity recreation because
 * onActivityCreated runs again for the new instance.
 */
private object KeepScreenOnCallbacks : Application.ActivityLifecycleCallbacks {
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
