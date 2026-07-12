# Plan: Sold Device Inventory Listener

- **Spec:** `_specs/sold-device-inventory-listener.md`
- **Branch:** `feature/sold-device-inventory-listener`
- **Status:** Implemented (pending manual verification, Step 5)
- **Created:** 2026-07-11

## Approach Summary

Both screens already query `inventory` with `whereIn("status", ["On-Hand", "On-Display"])`. Because a sale is just the inventory document's `status` field changing to `"Sold"`, the sold document simply *leaves* that query's result set. So the entire feature reduces to: **replace the one-shot `.get()` calls with `addSnapshotListener` on the exact same queries**, and recompute the derived UI state on every snapshot. This automatically covers sales, restocks/additions (spec Q4: yes), voided transactions, and offline catch-up — Firestore delivers a fresh consistent snapshot in all cases. No transactions collection is read.

Key architectural facts driving the plan:

- `PhoneListScreen` (`Phonelistactivity.kt:786`) holds `phones` in Compose state; all downstream views (filtering at line 877, merged groups at line 895, `PhoneListHolder.filteredPhones` at line 904) are derived via `remember`/`LaunchedEffect` keyed on `phones` — so once `phones` updates from a snapshot, filters, sorting, merged-variant cards, and the comparison flow all recompute for free.
- There is a cache fast-path (`Phonelistactivity.kt:912–923`): when `PhoneListHolder.isSynced`, the screen uses cached `allDevices` and never queries Firestore. The listener must attach *in addition to* showing cached data, so cached sessions still get real-time updates.
- `PhoneDetailScreen` (`Phonedetailactivity.kt:294`) renders a `HorizontalPager` over `PhoneListHolder.uniquePhoneModels` captured at composition. Each page (`PhoneDetailContent`) does its own one-shot inventory query (`Phonedetailactivity.kt:394–448`) to build `variants`, `allAvailableColors`, and `variantColorsMap`.

## Implementation Steps

### Step 1 — Extract shared inventory-grouping logic

The grouping of inventory documents into `Phone` objects (`Phonelistactivity.kt:1075–1140`) will run on every snapshot, and the same logic exists in `MainActivity`'s sync path (~line 521). Extract it into a top-level function (new file `Inventoryrepository.kt`, matching existing file-naming style):

- `fun groupInventoryDocs(docs: List<DocumentSnapshot>, specsMap: Map<String, DeviceSpecs>): List<Phone>` — pure function, exact same behavior as today (grouping key `manufacturer|model|ram|storage`, distinct non-empty colors, `stockCount = items.size`, sort by manufacturer/model/price).
- Keep `DeviceSpecs` parsing where it is; the `phones` (specs) collection is still fetched once with `.get()` — specs don't change when a unit sells.

This step is pure refactor; behavior identical.

### Step 2 — Real-time listener in `PhoneListScreen`

Rework the data-loading block (`Phonelistactivity.kt:912` onward):

1. Keep the cache fast-path: if `PhoneListHolder.isSynced`, immediately show cached `allDevices`/`allPhoneImages` and set `isLoading = false` (unchanged first paint).
2. In both paths (cached and cold), fetch the `phones` specs collection once, then attach `addSnapshotListener` to `db.collection("inventory").whereIn("status", listOf("On-Hand", "On-Display"))`.
3. On every snapshot: run `groupInventoryDocs`, assign to `phones` state, and also push into `PhoneListHolder` (add a `fun updateDevices(devices: List<Phone>)` setter, since `allDevices` has a private setter) so the detail screen's pager source and the comparison flow stay consistent with what the list shows.
4. `isLoading` turns off on the *first* snapshot only; later snapshots update state silently (spec: no spinner/flicker on updates). Firestore listeners deliver the whole updated result set each time, so no manual diffing is needed — recomputing the grouped list guarantees the final state matches actual inventory even for rapid batch sales (edge case: coalescing is inherent).
5. Image fetching: on snapshots that introduce phones with `phoneDocId`s not yet in `phoneImagesMap`, fetch just the missing `phone_images` docs (reuse `fetchAllPhoneImages` with the missing-id subset). Sales never require an image fetch; additions do.
6. Lifecycle: hold the `ListenerRegistration` and remove it in a `DisposableEffect(Unit) { onDispose { registration?.remove() } }`. The existing `LaunchedEffect(Unit)` load block becomes/coordinates with this `DisposableEffect` so exactly one listener exists per screen instance.
7. Error handling: the snapshot callback's error branch logs and returns, keeping the last known list on screen (spec req. 10). Firestore auto-resumes after connectivity loss — no extra work.
8. `TEST_MODE` path (`Phonelistactivity.kt:931`): leave untouched (spec edge case allows explicit exclusion); real-time applies only to the normal path.

Scroll-position stability (spec req. 3): items are keyed by identity in the `LazyColumn`/grid; verify the `items(...)` call uses a stable key (`manufacturer|model|ram|storage`) and add one if missing, so an unrelated card's removal doesn't reset scroll or re-animate untouched cards.

### Step 3 — Real-time listener in `PhoneDetailContent`

Replace the one-shot variants query (`Phonedetailactivity.kt:394–448`) with a snapshot listener:

