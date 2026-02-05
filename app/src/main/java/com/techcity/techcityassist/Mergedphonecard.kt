package com.techcity.techcityassist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

// ============================================
// DATA CLASSES FOR MERGED VIEW
// ============================================

/**
 * Represents a single variant within a merged phone group
 */
data class MergedVariant(
    val ram: String,
    val storage: String,
    val retailPrice: Double,
    val colors: List<String>,  // Colors available for this specific variant
    val inventoryDocIds: List<String> = emptyList(),  // Reference to original inventory docs
    val phoneDocId: String = ""  // Reference to phone specs doc
)

/**
 * Represents a group of phone variants merged into a single card
 * All variants share the same manufacturer + model but have different RAM/Storage configurations
 */
data class MergedPhoneGroup(
    val phoneDocId: String,  // Common phone doc ID for specs
    val manufacturer: String,
    val model: String,
    val deviceType: String,

    // All variants sorted by price
    val variants: List<MergedVariant>,

    // Union of all colors across all variants (for color swatches)
    val allColors: List<String>,

    // Map: colorName -> list of variants that have this color
    val colorToVariantsMap: Map<String, List<MergedVariant>>,

    // Common specs (same across variants)
    val chipset: String = "",
    val frontCamera: String = "",
    val rearCamera: String = "",
    val batteryCapacity: Int = 0,
    val displayType: String = "",
    val displaySize: String = "",
    val os: String = "",
    val network: String = "",
    val resolution: String = "",
    val refreshRate: Int = 0,
    val wiredCharging: Int = 0,
    val cpu: String = "",
    val gpu: String = "",

    // Price range for sorting/filtering
    val lowestPrice: Double,
    val highestPrice: Double,

    // Index to distinguish split groups (0, 1, 2...) when a phone has >2 variants
    val groupIndex: Int = 0
)

// ============================================
// MERGING LOGIC
// ============================================

/**
 * Groups phones by manufacturer+model and creates MergedPhoneGroup objects
 * Splits groups with more than 2 variants into multiple cards (max 2 variants per card)
 * @param phones List of individual phone entries
 * @param phoneImagesMap Map of phoneDocId to PhoneImages for color/image data
 * @return List of MergedPhoneGroup sorted by lowest price
 */
fun groupPhonesForMergedView(
    phones: List<Phone>,
    phoneImagesMap: Map<String, PhoneImages>
): List<MergedPhoneGroup> {
    // Group by manufacturer + model (case-insensitive)
    val grouped = phones.groupBy { "${it.manufacturer.lowercase()}|${it.model.lowercase()}" }

    val result = mutableListOf<MergedPhoneGroup>()

    grouped.forEach { (_, phonesInGroup) ->
        // Use the first phone for common attributes
        val firstPhone = phonesInGroup.first()

        // Build all variants list sorted by price
        val allVariants = phonesInGroup.map { phone ->
            MergedVariant(
                ram = phone.ram,
                storage = phone.storage,
                retailPrice = phone.retailPrice,
                colors = phone.colors,
                inventoryDocIds = phone.inventoryDocIds,
                phoneDocId = phone.phoneDocId
            )
        }.sortedBy { it.retailPrice }

        // Split variants into chunks of max 2
        val variantChunks = allVariants.chunked(2)

        variantChunks.forEachIndexed { chunkIndex, chunkVariants ->
            // Collect all unique colors for THIS chunk's variants only
            val allColorsSet = linkedSetOf<String>()
            chunkVariants.forEach { variant ->
                variant.colors.forEach { color ->
                    if (!allColorsSet.any { it.equals(color, ignoreCase = true) }) {
                        allColorsSet.add(color)
                    }
                }
            }
            val allColors = allColorsSet.toList()

            // Build color -> variants map for this chunk
            val colorToVariantsMap = mutableMapOf<String, MutableList<MergedVariant>>()
            chunkVariants.forEach { variant ->
                variant.colors.forEach { color ->
                    val canonicalColor = allColors.find { it.equals(color, ignoreCase = true) } ?: color
                    colorToVariantsMap.getOrPut(canonicalColor) { mutableListOf() }.add(variant)
                }
            }

            result.add(
                MergedPhoneGroup(
                    phoneDocId = firstPhone.phoneDocId,
                    manufacturer = firstPhone.manufacturer,
                    model = firstPhone.model,
                    deviceType = firstPhone.deviceType,
                    variants = chunkVariants,
                    allColors = allColors,
                    colorToVariantsMap = colorToVariantsMap,
                    chipset = firstPhone.chipset,
                    frontCamera = firstPhone.frontCamera,
                    rearCamera = firstPhone.rearCamera,
                    batteryCapacity = firstPhone.batteryCapacity,
                    displayType = firstPhone.displayType,
                    displaySize = firstPhone.displaySize,
                    os = firstPhone.os,
                    network = firstPhone.network,
                    resolution = firstPhone.resolution,
                    refreshRate = firstPhone.refreshRate,
                    wiredCharging = firstPhone.wiredCharging,
                    cpu = firstPhone.cpu,
                    gpu = firstPhone.gpu,
                    lowestPrice = chunkVariants.minOfOrNull { it.retailPrice } ?: 0.0,
                    highestPrice = chunkVariants.maxOfOrNull { it.retailPrice } ?: 0.0,
                    groupIndex = chunkIndex
                )
            )
        }
    }

    return result.sortedBy { it.lowestPrice }
}

