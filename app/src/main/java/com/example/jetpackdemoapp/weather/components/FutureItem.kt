package com.example.jetpackdemoapp.weather.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jetpackdemoapp.data.model.model.FutureWeather
import com.example.jetpackdemoapp.weather.getDrawableResourceId
import com.example.jetpackdemoapp.weather.viewModel.WeatherViewModel

@Composable
fun FutureItem(item: FutureWeather, viewModel: WeatherViewModel) {
    // Get temperature unit
    val temperatureUnit by viewModel.temperatureUnit.collectAsState()
    val unitSymbol = if (temperatureUnit == WeatherViewModel.TemperatureUnit.CELSIUS) "°C" else "°F"

    // Convert temperatures
    val convertedHighTemp = viewModel.convertToCurrentUnit(item.highTemp.toDouble()).toInt()
    val convertedLowTemp = viewModel.convertToCurrentUnit(item.lowTemp.toDouble()).toInt()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Day column - fixed width
        Text(
            text = item.day,
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier.width(50.dp)
        )

        // Weather icon column - fixed position and size
        Box(
            modifier = Modifier.width(60.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = getDrawableResourceId(picPath = item.picPath)),
                contentDescription = null,
                modifier = Modifier.size(40.dp)
            )
        }

        // Status column - fixed width with ellipsis if too long
        Text(
            text = item.status,
            color = Color.White,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        // High temperature - with right alignment and converted unit
        Text(
            text = "$convertedHighTemp°",
            color = Color.White,
            fontSize = 14.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.width(35.dp)
        )

        // Space between temperatures
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))

        // Low temperature - with right alignment and converted unit
        Text(
            text = "$convertedLowTemp°",
            color = Color.White,
            fontSize = 14.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.width(35.dp)
        )
    }
}

val dailyItems = listOf(
    FutureWeather(day = "Sun", picPath = "cloudy", status = "Cloudy", highTemp = 25, lowTemp = 16),
    FutureWeather(day = "Mon", picPath = "windy", status = "Windy", highTemp = 29, lowTemp = 15),
    FutureWeather(day = "Tue", picPath = "cloudy_sunny", status = "Cloudy Sunny", highTemp = 23, lowTemp = 13),
    FutureWeather(day = "Wed", picPath = "sunny", status = "Sunny", highTemp = 28, lowTemp = 11),
    FutureWeather(day = "Thu", picPath = "rainy", status = "Rainy", highTemp = 23, lowTemp = 12),
    FutureWeather(day = "Fri", picPath = "cloudy_sunny", status = "Cloudy Sunny", highTemp = 26, lowTemp = 14),
)