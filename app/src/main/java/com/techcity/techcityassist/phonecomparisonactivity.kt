package com.techcity.techcityassist

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.decode.SvgDecoder
import com.google.firebase.firestore.FirebaseFirestore
import com.techcity.techcityassist.ui.theme.TechCityAssistTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.NumberFormat
import java.util.Locale

class PhoneComparisonActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PHONE1_DOC_ID = "phone1_doc_id"
        const val EXTRA_PHONE1_MANUFACTURER = "phone1_manufacturer"
        const val EXTRA_PHONE1_MODEL = "phone1_model"
        const val EXTRA_PHONE1_RAM = "phone1_ram"
        const val EXTRA_PHONE1_STORAGE = "phone1_storage"
        const val EXTRA_PHONE1_RETAIL_PRICE = "phone1_retail_price"
        const val EXTRA_PHONE1_COLORS = "phone1_colors"
        const val EXTRA_PHONE1_CHIPSET = "phone1_chipset"
        const val EXTRA_PHONE1_FRONT_CAMERA = "phone1_front_camera"
        const val EXTRA_PHONE1_REAR_CAMERA = "phone1_rear_camera"
        const val EXTRA_PHONE1_BATTERY = "phone1_battery"
        const val EXTRA_PHONE1_DISPLAY_SIZE = "phone1_display_size"
        const val EXTRA_PHONE1_OS = "phone1_os"
        const val EXTRA_PHONE1_NETWORK = "phone1_network"
        const val EXTRA_PHONE1_RESOLUTION = "phone1_resolution"
        const val EXTRA_PHONE1_REFRESH_RATE = "phone1_refresh_rate"
        const val EXTRA_PHONE1_WIRED_CHARGING = "phone1_wired_charging"

        const val EXTRA_PHONE2_DOC_ID = "phone2_doc_id"
        const val EXTRA_PHONE2_MANUFACTURER = "phone2_manufacturer"
        const val EXTRA_PHONE2_MODEL = "phone2_model"
        const val EXTRA_PHONE2_RAM = "phone2_ram"
        const val EXTRA_PHONE2_STORAGE = "phone2_storage"
        const val EXTRA_PHONE2_RETAIL_PRICE = "phone2_retail_price"
        const val EXTRA_PHONE2_COLORS = "phone2_colors"
        const val EXTRA_PHONE2_CHIPSET = "phone2_chipset"
        const val EXTRA_PHONE2_FRONT_CAMERA = "phone2_front_camera"
        const val EXTRA_PHONE2_REAR_CAMERA = "phone2_rear_camera"
        const val EXTRA_PHONE2_BATTERY = "phone2_battery"
        const val EXTRA_PHONE2_DISPLAY_SIZE = "phone2_display_size"
        const val EXTRA_PHONE2_OS = "phone2_os"
        const val EXTRA_PHONE2_NETWORK = "phone2_network"
        const val EXTRA_PHONE2_RESOLUTION = "phone2_resolution"
        const val EXTRA_PHONE2_REFRESH_RATE = "phone2_refresh_rate"
        const val EXTRA_PHONE2_WIRED_CHARGING = "phone2_wired_charging"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Extract Phone 1 data
        val phone1 = Phone(
            phoneDocId = intent.getStringExtra(EXTRA_PHONE1_DOC_ID) ?: "",
            manufacturer = intent.getStringExtra(EXTRA_PHONE1_MANUFACTURER) ?: "",
            model = intent.getStringExtra(EXTRA_PHONE1_MODEL) ?: "",
            ram = intent.getStringExtra(EXTRA_PHONE1_RAM) ?: "",
            storage = intent.getStringExtra(EXTRA_PHONE1_STORAGE) ?: "",
            retailPrice = intent.getDoubleExtra(EXTRA_PHONE1_RETAIL_PRICE, 0.0),
            colors = intent.getStringArrayListExtra(EXTRA_PHONE1_COLORS) ?: arrayListOf(),
            chipset = intent.getStringExtra(EXTRA_PHONE1_CHIPSET) ?: "",
            frontCamera = intent.getStringExtra(EXTRA_PHONE1_FRONT_CAMERA) ?: "",
            rearCamera = intent.getStringExtra(EXTRA_PHONE1_REAR_CAMERA) ?: "",
            batteryCapacity = intent.getIntExtra(EXTRA_PHONE1_BATTERY, 0),
            displaySize = intent.getStringExtra(EXTRA_PHONE1_DISPLAY_SIZE) ?: "",
            os = intent.getStringExtra(EXTRA_PHONE1_OS) ?: "",
            network = intent.getStringExtra(EXTRA_PHONE1_NETWORK) ?: "",
            resolution = intent.getStringExtra(EXTRA_PHONE1_RESOLUTION) ?: "",
            refreshRate = intent.getIntExtra(EXTRA_PHONE1_REFRESH_RATE, 0),
            wiredCharging = intent.getIntExtra(EXTRA_PHONE1_WIRED_CHARGING, 0)
        )

        // Extract Phone 2 data
        val phone2 = Phone(
            phoneDocId = intent.getStringExtra(EXTRA_PHONE2_DOC_ID) ?: "",
            manufacturer = intent.getStringExtra(EXTRA_PHONE2_MANUFACTURER) ?: "",
            model = intent.getStringExtra(EXTRA_PHONE2_MODEL) ?: "",
            ram = intent.getStringExtra(EXTRA_PHONE2_RAM) ?: "",
            storage = intent.getStringExtra(EXTRA_PHONE2_STORAGE) ?: "",
            retailPrice = intent.getDoubleExtra(EXTRA_PHONE2_RETAIL_PRICE, 0.0),
            colors = intent.getStringArrayListExtra(EXTRA_PHONE2_COLORS) ?: arrayListOf(),
            chipset = intent.getStringExtra(EXTRA_PHONE2_CHIPSET) ?: "",
            frontCamera = intent.getStringExtra(EXTRA_PHONE2_FRONT_CAMERA) ?: "",
            rearCamera = intent.getStringExtra(EXTRA_PHONE2_REAR_CAMERA) ?: "",
            batteryCapacity = intent.getIntExtra(EXTRA_PHONE2_BATTERY, 0),
            displaySize = intent.getStringExtra(EXTRA_PHONE2_DISPLAY_SIZE) ?: "",
            os = intent.getStringExtra(EXTRA_PHONE2_OS) ?: "",
            network = intent.getStringExtra(EXTRA_PHONE2_NETWORK) ?: "",
            resolution = intent.getStringExtra(EXTRA_PHONE2_RESOLUTION) ?: "",
            refreshRate = intent.getIntExtra(EXTRA_PHONE2_REFRESH_RATE, 0),
            wiredCharging = intent.getIntExtra(EXTRA_PHONE2_WIRED_CHARGING, 0)
        )

        setContent {
            TechCityAssistTheme {
                PhoneComparisonScreen(
                    phone1 = phone1,
                    phone2 = phone2,
                    onBackPress = { finish() }
                )
            }
        }
    }
}

