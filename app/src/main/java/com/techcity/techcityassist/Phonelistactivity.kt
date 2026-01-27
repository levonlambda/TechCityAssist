package com.techcity.techcityassist

import android.content.Intent
import androidx.compose.ui.layout.SubcomposeLayout
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.decode.SvgDecoder
import coil.ImageLoader
import coil.imageLoader
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.ColorFilter
import android.os.Bundle
import android.util.Log
import androidx.compose.ui.text.style.TextAlign
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.techcity.techcityassist.ui.theme.TechCityAssistTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit

// ============================================
// TEST MODE - Set to false for production
// ============================================
const val TEST_MODE = false
const val TEST_PHONE_DOC_ID = "ia2JilFWjlB2su72xY56"

// Add multiple inventory doc IDs here
val TEST_INVENTORY_DOC_IDS = listOf(
    "2S2C1bGKckqBMXBYjoUN",
    "4eKU2rU8ntAa71yDmSyM",
    "AsrLbThr0AV25TcjM5fx",
    "B2g3xu38zg29r54WMKgD",
    "Q6SnPYasmNBr9hUYwJ0u",
    "SxZUY7nnWm2Kn1xG0Fo5",
    "el3mzxh08kJoYm6i4p21",
    "p02OUgpbgn7reLdrSVRK",
    "sefplzk0hP8mcCO2Jj7U",
    "za5Je0wc5rjGNaTJqTFc"
    // Add more IDs as needed
)
// ============================================

// CHANGE #1: Cache for AutoSizeText computed font sizes - avoids repeated layout passes
private val autoSizeTextCache = mutableMapOf<String, TextUnit>()

// OPTIMIZATION #10: Pre-allocated elevation values - avoids creating new Dp objects on every recomposition
private val CARD_ELEVATION_ODD = 1.dp
private val CARD_ELEVATION_EVEN = 8.dp

// List of manufacturers for filter buttons
val MANUFACTURER_FILTERS = listOf(
    "All",
    "Apple",
    "Samsung",
    "Infinix",
    "Tecno",
    "Xiaomi",
    "Honor",
    "Vivo",
    "Itel",
    "Oppo",
    "Realme"
)

class PhoneListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Get device type from intent
        val deviceType = intent.getStringExtra("DEVICE_TYPE") ?: "phone"

        setContent {
            TechCityAssistTheme {
                MainScreen(deviceType = deviceType)
            }
        }
    }
}

// Data class for filter state
data class DisplayFilters(
    val showPhonesBrandNew: Boolean = true,
    val showPhonesRefurbished: Boolean = false,
    val showTablets: Boolean = false,
    val showLaptops: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(deviceType: String = "phone") {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    // Filter state - persisted across recompositions
    var filters by remember { mutableStateOf(DisplayFilters()) }

    // Manufacturer filter state
    var selectedManufacturer by remember { mutableStateOf("All") }

    // Dynamic manufacturer list based on device type (will be populated from data)
    var manufacturerFilters by remember { mutableStateOf(listOf("All")) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    // Horizontal scrollable row of manufacturer filter buttons
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(end = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        manufacturerFilters.forEach { manufacturer ->
                            FilterChip(
                                selected = selectedManufacturer == manufacturer,
                                onClick = { selectedManufacturer = manufacturer },
                                label = {
                                    Text(
                                        text = manufacturer,
                                        fontSize = 13.sp,
                                        fontWeight = if (selectedManufacturer == manufacturer) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF666666),
                                    selectedLabelColor = Color.White,
                                    containerColor = Color.White,
                                    labelColor = Color(0xFF333333)
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = Color(0xFFDDDDDD),
                                    selectedBorderColor = Color(0xFF666666),
                                    enabled = true,
                                    selected = selectedManufacturer == manufacturer
                                )
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFCF9F5),
                    titleContentColor = Color(0xFF333333),
                    actionIconContentColor = Color(0xFF333333)
                ),
                actions = {
                    // Kebab menu (three dots)
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu"
                        )
                    }

                    // Dropdown menu
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        // Only show phone filters when viewing phones
                        if (deviceType == "phone") {
                            // Filter Header
                            Text(
                                text = "Display Filters",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF6200EE),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )

                            // Phones (Brand New) checkbox
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = filters.showPhonesBrandNew,
                                            onCheckedChange = null,  // Let DropdownMenuItem handle clicks
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = Color(0xFF6200EE)
                                            )
                                        )
                                        Text("Phones (Brand New)")
                                    }
                                },
                                onClick = {
                                    filters = filters.copy(showPhonesBrandNew = !filters.showPhonesBrandNew)
                                }
                            )

                            // Phones (Refurbished) checkbox
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = filters.showPhonesRefurbished,
                                            onCheckedChange = null,
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = Color(0xFF6200EE)
                                            )
                                        )
                                        Text("Phones (Refurbished)")
                                    }
                                },
                                onClick = {
                                    filters = filters.copy(showPhonesRefurbished = !filters.showPhonesRefurbished)
                                }
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        }

                        DropdownMenuItem(
                            text = { Text("Image Management") },
                            onClick = {
                                showMenu = false
                                val intent = Intent(context, ImageManagementActivity::class.java)
                                context.startActivity(intent)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Clear Image Cache (${ImageCacheManager.getCacheSizeFormatted(context)})") },
                            onClick = {
                                showMenu = false
                                ImageCacheManager.clearCache(context)
                            }
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        PhoneListScreen(
            modifier = Modifier.padding(innerPadding),
            filters = filters,
            selectedManufacturer = selectedManufacturer,
            deviceType = deviceType,
            onManufacturersLoaded = { manufacturers ->
                manufacturerFilters = listOf("All") + manufacturers
            }
        )
    }
}

data class DeviceSpecs(
    val docId: String = "",
    val chipset: String = "",
    val frontCamera: String = "",
    val rearCamera: String = "",
    val battery: Int = 0,
    val os: String = "",
    val network: String = "",
    val display: String = "",
    val displaySize: String = "",
    val resolution: String = "",
    val refreshRate: Int = 0,
    val wiredCharging: Int = 0,
    val deviceType: String = "",
    val gpu: String = "",
    val cpu: String = ""
)

// ColorImageData is now defined in PhoneListHolder.kt

/**
 * Check if a phone should be displayed based on current filters
 */
