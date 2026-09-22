# Responsive UI for Lower Resolution Devices

- **Slug:** `responsive-ui-lower-resolution`
- **Branch:** `bug-fix-ui-for-lower-resolution-devices`
- **Status:** Implemented (pending on-device verification, see `_plans/responsive-ui-lower-resolution.md`)
- **Created:** 2026-09-22

## Overview

The app currently looks correct on the two tablets it was tuned on (a 601 x 1007 dp tablet and a larger 824 x 1318 dp tablet), but on an 11-inch tablet with a 1200 x 1920 pixel display several elements no longer fit: the device image is cropped at its left and right edges on the phone list, phone detail and phone comparison screens, and on the phone detail screen the RAM/Storage/Price variant section only has room for three rows, so a fourth variant is cut off.

This feature makes those three screens fit correctly on 11-inch-and-smaller tablets and on lower resolution displays, without altering the layout on the larger, higher resolution tablets that already work perfectly.

## Current Behavior

- All screens are locked to portrait orientation.
- The phone detail and phone comparison screens pick one of two fixed layout presets based on screen width: a "standard" preset for widths up to 650 dp (tuned for the 601 dp tablet) and a "large" preset for anything wider (tuned for the 824 dp tablet). Each preset hard-codes image heights, specs column heights, chip sizes, font sizes and spacing.
- The phone list screen computes card dimensions from screen width with an 800 dp cutoff (a taller card and smaller paddings below 800 dp, a shorter card and wider paddings above it).
- On every screen the device image is rendered to fill the height of a fixed-height container, then scaled up an additional 15% and clipped to that container. On the 11-inch 1200 x 1920 tablet the container is not wide enough for the resulting image, so the left and right edges of the device are cut off.
- On the phone detail screen the variant section (one row per RAM/Storage/Price combination) sits under the image/specs area and takes whatever vertical space is left. On the 11-inch tablet the fixed image and specs column heights from the "large" preset consume too much of the screen, so only three variant rows fit and any fourth variant is pushed off-screen with no way to reach it.
- The 11-inch 1200 x 1920 tablet falls into the "large" bucket for the detail and comparison screens because its width is above 650 dp, even though its usable area is noticeably smaller than the 824 x 1318 dp tablet that preset was designed for.

## Goals

- Device images on the phone list, phone detail and phone comparison screens are fully visible (no edge cropping) on 11-inch-and-smaller tablets and on lower resolution displays, including the 1200 x 1920 tablet that surfaced the problem.
- The RAM/Storage/Price variant section on the phone detail screen shows every variant a device has (four or more) on these devices, without any row being cut off.
- The higher resolution tablets that currently render perfectly (in particular the 824 x 1318 dp tablet) keep their existing layouts pixel-for-pixel: same image sizes, same spacing, same fonts, same variant rows.
- The existing 601 x 1007 dp tablet layout ("standard" preset) is likewise preserved unchanged.
- Any adjustments are driven by the actual available screen size so that other tablets in the same size class benefit, not just the one model that was tested.

## Non-Goals

- No redesign of the three screens. Visual style, colours, ordering of elements and interaction behaviour (image zoom, colour selection, comparison marking, long-press actions) stay as they are.
- No changes to landscape support; the app remains portrait-only.
- No changes to how images are downloaded, cached or chosen (high-res vs low-res).
- No changes to the data model, Firestore queries, sync or variant grouping logic.
- No changes to phone-sized (non-tablet) layouts beyond what naturally follows from fixing the smaller-tablet case; phones are not a target of this work.
- No changes to other screens (home, login, image management, admin).

## Functional Requirements

1. On the phone list screen, each device card must display the complete device image, with both left and right edges visible, on the 11-inch 1200 x 1920 tablet and on any tablet whose usable width or height is smaller than the 824 x 1318 dp reference tablet.
2. On the phone detail screen, the device image (in its normal, non-zoomed state) must be fully visible with no edge cropping on the same class of devices.
3. On the phone detail screen, when the image is zoomed, the zoomed image must also remain fully visible within its area on these devices.
4. On the phone comparison screen, both compared devices' images must be fully visible with no edge cropping on the same class of devices.
5. On the phone detail screen, the variant section must display all variants for the device on these devices. With four variants, all four rows must be visible and readable without any row being clipped. If a device has more variants than can reasonably fit even after adjustment, the section must allow the user to reach the remaining rows (for example by scrolling that section) rather than silently hiding them.
6. Sizing adjustments must be selected from the available screen dimensions (width and/or height in dp), not from a hard-coded device model or name.
7. Devices whose screen dimensions match or exceed the 824 x 1318 dp reference tablet must continue to receive exactly the current "large" layout values. No numeric value, spacing or font used by that layout may change.
8. Devices that currently receive the "standard" layout (width up to 650 dp) must continue to receive exactly the current "standard" layout values.
9. Adjustments for the in-between size class must keep the same visual hierarchy as the large layout (same element order, same proportions between image and specs, same chip style) so that the screens look like the same design at a smaller scale, not a different design.
10. Text in the adjusted layouts must remain legible: spec labels, spec values, chip text and prices must not be shrunk below the sizes currently used by the "standard" layout.
11. The phone list card height and the image container on the card must be sized so that the image fits inside the card without clipping and without overlapping the RAM/Storage/Price area of the card.
12. Existing performance characteristics of the phone list (layout values computed once per screen size rather than per card) must be preserved.

