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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

/**
 * Spec comparison result: positive = phone1 is higher, negative = phone2 is higher, 0 = equal
 */
data class SpecComparisonResults(
    val comparisons: Map<String, Int> = emptyMap()  // spec name -> comparison result
) {
    fun getResult(specName: String): Int = comparisons[specName] ?: 0
    fun isDifferent(specName: String): Boolean = comparisons[specName] != 0
}

/**
 * Extract numeric value from a spec string for comparison
 * Returns null if no number can be extracted
 */
private fun extractNumericValue(spec: String): Double? {
    if (spec == "N/A" || spec.isEmpty()) return null

    // Try to find all numbers in the string and sum them (for multi-camera specs like "12MP + 50MP")
    val numbers = Regex("""(\d+\.?\d*)""").findAll(spec).map { it.value.toDoubleOrNull() ?: 0.0 }.toList()

    return if (numbers.isNotEmpty()) numbers.sum() else null
}

/**
 * Compare two spec values
 * Returns: 1 if value1 > value2, -1 if value1 < value2, 0 if equal
 */
private fun compareSpecValues(value1: String, value2: String): Int {
    if (value1 == value2) return 0

    val num1 = extractNumericValue(value1)
    val num2 = extractNumericValue(value2)

    return when {
        num1 == null && num2 == null -> 0  // Can't compare non-numeric values
        num1 == null -> -1  // N/A is considered lower
        num2 == null -> 1   // N/A is considered lower
        num1 > num2 -> 1
        num1 < num2 -> -1
        else -> 0
    }
}

/**
 * Compute comparison results for all specs between two phones
 * Positive = phone1 is higher, Negative = phone2 is higher, 0 = equal
 */
