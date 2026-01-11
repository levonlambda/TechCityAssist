package com.techcity.techcityassist

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Floating Action Button to access the Image Management screen
 *
 * Usage in your screen:
 *
 * Box(modifier = Modifier.fillMaxSize()) {
 *     // Your existing content
 *     LazyColumn(...) { ... }
 *
 *     // Add this at the bottom-right
 *     AdminAccessButton(
 *         context = LocalContext.current,
 *         modifier = Modifier
 *             .align(Alignment.BottomEnd)
 *             .padding(16.dp)
 *     )
 * }
 */
@Composable
fun AdminAccessButton(
    context: Context,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = {
            val intent = Intent(context, ImageManagementActivity::class.java)
            context.startActivity(intent)
        },
        modifier = modifier,
        containerColor = Color(0xFF6200EE)
    ) {
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = "Image Management",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * Alternative: Simple function to navigate to Image Management
 *
 * Usage:
 * Button(onClick = { navigateToImageManagement(context) }) {
 *     Text("Manage Images")
 * }
 */
fun navigateToImageManagement(context: Context) {
    val intent = Intent(context, ImageManagementActivity::class.java)
    context.startActivity(intent)
}