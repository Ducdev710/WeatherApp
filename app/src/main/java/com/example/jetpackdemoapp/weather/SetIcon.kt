package com.example.jetpackdemoapp.weather

import androidx.compose.runtime.Composable
import com.example.jetpackdemoapp.R

fun getWeatherIconPath(description: String): String {
    return when {
        description.contains("rain") -> "rainy"
        description.contains("cloud") -> "cloudy"
        description.contains("clear") -> "sunny"
        description.contains("wind") -> "windy"
        description.contains("storm") -> "storm"
        description.contains("snow") -> "cloudy"
        else -> "cloudy_sunny"
    }
}

@Composable
fun getDrawableResourceId(picPath: String): Int {
    return when (picPath) {
        "storm" -> R.drawable.storm
        "cloudy" -> R.drawable.cloudy
        "windy" -> R.drawable.windy
        "cloudy_sunny" -> R.drawable.cloudy_sunny
        "sunny" -> R.drawable.sunny
        "rainy" -> R.drawable.rainy
        else -> R.drawable.sunny
    }
}