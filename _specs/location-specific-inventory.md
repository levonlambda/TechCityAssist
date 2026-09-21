# Location-Specific Inventory Indicator

- **Slug:** `location-specific-inventory`
- **Branch:** `feature-branch-specific-inventory`
- **Status:** Draft
- **Created:** 2026-09-21

## Overview

TechCity operates from more than one store location, but the app currently treats all inventory as a single pool. Staff at a given branch cannot tell at a glance whether a device variant is physically on hand where they are, or only stocked at another branch.

This feature lets the user choose which store location the app is "set to" (sourced from the `accesory_locations` Firestore collection) through the kebab menu, remembers that choice, and surfaces it in the UI. The device list and device detail screens keep showing every item regardless of location, but the existing colour availability bar next to each RAM/storage row gains a new state: a neon green bar means "this variant exists, but only at a location other than the one the app is set to". The chosen location name is also displayed on the home screen (under the TechCity logo, above the category and brand buttons) and on the device detail screen (under the TechCity logo).

## Current Behavior

- **Location concept:** none. The app never reads the `accesory_locations` collection, and no location field on inventory documents is read or stored. All inventory documents in the `inventory` collection are grouped into `Phone` entries purely by manufacturer, model, RAM, and storage.
- **Home screen (MainActivity):** shows the round TechCity logo, then (in place) either the Phones / Tablets / Laptops category buttons or the brand buttons for the tapped category. There is no top app bar and no kebab menu on this screen. Nothing indicates a store location.
- **Device list (PhoneListActivity):** has a top app bar with a kebab (three-dot) menu containing Brand New / Refurbished toggles, min/max price filters, Image Management, Clear Image Cache, and Merge Variants. Each merged card lists variants as RAM chip, Storage chip, a thin vertical colour availability bar, then the price. The bar is filled with the currently selected colour's hex value when that variant is available in the selected colour, and is transparent (invisible) otherwise.
- **Device detail (PhoneDetailActivity):** shows the flat TechCity logo at the top, then the model name, images, colour dots, and a list of variant rows. Each variant row uses the same RAM chip / Storage chip / colour availability bar / price layout, with the same fill-or-transparent rule as the list card. There is no location text.
- **Live inventory:** both the list and the detail screen keep their variant and colour data in sync with Firestore via listeners, so units that are sold disappear from the rows automatically.

## Goals

- Let the user pick the store location the app is set to, from the locations stored in the `accesory_locations` collection, via the kebab menu.
- Persist the chosen location on the device so it survives app restarts, and assign a sensible default when none has been chosen yet.
- Keep the device list and detail screens showing every device regardless of location (no filtering).
- Extend the colour availability bar on both the device list cards and the detail screen so it can show neon green when a variant is only stocked at other locations.
- Display the currently set location as text on the home screen (below the logo, above the category buttons, and still visible while the brand buttons are shown) and on the detail screen (below the logo).

## Non-Goals

- No filtering, hiding, or re-ordering of devices based on location; the list content stays exactly as it is today.
- No per-location stock counts or quantities in the UI.
- No changes to the sync flow, price filters, refurbished/brand-new toggles, comparison, or image management features.
- No management (add / edit / delete) of locations from within the app; the `accesory_locations` collection is read-only for this feature.
- No location text on the device list screen itself (only the home and detail screens are in scope).
- No changes to how the selected colour is chosen or how the colour dots behave.

## Functional Requirements