fun shouldDisplayPhone(phone: Phone, filters: DisplayFilters): Boolean {
    val isRefurbished = phone.model.contains("refurbished", ignoreCase = true)
    val deviceType = phone.deviceType.lowercase().trim()

    return when {
        // If deviceType is empty/unknown, show based on all relevant filters
        deviceType.isEmpty() -> {
            // Show if any filter is enabled (don't hide items with unknown type)
            filters.showPhonesBrandNew || filters.showPhonesRefurbished ||
                    filters.showTablets || filters.showLaptops
        }
        // Check for Phones
        deviceType == "phone" -> {
            if (isRefurbished) {
                filters.showPhonesRefurbished
            } else {
                filters.showPhonesBrandNew
            }
        }
        // Check for Tablets
        deviceType == "tablet" -> filters.showTablets
        // Check for Laptops
        deviceType == "laptop" -> filters.showLaptops
        // Default: show if it's not explicitly filtered out
        else -> true
    }
}

/**
 * Check if a phone matches the selected manufacturer filter
 */
fun matchesManufacturerFilter(phone: Phone, selectedManufacturer: String): Boolean {
    if (selectedManufacturer == "All") return true
    return phone.manufacturer.equals(selectedManufacturer, ignoreCase = true)
}

/**
 * Navigate to Phone Detail Activity
 */
fun navigateToPhoneDetail(context: android.content.Context, phone: Phone, phoneIndex: Int = 0, selectedColor: String = "") {
    val intent = Intent(context, PhoneDetailActivity::class.java).apply {
        putExtra(PhoneDetailActivity.EXTRA_PHONE_INDEX, phoneIndex)
        putExtra(PhoneDetailActivity.EXTRA_PHONE_DOC_ID, phone.phoneDocId)
        putExtra(PhoneDetailActivity.EXTRA_MANUFACTURER, phone.manufacturer)
        putExtra(PhoneDetailActivity.EXTRA_MODEL, phone.model)
        putExtra(PhoneDetailActivity.EXTRA_RAM, phone.ram)
        putExtra(PhoneDetailActivity.EXTRA_STORAGE, phone.storage)
        putExtra(PhoneDetailActivity.EXTRA_RETAIL_PRICE, phone.retailPrice)
        putStringArrayListExtra(PhoneDetailActivity.EXTRA_COLORS, ArrayList(phone.colors))
        putExtra(PhoneDetailActivity.EXTRA_STOCK_COUNT, phone.stockCount)
        putExtra(PhoneDetailActivity.EXTRA_CHIPSET, phone.chipset)
        putExtra(PhoneDetailActivity.EXTRA_FRONT_CAMERA, phone.frontCamera)
        putExtra(PhoneDetailActivity.EXTRA_REAR_CAMERA, phone.rearCamera)
        putExtra(PhoneDetailActivity.EXTRA_BATTERY, phone.batteryCapacity)
        putExtra(PhoneDetailActivity.EXTRA_DISPLAY_TYPE, phone.displayType)
        putExtra(PhoneDetailActivity.EXTRA_DISPLAY_SIZE, phone.displaySize)
        putExtra(PhoneDetailActivity.EXTRA_OS, phone.os)
        putExtra(PhoneDetailActivity.EXTRA_NETWORK, phone.network)
        putExtra(PhoneDetailActivity.EXTRA_RESOLUTION, phone.resolution)
        putExtra(PhoneDetailActivity.EXTRA_REFRESH_RATE, phone.refreshRate)
        putExtra(PhoneDetailActivity.EXTRA_WIRED_CHARGING, phone.wiredCharging)
        putExtra(PhoneDetailActivity.EXTRA_DEVICE_TYPE, phone.deviceType)
        putExtra(PhoneDetailActivity.EXTRA_SELECTED_COLOR, selectedColor)
    }
    context.startActivity(intent)
}

