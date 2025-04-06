package com.example.jetpackdemoapp.weather.screen

import DailyForecastResponse
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.Button
import com.example.jetpackdemoapp.R
import com.example.jetpackdemoapp.data.model.model.FutureWeather
import com.example.jetpackdemoapp.data.model.model.HourlyForecastResponse
import com.example.jetpackdemoapp.data.model.model.HourlyWeather
import com.example.jetpackdemoapp.data.model.model.WeatherResponse
import com.example.jetpackdemoapp.data.model.service.RetrofitInstance
import com.example.jetpackdemoapp.weather.components.DetailItem
import com.example.jetpackdemoapp.weather.components.FutureItem
import com.example.jetpackdemoapp.weather.components.HourlyItem
import com.example.jetpackdemoapp.weather.components.WeatherDetailItem
import com.example.jetpackdemoapp.weather.components.dailyItems
import com.example.jetpackdemoapp.weather.getDrawableResourceId
import com.example.jetpackdemoapp.weather.getWeatherIconPath
import com.example.jetpackdemoapp.weather.navigation.BottomNavigation
import com.example.jetpackdemoapp.weather.viewModel.WeatherUiState
import com.example.jetpackdemoapp.weather.viewModel.WeatherViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

@SuppressLint("DefaultLocale", "ServiceCast")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun WeatherScreen(
    latitude: Double,
    longitude: Double,
    isCurrentLocation: Boolean = true,
    timezone: String? = null
) {
    // Get the context
    val context = LocalContext.current

    // Create the repository with context
    val factory = WeatherViewModel.Factory(
        repository = RetrofitInstance.getWeatherRepository(context = context),
        apiKey = "7a85369669e56294a7779bb9f8be8563"
    )

    // Rest of your code remains the same
    var currentScreen by remember { mutableStateOf("location") }

    // Lưu vị trí ban đầu (My Location) tách biệt
    val initialLatitude = remember { latitude }
    val initialLongitude = remember { longitude }

    // Track current location coordinates (in case they change via menu)
    var currentLatitude by remember { mutableStateOf(latitude) }
    var currentLongitude by remember { mutableStateOf(longitude) }
    var isMyLocation by remember { mutableStateOf(isCurrentLocation) }
    var currentTimezone by remember { mutableStateOf(timezone) }

    // Animation states
    val mapAlpha by animateFloatAsState(
        targetValue = if (currentScreen == "map") 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "mapAlpha"
    )

    val locationAlpha by animateFloatAsState(
        targetValue = if (currentScreen == "location") 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "locationAlpha"
    )

    val menuAlpha by animateFloatAsState(
        targetValue = if (currentScreen == "menu") 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "menuAlpha"
    )

    // Navigation visibility animation
    val navBarVisible by animateFloatAsState(
        targetValue = if (currentScreen == "location") 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "navBarVisible"
    )

    val viewModel: WeatherViewModel = viewModel(factory = factory)
    val currentWeatherState by viewModel.currentWeatherState.collectAsState()
    val hourlyForecastState by viewModel.hourlyForecastState.collectAsState()
    val dailyForecastState by viewModel.dailyForecastState.collectAsState()

    // Get the correct local time based on the timezone
    val formattedDateTime = remember(currentTimezone) {
        try {
            val zoneId = when {
                isMyLocation -> ZoneId.systemDefault()
                currentTimezone != null -> ZoneId.of(currentTimezone)
                else -> ZoneId.systemDefault()
            }

            val localDateTime = LocalDateTime.now(zoneId)
            val formatter = DateTimeFormatter.ofPattern("EEE MMM dd | HH:mm a")
            localDateTime.format(formatter)
        } catch (e: Exception) {
            // Fallback to system time if there's an error with timezone
            val formatter = DateTimeFormatter.ofPattern("EEE MMM dd | HH:mm a")
            LocalDateTime.now().format(formatter)
        }
    }

    LaunchedEffect(Unit) {
        // Set up location updates
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (isMyLocation) {
                    currentLatitude = location.latitude
                    currentLongitude = location.longitude

                    viewModel.saveMyLocation(location.latitude, location.longitude)
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        // Request location updates if permission is granted
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                10000, // 10 seconds
                10f,    // 10 meters
                locationListener
            )
        }

        // Lưu vị trí ban đầu là My Location
        viewModel.saveMyLocation(initialLatitude, initialLongitude)
    }

    // Request data when location changes
    LaunchedEffect(currentLatitude, currentLongitude) {
        if (isMyLocation) {
            viewModel.updateMyLocationWeather(currentLatitude, currentLongitude)
        } else {
            viewModel.fetchWeatherDataForSelectedCity(currentLatitude, currentLongitude)

            // Get timezone info for selected city
            viewModel.getCurrentWeatherForBottomSheet(currentLatitude, currentLongitude) { weatherResponse ->
                if (weatherResponse != null && weatherResponse.timezone != null) {
                    val timezoneSeconds = weatherResponse.timezone
                    val offsetHours = timezoneSeconds / 3600
                    val offsetMinutes = (Math.abs(timezoneSeconds) % 3600) / 60
                    val sign = if (offsetHours >= 0) "+" else "-"
                    val formattedHours = String.format("%02d", Math.abs(offsetHours))
                    val formattedMinutes = String.format("%02d", offsetMinutes)
                    val timezoneId = "UTC$sign$formattedHours:$formattedMinutes"
                    currentTimezone = timezoneId
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Map Screen
        AnimatedVisibility(
            visible = mapAlpha > 0,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(300))
        ) {
            MapScreen(
                latitude = currentLatitude,
                longitude = currentLongitude,
                onClose = {
                    currentScreen = "location"
                }
            )
        }

        // Weather/Location Screen
        AnimatedVisibility(
            visible = locationAlpha > 0,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(300))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 70.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(android.graphics.Color.parseColor("#00FFEF")),
                                Color(android.graphics.Color.parseColor("#FF6347"))
                            )
                        )
                    )
            ) {
                when {
                    currentWeatherState is WeatherUiState.Loading -> {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            CircularProgressIndicator(color = Color.White)
                        }
                    }
                    currentWeatherState is WeatherUiState.Error -> {
                        val errorMessage = (currentWeatherState as WeatherUiState.Error).message
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = "Error: $errorMessage",
                                color = Color.White,
                                fontSize = 18.sp
                            )
                        }
                    }
                    currentWeatherState is WeatherUiState.Success -> {
                        val currentWeather = (currentWeatherState as WeatherUiState.Success<WeatherResponse>).data
                        val hourlyForecasts = if (hourlyForecastState is WeatherUiState.Success) {
                            (hourlyForecastState as WeatherUiState.Success<HourlyForecastResponse>).data.list
                        } else emptyList()

                        val dailyForecasts = if (dailyForecastState is WeatherUiState.Success) {
                            val apiData = (dailyForecastState as WeatherUiState.Success<DailyForecastResponse>).data
                            Log.d("WeatherScreen", "Daily forecast data received: ${apiData.list.size} items")
                            processDailyForecast(apiData)
                        } else {
                            Log.d("WeatherScreen", "Using fallback daily forecast data")
                            dailyItems
                        }

                        val minTemp = if (dailyForecastState is WeatherUiState.Success) {
                            val forecasts = (dailyForecastState as WeatherUiState.Success<DailyForecastResponse>).data
                            forecasts.list.filter {
                                val date = LocalDateTime.ofEpochSecond(it.dt, 0, ZoneOffset.UTC)
                                date.toLocalDate() == LocalDateTime.now().toLocalDate()
                            }.minOfOrNull { it.main.temp_min }?.toInt() ?: currentWeather.main.temp_min.toInt()
                        } else {
                            currentWeather.main.temp_min.toInt()
                        }

                        val maxTemp = if (dailyForecastState is WeatherUiState.Success) {
                            val forecasts = (dailyForecastState as WeatherUiState.Success<DailyForecastResponse>).data
                            forecasts.list.filter {
                                val date = LocalDateTime.ofEpochSecond(it.dt, 0, ZoneOffset.UTC)
                                date.toLocalDate() == LocalDateTime.now().toLocalDate()
                            }.maxOfOrNull { it.main.temp_max }?.toInt() ?: currentWeather.main.temp_max.toInt()
                        } else {
                            currentWeather.main.temp_max.toInt()
                        }

                        WeatherContent(
                            currentWeather = currentWeather,
                            hourlyWeatherModels = processHourlyForecast(hourlyForecasts),
                            dailyWeatherModels = dailyForecasts,
                            formattedDateTime = formattedDateTime,
                            minTemp = minTemp,
                            maxTemp = maxTemp,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }

        // Menu Screen
        AnimatedVisibility(
            visible = menuAlpha > 0,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(300))
        ) {
            MenuScreen(
                // Use MyLocation coordinates
                viewModel = viewModel,
                onLocationSelect = { lat, lon, isCurrentLocation, locationTimezone ->
                    if (isCurrentLocation) {
                        // Nếu chọn My Location, sử dụng tọa độ My Location hiện tại
                        val myLocation = viewModel.getMyLocation()
                        currentLatitude = myLocation.first
                        currentLongitude = myLocation.second
                        isMyLocation = true
                        currentTimezone = null
                    } else {
                        // Nếu chọn thành phố khác
                        currentLatitude = lat
                        currentLongitude = lon
                        isMyLocation = false
                        currentTimezone = locationTimezone
                    }
                    currentScreen = "location"
                }
            )
        }
        // Animated Bottom Navigation
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = navBarVisible
                    translationY = (1f - navBarVisible) * 70f
                }
        ) {
            BottomNavigation(
                currentScreen = currentScreen,
                onNavigate = { screen ->
                    currentScreen = screen
                }
            )
        }
    }
}

