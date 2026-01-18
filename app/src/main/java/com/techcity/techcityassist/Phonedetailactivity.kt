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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.techcity.techcityassist.ui.theme.TechCityAssistTheme
import kotlinx.coroutines.tasks.await
import java.text.NumberFormat
import java.util.Locale

class PhoneDetailActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PHONE_DOC_ID = "phone_doc_id"
        const val EXTRA_MANUFACTURER = "manufacturer"
        const val EXTRA_MODEL = "model"
        const val EXTRA_RAM = "ram"
        const val EXTRA_STORAGE = "storage"
        const val EXTRA_RETAIL_PRICE = "retail_price"
        const val EXTRA_COLORS = "colors"
        const val EXTRA_STOCK_COUNT = "stock_count"
        const val EXTRA_CHIPSET = "chipset"
        const val EXTRA_FRONT_CAMERA = "front_camera"
        const val EXTRA_REAR_CAMERA = "rear_camera"
        const val EXTRA_BATTERY = "battery"
        const val EXTRA_DISPLAY_TYPE = "display_type"
        const val EXTRA_DISPLAY_SIZE = "display_size"
        const val EXTRA_OS = "os"
        const val EXTRA_NETWORK = "network"
        const val EXTRA_RESOLUTION = "resolution"
        const val EXTRA_REFRESH_RATE = "refresh_rate"
        const val EXTRA_WIRED_CHARGING = "wired_charging"
        const val EXTRA_DEVICE_TYPE = "device_type"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Extract phone data from intent
        val phoneDocId = intent.getStringExtra(EXTRA_PHONE_DOC_ID) ?: ""
        val manufacturer = intent.getStringExtra(EXTRA_MANUFACTURER) ?: ""
        val model = intent.getStringExtra(EXTRA_MODEL) ?: ""
        val ram = intent.getStringExtra(EXTRA_RAM) ?: ""
        val storage = intent.getStringExtra(EXTRA_STORAGE) ?: ""
        val retailPrice = intent.getDoubleExtra(EXTRA_RETAIL_PRICE, 0.0)
        val colors = intent.getStringArrayListExtra(EXTRA_COLORS) ?: arrayListOf()
        val stockCount = intent.getIntExtra(EXTRA_STOCK_COUNT, 0)
        val chipset = intent.getStringExtra(EXTRA_CHIPSET) ?: ""
        val frontCamera = intent.getStringExtra(EXTRA_FRONT_CAMERA) ?: ""
        val rearCamera = intent.getStringExtra(EXTRA_REAR_CAMERA) ?: ""
        val battery = intent.getIntExtra(EXTRA_BATTERY, 0)
        val displayType = intent.getStringExtra(EXTRA_DISPLAY_TYPE) ?: ""
        val displaySize = intent.getStringExtra(EXTRA_DISPLAY_SIZE) ?: ""
        val os = intent.getStringExtra(EXTRA_OS) ?: ""
        val network = intent.getStringExtra(EXTRA_NETWORK) ?: ""
        val resolution = intent.getStringExtra(EXTRA_RESOLUTION) ?: ""
        val refreshRate = intent.getIntExtra(EXTRA_REFRESH_RATE, 0)
        val wiredCharging = intent.getIntExtra(EXTRA_WIRED_CHARGING, 0)
        val deviceType = intent.getStringExtra(EXTRA_DEVICE_TYPE) ?: ""

        val phone = Phone(
            manufacturer = manufacturer,
            model = model,
            ram = ram,
            storage = storage,
            retailPrice = retailPrice,
            colors = colors,
            stockCount = stockCount,
            chipset = chipset,
            frontCamera = frontCamera,
            rearCamera = rearCamera,
            batteryCapacity = battery,
            displayType = displayType,
            displaySize = displaySize,
            os = os,
            network = network,
            resolution = resolution,
            refreshRate = refreshRate,
            wiredCharging = wiredCharging,
            phoneDocId = phoneDocId,
            deviceType = deviceType
        )

        setContent {
            TechCityAssistTheme {
                PhoneDetailScreen(
                    phone = phone,
                    onBackPress = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PhoneDetailScreen(
    phone: Phone,
    onBackPress: () -> Unit
) {
    val context = LocalContext.current
    val formatter = remember { NumberFormat.getNumberInstance(Locale.US) }

    // State for phone images
    var phoneImages by remember { mutableStateOf<PhoneImages?>(null) }
    var selectedColorIndex by remember { mutableStateOf(0) }
    var isLoadingImages by remember { mutableStateOf(true) }

    // State for all variants (RAM/Storage/Price configurations)
    var variants by remember { mutableStateOf<List<PhoneVariant>>(emptyList()) }
    var isLoadingVariants by remember { mutableStateOf(true) }

    // State for all unique colors across all variants
    var allAvailableColors by remember { mutableStateOf<List<String>>(phone.colors) }

    // State for tracking which colors are available for each variant
    var variantColorsMap by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }

    // Fetch phone images
    LaunchedEffect(phone.phoneDocId) {
        if (phone.phoneDocId.isNotEmpty()) {
            try {
                val db = FirebaseFirestore.getInstance()
                val doc = db.collection("phone_images").document(phone.phoneDocId).get().await()
                if (doc.exists()) {
                    phoneImages = parsePhoneImagesDocumentDetail(doc.id, doc.data)
                }
            } catch (e: Exception) {
                Log.e("PhoneDetail", "Error fetching images", e)
            }
        }
        isLoadingImages = false
    }

    // Fetch all variants for this phone model
    LaunchedEffect(phone.manufacturer, phone.model) {
        if (phone.manufacturer.isNotEmpty() && phone.model.isNotEmpty()) {
            try {
                val db = FirebaseFirestore.getInstance()
                val inventoryResult = db.collection("inventory")
                    .whereEqualTo("manufacturer", phone.manufacturer)
                    .whereEqualTo("model", phone.model)
                    .whereIn("status", listOf("On-Hand", "On-Display"))
                    .get()
                    .await()

                // Group by RAM and Storage to get unique variants
                val variantMap = mutableMapOf<String, PhoneVariant>()
                val allColors = mutableSetOf<String>()
                val variantColorsTemp = mutableMapOf<String, MutableSet<String>>()

                for (doc in inventoryResult.documents) {
                    val ram = doc.getString("ram") ?: ""
                    val storage = doc.getString("storage") ?: ""
                    val retailPrice = doc.getDouble("retailPrice") ?: 0.0
                    val dealersPrice = doc.getDouble("dealersPrice") ?: 0.0
                    val color = doc.getString("color") ?: ""

                    // Collect all unique colors
                    if (color.isNotEmpty()) {
                        allColors.add(color)
                    }

                    val key = "$ram|$storage"

                    // Track colors for each variant
                    if (color.isNotEmpty()) {
                        variantColorsTemp.getOrPut(key) { mutableSetOf() }.add(color)
                    }

                    if (!variantMap.containsKey(key)) {
                        variantMap[key] = PhoneVariant(
                            ram = ram,
                            storage = storage,
                            retailPrice = retailPrice,
                            dealersPrice = dealersPrice
                        )
                    }
                }

                // Sort by price
                variants = variantMap.values.sortedBy { it.retailPrice }

                // Update allAvailableColors
                allAvailableColors = allColors.toList().sorted()

                // Update variantColorsMap
                variantColorsMap = variantColorsTemp.mapValues { it.value.toList().sorted() }
            } catch (e: Exception) {
                Log.e("PhoneDetail", "Error fetching variants", e)
                // Fall back to the single variant passed in
                variants = listOf(
                    PhoneVariant(
                        ram = phone.ram,
                        storage = phone.storage,
                        retailPrice = phone.retailPrice,
                        dealersPrice = 0.0
                    )
                )
                allAvailableColors = phone.colors
                variantColorsMap = mapOf("${phone.ram}|${phone.storage}" to phone.colors)
            }
        }
        isLoadingVariants = false
    }

    // Get current color and image URL based on selected index
    val currentColor = allAvailableColors.getOrNull(selectedColorIndex) ?: allAvailableColors.firstOrNull() ?: ""
    val colorImages = phoneImages?.getImagesForColor(currentColor)
    val imageUrl = colorImages?.highRes?.ifEmpty { colorImages.lowRes } ?: colorImages?.lowRes

    // Format display name
    val displayName = remember(phone.manufacturer, phone.model) {
        if (phone.manufacturer.equals("Apple", ignoreCase = true)) {
            formatModelNameDetail(phone.model)
        } else {
            "${phone.manufacturer} ${formatModelNameDetail(phone.model)}"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBackPress) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
        ) {
            // Model name centered at top - large and bold like reference
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 0.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (phone.manufacturer.equals("Apple", ignoreCase = true)) {
                    Image(
                        painter = painterResource(id = R.drawable.apple_logo),
                        contentDescription = "Apple logo",
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = displayName,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1A1A1A)
                )
            }

            // Added spacer between model name and image
            Spacer(modifier = Modifier.height(48.dp))

            // Main content - Phone image and specs
            Row(
                modifier = Modifier
                    .fillMaxWidth(),  // CHANGED: Removed fillMaxHeight(0.75f) so Row wraps to content
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.Top
            ) {
                // Left side - Phone Image (much larger) with color dots right below
                Column(
                    modifier = Modifier
                        .weight(1.8f),  // CHANGED: Removed fillMaxHeight() so Column wraps to content
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Top
                ) {
                    // Pager state for swiping between colors
                    val colorCount = allAvailableColors.size
                    val infinitePageCount = if (colorCount > 1) 10000 else 1
                    val startPage = if (colorCount > 1) {
                        (infinitePageCount / 2) - ((infinitePageCount / 2) % colorCount)
                    } else {
                        0
                    }

                    val pagerState = rememberPagerState(
                        initialPage = startPage,
                        pageCount = { infinitePageCount }
                    )

                    // Get actual color index from page (for infinite scroll)
                    fun getActualColorIndex(page: Int): Int {
                        return if (colorCount > 0) page % colorCount else 0
                    }

                    // Sync selectedColorIndex when user swipes
                    LaunchedEffect(pagerState.currentPage) {
                        val newIndex = getActualColorIndex(pagerState.currentPage)
                        if (newIndex != selectedColorIndex) {
                            selectedColorIndex = newIndex
                        }
                    }

                    // Sync pager when user clicks on color swatch
                    LaunchedEffect(selectedColorIndex) {
                        val currentPagerIndex = getActualColorIndex(pagerState.currentPage)
                        if (currentPagerIndex != selectedColorIndex && colorCount > 0) {
                            // Calculate the closest page to scroll to
                            val currentPage = pagerState.currentPage
                            val currentActual = currentPage % colorCount
                            val diff = selectedColorIndex - currentActual
                            val targetPage = currentPage + diff
                            pagerState.animateScrollToPage(targetPage)
                        }
                    }

                    // Phone image with swipe - large, image scaled to crop whitespace
                    Box(
                        modifier = Modifier
                            .height(418.dp)  // Increased by ~10%
                            .fillMaxWidth(0.85f)
                            .clip(RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoadingImages) {
                            CircularProgressIndicator()
                        } else if (allAvailableColors.isNotEmpty()) {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize(),
                                beyondViewportPageCount = 1
                            ) { page ->
                                val actualIndex = getActualColorIndex(page)
                                val colorName = allAvailableColors.getOrNull(actualIndex) ?: ""
                                val colorImagesForPage = phoneImages?.getImagesForColor(colorName)
                                val imageUrlForPage = colorImagesForPage?.highRes?.ifEmpty { colorImagesForPage.lowRes } ?: colorImagesForPage?.lowRes

                                if (imageUrlForPage != null) {
                                    // Check for cached image first
                                    val isHighRes = colorImagesForPage?.lowRes.isNullOrEmpty() == true
                                    val cachedPath = ImageCacheManager.getLocalImageUri(
                                        context, phone.phoneDocId, colorName, isHighRes
                                    )

                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(cachedPath ?: imageUrlForPage)
                                            .memoryCachePolicy(CachePolicy.ENABLED)
                                            .diskCachePolicy(CachePolicy.ENABLED)
                                            .build(),
                                        contentDescription = "$displayName in $colorName",
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .graphicsLayer {
                                                scaleX = 1.15f  // Scale up to crop whitespace edges
                                                scaleY = 1.15f
                                            },
                                        contentScale = ContentScale.FillHeight
                                    )
                                } else {
                                    Text(
                                        text = "No Image Available",
                                        color = Color.Gray,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "No Image Available",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // Color name and swatches centered to phone image width
                    Box(
                        modifier = Modifier.fillMaxWidth(0.85f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Color name below image
                            if (currentColor.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = currentColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF555555)
                                )
                            }

                            // Color dots right below color name - show all unique colors
                            if (allAvailableColors.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    allAvailableColors.forEachIndexed { index, colorName ->
                                        val hexColor = phoneImages?.getHexColorForColor(colorName) ?: ""
                                        val isSelected = index == selectedColorIndex

                                        DetailColorDot(
                                            colorName = colorName,
                                            hexColor = hexColor,
                                            isSelected = isSelected,
                                            onClick = { selectedColorIndex = index }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Right side - Specs list
                Column(
                    modifier = Modifier
                        .weight(1.1f)
                        .height(478.dp)  // Matches image height (418) + color name + color swatches (~60)
                        .offset(x = (-10).dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // 1. Display size
                    if (phone.displaySize.isNotEmpty()) {
                        DetailSpecRowMultiLine(
                            iconRes = R.raw.screen_size_icon,
                            label = "Display Size",
                            value = "${phone.displaySize} inches",
                            isSvg = false,
                            iconSize = 50
                        )
                    }

                    // 2. Resolution
                    if (phone.resolution.isNotEmpty()) {
                        DetailSpecRowMultiLine(
                            iconRes = R.raw.resolution_icon,
                            label = "Resolution",
                            value = phone.resolution,
                            isSvg = false
                        )
                    }

                    // 3. Refresh rate
                    if (phone.refreshRate > 0) {
                        DetailSpecRowMultiLine(
                            iconRes = R.raw.refresh_rate_icon,
                            label = "Refresh Rate",
                            value = "${phone.refreshRate} Hz",
                            isSvg = false
                        )
                    }

                    // 4. Front Camera
                    if (phone.frontCamera.isNotEmpty()) {
                        DetailSpecRowMultiLine(
                            iconRes = R.raw.camera_icon,
                            label = "Front Camera",
                            value = phone.frontCamera,
                            isSvg = false
                        )
                    }

                    // 5. Rear Camera
                    if (phone.rearCamera.isNotEmpty()) {
                        DetailSpecRowMultiLine(
                            iconRes = R.raw.rear_camera_icon,
                            label = "Rear Camera",
                            value = phone.rearCamera,
                            isSvg = false
                        )
                    }

                    // 6. Chipset
                    if (phone.chipset.isNotEmpty()) {
                        DetailSpecRowMultiLine(
                            iconRes = R.raw.chipset_icon,
                            label = "Chipset",
                            value = phone.chipset,
                            isSvg = false
                        )
                    }

                    // 7. Battery
                    if (phone.batteryCapacity > 0) {
                        DetailSpecRowMultiLine(
                            iconRes = R.raw.battery_icon,
                            label = "Battery",
                            value = "${formatter.format(phone.batteryCapacity)} mAh",
                            isSvg = false
                        )
                    }

                    // 8. Charging
                    if (phone.wiredCharging > 0) {
                        DetailSpecRowMultiLine(
                            iconRes = R.raw.charging_icon,
                            label = "Charging",
                            value = "${phone.wiredCharging}W fast charging",
                            isSvg = false
                        )
                    }

                    // 9. OS
                    if (phone.os.isNotEmpty()) {
                        DetailSpecRowMultiLine(
                            iconRes = R.raw.os_icon,
                            label = "OS",
                            value = phone.os,
                            isSvg = false
                        )
                    }

                    // 10. Network
                    if (phone.network.isNotEmpty()) {
                        DetailSpecRowMultiLine(
                            iconRes = R.raw.network_icon,
                            label = "Network",
                            value = phone.network,
                            isSvg = false
                        )
                    }
                }
            }

            // RAM/Storage/Price variants - FULL WIDTH, below main content
            Spacer(modifier = Modifier.height(45.dp))

            if (isLoadingVariants) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                // Centered container - variants grow from center
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),  // Take remaining space
                    verticalArrangement = Arrangement.Center,  // Center variants vertically
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        variants.forEach { variant ->
                            val variantKey = "${variant.ram}|${variant.storage}"
                            val colorsForVariant = variantColorsMap[variantKey] ?: emptyList()
                            val isAvailableInSelectedColor = colorsForVariant.contains(currentColor)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Chips container - takes remaining space after price is measured
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 32.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // RAM chip
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isAvailableInSelectedColor) Color.White else Color(0xFFF5F5F5),
                                        modifier = Modifier
                                            .widthIn(min = 95.dp)
                                            .border(
                                                1.dp,
                                                if (isAvailableInSelectedColor) Color(0xFFE0E0E0) else Color(0xFFEEEEEE),
                                                RoundedCornerShape(6.dp)
                                            )
                                    ) {
                                        Text(
                                            text = "${variant.ram}GB RAM",
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isAvailableInSelectedColor) Color(0xFF333333) else Color(0xFFBBBBBB),
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Storage chip
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isAvailableInSelectedColor) Color.White else Color(0xFFF5F5F5),
                                        modifier = Modifier
                                            .widthIn(min = 140.dp)
                                            .border(
                                                1.dp,
                                                if (isAvailableInSelectedColor) Color(0xFFE0E0E0) else Color(0xFFEEEEEE),
                                                RoundedCornerShape(6.dp)
                                            )
                                    ) {
                                        Text(
                                            text = "${variant.storage}GB Storage",
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isAvailableInSelectedColor) Color(0xFF333333) else Color(0xFFBBBBBB),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }

                                // Price - grayed out if not available
                                Text(
                                    text = "₱${String.format("%,.2f", variant.retailPrice)}",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAvailableInSelectedColor) Color(0xFFDB2E2E) else Color(0xFFCCCCCC),
                                    modifier = Modifier.padding(end = 72.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailSpecRow(
    iconRes: Int,
    text: String,
    isSvg: Boolean = true
) {
    val context = LocalContext.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 0.dp)
    ) {
        if (isSvg) {
            // For SVG icons in raw folder
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(iconRes)
                    .decoderFactory(coil.decode.SvgDecoder.Factory())
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = null,
                modifier = Modifier.size(36.dp)
            )
        } else {
            // For PNG icons in raw folder
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(iconRes)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = null,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF222222),
            lineHeight = 18.sp
        )
    }
}

@Composable
fun DetailSpecRowMultiLine(
    iconRes: Int,
    label: String,
    value: String,
    isSvg: Boolean = true,
    iconSize: Int = 42
) {
    val context = LocalContext.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 0.dp)
    ) {
        if (isSvg) {
            // For SVG icons in raw folder
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(iconRes)
                    .decoderFactory(coil.decode.SvgDecoder.Factory())
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = null,
                modifier = Modifier.size(iconSize.dp)
            )
        } else {
            // For PNG icons in raw folder
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(iconRes)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = null,
                modifier = Modifier.size(iconSize.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

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
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF222222),
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun DetailColorDot(
    colorName: String,
    hexColor: String = "",
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    val backgroundColor = remember(hexColor, colorName) {
        if (hexColor.isNotEmpty()) {
            try {
                val cleanHex = hexColor.removePrefix("#")
                val colorLong = cleanHex.toLong(16)
                Color(
                    red = ((colorLong shr 16) and 0xFF) / 255f,
                    green = ((colorLong shr 8) and 0xFF) / 255f,
                    blue = (colorLong and 0xFF) / 255f
                )
            } catch (e: Exception) {
                getColorFromNameDetail(colorName)
            }
        } else {
            getColorFromNameDetail(colorName)
        }
    }

    Surface(
        modifier = Modifier
            .size(if (isSelected) 28.dp else 22.dp)
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

@Composable
fun StoragePriceTable(
    ram: String,
    storage: String,
    price: Double
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "STORAGE",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "PRICE",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End
            )
        }

        // Divider
        HorizontalDivider(
            color = Color(0xFFDDDDDD),
            thickness = 1.dp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Data row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${storage}GB",
                fontSize = 16.sp,
                color = Color(0xFF333333),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "PHP ${String.format("%,.0f", price)}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFDB2E2E),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End
            )
        }
    }
}

// Utility function to get color from name (private to avoid conflict with MainActivity)
private fun getColorFromNameDetail(colorName: String): Color {
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

// Utility function to format model name (private to avoid conflict with MainActivity)
private fun formatModelNameDetail(model: String): String {
    return model
        .replace("Iphone", "iPhone", ignoreCase = true)
        .replace("Ipad", "iPad", ignoreCase = true)
}

// Parse phone images document from Firestore (private to avoid conflict with MainActivity)
@Suppress("UNCHECKED_CAST")
private fun parsePhoneImagesDocumentDetail(docId: String, data: Map<String, Any>?): PhoneImages? {
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
        Log.e("PhoneDetail", "Error parsing phone images document: $docId", e)
        null
    }
}