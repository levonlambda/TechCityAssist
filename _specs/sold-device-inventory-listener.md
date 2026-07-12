# Sold Device Inventory Listener

- **Slug:** `sold-device-inventory-listener`
- **Branch:** `feature/sold-device-inventory-listener`
- **Status:** Draft
- **Created:** 2026-07-11

## Overview

Add a real-time Firestore listener that reacts when a device is sold through a device transaction. When a sale happens, the app re-evaluates what is actually available and updates the phone list screen (`PhoneListActivity`) and the phone detail screen (`PhoneDetailActivity`) accordingly. If other units of the same model, variant (RAM/storage), and color are still available (status `On-Hand` or `On-Display`), the UI stays as it is. If the sold unit was the last of its kind, the UI is updated to show only what remains available — removing the sold-out color, variant, or entire phone card as appropriate.

## Current Behavior

- Both `PhoneListActivity` and `PhoneDetailActivity` load availability with one-time Firestore `.get()` queries against the `inventory` collection, filtered to `status in ["On-Hand", "On-Display"]`.
- Inventory documents carry `manufacturer`, `model`, `ram`, `storage`, `color`, `retailPrice`, `dealersPrice`, and `status` fields. Each document represents one physical unit.
- The phone list groups inventory documents by `manufacturer|model|ram|storage` into `Phone` entries, deriving the color list and `stockCount` from the grouped documents (with an optional merged-variants mode that combines two variants into a single card).
- The detail screen queries inventory for the phone's manufacturer/model to build the variant list (`PhoneVariant`) and the per-variant color availability map.
- When a device is sold (its inventory document's `status` changes away from `On-Hand`/`On-Display`, e.g. to a sold status recorded by the device transaction), neither screen notices. The stale phone, variant, or color remains visible until the user re-launches or re-enters the screen, so staff can show or offer a unit that is no longer in stock.

## Goals

- Detect device sales in real time while the app is in the foreground, without requiring the user to navigate away and back or restart the app.
- Keep `PhoneListActivity` accurate: when the last available unit of a model/variant is sold, remove or update its card; when the last unit of a specific color is sold, remove that color from the card's color list; keep `stockCount` correct.
- Keep `PhoneDetailActivity` accurate: when availability changes for the phone being viewed, refresh the variant list and the per-variant color availability so only in-stock (`On-Hand`) or on-display (`On-Display`) options are shown.
- No visible change when a sale does not exhaust availability: if at least one unit of the same model, variant, and color remains `On-Hand` or `On-Display`, the displayed options remain the same (only internal counts update).
- Respect the existing merged-variants toggle: merged cards must also reflect availability changes correctly.
- Clean up listeners with the screen lifecycle so there are no leaks or background listeners after the screen is closed.

## Non-Goals

- Creating or writing device transaction records (sales are recorded by the admin/back-office side; this app only reacts to them).
- Handling restocks, returns, or newly added inventory in real time (nice-to-have if it falls out of the chosen listener design, but not required by this spec).
- Changing the sale/transaction data model or Firestore security rules beyond what is needed to read the relevant data.
- Offline conflict resolution beyond Firestore's built-in snapshot behavior.
- Push notifications or alerts about sales.

## Functional Requirements

1. The app must observe Firestore in real time so that a device sale recorded via a device transaction is detected while `PhoneListActivity` or `PhoneDetailActivity` is visible.
2. On detecting a sale, the app must determine whether any units of the same manufacturer, model, RAM/storage variant, and color remain with status `On-Hand` or `On-Display`.
3. If availability remains for that model/variant/color combination, the visible UI must not change (no flicker, no reordering, no removed options); only internal stock counts update.
4. If the sold unit was the last of its color for a variant, that color must no longer be offered for that variant on either screen.
5. If the sold unit was the last unit of a variant (all colors exhausted), the variant must be removed from the detail screen's variant list and the corresponding card in the list screen must be removed or updated (including within merged-variant cards).
6. If the sold unit was the last unit of the entire model, the phone must be removed from the phone list, and the detail screen (if open on that phone) must handle the phone becoming unavailable gracefully rather than crashing or showing empty/broken content.
7. The list screen's displayed grouping, sorting, and active display filters must continue to apply correctly after a real-time update.
8. If the user has a color or variant selected on the detail screen and that selection becomes unavailable, the selection must move to an available option (and the displayed image/price follow the new selection).
9. Listeners must be registered when the screen becomes active and removed when it is destroyed or stopped, following the activity/composable lifecycle.
10. Listener errors (permission denied, network loss) must not crash the app; the last known data remains displayed and normal loading resumes on reconnect.

## User Experience

- Staff browsing the phone list see sold-out phones, variants, or colors disappear automatically, typically within a few seconds of the sale being recorded, without any manual refresh.
- A customer-facing detail screen never offers a color or variant that just sold out; the option quietly disappears and, if it was selected, the screen falls back to an available option.
- When availability is unaffected by a sale (other units remain), the user notices nothing — no flicker, spinner, or scroll-position jump.
- If the phone being viewed sells out entirely, the detail screen communicates that the device is no longer available (e.g., returns to the list or shows an unavailable state) instead of showing stale data.
- Loading indicators are only shown on initial load, not on every real-time update.

## Edge Cases

- Multiple units sold in quick succession (e.g., a batch transaction) — updates must coalesce correctly and the final displayed state must match the actual remaining inventory.
- Sale of a unit whose color string differs only in case from others (color comparisons elsewhere are case-insensitive) — availability checks must use the same case-insensitive matching.
- The merged-variants toggle is on and one of the two merged variants sells out — the merged card must degrade to showing only the remaining variant.
- The detail screen is opened from a stale list entry for a phone that sold out between screens — the detail screen must handle finding zero available inventory.
- The device is offline when the sale occurs — the UI must catch up when connectivity returns.
- A transaction is corrected or voided after being recorded — the screens should reflect whatever the inventory statuses say at that time (inventory state is the source of truth for display).
- The `TEST_PHONE_DOC_ID` single-phone test path in `PhoneListActivity` — real-time updates either apply there too or are explicitly excluded without breaking that path.

## Acceptance Criteria

- [ ] With the phone list open, marking an inventory unit as sold in Firestore removes/updates the affected card, color, or count without user interaction.
- [ ] With the detail screen open on the affected phone, the sold-out color or variant disappears and any active selection falls back to an available option.
- [ ] When other units of the same model/variant/color remain `On-Hand` or `On-Display`, neither screen visibly changes.
- [ ] Selling the last unit of a model removes it from the list, and an open detail screen for it handles the state gracefully.
- [ ] The merged-variants display mode reflects availability changes correctly.
- [ ] Display filters, sorting, and comparison mode continue to work after real-time updates.
- [ ] No Firestore listeners remain active after the activities are destroyed (verified via lifecycle review/logging).
- [ ] Existing behavior on initial load is unchanged for both screens.

## Open Questions

1. What exactly does a "device transaction" look like in Firestore — is there a dedicated transactions collection this app can read, or is the sale visible only as the inventory document's `status` changing away from `On-Hand`/`On-Display`? Listening directly to the `inventory` query the app already uses would cover sales regardless of how they are recorded; confirm which source the listener should attach to. - yes listen directly to the inventory collection. a sale just means changing the status to sold.
2. What status value(s) mark a sold unit (e.g., `"Sold"`), and are there other statuses that should also count as unavailable? the status field is changed to "Sold"
3. When the phone currently open in the detail screen sells out completely, should the screen auto-close back to the list, or stay open with an "out of stock" indication? - auto-close back to the list.
4. Should real-time updates also cover additions (restocks/new arrivals appearing without refresh), which a snapshot listener would provide for free? - yes also cover addition.
5. Are there Firestore security-rule or read-cost concerns with keeping a listener on the full available-inventory query while the list screen is open? -im not sure