@SuppressLint("DefaultLocale")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun WeatherContent(
    currentWeather: WeatherResponse,
    hourlyWeatherModels: List<HourlyWeather>,
    dailyWeatherModels: List<FutureWeather>,
    formattedDateTime: String,
    minTemp: Int,
    maxTemp: Int,
    viewModel: WeatherViewModel
) {
    // Extract data from currentWeather
    val weatherDescription = currentWeather.weather.firstOrNull()?.description?.capitalize(Locale.getDefault()) ?: "Unknown"
    val windSpeed = currentWeather.wind.speed
    val humidity = currentWeather.main.humidity
    val rainVolume = currentWeather.rain?.the1h ?: 0.0
    val cityName = currentWeather.name ?: "Unknown Location"

    // Get temperature unit preference
    val temperatureUnit by viewModel.temperatureUnit.collectAsState()
    val unitSymbol = if (temperatureUnit == WeatherViewModel.TemperatureUnit.CELSIUS) "°C" else "°F"

    // Convert temperatures to the selected unit
    val convertedTemp = viewModel.convertToCurrentUnit(currentWeather.main.temp)
    val convertedMinTemp = viewModel.convertToCurrentUnit(minTemp.toDouble()).toInt()
    val convertedMaxTemp = viewModel.convertToCurrentUnit(maxTemp.toDouble()).toInt()
    val convertedFeelsLike = viewModel.convertToCurrentUnit(currentWeather.main.feels_like)

    // Calculate derived values with proper units
    val dewPointValue = calculateDewPoint(currentWeather.main.temp, humidity)
    val convertedDewPoint = viewModel.convertToCurrentUnit(dewPointValue)
    val dewPoint = String.format("%.1f%s", convertedDewPoint, unitSymbol)
    val pressure = "${currentWeather.main.pressure} hPa"
    val visibility = if (currentWeather.visibility != null) {
        "${currentWeather.visibility / 1000} km"
    } else "N/A"
    val feelsLike = String.format("%.1f%s", convertedFeelsLike, unitSymbol)

    // UV Index (simulated based on weather conditions)
    val uvIndex = when {
        weatherDescription.contains("clear", ignoreCase = true) -> "7"
        weatherDescription.contains("cloud", ignoreCase = true) -> "3"
        weatherDescription.contains("rain", ignoreCase = true) -> "1"
        else -> "4"
    }

    // Calculate sunset time from Unix timestamp
    val sunset = if (currentWeather.sys != null && currentWeather.sys.sunset != null) {
        LocalDateTime.ofEpochSecond(
            currentWeather.sys.sunset.toLong(),
            0,
            ZoneOffset.UTC
        ).format(DateTimeFormatter.ofPattern("HH:mm"))
    } else "N/A"

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                // Main Weather Condition
                Text(
                    text = weatherDescription.uppercase(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 48.dp),
                    textAlign = TextAlign.Center
                )

                Image(
                    painter = painterResource(
                        id = getDrawableResourceId(getWeatherIconPath(weatherDescription))
                    ),
                    contentDescription = weatherDescription,
                    modifier = Modifier
                        .size(150.dp)
                        .padding(top = 8.dp)
                )

                // Date and Time
                Text(
                    text = formattedDateTime,
                    fontSize = 19.sp,
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 8.dp),
                    textAlign = TextAlign.Center
                )

                // Location
                Text(
                    text = cityName,
                    fontSize = 30.sp,
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 8.dp),
                    textAlign = TextAlign.Center
                )

                // Temperature - updated to use converted temperature
                Text(
                    text = String.format("%.1f%s", convertedTemp, unitSymbol),
                    fontSize = 50.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 8.dp),
                    textAlign = TextAlign.Center
                )

                // Highest and Lowest Temperature - updated to use converted temperatures
                Text(
                    text = "H:${convertedMaxTemp}${unitSymbol} | L:${convertedMinTemp}${unitSymbol}",
                    fontSize = 20.sp,
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 8.dp),
                    textAlign = TextAlign.Center
                )

                // Weather summary box (rain, wind, humidity)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .background(
                            color = Color(android.graphics.Color.parseColor("#3287a8")),
                            shape = RoundedCornerShape(25.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        WeatherDetailItem(icon = R.drawable.rain, value = "${rainVolume}mm", label = "Rain")
                        WeatherDetailItem(icon = R.drawable.wind, value = "${windSpeed}m/s", label = "Wind Speed")
                        WeatherDetailItem(icon = R.drawable.humidity, value = "${humidity}%", label = "Humidity")
                    }
                }

                // Today's Details
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .background(
                            color = Color(android.graphics.Color.parseColor("#3287a8")),
                            shape = RoundedCornerShape(25.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Today's Details",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            DetailItem(title = "Feels Like", value = feelsLike, icon = R.drawable.feels_like)
                            DetailItem(title = "Dew Point", value = dewPoint, icon = R.drawable.dew_point)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            DetailItem(title = "UV Index", value = uvIndex, icon = R.drawable.uv)
                            DetailItem(title = "Sunset", value = sunset, icon = R.drawable.sunset)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            DetailItem(title = "Visibility", value = visibility, icon = R.drawable.visibility)
                            DetailItem(title = "Pressure", value = pressure, icon = R.drawable.pressure)
                        }
                    }
                }

                // Today's forecast label
                Text(
                    text = "Weather Forecast for Today",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                )
            }

            // Future hourly weather forecast
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(hourlyWeatherModels) { hourlyWeather ->
                        HourlyItem(hourlyWeather)
                    }
                }
            }

            // 7-day forecast header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Future",
                        fontSize = 20.sp,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Next 5 days >>",
                        fontSize = 14.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 7-day forecast items
            items(dailyWeatherModels) { item ->
                FutureItem(item)
            }
        }
    }
}