1. Convert the `LaunchedEffect(phone.manufacturer, phone.model)` block to a `DisposableEffect(phone.manufacturer, phone.model)` that attaches `addSnapshotListener` to the same query (`manufacturer` + `model` + `whereIn status`), and removes it `onDispose`. The pager's `beyondViewportPageCount = 1` means up to ~3 pages hold listeners at once — acceptable, and disposal is automatic as pages leave composition.
2. On each snapshot, rebuild `variants`, `allAvailableColors`, and `variantColorsMap` with the existing logic — but change the `orderedColors` seeding (`Phonedetailactivity.kt:438–443`): today it starts from `phone.colors` (the stale intent-time list) and only *adds* live colors. It must instead show **only** colors present in the live snapshot (preserving `phone.colors` order for those still available, appending new ones). Otherwise a sold-out color would remain visible forever. Comparisons stay case-insensitive (existing edge case).
3. Selection fallback (spec req. 8): after recomputing, if `selectedColorName` is no longer in the available colors for the selected variant (case-insensitive), reassign it to the first available color; likewise if the selected variant disappeared, select the first remaining variant. Image and price already follow selection state, so they update for free.
4. Auto-close on total sell-out (spec Q3): when a snapshot returns zero documents *after* a previous non-empty result (i.e., not the initial load of an already-stale entry — though zero-on-first-load should also close, covering the "opened from stale list entry" edge case), invoke the back action. `PhoneDetailContent` needs an `onPhoneUnavailable: () -> Unit` callback threaded from `PhoneDetailScreen`, which wires it to `onBackPress` (which calls `finish()` in the activity). Guard so it fires at most once.
5. Do **not** rebuild the pager's page list live. `PhoneListHolder.uniquePhoneModels` is read at composition; mutating it mid-swipe would shift page indices under the user. The currently viewed phone is protected by its own listener (step 3.4); other pages refresh their availability when their listeners attach. The list screen behind it is updated by Step 2, so navigating back is always fresh.
6. Error branch keeps the existing fallback behavior (`Phonedetailactivity.kt:449–459`): retain last known/intent data, no crash.

### Step 4 — `PhoneListHolder` and cache coherence

- Add `updateDevices(devices: List<Phone>)` (and update `allPhoneImages` additively when new images are fetched) so the live listener can refresh the singleton despite private setters. Do not flip `isSynced` or `lastSyncTime` — those describe the full daily sync.
- `SyncDataManager`'s on-disk JSON is intentionally *not* rewritten on every snapshot (write amplification for no benefit): the next app launch loads the day-old file, and the listener corrects it within the first snapshot. Note this in code where the listener updates the holder.

### Step 5 — Verification

Manual test matrix (edit inventory docs in the Firebase console while the app runs; each row maps to an acceptance criterion):

| Action in console | Expected on list screen | Expected on detail screen |
|---|---|---|
| Set one unit to `Sold`, duplicates of same model/variant/color remain | No visible change (count only) | No visible change |
| Sell last unit of a color | Color dot removed from card | Color removed; selection falls back if it was selected |
| Sell last unit of a variant | Card removed (merged card degrades to single variant when merged view is on) | Variant chip removed |
| Sell last unit of a model | Model disappears from list | Open detail page auto-closes to list |
| Flip a `Sold` unit back to `On-Hand` (restock/void) | Card/color/variant reappears | Option reappears |
| Batch: mark 3 units sold rapidly | Final state matches remaining inventory | Same |
| Toggle airplane mode, sell a unit, reconnect | UI catches up on reconnect | Same |

Also verify: scroll position survives an unrelated card removal; comparison mode and display filters still work after an update; Logcat shows listener detach on activity destroy (add a debug log in `onDispose`); cold start with cached sync still paints instantly then attaches the listener; `TEST_MODE` path still compiles and runs.

## Files Touched

| File | Change |
|---|---|
| `Inventoryrepository.kt` (new) | Shared `groupInventoryDocs` + listener attach helper |
| `Phonelistactivity.kt` | Listener-based loading, `DisposableEffect` cleanup, incremental image fetch, stable lazy-list keys |
| `Phonedetailactivity.kt` | Listener-based variants/colors, live-only color list, selection fallback, auto-close callback |
| `Phonelistholder.kt` | `updateDevices` setter, additive image-map update |
| `MainActivity.kt` | (Optional, low-risk) reuse `groupInventoryDocs` in the sync path to avoid divergence |

## Risks & Mitigations

- **Read cost:** a listener on the full available-inventory query bills reads per changed document, not per snapshot re-delivery; for a retail-store inventory size this is negligible. (Answers spec Q5 — no rule changes needed since the app already reads this query.)
- **Pager index drift:** avoided by design decision in Step 3.5 (page list frozen per detail-screen session).
- **Cached-then-live flicker:** the first live snapshot may differ from the day-old cache and legitimately change the list shortly after open — this is the feature working, not a bug; stable lazy keys keep it visually calm.
- **`whereIn` listener limits:** two-value `whereIn` is well within Firestore limits; no composite-index change expected for the list query (equality-free), but the detail query (`manufacturer` + `model` + `status in`) already runs today, so its index requirements are already satisfied.

## Out of Scope (per spec)

Writing transactions, security-rule changes, push notifications, persisting live updates to the daily sync files, and the `TEST_MODE` path.
