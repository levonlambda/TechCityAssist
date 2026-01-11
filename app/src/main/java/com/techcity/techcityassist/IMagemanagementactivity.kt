package com.techcity.techcityassist

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.techcity.techcityassist.ui.theme.TechCityAssistTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ImageManagementActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TechCityAssistTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ImageManagementScreen(
                        modifier = Modifier.padding(innerPadding),
                        onBackPress = { finish() }
                    )
                }
            }
        }
    }
}

// Data class to hold phone model info from phones collection
data class PhoneModel(
    val docId: String,
    val manufacturer: String,
    val model: String,
    val colors: List<String> = emptyList()  // Will be populated from inventory
)

// Data class for color image status
data class ColorImageStatus(
    val colorName: String,
    val hasHighRes: Boolean = false,
    val hasLowRes: Boolean = false,
    val highResUrl: String = "",
    val lowResUrl: String = "",
    val hexColor: String = ""  // Custom hex color (optional)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageManagementScreen(
    modifier: Modifier = Modifier,
    onBackPress: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // State variables
    var phoneModels by remember { mutableStateOf<List<PhoneModel>>(emptyList()) }
    var manufacturers by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedManufacturer by remember { mutableStateOf<String?>(null) }
    var selectedPhone by remember { mutableStateOf<PhoneModel?>(null) }
    var colorStatuses by remember { mutableStateOf<List<ColorImageStatus>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf("") }
    var showManufacturerDropdown by remember { mutableStateOf(false) }
    var showPhoneDropdown by remember { mutableStateOf(false) }
    var showSyncDialog by remember { mutableStateOf(false) }
    var syncResult by remember { mutableStateOf<String?>(null) }

    // Filtered phone models based on selected manufacturer
    val filteredPhoneModels = remember(selectedManufacturer, phoneModels) {
        if (selectedManufacturer == null) {
            emptyList()
        } else {
            phoneModels.filter { it.manufacturer == selectedManufacturer }
        }
    }

    // Currently selected color for upload
    var selectedColorForUpload by remember { mutableStateOf<String?>(null) }
    var isHighResUpload by remember { mutableStateOf(true) }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && selectedPhone != null && selectedColorForUpload != null) {
            scope.launch {
                isUploading = true
                uploadProgress = "Uploading ${if (isHighResUpload) "high-res" else "low-res"} image..."

                val success = uploadImageToStorage(
                    context = context,
                    phoneDocId = selectedPhone!!.docId,
                    colorName = selectedColorForUpload!!,
                    isHighRes = isHighResUpload,
                    imageUri = uri
                )

                if (success) {
                    uploadProgress = "Upload complete! Refreshing..."
                    // Refresh the color statuses
                    loadColorStatuses(selectedPhone!!) { statuses ->
                        colorStatuses = statuses
                    }
                    Toast.makeText(context, "Image uploaded successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Upload failed. Please try again.", Toast.LENGTH_SHORT).show()
                }

                isUploading = false
                uploadProgress = ""
                selectedColorForUpload = null
            }
        }
    }

    // Load phone models on start
    LaunchedEffect(Unit) {
        loadPhoneModels { models ->
            phoneModels = models
            // Extract unique manufacturers and sort them
            manufacturers = models.map { it.manufacturer }.distinct().sorted()
            isLoading = false
        }
    }

    // Reset selected phone when manufacturer changes
    LaunchedEffect(selectedManufacturer) {
        selectedPhone = null
        colorStatuses = emptyList()
    }

    // Load color statuses when phone is selected
    LaunchedEffect(selectedPhone) {
        if (selectedPhone != null) {
            isLoading = true
            loadColorStatuses(selectedPhone!!) { statuses ->
                colorStatuses = statuses
                isLoading = false
            }
        }
    }

    // Sync Dialog
    if (showSyncDialog) {
        AlertDialog(
            onDismissRequest = { showSyncDialog = false },
            title = { Text("Sync Phone Images") },
            text = {
                Column {
                    Text("This will scan all phones and:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• Create missing phone_images documents")
                    Text("• Add new colors to existing documents")
                    Spacer(modifier = Modifier.height(12.dp))
                    if (syncResult != null) {
                        Text(
                            text = syncResult!!,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            syncPhoneImagesCollection { result ->
                                syncResult = result
                                // Refresh phone models
                                loadPhoneModels { models ->
                                    phoneModels = models
                                    manufacturers = models.map { it.manufacturer }.distinct().sorted()
                                    isLoading = false
                                }
                            }
                        }
                    },
                    enabled = !isLoading
                ) {
                    Text("Run Sync")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSyncDialog = false
                    syncResult = null
                }) {
                    Text("Close")
                }
            }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Top App Bar
        TopAppBar(
            title = { Text("Image Management") },
            navigationIcon = {
                IconButton(onClick = onBackPress) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = { showSyncDialog = true }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Sync")
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Manufacturer Selector
            Text(
                text = "Select Manufacturer",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Dropdown for manufacturer selection
            ExposedDropdownMenuBox(
                expanded = showManufacturerDropdown,
                onExpandedChange = { showManufacturerDropdown = it }
            ) {
                OutlinedTextField(
                    value = selectedManufacturer ?: "Select a manufacturer...",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = showManufacturerDropdown,
                    onDismissRequest = { showManufacturerDropdown = false }
                ) {
                    manufacturers.forEach { manufacturer ->
                        DropdownMenuItem(
                            text = { Text(manufacturer) },
                            onClick = {
                                selectedManufacturer = manufacturer
                                showManufacturerDropdown = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Phone Model Selector (only shown after manufacturer is selected)
            if (selectedManufacturer != null) {
                Text(
                    text = "Select Phone Model",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Dropdown for phone selection
                ExposedDropdownMenuBox(
                    expanded = showPhoneDropdown,
                    onExpandedChange = { showPhoneDropdown = it }
                ) {
                    OutlinedTextField(
                        value = selectedPhone?.model ?: "Select a phone...",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = showPhoneDropdown,
                        onDismissRequest = { showPhoneDropdown = false }
                    ) {
                        filteredPhoneModels.forEach { phone ->
                            DropdownMenuItem(
                                text = { Text(phone.model) },
                                onClick = {
                                    selectedPhone = phone
                                    showPhoneDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Upload Progress
            if (isUploading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    text = uploadProgress,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Color List with Image Upload Options
            if (selectedPhone != null) {
                Text(
                    text = "Colors for ${selectedPhone!!.model}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (colorStatuses.isEmpty()) {
                    Text(
                        text = "No colors found for this phone.",
                        color = Color.Gray
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(colorStatuses) { colorStatus ->
                            ColorImageCard(
                                colorStatus = colorStatus,
                                phoneDocId = selectedPhone!!.docId,
                                onUploadHighRes = {
                                    selectedColorForUpload = colorStatus.colorName
                                    isHighResUpload = true
                                    imagePickerLauncher.launch("image/*")
                                },
                                onUploadLowRes = {
                                    selectedColorForUpload = colorStatus.colorName
                                    isHighResUpload = false
                                    imagePickerLauncher.launch("image/*")
                                },
                                onHexColorSaved = {
                                    // Refresh color statuses after hex color is saved
                                    loadColorStatuses(selectedPhone!!) { statuses ->
                                        colorStatuses = statuses
                                    }
                                },
                                isUploading = isUploading
                            )
                        }
                    }
                }
            } else if (selectedManufacturer != null) {
                // Manufacturer selected but no phone selected
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Select a phone model to manage images",
                        color = Color.Gray
                    )
                }
            } else {
                // Initial state - no manufacturer selected
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Select a manufacturer to get started",
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun ColorImageCard(
    colorStatus: ColorImageStatus,
    phoneDocId: String,
    onUploadHighRes: () -> Unit,
    onUploadLowRes: () -> Unit,
    onHexColorSaved: () -> Unit,
    isUploading: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // State for hex color input
    var hexColorInput by remember(colorStatus.hexColor) {
        mutableStateOf(colorStatus.hexColor)
    }
    var isSavingHexColor by remember { mutableStateOf(false) }
    var hexColorError by remember { mutableStateOf<String?>(null) }

    // Check if hex color has changed from saved value
    val hasUnsavedHexColor = hexColorInput != colorStatus.hexColor

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Color name header with preview
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Color preview circle
                    val previewColor = if (hexColorInput.isNotEmpty() && isValidHexColor(hexColorInput)) {
                        parseHexColor(hexColorInput)
                    } else {
                        getColorFromName(colorStatus.colorName)
                    }

                    Surface(
                        modifier = Modifier
                            .size(32.dp)
                            .border(1.dp, Color.Gray, CircleShape),
                        shape = CircleShape,
                        color = previewColor
                    ) {}

                    Text(
                        text = colorStatus.colorName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }

                // Status indicators
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusChip(
                        label = "High",
                        isComplete = colorStatus.hasHighRes
                    )
                    StatusChip(
                        label = "Low",
                        isComplete = colorStatus.hasLowRes
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Hex Color Input Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = hexColorInput,
                    onValueChange = { value ->
                        // Only allow valid hex characters
                        val filtered = value.uppercase().filter {
                            it in "0123456789ABCDEF#"
                        }
                        // Ensure # is at the start if present
                        hexColorInput = if (filtered.startsWith("#")) {
                            "#" + filtered.drop(1).take(6)
                        } else {
                            filtered.take(6)
                        }
                        hexColorError = null
                    },
                    label = { Text("Hex Color (optional)") },
                    placeholder = { Text("#FF5733") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    isError = hexColorError != null,
                    supportingText = if (hexColorError != null) {
                        { Text(hexColorError!!, color = MaterialTheme.colorScheme.error) }
                    } else if (hexColorInput.isEmpty()) {
                        { Text("Uses default color if empty", color = Color.Gray, fontSize = 11.sp) }
                    } else null,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters
                    )
                )

                // Save button for hex color
                Button(
                    onClick = {
                        // Validate hex color
                        if (hexColorInput.isNotEmpty() && !isValidHexColor(hexColorInput)) {
                            hexColorError = "Invalid hex format"
                            return@Button
                        }

                        scope.launch {
                            isSavingHexColor = true
                            val success = saveHexColor(
                                phoneDocId = phoneDocId,
                                colorName = colorStatus.colorName,
                                hexColor = hexColorInput
                            )
                            isSavingHexColor = false

                            if (success) {
                                Toast.makeText(context, "Color saved!", Toast.LENGTH_SHORT).show()
                                onHexColorSaved()
                            } else {
                                Toast.makeText(context, "Failed to save color", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = hasUnsavedHexColor && !isSavingHexColor && !isUploading,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    if (isSavingHexColor) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text("Save")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Image previews and upload buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // High-res section
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "High Resolution",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    if (colorStatus.hasHighRes) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(colorStatus.highResUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "High-res preview",
                            modifier = Modifier
                                .size(80.dp)
                                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No image", fontSize = 10.sp, color = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = onUploadHighRes,
                        enabled = !isUploading,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (colorStatus.hasHighRes) "Replace" else "Upload", fontSize = 12.sp)
                    }
                }

                // Low-res section
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Low Resolution",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    if (colorStatus.hasLowRes) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(colorStatus.lowResUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Low-res preview",
                            modifier = Modifier
                                .size(80.dp)
                                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No image", fontSize = 10.sp, color = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = onUploadLowRes,
                        enabled = !isUploading,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (colorStatus.hasLowRes) "Replace" else "Upload", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun StatusChip(label: String, isComplete: Boolean) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isComplete) Color(0xFF4CAF50) else Color(0xFFE0E0E0)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isComplete) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = label,
                fontSize = 10.sp,
                color = if (isComplete) Color.White else Color.Gray
            )
        }
    }
}

// ============================================
// Utility Functions
// ============================================

/**
 * Validate hex color format
 */
fun isValidHexColor(hex: String): Boolean {
    if (hex.isEmpty()) return true  // Empty is valid (means use default)
    val pattern = "^#?([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$".toRegex()
    return pattern.matches(hex)
}

/**
 * Parse hex color string to Compose Color
 */
fun parseHexColor(hex: String): Color {
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

// ============================================
// Firebase Functions
// ============================================

/**
 * Load all phone models from the phones collection
 */
fun loadPhoneModels(onComplete: (List<PhoneModel>) -> Unit) {
    val db = FirebaseFirestore.getInstance()

    db.collection("phones")
        .get()
        .addOnSuccessListener { phonesResult ->
            val phoneDocIds = phonesResult.documents.map { it.id }
            val phonesData = phonesResult.documents.associate { doc ->
                doc.id to PhoneModel(
                    docId = doc.id,
                    manufacturer = doc.getString("manufacturer") ?: "",
                    model = doc.getString("model") ?: ""
                )
            }

            // Now get colors from inventory for each phone
            db.collection("inventory")
                .get()
                .addOnSuccessListener { inventoryResult ->
                    // Group inventory by manufacturer|model and collect colors
                    val colorsMap = mutableMapOf<String, MutableSet<String>>()

                    inventoryResult.documents.forEach { doc ->
                        val manufacturer = doc.getString("manufacturer") ?: ""
                        val model = doc.getString("model") ?: ""
                        val color = doc.getString("color") ?: ""

                        if (manufacturer.isNotEmpty() && model.isNotEmpty() && color.isNotEmpty()) {
                            val key = "$manufacturer|$model"
                            colorsMap.getOrPut(key) { mutableSetOf() }.add(color)
                        }
                    }

                    // Match colors to phones
                    val phoneModels = phonesData.values.map { phone ->
                        val key = "${phone.manufacturer}|${phone.model}"
                        phone.copy(colors = colorsMap[key]?.toList()?.sorted() ?: emptyList())
                    }.sortedWith(compareBy({ it.manufacturer }, { it.model }))

                    onComplete(phoneModels)
                }
                .addOnFailureListener { e ->
                    Log.e("ImageMgmt", "Error loading inventory", e)
                    onComplete(phonesData.values.toList())
                }
        }
        .addOnFailureListener { e ->
            Log.e("ImageMgmt", "Error loading phones", e)
            onComplete(emptyList())
        }
}

/**
 * Load color statuses for a specific phone (including hex colors)
 */
fun loadColorStatuses(phone: PhoneModel, onComplete: (List<ColorImageStatus>) -> Unit) {
    val db = FirebaseFirestore.getInstance()

    // First get the phone_images document
    db.collection("phone_images").document(phone.docId)
        .get()
        .addOnSuccessListener { document ->
            val existingColors = mutableMapOf<String, ColorImageStatus>()

            if (document.exists()) {
                @Suppress("UNCHECKED_CAST")
                val colorsData = document.get("colors") as? Map<String, Map<String, String>>
                colorsData?.forEach { (colorName, data) ->
                    existingColors[colorName.lowercase()] = ColorImageStatus(
                        colorName = colorName,
                        hasHighRes = !data["highRes"].isNullOrEmpty(),
                        hasLowRes = !data["lowRes"].isNullOrEmpty(),
                        highResUrl = data["highRes"] ?: "",
                        lowResUrl = data["lowRes"] ?: "",
                        hexColor = data["hexColor"] ?: ""
                    )
                }
            }

            // Create status for all colors from inventory
            val statuses = phone.colors.map { color ->
                existingColors[color.lowercase()] ?: ColorImageStatus(colorName = color)
            }

            onComplete(statuses)
        }
        .addOnFailureListener { e ->
            Log.e("ImageMgmt", "Error loading color statuses", e)
            // Return colors without image status
            onComplete(phone.colors.map { ColorImageStatus(colorName = it) })
        }
}

/**
 * Save hex color to Firestore
 */
suspend fun saveHexColor(
    phoneDocId: String,
    colorName: String,
    hexColor: String
): Boolean {
    return try {
        val db = FirebaseFirestore.getInstance()
        val docRef = db.collection("phone_images").document(phoneDocId)
        val document = docRef.get().await()

        val fieldPath = "colors.$colorName.hexColor"

        if (document.exists()) {
            // Update existing document
            docRef.update(fieldPath, hexColor).await()
        } else {
            // Create new document with this color
            val newData = hashMapOf(
                "phoneDocId" to phoneDocId,
                "colors" to hashMapOf(
                    colorName to hashMapOf(
                        "highRes" to "",
                        "lowRes" to "",
                        "hexColor" to hexColor
                    )
                )
            )
            docRef.set(newData).await()
        }

        Log.d("ImageMgmt", "Successfully saved hex color for $colorName: $hexColor")
        true
    } catch (e: Exception) {
        Log.e("ImageMgmt", "Error saving hex color", e)
        false
    }
}

/**
 * Upload image to Firebase Storage and update Firestore
 */
suspend fun uploadImageToStorage(
    context: android.content.Context,
    phoneDocId: String,
    colorName: String,
    isHighRes: Boolean,
    imageUri: Uri
): Boolean {
    return try {
        val storage = FirebaseStorage.getInstance()
        val db = FirebaseFirestore.getInstance()

        val resolution = if (isHighRes) "high" else "low"
        val fileName = "${colorName.lowercase().replace(" ", "_")}_$resolution.png"
        val storagePath = "phone_images/$phoneDocId/$fileName"

        val storageRef = storage.reference.child(storagePath)

        // Upload the file
        storageRef.putFile(imageUri).await()

        // Get the download URL
        val downloadUrl = storageRef.downloadUrl.await().toString()

        // Update Firestore document
        val docRef = db.collection("phone_images").document(phoneDocId)
        val document = docRef.get().await()

        val fieldPath = if (isHighRes) {
            "colors.$colorName.highRes"
        } else {
            "colors.$colorName.lowRes"
        }

        if (document.exists()) {
            // Update existing document
            docRef.update(fieldPath, downloadUrl).await()
        } else {
            // Create new document with this color
            val newData = hashMapOf(
                "phoneDocId" to phoneDocId,
                "colors" to hashMapOf(
                    colorName to hashMapOf(
                        "highRes" to if (isHighRes) downloadUrl else "",
                        "lowRes" to if (isHighRes) "" else downloadUrl,
                        "hexColor" to ""
                    )
                )
            )
            docRef.set(newData).await()
        }

        Log.d("ImageMgmt", "Successfully uploaded: $storagePath")
        true
    } catch (e: Exception) {
        Log.e("ImageMgmt", "Error uploading image", e)
        false
    }
}

/**
 * Sync utility: Create missing phone_images documents and add new colors
 */
fun syncPhoneImagesCollection(onComplete: (String) -> Unit) {
    val db = FirebaseFirestore.getInstance()

    var documentsCreated = 0
    var colorsAdded = 0

    // Get all phones
    db.collection("phones")
        .get()
        .addOnSuccessListener { phonesResult ->
            val phones = phonesResult.documents.map { doc ->
                PhoneModel(
                    docId = doc.id,
                    manufacturer = doc.getString("manufacturer") ?: "",
                    model = doc.getString("model") ?: ""
                )
            }

            // Get colors from inventory
            db.collection("inventory")
                .get()
                .addOnSuccessListener { inventoryResult ->
                    val colorsMap = mutableMapOf<String, MutableSet<String>>()

                    inventoryResult.documents.forEach { doc ->
                        val manufacturer = doc.getString("manufacturer") ?: ""
                        val model = doc.getString("model") ?: ""
                        val color = doc.getString("color") ?: ""

                        if (manufacturer.isNotEmpty() && model.isNotEmpty() && color.isNotEmpty()) {
                            val key = "$manufacturer|$model"
                            colorsMap.getOrPut(key) { mutableSetOf() }.add(color)
                        }
                    }

                    // Get existing phone_images documents
                    db.collection("phone_images")
                        .get()
                        .addOnSuccessListener { phoneImagesResult ->
                            val existingDocs = phoneImagesResult.documents.associate { doc ->
                                @Suppress("UNCHECKED_CAST")
                                val colors = doc.get("colors") as? Map<String, Any> ?: emptyMap()
                                doc.id to colors.keys.map { it.lowercase() }.toSet()
                            }

                            var pendingOperations = 0
                            var completedOperations = 0

                            phones.forEach { phone ->
                                val key = "${phone.manufacturer}|${phone.model}"
                                val inventoryColors = colorsMap[key] ?: emptySet()

                                if (inventoryColors.isEmpty()) return@forEach

                                pendingOperations++

                                if (!existingDocs.containsKey(phone.docId)) {
                                    // Create new document
                                    val colorMap = inventoryColors.associate { color ->
                                        color to hashMapOf(
                                            "highRes" to "",
                                            "lowRes" to "",
                                            "hexColor" to ""
                                        )
                                    }

                                    val newDoc = hashMapOf(
                                        "phoneDocId" to phone.docId,
                                        "manufacturer" to phone.manufacturer,
                                        "model" to phone.model,
                                        "colors" to colorMap
                                    )

                                    db.collection("phone_images")
                                        .document(phone.docId)
                                        .set(newDoc)
                                        .addOnSuccessListener {
                                            documentsCreated++
                                            colorsAdded += inventoryColors.size
                                            completedOperations++

                                            if (completedOperations == pendingOperations) {
                                                onComplete("Created $documentsCreated documents, added $colorsAdded colors")
                                            }
                                        }
                                        .addOnFailureListener {
                                            completedOperations++
                                            if (completedOperations == pendingOperations) {
                                                onComplete("Created $documentsCreated documents, added $colorsAdded colors")
                                            }
                                        }
                                } else {
                                    // Check for new colors
                                    val existingColors = existingDocs[phone.docId] ?: emptySet()
                                    val newColors = inventoryColors.filter {
                                        it.lowercase() !in existingColors
                                    }

                                    if (newColors.isNotEmpty()) {
                                        val updates = mutableMapOf<String, Any>()
                                        newColors.forEach { color ->
                                            updates["colors.$color"] = hashMapOf(
                                                "highRes" to "",
                                                "lowRes" to "",
                                                "hexColor" to ""
                                            )
                                        }

                                        db.collection("phone_images")
                                            .document(phone.docId)
                                            .update(updates)
                                            .addOnSuccessListener {
                                                colorsAdded += newColors.size
                                                completedOperations++

                                                if (completedOperations == pendingOperations) {
                                                    onComplete("Created $documentsCreated documents, added $colorsAdded colors")
                                                }
                                            }
                                            .addOnFailureListener {
                                                completedOperations++
                                                if (completedOperations == pendingOperations) {
                                                    onComplete("Created $documentsCreated documents, added $colorsAdded colors")
                                                }
                                            }
                                    } else {
                                        completedOperations++
                                        if (completedOperations == pendingOperations) {
                                            onComplete("Created $documentsCreated documents, added $colorsAdded colors")
                                        }
                                    }
                                }
                            }

                            if (pendingOperations == 0) {
                                onComplete("No changes needed - all phones and colors are synced")
                            }
                        }
                }
        }
        .addOnFailureListener { e ->
            Log.e("ImageMgmt", "Error during sync", e)
            onComplete("Error: ${e.message}")
        }
}