// Helper functions

/**
 * Calculates the dew point based on temperature and humidity
 * @param tempC temperature in Celsius
 * @param humidity relative humidity percentage
 * @return dew point temperature in Celsius
 */
private fun calculateDewPoint(tempC: Double, humidity: Int): Double {
    return tempC - ((100 - humidity) / 5.0)
}

/**
 * Processes the hourly forecast data from API response
 * @param hourlyList list of hourly forecast items from API
 * @return list of simplified HourlyWeather objects for UI display
 */
// Update this function to match the correct structure of your API response
@RequiresApi(Build.VERSION_CODES.O)
private fun processHourlyForecast(hourlyList: List<HourlyForecastResponse.ListItem>): List<HourlyWeather> {
    // Get current time in seconds
    val currentTimeSeconds = System.currentTimeMillis() / 1000

    // Log current time for debugging
    Log.d("WeatherScreen", "Current time: ${LocalDateTime.ofEpochSecond(currentTimeSeconds, 0, ZoneOffset.UTC)}")

    // 1. First sort all forecasts by time
    val sortedForecasts = hourlyList.sortedBy { it.dt }

    // 2. Find the forecast closest to current time (could be slightly in past or future)
    val closestForecastIndex = sortedForecasts.indexOfFirst { it.dt > currentTimeSeconds }
        .takeIf { it >= 0 } ?: 0

    // 3. Take 5 forecasts starting from the closest one
    val selectedForecasts = if (closestForecastIndex + 5 <= sortedForecasts.size) {
        sortedForecasts.subList(closestForecastIndex, closestForecastIndex + 5)
    } else {
        sortedForecasts.subList(closestForecastIndex, sortedForecasts.size)
    }

    // Log selected forecasts
    selectedForecasts.forEach { forecast ->
        val forecastTime = LocalDateTime.ofEpochSecond(forecast.dt, 0, ZoneOffset.UTC)
        Log.d("WeatherScreen", "Selected forecast time: $forecastTime")
    }

    return selectedForecasts.map { hourly ->
        HourlyWeather(
            dt = hourly.dt,
            temp = hourly.main.temp,
            weather = hourly.weather,
            picPath = getWeatherIconPath(hourly.weather[0].description)
        )
    }
}
/**
 * Processes the daily forecast data from API response
 * @param dailyForecast daily forecast response from API
 * @return list of simplified FutureWeather objects for UI display
 */
