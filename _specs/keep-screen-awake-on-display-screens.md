# Keep Screen Awake on Display Screens

- **Slug:** `keep-screen-awake-on-display-screens`
- **Branch:** `feature/keep-screen-awake-on-display-screens`
- **Status:** Implemented (pending on-device verification, see `_plans/keep-screen-awake-on-display-screens.md`)
- **Created:** 2026-10-20

## Overview

The app runs on tablets placed in the store as customer-facing displays. When nobody touches a tablet for a while, Android treats the device as idle and turns the screen off, so the tablet goes dark in front of customers and staff have to wake it up again. This feature keeps the screen on for as long as the app is in the foreground on any of its screens, using the platform's built-in "keep screen on" request rather than any ongoing background activity. The behavior is always on; there is no setting to turn it off.

## Current Behavior

- All screens are locked to portrait orientation and run as plain activities with no special display handling.
- The app does nothing to prevent the system screen timeout. After the tablet's configured inactivity period, the screen dims and then turns off regardless of which screen the app is on.
- Once the screen is off, the tablet must be woken manually (power button or tap), and depending on device settings the lock screen may need to be dismissed before the app is visible again.
- No activity in the app currently asks the system to keep the screen on.

## Goals

- While any app screen is in the foreground (main, login, phone list, phone detail, phone comparison, image management), the tablet's screen must not dim or turn off, no matter how long it goes untouched.
- Use the platform's supported "keep screen on" request so the system handles it with no timers, no periodic work and no battery cost beyond the display itself.
- The behavior applies automatically whenever an app screen is visible and stops automatically when the app goes to the background or is closed.
- No change to how any screen looks or how users navigate between them.

## Non-Goals

- Keeping the screen on while the app is in the background, minimized, or closed.
- Running any periodic "light activity" or background work to simulate user interaction. The platform request replaces this entirely.
- Changing the tablet's system-level display timeout or developer settings.
- Preventing the user from turning the screen off manually with the power button.
- Overriding a device-management policy, battery saver or low-battery shutdown if the platform refuses the app's request.
- Adding a user-facing or per-device setting to toggle this behavior. The behavior is always on.
- Showing any message to staff if the system overrides the keep-on request (see Edge Cases).

## Functional Requirements

1. Every activity in the app (main, login, phone list, phone detail, phone comparison, image management) must ask the system to keep the screen on while it is in the foreground and visible.
2. The request must be in place from the moment a screen becomes visible, including when returning to it from another screen or from the background.
3. The request must be released when the activity is no longer visible (user presses Home, switches apps, or closes the app). Because every screen in the app makes the request, moving between app screens never leaves a gap where the timeout could trigger.
4. The mechanism must be the platform's standard "keep screen on" request for a visible window. No wake locks, no timers, no periodic tasks, no fake input, and no background services.
5. The mechanism must add no measurable CPU, network or disk cost, and no impact on scrolling or animation smoothness.
6. The behavior must survive the activity being recreated (for example a configuration change or process restoration) with no manual action.
7. The feature must not require any new permission declarations or runtime permission prompts.
8. The behavior is always on. There is no setting, flag, or per-location configuration to disable it.
9. The feature must work on every tablet the app currently supports (the 601 x 1007 dp tablet, the 824 x 1318 dp tablet and the 11-inch 1200 x 1920 pixel tablet).
10. The approach must be applied consistently across all activities so a new activity added later can adopt it the same way.

## User Experience

- Staff open the app and leave the tablet on any screen. The screen stays bright and visible for as long as it sits there; it never dims or goes black.
- Customers can walk up at any time and see the content immediately without anyone needing to wake the tablet.
- Staff signing in or managing images are not interrupted by a screen timeout mid-task.
- Nothing visible changes on any screen: no indicator, no toast, no spinner. The feature is silent.
- If staff press Home or switch to another app, normal system behavior resumes and the screen may time out.
- Pressing the power button still turns the screen off as usual. Waking the tablet brings the app back exactly where it was.

## Edge Cases

- **Dialog or overlay shown on top of a screen:** The underlying screen is still the foreground activity, so the screen must remain on.
- **App sent to background:** The request is released by the system because the window is no longer visible; when the app returns to the foreground, the request is active again with no extra work.
- **Activity recreated (configuration change, low memory restoration):** The recreated screen makes the request again automatically.
- **Device-management policy, battery saver or low battery:** The platform may ignore the request and turn the screen off anyway (for example a work-managed tablet with a forced "lock after 5 minutes" policy). The app stays silent, does not crash, and continues to request keep-on; normal behavior resumes when the platform allows. No message is shown to staff because the app cannot reliably detect the override or fix it.
- **Rapid navigation between screens:** Each screen makes its own request, so switching back and forth must not leak resources or leave stale state. There is nothing to clean up beyond what the platform handles for a window.
- **User manually turns the screen off with the power button:** The app must not fight this; it must not turn the screen back on by itself.
- **App killed by the system:** The request dies with the window; nothing outlives the app.

## Acceptance Criteria

- [ ] With the tablet's system screen timeout set to its shortest value, the screen stays on for at least 30 minutes untouched on the main activity.
- [ ] Same for the login activity.
- [ ] Same for the phone list activity.
- [ ] Same for the phone detail activity.
- [ ] Same for the phone comparison activity.
- [ ] Same for the image management activity.
- [ ] Navigating main, then phone list, then phone detail, then phone comparison, then back to main keeps the screen on throughout with no dimming at any step.
- [ ] Pressing Home while on any screen lets the tablet time out according to system settings.
- [ ] Returning to the app after Home re-enables keep-on and the screen stays on again.
- [ ] Pressing the power button turns the screen off immediately and the app does not turn it back on.
- [ ] No new permission is declared and no permission dialog is shown to the user at any point.
- [x] No timers, periodic tasks, wake locks or background services are introduced.
- [ ] No visible change to the layout or content of any screen.
- [ ] Scrolling the phone list and interacting with phone detail feel the same as before; no stutter or flicker introduced.
- [ ] Verified on all three supported tablets (601 x 1007 dp, 824 x 1318 dp, and the 11-inch 1200 x 1920 pixel device).

## Open Questions

- None. The following decisions were made during review:
  - Use the platform's "keep screen on" request instead of light background activity.
  - Apply to every activity, including phone comparison and login.
  - Behavior is always on; no toggle.
  - Stay silent if the system overrides the request (device policy, battery saver, low battery). Revisit if staff report tablets going dark on managed devices.
