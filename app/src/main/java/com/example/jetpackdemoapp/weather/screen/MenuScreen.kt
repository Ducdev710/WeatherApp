package com.example.jetpackdemoapp.weather.screen

import DailyForecastResponse
import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.example.jetpackdemoapp.R
import com.example.jetpackdemoapp.data.model.model.WeatherResponse
import com.example.jetpackdemoapp.data.model.utils.LocationPreferences
import com.example.jetpackdemoapp.weather.viewModel.WeatherViewModel
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.RadioButton

@SuppressLint("DefaultLocale")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MenuScreen(
    viewModel: WeatherViewModel,
    onLocationSelect: (Double, Double, Boolean, String?) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    val locationPreferences = remember { LocationPreferences(context) }

    val temperatureUnit by viewModel.temperatureUnit.collectAsState()
    var showMenu by remember { mutableStateOf(false) }

    // Store weather data for each location
    val locationWeatherData = remember { mutableStateMapOf<String, LocationWeatherData?>() }

    // Get My Location coordinates from ViewModel instead of parameters
    val myLocationCoordinates = viewModel.getMyLocation()

    // Track saved locations - load from preferences
    val savedLocations = remember {
        val savedFromPrefs = locationPreferences.getSavedLocations().toMutableStateList()
        // Always add current location at the beginning if it doesn't exist
        if (savedFromPrefs.none { it.isCurrentLocation }) {
            savedFromPrefs.add(
                0, SavedLocation("My Location", myLocationCoordinates.first, myLocationCoordinates.second, true)
            )
        }
        savedFromPrefs
    }

    // Save locations when they change (except the current location)
    LaunchedEffect(savedLocations.size) {
        if (savedLocations.size > 1) {
            locationPreferences.saveLocations(savedLocations)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.initTemperatureUnit()
    }

    // Get My Location weather data directly from ViewModel
    val myLocationWeather by viewModel.myLocationWeather.collectAsState()

    // Update My Location data when available
    LaunchedEffect(myLocationWeather) {
        myLocationWeather?.let { weather ->
            // Get the daily forecast for My Location from ViewModel
            val myLocationForecast = viewModel.getMyLocationDailyForecast()

            // Calculate min/max temps from forecast if available
            val minTemp = myLocationForecast?.list?.let { list ->
                val todayForecasts = list.filter {
                    val date = LocalDateTime.ofEpochSecond(it.dt, 0, ZoneOffset.UTC)
                    date.toLocalDate() == LocalDateTime.now().toLocalDate()
                }
                todayForecasts.minOfOrNull { it.main.temp_min }?.toInt()
            } ?: weather.main.temp_min.toInt()

            val maxTemp = myLocationForecast?.list?.let { list ->
                val todayForecasts = list.filter {
                    val date = LocalDateTime.ofEpochSecond(it.dt, 0, ZoneOffset.UTC)
                    date.toLocalDate() == LocalDateTime.now().toLocalDate()
                }
                todayForecasts.maxOfOrNull { it.main.temp_max }?.toInt()
            } ?: weather.main.temp_max.toInt()

            // Update My Location weather data
            locationWeatherData["My Location"] = LocationWeatherData(
                currentTemp = weather.main.temp.toInt(),
                highTemp = maxTemp,
                lowTemp = minTemp
            )
        }
    }

    // Fetch weather for saved locations
    LaunchedEffect(savedLocations) {
        savedLocations.forEach { location ->
            // Skip current location as it's handled separately
            if (!location.isCurrentLocation && !locationWeatherData.containsKey(location.name)) {
                // Start with null weather data to show loading state
                locationWeatherData[location.name] = null

                // Get current weather for this location
                viewModel.getCurrentWeatherForBottomSheet(location.latitude, location.longitude) { currentWeather ->
                    if (currentWeather != null) {
                        // Extract timezone information from the API response
                        val timezoneSeconds = currentWeather.timezone ?: 0
                        // Convert timezone offset from seconds to ZoneId format
                        val offsetHours = timezoneSeconds / 3600
                        val offsetMinutes = (Math.abs(timezoneSeconds) % 3600) / 60
                        val sign = if (offsetHours >= 0) "+" else "-"
                        val formattedHours = String.format("%02d", Math.abs(offsetHours))
                        val formattedMinutes = String.format("%02d", offsetMinutes)
                        val timezoneId = "UTC$sign$formattedHours:$formattedMinutes"

                        // Update the location with timezone info
                        location.timezone = timezoneId

                        // Get forecast for accurate min/max temps
                        viewModel.getDailyForecastForBottomSheet(location.latitude, location.longitude) { forecast ->
                            if (forecast != null) {
                                // Calculate min/max temps
                                val todayForecasts = forecast.list.filter {
                                    val date = LocalDateTime.ofEpochSecond(it.dt, 0, ZoneOffset.UTC)
                                    date.toLocalDate() == LocalDateTime.now().toLocalDate()
                                }

                                val minTemp = todayForecasts.minOfOrNull { it.main.temp_min }?.toInt()
                                    ?: currentWeather.main.temp_min.toInt()
                                val maxTemp = todayForecasts.maxOfOrNull { it.main.temp_max }?.toInt()
                                    ?: currentWeather.main.temp_max.toInt()

                                // Update weather data for this location
                                locationWeatherData[location.name] = LocationWeatherData(
                                    currentTemp = currentWeather.main.temp.toInt(),
                                    highTemp = maxTemp,
                                    lowTemp = minTemp
                                )
                            } else {
                                // Fallback to current weather values
                                locationWeatherData[location.name] = LocationWeatherData(
                                    currentTemp = currentWeather.main.temp.toInt(),
                                    highTemp = currentWeather.main.temp_max.toInt(),
                                    lowTemp = currentWeather.main.temp_min.toInt()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1A3A4A),  // Darker blue at top
                            Color(0xFF2D6F9E)   // Lighter blue at bottom
                        )
                    )
                )
                .padding(16.dp)
        ) {
            // Title row with 3-dots menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Weather",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                // Three dots menu button
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Settings",
                        tint = Color.White
                    )
                }
            }

            // Dropdown menu
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier
                    .background(Color(0xFF2a7599))
            ) {
                Text(
                    text = "Temperature Unit",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                Divider(color = Color.White.copy(alpha = 0.3f))

                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = temperatureUnit == WeatherViewModel.TemperatureUnit.CELSIUS,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Celsius (°C)", color = Color.White)
                        }
                    },
                    onClick = {
                        viewModel.setTemperatureUnit(WeatherViewModel.TemperatureUnit.CELSIUS)
                        showMenu = false
                    }
                )

                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = temperatureUnit == WeatherViewModel.TemperatureUnit.FAHRENHEIT,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Fahrenheit (°F)", color = Color.White)
                        }
                    },
                    onClick = {
                        viewModel.setTemperatureUnit(WeatherViewModel.TemperatureUnit.FAHRENHEIT)
                        showMenu = false
                    }
                )
            }

            // Search bar with suggestion dropdown
            LocationSearchBar(
                searchText = searchQuery,
                onSearchTextChanged = {
                    searchQuery = it
                    isSearching = it.isNotEmpty()
                },
                onLocationSelected = { name, lat, lon ->
                    // Add to saved locations if not already saved
                    if (savedLocations.none { it.latitude == lat && it.longitude == lon }) {
                        savedLocations.add(SavedLocation(name, lat, lon))
                    }
                    // Navigate to weather for selected location - not a current location
                    onLocationSelect(lat, lon, false, null)
                    searchQuery = ""
                    isSearching = false
                },
                viewModel = viewModel
            )

            // Display saved locations when not searching
            if (!isSearching) {
                Text(
                    text = "Saved Locations",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                LazyColumn {
                    items(savedLocations) { location ->
                        if (location.isCurrentLocation) {
                            // Regular item for My Location (no swipe to delete)
                            LocationItem(
                                location = location,
                                weatherData = locationWeatherData[location.name],
                                onClick = {
                                    onLocationSelect(
                                        location.latitude,
                                        location.longitude,
                                        location.isCurrentLocation,
                                        location.timezone
                                    )
                                },
                                viewModel = viewModel
                            )
                        } else {
                            // Swipe to delete for saved cities
                            SwipeToDeleteLocationItem(
                                location = location,
                                weatherData = locationWeatherData[location.name],
                                onClick = {
                                    onLocationSelect(
                                        location.latitude,
                                        location.longitude,
                                        location.isCurrentLocation,
                                        location.timezone
                                    )
                                },
                                onDelete = {
                                    // Find the index of this location in the list and remove it properly
                                    val index = savedLocations.indexOfFirst {
                                        it.name == location.name &&
                                                it.latitude == location.latitude &&
                                                it.longitude == location.longitude
                                    }
                                    if (index >= 0) {
                                        savedLocations.removeAt(index)
                                        // Update preferences immediately to persist the change
                                        locationPreferences.saveLocations(savedLocations)
                                    }
                                    // Remove from weather data map
                                    locationWeatherData.remove(location.name)
                                    // Notify user
                                    Toast.makeText(context, "${location.name} removed", Toast.LENGTH_SHORT).show()
                                },
                                viewModel = viewModel
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun LocationItem(
    location: SavedLocation,
    weatherData: LocationWeatherData?,
    onClick: () -> Unit,
    viewModel: WeatherViewModel
) {
    // Get temperature unit
    val temperatureUnit by viewModel.temperatureUnit.collectAsState()

    // Get current time in the location's timezone
    val currentTime = remember(location.timezone) {
        try {
            val zoneId = when {
                location.isCurrentLocation -> ZoneId.systemDefault()
                location.timezone != null -> ZoneId.of(location.timezone)
                else -> ZoneId.systemDefault() // Fallback
            }

            val formatter = DateTimeFormatter.ofPattern("HH:mm")
            LocalDateTime.now(zoneId).format(formatter)
        } catch (e: Exception) {
            // In case of any timezone parsing errors
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2a7599)
        ),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Location info (name and time)
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = location.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = currentTime,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }

            // Temperature info with conversion
            if (weatherData != null) {
                Column(horizontalAlignment = Alignment.End) {
                    // Convert temperatures according to selected unit
                    val convertedCurrentTemp = viewModel.convertToCurrentUnit(weatherData.currentTemp.toDouble()).toInt()
                    val convertedHighTemp = viewModel.convertToCurrentUnit(weatherData.highTemp.toDouble()).toInt()
                    val convertedLowTemp = viewModel.convertToCurrentUnit(weatherData.lowTemp.toDouble()).toInt()

                    Text(
                        text = "${convertedCurrentTemp}°",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Text(
                        text = "H:${convertedHighTemp}° L:${convertedLowTemp}°",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            } else {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun LocationSearchBar(
    searchText: String,
    onSearchTextChanged: (String) -> Unit,
    onLocationSelected: (String, Double, Double) -> Unit,
    viewModel: WeatherViewModel
) {
    val cities = remember { mutableStateOf<List<GeocodingResult>>(emptyList()) }
    val showSuggestions = remember { mutableStateOf(false) }
    val isLoading = remember { mutableStateOf(false) }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    // State for bottom sheet and selected city
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedCity by remember { mutableStateOf<GeocodingResult?>(null) }

    // Fetch city suggestions
    LaunchedEffect(searchText) {
        Log.d("LocationSearchBar", "Search text changed: '$searchText'")
        if (searchText.length >= 2) {
            isLoading.value = true
            delay(300)

            try {
                val suggestions = viewModel.getCitySuggestions(searchText)
                cities.value = suggestions
                showSuggestions.value = suggestions.isNotEmpty()
            } catch (e: Exception) {
                Log.e("LocationSearchBar", "Error fetching suggestions", e)
                Toast.makeText(
                    context,
                    "Error fetching suggestions: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                isLoading.value = false
            }
        } else {
            cities.value = emptyList()
            showSuggestions.value = false
        }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        // Search field
        OutlinedTextField(
            value = searchText,
            onValueChange = onSearchTextChanged,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search for a city", color = Color.White.copy(alpha = 0.7f)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color.White
                )
            },
            trailingIcon = {
                if (isLoading.value) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White
                    )
                } else if (searchText.isNotEmpty()) {
                    IconButton(onClick = { onSearchTextChanged("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = Color.White
                        )
                    }
                }
            },
            colors = TextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color(0xFF2a7599),
                unfocusedContainerColor = Color(0xFF2a7599),
                cursorColor = Color.White,
                focusedIndicatorColor = Color.White,
                unfocusedIndicatorColor = Color.White.copy(alpha = 0.7f)
            ),
            shape = RoundedCornerShape(8.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
        )

        // City suggestions dropdown
        AnimatedVisibility(
            visible = showSuggestions.value && cities.value.isNotEmpty(),
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 60.dp)
                    .zIndex(100f),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2a7599))
            ) {
                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    items(cities.value) { city ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedCity = city
                                    showBottomSheet = true
                                    onSearchTextChanged("")
                                    focusManager.clearFocus()
                                }
                                .padding(16.dp)
                        ) {
                            Text(
                                text = city.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Text(
                                text = city.state?.let { "${it}, ${city.country}" } ?: city.country,
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                        Divider(color = Color.White.copy(alpha = 0.3f), thickness = 0.5.dp)
                    }
                }
            }
        }
    }

    // Weather bottom sheet dialog
    if (showBottomSheet && selectedCity != null) {
        WeatherBottomSheet(
            city = selectedCity!!,
            viewModel = viewModel,
            onDismiss = { showBottomSheet = false },
            onSelectLocation = {
                onLocationSelected(selectedCity!!.name, selectedCity!!.lat, selectedCity!!.lon)
                showBottomSheet = false
            }
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherBottomSheet(
    city: GeocodingResult,
    viewModel: WeatherViewModel,
    onDismiss: () -> Unit,
    onSelectLocation: () -> Unit
) {
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val weatherState = remember { mutableStateOf<WeatherResponse?>(null) }
    val dailyForecastState = remember { mutableStateOf<DailyForecastResponse?>(null) }
    val isLoading = remember { mutableStateOf(true) }

    // Get temperature unit from viewModel
    val temperatureUnit by viewModel.temperatureUnit.collectAsState()
    val unitSuffix = if (temperatureUnit == WeatherViewModel.TemperatureUnit.CELSIUS) "°C" else "°F"

    // Fetch weather for selected city
    LaunchedEffect(city) {
        isLoading.value = true

        // Get current weather
        viewModel.getCurrentWeatherForBottomSheet(city.lat, city.lon) { response ->
            weatherState.value = response

            // Get forecast for accurate min/max temps
            viewModel.getDailyForecastForBottomSheet(city.lat, city.lon) { forecast ->
                dailyForecastState.value = forecast
                isLoading.value = false
            }
        }
    }

    // Calculate min and max temps from forecast
    val (minTemp, maxTemp) = remember(dailyForecastState.value, weatherState.value, temperatureUnit) {
        val forecast = dailyForecastState.value
        val weather = weatherState.value

        if (forecast != null) {
            // Filter forecasts for today
            val todayForecasts = forecast.list.filter {
                val date = LocalDateTime.ofEpochSecond(it.dt, 0, ZoneOffset.UTC)
                date.toLocalDate() == LocalDateTime.now().toLocalDate()
            }

            val min = todayForecasts.minOfOrNull { it.main.temp_min }?.let {
                viewModel.convertToCurrentUnit(it.toDouble()).toInt()
            } ?: weather?.main?.temp_min?.let {
                viewModel.convertToCurrentUnit(it.toDouble()).toInt()
            } ?: 0

            val max = todayForecasts.maxOfOrNull { it.main.temp_max }?.let {
                viewModel.convertToCurrentUnit(it.toDouble()).toInt()
            } ?: weather?.main?.temp_max?.let {
                viewModel.convertToCurrentUnit(it.toDouble()).toInt()
            } ?: 0

            Pair(min, max)
        } else if (weather != null) {
            Pair(
                viewModel.convertToCurrentUnit(weather.main.temp_min.toDouble()).toInt(),
                viewModel.convertToCurrentUnit(weather.main.temp_max.toDouble()).toInt()
            )
        } else {
            Pair(0, 0)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = bottomSheetState,
        containerColor = Color(0xFF1A3A4A),
        modifier = Modifier.fillMaxHeight(0.75f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Cancel and Save buttons at the top
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Cancel",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier
                        .clickable { onDismiss() }
                        .padding(4.dp)
                )

                Text(
                    text = "Save",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50),  // Green color for save button
                    modifier = Modifier
                        .clickable { onSelectLocation() }
                        .padding(4.dp)
                )
            }

            // City name and country header
            Text(
                text = city.name,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = city.state?.let { "${it}, ${city.country}" } ?: city.country,
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (isLoading.value) {
                // Loading indicator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else {
                weatherState.value?.let { weather ->
                    // Convert temperatures according to selected unit
                    val currentTemp = viewModel.convertToCurrentUnit(weather.main.temp.toDouble()).toInt()
                    val feelsLikeTemp = viewModel.convertToCurrentUnit(weather.main.feels_like.toDouble()).toInt()

                    // Current temperature and conditions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "$currentTemp$unitSuffix",
                                fontSize = 64.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Text(
                                text = weather.weather.firstOrNull()?.description?.capitalize() ?: "",
                                fontSize = 18.sp,
                                color = Color.White,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Text(
                                text = "Feels like $feelsLikeTemp$unitSuffix",
                                fontSize = 16.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }

                        // Weather icon
                        val iconCode = weather.weather.firstOrNull()?.icon ?: "01d"
                        AsyncImage(
                            model = "https://openweathermap.org/img/wn/$iconCode@2x.png",
                            contentDescription = "Weather icon",
                            modifier = Modifier.size(100.dp)
                        )
                    }

                    Divider(
                        color = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.padding(vertical = 16.dp)
                    )

                    // Additional weather details
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        WeatherDetail(
                            icon = R.drawable.humidity,
                            value = "${weather.main.humidity}%",
                            label = "Humidity"
                        )

                        WeatherDetail(
                            icon = R.drawable.wind,
                            value = "${weather.wind.speed} m/s",
                            label = "Wind"
                        )

                        WeatherDetail(
                            icon = R.drawable.pressure,
                            value = "${weather.main.pressure} hPa",
                            label = "Pressure"
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // High and low temperatures using calculated values
                    Text(
                        text = "High: $maxTemp$unitSuffix  Low: $minTemp$unitSuffix",
                        fontSize = 16.sp,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                } ?: run {
                    Text(
                        text = "Weather data unavailable",
                        fontSize = 18.sp,
                        color = Color.White,
                        modifier = Modifier.padding(vertical = 32.dp)
                    )
                }
            }

            // Button to select this location (can be removed since we have the Save button now)
            Button(
                onClick = onSelectLocation,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2D6F9E)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "View Complete Forecast",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun WeatherDetail(icon: Int, value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteLocationItem(
    location: SavedLocation,
    weatherData: LocationWeatherData?,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    viewModel: WeatherViewModel
) {
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { totalDistance -> totalDistance * 0.5f },
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.StartToEnd) {
                onDelete()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = false,
        backgroundContent = {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp), // Identical padding as the content card
                colors = CardDefaults.cardColors(
                    containerColor = Color.Red.copy(alpha = 0.8f)
                ),
                shape = RoundedCornerShape(8.dp), // Same corner radius
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 16.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Delete",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        content = {
            LocationItem(
                location = location,
                weatherData = weatherData,
                onClick = onClick,
                viewModel = viewModel
            )
        }
    )
}

// Data class to represent a saved location
data class SavedLocation(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val isCurrentLocation: Boolean = false,
    var timezone: String? = null  // Store timezone identifier or offset
)

// Data class to represent weather data for a location
data class LocationWeatherData(
    val currentTemp: Int,
    val highTemp: Int,
    val lowTemp: Int
)
// Data class to represent a geocoding result
data class GeocodingResult(
    val name: String,
    val lat: Double,
    val lon: Double,
    val country: String = "",
    val state: String? = null
)