@RequiresApi(Build.VERSION_CODES.O)
private fun processDailyForecast(dailyForecast: DailyForecastResponse): List<FutureWeather> {
    // Get today's date
    val today = LocalDate.now()

    Log.d("WeatherScreen", "Raw forecast data received with ${dailyForecast.list.size} items")

    // Group the forecast items by day
    val groupedByDay = dailyForecast.list.groupBy { item ->
        LocalDateTime.ofEpochSecond(item.dt.toLong(), 0, ZoneOffset.UTC).toLocalDate()
    }.toSortedMap() // Sort by date

    Log.d("WeatherScreen", "Grouped into ${groupedByDay.size} unique days")

    // Extract at most 6 days (today + next 5 days) instead of 7
    val next6Days = groupedByDay.keys.filter { it >= today }.take(6)

    // Create a FutureWeather object for each day
    return next6Days.map { date ->
        val items = groupedByDay[date] ?: emptyList()

        // Get min and max temperatures for the day
        val minTemp = items.minOfOrNull { it.main.temp_min }?.toInt() ?: 0
        val maxTemp = items.maxOfOrNull { it.main.temp_max }?.toInt() ?: 0

        // Get the most common weather condition
        val weatherCounts = items.groupBy { it.weather.firstOrNull()?.description ?: "Unknown" }
        val mostCommonWeather = weatherCounts.maxByOrNull { it.value.size }?.key ?: "Unknown"

        // Format day name (Today, Mon, Tue, etc.)
        val dayName = when {
            date.isEqual(today) -> "Today"
            else -> date.format(DateTimeFormatter.ofPattern("EEE"))
        }

        Log.d("WeatherScreen", "Created forecast for $dayName: $mostCommonWeather, min=$minTemp, max=$maxTemp")

        // Create the model object
        FutureWeather(
            day = dayName,
            picPath = getWeatherIconPath(mostCommonWeather),
            status = mostCommonWeather.capitalize(Locale.getDefault()),
            highTemp = maxTemp,
            lowTemp = minTemp
        )
    }.let { result ->
        // If we have fewer than 6 days, add placeholders to fill the gap
        if (result.size < 6) {
            Log.d("WeatherScreen", "Adding ${6 - result.size} placeholder days")
            val placeholders = (result.size until 6).map { offset ->
                val futureDate = today.plusDays(offset.toLong())
                val dayName = futureDate.format(DateTimeFormatter.ofPattern("EEE"))
                FutureWeather(
                    day = dayName,
                    picPath = "cloudy",
                    status = "No data",
                    highTemp = 0,
                    lowTemp = 0
                )
            }
            result + placeholders
        } else {
            result
        }
    }
}
/**
 * This function maps the weather description to the appropriate weather icon.
 * Used when we need to get weather icons for different time periods.
 *
 * @param weatherDescription The description of the weather condition
 * @return The icon code used by OpenWeatherMap
 */