// ============================================
// RESPONSIVE LAYOUT CONFIGURATION
// ============================================
// Your working tablet: 601 x 1007 dp - PRESERVE THIS EXACT LAYOUT
// Larger tablet: 824 x 1318 dp - SCALE UP FOR THIS
//
// Threshold: screen width > 650dp triggers large layout
// ============================================

/**
 * Configuration for all layout values in PhoneComparisonActivity
 */
data class ComparisonLayoutConfig(
    // Logo
    val logoHeight: Dp,

    // Model name
    val modelNameFontSize: TextUnit,
    val appleLogoSize: Dp,

    // Spacing
    val topPadding: Dp,
    val logoToModelSpacing: Dp,
    val modelToContentSpacing: Dp,
    val horizontalPadding: Dp,

    // Phone image
    val imageHeight: Dp,
    val imageScale: Float,
    val imageYOffset: Dp,

    // Color dots
    val colorDotSizeSelected: Dp,
    val colorDotSizeUnselected: Dp,
    val colorNameFontSize: TextUnit,

    // Spec row
    val specIconSize: Dp,
    val specLabelFontSize: TextUnit,
    val specValueFontSize: TextUnit,
    val specRowPadding: Dp,
    val arrowSize: Dp,

    // Price
    val priceFontSize: TextUnit,
    val priceBottomSpacing: Dp,

    // Column padding
    val leftColumnStartPadding: Dp,
    val leftColumnEndPadding: Dp,
    val rightColumnStartPadding: Dp,
    val rightColumnEndPadding: Dp
)

/**
 * Standard layout - your working tablet (601 x 1007 dp)
 * These are the EXACT values from your current working code - DO NOT CHANGE
 */
private fun createStandardComparisonLayoutConfig(): ComparisonLayoutConfig {
    return ComparisonLayoutConfig(
        // Logo - current value
        logoHeight = 36.dp,

        // Model name - current values
        modelNameFontSize = 26.sp,
        appleLogoSize = 32.dp,

        // Spacing - current values
        topPadding = 50.dp,
        logoToModelSpacing = 16.dp,
        modelToContentSpacing = 16.dp,
        horizontalPadding = 16.dp,

        // Phone image - current values
        imageHeight = 500.dp,
        imageScale = 1.15f,
        imageYOffset = (-40).dp,

        // Color dots - current values
        colorDotSizeSelected = 24.dp,
        colorDotSizeUnselected = 18.dp,
        colorNameFontSize = 14.sp,

        // Spec row - current values
        specIconSize = 42.dp,
        specLabelFontSize = 14.sp,
        specValueFontSize = 15.sp,
        specRowPadding = 6.dp,
        arrowSize = 32.dp,

        // Price - current values
        priceFontSize = 28.sp,
        priceBottomSpacing = 60.dp,

        // Column padding - current values
        leftColumnStartPadding = 48.dp,
        leftColumnEndPadding = 0.dp,
        rightColumnStartPadding = 0.dp,
        rightColumnEndPadding = 48.dp
    )
}

/**
 * Large layout - for screens bigger than 650dp width
 * Scaled up for your larger tablet (824 x 1318 dp)
 */