/**
 * Check if a merged phone group matches the price filter
 * Returns true if ANY variant falls within the price range
 */
fun matchesPriceFilterMerged(group: MergedPhoneGroup, filters: DisplayFilters): Boolean {
    return group.variants.any { variant ->
        val minOk = if (filters.enableMinPrice && filters.minPrice != null) {
            variant.retailPrice >= filters.minPrice
        } else {
            true
        }

        val maxOk = if (filters.enableMaxPrice && filters.maxPrice != null) {
            variant.retailPrice <= filters.maxPrice
        } else {
            true
        }

        minOk && maxOk
    }
}

/**
 * Check if a merged phone group matches the manufacturer filter
 */
fun matchesManufacturerFilterMerged(group: MergedPhoneGroup, selectedManufacturer: String): Boolean {
    if (selectedManufacturer == "All") return true
    return group.manufacturer.equals(selectedManufacturer, ignoreCase = true)
}

// ============================================
// MERGED PHONE CARD UI
// ============================================

// Pre-allocated elevation values
private val MERGED_CARD_ELEVATION_ODD = 1.dp
private val MERGED_CARD_ELEVATION_EVEN = 8.dp

// Color indicator constants
private val COLOR_BAR_WIDTH = 5.dp
private val COLOR_BAR_HEIGHT = 26.dp

/**
 * Parse hex color string to Compose Color (for merged card)
 */