@Composable
fun PhoneListScreen(
    modifier: Modifier = Modifier,
    filters: DisplayFilters = DisplayFilters(),
    selectedManufacturer: String = "All",
    deviceType: String = "phone",
    onManufacturersLoaded: (List<String>) -> Unit = {}
) {
    val context = LocalContext.current
    var phones by remember { mutableStateOf<List<Phone>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Track if we used cached data
    var usedCache by remember { mutableStateOf(false) }

    // State for showing doc IDs dialog (on long press)
    var selectedPhoneForDocIds by remember { mutableStateOf<Phone?>(null) }

    // Store phone images for all phones (phoneDocId -> PhoneImages)
    var phoneImagesMap by remember { mutableStateOf<Map<String, PhoneImages>>(emptyMap()) }

    // Create optimized ImageLoader with larger cache
    val imageLoader = remember {
        context.imageLoader.newBuilder()
            .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
            .diskCachePolicy(coil.request.CachePolicy.ENABLED)
            .crossfade(false) // Disable crossfade for smoother scrolling
            .build()
    }

    // Preload all images in background after data is loaded
    LaunchedEffect(phones, phoneImagesMap) {
        if (phones.isNotEmpty() && phoneImagesMap.isNotEmpty()) {
            phones.forEach { phone ->
                val phoneImages = phoneImagesMap[phone.phoneDocId]
                phone.colors.forEach { colorName ->
                    val images = phoneImages?.getImagesForColor(colorName)
                    val remoteUrl = images?.lowRes?.ifEmpty { images.highRes }

                    if (!remoteUrl.isNullOrEmpty()) {
                        val isHighRes = images?.lowRes.isNullOrEmpty() == true
                        // Check if already cached
                        if (!ImageCacheManager.isImageCached(context, phone.phoneDocId, colorName, isHighRes)) {
                            ImageCacheManager.downloadAndCacheImage(
                                context = context,
                                imageUrl = remoteUrl,
                                phoneDocId = phone.phoneDocId,
                                colorName = colorName,
                                isHighRes = isHighRes
                            )
                        }
                    }
                }
            }
        }
    }

    // Apply filters to the phone list and sort:
    // 1. Filter by device type first
    // 2. Exclude TechCity manufacturer
    // 3. Group by model (manufacturer + model name)
    // 4. Sort variants within each model by price (lowest to highest)
    // 5. Sort model groups by their lowest price

    // First filter by device type to get the base list - memoized
    val deviceTypeFiltered = remember(phones, deviceType) {
        phones.filter { phone ->
            phone.deviceType.equals(deviceType, ignoreCase = true)
        }
    }

    // Extract unique manufacturers for this device type and notify parent
    LaunchedEffect(deviceTypeFiltered) {
        val manufacturers = deviceTypeFiltered
            .map { it.manufacturer }
            .filter { !it.equals("techcity", ignoreCase = true) }
            .distinct()
            .sorted()
        onManufacturersLoaded(manufacturers)
    }

    // Memoized filtering - only recalculates when inputs change
    val filteredPhones = remember(deviceTypeFiltered, selectedManufacturer, filters, deviceType) {
        deviceTypeFiltered
            .filter { phone ->
                // Exclude TechCity manufacturer
                !phone.manufacturer.equals("techcity", ignoreCase = true) &&
                        matchesManufacturerFilter(phone, selectedManufacturer) &&
                        // Apply brand new/refurbished filter only for phones
                        (deviceType != "phone" || run {
                            val isRefurbished = phone.model.contains("refurbished", ignoreCase = true)
                            if (isRefurbished) filters.showPhonesRefurbished else filters.showPhonesBrandNew
                        })
            }
            .groupBy { "${it.manufacturer}|${it.model}" }  // Group by model
            .map { (_, variants) ->
                // Sort variants within each model by price
                variants.sortedBy { it.retailPrice }
            }
            .sortedBy { variants ->
                // Sort model groups by their lowest price (first item after sorting)
                variants.firstOrNull()?.retailPrice ?: Double.MAX_VALUE
            }
            .flatten()  // Flatten back to a single list
    }

    // Update PhoneListHolder whenever filteredPhones or phoneImagesMap changes
    LaunchedEffect(filteredPhones, phoneImagesMap) {
        PhoneListHolder.filteredPhones = filteredPhones
        PhoneListHolder.phoneImagesMap = phoneImagesMap
    }

    // ============================================
    // DATA LOADING - CHECK CACHE FIRST
    // ============================================
    LaunchedEffect(Unit) {
        // CHECK IF WE HAVE CACHED DATA
        if (PhoneListHolder.isSynced && PhoneListHolder.allDevices.isNotEmpty()) {
            Log.d("PhoneList", "Using cached data (${PhoneListHolder.allDevices.size} devices)")
            usedCache = true

            // Use cached data - instant!
            phones = PhoneListHolder.allDevices
            phoneImagesMap = PhoneListHolder.allPhoneImages
            isLoading = false
            return@LaunchedEffect
        }

        // NO CACHED DATA - Fetch from Firebase (original behavior)
        Log.d("PhoneList", "No cached data, fetching from Firebase...")
        usedCache = false

        val db = FirebaseFirestore.getInstance()

        if (TEST_MODE) {
            db.collection("phones").document(TEST_PHONE_DOC_ID)
                .get()
                .addOnSuccessListener { phoneDoc ->
                    val specs = if (phoneDoc.exists()) {
                        DeviceSpecs(
                            docId = phoneDoc.id,
                            chipset = phoneDoc.getString("chipset") ?: "",
                            frontCamera = phoneDoc.getString("frontCamera") ?: "",
                            rearCamera = phoneDoc.getString("rearCamera") ?: "",
                            battery = phoneDoc.getLong("battery")?.toInt() ?: 0,
                            os = phoneDoc.getString("os") ?: "",
                            network = phoneDoc.getString("network") ?: "",
                            display = phoneDoc.getString("display") ?: "",
                            displaySize = phoneDoc.getString("displaySize") ?: "",
                            resolution = phoneDoc.getString("resolution") ?: "",
                            refreshRate = phoneDoc.getLong("resolution_extra")?.toInt() ?: 0,
                            wiredCharging = phoneDoc.getLong("wiredCharging")?.toInt() ?: 0,
                            deviceType = phoneDoc.getString("deviceType") ?: "",
                            gpu = phoneDoc.getString("gpu") ?: "",
                            cpu = phoneDoc.getString("cpu") ?: ""
                        )
                    } else {
                        DeviceSpecs()
                    }

                    val inventoryItems = mutableListOf<Map<String, Any>>()
                    var fetchedCount = 0

                    TEST_INVENTORY_DOC_IDS.forEach { docId ->
                        db.collection("inventory").document(docId)
                            .get()
                            .addOnSuccessListener { invDoc ->
                                fetchedCount++

                                if (invDoc.exists()) {
                                    inventoryItems.add(mapOf(
                                        "docId" to invDoc.id,
                                        "manufacturer" to (invDoc.getString("manufacturer") ?: ""),
                                        "model" to (invDoc.getString("model") ?: ""),
                                        "ram" to (invDoc.getString("ram") ?: ""),
                                        "storage" to (invDoc.getString("storage") ?: ""),
                                        "color" to (invDoc.getString("color") ?: ""),
                                        "retailPrice" to (invDoc.getDouble("retailPrice") ?: 0.0),
                                        "dealersPrice" to (invDoc.getDouble("dealersPrice") ?: 0.0)
                                    ))
                                }

                                if (fetchedCount == TEST_INVENTORY_DOC_IDS.size) {
                                    if (inventoryItems.isNotEmpty()) {
                                        val phoneList = inventoryItems
                                            .groupBy { "${it["ram"]}|${it["storage"]}" }
                                            .map { (_, variantItems) ->
                                                val firstItem = variantItems.first()
                                                val colors = variantItems
                                                    .map { it["color"] as String }
                                                    .distinct()
                                                    .filter { it.isNotEmpty() }

                                                Phone(
                                                    manufacturer = firstItem["manufacturer"] as String,
                                                    model = firstItem["model"] as String,
                                                    ram = firstItem["ram"] as String,
                                                    storage = firstItem["storage"] as String,
                                                    retailPrice = firstItem["retailPrice"] as Double,
                                                    colors = colors,
                                                    stockCount = variantItems.size,
                                                    chipset = specs.chipset,
                                                    frontCamera = specs.frontCamera,
                                                    rearCamera = specs.rearCamera,
                                                    batteryCapacity = specs.battery,
                                                    displayType = specs.display,
                                                    displaySize = specs.displaySize,
                                                    os = specs.os,
                                                    network = specs.network,
                                                    resolution = specs.resolution,
                                                    refreshRate = specs.refreshRate,
                                                    wiredCharging = specs.wiredCharging,
                                                    inventoryDocIds = variantItems.map { it["docId"] as String },
                                                    phoneDocId = specs.docId,
                                                    variants = emptyList(),
                                                    deviceType = specs.deviceType,
                                                    gpu = specs.gpu,
                                                    cpu = specs.cpu
                                                )
                                            }
                                            .sortedBy { it.retailPrice }

                                        phones = phoneList

                                        // Fetch phone_images for this phone
                                        fetchPhoneImages(db, specs.docId) { images ->
                                            if (images != null) {
                                                phoneImagesMap = mapOf(specs.docId to images)
                                            }
                                            isLoading = false
                                        }
                                    } else {
                                        isLoading = false
                                    }
                                }
                            }
                            .addOnFailureListener { e ->
                                fetchedCount++
                                Log.e("Firestore", "Error getting inventory doc: $docId", e)
                                if (fetchedCount == TEST_INVENTORY_DOC_IDS.size) {
                                    isLoading = false
                                }
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Error getting test phone", e)
                    isLoading = false
                }
        } else {
            db.collection("phones")
                .get()
                .addOnSuccessListener { phonesResult ->
                    val specsMap = phonesResult.documents.associate { doc ->
                        val key = "${doc.getString("manufacturer") ?: ""}|${doc.getString("model") ?: ""}"
                        key to DeviceSpecs(
                            docId = doc.id,
                            chipset = doc.getString("chipset") ?: "",
                            frontCamera = doc.getString("frontCamera") ?: "",
                            rearCamera = doc.getString("rearCamera") ?: "",
                            battery = doc.getLong("battery")?.toInt() ?: 0,
                            os = doc.getString("os") ?: "",
                            network = doc.getString("network") ?: "",
                            display = doc.getString("display") ?: "",
                            displaySize = doc.getString("displaySize") ?: "",
                            resolution = doc.getString("resolution") ?: "",
                            refreshRate = doc.getLong("resolution_extra")?.toInt() ?: 0,
                            wiredCharging = doc.getLong("wiredCharging")?.toInt() ?: 0,
                            deviceType = doc.getString("deviceType") ?: "",
                            gpu = doc.getString("gpu") ?: "",
                            cpu = doc.getString("cpu") ?: ""
                        )
                    }

                    db.collection("inventory")
                        .whereIn("status", listOf("On-Hand", "On-Display"))
                        .get()
                        .addOnSuccessListener { inventoryResult ->
                            val grouped = inventoryResult.documents
                                .mapNotNull { doc ->
                                    val manufacturer = doc.getString("manufacturer") ?: return@mapNotNull null
                                    val model = doc.getString("model") ?: return@mapNotNull null
                                    val ram = doc.getString("ram") ?: ""
                                    val storage = doc.getString("storage") ?: ""
                                    val color = doc.getString("color") ?: ""
                                    val retailPrice = doc.getDouble("retailPrice") ?: 0.0
                                    val dealersPrice = doc.getDouble("dealersPrice") ?: 0.0
                                    val docId = doc.id

                                    val key = "$manufacturer|$model|$ram|$storage"
                                    Pair(key, mapOf(
                                        "manufacturer" to manufacturer,
                                        "model" to model,
                                        "ram" to ram,
                                        "storage" to storage,
                                        "color" to color,
                                        "retailPrice" to retailPrice,
                                        "dealersPrice" to dealersPrice,
                                        "docId" to docId
                                    ))
                                }
                                .groupBy({ it.first }, { it.second })
                                .map { (key, items) ->
                                    val parts = key.split("|")
                                    val manufacturer = parts[0]
                                    val model = parts[1]
                                    val ram = parts[2]
                                    val storage = parts[3]

                                    val colors = items.map { it["color"] as String }.distinct().filter { it.isNotEmpty() }
                                    val inventoryDocIds = items.map { it["docId"] as String }
                                    val retailPrice = items.firstOrNull()?.get("retailPrice") as? Double ?: 0.0

                                    val specsKey = "$manufacturer|$model"
                                    val specs = specsMap[specsKey] ?: DeviceSpecs()

                                    Phone(
                                        manufacturer = manufacturer,
                                        model = model,
                                        ram = ram,
                                        storage = storage,
                                        retailPrice = retailPrice,
                                        colors = colors,
                                        stockCount = items.size,
                                        chipset = specs.chipset,
                                        frontCamera = specs.frontCamera,
                                        rearCamera = specs.rearCamera,
                                        batteryCapacity = specs.battery,
                                        displayType = specs.display,
                                        displaySize = specs.displaySize,
                                        os = specs.os,
                                        network = specs.network,
                                        resolution = specs.resolution,
                                        refreshRate = specs.refreshRate,
                                        wiredCharging = specs.wiredCharging,
                                        inventoryDocIds = inventoryDocIds,
                                        phoneDocId = specs.docId,
                                        variants = emptyList(),
                                        deviceType = specs.deviceType,
                                        gpu = specs.gpu,
                                        cpu = specs.cpu
                                    )
                                }
                                .sortedWith(compareBy({ it.manufacturer }, { it.model }, { it.retailPrice }))

                            phones = grouped

                            // Fetch all phone images
                            val phoneDocIds = grouped.mapNotNull {
                                it.phoneDocId.ifEmpty { null }
                            }.distinct()

                            if (phoneDocIds.isNotEmpty()) {
                                fetchAllPhoneImages(db, phoneDocIds) { imagesMap ->
                                    phoneImagesMap = imagesMap
                                    isLoading = false
                                }
                            } else {
                                isLoading = false
                            }
                        }
                        .addOnFailureListener { exception ->
                            Log.e("Firestore", "Error getting inventory", exception)
                            isLoading = false
                        }
                }
                .addOnFailureListener { exception ->
                    Log.e("Firestore", "Error getting phones specs", exception)
                    isLoading = false
                }
        }
    }

    // Dialog for showing document IDs (on long press)
    if (selectedPhoneForDocIds != null) {
        AlertDialog(
            onDismissRequest = { selectedPhoneForDocIds = null },
            title = { Text("Document IDs") },
            text = {
                SelectionContainer {
                    Column {
                        Text(
                            text = "${selectedPhoneForDocIds!!.manufacturer} ${selectedPhoneForDocIds!!.model}",
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(text = "Phones Collection (specs):", fontWeight = FontWeight.Medium)
                        Text(
                            text = selectedPhoneForDocIds!!.phoneDocId.ifEmpty { "Not found" },
                            fontSize = 12.sp,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(text = "Inventory Collection:", fontWeight = FontWeight.Medium)
                        selectedPhoneForDocIds!!.inventoryDocIds.forEach { docId ->
                            Text(
                                text = docId,
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(text = "Device Type:", fontWeight = FontWeight.Medium)
                        Text(
                            text = selectedPhoneForDocIds!!.deviceType.ifEmpty { "Phone" },
                            fontSize = 12.sp,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(text = "Data Source:", fontWeight = FontWeight.Medium)
                        Text(
                            text = if (usedCache) "Cached (fast)" else "Firebase (live)",
                            fontSize = 12.sp,
                            color = if (usedCache) Color(0xFF4CAF50) else Color(0xFFFF9800)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedPhoneForDocIds = null }) {
                    Text("Close")
                }
            }
        )
    }

    if (isLoading) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFFFCF9F5)),  // Warm off-white background
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Loading ${deviceType}s...",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                if (!PhoneListHolder.isSynced) {
                    Text(
                        text = "Tip: Use SYNC on home screen for faster loading",
                        fontSize = 12.sp,
                        color = Color(0xFF999999)
                    )
                }
            }
        }
    } else if (filteredPhones.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFFFCF9F5)),  // Warm off-white background
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No ${deviceType}s found")
                Text(
                    text = "Try selecting a different manufacturer",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFFFCF9F5))  // Warm off-white background
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(
                items = filteredPhones,
                key = { _, phone -> "${phone.phoneDocId}_${phone.ram}_${phone.storage}" }
            ) { index, phone ->
                PhoneCard(
                    phone = phone,
                    phoneImages = phoneImagesMap[phone.phoneDocId],
                    imageLoader = imageLoader,
                    onClick = { selectedColor ->
                        // Navigate to detail activity with unique model index and selected color
                        val uniqueIndex = PhoneListHolder.getUniqueModelIndex(phone)
                        navigateToPhoneDetail(context, phone, uniqueIndex, selectedColor)
                    },
                    onLongClick = {
                        // Show doc IDs dialog
                        selectedPhoneForDocIds = phone
                    },
                    isAlternate = index % 2 == 1,
                    initialColorIndex = index % 2  // Alternate starting color
                )
            }
        }
    }
}

