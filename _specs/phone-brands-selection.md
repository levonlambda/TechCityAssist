 Phone Brands Selection

- **Slug:** `phone-brands-selection`
- **Branch:** `feature-phone-brands-selection`
- **Status:** Draft
- **Created:** 2026-09-20

## Overview

When the user taps one of the device category buttons on the home screen (Phones, Tablets, Laptops), the app should present a set of brand buttons for that category instead of jumping straight into the full device list. Tapping a brand opens the device list (PhoneListActivity) with that brand already selected in the manufacturer filter row at the top of the screen, so the list is pre-filtered to the chosen brand. The filter row remains fully interactive — the user can switch to a different brand (or "All") at any time and the list updates accordingly.

This gives staff a faster path to the devices they are usually asked about ("show me Samsung phones") without scrolling or filtering after the list loads.

## Current Behavior

- The home screen (MainActivity) shows three category buttons: Phones, Tablets, Laptops. Each one immediately launches PhoneListActivity with a `DEVICE_TYPE` intent extra (`"phone"`, `"tablet"`, or `"laptop"`).
- PhoneListActivity shows a horizontally scrollable row of manufacturer FilterChips at the top. The list of manufacturers is built dynamically from the synced device data for the current device type, sorted with a priority order (Apple, Samsung, Infinix, Tecno, Xiaomi first, then the rest alphabetically), with an "All" chip prepended.
- The manufacturer filter always starts on "All", so the user always lands on the unfiltered list and must tap a chip to narrow it down.
- Category buttons are disabled until device data has been synced (locally cached or freshly fetched).

## Goals

- Show a brand selection step after the user taps Phones, Tablets, or Laptops on the home screen.
- The brands offered must match the brands that actually exist in the synced data for the chosen device type (the same set that would appear as filter chips in PhoneListActivity), in the same priority-sorted order.
- Tapping a brand opens PhoneListActivity with the chosen brand pre-selected in the top filter chip row and the list already filtered to that brand.
- The top filter chip row in PhoneListActivity keeps its existing behavior: the user can tap any other chip to change the brand and the list updates immediately.
- Provide a way to back out of the brand selection without opening the list (e.g. returning to the category buttons).

## Non-Goals

- No changes to how the manufacturer filter chips behave once inside PhoneListActivity (selection, long-press price-list copy, refurbished/brand-new filters, price filters).
- No changes to the sync flow, data model, or Firestore queries.
- No brand logos or imagery — text buttons consistent with the existing home screen style are sufficient.
- No persistence of the last-chosen brand between app launches.

## Functional Requirements

1. Tapping Phones, Tablets, or Laptops on the home screen shows a brand selection view for that category instead of opening PhoneListActivity directly.
2. The brand selection lists every manufacturer present in the synced data for the selected device type, excluding the internal "techcity" manufacturer, ordered by the existing priority rules (priority brands first, remainder alphabetical).
3. Tapping a brand launches PhoneListActivity with both the device type and the selected brand passed along (e.g. via intent extras).
4. On open, PhoneListActivity shows the passed brand as the selected chip in the top manufacturer filter row, and the device list is filtered to that brand.
5. If the passed brand is missing or does not match any chip for that device type, PhoneListActivity falls back to "All".
6. After opening with a pre-selected brand, tapping any other chip in the filter row changes the selection and re-filters the list, exactly as it does today.
7. The user can navigate back from the brand selection to the category buttons, and back from PhoneListActivity to wherever they came from, without the app closing unexpectedly.
8. Brand selection is only reachable when data is synced (same enablement rule as the current category buttons).

## User Experience

- Home screen: the user taps a category button (e.g. "PHONES"). Instead of the list opening, they see a set of brand buttons — "Apple", "Samsung", "Infinix", "Tecno", "Xiaomi", and any other brands present in the data — styled consistently with the existing white rounded home buttons.
- The user taps a brand (e.g. "Samsung"). PhoneListActivity opens showing only Samsung devices, with the "Samsung" chip highlighted in the filter row at the top.
- The user can immediately tap a different chip (e.g. "Apple" or "All") and the list re-filters — no need to go back to the home screen.
- Pressing back from the brand selection returns to the category buttons; the visual transition should feel like part of the home screen rather than a jarring new screen.

## Edge Cases

- **Brand with devices in one category only:** the brand list is per device type — a brand that only sells tablets must not appear when the user tapped Phones.
- **No brands / empty category:** if the synced data has no devices for the chosen category, show the "All" option (leading to the existing empty-list state in PhoneListActivity) rather than a blank brand screen.
- **Stale intent brand:** if a brand passed to PhoneListActivity no longer exists after a re-sync, fall back to "All" instead of showing an empty list with a phantom selection.
- **Many brands:** the brand list must remain usable if the data contains more brands than fit on screen (scrollable).
- **Rapid taps / double tap:** tapping a brand twice quickly must not open PhoneListActivity twice.

## Acceptance Criteria

- [ ] Tapping Phones, Tablets, or Laptops shows brand buttons instead of opening the list directly.
- [ ] Brand buttons reflect the actual manufacturers in the synced data for that device type, priority-sorted, with "All" and "techcity" excluded.
- [ ] Tapping a brand opens PhoneListActivity with that brand's chip selected and the list filtered to that brand.
- [ ] The filter chip row remains fully functional after opening with a pre-selected brand — changing chips updates the list.
- [ ] An unknown or missing brand extra falls back to "All" without crashing.
- [ ] Back navigation from the brand selection returns to the category buttons.
- [ ] Brand selection respects the existing "disabled until synced" rule.

## Open Questions

- Should the brand selection replace the category buttons in place on the home screen, appear as a bottom sheet/dialog, or be a separate screen? (In-place replacement on the home screen is the least disruptive to the current single-activity home design.) - lets try replacing the cattegory button in place of the home screen
- Should the brand list come from the synced data (dynamic, matches the chips exactly) or from the hardcoded `MANUFACTURER_FILTERS` list? The chips themselves are dynamic, so dynamic is recommended for consistency. - from the synced data
- Should "All" be visually distinguished (e.g. listed first, different styling) from the brand buttons? - no ALL button