private fun computeSpecComparisons(phone1: Phone, phone2: Phone): SpecComparisonResults {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    val comparisons = mutableMapOf<String, Int>()

    // Display Size
    val spec1DisplaySize = if (phone1.displaySize.isNotEmpty()) "${phone1.displaySize} inches" else "N/A"
    val spec2DisplaySize = if (phone2.displaySize.isNotEmpty()) "${phone2.displaySize} inches" else "N/A"
    comparisons["Display Size"] = compareSpecValues(spec1DisplaySize, spec2DisplaySize)

    // Resolution (compare total pixels if possible)
    val spec1Resolution = phone1.resolution.ifEmpty { "N/A" }
    val spec2Resolution = phone2.resolution.ifEmpty { "N/A" }
    comparisons["Resolution"] = compareSpecValues(spec1Resolution, spec2Resolution)

    // Refresh Rate
    val spec1RefreshRate = if (phone1.refreshRate > 0) "${phone1.refreshRate} Hz" else "N/A"
    val spec2RefreshRate = if (phone2.refreshRate > 0) "${phone2.refreshRate} Hz" else "N/A"
    comparisons["Refresh Rate"] = compareSpecValues(spec1RefreshRate, spec2RefreshRate)

    // Front Camera
    val spec1FrontCamera = phone1.frontCamera.ifEmpty { "N/A" }
    val spec2FrontCamera = phone2.frontCamera.ifEmpty { "N/A" }
    comparisons["Front Camera"] = compareSpecValues(spec1FrontCamera, spec2FrontCamera)

    // Rear Camera
    val spec1RearCamera = phone1.rearCamera.ifEmpty { "N/A" }
    val spec2RearCamera = phone2.rearCamera.ifEmpty { "N/A" }
    comparisons["Rear Camera"] = compareSpecValues(spec1RearCamera, spec2RearCamera)

    // Chipset - can't meaningfully compare, always 0
    val spec1Chipset = phone1.chipset.ifEmpty { "N/A" }
    val spec2Chipset = phone2.chipset.ifEmpty { "N/A" }
    comparisons["Chipset"] = if (spec1Chipset == spec2Chipset) 0 else {
        // Can't compare chipsets numerically, just mark as different but no winner
        0
    }

    // Battery
    val spec1Battery = if (phone1.batteryCapacity > 0) "${formatter.format(phone1.batteryCapacity)} mAh" else "N/A"
    val spec2Battery = if (phone2.batteryCapacity > 0) "${formatter.format(phone2.batteryCapacity)} mAh" else "N/A"
    comparisons["Battery"] = compareSpecValues(spec1Battery, spec2Battery)

    // Charging
    val spec1Charging = if (phone1.wiredCharging > 0) "${phone1.wiredCharging}W" else "N/A"
    val spec2Charging = if (phone2.wiredCharging > 0) "${phone2.wiredCharging}W" else "N/A"
    comparisons["Charging"] = compareSpecValues(spec1Charging, spec2Charging)

    // OS - can't meaningfully compare
    val spec1Os = phone1.os.ifEmpty { "N/A" }
    val spec2Os = phone2.os.ifEmpty { "N/A" }
    comparisons["OS"] = if (spec1Os == spec2Os) 0 else 0  // Different but no winner

    // Network - can't meaningfully compare
    val spec1Network = phone1.network.ifEmpty { "N/A" }
    val spec2Network = phone2.network.ifEmpty { "N/A" }
    comparisons["Network"] = if (spec1Network == spec2Network) 0 else 0  // Different but no winner

    // RAM
    val spec1Ram = if (phone1.ram.isNotEmpty()) "${phone1.ram}GB" else "N/A"
    val spec2Ram = if (phone2.ram.isNotEmpty()) "${phone2.ram}GB" else "N/A"
    comparisons["RAM"] = compareSpecValues(spec1Ram, spec2Ram)

    // Storage
    val spec1Storage = if (phone1.storage.isNotEmpty()) "${phone1.storage}GB" else "N/A"
    val spec2Storage = if (phone2.storage.isNotEmpty()) "${phone2.storage}GB" else "N/A"
    comparisons["Storage"] = compareSpecValues(spec1Storage, spec2Storage)

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

    // Phone images state
    var phone1Images by remember { mutableStateOf<PhoneImages?>(null) }
    var phone2Images by remember { mutableStateOf<PhoneImages?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Compute spec comparison results (which phone has higher value for each spec)
    val specComparisons = remember(phone1, phone2) {
        computeSpecComparisons(phone1, phone2)
    }

    // Shared state for synchronized row heights across both columns
    // Key: spec label, Value: max height measured from either column
    val specRowHeights = remember { mutableStateMapOf<String, Dp>() }

    // Toggle state for showing/hiding comparison arrows
    var showComparisonArrows by remember { mutableStateOf(true) }

    // Callback to update row heights when measured
    val onSpecRowHeightMeasured: (String, Dp) -> Unit = { label, height ->
        val currentHeight = specRowHeights[label] ?: 0.dp
        if (height > currentHeight) {
            specRowHeights[label] = height
        }
    }

    // Fetch phone images
    LaunchedEffect(phone1.phoneDocId, phone2.phoneDocId) {
        try {
            val db = FirebaseFirestore.getInstance()

            // Fetch phone 1 images
            if (phone1.phoneDocId.isNotEmpty()) {
                val doc1 = db.collection("phone_images").document(phone1.phoneDocId).get().await()
                if (doc1.exists()) {
                    phone1Images = parsePhoneImagesDocumentComparison(doc1.id, doc1.data)
                }
            }

            // Fetch phone 2 images
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
            .padding(horizontal = 16.dp)
    ) {
        // TechCity logo at top center
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 50.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.tc_logo_flat_colored),
                contentDescription = "TechCity Logo",
                modifier = Modifier.height(36.dp),
                contentScale = ContentScale.FillHeight
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

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
                isLeftColumn = true,
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
                isLeftColumn = false,
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
    isLeftColumn: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val formatter = remember { NumberFormat.getNumberInstance(Locale.US) }

    // Selected color for image display
    var selectedColorName by remember(phone.phoneDocId) {
        mutableStateOf(phone.colors.firstOrNull() ?: "")
    }

    // CHANGE #2: Infinite alternating swipe
    // Use large page count with modulo 2 for infinite alternation
    val infinitePageCount = 10000
    val startPage = infinitePageCount / 2  // Start in the middle

    val pagerState = rememberPagerState(
        initialPage = startPage,
        pageCount = { infinitePageCount }
    )

    // Get actual view index (0 = specs, 1 = image)
    fun getActualViewIndex(page: Int): Int = page % 2

    // Format display name
    val displayName = remember(phone.manufacturer, phone.model) {
        if (phone.manufacturer.equals("Apple", ignoreCase = true)) {
            formatModelNameComparison(phone.model)
        } else {
            "${phone.manufacturer} ${formatModelNameComparison(phone.model)}"
        }
    }

    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Model name at top
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (phone.manufacturer.equals("Apple", ignoreCase = true)) {
                Image(
                    painter = painterResource(id = R.drawable.apple_logo),
                    contentDescription = "Apple logo",
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = displayName,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A),
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }

        Spacer(modifier = Modifier.height(45.dp))

        // Swipeable content area - infinite alternation
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { page ->
            when (getActualViewIndex(page)) {
                0 -> {
                    // Specs view
                    PhoneSpecsView(
                        phone = phone,
                        specComparisons = specComparisons,
                        specRowHeights = specRowHeights,
                        onSpecRowHeightMeasured = onSpecRowHeightMeasured,
                        showComparisonArrows = showComparisonArrows,
                        onToggleArrows = onToggleArrows,
                        isLeftColumn = isLeftColumn,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                1 -> {
                    // Image view
                    PhoneImageView(
                        phone = phone,
                        phoneImages = phoneImages,
                        selectedColorName = selectedColorName,
                        onColorSelected = { selectedColorName = it },
                        isLoading = isLoading,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Price - below specs
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (phone.retailPrice > 0) "₱${String.format("%,.2f", phone.retailPrice)}" else "N/A",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFDB2E2E)
        )
        Spacer(modifier = Modifier.height(60.dp))
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
    isLeftColumn: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val formatter = remember { NumberFormat.getNumberInstance(Locale.US) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            // Left column: start padding, arrow at end (close to separator)
            // Right column: arrow at start (close to separator), content centered, end padding
            .padding(start = if (isLeftColumn) 48.dp else 0.dp, end = if (isLeftColumn) 0.dp else 48.dp, top = 0.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
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
            isLeftColumn = isLeftColumn
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
            isLeftColumn = isLeftColumn
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
            isLeftColumn = isLeftColumn
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
            isLeftColumn = isLeftColumn
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
            isLeftColumn = isLeftColumn
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
            isLeftColumn = isLeftColumn
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
            isLeftColumn = isLeftColumn
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
            isLeftColumn = isLeftColumn
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
            isLeftColumn = isLeftColumn
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
            isLeftColumn = isLeftColumn
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
            isLeftColumn = isLeftColumn
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
            isLeftColumn = isLeftColumn
        )

        // Note: Price is shown separately at bottom of column, visible in both views
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Get image URL for selected color
    val colorImages = phoneImages?.getImagesForColor(selectedColorName)
    val imageUrl = colorImages?.highRes?.ifEmpty { colorImages.lowRes } ?: colorImages?.lowRes

    Column(
        modifier = modifier
            .fillMaxSize()
            .offset(y = (-40).dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Phone image - fixed height like PhoneDetailActivity
        Box(
            modifier = Modifier
                .height(500.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp))
            } else if (imageUrl != null) {
                // Check for cached image
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
                            scaleX = 1.15f
                            scaleY = 1.15f
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

        // Color name - minimal gap
        if (selectedColorName.isNotEmpty()) {
            Text(
                text = selectedColorName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF555555)
            )
        }

        // Color swatches - minimal gap
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
    comparisonResult: Int = 0,  // positive = phone1 higher, negative = phone2 higher, 0 = equal
    minHeight: Dp = 0.dp,
    onHeightMeasured: (Dp) -> Unit = {},
    showArrow: Boolean = true,
    onToggleArrow: () -> Unit = {},
    isLeftColumn: Boolean = true
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    // Arrow size - increased for better visibility
    val arrowSize = 32.dp
    // Fixed space allocated for arrow (consistent for both columns)
    val arrowSpaceWidth = arrowSize

    // Determine which arrow to show (if any)
    // comparisonResult > 0 means phone1 (left) has higher value
    // comparisonResult < 0 means phone2 (right) has higher value
    val arrowRes: Int? = when {
        comparisonResult == 0 -> null  // Equal, no arrow
        isLeftColumn && comparisonResult > 0 -> R.raw.up_arrow    // Left column, this phone is higher
        isLeftColumn && comparisonResult < 0 -> R.raw.down_arrow  // Left column, this phone is lower
        !isLeftColumn && comparisonResult < 0 -> R.raw.up_arrow   // Right column, this phone is higher
        !isLeftColumn && comparisonResult > 0 -> R.raw.down_arrow // Right column, this phone is lower
        else -> null
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = minHeight)
            .onGloballyPositioned { coordinates ->
                val measuredHeight = with(density) { coordinates.size.height.toDp() }
                onHeightMeasured(measuredHeight)
            }
            .clickable { onToggleArrow() }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isLeftColumn) {
            // LEFT COLUMN LAYOUT: Icon + Label/Value + Arrow at far right (close to separator)

            // Icon
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(iconRes)
                    .decoderFactory(SvgDecoder.Factory())
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = null,
                modifier = Modifier.size(42.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            // Label and Value
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF888888),
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    fontSize = if (isPrice) 20.sp else 15.sp,
                    fontWeight = if (isPrice) FontWeight.Bold else FontWeight.SemiBold,
                    color = valueColor,
                    lineHeight = if (isPrice) 24.sp else 18.sp,
                    maxLines = 2
                )
            }

            // Arrow space at far right (close to separator) - always present for consistent layout
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
                        modifier = Modifier.size(arrowSize)
                    )
                }
            }
        } else {
            // RIGHT COLUMN LAYOUT: Arrow at far left (close to separator) + CENTERED content

            // Arrow space at far left (close to separator) - always present for consistent layout
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
                        modifier = Modifier.size(arrowSize)
                    )
                }
            }

            // Content area - centered in remaining space
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icon
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(iconRes)
                            .decoderFactory(SvgDecoder.Factory())
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.size(42.dp)
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    // Label and Value
                    Column {
                        Text(
                            text = label,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF888888),
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = value,
                            fontSize = if (isPrice) 20.sp else 15.sp,
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
    onClick: () -> Unit
) {
    val backgroundColor = remember(hexColor, colorName) {
        if (hexColor.isNotEmpty()) {
            parseHexColorComparison(hexColor)
        } else {
            getColorFromNameComparison(colorName)
        }
    }

    // CHANGE #1: Consistent border color matching PhoneDetailActivity
    // Always use gray border (0xFFDDDDDD), just change size for selection
    Surface(
        modifier = Modifier
            .size(if (isSelected) 24.dp else 18.dp)
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