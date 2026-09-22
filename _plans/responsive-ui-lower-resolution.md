# Plan: Responsive UI for Lower Resolution Devices

- **Spec:** `_specs/responsive-ui-lower-resolution.md`
- **Branch:** `bug-fix-ui-for-lower-resolution-devices`
- **Status:** Implemented (code complete and compiling; pending Step 0 measurement on the 11-inch tablet to confirm `LARGE_MIN_HEIGHT_DP = 1300`, and Step 6 on-device verification)
- **Created:** 2026-09-22

## Revision after first on-device review (2026-09-22)

The owner reviewed the first build on the 11-inch tablet and asked for two changes, now applied:

1. **Images: back to the original rendering, 10% smaller.** The contained (`ContentScale.Fit`) image made the phones look too small. All three screens now use the exact original `FillHeight` + `graphicsLayer` 1.15-scale chain on every tier. On the medium tier the image is simply 10% smaller: detail `imageHeight` 522 → 470 dp (specs column 540 dp so it spans the image plus colour swatches), comparison `imageHeight` 625 → 562 dp, and list/merged cards fill 90% of the image slot height (`imageHeightFraction = 0.9f`, other tiers keep `fillMaxHeight()`). Because `FillHeight` scales width with height, the 10% reduction also pulls the edges back inside the container. The `fitImageInsideBox` flags described in Steps 2c/2d/3/4c below were replaced: the detail config keeps a `variantListScrollable` flag (medium only) and the card config has `imageHeightFraction`; the comparison config has no extra field. The image column stays 140 dp on all tiers.
2. **Home screen brand grid.** On the medium tier the two-column brand grid used the pill's natural aspect ratio (~103 dp tall at the capped row width), so five rows needed the vertical scroll. Medium tier now fixes each logo pill at 80 dp tall (centre-cropped, the same treatment the single-column pills already use), so 5 rows x 2 columns fit without scrolling. Other tiers are untouched. This is in `MainActivity.kt`, two-column branch of the brand list.

**Second tuning pass (2026-09-22):** some detail images still cropped, so the medium detail `imageHeight` went 470 → 446 dp (a further 5%, specs column 515 dp) and the medium comparison `imageHeight` went 562 → 478 dp (a further 15%). Medium tier only; standard and large presets untouched.

**Third tuning pass (2026-09-22):** medium list/merged card `imageHeightFraction` 0.9 → 0.855 (a further 5%) and medium comparison `imageHeight` 478 → 430 dp (a further 10%). Medium tier only.

**Fourth pass, new SMALL tier (2026-09-22):** tablets with a physical diagonal of 9" or less get their own tier, checked before the width/height rule. The diagonal comes from the system display metrics (pixels divided by the reported physical xdpi/ydpi, with a fallback to the logical density when a device reports an implausible value), so the rule is still dimension-based. The debug overlay on the home screen now prints the diagonal and the chosen tier so each tablet can be checked. Small-tier changes, all confined to `TabletTier.SMALL` branches and `copy()`-derived presets:
- Home screen: logo 280 → 210 dp (25% smaller). Two-column brand pills 64 dp tall with 12 dp row gaps; single-column pills 280 x 68 dp with 12 dp gaps, so five rows fit without scrolling.
- Phone list / merged cards: `imageHeightFraction = 0.85f` (15% smaller).
- Phone detail: standard preset copied with `imageHeight` 418 → 376 dp (10% smaller), `specsColumnHeight` 478 → 430 dp so four variant rows fit, and `variantListScrollable = true` for five or more.
- Phone comparison: standard preset copied with `imageHeight` 500 → 400 dp (20% smaller), then a further 8% to 368 dp after review.
Note: the "601 x 1007 dp" reference tablet in the code comments is very likely an 8.7" device (800 x 1340 px at 1.33x reports exactly 600 x 1007 dp), so it will now land in the SMALL tier by the owner's "9 inches or less" rule. Any tablet larger than 9" that reports ≤ 650 dp width keeps the untouched STANDARD preset.

## Approach Summary

All three affected screens already size themselves from a **layout config object chosen once per screen configuration**:

