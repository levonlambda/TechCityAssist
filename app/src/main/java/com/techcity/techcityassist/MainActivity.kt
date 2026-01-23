package com.techcity.techcityassist

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.techcity.techcityassist.ui.theme.TechCityAssistTheme

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

            // Phones button
            HomeButton(
                text = "Phones",
                onClick = {
                    val intent = Intent(context, PhoneListActivity::class.java).apply {
                        putExtra("DEVICE_TYPE", "phone")
                    }
                    context.startActivity(intent)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tablets button
            HomeButton(
                text = "Tablets",
                onClick = {
                    val intent = Intent(context, PhoneListActivity::class.java).apply {
                        putExtra("DEVICE_TYPE", "tablet")
                    }
                    context.startActivity(intent)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Laptops button
            HomeButton(
                text = "Laptops",
                onClick = {
                    val intent = Intent(context, PhoneListActivity::class.java).apply {
                        putExtra("DEVICE_TYPE", "laptop")
                    }
                    context.startActivity(intent)
                }
            )

            // Bottom spacer - larger to push content up
            Spacer(modifier = Modifier.weight(2f))

            // SYNC button at bottom - disabled for now
            Button(
                onClick = { /* Disabled */ },
                enabled = false,
                modifier = Modifier
                    .width(320.dp)
                    .height(56.dp)
                    .border(1.dp, Color(0xFFDDDDDD), RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF333333),
                    disabledContainerColor = Color(0xFFE0E0E0),
                    disabledContentColor = Color(0xFF999999)
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 2.dp,
                    pressedElevation = 4.dp
                )
            ) {
                Text(
                    text = "SYNC",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun HomeButton(
    text: String,
    onClick: () -> Unit
) {
    // Matching PhoneListActivity FilterChip style - white background with border
    Button(
        onClick = onClick,
        modifier = Modifier
            .width(320.dp)
            .height(56.dp)
            .border(1.dp, Color(0xFFDDDDDD), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = Color(0xFF333333)
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