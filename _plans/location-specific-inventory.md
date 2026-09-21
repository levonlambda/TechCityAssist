# Plan: Location-Specific Inventory Indicator

- **Spec:** `_specs/location-specific-inventory.md`
- **Branch:** `feature-branch-specific-inventory`
- **Status:** Implemented (pending Firebase rules check — Step 0 — and manual verification, Step 8)
- **Created:** 2026-09-21

## Decisions carried over from spec review

- Firestore collection is **`accessory_locations`** (two "s"); each document has a **`name`** field holding the display name.
- Each `inventory` document has a **`location`** field (string, matched against the location `name`).
- **Default location** = the first document returned from `accessory_locations` (collection order, no sorting).
- Units with a **missing/blank `location`** count as "at another location" (they can trigger neon green).
- Neon green is tied to the **currently selected colour**, exactly like today's bar. No preferred shade yet; the owner may later restyle the indicator (e.g. two-tone blue/black), so the indicator is built as one shared composable to make that a single edit.
- Home-screen text is a **plain label with the location name** ("Location: Main Branch"). Tapping it does nothing.
- **Per-unit location is included in the synced snapshot** so cards show neon green from the first frame, before the live listener delivers.

## Approach Summary

The feature is three small pieces layered onto machinery that already exists:

1. **Location state.** A new `Locationmanager.kt` object (matching the existing `Authmanager` / `SyncDataManager` naming style) owns the list of locations, the selected location, persistence in `SharedPreferences`, the default rule, and the pure availability rule. The selected location is exposed as Compose `mutableStateOf`, so any composable that reads it recomposes when it changes — this is what makes the kebab picker update the cards, home text, and detail text without restarts or plumbing.

2. **Per-colour location data on the model.** `groupInventoryDocs` (`Inventoryrepository.kt:60`) is the single place inventory documents become `Phone` objects, used by both `syncAllData` (`MainActivity.kt:723`) and the list's live listener (`Phonelistactivity.kt:1020`). Adding one field, `colorLocations: Map<String, List<String>>` (colour → one location string per unit, blank for missing), to `Phone` and populating it there means the sync snapshot, the disk cache, and the live listener all carry location data with no duplicated parsing. `MergedVariant` gets the same field so the merged card can evaluate the rule per variant.

3. **Three-state bar + two labels.** The bar colour is computed in exactly two places — `Mergedphonecard.kt:510-522` and `Phonedetailactivity.kt:1026-1038` — and both already branch on "is this variant available in the selected colour". Each becomes a three-way branch: in stock here → colour fill (unchanged), in stock elsewhere only → neon green, nowhere → transparent (unchanged). The two label `Text`s are inserted under each logo.

No screens are added. Firestore reads are one extra `get()` on `accessory_locations` at app start (plus a refresh when the picker opens). The device list content, ordering, filters, and merge toggle are untouched.

## Implementation Steps

### Step 0 — Owner prep (no code)