| Screen | Config type | Selector | Current rule |
|---|---|---|---|
| Phone detail | `DetailLayoutConfig` | `rememberDetailLayoutConfig()` (`Phonedetailactivity.kt:276`) | width > 650 dp → `createLargeLayoutConfig()`, else `createStandardLayoutConfig()` |
| Phone comparison | `ComparisonLayoutConfig` | `rememberComparisonLayoutConfig()` (`phonecomparisonactivity.kt:303`) | same 650 dp rule |
| Phone list (plain + merged cards) | `CardLayoutConfig` | inline `remember(screenWidthDp)` in `PhoneListScreen` (`Phonelistactivity.kt:1610-1651`) | width < 800 dp → 330 dp card + vertical spec layout, else 300 dp card |

The fix is therefore **not a redesign**: it is (a) adding a third, "medium tablet" tier to each selector that is picked from **both width and height**, (b) giving that tier its own scaled-down value set derived from the large preset, and (c) making the device image **fit** its container on that tier instead of filling the container's height and being clipped. The existing standard and large value sets are left byte-for-byte untouched, and the selector is written so that any device that qualifies for the large tier today *and* is at least as big as the 824 x 1318 dp reference tablet keeps getting the large tier.

**Why the image is cropped today.** On every screen the image is drawn with `ContentScale.FillHeight` inside a box with a *fixed height* and a *width that does not grow with it*, then enlarged a further 15% via `graphicsLayer` scale (detail and comparison), or placed in a fixed 140 dp wide column (list cards). Whenever the resulting image is wider than the box it is clipped left and right. On the 11-inch tablet the ratio of box height to box width is worse than on the 824 dp tablet, so the crop becomes visible. Per the owner's answer to spec open question 4, on the medium tier the image will simply be drawn **un-enlarged and contained** (`ContentScale.Fit` inside a box sized to the available area), which cannot crop regardless of the image's aspect ratio. The large and standard tiers keep `FillHeight` + 1.15 scale exactly as now.

