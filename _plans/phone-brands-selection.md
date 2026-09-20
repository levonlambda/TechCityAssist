# Plan: Phone Brands Selection

- **Spec:** `_specs/phone-brands-selection.md`
- **Branch:** `feature-phone-brands-selection`
- **Status:** Implemented (pending manual verification, Step 4)
- **Created:** 2026-09-20

## Decisions carried over from spec review

- Brand selection **replaces the category buttons in place** on the home screen (no new activity, no bottom sheet).
- Brand list is built **from the synced data** (`PhoneListHolder`), not from the hardcoded `MANUFACTURER_FILTERS` list.
- **No "All" button** in the brand selection — the user picks a concrete brand. "All" remains available afterwards via the chip row inside PhoneListActivity.

## Approach Summary

Everything needed already exists; the feature is wiring, not new machinery:

- The home screen (`MainActivity.kt`, `HomeScreen` composable) currently launches `PhoneListActivity` directly from the three `HomeButton`s with a `DEVICE_TYPE` extra (`MainActivity.kt:212–249`). We insert a small state machine: `selectedCategory: String?` — `null` shows the three category buttons (today's UI, unchanged), non-null swaps that section of the column for a brand-button list for that category.
- Brands per category are already computable locally: `PhoneListHolder.getDevicesByType(deviceType)` (`Phonelistholder.kt:115`) gives the devices, and `sortManufacturersWithPriority(...)` (`Phonelistactivity.kt:224`) — a top-level function in the same package — gives the exact ordering the chip row uses. Excluding `"techcity"` (case-insensitive) mirrors the chip-row pipeline (`Phonelistactivity.kt:856–875`).
- `PhoneListActivity` already keeps the selected brand in one place: `var selectedManufacturer by remember { mutableStateOf("All") }` (`Phonelistactivity.kt:332`). Pre-selecting a brand is just seeding that initial value from a new intent extra; every downstream behavior (filtering at `Phonelistactivity.kt:879`, chip highlighting, chip taps, price-list copy) works untouched because it all reads/writes that one state variable.
- Fallback-to-"All" for a stale brand is handled where the dynamic chip list already arrives: the `onManufacturersLoaded` callback (`Phonelistactivity.kt:656–659`).

No data-model, sync, or Firestore changes. Two files touched.

## Implementation Steps

### Step 1 — Brand selection state on the home screen (`MainActivity.kt`)

1. In `HomeScreen`, add `var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }` holding the device-type string (`"phone"` / `"tablet"` / `"laptop"`). `rememberSaveable` so a configuration change or process recreation doesn't silently reset the view (activity is portrait-locked, but keyboard/locale changes still recreate).
2. Change the three `HomeButton` `onClick`s from `startActivity(...)` to `selectedCategory = "<type>"`. Enablement rule stays exactly as-is (`isSynced && !isSyncing && !isCheckingLocalData`), which satisfies the spec's "only reachable when synced" requirement — the brand view can only ever be entered from an enabled category button.
3. In the middle section of the column (where the three buttons render), branch on `selectedCategory`:
   - `null` → render today's three category buttons, unchanged.
   - non-null → render the brand list (Step 2) plus a back affordance.
4. Back navigation: add `BackHandler(enabled = selectedCategory != null) { selectedCategory = null }` so the system back gesture returns to the category buttons instead of leaving the app. Also render an explicit "← BACK" text button (or equivalent) above/below the brand list for discoverability — same visual language as the existing `TextButton` sign-out link.
5. The rest of the home screen (logo, sync status, SYNC button, sign-out) stays visible and functional in both states, so the transition feels like part of the home screen per the spec's UX note. If a re-sync completes while the brand view is open, the brand list recomputes automatically (Step 2 derives it from `PhoneListHolder` state).

### Step 2 — Brand list composable (`MainActivity.kt`)

1. Compute the brand list with `remember(selectedCategory, PhoneListHolder.lastSyncTime)`:
   - `PhoneListHolder.getDevicesByType(selectedCategory)`
   - `.map { it.manufacturer }.filter { it.isNotBlank() && !it.equals("techcity", ignoreCase = true) }.distinct()`
   - passed through `sortManufacturersWithPriority(...)` — identical set and order to the chips the user will see next, satisfying the "brand exists in one category only" edge case for free (the filter is per device type).
2. Render one button per brand, reusing the existing `HomeButton` composable (white, rounded, bordered — visual consistency comes free). Wrap the buttons in a `verticalScroll` container (or constrain the section and scroll within it) so many brands stay usable — the current fixed `Spacer(weight)` layout would overflow with >4 brands.
3. Each brand button's `onClick` launches:
   ```
   Intent(context, PhoneListActivity::class.java).apply {
       putExtra("DEVICE_TYPE", selectedCategory)
       putExtra("SELECTED_BRAND", brand)
   }
   ```
4. Double-tap guard: a `remember`ed `hasLaunched` flag set on first tap and reset when the composable re-enters composition (e.g. via a `LaunchedEffect`/lifecycle resume observer), so two rapid taps can't stack two PhoneListActivity instances. (Belt-and-braces: also add `FLAG_ACTIVITY_SINGLE_TOP` to the intent.)
5. Empty category (no brands after filtering): render a short "No <category> available" message with the back affordance instead of a blank area. *Note:* the spec's edge-case section still mentions showing an "All" option here, but that predates the "no All button" decision — flagging this as the one intentional deviation; say the word if you'd rather show a single "ALL" fallback button in the empty case.
6. `selectedCategory` is intentionally **not** cleared after launching the list — returning from PhoneListActivity lands the user back on the brand list for the category they were browsing, which is the natural back-stack expectation. One system-back more returns to the category buttons (Step 1.4).

### Step 3 — Pre-selected brand in `PhoneListActivity` (`Phonelistactivity.kt`)

1. In `onCreate` (`Phonelistactivity.kt:173–185`), read the new extra alongside the existing one: `val initialBrand = intent.getStringExtra("SELECTED_BRAND")`, and pass it to `MainScreen(deviceType, initialBrand)`.
2. In `MainScreen`, seed the existing state: `var selectedManufacturer by remember { mutableStateOf(initialBrand ?: "All") }` (replacing the hardcoded `"All"` at `Phonelistactivity.kt:332`). Because the phone list filters via `matchesManufacturerFilter` (case-insensitive, `Phonelistactivity.kt:719`) inside a `remember(..., selectedManufacturer, ...)` block, the very first frame of the list is already brand-filtered — no flash of the unfiltered list.
3. Validation / stale-brand fallback in the `onManufacturersLoaded` callback (`Phonelistactivity.kt:656–659`), which fires once the dynamic chip list is derived from the data:
   - Find a case-insensitive match for `selectedManufacturer` in the loaded list; if found, normalize `selectedManufacturer` to the list's exact casing (so chip highlighting, which compares with `==`, is guaranteed to light up).
   - If no match (brand vanished after a re-sync, or a malformed extra), reset `selectedManufacturer` to `"All"` — the spec's required fallback, no crash, no phantom empty list.
4. Nothing else changes: chip taps (`Phonelistactivity.kt:355`), price-list copy, brand-new/refurbished and price filters, merged view, comparison flow — all read the same `selectedManufacturer` state and keep today's behavior, which is exactly the spec's "chips remain fully interactive" requirement.

### Step 4 — Verification (manual)

| Action | Expected |
|---|---|
| Tap PHONES on home screen | Category buttons replaced in place by phone-brand buttons (priority order: Apple, Samsung, Infinix, Tecno, Xiaomi, then others A→Z); no "All" button; logo/sync/sign-out still visible |
| Tap TABLETS / LAPTOPS | Brand list contains only brands that actually have that device type in the synced data |
| Tap "Samsung" under Phones | PhoneListActivity opens already filtered to Samsung; "Samsung" chip highlighted in the top row |
| In the list, tap "Apple" chip, then "All" chip | List re-filters each time — existing behavior intact |
| Long-press behaviors / copy price list with pre-selected brand | Copies that brand's price list, as today |
| System back from brand list | Returns to the three category buttons, app does not close |
| System back from PhoneListActivity | Returns to the brand list; back again → category buttons |
| Double-tap a brand quickly | Only one PhoneListActivity instance opens |
| Category with no devices | "No <category> available" message + back, no crash |
| Launch list with a brand, force re-sync that removes it, reopen | Chip row falls back to "All" (verify via `onManufacturersLoaded` path — can simulate by passing a bogus `SELECTED_BRAND` in a debug launch) |
| Rotate-equivalent recreation (e.g. locale change) while brand view open | Brand view survives (`rememberSaveable`) |
| Buttons before sync completes on cold start | Category buttons disabled → brand view unreachable, as today |

## Files Touched

| File | Change |
|---|---|
| `MainActivity.kt` | `selectedCategory` state, in-place brand-list section, `BackHandler`, brand-list derivation from `PhoneListHolder` + `sortManufacturersWithPriority`, launch guard, `SELECTED_BRAND` extra |
| `Phonelistactivity.kt` | Read `SELECTED_BRAND` extra, seed `selectedManufacturer`, case-normalize/fallback in `onManufacturersLoaded` |

No new files. No changes to `Phonelistholder.kt`, sync, or Firestore.

## Risks & Mitigations

- **Chip highlight casing mismatch:** the chip row compares with `==` while filtering is case-insensitive; a brand extra with different casing would filter correctly but highlight nothing. Mitigated by normalizing to the loaded list's casing in Step 3.3.
- **Home-screen data vs. list data drift:** the brand list and the chip list are computed from the same `PhoneListHolder.allDevices` with the same exclusions and ordering, so they can only diverge if the data changes between tap and list load (e.g. the live inventory listener removes the last device of that brand). The Step 3.3 fallback covers exactly this.
- **Layout overflow with many brands:** handled by making the brand section scrollable (Step 2.2); the three-button layout today is fixed-height and would not cope.
- **State reset on recreation:** `rememberSaveable` keeps the brand view open across activity recreation; plain `remember` would snap back to category buttons.

## Out of Scope (per spec)

Chip-row behavior changes inside PhoneListActivity, sync/data-model/Firestore changes, brand logos, and persisting the last-chosen brand across launches.