## User Experience

- **Phone list:** On the 11-inch tablet the user scrolls through device cards and sees the whole phone in each card, including the rounded edges and side buttons that are currently cut off. Cards keep the same look and the same RAM/Storage/Price row beneath the image. On the larger tablet nothing changes.
- **Phone detail:** On the 11-inch tablet the user sees the full device image on the left, the specs column on the right, and beneath them every RAM/Storage/Price variant row, including the fourth one for devices that have four variants. Tapping the image still zooms it and hides the variant section, and tapping again restores the layout. On the larger tablet the screen is identical to today.
- **Phone comparison:** On the 11-inch tablet both device images are shown in full side by side with the spec comparison beneath them. On the larger tablet nothing changes.
- The transition between "smaller tablet" and "larger tablet" layouts is invisible to the user; each device simply shows a layout that fits.

## Edge Cases

- **Device exactly at the boundary:** a tablet whose dimensions equal the 824 x 1318 dp reference must receive the unchanged large layout. Only devices that are smaller in the relevant dimension receive adjustments.
- **Width qualifies as large but height is short:** the 1200 x 1920 tablet is wide enough for the large preset but not tall enough for its fixed heights. The size classification must account for height (or overall usable area), not width alone, so this device is treated as a smaller tablet.
- **Very tall/narrow or very wide/short aspect ratios:** images must remain uncropped and the variant section must remain reachable regardless of aspect ratio within the tablet range.
- **Devices with one or two variants:** the variant section must not look sparse or misaligned on the adjusted layout; rows should keep the same alignment and spacing as today.
- **Devices with five or more variants:** all rows must still be reachable (see requirement 5); nothing may be silently hidden.
- **Images with unusual aspect ratios (very wide or very tall product shots):** the image must still be fully contained in its area; letterboxing inside the container is acceptable, cropping is not.
- **"No Image" placeholder:** the placeholder text must remain centred in the adjusted image area.
- **System font scaling:** with the user's system font size increased, the adjusted layouts must not push the fourth variant row off-screen; the section must still be reachable.
- **Display size setting changed on the device:** Android's display size setting changes the reported dp dimensions. The chosen layout must follow the reported dp values so the app adapts consistently.
- **Rotation:** the app is portrait-only, so no landscape variant is required, but the layout choice must be re-evaluated if the reported configuration changes (for example on a foldable or after a display size change).

## Acceptance Criteria

- [ ] On an 11-inch 1200 x 1920 tablet, every device card on the phone list shows the full device image with no left/right cropping.
- [ ] On the same tablet, the phone detail screen shows the full device image with no left/right cropping in both normal and zoomed states.
- [ ] On the same tablet, the phone detail screen shows all four variant rows for a device with four RAM/Storage combinations, fully visible and readable.
- [ ] On the same tablet, the phone comparison screen shows both device images with no left/right cropping.
- [ ] On the 824 x 1318 dp tablet, screenshots of the phone list, phone detail and phone comparison screens taken before and after the change are identical.
- [ ] On the 601 x 1007 dp tablet, screenshots of the same three screens taken before and after the change are identical.
- [ ] The layout choice is derived from screen dimensions only; no device model, manufacturer or build property is consulted.
- [ ] Text sizes in the adjusted layout are never smaller than those of the current standard layout.
- [ ] Phone list scrolling performance is unchanged (layout values are still computed once per screen configuration, not per card).
- [ ] A device with five or more variants can still reach every variant row on the 11-inch tablet.

## Open Questions

1. What are the exact dp dimensions reported by the 11-inch 1200 x 1920 tablet (they depend on its density bucket)? These values are needed to place the size-class threshold safely below the 824 x 1318 dp reference tablet. - 257 x 168.6 x 7.6 mm (10.12 x 6.64 x 0.30 in) these are the dimension
2. Are there other tablet models in use in the shops (screen size and resolution) that should be verified against the adjusted layout before release? - for now no. 
3. For devices with more variants than fit even after adjustment, is a scrollable variant section acceptable, or should the image/specs area shrink further to keep all variants on screen at once? - try to make 4 variants fit. if there are more than 4 then scrollable variant section is fine
4. Should the 15% image enlargement currently applied on all screens be retained on the smaller tablets (with a correspondingly wider container), or is a non-enlarged image acceptable there as long as it fits? -a non-enlarged image is acceptable as long as it fits (but again dont change the behavior on working sizes. I want you to focus on the 11 inch tablets)