**Why the fourth variant is cut off.** The detail screen is a non-scrolling `Column`. The variant section takes `weight(1f)`, i.e. whatever is left after the header (logo 60 dp + spacing 36 + model name + 48) and the fixed 598 dp specs column / 522 dp image. On a shorter screen that remainder is roughly three rows. The medium tier will (a) shrink the image and specs column heights so at least four rows fit, and (b) wrap the variant list in a `verticalScroll` so five or more rows remain reachable (owner's answer to open question 3).

**Tier selection rule (all three screens):**

```
standard : widthDp <= 650                                   (unchanged)
large    : widthDp > 650  AND  heightDp >= LARGE_MIN_HEIGHT  (unchanged values)
medium   : widthDp > 650  AND  heightDp <  LARGE_MIN_HEIGHT  (new)
```

`LARGE_MIN_HEIGHT` is a single shared constant. Its exact value is fixed in Step 0 from the measured dp height of the 11-inch tablet and the known 1318 dp height of the reference tablet; the working assumption is **1300 dp** (safely below 1318 so the reference tablet is unaffected, and above the ~1280 dp an 800 x 1280 dp / 1200 x 1920 px tablet would report). `screenHeightDp` from `LocalConfiguration` already excludes system bars, which is what we want.

The list screen has a slightly different base rule (800 dp width, not 650). The medium tier there is `widthDp >= 800 && heightDp < LARGE_MIN_HEIGHT` *or* `650 < widthDp < 800` (see Step 3) — either way only the image handling and card height change; the current `< 800` and `>= 800` value sets stay as they are for devices that don't fall in the new band. Step 0 decides which branch the 11-inch tablet actually hits.

## Implementation Steps

### Step 0 — Measure the 11-inch tablet (owner + 5-minute code change)

1. Flip `SHOW_DEBUG_INFO` (`MainActivity.kt:75`) to `true`, install on the 11-inch tablet, read the overlay in the top-right of the home screen: it prints `widthDp x heightDp` and the density multiplier (`DebugScreenInfoOverlay`, `MainActivity.kt:575`). Also open the phone detail and comparison screens once and read the `Using LARGE/STANDARD layout for screen width` lines from logcat (tags `PhoneDetail`, `PhoneComparison`).
2. Repeat on the 824 x 1318 dp tablet to confirm it still reports ≥ 1318 dp height (sanity check that the constant will not catch it).
3. Record both readings at the top of this plan, pick `LARGE_MIN_HEIGHT` (default 1300 dp), and set `SHOW_DEBUG_INFO` back to `false`.

Expected outcome for 1200 x 1920 px: density 1.5 → **800 x ~1232–1280 dp** (height minus status/navigation bars). If instead it reports ~900 x 1440 dp (density 1.33) the same rule still works because 1440 ≥ 1300 would be *wrong* — in that case the constant is raised to sit between the two tablets' heights, or the rule switches to the *area* (`width * height`) comparison. This is the only decision that depends on the measurement; everything below is written against tier names, not numbers.

### Step 1 — Shared tier helper (new file `Screensizeclass.kt`)

Small, dependency-free helper so the three screens agree on the rule and the constant lives in one place:

- `enum class TabletTier { STANDARD, MEDIUM, LARGE }`
- `const val LARGE_MIN_HEIGHT_DP = 1300` (value from Step 0)
- `const val LARGE_MIN_WIDTH_DP = 650` (existing threshold, moved here unchanged)
- `fun tabletTier(widthDp: Int, heightDp: Int): TabletTier` implementing the rule above.
- `@Composable fun rememberTabletTier(): TabletTier` reading `LocalConfiguration.current.screenWidthDp / screenHeightDp` and remembering on both. Because it keys on the configuration values, it re-evaluates automatically after a display-size change or on a foldable (spec edge case "Rotation / Display size setting").

No device model, manufacturer or build property is consulted (spec req. 6, acceptance criterion 7).

### Step 2 — Phone detail screen (`Phonedetailactivity.kt`)

**2a. Config selection.** `rememberDetailLayoutConfig()` (line 276) switches on `rememberTabletTier()` instead of width alone: `STANDARD → createStandardLayoutConfig()`, `LARGE → createLargeLayoutConfig()`, `MEDIUM → createMediumLayoutConfig()` (new). The two existing factory functions are not edited. Keep the existing `Log.d` lines and add one for MEDIUM.

**2b. New `createMediumLayoutConfig()`.** Values sit between standard and large, chosen so that on an 800 x ~1250 dp screen the header + image/specs block + four variant rows fit with margin. Starting values (to be tuned on-device in Step 6, but every value stays ≥ the standard preset per spec req. 10):

| Field | Standard | **Medium (proposed)** | Large |
|---|---|---|---|
| logoHeight | 40 | 48 | 60 |
| modelNameMax/Min | 42/24 | 46/26 | 54/30 |
| appleLogoSize | 56 | 60 | 72 |
| logoToModelSpacing | 24 | 24 | 36 |
| modelToContentSpacing | 32 | 24 | 48 |
| horizontalPadding | 24 | 28 | 32 |
| imageHeight | 418 | **420** | 522 |
| imageScale | 1.15 | **1.0** | 1.15 |
| specsColumnHeight | 478 | **470** | 598 |
| specIconSize / Large | 42/50 | 46/54 | 52/62 |
| specLabel/ValueFontSize | 14/15 | 15/16 | 17/19 |
| specRowSpacing | 14 | 12 | 18 |
| colorDot selected/unselected | 28/22 | 30/24 | 36/28 |
| colorNameFontSize | 14 | 15 | 17 |
| colorDotsSpacing | 10 | 12 | 14 |
| variantChipPaddingH/V | 12/10 | 14/8 | 16/14 |
| variantChipFontSize | 15 | 16 | 19 |
| ramChip/storageChipMinWidth | 110/140 | 125/155 | 140/175 |
| priceFontSize | 22 | 24 | 28 |
| priceEndPadding | 72 | 80 | 90 |
| colorBar W/H | 6/32 | 7/34 | 8/40 |
| variantRowSpacing | 8 | 8 | 12 |
| variantStartPadding | 32 | 36 | 40 |

Budget check on the assumed 800 x 1250 dp: top padding 50 + logo 48 + location text ~28 + 24 + model name ~56 + 24 + specs column 470 + 10 + four variant rows (chip ≈ 16 sp text + 2×8 pad ≈ 36 dp, +8 spacing → 4×44 = 176) ≈ **886 dp**, leaving ~360 dp headroom — enough that even with system font scaling four rows fit. The image (420 dp tall) sits inside the 470 dp specs-row height, matching the standard tier's 418-in-478 proportion, so the visual hierarchy is the same (spec req. 9).

**2c. Image fitting on the medium tier only.** Add one boolean to `DetailLayoutConfig`: `fitImageInsideBox: Boolean` (`false` for standard and large — the two existing factories get the field with `false`, which is the only edit they receive; `true` for medium). At the three `AsyncImage` call sites inside the image `Box` (lines ~700–715, ~775–795, and the image `Box` itself at 672–682):

- when `fitImageInsideBox` is true: modifier `fillMaxSize()` and `contentScale = ContentScale.Fit`, no `graphicsLayer` scale (scale stays 1.0 via `imageScale = 1.0f`, so the existing `effectiveImageScale` expression can remain and is a no-op);
- otherwise: the exact current modifier chain (`fillMaxHeight()` + `graphicsLayer { scale }` + `FillHeight`).

The `Box` keeps `.fillMaxWidth(0.85f)` and `.clip(RoundedCornerShape(8.dp))`; with `Fit` the image is letterboxed inside it, never clipped (spec edge case "Images with unusual aspect ratios").

**2d. Zoomed state.** When zoomed the image box takes `weight(1f)` and the image is scaled by `imageScale * 0.7` and translated up by a hard-coded `-225f`/`-200f` px (lines 708, 787) with the swatch box offset `-200.dp` (line 813). On the medium tier: `Fit` + scale 1.0 already keeps the zoomed image inside its box (spec req. 3). The pixel translations were tuned for the large tablet's density; on the medium tier they are applied as-is but verified in Step 6 — if the color swatches overlap the image, gate the `translationY`/`offset` on `!fitImageInsideBox` (i.e. only the two existing tiers keep the translation). This is confined to the medium branch so the working tiers are untouched.

**2e. Variant section scroll.** The inner `Column` that lists variants (line ~1049, `verticalArrangement = spacedBy(variantRowSpacing)`) gets `.verticalScroll(rememberScrollState())` **only when the tier is medium** (`Modifier.then(if (fitImageInsideBox) Modifier.verticalScroll(...) else Modifier)`). The outer `weight(1f)` column keeps `Arrangement.Center`, so 1–4 rows remain centred as today, and with five or more rows the section scrolls (spec req. 5, edge case "five or more variants"). Standard and large tiers get no scroll modifier, so their measurement and drawing are unchanged.

### Step 3 — Phone list screen (`Phonelistactivity.kt` + `Mergedphonecard.kt`)

**3a. Card config selection.** In `PhoneListScreen` (line 1610) also read `screenHeightDp` and compute `val tier = tabletTier(widthDp, heightDp)`; key the `remember` on both dimensions (still one computation per screen configuration, never per card — spec req. 12). Add two fields to `CardLayoutConfig` (line 212): `fitImageInsideBox: Boolean` and `imageColumnWidth: Dp`. Existing branches set `fitImageInsideBox = false` and `imageColumnWidth = 140.dp` (the literal that is currently hard-coded at `Phonelistactivity.kt:2391` and `Mergedphonecard.kt:613`), so nothing changes for them.

**3b. Medium branch values.** When `tier == MEDIUM`: keep whichever spec layout the width rule already picks (`useVerticalSpecLayout`/`cardHeight` unchanged, so text and spec rows look the same as they do now on that width), but set `fitImageInsideBox = true` and `imageColumnWidth = 160.dp`. The wider column plus `Fit` guarantees the full device image (including side buttons) is visible while the image still reads at nearly the same size (spec req. 1 and 11). If Step 0 shows the tablet reports exactly 800 dp width (so it currently gets the 300 dp card), no card-height change is needed; if it reports < 800 dp, the 330 dp card is kept as well — only the image column changes in both cases.

**3c. `PhoneCard` image column** (lines 2383–2395): replace `.width(140.dp)` with `.width(layoutConfig.imageColumnWidth)`. In `PhoneImageItem` (line 2455) pass `layoutConfig.fitImageInsideBox` and choose `fillMaxSize()` + `ContentScale.Fit` when true, else the current `fillMaxHeight()` + `FillHeight`. The `estimatedContentWidth` formula at line 1624 subtracts the literal `148.dp` for the image column; switch that to `imageColumnWidth + 8.dp` so chip font scaling on the medium tier accounts for the wider column (on the existing tiers the result is identical: 140 + 8 = 148).

**3d. `MergedPhoneCard`** (`Mergedphonecard.kt:613` and `:655–660`): same two substitutions — column width from `layoutConfig.imageColumnWidth`, and `Fit`/`fillMaxSize` when `layoutConfig.fitImageInsideBox`. `calculateMergedCardHeight` and the `imageHeight = cardHeight - 84.dp` rule stay as they are.

### Step 4 — Phone comparison screen (`phonecomparisonactivity.kt`)

**4a. Config selection.** `rememberComparisonLayoutConfig()` (line 303) switches on `rememberTabletTier()`; add `createMediumComparisonLayoutConfig()`; existing factories untouched except for a new `fitImageInsideBox = false` field.

**4b. Medium values** (between standard and large; image un-enlarged):

| Field | Standard | **Medium (proposed)** | Large |
|---|---|---|---|
| logoHeight | 36 | 44 | 54 |
| modelNameFontSize / appleLogoSize | 26 / 32 | 28 / 36 | 32 / 40 |
| topPadding / logoToModel / modelToContent / horizontalPadding | 50/16/16/16 | 50/20/20/20 | 60/24/24/24 |
| imageHeight | 500 | **520** | 625 |
| imageScale | 1.15 | **1.0** | 1.15 |
| imageYOffset | -40 | -40 | -50 |
| colorDot selected/unselected, colorNameFontSize | 24/18, 14 | 26/20, 15 | 30/22, 17 |
| specIconSize, label/value font, specRowPadding, arrowSize | 42, 14/15, 6, 32 | 46, 15/16, 7, 36 | 52, 17/19, 8, 40 |
| priceFontSize / priceBottomSpacing | 28 / 60 | 30 / 64 | 35 / 75 |
| left/right column paddings | 48/0/0/48 | 52/0/0/52 | 60/0/0/60 |

**4c. Image fitting.** In `PhoneImageView` (line ~934–960) the `Box` is `height(imageHeight).fillMaxWidth()`; the `AsyncImage` becomes `fillMaxSize()` + `ContentScale.Fit` with no `graphicsLayer` scale when `fitImageInsideBox` is true, otherwise the current chain. Each comparison column is half the screen width minus paddings (~340 dp on an 800 dp screen), which with `Fit` is always wide enough (spec req. 4).

### Step 5 — Guard rails for the working tiers

- The only edits to `createStandardLayoutConfig`, `createLargeLayoutConfig`, `createStandardComparisonLayoutConfig`, `createLargeComparisonLayoutConfig` and the two existing `CardLayoutConfig` branches are the added boolean/width fields set to their current implicit values (`false`, `140.dp`). No numeric literal in them changes (spec req. 7, 8).
- Every new modifier (`verticalScroll`, `Fit`, `fillMaxSize`, wider column) is wrapped in `if (fitImageInsideBox)`, so on the standard and large tiers the modifier chain is the same object sequence as today.
- The selector for the large tier is `width > 650 && height >= LARGE_MIN_HEIGHT`; the 824 x 1318 dp tablet satisfies both, so it cannot fall into the medium tier.

### Step 6 — Verification (manual, on real devices)

| # | Device | Screen | Check |
|---|---|---|---|
| 1 | 11-inch (1200 x 1920) | Home (debug overlay on) | Reported dp matches Step 0 note; overlay then turned off |
| 2 | 11-inch | Phone list, plain cards | Every image fully visible incl. edges; RAM/Storage/Price row unchanged; scroll smooth |
| 3 | 11-inch | Phone list, merged view | Same as #2 for merged cards with 1, 2 and 4 variants |
| 4 | 11-inch | Phone detail, 4-variant device | Image fully visible; specs column fully visible; all 4 rows visible without scrolling |
| 5 | 11-inch | Phone detail, ≥5-variant device (or temporarily seeded) | Rows 1–4 visible, section scrolls to reveal the rest |
| 6 | 11-inch | Phone detail, zoom | Tap image → zoomed image fully visible, swatches not overlapping; tap again restores |
| 7 | 11-inch | Phone detail, 1- and 2-variant device | Rows centred/aligned as before |
| 8 | 11-inch | Phone detail, system font size = Largest | 4 rows still reachable |
| 9 | 11-inch | Comparison | Both images fully visible; spec rows aligned across columns |
| 10 | 824 x 1318 dp tablet | List, detail (normal + zoom), comparison | Screenshots identical to screenshots taken on `main` before the change |
| 11 | 601 x 1007 dp tablet | Same three screens | Screenshots identical to `main` |
| 12 | Any | logcat | Detail/comparison log the expected tier; no per-card `LocalConfiguration` reads added |

Screenshots for #10/#11 are taken with the same build variant before and after, and compared visually side by side (a pixel-diff tool is optional). If any working-tier screenshot differs, that is a defect in Step 5's guard rails, not a tuning issue.

### Step 7 — Wrap-up

- Set `SHOW_DEBUG_INFO` back to `false` if it was flipped.
- Update the spec's status to Implemented and this plan's status accordingly.
- Commit on `bug-fix-ui-for-lower-resolution-devices`; open a PR to `main`.

## Files Touched

| File | Change |
|---|---|
| `app/src/main/java/com/techcity/techcityassist/Screensizeclass.kt` | **New.** `TabletTier`, thresholds, `tabletTier()`, `rememberTabletTier()` |
| `app/src/main/java/com/techcity/techcityassist/Phonedetailactivity.kt` | Tier-based selector; `createMediumLayoutConfig()`; `fitImageInsideBox` field; conditional `Fit` at 3 image call sites; conditional `verticalScroll` on variant column |
| `app/src/main/java/com/techcity/techcityassist/phonecomparisonactivity.kt` | Tier-based selector; `createMediumComparisonLayoutConfig()`; `fitImageInsideBox` field; conditional `Fit` in `PhoneImageView` |
| `app/src/main/java/com/techcity/techcityassist/Phonelistactivity.kt` | `CardLayoutConfig` + 2 fields; tier-aware `remember` in `PhoneListScreen`; `PhoneCard` image column width + `PhoneImageItem` content scale |
| `app/src/main/java/com/techcity/techcityassist/Mergedphonecard.kt` | Image column width + content scale from config |
| `app/src/main/java/com/techcity/techcityassist/MainActivity.kt` | Medium tier: two-column brand pills fixed at 80 dp tall so 5 rows fit without scrolling. Also temporarily `SHOW_DEBUG_INFO = true` for Step 0 only; reverted before commit |

No manifest, Gradle, data-model, repository or Firestore changes.

## Risks and Mitigations

- **Measured dp differs from the assumption** (e.g. the tablet reports 900 x 1440 dp). Mitigation: Step 0 is done first and the single constant (or an area-based rule) is set from real numbers before any layout work; all later steps reference tier names only.
- **The 11-inch tablet reports exactly 1318 dp or more height** (would mean height alone cannot distinguish it from the reference tablet). Mitigation: fall back to a two-dimension rule (`width >= 824 && height >= 1318` for large), still purely dimension-based.
- **Medium values need on-device tuning.** The proposed numbers are a starting point with ~360 dp of vertical headroom; tuning happens only inside the medium factory functions, so it cannot leak into the working tiers.
- **`ContentScale.Fit` makes the image visibly smaller than users are used to.** Accepted by the owner (open question 4). The wider image column on list cards (160 dp) and the un-shrunk 420/520 dp heights keep the visual size close to today's.
- **Zoom translation constants are in pixels**, so the -225 px / -200 px nudge is smaller on lower-density screens. Verified in Step 6 #6; gated off on the medium tier if it causes overlap.
- **Merged cards grow with variant count** (`calculateMergedCardHeight`) but their image height is fixed from `cardHeight - 84 dp`, so the `Fit` change is sufficient; no height rule changes.

## Out of Scope (per spec Non-Goals)

Landscape, phone-sized devices, other screens, image loading/caching, data model, and any change to the standard or large value sets.