private fun parseHexColorMerged(hex: String): Color {
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

/**
 * Card layout configuration for merged cards
 * Similar to CardLayoutConfig but with additional variant-related values
 */
data class MergedCardLayoutConfig(
    val useVerticalSpecLayout: Boolean,
    val cardMinHeight: Dp,
    val specRowStartPadding: Dp,
    val startPadding: Dp,
    val variantRowHeight: Dp,
    val variantChipPaddingH: Dp,
    val variantChipPaddingV: Dp,
    val variantFontSize: androidx.compose.ui.unit.TextUnit,
    val priceFontSize: androidx.compose.ui.unit.TextUnit,
    val ramChipMinWidth: Dp,
    val storageChipMinWidth: Dp
)

/**
 * Calculate merged card height based on number of variants
 * Uses the same base height as the original PhoneCard from layoutConfig
 */
fun calculateMergedCardHeight(variantCount: Int, baseCardHeight: Dp, variantRowHeight: Dp = 40.dp): Dp {
    // Base height covers specs + 1 variant row (same as original card)
    // Add height for each additional variant
    val additionalVariants = (variantCount - 1).coerceAtLeast(0)
    return baseCardHeight + (variantRowHeight * additionalVariants)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MergedPhoneCard(
    group: MergedPhoneGroup,
    phoneImages: PhoneImages? = null,
    imageLoader: ImageLoader,
    layoutConfig: CardLayoutConfig,  // Reuse existing layout config for specs
    isMarkedForComparison: Boolean = false,
    onClick: (selectedVariant: MergedVariant, selectedColorName: String) -> Unit,
    onLongClick: (selectedVariant: MergedVariant) -> Unit = {},
    isAlternate: Boolean = false,
    initialColorIndex: Int = 0
) {
    val formatter = remember { NumberFormat.getNumberInstance(Locale.US) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Display name for the model
    val displayName = remember(group.manufacturer, group.model) {
        if (group.manufacturer.equals("Apple", ignoreCase = true)) {
            formatModelName(group.model)
        } else {
            "${group.manufacturer} ${formatModelName(group.model)}"
        }
    }

    // Get color data with images
    val colorsWithImages = remember(group.phoneDocId, group.allColors, phoneImages) {
        group.allColors.map { colorName ->
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
    }

    // Pager state for color swiping
    val actualColorCount = colorsWithImages.size
    val infinitePageCount = if (actualColorCount > 1) 10000 else 1
    val startPage = if (actualColorCount > 1) {
        (infinitePageCount / 2) - ((infinitePageCount / 2) % actualColorCount) + initialColorIndex.coerceIn(0, actualColorCount - 1)
    } else {
        0
    }

    val pagerState = rememberPagerState(
        initialPage = startPage,
        pageCount = { infinitePageCount }
    )

    fun getActualColorIndex(page: Int): Int {
        return if (actualColorCount > 0) page % actualColorCount else 0
    }

    // Current selected color
    val currentColorIndex = getActualColorIndex(pagerState.currentPage)
    val currentColorName = colorsWithImages.getOrNull(currentColorIndex)?.colorName ?: ""
    val currentColorHex = colorsWithImages.getOrNull(currentColorIndex)?.hexColor ?: ""

    // Variants that have the current color
    val variantsWithCurrentColor = remember(currentColorName, group.colorToVariantsMap) {
        group.colorToVariantsMap[currentColorName] ?: emptyList()
    }

    // Card styling
    val cardBackgroundColor = if (isMarkedForComparison) Color(0xFFE3F2FD) else Color.White
    val cardElevation = if (isAlternate) MERGED_CARD_ELEVATION_ODD else MERGED_CARD_ELEVATION_EVEN

    // Calculate dynamic card height based on variant count using layoutConfig's card height as base
    val cardHeight = remember(group.variants.size, layoutConfig.cardHeight) {
        calculateMergedCardHeight(group.variants.size, layoutConfig.cardHeight)
    }

    // Fixed image height to match the original PhoneCard
    // Original: cardHeight - padding (40dp) - color section (~44dp) = cardHeight - 84dp
    val imageHeight = remember(layoutConfig.cardHeight) {
        layoutConfig.cardHeight - 84.dp
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight)
            .combinedClickable(
                onClick = { onClick(group.variants.first(), currentColorName) },
                onLongClick = { onLongClick(group.variants.first()) }
            )
            .then(
                if (isMarkedForComparison) {
                    Modifier.border(2.dp, Color(0xFF2196F3), RoundedCornerShape(16.dp))
                } else {
                    Modifier
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
            // Left side - Model info, specs, and variants
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top section - Model name
                Row(
                    modifier = Modifier.padding(start = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (group.manufacturer.equals("Apple", ignoreCase = true)) {
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

                // Specs section
                val isLaptop = group.deviceType.equals("laptop", ignoreCase = true)

                if (isLaptop) {
                    // LAPTOP LAYOUT
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = layoutConfig.specRowStartPadding, end = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(32.dp)
                    ) {
                        LaptopSpecItem(label = "OS", value = group.os.ifEmpty { "N/A" }, iconRes = R.raw.os_icon, modifier = Modifier.weight(1f))
                        LaptopSpecItem(label = "Display Size", value = if (group.displaySize.isNotEmpty()) "${group.displaySize} Inches" else "N/A", iconRes = R.raw.screen_size_icon, modifier = Modifier.weight(1f))
                        LaptopSpecItem(label = "Resolution", value = group.resolution.ifEmpty { "N/A" }, iconRes = R.raw.resolution_icon, modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = layoutConfig.specRowStartPadding, end = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Spacer(modifier = Modifier.weight(0.15f))
                        LaptopSpecItemLarge(label = "CPU", value = group.cpu.ifEmpty { "N/A" }, iconRes = R.raw.chipset_icon, modifier = Modifier.weight(0.9f))
                        LaptopSpecItemLarge(label = "GPU", value = group.gpu.ifEmpty { "N/A" }, iconRes = R.raw.gpu_icon, modifier = Modifier.weight(1.2f))
                        Spacer(modifier = Modifier.weight(0.15f))
                    }
                } else {
                    // PHONE/TABLET LAYOUT
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = layoutConfig.specRowStartPadding, end = 0.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        SpecItem(label = "OS", value = group.os.ifEmpty { "N/A" }, iconRes = R.raw.os_icon, useVerticalLayout = layoutConfig.useVerticalSpecLayout, modifier = Modifier.weight(1f))
                        SpecItem(label = "Battery", value = if (group.batteryCapacity > 0) "${formatter.format(group.batteryCapacity)} mAh" else "N/A", iconRes = R.raw.battery_icon, useVerticalLayout = layoutConfig.useVerticalSpecLayout, modifier = Modifier.weight(1f))
                        SpecItem(label = "Front Cam", value = group.frontCamera.ifEmpty { "N/A" }, iconRes = R.raw.camera_icon, useVerticalLayout = layoutConfig.useVerticalSpecLayout, modifier = Modifier.weight(1f))
                        SpecItem(label = "Rear Cam", value = if (layoutConfig.useVerticalSpecLayout) group.rearCamera.replace(" ", "").ifEmpty { "N/A" } else group.rearCamera.ifEmpty { "N/A" }, iconRes = R.raw.rear_camera_icon, useVerticalLayout = layoutConfig.useVerticalSpecLayout, modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = layoutConfig.specRowStartPadding, end = 0.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        SpecItem(label = "Display Size", value = if (group.displaySize.isNotEmpty()) "${group.displaySize} Inches" else "N/A", iconRes = R.raw.screen_size_icon, useVerticalLayout = layoutConfig.useVerticalSpecLayout, modifier = Modifier.weight(1f))
                        SpecItem(label = "Refresh Rate", value = if (group.refreshRate > 0) "${group.refreshRate} Hz" else "N/A", iconRes = R.raw.refresh_rate_icon, useVerticalLayout = layoutConfig.useVerticalSpecLayout, modifier = Modifier.weight(1f))
                        SpecItem(label = "Charging", value = if (group.wiredCharging > 0) "${group.wiredCharging}W" else "N/A", iconRes = R.raw.charging_icon, useVerticalLayout = layoutConfig.useVerticalSpecLayout, modifier = Modifier.weight(1f))
                        SpecItem(label = "Network", value = group.network.ifEmpty { "N/A" }, iconRes = R.raw.network_icon, useVerticalLayout = layoutConfig.useVerticalSpecLayout, modifier = Modifier.weight(1f))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Variants section - similar to PhoneDetailActivity
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = layoutConfig.startPadding),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    group.variants.forEach { variant ->
                        val isAvailableInSelectedColor = variantsWithCurrentColor.any {
                            it.ram == variant.ram && it.storage == variant.storage
                        }

                        // Get color for the bar
                        val barColor = if (isAvailableInSelectedColor) {
                            if (currentColorHex.isNotEmpty()) {
                                parseHexColorMerged(currentColorHex)
                            } else {
                                getColorFromName(currentColorName)
                            }
                        } else {
                            Color.Transparent
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { onClick(variant, currentColorName) },
                                    onLongClick = { onLongClick(variant) }
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // RAM chip
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.White,
                                modifier = Modifier
                                    .width(90.dp)
                                    .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(6.dp))
                            ) {
                                Text(
                                    text = "${variant.ram}GB RAM",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF333333),
                                    textAlign = TextAlign.Center
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Storage chip
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.White,
                                modifier = Modifier
                                    .width(110.dp)
                                    .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(6.dp))
                            ) {
                                Text(
                                    text = "${variant.storage}GB Storage",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF333333),
                                    textAlign = TextAlign.Center
                                )
                            }

                            // Color availability bar
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(
                                modifier = Modifier
                                    .width(COLOR_BAR_WIDTH)
                                    .height(COLOR_BAR_HEIGHT)
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

                            Spacer(modifier = Modifier.weight(1f))

                            // Price - consistent alignment for all rows
                            Text(
                                text = "₱${String.format("%,.2f", variant.retailPrice)}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDB2E2E)
                            )
                        }
                    }
                }
            }

            // Right side - Phone image and color selector
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(140.dp)  // Fixed width to prevent compression
            ) {
                // Phone image with swipe - fixed height to match original PhoneCard
                Box(
                    modifier = Modifier
                        .height(imageHeight)
                        .fillMaxWidth()
                        .padding(start = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (colorsWithImages.isNotEmpty()) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            beyondViewportPageCount = 1
                        ) { page ->
                            val colorIndex = getActualColorIndex(page)
                            val colorData = colorsWithImages.getOrNull(colorIndex)

                            if (colorData != null) {
                                val imageSource = colorData.imageUrl

                                if (imageSource != null) {
                                    val imageRequest = remember(imageSource, colorData.cacheVersion) {
                                        ImageRequest.Builder(context)
                                            .data(imageSource)
                                            .memoryCacheKey("${group.phoneDocId}_${colorData.colorName}_${colorData.cacheVersion}")
                                            .memoryCachePolicy(CachePolicy.ENABLED)
                                            .diskCachePolicy(CachePolicy.ENABLED)
                                            .crossfade(false)
                                            .build()
                                    }

                                    AsyncImage(
                                        model = imageRequest,
                                        contentDescription = "${displayName} in ${colorData.colorName}",
                                        imageLoader = imageLoader,
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
                    } else {
                        Text(
                            text = "No Image",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                // Color name and dots below the image
                if (colorsWithImages.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Color name
                    Text(
                        text = currentColorName,
                        fontSize = 11.sp,
                        color = Color(0xFF666666),
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Color dots - use requiredSize to enforce circular shape
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.height(20.dp)  // Fixed height for dot row
                    ) {
                        colorsWithImages.forEachIndexed { index, colorData ->
                            val dotColor = if (colorData.hexColor.isNotEmpty()) {
                                parseHexColorMerged(colorData.hexColor)
                            } else {
                                getColorFromName(colorData.colorName)
                            }

                            Surface(
                                modifier = Modifier
                                    .requiredSize(18.dp)  // Enforce exact size
                                    .border(
                                        width = 1.dp,
                                        color = Color(0xFFDDDDDD),
                                        shape = CircleShape
                                    )
                                    .clickable(
                                        onClick = {
                                            scope.launch {
                                                val currentActualIndex = getActualColorIndex(pagerState.currentPage)
                                                val diff = index - currentActualIndex
                                                pagerState.animateScrollToPage(pagerState.currentPage + diff)
                                            }
                                        }
                                    ),
                                shape = CircleShape,
                                color = dotColor
                            ) {}
                        }
                    }
                }
            }
        }
    }
}

/**
 * Convert MergedVariant to Phone for navigation to detail activity
 */
fun mergedVariantToPhone(group: MergedPhoneGroup, variant: MergedVariant): Phone {
    return Phone(
        phoneDocId = group.phoneDocId,
        manufacturer = group.manufacturer,
        model = group.model,
        ram = variant.ram,
        storage = variant.storage,
        retailPrice = variant.retailPrice,
        colors = variant.colors,
        stockCount = 0,  // Not available in merged view
        chipset = group.chipset,
        frontCamera = group.frontCamera,
        rearCamera = group.rearCamera,
        batteryCapacity = group.batteryCapacity,
        displayType = group.displayType,
        displaySize = group.displaySize,
        os = group.os,
        network = group.network,
        resolution = group.resolution,
        refreshRate = group.refreshRate,
        wiredCharging = group.wiredCharging,
        deviceType = group.deviceType,
        cpu = group.cpu,
        gpu = group.gpu,
        inventoryDocIds = variant.inventoryDocIds,
        variants = emptyList()
    )
}