1. Confirm the Firestore security rules allow signed-in reads on `accessory_locations` (the app's rules gate on `request.auth != null`; if rules are per-collection, add a read rule for this collection or the picker will be empty with a permission error).
2. Confirm at least one `accessory_locations` document exists with a non-empty `name`, and that inventory `location` values spell the names the same way (matching is case/whitespace-insensitive, but "Main Branch" vs "Main" would not match).

### Step 1 — `Locationmanager.kt` (new)

An `object LocationManager` with:

- **Persistence:** `SharedPreferences("location_prefs")`, key `selected_location`. `init(context)` loads the persisted value on app start; call it from `TechCityApplication.onCreate()` (`TechCityApplication.kt:6-11`) next to `Authmanager.startSessionGuard`.
- **Observable state:** `var selectedLocation by mutableStateOf("")` and `var locations by mutableStateOf<List<String>>(emptyList())`. Plain `object` + `mutableStateOf` is enough (same pattern as reading `PhoneListHolder.lastSyncTime` inside `remember` today); no ViewModel needed.
- **`suspend fun loadLocations(context)`**: `db.collection("accessory_locations").get().await()`, map each doc's `name` (skip blank), assign to `locations`, then apply the default rule:
  - if `selectedLocation` is blank → select `locations.first()` (if any) and persist;
  - if `selectedLocation` is set but no longer in `locations` (deleted in Firestore) → fall back to `locations.first()` and persist;
  - if the fetch fails or returns nothing → leave `selectedLocation` as-is (persisted value survives offline; blank stays blank → placeholder).
  Wrap Firestore errors through `Authmanager.handleFirestoreError` like the other listeners do, so a revoked account still routes to login.
- **`fun select(context, name)`**: sets `selectedLocation` and persists. Nothing else — filters/chips/merge toggle live in `MainScreen` state and are not touched (spec req. 15).
- **`fun displayLabel(): String`**: `"Location: $selectedLocation"` or `"Location: No location set"` when blank. Used by both labels so wording stays identical.
- **Availability rule (pure, unit-testable):**
  ```
  enum class LocationAvailability { HERE, ELSEWHERE, NONE }
  fun availability(unitLocations: List<String>?, selected: String = selectedLocation): LocationAvailability
  ```
  - `unitLocations == null || unitLocations.isEmpty()` → `NONE` (no unit of that variant in that colour anywhere).
  - `selected.isBlank()` → `HERE` (spec req. 6: no location set ⇒ never neon green).
  - any entry equals `selected` after `trim()` + `equals(ignoreCase = true)` → `HERE`.
  - otherwise → `ELSEWHERE` (this is where blank `location` values land, per the owner's decision).
- **Shared indicator:** `val LOCATION_ELSEWHERE_COLOR = Color(0xFF39FF14)` and a small composable `AvailabilityBar(state, fillColor, width, height)` that renders the existing rounded `Box` (fill + light border for `HERE`, neon green + border for `ELSEWHERE`, transparent for `NONE`). Both screens call this instead of their inline `Box`, so a later two-tone restyle is one edit.

### Step 2 — Carry per-unit location through the data model

1. **`Phone.kt`**: add `val colorLocations: Map<String, List<String>> = emptyMap()`. Default value keeps every existing constructor call (`Phonecomparisonactivity`, `mergedVariantToPhone`, JSON loader) compiling unchanged.
2. **`Inventoryrepository.kt`**:
   - `InventoryUnit` (`:44-53`) gains `location: String` read via `doc.getString("location") ?: ""` in the `mapNotNull` at `:65-76`.
   - In the group `map` (`:79-113`), build `colorLocations = items.filter { it.color.isNotEmpty() }.groupBy({ it.color }, { it.location.trim() })` and pass it to the `Phone(...)` constructor. Grouping key and ordering are untouched, so the list content and order are identical (spec req. 7).
   - Colour keys are stored as they appear in the data; lookups use the existing case-insensitive colour matching (see Step 4).
3. **`Syncdatamanager.kt`**:
   - `phoneToJson` (`:170-195`): add `put("colorLocations", JSONObject(colour → JSONArray(locations)))`.
   - `jsonToPhone` (`:198-224`): read it back with `optJSONObject("colorLocations")`; absent key → `emptyMap()`.
   - **Legacy cache handling:** a cache written by the previous build has no `colorLocations`. `availability()` would treat every colour as `NONE` and hide bars that should be filled. To avoid that, when a `Phone` has non-empty `colors` but an empty `colorLocations`, the bar code falls back to the current colour-only rule (filled if the colour is in `variant.colors`, else transparent). The live listener replaces the data within seconds, and the next daily sync writes the new schema. No cache-version bump needed.
4. **`Mergedphonecard.kt`**:
   - `MergedVariant` (`:43-50`) gains `colorLocations: Map<String, List<String>> = emptyMap()`; populate it in `groupPhonesForMergedView` (`:118-126`) from `phone.colorLocations`.
   - `mergedVariantToPhone` in `Phonelistactivity.kt` should copy it back so the `Phone` handed to detail navigation is complete (not strictly needed — the detail screen has its own listener — but keeps the objects consistent).

### Step 3 — Load locations at startup and after sync

1. **`MainActivity.kt` `HomeScreen`**: in the startup `LaunchedEffect(Unit)` (`:161-200`), after the local-data branch and before `onDataReady()`, call `LocationManager.loadLocations(context)` on `Dispatchers.IO`. It is a single small read; if it is slow or offline, the persisted value is already in place from `init`, so the splash is not held hostage — run it as a separate `LaunchedEffect(Unit)` so it does not delay `onDataReady()` at all.
2. **`syncAllData`** (`MainActivity.kt:685`): call `LocationManager.loadLocations(context)` as a final step so a manual SYNC also refreshes the location list. (Optional but cheap.)

### Step 4 — Three-state bar in the merged list card (`Mergedphonecard.kt`)

At `:510-522`, replace the two-way branch:

- Look up the unit locations for this variant in the current colour with a case-insensitive key match: `variant.colorLocations.entries.firstOrNull { it.key.equals(currentColorName, true) }?.value`.
- If `variant.colorLocations.isEmpty()` (legacy cache, Step 2.3) → state = `HERE` if `isAvailableInSelectedColor` else `NONE` (today's behaviour).
- Else → `state = LocationManager.availability(unitLocations)`. Reading `LocationManager.selectedLocation` inside the composable subscribes the card to location changes, so picking a new location in the kebab menu recomposes visible cards immediately (spec req. 3).
- Fill colour for `HERE` is computed exactly as today (`parseHexColorMerged(currentColorHex)` / `getColorFromName`).
- Replace the inline `Box` at `:578-590` with `AvailabilityBar(state, fillColor, COLOR_BAR_WIDTH, COLOR_BAR_HEIGHT)`.

Live updates (spec req. 10) come for free: the listener at `Phonelistactivity.kt:998-1027` rebuilds `phones` via `groupInventoryDocs`, which now includes `colorLocations`, and `mergedPhoneGroups` is derived from `phones` at `:913`.

The non-merged `PhoneCard` (`Phonelistactivity.kt:1862`) renders no availability bar and is left alone.

### Step 5 — Location picker in the kebab menu (`Phonelistactivity.kt`)

1. In `MainScreen` (`:323`), add `var showLocationDialog by remember { mutableStateOf(false) }`.
2. In the `DropdownMenu` (`:429`), before the `Image Management` item (`:601`), add a "Location" header (same style as the "Display Filters" / "View Mode" headers) and one `DropdownMenuItem` whose text is `LocationManager.displayLabel()`; `onClick` closes the menu and sets `showLocationDialog = true`. Shown for every device type (not only phones).
3. Render an `AlertDialog` when `showLocationDialog` is true:
   - On open, launch `LocationManager.loadLocations(context)` in `rememberCoroutineScope()` to refresh the list (cached/persisted values show instantly; refresh corrects them).
   - Body: a `Column` of rows with `RadioButton` + name for each entry in `LocationManager.locations`, current one selected. Tap → `LocationManager.select(context, name)` and dismiss.
   - Empty list → "No locations available" text; dismiss button only.
   - Colours follow the menu's palette (`0xFFFCF9F5` container, `0xFF333333` text).
4. No other `MainScreen` state is touched, so `selectedManufacturer`, `filters`, `useMergedView`, and the `LazyColumn` scroll state survive a location change (spec req. 15).

### Step 6 — Home screen label (`MainActivity.kt`)

Between the logo `Image` (`:237-242`) and the weighted `Column` (`:253`): replace `Spacer(80.dp)` with `Spacer(24.dp)` + `Text(LocationManager.displayLabel(), fontSize = 14.sp, color = 0xFF666666)` + `Spacer(40.dp)`, keeping the total height close to the current 80 dp so the logo and buttons do not visibly move. Because the label sits **above** the `if (category == null)` branch, it stays in place when the brand buttons replace the category buttons (spec req. 12). Reading `LocationManager.selectedLocation` makes it recompose after a change made in the list's kebab menu (spec req. 14).

### Step 7 — Detail screen: label + three-state bar (`Phonedetailactivity.kt`)

1. **Label:** after the logo `Row` (`:582-595`) and before `Spacer(layoutConfig.logoToModelSpacing)` (`:598`), add a centred `Text(LocationManager.displayLabel())` with a small top padding, sized off `layoutConfig` (e.g. `colorNameFontSize`) so it scales with the tablet/phone configs. Reduce `logoToModelSpacing` usage slightly if the model name visibly shifts.
2. **Per-variant location data:** in the listener loop (`:484-515`), alongside `variantColorsTemp`, build `variantColorLocationsTemp: MutableMap<String, MutableMap<String, MutableList<String>>>` keyed `"$ram|$storage"` → colour → locations, reading `doc.getString("location") ?: ""` (trimmed). Store it in a new `var variantColorLocationsMap by remember(phone.phoneDocId) { mutableStateOf(...) }`.
   - Error fallback (`:453-462`) seeds it from `phone.colorLocations` (the intent-time `Phone` now carries it) so the bar still has a best guess offline.
3. **Bar:** at `:1026-1038`, mirror Step 4: look up `variantColorLocationsMap[variantKey]` → current colour (case-insensitive) → `LocationManager.availability(...)`; `HERE` keeps today's fill computation; render via `AvailabilityBar(state, fillColor, layoutConfig.colorBarWidth, layoutConfig.colorBarHeight)` replacing the inline `Box` at `:1109-1121`.

The list card and the detail screen now compute the state from the same rule and the same field name, so they cannot disagree for the same device/colour/location (spec acceptance item 6).

### Step 8 — Manual verification

Run on the tablet and a phone:

1. Fresh install → home shows "Location: <first collection entry>" without any interaction; kill and relaunch → same value.
2. Device list → kebab → Location → picker lists all `accessory_locations` names, current one selected; pick another → dialog closes, cards' bars change where expected, brand chip / price filters / merge toggle unchanged.
3. Back to home → label updated. Open a detail page → label under logo matches; variant bars match the list card for the same colour.
4. Bar states: choose a device with units at two locations; verify colour fill at the stocked location, neon green after switching to the other location, transparent for a colour with no units.
5. In the Firestore console mark the last unit of a variant at the current location as `Sold` while the list and detail are open → bar flips to neon green (units elsewhere) or transparent (none), without leaving the screen.
6. Airplane mode with a persisted location → app opens, label shows the persisted name, picker shows cached or empty state, no crash.
7. Clear app data, airplane mode → label shows "No location set", no neon green anywhere.
8. Confirm the list shows the same devices in the same order as before the branch for each category.

## Files Touched

| File | Change |
|---|---|
| `Locationmanager.kt` (new) | Location state, persistence, default rule, availability rule, shared `AvailabilityBar` |
| `TechCityApplication.kt` | `LocationManager.init(this)` |
| `Phone.kt` | `colorLocations` field |
| `Inventoryrepository.kt` | Read `location`, build `colorLocations` |
| `Syncdatamanager.kt` | Serialize/deserialize `colorLocations` |
| `Mergedphonecard.kt` | `MergedVariant.colorLocations`, three-state bar |
| `Phonelistactivity.kt` | Kebab "Location" entry + picker dialog, `mergedVariantToPhone` copy |
| `MainActivity.kt` | Load locations at startup/sync, home label |
| `Phonedetailactivity.kt` | Detail label, per-variant location map, three-state bar |

## Risks and Notes

- **Neon green legibility:** `#39FF14` on the white card is bright but thin (5–8 dp wide). If it reads poorly on the tablet, the shared `AvailabilityBar` is the single place to swap in the owner's two-tone idea.
- **Colour key casing:** `colorLocations` keys come straight from inventory data; lookups are case-insensitive to match how colours are matched everywhere else (`equals(ignoreCase = true)`).
- **Stale cache first launch:** the first launch after installing this build may load a same-day cache without `colorLocations`; the fallback in Step 2.3 keeps today's look until the listener or next sync replaces it.
- **`accessory_locations` order:** "first document" means Firestore's default document-ID ordering. If the owner wants a specific default, add an `order` field later and sort on it in `loadLocations` — a one-line change.
- **Comparison screen:** untouched; `Phone`'s new field has a default so its constructor calls compile.
