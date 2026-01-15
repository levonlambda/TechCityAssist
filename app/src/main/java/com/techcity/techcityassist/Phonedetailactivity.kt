package com.techcity.techcityassist

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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

@OptIn(ExperimentalMaterial3Api::class)
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

    // Get current color and image URL based on selected index
    val currentColor = phone.colors.getOrNull(selectedColorIndex) ?: phone.colors.firstOrNull() ?: ""
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
                    .padding(vertical = 12.dp),
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

            // Main content - Phone image and specs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side - Phone Image (much larger) with color dots right below
                Column(
                    modifier = Modifier
                        .weight(2f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    // Phone image - large with tight border, image scaled to crop whitespace
                    Box(
                        modifier = Modifier
                            .fillMaxHeight(0.88f)
                            .wrapContentWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoadingImages) {
                            CircularProgressIndicator()
                        } else if (imageUrl != null) {
                            // Check for cached image first
                            val isHighRes = colorImages?.lowRes.isNullOrEmpty() == true
                            val cachedPath = ImageCacheManager.getLocalImageUri(
                                context, phone.phoneDocId, currentColor, isHighRes
                            )

                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(cachedPath ?: imageUrl)
                                    .memoryCachePolicy(CachePolicy.ENABLED)
                                    .diskCachePolicy(CachePolicy.ENABLED)
                                    .build(),
                                contentDescription = "$displayName in $currentColor",
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

                    // Color dots right below image
                    if (phone.colors.isNotEmpty()) {
                        Row(
                            modifier = Modifier.padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            phone.colors.forEachIndexed { index, colorName ->
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

                // Right side - Specs list (more specs)
                Column(
                    modifier = Modifier
                        .weight(0.9f)
                        .fillMaxHeight()
                        .padding(start = 8.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    // Display size
                    if (phone.displaySize.isNotEmpty()) {
                        DetailSpecRow(
                            iconRes = R.raw.screen_size_icon,
                            text = "${phone.displaySize} inches",
                            isSvg = false
                        )
                    }

                    // Resolution
                    if (phone.resolution.isNotEmpty()) {
                        DetailSpecRow(
                            iconRes = R.raw.resolution_icon,
                            text = phone.resolution,
                            isSvg = false
                        )
                    }

                    // Refresh rate
                    if (phone.refreshRate > 0) {
                        DetailSpecRow(
                            iconRes = R.raw.refresh_rate_icon,
                            text = "${phone.refreshRate} Hz",
                            isSvg = false
                        )
                    }

                    // Chipset
                    if (phone.chipset.isNotEmpty()) {
                        DetailSpecRow(
                            iconRes = R.raw.chipset_icon,
                            text = phone.chipset,
                            isSvg = false
                        )
                    }

                    // Battery
                    if (phone.batteryCapacity > 0) {
                        DetailSpecRow(
                            iconRes = R.raw.battery_icon,
                            text = "${formatter.format(phone.batteryCapacity)} mAh",
                            isSvg = false
                        )
                    }

                    // Charging
                    if (phone.wiredCharging > 0) {
                        DetailSpecRow(
                            iconRes = R.raw.charging_icon,
                            text = "${phone.wiredCharging}W fast charging",
                            isSvg = false
                        )
                    }

                    // Rear Camera
                    if (phone.rearCamera.isNotEmpty()) {
                        DetailSpecRow(
                            iconRes = R.raw.rear_camera_icon,
                            text = "${phone.rearCamera} Rear Camera",
                            isSvg = false
                        )
                    }

                    // Front Camera
                    if (phone.frontCamera.isNotEmpty()) {
                        DetailSpecRow(
                            iconRes = R.raw.camera_icon,
                            text = "${phone.frontCamera} Front Camera",
                            isSvg = false
                        )
                    }

                    // Network
                    if (phone.network.isNotEmpty()) {
                        DetailSpecRow(
                            iconRes = R.raw.network_icon,
                            text = phone.network,
                            isSvg = false
                        )
                    }

                    // OS
                    if (phone.os.isNotEmpty()) {
                        DetailSpecRow(
                            iconRes = R.raw.os_icon,
                            text = phone.os,
                            isSvg = false
                        )
                    }
                }
            }

            // Bottom section - Storage/RAM and Price
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Storage and RAM on the left
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White,
                        modifier = Modifier.border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                    ) {
                        Text(
                            text = "${phone.ram}GB RAM",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF333333)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White,
                        modifier = Modifier.border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                    ) {
                        Text(
                            text = "${phone.storage}GB Storage",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF333333)
                        )
                    }
                }

                // Price on the right - large and bold
                Text(
                    text = "PHP ${String.format("%,.0f", phone.retailPrice)}",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFDB2E2E)
                )
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
        modifier = Modifier.padding(vertical = 8.dp)
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
                modifier = Modifier.size(44.dp)
            )
        } else {
            // For PNG icons in raw folder
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(iconRes)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = null,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF222222),
            lineHeight = 24.sp
        )
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
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Color(0xFF6200EE) else Color(0xFFDDDDDD),
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