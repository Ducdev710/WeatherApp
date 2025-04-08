package com.example.jetpackdemoapp.weather.components

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jetpackdemoapp.data.model.model.HourlyWeather
import com.example.jetpackdemoapp.weather.getDrawableResourceId
import com.example.jetpackdemoapp.weather.viewModel.WeatherViewModel
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@SuppressLint("DefaultLocale")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun FutureModelViewHolder(model: HourlyWeather, viewModel: WeatherViewModel) {
    // Convert timestamp to human-readable hour
    val hour = LocalDateTime.ofEpochSecond(model.dt, 0, ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("ha"))

    // Convert temperature to current unit and format with one decimal place
    val convertedTemp = viewModel.convertToCurrentUnit(model.temp)
    val formattedTemp = String.format("%.1f", convertedTemp)

    Column(
        modifier = Modifier
            .width(90.dp)
            .wrapContentHeight()
            .padding(4.dp)
            .background(
                color = Color(android.graphics.Color.parseColor("#3287a8")),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = hour,
            fontSize = 16.sp,
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            textAlign = TextAlign.Center
        )
        Image(
            painter = painterResource(id = getDrawableResourceId(model.picPath)),
            contentDescription = null,
            modifier = Modifier
                .size(45.dp)
                .padding(8.dp),
            contentScale = ContentScale.Crop
        )
        Text(
            text = "${formattedTemp}°",
            fontSize = 16.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            textAlign = TextAlign.Center
        )
    }
}

@SuppressLint("DefaultLocale")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HourlyItem(model: HourlyWeather, viewModel: WeatherViewModel) {
    // Simply delegate to your existing FutureModelViewHolder
    FutureModelViewHolder(model, viewModel)
}