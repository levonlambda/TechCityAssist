package com.techcity.techcityassist

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.techcity.techcityassist.ui.theme.TechCityAssistTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TechCityAssistTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    HomeScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Sync state
    var isSyncing by remember { mutableStateOf(false) }
    var syncStatus by remember { mutableStateOf("") }
    var syncError by remember { mutableStateOf<String?>(null) }

    // Track if data is synced (to show status)
    var isSynced by remember { mutableStateOf(false) }
    var lastSyncTime by remember { mutableStateOf("") }

    // Loading state for initial local data check
    var isCheckingLocalData by remember { mutableStateOf(true) }

    // On startup: Check for local data from today
    LaunchedEffect(Unit) {
        if (SyncDataManager.hasTodaySync(context)) {
            // Load from local storage
            syncStatus = "Loading cached data..."
            val localData = withContext(Dispatchers.IO) {
                SyncDataManager.loadSyncedData(context)
            }

            if (localData != null) {
                val (devices, images) = localData
                PhoneListHolder.setSyncedData(devices, images)
                isSynced = true
                lastSyncTime = SyncDataManager.getTimeSinceSync(context)
                Log.d("MainActivity", "Loaded ${devices.size} devices from local storage")
            }
            syncStatus = ""
        }
        isCheckingLocalData = false
    }

    // Update sync status when PhoneListHolder changes
    LaunchedEffect(PhoneListHolder.lastSyncTime) {
        if (PhoneListHolder.isSynced) {
            isSynced = true
            lastSyncTime = SyncDataManager.getTimeSinceSync(context)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFCF9F5)),  // Warm off-white background
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Top spacer - smaller to push content up
            Spacer(modifier = Modifier.weight(0.45f))

            // Logo
            Image(
                painter = painterResource(id = R.drawable.tc_logo_round),
                contentDescription = "Tech City Logo",
                modifier = Modifier
                    .size(280.dp)
            )

            Spacer(modifier = Modifier.height(80.dp))

            // Phones button - disabled until synced
            HomeButton(
                text = "Phones",
                onClick = {
                    val intent = Intent(context, PhoneListActivity::class.java).apply {
                        putExtra("DEVICE_TYPE", "phone")
                    }
                    context.startActivity(intent)
                },
                enabled = isSynced && !isSyncing && !isCheckingLocalData
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tablets button - disabled until synced
            HomeButton(
                text = "Tablets",
                onClick = {
                    val intent = Intent(context, PhoneListActivity::class.java).apply {
                        putExtra("DEVICE_TYPE", "tablet")
                    }
                    context.startActivity(intent)
                },
                enabled = isSynced && !isSyncing && !isCheckingLocalData
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Laptops button - disabled until synced
            HomeButton(
                text = "Laptops",
                onClick = {
                    val intent = Intent(context, PhoneListActivity::class.java).apply {
                        putExtra("DEVICE_TYPE", "laptop")
                    }
                    context.startActivity(intent)
                },
                enabled = isSynced && !isSyncing && !isCheckingLocalData
            )

            // Bottom spacer - larger to push content up
            Spacer(modifier = Modifier.weight(2f))

            // Sync status text
            if (isSynced && !isCheckingLocalData) {
                Text(
                    text = "Last synced: $lastSyncTime",
                    fontSize = 12.sp,
                    color = Color(0xFF666666)
                )
                Text(
                    text = "${PhoneListHolder.allDevices.size} devices cached",
                    fontSize = 11.sp,
                    color = Color(0xFF999999)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Sync progress/status
            if (isSyncing || isCheckingLocalData) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF6200EE)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isCheckingLocalData) "Checking local data..." else syncStatus,
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                }
            }

            // Error message
            if (syncError != null) {
                Text(
                    text = syncError!!,
                    fontSize = 12.sp,
                    color = Color(0xFFE53935),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // SYNC button at bottom
            // Now always enabled (except when syncing) so user can force re-sync
            Button(
                onClick = {
                    scope.launch {
                        isSyncing = true
                        syncError = null

                        try {
                            syncAllData(
                                context = context,
                                onProgress = { status -> syncStatus = status }
                            )
                            isSynced = true
                            lastSyncTime = SyncDataManager.getTimeSinceSync(context)
                        } catch (e: Exception) {
                            Log.e("MainActivity", "Sync failed", e)
                            syncError = "Sync failed: ${e.message}"
                        } finally {
                            isSyncing = false
                            syncStatus = ""
                        }
                    }
                },
                enabled = !isSyncing && !isCheckingLocalData,
                modifier = Modifier
                    .width(320.dp)
                    .height(56.dp)
                    .border(1.dp, Color(0xFFDDDDDD), RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSynced) Color(0xFF4CAF50) else Color(0xFF6200EE),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFFE0E0E0),
                    disabledContentColor = Color(0xFF999999)
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 2.dp,
                    pressedElevation = 4.dp
                )
            ) {
                Text(
                    text = when {
                        isSyncing -> "SYNCING"
                        isSynced -> "SYNCED ✓"
                        else -> "SYNC"
                    },
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * Sync all data from Firebase and store in PhoneListHolder + local storage
 * OPTION C: Now includes image preloading during sync for instant display
 */
suspend fun syncAllData(
    context: android.content.Context,
    onProgress: (String) -> Unit
) = withContext(Dispatchers.IO) {
    val db = FirebaseFirestore.getInstance()

    onProgress("Fetching device specs...")

    // Step 1: Fetch all phones specs
    val phonesResult = db.collection("phones").get().await()

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

    onProgress("Fetching inventory (${specsMap.size} specs loaded)...")

    // Step 2: Fetch all inventory
    val inventoryResult = db.collection("inventory")
        .whereIn("status", listOf("On-Hand", "On-Display"))
        .get()
        .await()

    onProgress("Processing ${inventoryResult.size()} inventory items...")

    // Step 3: Group inventory into devices
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

    onProgress("Fetching image URLs for ${grouped.size} devices...")

    // Step 4: Fetch all phone images metadata
    val phoneDocIds = grouped.mapNotNull { it.phoneDocId.ifEmpty { null } }.distinct()

    val imagesMap = mutableMapOf<String, PhoneImages>()

    if (phoneDocIds.isNotEmpty()) {
        // Batch fetch in chunks of 30 (Firestore limit)
        phoneDocIds.chunked(30).forEachIndexed { index, batch ->
            onProgress("Fetching image URLs batch ${index + 1}/${(phoneDocIds.size + 29) / 30}...")

            val imageResults = db.collection("phone_images")
                .whereIn("phoneDocId", batch)
                .get()
                .await()

            imageResults.documents.forEach { doc ->
                val phoneImages = parsePhoneImagesDocumentSync(doc.id, doc.data)
                if (phoneImages != null) {
                    imagesMap[phoneImages.phoneDocId] = phoneImages
                }
            }
        }

        // Also try to fetch by document ID directly for any missing
        val missingIds = phoneDocIds.filter { it !in imagesMap }
        missingIds.forEach { docId ->
            try {
                val doc = db.collection("phone_images").document(docId).get().await()
                if (doc.exists()) {
                    val phoneImages = parsePhoneImagesDocumentSync(doc.id, doc.data)
                    if (phoneImages != null) {
                        imagesMap[docId] = phoneImages
                    }
                }
            } catch (e: Exception) {
                Log.w("MainActivity", "Could not fetch images for $docId", e)
            }
        }
    }

    // ============================================
    // OPTION C: PRELOAD ALL IMAGES DURING SYNC
    // This downloads images to local cache so they
    // display instantly when viewing phone list
    // ============================================
    onProgress("Preparing image preload...")

    // Data class to track images that need downloading
    data class ImageToDownload(
        val phoneDocId: String,
        val colorName: String,
        val imageUrl: String,
        val isHighRes: Boolean
    )

    val imagesToDownload = mutableListOf<ImageToDownload>()

    // Collect all image URLs that need to be downloaded
    grouped.forEach { phone ->
        val phoneImages = imagesMap[phone.phoneDocId]
        if (phoneImages != null) {
            phone.colors.forEach { colorName ->
                val colorImages = phoneImages.getImagesForColor(colorName)
                if (colorImages != null) {
                    // Prefer lowRes for list view (smaller file, faster download)
                    val imageUrl = colorImages.lowRes.ifEmpty { colorImages.highRes }
                    val isHighRes = colorImages.lowRes.isEmpty() && colorImages.highRes.isNotEmpty()

                    if (imageUrl.isNotEmpty()) {
                        // Only add if not already cached
                        if (!ImageCacheManager.isImageCached(context, phone.phoneDocId, colorName, isHighRes)) {
                            imagesToDownload.add(
                                ImageToDownload(
                                    phoneDocId = phone.phoneDocId,
                                    colorName = colorName,
                                    imageUrl = imageUrl,
                                    isHighRes = isHighRes
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    // Download images with progress updates
    val totalImages = imagesToDownload.size
    if (totalImages > 0) {
        onProgress("Downloading $totalImages images...")
        Log.d("MainActivity", "Starting download of $totalImages images")

        var successCount = 0
        var failCount = 0

        imagesToDownload.forEachIndexed { index, imageInfo ->
            // Update progress every 5 images or at key milestones
            if (index % 5 == 0 || index == totalImages - 1) {
                val percent = ((index + 1) * 100) / totalImages
                onProgress("Downloading images: ${index + 1}/$totalImages ($percent%)")
            }

            try {
                val result = ImageCacheManager.downloadAndCacheImage(
                    context = context,
                    imageUrl = imageInfo.imageUrl,
                    phoneDocId = imageInfo.phoneDocId,
                    colorName = imageInfo.colorName,
                    isHighRes = imageInfo.isHighRes
                )
                if (result != null) {
                    successCount++
                } else {
                    failCount++
                }
            } catch (e: Exception) {
                failCount++
                Log.w("MainActivity", "Failed to download image for ${imageInfo.phoneDocId}/${imageInfo.colorName}: ${e.message}")
                // Continue with other images even if one fails
            }
        }

        Log.d("MainActivity", "Image download complete: $successCount success, $failCount failed")
        onProgress("Downloaded $successCount images" + if (failCount > 0) " ($failCount failed)" else "")
    } else {
        onProgress("All images already cached!")
        Log.d("MainActivity", "All images already cached, skipping download")
    }
    // ============================================
    // END OPTION C
    // ============================================

    onProgress("Saving ${grouped.size} devices, ${imagesMap.size} image sets...")

    // Step 5: Store in PhoneListHolder (in-memory)
    PhoneListHolder.setSyncedData(grouped, imagesMap)

    // Step 6: Save to local storage for persistence
    onProgress("Saving to local storage...")
    SyncDataManager.saveSyncedData(context, grouped, imagesMap)

    onProgress("Sync complete!")
}

/**
 * Parse phone images document (sync version without Log import conflicts)
 */
@Suppress("UNCHECKED_CAST")
private fun parsePhoneImagesDocumentSync(docId: String, data: Map<String, Any>?): PhoneImages? {
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
        Log.e("MainActivity", "Error parsing phone images document: $docId", e)
        null
    }
}

@Composable
fun HomeButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    // Matching PhoneListActivity FilterChip style - white background with border
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .width(320.dp)
            .height(56.dp)
            .border(1.dp, Color(0xFFDDDDDD), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = Color(0xFF333333),
            disabledContainerColor = Color(0xFFF5F5F5),
            disabledContentColor = Color(0xFFAAAAAA)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        )
    ) {
        Text(
            text = text.uppercase(),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
    }
}