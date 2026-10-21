# Plan: Keep Screen Awake on Display Screens

- **Spec:** `_specs/keep-screen-awake-on-display-screens.md`
- **Branch:** `feature/keep-screen-awake-on-display-screens`
- **Status:** Implemented (code complete and compiling; pending Step 4 on-device verification on the three tablets)
- **Created:** 2026-10-20

## Approach Summary

Android lets a visible window ask the system not to dim or turn off the display: the window flag `FLAG_KEEP_SCREEN_ON` (`WindowManager.LayoutParams`). It needs no permission, no wake lock and no ongoing work. The system honors it only while the window is visible and in front, so it is released automatically when the user presses Home, switches apps, or the activity finishes, and it comes back automatically when the activity is visible again. That matches every requirement in the spec (FR 1-9) without any per-screen lifecycle code.

The spec requires the behavior on **every** activity (main, login, phone list, phone detail, phone comparison, image management) and asks that it be applied consistently so new activities pick it up the same way (FR 10). Rather than adding the flag to six `onCreate` methods, the plan registers **one `Application.ActivityLifecycleCallbacks` in `TechCityApplication`** that sets the flag in `onActivityCreated` for whatever activity is being created. Every current and future activity is covered with no per-activity edit, and there is exactly one place to look.

Why `onActivityCreated` and not `onActivityResumed`: the window object exists before `onCreate` runs, the flag is a persistent property of the window (not a one-shot request), and an activity that is recreated after a configuration change or process restoration goes through `onCreate` again, so FR 6 is satisfied for free. Setting it in `onResumed` would also work but would re-add the flag on every return, which is harmless yet pointless.

Why a separate callback object rather than extending the session guard in `Authmanager`: the guard is about auth revocation; mixing display policy into it would make both harder to reason about. Registering a second callback costs nothing.

Why not the Compose route (a `DisposableEffect` that sets the flag from `LocalView.current`): it would have to be repeated in six `setContent` blocks, which is exactly the duplication FR 10 is trying to avoid. `MainActivity` also has an early-return branch that starts `LoginActivity` and finishes before `setContent`; a window-level flag set in `onActivityCreated` is unaffected by that branch, and it does not matter either way because that activity instance finishes immediately.

**Spec edge cases, mapped:**

| Edge case | How the window flag behaves |
|---|---|
| Dialog or overlay on top | The activity window is still visible underneath; flag stays effective. |
| App sent to background / Home | Window no longer visible; system stops honoring the flag. On return, window is visible again and the flag (still set) is honored. Nothing to do. |
| Activity recreated | `onActivityCreated` fires again and re-sets the flag. |
| Device policy / battery saver / low battery | System may ignore the flag. App does nothing and shows nothing (spec decision). |
| Rapid navigation between screens | Each activity owns its own window and flag; nothing to clean up. |
| Power button | Turns the screen off regardless of the flag. The flag never turns a screen back on. |
| App killed | Window gone, flag gone. |

## Implementation Steps

### Step 1 — Add the keep-screen-on lifecycle callback

File: `app/src/main/java/com/techcity/techcityassist/TechCityApplication.kt`

1. Create a small private object (or a top-level file-private class) implementing `Application.ActivityLifecycleCallbacks` with every method as a no-op except `onActivityCreated`, which calls `activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)`.
2. In `TechCityApplication.onCreate()`, after the existing `Authmanager.startSessionGuard(this)` and `LocationManager.init(this)` calls, register it with `registerActivityLifecycleCallbacks(...)`.
3. Add a short comment above the registration stating the intent (store display tablets must never time out while the app is in front) and pointing at the spec slug, so a future reader knows why every window carries the flag.

No change to the manifest, no new dependency, no new permission.

### Step 2 — Confirm nothing else touches the window flags

1. Search the codebase for `FLAG_KEEP_SCREEN_ON`, `clearFlags`, `keepScreenOn` and `WakeLock` to confirm no existing code clears the flag or already requests it. (Checked at planning time: there are none.)
2. Confirm all six activities extend `ComponentActivity` (they do) and are declared in the manifest (they are), so the callback reaches all of them.

### Step 3 — Build

Run the Gradle build to make sure the project compiles:

```
.\gradlew.bat :app:assembleDebug
```

### Step 4 — On-device verification (owner, on each of the three tablets)

Pre-step: in the tablet's Settings, set **Display → Screen timeout** to its shortest value (usually 15 seconds) so each check is quick.

1. Install the debug build and open the app. On each screen below leave the tablet untouched for at least 1 minute (well past the 15-second timeout) and confirm the screen neither dims nor turns off:
   - Main (brand grid)
   - Login (sign out first, or launch with no account)
   - Phone list
   - Phone detail
   - Phone comparison
   - Image management (admin)
2. Walk the path main → phone list → phone detail → phone comparison → back to main, pausing 30 seconds on each step, and confirm no dimming at any point.
3. With the app on any screen, open a dialog or overlay if one exists on that screen and leave it untouched; the screen must stay on.
4. Press Home and leave the tablet; it must time out normally. Reopen the app; the screen must stay on again.
5. Press the power button while on any screen; the screen must turn off immediately and must not come back on by itself.
6. Scroll the phone list and interact with phone detail to confirm no change in smoothness or layout.
7. Optional confirmation from a PC with the tablet attached over ADB, while the app is in front:

```
adb shell dumpsys window windows | findstr /i "FLAG_KEEP_SCREEN_ON techcityassist"
```

The app's window entry should list `FLAG_KEEP_SCREEN_ON` among its flags.

8. For the 30-minute acceptance checks in the spec, leave one tablet on the phone list and one on phone detail for 30 minutes and confirm they are still lit at the end. This only needs to be done once, on one tablet, if the 1-minute checks pass on all three.

### Step 5 — Update the spec and plan status

1. Tick the acceptance criteria in `_specs/keep-screen-awake-on-display-screens.md` as they are verified and set its status to Implemented.
2. Set this plan's status to Implemented, noting which tablets were verified.

## Files Touched

| File | Change |
|---|---|
| `app/src/main/java/com/techcity/techcityassist/TechCityApplication.kt` | Register one lifecycle callback that adds `FLAG_KEEP_SCREEN_ON` to every activity window on create. |
| `_specs/keep-screen-awake-on-display-screens.md` | Status and acceptance criteria after verification. |
| `_plans/keep-screen-awake-on-display-screens.md` | Status after verification. |

No activity files, manifest, Gradle files or resources change.

## Risks and Notes

- **Battery:** the display is the only cost. Tablets on the sales floor are expected to be plugged in; if a tablet runs on battery all day, the screen will drain it faster than before because it never turns off. This is the intended behavior per the spec (always on, no toggle).
- **Burn-in:** the main screen and detail screens are mostly static. OLED tablets left on one screen for many hours a day could show image retention over the long term. Most store tablets are LCD, where this is not a concern. Worth knowing, not worth acting on now.
- **Managed devices:** if a tablet is enrolled in a device-management profile with a forced lock timeout, the flag will be ignored and the screen will still lock. The spec says to stay silent; if this shows up in practice, the fix is on the device policy side, not in the app.
- **Splash screen on main:** `installSplashScreen()` runs before `super.onCreate()`, and the splash is rendered in the same window, so the flag applies during the splash as well. No special handling needed.
- **Testing:** there is no practical unit test for a window flag. An instrumented test could launch each activity with `ActivityScenario` and assert `activity.window.attributes.flags and FLAG_KEEP_SCREEN_ON != 0`. This is cheap to add if desired, but the on-device checks in Step 4 are the real verification, so the plan leaves the test optional.