// ============================================
// Fetch Phone Images Functions
// ============================================

fun fetchPhoneImages(
    db: FirebaseFirestore,
    phoneDocId: String,
    onComplete: (PhoneImages?) -> Unit
) {
    db.collection("phone_images").document(phoneDocId)
        .get()
        .addOnSuccessListener { document ->
            if (document.exists()) {
                val phoneImages = parsePhoneImagesDocument(document.id, document.data)
                onComplete(phoneImages)
            } else {
                onComplete(null)
            }
        }
        .addOnFailureListener { e ->
            Log.e("PhoneImages", "Error fetching images for: $phoneDocId", e)
            onComplete(null)
        }
}

fun fetchAllPhoneImages(
    db: FirebaseFirestore,
    phoneDocIds: List<String>,
    onComplete: (Map<String, PhoneImages>) -> Unit
) {
    val result = mutableMapOf<String, PhoneImages>()
    var completedCount = 0

    if (phoneDocIds.isEmpty()) {
        onComplete(emptyMap())
        return
    }

    phoneDocIds.forEach { docId ->
        db.collection("phone_images").document(docId)
            .get()
            .addOnSuccessListener { document ->
                completedCount++
                if (document.exists()) {
                    val phoneImages = parsePhoneImagesDocument(document.id, document.data)
                    if (phoneImages != null) {
                        result[docId] = phoneImages
                    }
                }
                if (completedCount == phoneDocIds.size) {
                    onComplete(result)
                }
            }
            .addOnFailureListener { e ->
                completedCount++
                Log.e("PhoneImages", "Error fetching images for: $docId", e)
                if (completedCount == phoneDocIds.size) {
                    onComplete(result)
                }
            }
    }
}

