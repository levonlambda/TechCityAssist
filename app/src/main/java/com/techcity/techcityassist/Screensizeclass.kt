package com.techcity.techcityassist

import android.content.res.Resources
import android.util.DisplayMetrics
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import kotlin.math.sqrt

// ============================================
// SHARED TABLET SIZE TIER
// ============================================
// Reference devices:
//   SMALL     tablets with a physical diagonal of 9" or less
//             (e.g. 8.7" 800 x 1340 px -> ~600 x 1007 dp) - image sizes
//             reduced, home-screen logo/brand pills compacted
//   STANDARD  601 x 1007 dp class, larger than 9" (width <= 650dp) - preserved exactly
//   LARGE     824 x 1318 dp tablet  (width > 650dp, tall)         - preserved exactly
//   MEDIUM    11" 1200 x 1920 px tablet (~800 x ~1250 dp):
//             wide enough for the large tier but not tall enough
//             for its fixed heights -> images crop, 4th variant clips.
//
// The tier is derived from the reported screen dimensions only (dp size and
// the display's physical dpi); no device model, manufacturer or build
// property is consulted.
// ============================================

enum class TabletTier { SMALL, STANDARD, MEDIUM, LARGE }

/** Physical diagonal (inches) at or below which a tablet is SMALL. */
const val SMALL_MAX_DIAGONAL_INCHES = 9.0f

/** Existing width threshold that separates the standard tier from the rest. */
const val LARGE_MIN_WIDTH_DP = 650

/**
 * Minimum reported screen height (dp) for the LARGE tier.
 * Sits safely below the 1318dp reference tablet and above the ~1250-1280dp
 * an 800 x 1280dp (1200 x 1920 px @1.5x) tablet reports.
 * Adjust from the Step 0 measurement in _plans/responsive-ui-lower-resolution.md.
 */
const val LARGE_MIN_HEIGHT_DP = 1300

/**
 * Physical screen diagonal in inches from the system display metrics
 * (full display, not the app window). Falls back to the logical density
 * when a device reports an implausible physical dpi.
 */
fun screenDiagonalInches(metrics: DisplayMetrics = Resources.getSystem().displayMetrics): Float {
    val logicalDpi = metrics.densityDpi.toFloat()
    val xdpi = if (metrics.xdpi in 50f..1000f) metrics.xdpi else logicalDpi
    val ydpi = if (metrics.ydpi in 50f..1000f) metrics.ydpi else logicalDpi
    val widthIn = metrics.widthPixels / xdpi
    val heightIn = metrics.heightPixels / ydpi
    return sqrt(widthIn * widthIn + heightIn * heightIn)
}

fun tabletTier(widthDp: Int, heightDp: Int, diagonalInches: Float): TabletTier = when {
    diagonalInches > 0f && diagonalInches <= SMALL_MAX_DIAGONAL_INCHES -> TabletTier.SMALL
    widthDp <= LARGE_MIN_WIDTH_DP -> TabletTier.STANDARD
    heightDp >= LARGE_MIN_HEIGHT_DP -> TabletTier.LARGE
    else -> TabletTier.MEDIUM
}

@Composable
fun rememberTabletTier(): TabletTier {
    val configuration = LocalConfiguration.current
    val widthDp = configuration.screenWidthDp
    val heightDp = configuration.screenHeightDp
    return remember(widthDp, heightDp) { tabletTier(widthDp, heightDp, screenDiagonalInches()) }
}
