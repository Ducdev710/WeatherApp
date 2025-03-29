package com.example.jetpackdemoapp.weather.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jetpackdemoapp.R

@Composable
fun BottomNavigation(
    currentScreen: String = "location",
    onNavigate: (String) -> Unit = {}
) {
    // Set selected index based on current screen
    val selectedIndex = when (currentScreen) {
        "map" -> 0
        "location" -> 1
        "menu" -> 2
        else -> 1 // Default to location
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF3287a8))
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Map Icon
            NavigationItem(
                icon = R.drawable.map,
                label = "Map",
                isSelected = selectedIndex == 0,
                onClick = {
                    onNavigate("map")
                }
            )

            // Location Icon
            NavigationItem(
                icon = R.drawable.location,
                label = "Location",
                isSelected = selectedIndex == 1,
                onClick = {
                    onNavigate("location")
                }
            )

            // Menu Icon
            NavigationItem(
                icon = R.drawable.baseline_menu_24,
                label = "Menu",
                isSelected = selectedIndex == 2,
                onClick = {
                    onNavigate("menu")
                }
            )
        }
    }
}

@Composable
private fun NavigationItem(
    icon: Int,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = label,
            tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
            modifier = Modifier.size(24.dp)
        )

        // Only show label when this item is selected
        if (isSelected) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color.White
            )
        }
    }
}