private fun getWeatherIconFromDescription(weatherDescription: String): String {
    return when {
        weatherDescription.contains("clear", ignoreCase = true) -> "01d"
        weatherDescription.contains("few clouds", ignoreCase = true) -> "02d"
        weatherDescription.contains("scattered clouds", ignoreCase = true) -> "03d"
        weatherDescription.contains("broken clouds", ignoreCase = true) ||
                weatherDescription.contains("overcast", ignoreCase = true) -> "04d"
        weatherDescription.contains("shower rain", ignoreCase = true) -> "09d"
        weatherDescription.contains("rain", ignoreCase = true) -> "10d"
        weatherDescription.contains("thunderstorm", ignoreCase = true) -> "11d"
        weatherDescription.contains("snow", ignoreCase = true) -> "13d"
        weatherDescription.contains("mist", ignoreCase = true) ||
                weatherDescription.contains("fog", ignoreCase = true) ||
                weatherDescription.contains("haze", ignoreCase = true) -> "50d"
        else -> "01d" // Default icon
    }
}

/**
 * Draws a CircularProgressIndicator for loading state.
 */
@Composable
private fun CircularProgressIndicator(
    color: Color,
    modifier: Modifier = Modifier.size(40.dp)
) {
    androidx.compose.material3.CircularProgressIndicator(
        color = color,
        modifier = modifier
    )
}

/**
 * A helper function to get time of day (morning, afternoon, evening, night)
 * based on the current hour.
 */
@RequiresApi(Build.VERSION_CODES.O)
private fun getTimeOfDay(): String {
    val hour = LocalDateTime.now().hour
    return when {
        hour < 6 -> "night"
        hour < 12 -> "morning"
        hour < 18 -> "afternoon"
        else -> "evening"
    }
}