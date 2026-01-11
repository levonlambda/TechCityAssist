package com.techcity.techcityassist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest

/**
 * A composable that displays a phone image loaded from Firebase Storage URL
 *
 * @param imageUrl The Firebase Storage download URL
 * @param contentDescription Accessibility description
 * @param modifier Modifier for the image
 * @param useHighRes Whether to prioritize high resolution (use false for list views)
 */
@Composable
fun PhoneImage(
    imageUrl: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    useHighRes: Boolean = false
) {
    val context = LocalContext.current

    if (imageUrl.isNullOrEmpty()) {
        // Fallback to placeholder if no URL
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            // You can replace this with your placeholder image
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        }
    } else {
        var isLoading by remember { mutableStateOf(true) }
        var isError by remember { mutableStateOf(false) }

        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                onState = { state ->
                    isLoading = state is AsyncImagePainter.State.Loading
                    isError = state is AsyncImagePainter.State.Error
                }
            )

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        }
    }
}

/**
 * Example: Updated PhoneCard that uses Firebase Storage images
 *
 * This shows how you might modify PhoneCard to display actual phone images
 * instead of the placeholder.
 */
@Composable
fun PhoneCardWithImage(
    phone: Phone,
    phoneImages: PhoneImages?,  // Add this parameter
    selectedColor: String?,     // Which color variant to show
    onClick: () -> Unit
) {
    // Get the image URL for the selected color (or first available color)
    val colorToShow = selectedColor ?: phone.colors.firstOrNull() ?: ""
    val imageUrls = phoneImages?.getImagesForColor(colorToShow)

    // Use low-res for card view (faster loading, less bandwidth)
    val imageUrl = imageUrls?.lowRes

    // ... rest of your PhoneCard code ...
    // Replace the Image composable with:
    /*
    PhoneImage(
        imageUrl = imageUrl,
        contentDescription = "${phone.manufacturer} ${phone.model} in $colorToShow",
        modifier = Modifier
            .weight(1f)
            .padding(start = 8.dp)
    )
    */
}

/**
 * Example: How to load phone images in your screen
 */
@Composable
fun ExamplePhoneListWithImages() {
    var phones by remember { mutableStateOf<List<Phone>>(emptyList()) }
    var phoneImagesMap by remember { mutableStateOf<Map<String, PhoneImages>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }

    val repository = remember { PhoneImagesRepository() }

    LaunchedEffect(Unit) {
        // First, load your phones as you currently do
        // ... your existing phone loading code ...

        // Then, load images for all phones
        val phoneDocIds = phones.mapNotNull {
            it.phoneDocId.ifEmpty { null }
        }.distinct()

        if (phoneDocIds.isNotEmpty()) {
            phoneImagesMap = repository.getPhoneImagesForMultiple(phoneDocIds)
        }

        isLoading = false
    }

    // In your LazyColumn, pass the phone images:
    /*
    items(phones) { phone ->
        PhoneCardWithImage(
            phone = phone,
            phoneImages = phoneImagesMap[phone.phoneDocId],
            selectedColor = phone.colors.firstOrNull(),
            onClick = { /* ... */ }
        )
    }
    */
}

/**
 * Example: Viewing a phone detail with high-res image
 */
@Composable
fun PhoneDetailImage(
    phoneImages: PhoneImages?,
    colorName: String,
    modifier: Modifier = Modifier
) {
    val imageUrls = phoneImages?.getImagesForColor(colorName)

    // Use high-res for detail view
    PhoneImage(
        imageUrl = imageUrls?.highRes ?: imageUrls?.lowRes,
        contentDescription = "Phone in $colorName",
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp)),
        useHighRes = true
    )
}