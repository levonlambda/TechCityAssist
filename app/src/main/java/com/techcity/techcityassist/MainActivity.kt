package com.techcity.techcityassist

import androidx.compose.ui.layout.SubcomposeLayout
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.ColorFilter
import android.os.Bundle
import android.util.Log
import androidx.compose.ui.text.style.TextAlign
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import java.text.NumberFormat
import java.util.Locale

// ============================================
// TEST MODE - Set to false for production
// ============================================
const val TEST_MODE = true
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
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PhoneListScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

data class DeviceSpecs(
    val docId: String = "",
    val chipset: String = "",
    val frontCamera: String = "",
    val rearCamera: String = "",
    val battery: Int = 0,
    val os: String = "",
    val network: String = "",  // NEW
    val display: String = "",
    val displaySize: String = "",
    val resolution: String = "",
    val refreshRate: Int = 0,
    val wiredCharging: Int = 0
)

@Composable
fun PhoneListScreen(modifier: Modifier = Modifier) {
    var phones by remember { mutableStateOf<List<Phone>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedPhone by remember { mutableStateOf<Phone?>(null) }

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
                            network = phoneDoc.getString("network") ?: "",  // NEW
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
                                                    network = specs.network,  // NEW
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
                                    }
                                    isLoading = false
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
                            network = doc.getString("network") ?: "",  // NEW
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
                                        network = specs.network,  // NEW
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
                            isLoading = false
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
                    onClick = { selectedPhone = phone }
                )
            }
        }
    }
}

@Composable
fun PhoneCard(phone: Phone, onClick: () -> Unit) {
    val formatter = NumberFormat.getNumberInstance(Locale.US)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .clickable { onClick() },
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
                // Phone image
                Image(
                    painter = painterResource(id = R.drawable.phone_placeholder),
                    contentDescription = "${phone.manufacturer} ${phone.model}",
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                    contentScale = ContentScale.FillHeight
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Color dots below image
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    phone.colors.forEach { color ->
                        ColorDot(colorName = color)
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
fun ColorDot(colorName: String) {
    val backgroundColor = getColorFromName(colorName)

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