private fun createLargeComparisonLayoutConfig(): ComparisonLayoutConfig {
    return ComparisonLayoutConfig(
        // Logo - A LOT BIGGER (50% increase)
        logoHeight = 54.dp,

        // Model name - BIGGER (~25% increase)
        modelNameFontSize = 32.sp,
        appleLogoSize = 40.dp,

        // Spacing - INCREASED GAPS
        topPadding = 60.dp,
        logoToModelSpacing = 24.dp,
        modelToContentSpacing = 24.dp,
        horizontalPadding = 24.dp,

        // Phone image - 25% BIGGER (500 * 1.25 = 625)
        imageHeight = 625.dp,
        imageScale = 1.15f,
        imageYOffset = (-50).dp,

        // Color dots - proportionally bigger
        colorDotSizeSelected = 30.dp,
        colorDotSizeUnselected = 22.dp,
        colorNameFontSize = 17.sp,

        // Spec row - INCREASED (~20-25% bigger)
        specIconSize = 52.dp,
        specLabelFontSize = 17.sp,
        specValueFontSize = 19.sp,
        specRowPadding = 8.dp,
        arrowSize = 40.dp,

        // Price - BIGGER (~25% increase)
        priceFontSize = 35.sp,
        priceBottomSpacing = 75.dp,

        // Column padding - proportionally increased
        leftColumnStartPadding = 60.dp,
        leftColumnEndPadding = 0.dp,
        rightColumnStartPadding = 0.dp,
        rightColumnEndPadding = 60.dp
    )
}

/**
 * Get the appropriate layout config based on screen size
 */
@Composable
fun rememberComparisonLayoutConfig(): ComparisonLayoutConfig {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp

    return remember(screenWidthDp) {
        if (screenWidthDp > 650) {
            Log.d("PhoneComparison", "Using LARGE layout for screen width: ${screenWidthDp}dp")
            createLargeComparisonLayoutConfig()
        } else {
            Log.d("PhoneComparison", "Using STANDARD layout for screen width: ${screenWidthDp}dp")
            createStandardComparisonLayoutConfig()
        }
    }
}

// ============================================
// END RESPONSIVE CONFIGURATION
// ============================================

/**
 * Spec comparison result: positive = phone1 is higher, negative = phone2 is higher, 0 = equal
 */
data class SpecComparisonResults(
    val comparisons: Map<String, Int> = emptyMap()
) {
    fun getResult(specName: String): Int = comparisons[specName] ?: 0
    fun isDifferent(specName: String): Boolean = comparisons[specName] != 0
}

private fun extractDisplaySize(spec: String): Double? {
    if (spec == "N/A" || spec.isEmpty()) return null
    val match = Regex("""(\d+\.\d+|\d+)""").find(spec)
    return match?.value?.toDoubleOrNull()
}

private fun extractResolutionPixels(spec: String): Long? {
    if (spec == "N/A" || spec.isEmpty()) return null
    val match = Regex("""(\d+)\s*[xX]\s*(\d+)""").find(spec)
    return if (match != null) {
        val width = match.groupValues[1].toLongOrNull() ?: return null
        val height = match.groupValues[2].toLongOrNull() ?: return null
        width * height
    } else null
}

private fun extractCameraMP(spec: String): Int? {
    if (spec == "N/A" || spec.isEmpty()) return null
    val matches = Regex("""(\d+)\s*MP""", RegexOption.IGNORE_CASE).findAll(spec)
    val values = matches.map { it.groupValues[1].toIntOrNull() ?: 0 }.toList()
    return if (values.isNotEmpty()) values.sum() else null
}

private fun extractNetworkGeneration(spec: String): Int? {
    if (spec == "N/A" || spec.isEmpty()) return null
    val match = Regex("""(\d+)\s*G""", RegexOption.IGNORE_CASE).find(spec)
    return match?.groupValues?.get(1)?.toIntOrNull()
}

private fun extractNumberWithCommas(spec: String): Int? {
    if (spec == "N/A" || spec.isEmpty()) return null
    val cleanSpec = spec.replace(",", "")
    val match = Regex("""(\d+)""").find(cleanSpec)
    return match?.value?.toIntOrNull()
}

private fun <T : Comparable<T>> compareValues(value1: T?, value2: T?): Int {
    return when {
        value1 == null && value2 == null -> 0
        value1 == null -> -1
        value2 == null -> 1
        value1 > value2 -> 1
        value1 < value2 -> -1
        else -> 0
    }
}

private fun computeSpecComparisons(phone1: Phone, phone2: Phone): SpecComparisonResults {
    val comparisons = mutableMapOf<String, Int>()

    val displaySize1 = extractDisplaySize(phone1.displaySize)
    val displaySize2 = extractDisplaySize(phone2.displaySize)
    comparisons["Display Size"] = compareValues(displaySize1, displaySize2)

    val resolution1 = extractResolutionPixels(phone1.resolution)
    val resolution2 = extractResolutionPixels(phone2.resolution)
    comparisons["Resolution"] = compareValues(resolution1, resolution2)

    val refreshRate1 = if (phone1.refreshRate > 0) phone1.refreshRate else null
    val refreshRate2 = if (phone2.refreshRate > 0) phone2.refreshRate else null
    comparisons["Refresh Rate"] = compareValues(refreshRate1, refreshRate2)

    val frontCamera1 = extractCameraMP(phone1.frontCamera)
    val frontCamera2 = extractCameraMP(phone2.frontCamera)
    comparisons["Front Camera"] = compareValues(frontCamera1, frontCamera2)

    val rearCamera1 = extractCameraMP(phone1.rearCamera)
    val rearCamera2 = extractCameraMP(phone2.rearCamera)
    comparisons["Rear Camera"] = compareValues(rearCamera1, rearCamera2)

    comparisons["Chipset"] = 0

    val battery1 = if (phone1.batteryCapacity > 0) phone1.batteryCapacity else null
    val battery2 = if (phone2.batteryCapacity > 0) phone2.batteryCapacity else null
    comparisons["Battery"] = compareValues(battery1, battery2)

    val charging1 = if (phone1.wiredCharging > 0) phone1.wiredCharging else null
    val charging2 = if (phone2.wiredCharging > 0) phone2.wiredCharging else null
    comparisons["Charging"] = compareValues(charging1, charging2)

    comparisons["OS"] = 0

    val network1 = extractNetworkGeneration(phone1.network)
    val network2 = extractNetworkGeneration(phone2.network)
    comparisons["Network"] = compareValues(network1, network2)

    val ram1 = extractNumberWithCommas(phone1.ram)
    val ram2 = extractNumberWithCommas(phone2.ram)
    comparisons["RAM"] = compareValues(ram1, ram2)

    val storage1 = extractNumberWithCommas(phone1.storage)
    val storage2 = extractNumberWithCommas(phone2.storage)
    comparisons["Storage"] = compareValues(storage1, storage2)

    return SpecComparisonResults(comparisons)
}