@Suppress("UNCHECKED_CAST")
fun parsePhoneImagesDocument(docId: String, data: Map<String, Any>?): PhoneImages? {
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
        Log.e("PhoneImages", "Error parsing phone images document: $docId", e)
        null
    }
}

// ============================================
// Phone Card with Swipeable Images
// ============================================

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhoneCard(
    phone: Phone,
    phoneImages: PhoneImages? = null,
    imageLoader: ImageLoader,
    onClick: (selectedColorName: String) -> Unit,
    onLongClick: () -> Unit = {},
    isAlternate: Boolean = false,
    initialColorIndex: Int = 0
) {
    val formatter = remember { NumberFormat.getNumberInstance(Locale.US) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Memoize the formatted price
    val formattedPrice = remember(phone.retailPrice) {
        "\u20B1${String.format("%,.2f", phone.retailPrice)}"
    }

    // Memoize display name
    val displayName = remember(phone.manufacturer, phone.model) {
        if (phone.manufacturer.equals("Apple", ignoreCase = true)) {
            formatModelName(phone.model)
        } else {
            "${phone.manufacturer} ${formatModelName(phone.model)}"
        }
    }

    // Use precomputed color data from sync (avoids filesystem I/O during scroll)
    // Falls back to computing on-the-fly if precomputed data not available
    val precomputedKey = PhoneListHolder.getColorDataKey(phone)
    var colorsWithImages by remember(phone.phoneDocId, phone.ram, phone.storage) {
        mutableStateOf(
            PhoneListHolder.precomputedColorData[precomputedKey]
                ?: phone.colors.map { colorName ->
                    val images = phoneImages?.getImagesForColor(colorName)
                    val remoteUrl = images?.lowRes?.ifEmpty { images.highRes }
                    ColorImageData(
                        colorName = colorName,
                        imageUrl = remoteUrl,
                        hexColor = images?.hexColor ?: "",
                        remoteUrl = remoteUrl,
                        isCached = false
                    )
                }
        )
    }

    // Pager state for swiping between images
    // Use initialColorIndex to alternate starting colors between adjacent cards
    // Use large page count for infinite loop effect
    val actualColorCount = colorsWithImages.size
    val infinitePageCount = if (actualColorCount > 1) 10000 else 1
    val startPage = if (actualColorCount > 1) {
        // Start in the middle, adjusted for initialColorIndex
        (infinitePageCount / 2) - ((infinitePageCount / 2) % actualColorCount) + initialColorIndex.coerceIn(0, actualColorCount - 1)
    } else {
        0
    }

    val pagerState = rememberPagerState(
        initialPage = startPage,
        pageCount = { infinitePageCount }
    )

    // Get actual color index from page (for infinite scroll)
    fun getActualColorIndex(page: Int): Int {
        return if (actualColorCount > 0) page % actualColorCount else 0
    }

    // Note: LaunchedEffect for image caching removed - images are now preloaded during sync
    // (see MainActivity.syncAllData and PhoneListHolder.precomputedColorData)
    // This eliminates coroutine launches and list operations during scroll

    // State for re-download dialog
    var showRedownloadDialog by remember { mutableStateOf(false) }
    var isRedownloading by remember { mutableStateOf(false) }
    var redownloadProgress by remember { mutableStateOf("") }

    // Re-download dialog
    if (showRedownloadDialog) {
        // Check if we have any remote URLs to download
        val hasRemoteUrls = colorsWithImages.any { !it.remoteUrl.isNullOrEmpty() }
        val colorsWithoutUrls = colorsWithImages.filter { it.remoteUrl.isNullOrEmpty() }.map { it.colorName }
        val availableImageColors = phoneImages?.getAvailableColors() ?: emptyList()

        AlertDialog(
            onDismissRequest = {
                if (!isRedownloading) showRedownloadDialog = false
            },
            title = { Text("Refresh Images") },
            text = {
                Column {
                    Text("${phone.manufacturer} ${phone.model}")
                    Text("Phone Doc ID: ${phone.phoneDocId.ifEmpty { "MISSING!" }}", fontSize = 10.sp, color = if (phone.phoneDocId.isEmpty()) Color.Red else Color.Gray)
                    Text("Phone Images: ${if (phoneImages != null) "Found" else "NOT FOUND"}", fontSize = 10.sp, color = if (phoneImages == null) Color.Red else Color.Gray)
                    if (availableImageColors.isNotEmpty()) {
                        Text("Image colors: ${availableImageColors.joinToString(", ")}", fontSize = 10.sp, color = Color.Gray)
                    }
                    Text("Inventory colors: ${phone.colors.joinToString(", ")}", fontSize = 10.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (isRedownloading) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Text(redownloadProgress, fontSize = 14.sp, color = Color.Gray)
                        }
                    } else if (!hasRemoteUrls) {
                        Text(
                            "No image URLs found for this phone. Make sure images are uploaded in Image Management.",
                            fontSize = 14.sp,
                            color = Color(0xFFE65100)
                        )
                        if (colorsWithoutUrls.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Colors without URLs: ${colorsWithoutUrls.joinToString(", ")}",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    } else {
                        Text(
                            "This will delete cached images and re-download them from the server.",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        if (colorsWithoutUrls.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Note: No URLs for: ${colorsWithoutUrls.joinToString(", ")}",
                                fontSize = 11.sp,
                                color = Color(0xFFFF9800)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            isRedownloading = true

                            redownloadProgress = "Clearing cache..."

                            // Clear Coil's memory cache for these images
                            colorsWithImages.forEach { colorData ->
                                val cacheKey = "${phone.phoneDocId}_${colorData.colorName}_${colorData.cacheVersion}"
                                imageLoader.memoryCache?.remove(MemoryCache.Key(cacheKey))
                            }

                            // Delete local cached files
                            colorsWithImages.forEach { colorData ->
                                val isHighRes = phoneImages?.getImagesForColor(colorData.colorName)?.lowRes.isNullOrEmpty() == true
                                val file = ImageCacheManager.getLocalFilePath(
                                    context,
                                    phone.phoneDocId,
                                    colorData.colorName,
                                    isHighRes
                                )
                                if (file.exists()) file.delete()

                                val otherFile = ImageCacheManager.getLocalFilePath(
                                    context,
                                    phone.phoneDocId,
                                    colorData.colorName,
                                    !isHighRes
                                )
                                if (otherFile.exists()) otherFile.delete()
                            }

                            val total = colorsWithImages.size
                            val updatedColors = colorsWithImages.toMutableList()
                            val newCacheVersion = System.currentTimeMillis()

                            colorsWithImages.forEachIndexed { index, colorData ->
                                if (!colorData.remoteUrl.isNullOrEmpty()) {
                                    redownloadProgress = "Downloading ${index + 1}/$total..."

                                    val isHighRes = phoneImages?.getImagesForColor(colorData.colorName)?.lowRes.isNullOrEmpty() == true
                                    val cachedPath = ImageCacheManager.downloadAndCacheImage(
                                        context = context,
                                        imageUrl = colorData.remoteUrl,
                                        phoneDocId = phone.phoneDocId,
                                        colorName = colorData.colorName,
                                        isHighRes = isHighRes ?: false
                                    )

                                    // Update the color data with new cached path and cache version
                                    if (cachedPath != null) {
                                        updatedColors[index] = colorData.copy(
                                            imageUrl = cachedPath,
                                            isCached = true,
                                            cacheVersion = newCacheVersion
                                        )
                                    }
                                }
                            }

                            // Update the state with all the new cached paths
                            colorsWithImages = updatedColors

                            redownloadProgress = "Complete!"
                            delay(500)
                            isRedownloading = false
                            showRedownloadDialog = false
                        }
                    },
                    enabled = !isRedownloading
                ) {
                    Text("Refresh")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRedownloadDialog = false },
                    enabled = !isRedownloading
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Alternate card background colors for visual distinction
    val cardBackgroundColor = Color.White

    // OPTIMIZATION #10: Use pre-allocated constants instead of creating new Dp objects
    val cardElevation = if (isAlternate) CARD_ELEVATION_ODD else CARD_ELEVATION_EVEN

    // Get actual screen width to determine if SpecItems will stack vertically
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp

    // SpecItem switches to vertical layout when its width < 85dp
    // Using 800dp threshold to test - will make ALL screens use taller card
    val useVerticalSpecLayout = screenWidthDp < 800.dp

    // Increase card height when specs stack vertically
    val cardHeight = if (useVerticalSpecLayout) 330.dp else 300.dp

    // Determine padding for spec rows based on layout
    val specRowStartPadding = if (useVerticalSpecLayout) 16.dp else 48.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight)
            .combinedClickable(
                onClick = {
                    val currentColorName = colorsWithImages.getOrNull(getActualColorIndex(pagerState.currentPage))?.colorName ?: ""
                    onClick(currentColorName)
                },
                onLongClick = {
                    // Show both the re-download dialog and doc IDs on long press
                    showRedownloadDialog = true
                    onLongClick()
                }
            ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // Left side - All info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top section - Model name with optional Apple logo
                Row(
                    modifier = Modifier.padding(start = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (phone.manufacturer.equals("Apple", ignoreCase = true)) {
                        Image(
                            painter = painterResource(id = R.drawable.apple_logo),
                            contentDescription = "Apple logo",
                            modifier = Modifier.size(42.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                    }
                    AutoSizeText(
                        text = displayName,
                        maxFontSize = 28.sp,
                        minFontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A),
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Check if device is a laptop
                val isLaptop = phone.deviceType.equals("laptop", ignoreCase = true)

                if (isLaptop) {
                    // LAPTOP LAYOUT - Checkerboard pattern
                    // First row: OS, Display Size, Resolution (3 items)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = specRowStartPadding, end = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(32.dp)
                    ) {
                        LaptopSpecItem(label = "OS", value = phone.os.ifEmpty { "N/A" }, iconRes = R.raw.os_icon, modifier = Modifier.weight(1f))
                        LaptopSpecItem(label = "Display Size", value = if (phone.displaySize.isNotEmpty()) "${phone.displaySize} Inches" else "N/A", iconRes = R.raw.screen_size_icon, modifier = Modifier.weight(1f))
                        LaptopSpecItem(label = "Resolution", value = phone.resolution.ifEmpty { "N/A" }, iconRes = R.raw.resolution_icon, modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Second row: CPU, GPU (2 items centered in checkerboard pattern)
                    // GPU gets more width since GPU names tend to be longer
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = specRowStartPadding, end = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Minimal spacer
                        Spacer(modifier = Modifier.weight(0.15f))
                        LaptopSpecItemLarge(label = "CPU", value = phone.cpu.ifEmpty { "N/A" }, iconRes = R.raw.chipset_icon, modifier = Modifier.weight(0.9f))
                        LaptopSpecItemLarge(label = "GPU", value = phone.gpu.ifEmpty { "N/A" }, iconRes = R.raw.gpu_icon, modifier = Modifier.weight(1.2f))
                        // Minimal spacer to balance
                        Spacer(modifier = Modifier.weight(0.15f))
                    }
                } else {
                    // PHONE/TABLET LAYOUT - 4 specs per row
                    // First row: OS, Battery, Front Cam, Rear Cam
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = specRowStartPadding, end = 0.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        SpecItem(label = "OS", value = phone.os.ifEmpty { "N/A" }, iconRes = R.raw.os_icon, useVerticalLayout = useVerticalSpecLayout, modifier = Modifier.weight(1f))
                        SpecItem(label = "Battery", value = if (phone.batteryCapacity > 0) "${formatter.format(phone.batteryCapacity)} mAh" else "N/A", iconRes = R.raw.battery_icon, useVerticalLayout = useVerticalSpecLayout, modifier = Modifier.weight(1f))
                        SpecItem(label = "Front Cam", value = phone.frontCamera.ifEmpty { "N/A" }, iconRes = R.raw.camera_icon, useVerticalLayout = useVerticalSpecLayout, modifier = Modifier.weight(1f))
                        SpecItem(label = "Rear Cam", value = if (useVerticalSpecLayout) phone.rearCamera.replace(" ", "").ifEmpty { "N/A" } else phone.rearCamera.ifEmpty { "N/A" }, iconRes = R.raw.rear_camera_icon, useVerticalLayout = useVerticalSpecLayout, modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Second row: Display Size, Refresh Rate, Charging, Network
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = specRowStartPadding, end = 0.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        SpecItem(label = "Display Size", value = if (phone.displaySize.isNotEmpty()) "${phone.displaySize} Inches" else "N/A", iconRes = R.raw.screen_size_icon, useVerticalLayout = useVerticalSpecLayout, modifier = Modifier.weight(1f))
                        SpecItem(label = "Refresh Rate", value = if (phone.refreshRate > 0) "${phone.refreshRate} Hz" else "N/A", iconRes = R.raw.refresh_rate_icon, useVerticalLayout = useVerticalSpecLayout, modifier = Modifier.weight(1f))
                        SpecItem(label = "Charging", value = if (phone.wiredCharging > 0) "${phone.wiredCharging}W" else "N/A", iconRes = R.raw.charging_icon, useVerticalLayout = useVerticalSpecLayout, modifier = Modifier.weight(1f))
                        SpecItem(label = "Network", value = phone.network.ifEmpty { "N/A" }, iconRes = R.raw.network_icon, useVerticalLayout = useVerticalSpecLayout, modifier = Modifier.weight(1f))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Bottom section - RAM, Storage, Price (responsive font sizing)
                // OPTIMIZATION: Replaced BoxWithConstraints with calculated width
                // This eliminates subcomposition overhead (extra layout pass per card)

                // Calculate estimated content width from known layout values
                // Layout: Screen -> LazyColumn(padding=16dp) -> Card(padding=20dp) -> Row[InfoColumn(weight=1f), ImageColumn(~148dp)]
                val screenConfig = LocalConfiguration.current
                val screenWidth = screenConfig.screenWidthDp.dp
                val useSmallLayout = screenWidth < 800.dp
                val startPadding = if (useSmallLayout) 24.dp else 48.dp

                // Estimate the available width for this section:
                // screenWidth - lazyColumnPadding(32dp) - cardPadding(40dp) - imageColumn(148dp) - startPadding
                val estimatedContentWidth = screenWidth - 32.dp - 40.dp - 148.dp - startPadding

                // Calculate font scale based on estimated width (same logic as before)
                val scaleFactor = (estimatedContentWidth / 600.dp).coerceAtMost(1f)
                val ramStorageFontSize = (13 * scaleFactor).coerceAtLeast(11f).sp
                val priceFontSize = (24 * scaleFactor).coerceAtLeast(20f).sp
                val chipPaddingH = (14 * scaleFactor).coerceAtLeast(8f).dp
                val chipPaddingV = (8 * scaleFactor).coerceAtLeast(4f).dp

                // Reduce spacing when using vertical spec layout (smaller screens)
                val useVerticalSpecLayoutBottom = estimatedContentWidth < 340.dp
                val storagePriceSpacing = if (useVerticalSpecLayoutBottom) 0.dp else 20.dp

                // Adjust weights to give price more space on smaller screens
                val ramStorageWeight = if (useVerticalSpecLayoutBottom) 1f else 3f
                val priceWeight = if (useVerticalSpecLayoutBottom) 3f else 1.5f

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = startPadding, end = 0.dp),
                    horizontalArrangement = Arrangement.spacedBy(storagePriceSpacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // First 3 columns worth of space for RAM and Storage
                    Row(
                        modifier = Modifier.weight(ramStorageWeight),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.White,
                            modifier = Modifier.border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(6.dp))
                        ) {
                            Text(
                                text = "${phone.ram}GB RAM",
                                modifier = Modifier.padding(horizontal = chipPaddingH, vertical = chipPaddingV),
                                fontSize = ramStorageFontSize,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF555555)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.White,
                            modifier = Modifier.border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(6.dp))
                        ) {
                            Text(
                                text = "${phone.storage}GB Storage",
                                modifier = Modifier.padding(horizontal = chipPaddingH, vertical = chipPaddingV),
                                fontSize = ramStorageFontSize,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF555555)
                            )
                        }
                    }

                    // Price aligned with 4th column (Charging/Rear Cam)
                    Text(
                        text = formattedPrice,
                        modifier = Modifier.weight(priceWeight),
                        fontSize = priceFontSize,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDB2E2E),
                        maxLines = 1,
                        softWrap = false,
                        textAlign = TextAlign.End
                    )
                }
            }

            // Right side - Phone image and colors
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Phone image with swipe - using fixed width container to prevent card rearranging
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .width(140.dp)
                        .padding(start = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (colorsWithImages.isNotEmpty()) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            beyondViewportPageCount = 1
                        ) { page ->
                            val actualIndex = getActualColorIndex(page)
                            val colorData = colorsWithImages[actualIndex]
                            PhoneImageItem(
                                colorData = colorData,
                                phone = phone,
                                context = context,
                                imageLoader = imageLoader
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No Image",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Current color name - updates when swiping
                val currentColorName = colorsWithImages.getOrNull(getActualColorIndex(pagerState.currentPage))?.colorName ?: ""
                if (currentColorName.isNotEmpty()) {
                    Text(
                        text = currentColorName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF555555)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Color dots below image
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colorsWithImages.forEach { colorData ->
                        ColorDot(
                            colorName = colorData.colorName,
                            hexColor = colorData.hexColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PhoneImageItem(
    colorData: ColorImageData,
    phone: Phone,
    context: android.content.Context,
    imageLoader: ImageLoader
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (colorData.imageUrl != null) {
            val imageModel = remember(colorData.imageUrl, colorData.isCached, colorData.cacheVersion) {
                if (colorData.isCached) {
                    java.io.File(colorData.imageUrl)
                } else {
                    colorData.imageUrl
                }
            }

            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageModel)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .memoryCacheKey("${phone.phoneDocId}_${colorData.colorName}_${colorData.cacheVersion}")
                    .build(),
                imageLoader = imageLoader,
                contentDescription = "${phone.manufacturer} ${phone.model} in ${colorData.colorName}",
                modifier = Modifier.fillMaxHeight(),
                contentScale = ContentScale.FillHeight
            )
        } else {
            Text(
                text = "No Image",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

/**
 * SpecItem - Optimized to accept layout mode from parent instead of using BoxWithConstraints
 * This eliminates 8 BoxWithConstraints measurements per card (major performance improvement)
 */
@Composable
fun SpecItem(
    label: String,
    value: String,
    iconRes: Int? = null,
    useVerticalLayout: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Memoize the image request with caching
    val imageRequest = remember(iconRes) {
        iconRes?.let {
            ImageRequest.Builder(context)
                .data(it)
                .decoderFactory(SvgDecoder.Factory())
                .memoryCachePolicy(CachePolicy.ENABLED)
                .memoryCacheKey("spec_icon_$it")
                .build()
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (useVerticalLayout) {
            // Vertical layout: Icon -> Label -> Value (for smaller screens)
            if (imageRequest != null) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp)
                )
            }

            Text(
                text = label,
                fontSize = 10.sp,
                color = Color(0xFF888888),
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF333333),
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            // Horizontal layout: Icon + Label side by side, Value below (for larger screens)
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (imageRequest != null) {
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }

                Text(
                    text = label,
                    fontSize = 11.sp,
                    color = Color(0xFF888888),
                    maxLines = 2,
                    lineHeight = 13.sp
                )
            }

            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF333333),
                maxLines = 1,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ColorDot(
    colorName: String,
    hexColor: String = ""
) {
    // Memoize the color calculation
    val backgroundColor = remember(hexColor, colorName) {
        if (hexColor.isNotEmpty()) {
            hexToColor(hexColor)
        } else {
            getColorFromName(colorName)
        }
    }

    Surface(
        modifier = Modifier
            .size(18.dp)
            .border(
                width = 1.dp,
                color = Color(0xFFDDDDDD),
                shape = CircleShape
            ),
        shape = CircleShape,
        color = backgroundColor
    ) {}
}

/**
 * Spec item specifically designed for laptops with longer values (CPU, GPU names)
 * Uses auto-sizing text for the value to fit without clipping
 */
@Composable
fun LaptopSpecItem(
    label: String,
    value: String,
    iconRes: Int? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val imageRequest = remember(iconRes) {
        iconRes?.let {
            ImageRequest.Builder(context)
                .data(it)
                .decoderFactory(SvgDecoder.Factory())
                .memoryCachePolicy(CachePolicy.ENABLED)
                .memoryCacheKey("spec_icon_$it")
                .build()
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Icon and label row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (imageRequest != null) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }

            Text(
                text = label,
                fontSize = 9.sp,
                color = Color(0xFF888888),
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Auto-sizing value text
        AutoSizeText(
            text = value,
            maxFontSize = 13.sp,
            minFontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF333333),
            modifier = Modifier.fillMaxWidth(),
            maxLines = 1
        )
    }
}

/**
 * Larger spec item for laptop CPU/GPU when there are only 2 items in the row
 * Has more space so can use larger font
 */
@Composable
fun LaptopSpecItemLarge(
    label: String,
    value: String,
    iconRes: Int? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val imageRequest = remember(iconRes) {
        iconRes?.let {
            ImageRequest.Builder(context)
                .data(it)
                .decoderFactory(SvgDecoder.Factory())
                .memoryCachePolicy(CachePolicy.ENABLED)
                .memoryCacheKey("spec_icon_$it")
                .build()
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Icon and label row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (imageRequest != null) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            Text(
                text = label,
                fontSize = 12.sp,
                color = Color(0xFF888888),
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Auto-sizing value text - larger font range with smaller minimum to fit long GPU names
        AutoSizeText(
            text = value,
            maxFontSize = 15.sp,
            minFontSize = 7.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF333333),
            modifier = Modifier.fillMaxWidth(),
            maxLines = 1
        )
    }
}

/**
 * CHANGE #2: Auto-sizing text with CACHING optimization
 * First render: Iteratively shrinks font until it fits (same as before)
 * Subsequent renders: Uses cached size instantly (no repeated layout passes)
 */
@Composable
fun AutoSizeText(
    text: String,
    modifier: Modifier = Modifier,
    maxFontSize: TextUnit,
    minFontSize: TextUnit = 12.sp,
    fontWeight: FontWeight = FontWeight.Normal,
    color: Color = Color.Black,
    maxLines: Int = 1,
    textAlign: TextAlign = TextAlign.Center
) {
    // Cache key includes text and maxFontSize since same text might need different sizes in different contexts
    val cacheKey = "$text|${maxFontSize.value}"
    val cachedSize = autoSizeTextCache[cacheKey]

    var fontSize by remember(text, maxFontSize) { mutableStateOf(cachedSize ?: maxFontSize) }
    var readyToDraw by remember(text, maxFontSize) { mutableStateOf(cachedSize != null) }

    Text(
        text = text,
        modifier = modifier.drawWithContent {
            if (readyToDraw) drawContent()
        },
        fontSize = fontSize,
        fontWeight = fontWeight,
        color = color,
        maxLines = maxLines,
        softWrap = false,
        overflow = TextOverflow.Clip,
        textAlign = textAlign,
        onTextLayout = { textLayoutResult ->
            if (textLayoutResult.didOverflowWidth && fontSize > minFontSize) {
                // Reduce font size by 1sp and try again
                fontSize = (fontSize.value - 1f).sp
            } else {
                // Cache the computed size for future use
                if (cachedSize == null) {
                    autoSizeTextCache[cacheKey] = fontSize
                }
                readyToDraw = true
            }
        }
    )
}

/**
 * Parse hex color string to Compose Color (renamed to avoid conflict with ImageManagementActivity)
 */
private fun hexToColor(hex: String): Color {
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
            // Short form: #RGB -> #RRGGBB
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

fun getColorFromName(colorName: String): Color {
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

fun formatModelName(model: String): String {
    return model
        .replace("Iphone", "iPhone", ignoreCase = true)
        .replace("Ipad", "iPad", ignoreCase = true)
        .replace("(refurbished)", "*", ignoreCase = true)
}