1. The app must read the list of store locations from the `accesory_locations` Firestore collection and expose them for selection.
2. The kebab menu in the device list top app bar must contain a "Location" entry that opens a chooser listing every location from `accesory_locations`, with the currently set location clearly marked as selected.
3. Selecting a location from the chooser must save it as the app's current location and immediately update every screen that shows the location text or the neon green indicator, without requiring an app restart.
4. The chosen location must be persisted locally on the device so it is restored on the next app launch.
5. If no location has been chosen yet (first launch or after a fresh install), the app must automatically assign a default location so that the location is never empty. The default must be a real entry from `accesory_locations` (see Open Questions for which one).
6. If the `accesory_locations` collection cannot be read (offline, permission error) and no location is persisted, the app must still function, showing a neutral placeholder such as "No location set" wherever the location text appears, and treating every variant as if it were at the selected location (no neon green shown).
7. The device list must continue to display all devices for the chosen device type regardless of their location; adding a location must not remove, hide, or reorder any item.
8. For each variant row on a device list card, the colour availability bar must follow these rules, evaluated for the currently selected colour on that card:
   - If at least one unit of that variant in the selected colour is stocked at the app's current location: fill the bar with the selected colour's hex value (existing behaviour).
   - If no unit of that variant in the selected colour is stocked at the current location, but at least one such unit is stocked at a different location: fill the bar with neon green.
   - If no unit of that variant exists in the selected colour at any location: leave the bar transparent (existing behaviour).
9. The device detail screen variant rows must apply exactly the same three-state rule as requirement 8, using the detail screen's currently selected colour.
10. The neon green state must remain accurate as the live inventory listeners update, so that a unit sold at the current location flips the bar to neon green (if other locations still have it) or transparent (if none do) without leaving the screen.
11. The home screen must display the current location as text positioned below the TechCity logo and above the category buttons.
12. The home screen location text must remain visible, in the same position, when the user taps Phones, Tablets, or Laptops and the brand buttons replace the category buttons.
13. The device detail screen must display the current location as text positioned directly below the TechCity logo and above the model name.
14. The location text on both screens must reflect the persisted location on open and update immediately if the location is changed elsewhere in the app and the user returns to that screen.
15. Changing the location must not reset any other user state on the device list screen (selected brand chip, price filters, merge toggle, scroll position).

## User Experience

- **Home screen:** the user sees the TechCity logo, then a small, unobtrusive line of text such as "Location: Main Branch" beneath it, then the Phones / Tablets / Laptops buttons. Tapping a category swaps the buttons for brand buttons; the location line stays exactly where it was. The text style should match the home screen's existing muted, warm off-white aesthetic and must not push the logo or buttons noticeably.
- **Device list, kebab menu:** the user opens the kebab menu and sees a new "Location" entry (with the current location shown alongside it, e.g. "Location: Main Branch"). Tapping it opens a simple picker listing all locations. The current one is highlighted. Tapping another location closes the picker, and the cards visibly update: variants that were only stocked elsewhere now show a neon green bar.
- **Device list cards:** the vertical bar after the RAM and Storage chips now has three possible looks: the selected colour (in stock here), neon green (in stock elsewhere only), or invisible (not in stock in that colour anywhere). Neon green should be immediately distinguishable from any real device colour, including the greens that exist in the product catalogue, and should carry a light border like the filled state so it reads as a deliberate indicator rather than a rendering glitch.
- **Device detail screen:** beneath the flat TechCity logo the user sees the same location text as on the home screen. The variant rows below use the identical three-state bar behaviour as the list, so the two screens never disagree.
- **First launch:** the user never has to pick a location before using the app; a default is already set and shown. They only visit the kebab menu if they want to change it.

## Edge Cases

- **Empty `accesory_locations` collection:** the picker shows an empty-state message ("No locations available"), the location text shows the neutral placeholder, and no neon green is displayed anywhere.
- **Persisted location later deleted from Firestore:** the app should fall back to the default location (or the placeholder if none can be determined) rather than keep showing a location that no longer exists.
- **Inventory documents with no location value:** treat such units as belonging to no location. They must still count toward "exists somewhere" so the variant is not hidden, but they must not trigger neon green on their own unless the product decision in Open Questions says otherwise.
- **Location name casing / whitespace differences:** matching an inventory unit's location to the selected location must be tolerant of case and surrounding whitespace so that a data-entry inconsistency does not produce a false neon green.
- **Variant stocked at both the current location and elsewhere:** shows the selected colour (in stock here); the presence of units elsewhere is irrelevant.
- **Variant stocked at the current location in a different colour only:** the selected-colour rule wins, so it shows neon green if that colour exists elsewhere, or transparent if it exists nowhere.
- **Location changed while the device list or detail screen is open in the back stack:** on returning to those screens, the bars and location text must reflect the new location.
- **Offline after a location was persisted:** the persisted location is still used; the picker may show cached locations or a loading/error state, but the rest of the app is unaffected.
- **Merge Variants toggled off:** the non-merged card layout (which does not currently render a colour availability bar) should remain unchanged; the neon green rule applies wherever the bar is rendered.
- **Comparison screen:** out of scope; it should keep whatever it displays today and not crash if location data is absent.

