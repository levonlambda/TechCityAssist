package com.techcity.techcityassist

import android.content.Intent
import androidx.compose.ui.layout.SubcomposeLayout
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.decode.SvgDecoder
import coil.imageLoader
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TechCityAssistTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("TechCity Assist") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF6200EE),
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
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
        PhoneListScreen(modifier = Modifier.padding(innerPadding))
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
    val wiredCharging: Int = 0
)

// Data class to hold color and image info for each color variant
data class ColorImageData(
    val colorName: String,
    val imageUrl: String?,       // Local path if cached, otherwise remote URL
    val hexColor: String,
    val remoteUrl: String? = null,  // Original remote URL for downloading
    val isCached: Boolean = false   // Whether the image is cached locally
)

@Composable
fun PhoneListScreen(modifier: Modifier = Modifier) {
    var phones by remember { mutableStateOf<List<Phone>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedPhone by remember { mutableStateOf<Phone?>(null) }

    // Store phone images for all phones (phoneDocId -> PhoneImages)
    var phoneImagesMap by remember { mutableStateOf<Map<String, PhoneImages>>(emptyMap()) }

    LaunchedEffect(Unit) {
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
                            wiredCharging = phoneDoc.getLong("wiredCharging")?.toInt() ?: 0
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
                                                    variants = emptyList()
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
                            wiredCharging = doc.getLong("wiredCharging")?.toInt() ?: 0
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
                                        variants = emptyList()
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

    if (selectedPhone != null) {
        AlertDialog(
            onDismissRequest = { selectedPhone = null },
            title = { Text("Document IDs") },
            text = {
                SelectionContainer {
                    Column {
                        Text(
                            text = "${selectedPhone!!.manufacturer} ${selectedPhone!!.model}",
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(text = "Phones Collection (specs):", fontWeight = FontWeight.Medium)
                        Text(
                            text = selectedPhone!!.phoneDocId.ifEmpty { "Not found" },
                            fontSize = 12.sp,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(text = "Inventory Collection:", fontWeight = FontWeight.Medium)
                        selectedPhone!!.inventoryDocIds.forEach { docId ->
                            Text(
                                text = docId,
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedPhone = null }) {
                    Text("Close")
                }
            }
        )
    }

    if (isLoading) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (phones.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No phones available")
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(phones) { phone ->
                PhoneCard(
                    phone = phone,
                    phoneImages = phoneImagesMap[phone.phoneDocId],
                    onClick = { selectedPhone = phone }
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
    onClick: () -> Unit
) {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Build list of colors with their image URLs and hex colors
    // This will be updated as images are cached
    var colorsWithImages by remember(phone.colors, phoneImages) {
        mutableStateOf(
            phone.colors.map { colorName ->
                val images = phoneImages?.getImagesForColor(colorName)
                val remoteUrl = images?.lowRes?.ifEmpty { images.highRes }

                // Check if we have a cached version
                val isHighRes = images?.lowRes.isNullOrEmpty() && !images?.highRes.isNullOrEmpty()
                val cachedPath = ImageCacheManager.getLocalImageUri(
                    context,
                    phone.phoneDocId,
                    colorName,
                    isHighRes
                )

                ColorImageData(
                    colorName = colorName,
                    imageUrl = cachedPath ?: remoteUrl,  // Use cached path if available
                    hexColor = images?.hexColor ?: "",
                    remoteUrl = remoteUrl,  // Keep remote URL for downloading
                    isCached = cachedPath != null
                )
            }
        )
    }

    // Pager state for swiping between images
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { maxOf(1, colorsWithImages.size) }
    )

    // Preload and cache adjacent images
    val currentColorIndex = pagerState.currentPage
    LaunchedEffect(currentColorIndex, colorsWithImages) {
        // Cache images for previous, current, and next colors
        listOf(currentColorIndex - 1, currentColorIndex, currentColorIndex + 1)
            .filter { it in colorsWithImages.indices }
            .forEach { index ->
                val colorData = colorsWithImages[index]

                // If not cached yet and has remote URL, download and cache
                if (!colorData.isCached && !colorData.remoteUrl.isNullOrEmpty()) {
                    val isHighRes = phoneImages?.getImagesForColor(colorData.colorName)?.lowRes.isNullOrEmpty()

                    val cachedPath = ImageCacheManager.downloadAndCacheImage(
                        context = context,
                        imageUrl = colorData.remoteUrl,
                        phoneDocId = phone.phoneDocId,
                        colorName = colorData.colorName,
                        isHighRes = isHighRes
                    )

                    // Update the state with the cached path
                    if (cachedPath != null) {
                        colorsWithImages = colorsWithImages.toMutableList().apply {
                            this[index] = colorData.copy(
                                imageUrl = cachedPath,
                                isCached = true
                            )
                        }
                    }
                }
            }
    }

    // State for re-download dialog
    var showRedownloadDialog by remember { mutableStateOf(false) }
    var isRedownloading by remember { mutableStateOf(false) }
    var redownloadProgress by remember { mutableStateOf("") }

    // Re-download dialog
    if (showRedownloadDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isRedownloading) showRedownloadDialog = false
            },
            title = { Text("Refresh Images") },
            text = {
                Column {
                    Text("${phone.manufacturer} ${phone.model}")
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
                    } else {
                        Text(
                            "This will delete cached images and re-download them from the server.",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            isRedownloading = true

                            // Delete cached images for this phone
                            redownloadProgress = "Clearing cache..."
                            colorsWithImages.forEach { colorData ->
                                val isHighRes = phoneImages?.getImagesForColor(colorData.colorName)?.lowRes.isNullOrEmpty()
                                val file = ImageCacheManager.getLocalFilePath(
                                    context,
                                    phone.phoneDocId,
                                    colorData.colorName,
                                    isHighRes
                                )
                                if (file.exists()) file.delete()

                                // Also delete the other resolution if exists
                                val otherFile = ImageCacheManager.getLocalFilePath(
                                    context,
                                    phone.phoneDocId,
                                    colorData.colorName,
                                    !isHighRes
                                )
                                if (otherFile.exists()) otherFile.delete()
                            }

                            // Re-download all images
                            val total = colorsWithImages.size
                            colorsWithImages.forEachIndexed { index, colorData ->
                                if (!colorData.remoteUrl.isNullOrEmpty()) {
                                    redownloadProgress = "Downloading ${index + 1}/$total..."

                                    val isHighRes = phoneImages?.getImagesForColor(colorData.colorName)?.lowRes.isNullOrEmpty()
                                    val cachedPath = ImageCacheManager.downloadAndCacheImage(
                                        context = context,
                                        imageUrl = colorData.remoteUrl,
                                        phoneDocId = phone.phoneDocId,
                                        colorName = colorData.colorName,
                                        isHighRes = isHighRes ?: false
                                    )

                                    // Update state with new cached path
                                    if (cachedPath != null) {
                                        colorsWithImages = colorsWithImages.toMutableList().apply {
                                            this[index] = colorData.copy(
                                                imageUrl = cachedPath,
                                                isCached = true
                                            )
                                        }
                                    }
                                }
                            }

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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .combinedClickable(
                onClick = { onClick() },
                onLongClick = { showRedownloadDialog = true }
            ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F8))
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

                    Text(
                        text = formatModelName(phone.model),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A),
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // First row: OS, Battery, Front Cam, Rear Cam
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 48.dp, end = 0.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    SpecItem(label = "OS", value = phone.os.ifEmpty { "N/A" }, iconRes = R.raw.os_icon, modifier = Modifier.weight(1f))
                    SpecItem(label = "Battery", value = if (phone.batteryCapacity > 0) "${formatter.format(phone.batteryCapacity)} mAh" else "N/A", iconRes = R.raw.battery_icon, modifier = Modifier.weight(1f))
                    SpecItem(label = "Front Cam", value = phone.frontCamera.ifEmpty { "N/A" }, iconRes = R.raw.camera_icon, modifier = Modifier.weight(1f))
                    SpecItem(label = "Rear Cam", value = phone.rearCamera.ifEmpty { "N/A" }, iconRes = R.raw.rear_camera_icon, modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Second row: Display Size, Refresh Rate, Charging, Network
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 48.dp, end = 0.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    SpecItem(label = "Display Size", value = if (phone.displaySize.isNotEmpty()) "${phone.displaySize} Inches" else "N/A", iconRes = R.raw.screen_size_icon, modifier = Modifier.weight(1f))
                    SpecItem(label = "Refresh Rate", value = if (phone.refreshRate > 0) "${phone.refreshRate} Hz" else "N/A", iconRes = R.raw.refresh_rate_icon, modifier = Modifier.weight(1f))
                    SpecItem(label = "Charging", value = if (phone.wiredCharging > 0) "${phone.wiredCharging}W" else "N/A", iconRes = R.raw.charging_icon, modifier = Modifier.weight(1f))
                    SpecItem(label = "Network", value = phone.network.ifEmpty { "N/A" }, iconRes = R.raw.network_icon, modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Bottom section - RAM, Storage, Price
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 48.dp, end = 0.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // First 3 columns worth of space for RAM and Storage
                    Row(
                        modifier = Modifier.weight(3f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.White,
                            modifier = Modifier.border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(6.dp))
                        ) {
                            Text(
                                text = "${phone.ram}GB RAM",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                fontSize = 13.sp,
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
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                fontSize = 13.sp,
                                color = Color(0xFF555555)
                            )
                        }
                    }

                    // Price aligned with 4th column (Charging/Rear Cam)
                    Text(
                        text = "₱${String.format("%,.2f", phone.retailPrice)}",
                        modifier = Modifier.weight(1.5f),
                        fontSize = 24.sp,
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
                        .width(140.dp)  // Fixed width to prevent layout shifts
                        .padding(start = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (colorsWithImages.isNotEmpty()) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            val colorData = colorsWithImages[page]

                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (colorData.imageUrl != null) {
                                    var isImageLoading by remember { mutableStateOf(true) }

                                    // Use File for local cached images, URL string for remote
                                    val imageModel = if (colorData.isCached) {
                                        java.io.File(colorData.imageUrl)
                                    } else {
                                        colorData.imageUrl
                                    }

                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(imageModel)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "${phone.manufacturer} ${phone.model} in ${colorData.colorName}",
                                        modifier = Modifier.fillMaxHeight(),
                                        contentScale = ContentScale.FillHeight,
                                        onState = { state ->
                                            isImageLoading = state is AsyncImagePainter.State.Loading
                                        }
                                    )

                                    if (isImageLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp
                                        )
                                    }
                                } else {
                                    // No image uploaded - show text
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
                        }
                    } else {
                        // No colors at all - show text
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
fun SpecItem(
    label: String,
    value: String,
    iconRes: Int? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (iconRes != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(iconRes)
                        .decoderFactory(SvgDecoder.Factory())
                        .build(),
                    contentDescription = label,
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

@Composable
fun ColorDot(
    colorName: String,
    hexColor: String = ""
) {
    // Use hex color from phone_images if provided, otherwise fall back to default
    val backgroundColor = if (hexColor.isNotEmpty()) {
        hexToColor(hexColor)
    } else {
        getColorFromName(colorName)
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
}