@Composable
fun PhoneComparisonScreen(
    phone1: Phone,
    phone2: Phone,
    onBackPress: () -> Unit
) {
    val context = LocalContext.current
    val formatter = remember { NumberFormat.getNumberInstance(Locale.US) }

    // Get responsive layout configuration
    val layoutConfig = rememberComparisonLayoutConfig()

    var phone1Images by remember { mutableStateOf<PhoneImages?>(null) }
    var phone2Images by remember { mutableStateOf<PhoneImages?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val specComparisons = remember(phone1, phone2) {
        computeSpecComparisons(phone1, phone2)
    }

    val specRowHeights = remember { mutableStateMapOf<String, Dp>() }

    var showComparisonArrows by remember { mutableStateOf(true) }

    var showBackgroundPill by remember { mutableStateOf(true) }

    val onSpecRowHeightMeasured: (String, Dp) -> Unit = { label, height ->
        val currentHeight = specRowHeights[label] ?: 0.dp
        if (height > currentHeight) {
            specRowHeights[label] = height
        }
    }

    LaunchedEffect(phone1.phoneDocId, phone2.phoneDocId) {
        try {
            val db = FirebaseFirestore.getInstance()

            if (phone1.phoneDocId.isNotEmpty()) {
                val doc1 = db.collection("phone_images").document(phone1.phoneDocId).get().await()
                if (doc1.exists()) {
                    phone1Images = parsePhoneImagesDocumentComparison(doc1.id, doc1.data)
                }
            }

            if (phone2.phoneDocId.isNotEmpty()) {
                val doc2 = db.collection("phone_images").document(phone2.phoneDocId).get().await()
                if (doc2.exists()) {
                    phone2Images = parsePhoneImagesDocumentComparison(doc2.id, doc2.data)
                }
            }
        } catch (e: Exception) {
            Log.e("PhoneComparison", "Error fetching images", e)
        }
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = layoutConfig.horizontalPadding)
    ) {
        // TechCity logo at top center - USES CONFIG
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = layoutConfig.topPadding),
            horizontalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.tc_logo_flat_colored),
                contentDescription = "TechCity Logo - tap to toggle background",
                modifier = Modifier
                    .height(layoutConfig.logoHeight)
                    .clickable { showBackgroundPill = !showBackgroundPill },
                contentScale = ContentScale.FillHeight
            )
        }

        Spacer(modifier = Modifier.height(layoutConfig.logoToModelSpacing))

        // Main comparison area - two columns
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Phone 1 column (LEFT)
            PhoneComparisonColumn(
                phone = phone1,
                phoneImages = phone1Images,
                isLoading = isLoading,
                specComparisons = specComparisons,
                specRowHeights = specRowHeights,
                onSpecRowHeightMeasured = onSpecRowHeightMeasured,
                showComparisonArrows = showComparisonArrows,
                onToggleArrows = { showComparisonArrows = !showComparisonArrows },
                showBackgroundPill = showBackgroundPill,
                isLeftColumn = true,
                layoutConfig = layoutConfig,
                modifier = Modifier.weight(1f)
            )

            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .padding(vertical = 16.dp)
                    .background(Color(0xFFE0E0E0))
            )

            // Phone 2 column (RIGHT)
            PhoneComparisonColumn(
                phone = phone2,
                phoneImages = phone2Images,
                isLoading = isLoading,
                specComparisons = specComparisons,
                specRowHeights = specRowHeights,
                onSpecRowHeightMeasured = onSpecRowHeightMeasured,
                showComparisonArrows = showComparisonArrows,
                onToggleArrows = { showComparisonArrows = !showComparisonArrows },
                showBackgroundPill = showBackgroundPill,
                isLeftColumn = false,
                layoutConfig = layoutConfig,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhoneComparisonColumn(
    phone: Phone,
    phoneImages: PhoneImages?,
    isLoading: Boolean,
    specComparisons: SpecComparisonResults,
    specRowHeights: SnapshotStateMap<String, Dp>,
    onSpecRowHeightMeasured: (String, Dp) -> Unit,
    showComparisonArrows: Boolean,
    onToggleArrows: () -> Unit,
    showBackgroundPill: Boolean,
    isLeftColumn: Boolean,
    layoutConfig: ComparisonLayoutConfig,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val formatter = remember { NumberFormat.getNumberInstance(Locale.US) }

    var selectedColorName by remember(phone.phoneDocId) {
        mutableStateOf(phone.colors.firstOrNull() ?: "")
    }

    val infinitePageCount = 10000
    val startPage = infinitePageCount / 2

    val pagerState = rememberPagerState(
        initialPage = startPage,
        pageCount = { infinitePageCount }
    )

    fun getActualViewIndex(page: Int): Int = page % 2

    val displayName = remember(phone.manufacturer, phone.model) {
        if (phone.manufacturer.equals("Apple", ignoreCase = true)) {
            formatModelNameComparison(phone.model)
        } else {
            val fullName = "${phone.manufacturer} ${formatModelNameComparison(phone.model)}"
            val modelOnly = formatModelNameComparison(phone.model)
            if (fullName.length > 20) modelOnly else fullName
        }
    }

    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Model name at top - USES CONFIG
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            if (phone.manufacturer.equals("Apple", ignoreCase = true)) {
                Image(
                    painter = painterResource(id = R.drawable.apple_logo),
                    contentDescription = "Apple logo",
                    modifier = Modifier.size(layoutConfig.appleLogoSize)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = displayName,
                fontSize = layoutConfig.modelNameFontSize,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A),
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false
            )
        }

        Spacer(modifier = Modifier.height(layoutConfig.modelToContentSpacing))

        // Swipeable content area
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { page ->
            when (getActualViewIndex(page)) {
                0 -> {
                    PhoneSpecsView(
                        phone = phone,
                        specComparisons = specComparisons,
                        specRowHeights = specRowHeights,
                        onSpecRowHeightMeasured = onSpecRowHeightMeasured,
                        showComparisonArrows = showComparisonArrows,
                        onToggleArrows = onToggleArrows,
                        showBackgroundPill = showBackgroundPill,
                        isLeftColumn = isLeftColumn,
                        layoutConfig = layoutConfig,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                1 -> {
                    PhoneImageView(
                        phone = phone,
                        phoneImages = phoneImages,
                        selectedColorName = selectedColorName,
                        onColorSelected = { selectedColorName = it },
                        isLoading = isLoading,
                        layoutConfig = layoutConfig,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Price - USES CONFIG
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (phone.retailPrice > 0) "₱${String.format("%,.2f", phone.retailPrice)}" else "N/A",
            fontSize = layoutConfig.priceFontSize,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFDB2E2E)
        )
        Spacer(modifier = Modifier.height(layoutConfig.priceBottomSpacing))
    }
}

@Composable
fun PhoneSpecsView(
    phone: Phone,
    specComparisons: SpecComparisonResults,
    specRowHeights: SnapshotStateMap<String, Dp>,
    onSpecRowHeightMeasured: (String, Dp) -> Unit,
    showComparisonArrows: Boolean,
    onToggleArrows: () -> Unit,
    showBackgroundPill: Boolean,
    isLeftColumn: Boolean,
    layoutConfig: ComparisonLayoutConfig,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val formatter = remember { NumberFormat.getNumberInstance(Locale.US) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = if (isLeftColumn) layoutConfig.leftColumnStartPadding else layoutConfig.rightColumnStartPadding,
                end = if (isLeftColumn) layoutConfig.leftColumnEndPadding else layoutConfig.rightColumnEndPadding,
                top = 0.dp
            ),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Display Size
        ComparisonSpecRow(
            iconRes = R.raw.screen_size_icon,
            label = "Display Size",
            value = if (phone.displaySize.isNotEmpty()) "${phone.displaySize} inches" else "N/A",
            comparisonResult = specComparisons.getResult("Display Size"),
            minHeight = specRowHeights["Display Size"] ?: 0.dp,
            onHeightMeasured = { height -> onSpecRowHeightMeasured("Display Size", height) },
            showArrow = showComparisonArrows,
            onToggleArrow = onToggleArrows,
            isLeftColumn = isLeftColumn,
            rowIndex = 0,
            showBackgroundPill = showBackgroundPill,
            layoutConfig = layoutConfig
        )

        // Resolution
        ComparisonSpecRow(
            iconRes = R.raw.resolution_icon,
            label = "Resolution",
            value = phone.resolution.ifEmpty { "N/A" },
            comparisonResult = specComparisons.getResult("Resolution"),
            minHeight = specRowHeights["Resolution"] ?: 0.dp,
            onHeightMeasured = { height -> onSpecRowHeightMeasured("Resolution", height) },
            showArrow = showComparisonArrows,
            onToggleArrow = onToggleArrows,
            isLeftColumn = isLeftColumn,
            rowIndex = 1,
            showBackgroundPill = showBackgroundPill,
            layoutConfig = layoutConfig
        )

        // Refresh Rate
        ComparisonSpecRow(
            iconRes = R.raw.refresh_rate_icon,
            label = "Refresh Rate",
            value = if (phone.refreshRate > 0) "${phone.refreshRate} Hz" else "N/A",
            comparisonResult = specComparisons.getResult("Refresh Rate"),
            minHeight = specRowHeights["Refresh Rate"] ?: 0.dp,
            onHeightMeasured = { height -> onSpecRowHeightMeasured("Refresh Rate", height) },
            showArrow = showComparisonArrows,
            onToggleArrow = onToggleArrows,
            isLeftColumn = isLeftColumn,
            rowIndex = 2,
            showBackgroundPill = showBackgroundPill,
            layoutConfig = layoutConfig
        )

        // Front Camera
        ComparisonSpecRow(
            iconRes = R.raw.camera_icon,
            label = "Front Camera",
            value = phone.frontCamera.ifEmpty { "N/A" },
            comparisonResult = specComparisons.getResult("Front Camera"),
            minHeight = specRowHeights["Front Camera"] ?: 0.dp,
            onHeightMeasured = { height -> onSpecRowHeightMeasured("Front Camera", height) },
            showArrow = showComparisonArrows,
            onToggleArrow = onToggleArrows,
            isLeftColumn = isLeftColumn,
            rowIndex = 3,
            showBackgroundPill = showBackgroundPill,
            layoutConfig = layoutConfig
        )

        // Rear Camera
        ComparisonSpecRow(
            iconRes = R.raw.rear_camera_icon,
            label = "Rear Camera",
            value = phone.rearCamera.ifEmpty { "N/A" },
            comparisonResult = specComparisons.getResult("Rear Camera"),
            minHeight = specRowHeights["Rear Camera"] ?: 0.dp,
            onHeightMeasured = { height -> onSpecRowHeightMeasured("Rear Camera", height) },
            showArrow = showComparisonArrows,
            onToggleArrow = onToggleArrows,
            isLeftColumn = isLeftColumn,
            rowIndex = 4,
            showBackgroundPill = showBackgroundPill,
            layoutConfig = layoutConfig
        )

        // Chipset
        ComparisonSpecRow(
            iconRes = R.raw.chipset_icon,
            label = "Chipset",
            value = phone.chipset.ifEmpty { "N/A" },
            comparisonResult = specComparisons.getResult("Chipset"),
            minHeight = specRowHeights["Chipset"] ?: 0.dp,
            onHeightMeasured = { height -> onSpecRowHeightMeasured("Chipset", height) },
            showArrow = showComparisonArrows,
            onToggleArrow = onToggleArrows,
            isLeftColumn = isLeftColumn,
            rowIndex = 5,
            showBackgroundPill = showBackgroundPill,
            layoutConfig = layoutConfig
        )

        // Battery
        ComparisonSpecRow(
            iconRes = R.raw.battery_icon,
            label = "Battery",
            value = if (phone.batteryCapacity > 0) "${formatter.format(phone.batteryCapacity)} mAh" else "N/A",
            comparisonResult = specComparisons.getResult("Battery"),
            minHeight = specRowHeights["Battery"] ?: 0.dp,
            onHeightMeasured = { height -> onSpecRowHeightMeasured("Battery", height) },
            showArrow = showComparisonArrows,
            onToggleArrow = onToggleArrows,
            isLeftColumn = isLeftColumn,
            rowIndex = 6,
            showBackgroundPill = showBackgroundPill,
            layoutConfig = layoutConfig
        )

        // Charging
        ComparisonSpecRow(
            iconRes = R.raw.charging_icon,
            label = "Charging",
            value = if (phone.wiredCharging > 0) "${phone.wiredCharging}W" else "N/A",
            comparisonResult = specComparisons.getResult("Charging"),
            minHeight = specRowHeights["Charging"] ?: 0.dp,
            onHeightMeasured = { height -> onSpecRowHeightMeasured("Charging", height) },
            showArrow = showComparisonArrows,
            onToggleArrow = onToggleArrows,
            isLeftColumn = isLeftColumn,
            rowIndex = 7,
            showBackgroundPill = showBackgroundPill,
            layoutConfig = layoutConfig
        )

        // OS
        ComparisonSpecRow(
            iconRes = R.raw.os_icon,
            label = "OS",
            value = phone.os.ifEmpty { "N/A" },
            comparisonResult = specComparisons.getResult("OS"),
            minHeight = specRowHeights["OS"] ?: 0.dp,
            onHeightMeasured = { height -> onSpecRowHeightMeasured("OS", height) },
            showArrow = showComparisonArrows,
            onToggleArrow = onToggleArrows,
            isLeftColumn = isLeftColumn,
            rowIndex = 8,
            showBackgroundPill = showBackgroundPill,
            layoutConfig = layoutConfig
        )

        // Network
        ComparisonSpecRow(
            iconRes = R.raw.network_icon,
            label = "Network",
            value = phone.network.ifEmpty { "N/A" },
            comparisonResult = specComparisons.getResult("Network"),
            minHeight = specRowHeights["Network"] ?: 0.dp,
            onHeightMeasured = { height -> onSpecRowHeightMeasured("Network", height) },
            showArrow = showComparisonArrows,
            onToggleArrow = onToggleArrows,
            isLeftColumn = isLeftColumn,
            rowIndex = 9,
            showBackgroundPill = showBackgroundPill,
            layoutConfig = layoutConfig
        )

        // RAM
        ComparisonSpecRow(
            iconRes = R.raw.ram_icon,
            label = "RAM",
            value = if (phone.ram.isNotEmpty()) "${phone.ram}GB" else "N/A",
            comparisonResult = specComparisons.getResult("RAM"),
            minHeight = specRowHeights["RAM"] ?: 0.dp,
            onHeightMeasured = { height -> onSpecRowHeightMeasured("RAM", height) },
            showArrow = showComparisonArrows,
            onToggleArrow = onToggleArrows,
            isLeftColumn = isLeftColumn,
            rowIndex = 10,
            showBackgroundPill = showBackgroundPill,
            layoutConfig = layoutConfig
        )

        // Storage
        ComparisonSpecRow(
            iconRes = R.raw.storage_icon,
            label = "Storage",
            value = if (phone.storage.isNotEmpty()) "${phone.storage}GB" else "N/A",
            comparisonResult = specComparisons.getResult("Storage"),
            minHeight = specRowHeights["Storage"] ?: 0.dp,
            onHeightMeasured = { height -> onSpecRowHeightMeasured("Storage", height) },
            showArrow = showComparisonArrows,
            onToggleArrow = onToggleArrows,
            isLeftColumn = isLeftColumn,
            rowIndex = 11,
            showBackgroundPill = showBackgroundPill,
            layoutConfig = layoutConfig
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhoneImageView(
    phone: Phone,
    phoneImages: PhoneImages?,
    selectedColorName: String,
    onColorSelected: (String) -> Unit,
    isLoading: Boolean,
    layoutConfig: ComparisonLayoutConfig,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val colorImages = phoneImages?.getImagesForColor(selectedColorName)
    val imageUrl = colorImages?.highRes?.ifEmpty { colorImages.lowRes } ?: colorImages?.lowRes

    Column(
        modifier = modifier
            .fillMaxSize()
            .offset(y = layoutConfig.imageYOffset),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Phone image - USES CONFIG (25% BIGGER for large screens)
        Box(
            modifier = Modifier
                .height(layoutConfig.imageHeight)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp))
            } else if (imageUrl != null) {
                val isHighRes = colorImages?.lowRes.isNullOrEmpty() == true
                val cachedPath = ImageCacheManager.getLocalImageUri(
                    context, phone.phoneDocId, selectedColorName, isHighRes
                )

                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(cachedPath ?: imageUrl)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .build(),
                    contentDescription = "${phone.manufacturer} ${phone.model} in $selectedColorName",
                    modifier = Modifier
                        .fillMaxHeight()
                        .graphicsLayer {
                            scaleX = layoutConfig.imageScale
                            scaleY = layoutConfig.imageScale
                        },
                    contentScale = ContentScale.FillHeight
                )
            } else {
                Text(
                    text = "No Image\nAvailable",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Color name - USES CONFIG
        if (selectedColorName.isNotEmpty()) {
            Text(
                text = selectedColorName,
                fontSize = layoutConfig.colorNameFontSize,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF555555)
            )
        }

        // Color swatches - USES CONFIG
        if (phone.colors.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                phone.colors.forEach { colorName ->
                    val hexColor = phoneImages?.getHexColorForColor(colorName) ?: ""
                    val isSelected = colorName.equals(selectedColorName, ignoreCase = true)

                    ComparisonColorDot(
                        colorName = colorName,
                        hexColor = hexColor,
                        isSelected = isSelected,
                        selectedSize = layoutConfig.colorDotSizeSelected,
                        unselectedSize = layoutConfig.colorDotSizeUnselected,
                        onClick = { onColorSelected(colorName) }
                    )
                }
            }
        }
    }
}

@Composable
fun ComparisonSpecRow(
    iconRes: Int,
    label: String,
    value: String,
    valueColor: Color = Color(0xFF222222),
    isPrice: Boolean = false,
    comparisonResult: Int = 0,
    minHeight: Dp = 0.dp,
    onHeightMeasured: (Dp) -> Unit = {},
    showArrow: Boolean = true,
    onToggleArrow: () -> Unit = {},
    isLeftColumn: Boolean = true,
    rowIndex: Int = 0,
    showBackgroundPill: Boolean = true,
    layoutConfig: ComparisonLayoutConfig
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    val backgroundColor = if (showBackgroundPill) {
        if (rowIndex % 2 == 0) Color.White else Color(0xFFF5F5F5)
    } else {
        Color.Transparent
    }

    val arrowSpaceWidth = layoutConfig.arrowSize

    val arrowRes: Int? = when {
        comparisonResult == 0 -> null
        isLeftColumn && comparisonResult > 0 -> R.raw.up_arrow
        isLeftColumn && comparisonResult < 0 -> R.raw.down_arrow
        !isLeftColumn && comparisonResult < 0 -> R.raw.up_arrow
        !isLeftColumn && comparisonResult > 0 -> R.raw.down_arrow
        else -> null
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = minHeight)
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .onGloballyPositioned { coordinates ->
                val measuredHeight = with(density) { coordinates.size.height.toDp() }
                onHeightMeasured(measuredHeight)
            }
            .clickable { onToggleArrow() }
            .padding(vertical = layoutConfig.specRowPadding, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isLeftColumn) {
            // LEFT COLUMN LAYOUT

            // Icon - USES CONFIG
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(iconRes)
                    .decoderFactory(SvgDecoder.Factory())
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = null,
                modifier = Modifier.size(layoutConfig.specIconSize)
            )

            Spacer(modifier = Modifier.width(14.dp))

            // Label and Value - USES CONFIG
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = label,
                    fontSize = layoutConfig.specLabelFontSize,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF888888),
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    fontSize = if (isPrice) 20.sp else layoutConfig.specValueFontSize,
                    fontWeight = if (isPrice) FontWeight.Bold else FontWeight.SemiBold,
                    color = valueColor,
                    lineHeight = if (isPrice) 24.sp else 18.sp,
                    maxLines = 2
                )
            }

            // Arrow space - USES CONFIG
            Box(
                modifier = Modifier.width(arrowSpaceWidth),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (showArrow && arrowRes != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(arrowRes)
                            .decoderFactory(SvgDecoder.Factory())
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .build(),
                        contentDescription = if (arrowRes == R.raw.up_arrow) "Higher" else "Lower",
                        modifier = Modifier.size(layoutConfig.arrowSize)
                    )
                }
            }
        } else {
            // RIGHT COLUMN LAYOUT

            // Arrow space - USES CONFIG
            Box(
                modifier = Modifier.width(arrowSpaceWidth),
                contentAlignment = Alignment.CenterStart
            ) {
                if (showArrow && arrowRes != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(arrowRes)
                            .decoderFactory(SvgDecoder.Factory())
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .build(),
                        contentDescription = if (arrowRes == R.raw.up_arrow) "Higher" else "Lower",
                        modifier = Modifier.size(layoutConfig.arrowSize)
                    )
                }
            }

            // Content area
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icon - USES CONFIG
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(iconRes)
                            .decoderFactory(SvgDecoder.Factory())
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.size(layoutConfig.specIconSize)
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    // Label and Value - USES CONFIG
                    Column {
                        Text(
                            text = label,
                            fontSize = layoutConfig.specLabelFontSize,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF888888),
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = value,
                            fontSize = if (isPrice) 20.sp else layoutConfig.specValueFontSize,
                            fontWeight = if (isPrice) FontWeight.Bold else FontWeight.SemiBold,
                            color = valueColor,
                            lineHeight = if (isPrice) 24.sp else 18.sp,
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ComparisonColorDot(
    colorName: String,
    hexColor: String = "",
    isSelected: Boolean = false,
    selectedSize: Dp = 24.dp,
    unselectedSize: Dp = 18.dp,
    onClick: () -> Unit
) {
    val backgroundColor = remember(hexColor, colorName) {
        if (hexColor.isNotEmpty()) {
            parseHexColorComparison(hexColor)
        } else {
            getColorFromNameComparison(colorName)
        }
    }

    Surface(
        modifier = Modifier
            .size(if (isSelected) selectedSize else unselectedSize)
            .border(
                width = 1.dp,
                color = Color(0xFFDDDDDD),
                shape = CircleShape
            )
            .clickable { onClick() },
        shape = CircleShape,
        color = backgroundColor
    ) {}
}

// ============================================
// Utility Functions
// ============================================

private fun formatModelNameComparison(model: String): String {
    return model
        .replace("Iphone", "iPhone", ignoreCase = true)
        .replace("Ipad", "iPad", ignoreCase = true)
        .replace("(refurbished)", "*", ignoreCase = true)
}

private fun parseHexColorComparison(hex: String): Color {
    return try {
        val cleanHex = hex.removePrefix("#")
        val colorLong = cleanHex.toLong(16)

        if (cleanHex.length == 6) {
            Color(
                red = ((colorLong shr 16) and 0xFF) / 255f,
                green = ((colorLong shr 8) and 0xFF) / 255f,
                blue = (colorLong and 0xFF) / 255f
            )
        } else if (cleanHex.length == 3) {
            val r = ((colorLong shr 8) and 0xF) * 17
            val g = ((colorLong shr 4) and 0xF) * 17
            val b = (colorLong and 0xF) * 17
            Color(r / 255f, g / 255f, b / 255f)
        } else {
            Color.Gray
        }
    } catch (e: Exception) {
        Color.Gray
    }
}

private fun getColorFromNameComparison(colorName: String): Color {
    return when (colorName.lowercase()) {
        "black" -> Color.Black
        "white" -> Color.White
        "blue" -> Color(0xFF4A90D9)
        "red" -> Color.Red
        "green" -> Color.Green
        "gold" -> Color(0xFFFFD700)
        "silver" -> Color(0xFFC0C0C0)
        "purple" -> Color(0xFF800080)
        "pink" -> Color(0xFFFFC0CB)
        "orange" -> Color(0xFFFFA500)
        "yellow" -> Color.Yellow
        "gray", "grey" -> Color.Gray
        "brown" -> Color(0xFF8B4513)
        "midnight" -> Color(0xFF191970)
        "starlight" -> Color(0xFFFAF0E6)
        "light silver" -> Color(0xFFD3D3D3)
        "titanium blue" -> Color(0xFF4A90D9)
        "titanium black" -> Color(0xFF333333)
        else -> Color.Gray
    }
}

@Suppress("UNCHECKED_CAST")
private fun parsePhoneImagesDocumentComparison(docId: String, data: Map<String, Any>?): PhoneImages? {
    if (data == null) return null

    return try {
        val colorsMap = mutableMapOf<String, ColorImages>()

        val colorsData = data["colors"] as? Map<String, Map<String, String>>
        colorsData?.forEach { (colorName, imageUrls) ->
            colorsMap[colorName] = ColorImages(
                highRes = imageUrls["highRes"] ?: "",
                lowRes = imageUrls["lowRes"] ?: "",
                hexColor = imageUrls["hexColor"] ?: ""
            )
        }

        PhoneImages(
            phoneDocId = data["phoneDocId"] as? String ?: docId,
            manufacturer = data["manufacturer"] as? String ?: "",
            model = data["model"] as? String ?: "",
            colors = colorsMap
        )
    } catch (e: Exception) {
        Log.e("PhoneComparison", "Error parsing phone images document: $docId", e)
        null
    }
}