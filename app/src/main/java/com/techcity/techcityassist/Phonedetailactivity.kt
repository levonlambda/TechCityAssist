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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.text.style.TextOverflow
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
        const val EXTRA_PHONE_INDEX = "phone_index"
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
        const val EXTRA_SELECTED_COLOR = "selected_color"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialIndex = intent.getIntExtra(EXTRA_PHONE_INDEX, 0)
        val initialSelectedColor = intent.getStringExtra(EXTRA_SELECTED_COLOR) ?: ""

        setContent {
            TechCityAssistTheme {
                PhoneDetailScreen(
                    initialIndex = initialIndex,
                    initialSelectedColor = initialSelectedColor,
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
// (Using 650dp gives buffer so 601dp device stays on standard layout)
// ============================================

/**
 * Configuration for all layout values in PhoneDetailActivity
 */
data class DetailLayoutConfig(
    // Logo
    val logoHeight: Dp,

    // Model name
    val modelNameMaxFontSize: TextUnit,
    val modelNameMinFontSize: TextUnit,
    val appleLogoSize: Dp,

    // Spacing
    val logoToModelSpacing: Dp,
    val modelToContentSpacing: Dp,
    val horizontalPadding: Dp,

    // Image
    val imageHeight: Dp,
    val imageScale: Float,

    // Specs column
    val specsColumnHeight: Dp,

    // Spec row (icon, label, value)
    val specIconSize: Dp,
    val specIconSizeLarge: Dp,  // For screen_size_icon which is slightly bigger
    val specLabelFontSize: TextUnit,
    val specValueFontSize: TextUnit,
    val specRowSpacing: Dp,

    // Color dots
    val colorDotSizeSelected: Dp,
    val colorDotSizeUnselected: Dp,
    val colorNameFontSize: TextUnit,
    val colorDotsSpacing: Dp,

    // RAM/Storage/Price section
    val variantChipPaddingH: Dp,
    val variantChipPaddingV: Dp,
    val variantChipFontSize: TextUnit,
    val ramChipMinWidth: Dp,
    val storageChipMinWidth: Dp,
    val priceFontSize: TextUnit,
    val priceEndPadding: Dp,
    val colorBarWidth: Dp,
    val colorBarHeight: Dp,
    val variantRowSpacing: Dp,
    val variantStartPadding: Dp
)

/**
 * Standard layout - your working tablet (601 x 1007 dp)
 * These are the EXACT values from your current working code - DO NOT CHANGE
 */
private fun createStandardLayoutConfig(): DetailLayoutConfig {
    return DetailLayoutConfig(
        // Logo - current value
        logoHeight = 40.dp,

        // Model name - current values
        modelNameMaxFontSize = 42.sp,
        modelNameMinFontSize = 24.sp,
        appleLogoSize = 56.dp,

        // Spacing - current values
        logoToModelSpacing = 24.dp,
        modelToContentSpacing = 32.dp,
        horizontalPadding = 24.dp,

        // Image - current values
        imageHeight = 418.dp,
        imageScale = 1.15f,

        // Specs column - current value
        specsColumnHeight = 478.dp,

        // Spec row - current values
        specIconSize = 42.dp,
        specIconSizeLarge = 50.dp,
        specLabelFontSize = 14.sp,
        specValueFontSize = 15.sp,
        specRowSpacing = 14.dp,

        // Color dots - current values
        colorDotSizeSelected = 28.dp,
        colorDotSizeUnselected = 22.dp,
        colorNameFontSize = 14.sp,
        colorDotsSpacing = 10.dp,

        // RAM/Storage/Price - current values
        variantChipPaddingH = 12.dp,
        variantChipPaddingV = 10.dp,
        variantChipFontSize = 15.sp,
        ramChipMinWidth = 110.dp,
        storageChipMinWidth = 140.dp,
        priceFontSize = 22.sp,
        priceEndPadding = 72.dp,
        colorBarWidth = 6.dp,
        colorBarHeight = 32.dp,
        variantRowSpacing = 8.dp,
        variantStartPadding = 32.dp
    )
}

/**
 * Large layout - for screens bigger than 650dp width
 * Scaled up for your larger tablet (824 x 1318 dp)
 */
private fun createLargeLayoutConfig(): DetailLayoutConfig {
    return DetailLayoutConfig(
        // Logo - A LOT BIGGER (50% increase)
        logoHeight = 60.dp,

        // Model name - BIGGER (increased by ~25%)
        modelNameMaxFontSize = 54.sp,
        modelNameMinFontSize = 30.sp,
        appleLogoSize = 72.dp,

        // Spacing - INCREASED GAPS
        logoToModelSpacing = 36.dp,
        modelToContentSpacing = 48.dp,
        horizontalPadding = 32.dp,

        // Image - 25% BIGGER (418 * 1.25 = 522.5)
        imageHeight = 522.dp,
        imageScale = 1.15f,

        // Specs column - proportionally bigger (478 * 1.25 = 597.5)
        specsColumnHeight = 598.dp,

        // Spec row - INCREASED (icons, labels, values ~20-25% bigger)
        specIconSize = 52.dp,
        specIconSizeLarge = 62.dp,
        specLabelFontSize = 17.sp,
        specValueFontSize = 19.sp,
        specRowSpacing = 18.dp,

        // Color dots - proportionally bigger
        colorDotSizeSelected = 36.dp,
        colorDotSizeUnselected = 28.dp,
        colorNameFontSize = 17.sp,
        colorDotsSpacing = 14.dp,

        // RAM/Storage/Price - INCREASED FONT SIZES
        variantChipPaddingH = 16.dp,
        variantChipPaddingV = 14.dp,
        variantChipFontSize = 19.sp,
        ramChipMinWidth = 140.dp,
        storageChipMinWidth = 175.dp,
        priceFontSize = 28.sp,
        priceEndPadding = 90.dp,
        colorBarWidth = 8.dp,
        colorBarHeight = 40.dp,
        variantRowSpacing = 12.dp,
        variantStartPadding = 40.dp
    )
}

/**
 * Get the appropriate layout config based on screen size
 *
 * PRESERVES standard layout for screens <= 650dp width
 * (Your working 601dp tablet will use standard layout)
 *
 * USES large layout for screens > 650dp width
 * (Your 824dp tablet will use large layout)
 */
@Composable
fun rememberDetailLayoutConfig(): DetailLayoutConfig {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp

    return remember(screenWidthDp) {
        if (screenWidthDp > 650) {
            // Larger screen (824dp tablet) - use scaled up layout
            Log.d("PhoneDetail", "Using LARGE layout for screen width: ${screenWidthDp}dp")
            createLargeLayoutConfig()
        } else {
            // Standard screen (601dp tablet) - preserve exact layout
            Log.d("PhoneDetail", "Using STANDARD layout for screen width: ${screenWidthDp}dp")
            createStandardLayoutConfig()
        }
    }
}

// ============================================
// END RESPONSIVE CONFIGURATION
// ============================================

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhoneDetailScreen(
    initialIndex: Int,
    initialSelectedColor: String = "",
    onBackPress: () -> Unit
) {
    val phones = PhoneListHolder.uniquePhoneModels
    val phoneImagesMapHolder = PhoneListHolder.phoneImagesMap

    if (phones.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Text("No phones available", color = Color.Gray)
        }
        return
    }

    val phonePagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, phones.size - 1),
        pageCount = { phones.size }
    )

    HorizontalPager(
        state = phonePagerState,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        beyondViewportPageCount = 1
    ) { page ->
        val phone = phones[page]
        val initialPhoneImages = phoneImagesMapHolder[phone.phoneDocId]

        val colorForThisPage = if (page == initialIndex) initialSelectedColor else ""

        PhoneDetailContent(
            phone = phone,
            initialPhoneImages = initialPhoneImages,
            initialSelectedColor = colorForThisPage
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhoneDetailContent(
    phone: Phone,
    initialPhoneImages: PhoneImages?,
    initialSelectedColor: String = ""
) {
    val context = LocalContext.current
    val formatter = remember { NumberFormat.getNumberInstance(Locale.US) }

    // Get responsive layout configuration based on screen size
    val layoutConfig = rememberDetailLayoutConfig()

    var phoneImages by remember(phone.phoneDocId) { mutableStateOf(initialPhoneImages) }
    var isLoadingImages by remember(phone.phoneDocId) { mutableStateOf(initialPhoneImages == null) }

    var variants by remember(phone.phoneDocId) { mutableStateOf<List<PhoneVariant>>(emptyList()) }
    var isLoadingVariants by remember(phone.phoneDocId) { mutableStateOf(true) }

    var allAvailableColors by remember(phone.phoneDocId) { mutableStateOf(phone.colors) }

    var variantColorsMap by remember(phone.phoneDocId) { mutableStateOf<Map<String, List<String>>>(emptyMap()) }

    var selectedColorName by remember(phone.phoneDocId) {
        mutableStateOf(
            if (initialSelectedColor.isNotEmpty()) initialSelectedColor
            else phone.colors.firstOrNull() ?: ""
        )
    }

    val selectedColorIndex = remember(allAvailableColors, selectedColorName) {
        val index = allAvailableColors.indexOfFirst { it.equals(selectedColorName, ignoreCase = true) }
        if (index >= 0) index else 0
    }

    // Zoom state for expanded image view
    var isImageZoomed by remember { mutableStateOf(false) }

    LaunchedEffect(phone.phoneDocId) {
        if (phone.phoneDocId.isNotEmpty() && phoneImages == null) {
            try {
                val db = FirebaseFirestore.getInstance()
                val doc = db.collection("phone_images").document(phone.phoneDocId).get().await()
                if (doc.exists()) {
                    phoneImages = parsePhoneImagesDocumentDetail(doc.id, doc.data)
                }
            } catch (e: Exception) {
                Log.e("PhoneDetail", "Error fetching images", e)
            }
            isLoadingImages = false
        } else {
            isLoadingImages = false
        }
    }

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

                val variantMap = mutableMapOf<String, PhoneVariant>()
                val allColors = mutableSetOf<String>()
                val variantColorsTemp = mutableMapOf<String, MutableSet<String>>()

                for (doc in inventoryResult.documents) {
                    val ram = doc.getString("ram") ?: ""
                    val storage = doc.getString("storage") ?: ""
                    val retailPrice = doc.getDouble("retailPrice") ?: 0.0
                    val dealersPrice = doc.getDouble("dealersPrice") ?: 0.0
                    val color = doc.getString("color") ?: ""

                    if (color.isNotEmpty()) {
                        allColors.add(color)
                    }

                    val key = "$ram|$storage"

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

                variants = variantMap.values.sortedBy { it.retailPrice }

                val orderedColors = phone.colors.toMutableList()
                allColors.forEach { color ->
                    if (!orderedColors.any { it.equals(color, ignoreCase = true) }) {
                        orderedColors.add(color)
                    }
                }
                allAvailableColors = orderedColors

                variantColorsMap = variantColorsTemp.mapValues { (_, colors) ->
                    orderedColors.filter { ordered -> colors.any { it.equals(ordered, ignoreCase = true) } }
                }
            } catch (e: Exception) {
                Log.e("PhoneDetail", "Error fetching variants", e)
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

    val currentColor = selectedColorName.ifEmpty { allAvailableColors.firstOrNull() ?: "" }
    val colorImages = phoneImages?.getImagesForColor(currentColor)
    val imageUrl = colorImages?.highRes?.ifEmpty { colorImages.lowRes } ?: colorImages?.lowRes

    val displayName = remember(phone.manufacturer, phone.model) {
        if (phone.manufacturer.equals("Apple", ignoreCase = true)) {
            formatModelNameDetail(phone.model)
        } else {
            "${phone.manufacturer} ${formatModelNameDetail(phone.model)}"
        }
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
                .padding(top = 50.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.tc_logo_flat_colored),
                contentDescription = "TechCity Logo",
                modifier = Modifier.height(layoutConfig.logoHeight),
                contentScale = ContentScale.FillHeight
            )
        }

        // Spacer between logo and model name - USES CONFIG
        Spacer(modifier = Modifier.height(layoutConfig.logoToModelSpacing))

        // Model name centered at top - USES CONFIG
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
                    modifier = Modifier.size(layoutConfig.appleLogoSize)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            AutoSizeTextDetail(
                text = displayName,
                maxFontSize = layoutConfig.modelNameMaxFontSize,
                minFontSize = layoutConfig.modelNameMinFontSize,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1A1A1A)
            )
        }

        // Spacer between model name and content - USES CONFIG (INCREASED GAP)
        Spacer(modifier = Modifier.height(layoutConfig.modelToContentSpacing))

        // Main content - Phone image and specs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (isImageZoomed) Modifier.weight(1f) else Modifier),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            // Left side - Phone Image with color dots
            Column(
                modifier = Modifier
                    .weight(if (isImageZoomed) 1f else 1.8f),
                horizontalAlignment = if (isImageZoomed) Alignment.CenterHorizontally else Alignment.Start,
                verticalArrangement = Arrangement.Top
            ) {
                // Calculate image scale - reduce by 20% when zoomed
                val effectiveImageScale = if (isImageZoomed) layoutConfig.imageScale * 0.7f else layoutConfig.imageScale

                // Phone image - USES CONFIG (25% BIGGER for large screens)
                Box(
                    modifier = Modifier
                        .then(if (isImageZoomed) Modifier.weight(1f) else Modifier.height(layoutConfig.imageHeight))
                        .fillMaxWidth(if (isImageZoomed) 1f else 0.85f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { isImageZoomed = !isImageZoomed },
                    contentAlignment = if (isImageZoomed) Alignment.TopCenter else Alignment.Center
                ) {
                    if (isLoadingImages) {
                        CircularProgressIndicator()
                    } else if (isLoadingVariants) {
                        val colorImagesStatic = phoneImages?.getImagesForColor(selectedColorName)
                        val imageUrlStatic = colorImagesStatic?.highRes?.ifEmpty { colorImagesStatic.lowRes } ?: colorImagesStatic?.lowRes

                        if (imageUrlStatic != null) {
                            val isHighRes = colorImagesStatic?.lowRes.isNullOrEmpty() == true
                            val cachedPath = ImageCacheManager.getLocalImageUri(
                                context, phone.phoneDocId, selectedColorName, isHighRes
                            )

                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(cachedPath ?: imageUrlStatic)
                                    .memoryCachePolicy(CachePolicy.ENABLED)
                                    .diskCachePolicy(CachePolicy.ENABLED)
                                    .build(),
                                contentDescription = "$displayName in $selectedColorName",
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .graphicsLayer {
                                        scaleX = effectiveImageScale
                                        scaleY = effectiveImageScale
                                        // Move image up when zoomed to reduce gap from model name
                                        if (isImageZoomed) {
                                            translationY = -225f
                                        }
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
                    } else if (allAvailableColors.isNotEmpty()) {
                        val colorCount = allAvailableColors.size
                        val infinitePageCount = if (colorCount > 1) 10000 else 1
                        val startPage = if (colorCount > 1) {
                            (infinitePageCount / 2) - ((infinitePageCount / 2) % colorCount) + selectedColorIndex
                        } else {
                            0
                        }

                        val pagerState = rememberPagerState(
                            initialPage = startPage,
                            pageCount = { infinitePageCount }
                        )

                        fun getActualColorIndex(page: Int): Int {
                            return if (colorCount > 0) page % colorCount else 0
                        }

                        LaunchedEffect(pagerState.settledPage) {
                            val newIndex = getActualColorIndex(pagerState.settledPage)
                            val newColorName = allAvailableColors.getOrNull(newIndex) ?: ""
                            if (newColorName.isNotEmpty() && !newColorName.equals(selectedColorName, ignoreCase = true)) {
                                selectedColorName = newColorName
                            }
                        }

                        LaunchedEffect(selectedColorIndex) {
                            val currentPagerIndex = getActualColorIndex(pagerState.currentPage)
                            if (currentPagerIndex != selectedColorIndex && colorCount > 0) {
                                val currentPage = pagerState.currentPage
                                val currentActual = currentPage % colorCount
                                val diff = selectedColorIndex - currentActual
                                val targetPage = currentPage + diff
                                pagerState.animateScrollToPage(targetPage)
                            }
                        }

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
                                            scaleX = effectiveImageScale
                                            scaleY = effectiveImageScale
                                            // Move image up when zoomed to reduce gap from model name
                                            if (isImageZoomed) {
                                                translationY = -200f
                                            }
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

                // Color name and swatches - USES CONFIG
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .offset(y = if (isImageZoomed) (-200).dp else 0.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (currentColor.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(if (isImageZoomed) 0.dp else 6.dp))
                            Text(
                                text = currentColor,
                                fontSize = layoutConfig.colorNameFontSize,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF555555)
                            )
                        }

                        if (allAvailableColors.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(if (isImageZoomed) 4.dp else 8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(layoutConfig.colorDotsSpacing),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                allAvailableColors.forEachIndexed { index, colorName ->
                                    val hexColor = phoneImages?.getHexColorForColor(colorName) ?: ""
                                    val isSelected = index == selectedColorIndex

                                    DetailColorDot(
                                        colorName = colorName,
                                        hexColor = hexColor,
                                        isSelected = isSelected,
                                        selectedSize = layoutConfig.colorDotSizeSelected,
                                        unselectedSize = layoutConfig.colorDotSizeUnselected,
                                        onClick = { selectedColorName = colorName }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Right side - Specs list - USES CONFIG (hidden when zoomed)
            if (!isImageZoomed) {
                Column(
                    modifier = Modifier
                        .weight(1.1f)
                        .height(layoutConfig.specsColumnHeight)
                        .offset(x = (-10).dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    val isLaptop = phone.deviceType.equals("laptop", ignoreCase = true)

                    if (isLaptop) {
                        // LAPTOP SPECS
                        DetailSpecRowMultiLine(
                            iconRes = R.raw.screen_size_icon,
                            label = "Display Size",
                            value = if (phone.displaySize.isNotEmpty()) "${phone.displaySize} inches" else "N/A",
                            isSvg = false,
                            layoutConfig = layoutConfig,
                            iconSize = layoutConfig.specIconSizeLarge,
                            iconOffsetX = -4,
                            textStartOffset = -8
                        )

                        DetailSpecRowMultiLine(
                            iconRes = R.raw.resolution_icon,
                            label = "Resolution",
                            value = phone.resolution.ifEmpty { "N/A" },
                            isSvg = false,
                            layoutConfig = layoutConfig
                        )

                        DetailSpecRowMultiLine(
                            iconRes = R.raw.refresh_rate_icon,
                            label = "Refresh Rate",
                            value = if (phone.refreshRate > 0) "${phone.refreshRate} Hz" else "N/A",
                            isSvg = false,
                            layoutConfig = layoutConfig
                        )

                        DetailSpecRowMultiLine(
                            iconRes = R.raw.chipset_icon,
                            label = "CPU",
                            value = phone.cpu.ifEmpty { "N/A" },
                            isSvg = false,
                            layoutConfig = layoutConfig
                        )

                        DetailSpecRowMultiLine(
                            iconRes = R.raw.gpu_icon,
                            label = "GPU",
                            value = phone.gpu.ifEmpty { "N/A" },
                            isSvg = false,
                            layoutConfig = layoutConfig
                        )

                        DetailSpecRowMultiLine(
                            iconRes = R.raw.battery_icon,
                            label = "Battery",
                            value = if (phone.batteryCapacity > 0) "${formatter.format(phone.batteryCapacity)} mAh" else "N/A",
                            isSvg = false,
                            layoutConfig = layoutConfig
                        )

                        DetailSpecRowMultiLine(
                            iconRes = R.raw.os_icon,
                            label = "OS",
                            value = phone.os.ifEmpty { "N/A" },
                            isSvg = false,
                            layoutConfig = layoutConfig
                        )

                    } else {
                        // PHONE/TABLET SPECS
                        if (phone.displaySize.isNotEmpty()) {
                            DetailSpecRowMultiLine(
                                iconRes = R.raw.screen_size_icon,
                                label = "Display Size",
                                value = "${phone.displaySize} inches",
                                isSvg = false,
                                layoutConfig = layoutConfig,
                                iconSize = layoutConfig.specIconSizeLarge,
                                iconOffsetX = -4,
                                textStartOffset = -8
                            )
                        }

                        if (phone.resolution.isNotEmpty()) {
                            DetailSpecRowMultiLine(
                                iconRes = R.raw.resolution_icon,
                                label = "Resolution",
                                value = phone.resolution,
                                isSvg = false,
                                layoutConfig = layoutConfig
                            )
                        }

                        if (phone.refreshRate > 0) {
                            DetailSpecRowMultiLine(
                                iconRes = R.raw.refresh_rate_icon,
                                label = "Refresh Rate",
                                value = "${phone.refreshRate} Hz",
                                isSvg = false,
                                layoutConfig = layoutConfig
                            )
                        }

                        if (phone.frontCamera.isNotEmpty()) {
                            DetailSpecRowMultiLine(
                                iconRes = R.raw.camera_icon,
                                label = "Front Camera",
                                value = phone.frontCamera,
                                isSvg = false,
                                layoutConfig = layoutConfig
                            )
                        }

                        if (phone.rearCamera.isNotEmpty()) {
                            DetailSpecRowMultiLine(
                                iconRes = R.raw.rear_camera_icon,
                                label = "Rear Camera",
                                value = phone.rearCamera,
                                isSvg = false,
                                layoutConfig = layoutConfig
                            )
                        }

                        if (phone.chipset.isNotEmpty()) {
                            DetailSpecRowMultiLine(
                                iconRes = R.raw.chipset_icon,
                                label = "Chipset",
                                value = phone.chipset,
                                isSvg = false,
                                layoutConfig = layoutConfig
                            )
                        }

                        if (phone.batteryCapacity > 0) {
                            DetailSpecRowMultiLine(
                                iconRes = R.raw.battery_icon,
                                label = "Battery",
                                value = "${formatter.format(phone.batteryCapacity)} mAh",
                                isSvg = false,
                                layoutConfig = layoutConfig
                            )
                        }

                        if (phone.wiredCharging > 0) {
                            DetailSpecRowMultiLine(
                                iconRes = R.raw.charging_icon,
                                label = "Charging",
                                value = "${phone.wiredCharging}W fast charging",
                                isSvg = false,
                                layoutConfig = layoutConfig
                            )
                        }

                        if (phone.os.isNotEmpty()) {
                            DetailSpecRowMultiLine(
                                iconRes = R.raw.os_icon,
                                label = "OS",
                                value = phone.os,
                                isSvg = false,
                                layoutConfig = layoutConfig
                            )
                        }

                        if (phone.network.isNotEmpty()) {
                            DetailSpecRowMultiLine(
                                iconRes = R.raw.network_icon,
                                label = "Network",
                                value = phone.network,
                                isSvg = false,
                                layoutConfig = layoutConfig
                            )
                        }
                    }
                }
            }
        }

        // RAM/Storage/Price variants - USES CONFIG (hidden when image is zoomed)
        if (!isImageZoomed) {
            Spacer(modifier = Modifier.height(10.dp))

            if (isLoadingVariants) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(layoutConfig.variantRowSpacing)
                    ) {
                        variants.forEach { variant ->
                            val variantKey = "${variant.ram}|${variant.storage}"
                            val colorsForVariant = variantColorsMap[variantKey] ?: emptyList()
                            val isAvailableInSelectedColor = colorsForVariant.contains(currentColor)

                            val barColor = if (isAvailableInSelectedColor) {
                                val hexColor = phoneImages?.getHexColorForColor(currentColor) ?: ""
                                if (hexColor.isNotEmpty()) {
                                    parseHexColorDetail(hexColor)
                                } else {
                                    getColorFromNameDetail(currentColor)
                                }
                            } else {
                                Color.Transparent
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Chips container - USES CONFIG
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = layoutConfig.variantStartPadding),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // RAM chip - USES CONFIG (fixed width for consistent alignment)
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color.White,
                                        modifier = Modifier
                                            .width(layoutConfig.ramChipMinWidth)
                                            .border(
                                                1.dp,
                                                Color(0xFFE0E0E0),
                                                RoundedCornerShape(6.dp)
                                            )
                                    ) {
                                        Text(
                                            text = "${variant.ram}GB RAM",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(
                                                    horizontal = layoutConfig.variantChipPaddingH,
                                                    vertical = layoutConfig.variantChipPaddingV
                                                ),
                                            fontSize = layoutConfig.variantChipFontSize,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF333333),
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Storage chip - USES CONFIG (fixed width for consistent alignment)
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color.White,
                                        modifier = Modifier
                                            .width(layoutConfig.storageChipMinWidth)
                                            .border(
                                                1.dp,
                                                Color(0xFFE0E0E0),
                                                RoundedCornerShape(6.dp)
                                            )
                                    ) {
                                        Text(
                                            text = "${variant.storage}GB Storage",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(
                                                    horizontal = layoutConfig.variantChipPaddingH,
                                                    vertical = layoutConfig.variantChipPaddingV
                                                ),
                                            fontSize = layoutConfig.variantChipFontSize,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF333333),
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    // Color availability bar - USES CONFIG
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Box(
                                        modifier = Modifier
                                            .width(layoutConfig.colorBarWidth)
                                            .height(layoutConfig.colorBarHeight)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(barColor)
                                            .then(
                                                if (isAvailableInSelectedColor) {
                                                    Modifier.border(1.dp, Color(0xFFDDDDDD), RoundedCornerShape(3.dp))
                                                } else {
                                                    Modifier
                                                }
                                            )
                                    )
                                }

                                // Price - USES CONFIG (BIGGER FONT for large screens)
                                Text(
                                    text = "₱${String.format("%,.2f", variant.retailPrice)}",
                                    fontSize = layoutConfig.priceFontSize,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFDB2E2E),
                                    modifier = Modifier.padding(end = layoutConfig.priceEndPadding)
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
    isSvg: Boolean = true,
    layoutConfig: DetailLayoutConfig
) {
    val context = LocalContext.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 0.dp)
    ) {
        if (isSvg) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(iconRes)
                    .decoderFactory(coil.decode.SvgDecoder.Factory())
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = null,
                modifier = Modifier.size(layoutConfig.specIconSize)
            )
        } else {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(iconRes)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = null,
                modifier = Modifier.size(layoutConfig.specIconSize)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = text,
            fontSize = layoutConfig.specValueFontSize,
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
    layoutConfig: DetailLayoutConfig,
    iconSize: Dp = layoutConfig.specIconSize,
    iconOffsetX: Int = 0,
    textStartOffset: Int = 0
) {
    val context = LocalContext.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 0.dp)
    ) {
        if (isSvg) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(iconRes)
                    .decoderFactory(coil.decode.SvgDecoder.Factory())
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .size(iconSize)
                    .offset(x = iconOffsetX.dp)
            )
        } else {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(iconRes)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .size(iconSize)
                    .offset(x = iconOffsetX.dp)
            )
        }

        Spacer(modifier = Modifier.width((layoutConfig.specRowSpacing.value.toInt() + textStartOffset).dp))

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
                fontSize = layoutConfig.specValueFontSize,
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
    selectedSize: Dp = 28.dp,
    unselectedSize: Dp = 22.dp,
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

/**
 * Auto-sizing text that shrinks font size to fit on a single line
 */
@Composable
fun AutoSizeTextDetail(
    text: String,
    modifier: Modifier = Modifier,
    maxFontSize: TextUnit,
    minFontSize: TextUnit = 12.sp,
    fontWeight: FontWeight = FontWeight.Normal,
    color: Color = Color.Black,
    maxLines: Int = 1
) {
    var fontSize by remember(text) { mutableStateOf(maxFontSize) }
    var readyToDraw by remember(text) { mutableStateOf(false) }

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
        onTextLayout = { textLayoutResult ->
            if (textLayoutResult.didOverflowWidth && fontSize > minFontSize) {
                fontSize = (fontSize.value - 1f).sp
            } else {
                readyToDraw = true
            }
        }
    )
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

        HorizontalDivider(
            color = Color(0xFFDDDDDD),
            thickness = 1.dp
        )

        Spacer(modifier = Modifier.height(8.dp))

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

// Utility functions
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

private fun parseHexColorDetail(hex: String): Color {
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

private fun formatModelNameDetail(model: String): String {
    return model
        .replace("Iphone", "iPhone", ignoreCase = true)
        .replace("Ipad", "iPad", ignoreCase = true)
        .replace("(refurbished)", "*", ignoreCase = true)
}

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