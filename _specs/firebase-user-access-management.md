# Firebase User Access Management

- **Slug:** `firebase-user-access-management`
- **Branch:** `feature/firebase-user-access-management`
- **Status:** Draft
- **Created:** 2026-08-12

## Overview

TechCityAssist currently connects to Firebase (Firestore) without any sign-in or access control, meaning anyone with the app build can read and write inventory data. This feature introduces user authentication and an owner-controlled access list, so the owner can grant access to specific users and revoke it at any time. Users without granted access must not be able to read or modify any data.

## Current Behavior

- The app initializes Firestore directly in its activities and repositories (inventory, phone images, comparisons) with no authentication step.
- There is no login screen; the app opens straight into the main activity and data loads for anyone running the app.
- There is no concept of user identity, roles, or an approved-users list anywhere in the app or database.
- Firebase security is effectively governed only by the project's security rules, with no per-user enforcement surfaced in the app.

## Goals

- Require every user to sign in before the app loads or displays any Firebase data.
- Give the owner (admin) a way to grant access to a user and to remove that access later.
- Enforce access both in the app experience (blocked users see an appropriate screen, not data) and at the Firebase security-rules level (blocked users cannot read or write data even outside the app).
- Have access revocation take effect promptly, including for users who are currently signed in.
- Provide the admin a view of who currently has access and who has requested or been denied access.

## Non-Goals

- Fine-grained permissions or multiple roles beyond a simple admin vs. approved user distinction (e.g., no per-collection or read-only tiers in this iteration).
- Self-service account management such as password reset flows beyond what the chosen sign-in provider offers out of the box.
- Audit logging or history of access changes.
- Managing Firebase project members (console/IAM access); this feature only governs app users' access to app data.

## Functional Requirements

1. The app must present a sign-in screen on launch when no user is signed in, and no inventory data may load before sign-in completes.
2. After sign-in, the app must check whether the signed-in user has been granted access before showing the main experience.
3. A user who is signed in but not granted access must see a "pending approval / access denied" screen with no access to inventory data, and an option to sign out.
4. The owner's account must be designated as admin and always retain access; the admin cannot revoke their own access.
5. The admin must have an in-app screen listing users who have signed in to the app, showing at minimum their identity (name/email) and current access state (pending, granted, revoked).
6. The admin must be able to grant access to a pending or revoked user from that screen.
7. The admin must be able to revoke access from a granted user from that screen.
8. Firebase security rules must deny all reads and writes of app data to unauthenticated users and to authenticated users who have not been granted access.
9. When a user's access is revoked while they are using the app, they must be returned to the access-denied state without needing to reinstall; at minimum, revocation must take effect on the next data operation or app restart.
10. Signing out must return the user to the sign-in screen and stop all active data listeners.
11. The access-management screen must be reachable only by the admin (e.g., from settings), and hidden from non-admin users.

## User Experience

- **Sign-in screen:** First screen for signed-out users, with a simple branded sign-in flow. On success, the user proceeds either to the main phone list (if granted) or to the pending/denied screen.
- **Pending / denied screen:** Explains that the account is awaiting approval or has had access removed, and offers a sign-out button. No navigation to any data screens is possible from here.
- **Main app:** Behaves exactly as today for granted users; existing screens (phone list, detail, comparison, image management) are unchanged apart from requiring an authorized session.
- **Access management screen (admin only):** A list of user accounts with their email/name and status, with clear grant/revoke actions per user and confirmation before revoking. An entry point appears in the existing settings area only for the admin.

## Edge Cases

- A brand-new user signs in for the first time: they appear in the admin's list as pending and see the pending screen until granted.
- The admin revokes a user who is mid-session: active listeners will start failing due to security rules; the app must handle these permission errors gracefully and route the user to the denied screen rather than crashing.
- No network connectivity at launch: the app must handle inability to verify access state without crashing, and must not fall back to showing cached data to an unverified user.
- The admin account signs in on a fresh install: it must be recognized as admin without needing another admin to approve it.
- A revoked user signs out and back in: they must remain revoked, not reset to pending.
- Firestore offline persistence: previously cached data must not remain readable in-app after access is revoked and the user is routed to the denied screen.

## Acceptance Criteria

- [ ] Launching the app while signed out shows the sign-in screen and no inventory data is fetched.
- [ ] A newly signed-in, unapproved user sees the pending/denied screen and cannot reach any data screens.
- [ ] The admin can see the list of users with their access states.
- [ ] Granting access lets that user into the main app on their next entry (or refresh) without reinstalling.
- [ ] Revoking access blocks that user's reads/writes at the security-rules level, verified by attempting access as the revoked user.
- [ ] A revoked mid-session user is routed to the denied screen without the app crashing.
- [ ] The admin cannot revoke their own access, and non-admin users cannot see or reach the access-management screen.
- [ ] Sign-out works from both the main app and the denied screen, returning to the sign-in screen.

## Open Questions

- Which sign-in provider(s) should be supported — Google Sign-In only, email/password, or both? - i prefer email/password
- Should new users default to a visible "pending" state the admin approves, or should the admin pre-authorize emails before the user ever signs in?  - the admin should authorize before the user signs in
- How is the admin account designated — hardcoded to the owner's email, or configurable in the database? - configurable in the database 
- Is prompt (near-real-time) revocation required, or is "on next app launch / next data operation" acceptable? - near real time is preferred
- Do other people currently use the app whose access must be preserved during rollout (migration concern when security rules tighten)? - no
