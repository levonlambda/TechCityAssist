package com.techcity.techcityassist

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.firebase.firestore.FirebaseFirestore
import com.techcity.techcityassist.ui.theme.TechCityAssistTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

// ============================================
// DEBUG MODE - Set to false for production
// ============================================
const val SHOW_DEBUG_INFO = false
// ============================================

class MainActivity : ComponentActivity() {

    // Track if initial data loading is complete (for splash screen)
    private var isDataReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen BEFORE super.onCreate()
        val splashScreen = installSplashScreen()

        // Keep the splash screen visible while loading initial data
        splashScreen.setKeepOnScreenCondition {
            !isDataReady
        }

        super.onCreate(savedInstanceState)

        // Auth gate: Firebase Auth persists the session on-device, so this is
        // an instant local check — the login screen appears only when no user
        // is signed in (first launch, after sign-out, or after revocation).
        if (!Authmanager.isSignedIn()) {
            isDataReady = true  // release the splash before leaving
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Force portrait orientation (Android 16+ ignores manifest attribute)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        enableEdgeToEdge()
        setContent {
            TechCityAssistTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    HomeScreen(
                        modifier = Modifier.padding(innerPadding),
                        onDataReady = { isDataReady = true }
                    )
                }
            }
        }
    }
}

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onDataReady: () -> Unit = {}
) {
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

    // Which category's brand list is showing; null = the three category buttons.
    // Saveable so activity recreation doesn't snap back to the category buttons.
    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }

    // Double-tap guard: set when a brand launches PhoneListActivity, cleared
    // when the home screen resumes (i.e. the user navigated back).
    var hasLaunchedList by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) hasLaunchedList = false
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // System back closes the brand list instead of leaving the app
    BackHandler(enabled = selectedCategory != null) {
        selectedCategory = null
    }

    // Refresh the store location list (and apply the default) independently
    // of the data load so a slow/offline read never delays the splash.
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            LocationManager.loadLocations(context)
        }
    }

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

                // Pre-compute color data (filesystem I/O happens here, not during scroll)
                syncStatus = "Preparing color data..."
                val precomputedColorData = withContext(Dispatchers.IO) {
                    val colorData = mutableMapOf<String, List<ColorImageData>>()
                    devices.forEach { phone ->
                        val phoneImages = images[phone.phoneDocId]
                        val colorDataList = phone.colors.map { colorName ->
                            val colorImages = phoneImages?.getImagesForColor(colorName)
                            val remoteUrl = colorImages?.lowRes?.ifEmpty { colorImages.highRes }
                            val isHighRes = colorImages?.lowRes.isNullOrEmpty() == true && !colorImages?.highRes.isNullOrEmpty()
                            val cachedPath = ImageCacheManager.getLocalImageUri(
                                context, phone.phoneDocId, colorName, isHighRes
                            )
                            ColorImageData(
                                colorName = colorName,
                                imageUrl = cachedPath ?: remoteUrl,
                                hexColor = colorImages?.hexColor ?: "",
                                remoteUrl = remoteUrl,
                                isCached = cachedPath != null
                            )
                        }
                        val key = "${phone.phoneDocId}_${phone.ram}_${phone.storage}"
                        colorData[key] = colorDataList
                    }
                    colorData
                }

                PhoneListHolder.setSyncedData(devices, images, precomputedColorData)
                isSynced = true
                lastSyncTime = SyncDataManager.getTimeSinceSync(context)
                Log.d("MainActivity", "Loaded ${devices.size} devices from local storage")
            }
            syncStatus = ""
        }
        isCheckingLocalData = false

        // Signal that data loading is complete (dismisses splash screen)
        onDataReady()
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

            // Store location the app is set to. Sits above the category /
            // brand region so it stays put when the brand list is shown.
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = LocationManager.displayName(),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF666666)
            )
            Spacer(modifier = Modifier.height(40.dp))

            val category = selectedCategory

            // Everything below the logo shares one weighted region that is
            // identical in both modes, so the logo position never shifts
            // between the category buttons and the brand list
            Column(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            if (category == null) {
                // Phones button - disabled until synced
                HomeButton(
                    text = "Phones",
                    onClick = { selectedCategory = "phone" },
                    enabled = isSynced && !isSyncing && !isCheckingLocalData
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Tablets button - disabled until synced
                HomeButton(
                    text = "Tablets",
                    onClick = { selectedCategory = "tablet" },
                    enabled = isSynced && !isSyncing && !isCheckingLocalData
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Laptops button - disabled until synced
                HomeButton(
                    text = "Laptops",
                    onClick = { selectedCategory = "laptop" },
                    enabled = isSynced && !isSyncing && !isCheckingLocalData
                )

                // Bottom spacer - larger to push content up
                Spacer(modifier = Modifier.weight(1f))
            } else {
                // Brand buttons for the tapped category, same set and order as
                // the filter chips in PhoneListActivity
                val brands = remember(category, PhoneListHolder.lastSyncTime) {
                    sortManufacturersWithPriority(
                        PhoneListHolder.getDevicesByType(category)
                            .map { it.manufacturer }
                            .filter { it.isNotBlank() && !it.equals("techcity", ignoreCase = true) }
                            .distinct()
                    )
                }
                val categoryLabel = when (category) {
                    "phone" -> "phones"
                    "tablet" -> "tablets"
                    "laptop" -> "laptops"
                    else -> "devices"
                }

                if (brands.isEmpty()) {
                    Text(
                        text = "No $categoryLabel available",
                        fontSize = 16.sp,
                        color = Color(0xFF666666)
                    )
                } else {
                    // Up to 5 brands: single column matching the category buttons.
                    // More than 5: two-column grid so ~10 fit without scrolling;
                    // scrolling only kicks in beyond that.
                    val launchBrand: (String) -> Unit = { brand ->
                        if (!hasLaunchedList) {
                            hasLaunchedList = true
                            val intent = Intent(context, PhoneListActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                putExtra("DEVICE_TYPE", category)
                                putExtra("SELECTED_BRAND", brand)
                            }
                            context.startActivity(intent)
                        }
                    }
                    val useTwoColumns = brands.size > 5

                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (useTwoColumns) {
                            brands.chunked(2).forEachIndexed { rowIndex, rowBrands ->
                                if (rowIndex > 0) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                                Row(
                                    // widthIn must precede fillMaxWidth: once
                                    // fillMaxWidth fixes the width, a later max
                                    // cap is ignored. Capped row -> ~226dp wide,
                                    // ~92dp tall pills, so 5+ rows fit on screen.
                                    modifier = Modifier
                                        .widthIn(max = 520.dp)
                                        .fillMaxWidth(0.9f),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    rowBrands.forEach { brand ->
                                        val logoRes = brandLogoRes(brand)
                                        if (logoRes != null) {
                                            BrandLogoButton(
                                                logoRes = logoRes,
                                                brand = brand,
                                                onClick = { launchBrand(brand) },
                                                enabled = !isSyncing,
                                                modifier = Modifier.weight(1f)
                                            )
                                        } else {
                                            HomeButton(
                                                text = brand,
                                                onClick = { launchBrand(brand) },
                                                enabled = !isSyncing,
                                                modifier = Modifier.weight(1f),
                                                fontSize = 18.sp
                                            )
                                        }
                                    }
                                    // Keep a lone last button the same width as the others
                                    if (rowBrands.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        } else {
                            brands.forEachIndexed { index, brand ->
                                if (index > 0) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                                val logoRes = brandLogoRes(brand)
                                if (logoRes != null) {
                                    // Category-button width; height is the natural
                                    // pill height (~130dp) reduced ~31%, so the pill
                                    // is center-cropped at top and bottom
                                    BrandLogoButton(
                                        logoRes = logoRes,
                                        brand = brand,
                                        onClick = { launchBrand(brand) },
                                        enabled = !isSyncing,
                                        modifier = Modifier
                                            .width(320.dp)
                                            .height(90.dp),
                                        keepAspect = false
                                    )
                                } else {
                                    HomeButton(
                                        text = brand,
                                        onClick = { launchBrand(brand) },
                                        enabled = !isSyncing
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = { selectedCategory = null }) {
                    Text(
                        text = "← BACK",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF666666)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Sync controls and status only show alongside the category
            // buttons; the brand view uses that space for the brand list
            if (category == null) {
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
                            // Revoked access surfaces as PERMISSION_DENIED once
                            // the token expires; sign out and return to login.
                            if (!Authmanager.handleFirestoreError(context, e)) {
                                syncError = "Sync failed: ${e.message}"
                            }
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

            Spacer(modifier = Modifier.height(8.dp))

            // Sign out: clears cached data and returns to the login screen
            TextButton(
                onClick = { Authmanager.forceSignOut(context, accessRemoved = false) },
                enabled = !isSyncing
            ) {
                Text(
                    text = "Sign out",
                    fontSize = 13.sp,
                    color = Color(0xFF666666)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            } // end category-only sync section
            } // end weighted content region
        }

        // ============================================
        // DEBUG INFO OVERLAY
        // ============================================
        if (SHOW_DEBUG_INFO) {
            DebugScreenInfoOverlay()
        }
    }
}

/**
 * Debug overlay showing screen dimensions
 * Displays in the top-right corner
 */
@Composable
fun DebugScreenInfoOverlay() {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    // Get screen dimensions in dp
    val screenWidthDp = configuration.screenWidthDp
    val screenHeightDp = configuration.screenHeightDp

    // Get screen dimensions in pixels
    val screenWidthPx = with(density) { screenWidthDp.dp.toPx().toInt() }
    val screenHeightPx = with(density) { screenHeightDp.dp.toPx().toInt() }

    // Determine window size class
    val sizeClass = when {
        screenWidthDp < 600 -> "Compact"
        screenWidthDp < 840 -> "Medium"
        else -> "Expanded"
    }

    // Density info
    val densityDpi = density.density

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopEnd
    ) {
        Surface(
            modifier = Modifier
                .padding(top = 48.dp, end = 8.dp),
            shape = RoundedCornerShape(8.dp),
            color = Color.Black.copy(alpha = 0.75f),
            shadowElevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "🔧 DEBUG INFO",
                    color = Color.Yellow,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Screen dimensions in dp (most important!)
                Text(
                    text = "Screen Size (dp):",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 9.sp
                )
                Text(
                    text = "${screenWidthDp} x ${screenHeightDp}",
                    color = Color.Cyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Window size class
                Text(
                    text = "Size Class:",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 9.sp
                )
                Text(
                    text = sizeClass,
                    color = when(sizeClass) {
                        "Compact" -> Color.Green
                        "Medium" -> Color.Yellow
                        else -> Color(0xFFFF9800)
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Screen dimensions in pixels
                Text(
                    text = "Screen Size (px):",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 9.sp
                )
                Text(
                    text = "${screenWidthPx} x ${screenHeightPx}",
                    color = Color.White,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Density
                Text(
                    text = "Density:",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 9.sp
                )
                Text(
                    text = "${String.format("%.2f", densityDpi)}x",
                    color = Color.White,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Reminder to disable
                Text(
                    text = "Set SHOW_DEBUG_INFO",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 8.sp
                )
                Text(
                    text = "= false for production",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 8.sp
                )
            }
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
        .whereIn("status", AVAILABLE_INVENTORY_STATUSES)
        .get()
        .await()

    onProgress("Processing ${inventoryResult.size()} inventory items...")

    // Step 3: Group inventory into devices (shared with the live listener)
    val grouped = groupInventoryDocs(inventoryResult.documents, specsMap)

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

    // ============================================
    // PRE-COMPUTE COLOR DATA FOR INSTANT SCROLL
    // This avoids filesystem I/O during card composition
    // ============================================
    onProgress("Pre-computing color data...")

    val precomputedColorData = mutableMapOf<String, List<ColorImageData>>()

    grouped.forEach { phone ->
        val phoneImages = imagesMap[phone.phoneDocId]
        val colorDataList = phone.colors.map { colorName ->
            val images = phoneImages?.getImagesForColor(colorName)
            val remoteUrl = images?.lowRes?.ifEmpty { images.highRes }
            val isHighRes = images?.lowRes.isNullOrEmpty() == true && !images?.highRes.isNullOrEmpty()

            // Check cache - this I/O happens during sync, not during scroll
            val cachedPath = ImageCacheManager.getLocalImageUri(
                context, phone.phoneDocId, colorName, isHighRes
            )

            ColorImageData(
                colorName = colorName,
                imageUrl = cachedPath ?: remoteUrl,
                hexColor = images?.hexColor ?: "",
                remoteUrl = remoteUrl,
                isCached = cachedPath != null
            )
        }

        val key = "${phone.phoneDocId}_${phone.ram}_${phone.storage}"
        precomputedColorData[key] = colorDataList
    }

    Log.d("MainActivity", "Pre-computed color data for ${precomputedColorData.size} variants")
    // ============================================

    onProgress("Saving ${grouped.size} devices, ${imagesMap.size} image sets...")

    // Step 5: Store in PhoneListHolder (in-memory)
    PhoneListHolder.setSyncedData(grouped, imagesMap, precomputedColorData)

    // Step 6: Save to local storage for persistence
    onProgress("Saving to local storage...")
    SyncDataManager.saveSyncedData(context, grouped, imagesMap)

    // Step 7: Refresh the store location list (applies the default if needed)
    LocationManager.loadLocations(context)

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

// Bundled brand logo drawables (trimmed pill images); brands without a
// logo fall back to a text HomeButton
fun brandLogoRes(brand: String): Int? = when (brand.lowercase()) {
    "acer" -> R.drawable.brand_acer
    "apple" -> R.drawable.brand_apple
    "honor" -> R.drawable.brand_honor
    "infinix" -> R.drawable.brand_infinix
    "itel" -> R.drawable.brand_itel
    "oppo" -> R.drawable.brand_oppo
    "realme" -> R.drawable.brand_realme
    "samsung" -> R.drawable.brand_samsung
    "tecno" -> R.drawable.brand_tecno
    "vivo" -> R.drawable.brand_vivo
    "xiaomi" -> R.drawable.brand_xiaomi
    else -> null
}

@Composable
fun BrandLogoButton(
    logoRes: Int,
    brand: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    keepAspect: Boolean = true
) {
    Image(
        painter = painterResource(id = logoRes),
        contentDescription = brand,
        // keepAspect: fill the slot's width at the logo's natural 640x261
        // proportions. Otherwise the caller fixes the size and the pill is
        // center-cropped vertically to fill it without distorting the text.
        contentScale = if (keepAspect) ContentScale.Fit else ContentScale.Crop,
        alpha = if (enabled) 1f else 0.4f,
        modifier = modifier
            .then(if (keepAspect) Modifier.aspectRatio(640f / 261f) else Modifier)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
    )
}

@Composable
fun HomeButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier.width(320.dp),
    fontSize: TextUnit = 22.sp
) {
    // Matching PhoneListActivity FilterChip style - white background with border
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
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
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}