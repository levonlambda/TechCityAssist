# Plan: Firebase User Access Management

- **Spec:** `_specs/firebase-user-access-management.md`
- **Branch:** `feature/firebase-user-access-management`
- **Status:** Implemented (pending Firebase Console setup — Step 0 and Step 5 — and manual verification, Step 7)
- **Created:** 2026-08-12

## Approach Summary

Access is managed entirely through **Firebase Authentication accounts that the owner pre-creates in the Firebase Console** (Authentication → Users, Email/Password provider). Granting access = creating a username/password account in the console; revoking access = deleting (or disabling) that account. The app itself needs no admin screen, no `users` collection, and no pending/approved states — it only needs to (1) require sign-in before any data loads, (2) enforce `request.auth != null` in Firestore **and Storage** security rules, and (3) actively detect that a signed-in account has been deleted/disabled and kick the user back to the login screen.

**Deviations from the spec (per owner's direction):** spec requirements 3, 5, 6, 7, and 11 (in-app admin screen, pending/denied states, grant/revoke UI) are replaced by console-managed accounts. The "pending approval" screen is dropped entirely — anyone who can sign in is, by definition, granted.

Key technical fact driving the design: deleting a Firebase Auth user **does not invalidate their current ID token** — it stays cryptographically valid for up to 1 hour, and Firestore rules will keep accepting it until it expires. Deletion *does* immediately revoke the refresh token, so `FirebaseUser.reload()` (or a forced token refresh) fails right away with `FirebaseAuthInvalidUserException`. Near-real-time revocation therefore comes from the **app-side**: a lightweight session guard that calls `reload()` on app foreground and on a short periodic timer, signing the user out the moment the account no longer exists.

Username handling: Firebase's Email/Password provider requires an email-formatted identifier. Convention: the owner creates accounts in the console as `<username>@techcity.app` (e.g. `staff1@techcity.app`). The login screen shows a **Username** field and appends the domain automatically (if the input already contains `@`, it is used as-is). The domain does not need to exist as a real mailbox.

## Implementation Steps

### Step 0 — Firebase Console prep (owner, no code)

1. In Firebase Console → Authentication → Sign-in method: enable **Email/Password**. Leave "Email link" off.
2. Create the initial accounts (owner's own + any staff) as `<username>@techcity.app` with chosen passwords.
3. Note for operations: **Disable** a user (Console → Users → ⋮ → Disable account) for temporary revocation (re-enable restores access with the same password); **Delete** for permanent removal. Both are detected identically by the app's session guard.

### Step 1 — Add Firebase Auth dependency

`app/build.gradle.kts` already uses the Firebase BoM (line 68) with `firebase-firestore` and `firebase-storage`. Add `implementation("com.google.firebase:firebase-auth")` alongside them (BoM manages the version). No other build changes.

### Step 2 — `Authmanager.kt` (new, matching existing file-naming style)

A small object encapsulating all auth concerns so activities stay thin:

- `isSignedIn(): Boolean` — wraps `FirebaseAuth.getInstance().currentUser != null`.
- `usernameToEmail(input: String): String` — appends `@techcity.app` unless input contains `@`.
- `signIn(username, password)` — suspend wrapper around `signInWithEmailAndPassword`, mapping Firebase exceptions to user-friendly messages ("Wrong username or password", "Account disabled", "No connection").
- `signOut(context)` — signs out **and clears local data**: `PhoneListHolder` reset, `SyncDataManager` synced-JSON files deleted, so a revoked device doesn't keep readable cached inventory (spec edge case). Image cache files may stay (they're just pictures, low sensitivity — decision noted below in Risks).
- `startSessionGuard(activity)` / lifecycle hook — the revocation detector:
  - On every activity `onResume` and on a repeating ~60-second tick while the app is foregrounded, call `currentUser.reload()` (cheap network call).
  - If it fails with `FirebaseAuthInvalidUserException` (deleted or disabled account) → call `signOut(context)` and route to `LoginActivity` with a "Your access has been removed" message, clearing the back stack (`FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK`).
  - Transient network failures are ignored (keep session; retry next tick).
  - Also register a `FirebaseAuth.AuthStateListener` as a belt-and-suspenders redirect for any other path that ends the session.
- `handleFirestoreError(e)` helper: repositories/listeners call this on `PERMISSION_DENIED` — treated the same as revocation (this is the ≤1-hour token-expiry backstop if a tick was missed).

Rather than wiring the guard into all five activities by hand, implement it once via `Application.registerActivityLifecycleCallbacks` in a small `TechCityApplication` class (new, registered in the manifest) — every current and future activity is covered automatically, except `LoginActivity` which is excluded.

### Step 3 — `Loginactivity.kt` (new) + manifest

- Compose screen consistent with the app's existing style (warm off-white `0xFFFCF9F5` background, `tc_logo_round` logo, purple `0xFF6200EE` accent like the sync button): Username field, Password field (masked, show/hide toggle), "SIGN IN" button with loading spinner, inline error text.
- On success → launch `MainActivity`, `finish()` the login activity.
- Register in `AndroidManifest.xml` as `exported="false"`, portrait, standard theme (MainActivity remains the launcher so the existing splash-screen flow at `MainActivity.kt:68-93` is untouched).
- No sign-up, no password reset link (accounts are console-managed; a forgotten password is fixed by the owner resetting it in the console — note this on the screen or leave it to word-of-mouth).

### Step 4 — Gate `MainActivity` on sign-in

In `MainActivity.onCreate` (`MainActivity.kt:68`), immediately after `super.onCreate`: if `!Authmanager.isSignedIn()`, start `LoginActivity` and `finish()` before `setContent`. Set `isDataReady = true` first so `setKeepOnScreenCondition` (line 73) releases the splash instead of hanging. The `HomeScreen` `LaunchedEffect` (line 117) that loads cached local data therefore never runs for a signed-out user — no data is shown pre-auth (spec req. 1).

**Silent sign-in / no repeated login prompts:** Firebase Auth persists the session on-device across app restarts, so this gate is a purely local, instant `currentUser` check — returning users go straight to the home screen with no credential entry and no blocking network call. Credential validity is verified *in the background* by the Step 2 session guard (`reload()` on resume + periodic tick); the login screen reappears only on first launch, after an explicit sign-out, or when the guard discovers the account was deleted/disabled in the console. A background check that merely fails from lack of network keeps the session and the app proceeds with cached data.

Add a small **Sign out** text button on the home screen (below the SYNC button area, `MainActivity.kt:289-341`) that calls `Authmanager.signOut(context)` and routes to `LoginActivity`. Home screen is the natural "settings" surface here; the merged-view toggle inside `PhoneListActivity` is a per-list control and not a good fit.

### Step 5 — Security rules (Firestore + Storage)

The client-side gate is cosmetic without rules. Update in Firebase Console (or via `firestore.rules` / `storage.rules` files if the project adopts the Firebase CLI later — console is fine for now):

- **Firestore:** all collections (`inventory`, `phones`, `phone_images`, and any future ones) → `allow read, write: if request.auth != null;` (single wildcard match on `/{document=**}`).
- **Storage:** the app pulls device images from Firebase Storage — same rule: `allow read, write: if request.auth != null;`.
- Deploy rules **only after** Steps 1–4 are shipped to all devices in use, otherwise existing installs go dark mid-day (owner said there are no other users to migrate, so ordering risk is minimal — but still deploy rules last).

### Step 6 — Permission-denied handling in data paths

Firestore listeners created by `Inventoryrepository.kt` / `Phonelistactivity.kt` / `Phonedetailactivity.kt` already have error branches that log-and-keep-last-state. Extend those branches: if the error code is `PERMISSION_DENIED`, call `Authmanager.handleFirestoreError` → sign-out + redirect (revoked mid-session, token expired). All other errors keep today's behavior (retain last data, no crash). The one-shot sync path in `MainActivity` (`syncAllData`, ~line 490) surfaces it as the existing `syncError` text plus the same redirect.

### Step 7 — Verification

Manual test matrix (each row maps to an acceptance criterion; "console" = Firebase Console):

| Action | Expected |
|---|---|
| Fresh install, open app | Login screen; no data fetched (verify via no Firestore traffic in logs) |
| Sign in with a console-created account | Home screen; sync and all lists work as today |
| Sign in with wrong password / nonexistent username | Inline error, stays on login |
| Kill and reopen app while signed in | Straight to home screen (session persists) |
| **Delete** the signed-in account in console, app foregrounded | Within ~1 tick (≤60s) app returns to login with "access removed" message |
| **Disable** the account instead | Same as delete; re-enabling lets the user sign back in with the same password |
| Delete account while app is backgrounded, then reopen | `onResume` reload fails → login screen |
| After revocation, check device | `PhoneListHolder` empty, synced JSON files deleted, no inventory viewable anywhere |
| Signed-out (or revoked) user, rules deployed | Firestore/Storage reads fail — verify with rules playground or a signed-out REST call |
| Airplane mode at app launch while signed in | App opens normally with cached data (transient reload failure ignored) |
| Sign out from home screen | Returns to login; local synced data cleared |

## Files Touched

| File | Change |
|---|---|
| `app/build.gradle.kts` | Add `firebase-auth` dependency |
| `Authmanager.kt` (new) | Sign-in/out, username→email mapping, session guard, permission-denied handler |
| `Loginactivity.kt` (new) | Login screen |
| `TechCityApplication.kt` (new) | Registers session guard via activity lifecycle callbacks |
| `AndroidManifest.xml` | Register `LoginActivity` + application class |
| `MainActivity.kt` | Auth gate in `onCreate`, sign-out button, sync-path permission handling |
| `Inventoryrepository.kt`, `Phonelistactivity.kt`, `Phonedetailactivity.kt` | `PERMISSION_DENIED` branch → `Authmanager.handleFirestoreError` |
| Firebase Console | Enable Email/Password, create accounts, deploy Firestore + Storage rules |

## Risks & Mitigations

- **Revocation lag ceiling:** if the session guard somehow never runs (app killed, guard bug), the deleted user's token still dies within 1 hour and Step 6 catches the resulting `PERMISSION_DENIED`. Two independent layers cover spec req. 9.
- **Rules deployed before app update:** any device on the old build loses data access instantly. Mitigation: deploy rules last (Step 5 ordering); owner confirmed no other active users.
- **Offline cached data after revocation:** Firestore's own offline cache and the `SyncDataManager` JSON are cleared on sign-out; cached image files remain on disk but are inaccessible through the app once signed out. If image files themselves are considered sensitive, add `ImageCacheManager` cleanup to `signOut` (one extra call — decide during implementation).
- **`reload()` network cost:** one tiny HTTPS call per minute while foregrounded — negligible; interval is a single constant if it ever needs loosening.
- **Owner lockout:** the owner deleting their own account in the console locks them out of the app like anyone else (no in-app admin role exists to protect). Recovery is simply re-creating the account in the console — acceptable, but worth knowing.
- **Username domain typo:** accounts must be created with the exact `@techcity.app` suffix the app appends; a mismatch looks like "wrong password". Documented in Step 0; keep the domain as a single constant in `Authmanager`.

## Out of Scope (per spec + owner's direction)

In-app user management or approval screens, a `users` Firestore collection, roles beyond signed-in/not, password reset flows, audit logging, Firebase project/IAM membership, and push-based (listener-triggered) revocation — the polling guard plus rules cover the requirement.