## Acceptance Criteria

- [ ] The kebab menu on the device list screen has a "Location" entry that opens a picker populated from the `accesory_locations` collection, with the current location marked.
- [ ] Choosing a location persists it, and the choice survives closing and reopening the app.
- [ ] On a fresh install, a default location is set automatically and displayed without user action.
- [ ] The device list still shows every device it showed before the feature, in the same order, regardless of the selected location.
- [ ] On device list cards, a variant in the selected colour stocked at the current location shows the colour-filled bar; stocked only elsewhere shows a neon green bar; stocked nowhere in that colour shows no bar.
- [ ] The device detail screen variant rows show the identical three-state bar for the same device, colour, and location.
- [ ] Selling (removing) the last unit at the current location causes the bar to change to neon green or transparent live, without leaving the screen.
- [ ] The home screen shows the current location text below the logo and above the category buttons.
- [ ] The location text on the home screen stays visible and in place when brand buttons replace the category buttons.
- [ ] The device detail screen shows the current location text below the TechCity logo.
- [ ] Changing the location updates the home screen text, detail screen text, and bar colours the next time those screens are shown, without an app restart.
- [ ] When the locations collection is unreadable and nothing is persisted, the app shows a neutral placeholder and no neon green, and does not crash.
- [ ] Changing the location does not reset the selected brand chip, price filters, or Merge Variants toggle on the device list.

## Open Questions

- **Collection name spelling:** the request says `accesory_locations` (one "s"). Confirm the exact Firestore collection name before implementation, since a mismatch would yield an empty picker. -  accessory_locations is the collection name
- **Location document shape:** which field on an `accesory_locations` document holds the display name (and is there a separate identifier or ordering field)? Should the picker sort locations alphabetically or by a stored order? - name field
- **Inventory location field:** the `inventory` documents are not currently read for any location value. Confirm the field name on each inventory unit that identifies its location, and whether it stores the location's display name or a document ID / reference. -inventory document contains a field called location
- **Default location rule:** when nothing is persisted, should the default be the first location in the collection, a location explicitly flagged as default in Firestore, or a hardcoded name (e.g. the main branch)? - just show first location in the collection
- **Units with a missing location value:** should they be treated as "at the current location" (never neon green), "at another location" (neon green), or ignored for the location rule as this spec assumes? - at another location
- **Bar semantics:** this spec ties the neon green state to the currently selected colour, matching the existing bar behaviour. Should neon green instead ignore colour and mean "this RAM/storage variant exists in any colour at another location but not here"? - this spec ties the neon green state to the currently selected colour, matching the existing bar behaviour.
- **Exact neon green value:** is there a preferred shade (e.g. #39FF14) so it stays distinguishable from catalogue greens? - no prefferred shade for now but i might change the color value later on to something more disticnt like half of the upper bar is blue and the lower half is black to make it distinct 
- **Home screen location text style:** plain label ("Location: Main Branch") or just the location name? Should tapping it also open the location picker, given the home screen has no kebab menu? - plain label showing the name field 
- **Sync data cache:** the home screen's category buttons depend on locally synced device data. Should the per-unit location be included in that synced snapshot so the list can show neon green before the live listener attaches, or is it acceptable for the bars to settle once the listener delivers data? - the per-unit location be included in that synced snapshot so the list can show neon green before the live listener attaches
