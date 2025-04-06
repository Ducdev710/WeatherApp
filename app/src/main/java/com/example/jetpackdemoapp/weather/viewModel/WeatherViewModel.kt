package com.example.jetpackdemoapp.weather.viewModel

import DailyForecastResponse
import android.app.Application
import android.content.Context
import android.os.Build
import android.provider.Settings.Secure.putFloat
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.jetpackdemoapp.data.model.model.HourlyForecastResponse
import com.example.jetpackdemoapp.data.model.model.WeatherResponse
import com.example.jetpackdemoapp.data.model.repository.WeatherRepository
import com.example.jetpackdemoapp.weather.screen.GeocodingResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class WeatherViewModel(
    private val repository: WeatherRepository,
    private val apiKey: String
) : ViewModel() {

    // Current weather data for the actively displayed location
    private val _currentWeatherState = MutableStateFlow<WeatherUiState<WeatherResponse>>(
        WeatherUiState.Loading
    )
    val currentWeatherState: StateFlow<WeatherUiState<WeatherResponse>> = _currentWeatherState

    private val _hourlyForecastState = MutableStateFlow<WeatherUiState<HourlyForecastResponse>>(
        WeatherUiState.Loading
    )
    val hourlyForecastState: StateFlow<WeatherUiState<HourlyForecastResponse>> = _hourlyForecastState

    private val _dailyForecastState = MutableStateFlow<WeatherUiState<DailyForecastResponse>>(
        WeatherUiState.Loading
    )
    val dailyForecastState: StateFlow<WeatherUiState<DailyForecastResponse>> = _dailyForecastState

    private val _geocodingState = MutableStateFlow<WeatherUiState<List<GeocodingResult>>>(
        WeatherUiState.Loading
    )
    val geocodingState: StateFlow<WeatherUiState<List<GeocodingResult>>> = _geocodingState

    // Separate data specifically for My Location - never overwritten by other locations
    private val _myLocationWeather = MutableStateFlow<WeatherResponse?>(null)
    val myLocationWeather: StateFlow<WeatherResponse?> = _myLocationWeather

    private val _myLocationCoordinates = MutableStateFlow<Pair<Double, Double>?>(null)
    val myLocationCoordinates: StateFlow<Pair<Double, Double>?> = _myLocationCoordinates

    // New - Weather data for My Location only
    private val _myLocationDailyForecast = MutableStateFlow<DailyForecastResponse?>(null)
    val myLocationDailyForecast: StateFlow<DailyForecastResponse?> = _myLocationDailyForecast

    // Flag để biết đã có vị trí định vị chưa
    private val _hasLocationBeenSet = MutableStateFlow(false)
    val hasLocationBeenSet: StateFlow<Boolean> = _hasLocationBeenSet

    // Add permission state management
    private val _permissionFlowState = MutableStateFlow(PermissionFlowState.INITIAL)
    val permissionFlowState: StateFlow<PermissionFlowState> = _permissionFlowState

    enum class PermissionFlowState {
        INITIAL,
        NOTIFICATION_GRANTED,
        LOCATION_REQUESTED,
        ALL_GRANTED
    }

    private val _temperatureUnit = MutableStateFlow(TemperatureUnit.CELSIUS)
    val temperatureUnit: StateFlow<TemperatureUnit> = _temperatureUnit

    // Thêm enum class cho đơn vị nhiệt độ
    enum class TemperatureUnit {
        CELSIUS,
        FAHRENHEIT  // The correct enum case
    }

    // Thêm hàm để chuyển đổi đơn vị nhiệt độ
    fun setTemperatureUnit(unit: TemperatureUnit) {
        _temperatureUnit.value = unit
        saveTemperatureUnit(unit)
    }

    // Hàm chuyển đổi từ Celsius sang Fahrenheit
    fun convertToCurrentUnit(celsiusTemp: Double): Double {
        return if (_temperatureUnit.value == TemperatureUnit.FAHRENHEIT) {
            celsiusTemp * 9/5 + 32
        } else {
            celsiusTemp
        }
    }

    // Hàm lưu đơn vị nhiệt độ vào SharedPreferences
    // Replace both existing saveTemperatureUnit methods with this one
    private fun saveTemperatureUnit(unit: TemperatureUnit) {
        viewModelScope.launch {
            try {
                repository.saveTemperatureUnit(unit.name)
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Error saving temperature unit", e)
            }
        }
    }
    // Add this init block right after your variable declarations
    init {
        loadTemperatureUnit()
    }


    // Hàm đọc đơn vị nhiệt độ từ SharedPreferences
    private fun loadTemperatureUnit() {
        viewModelScope.launch {
            try {
                val savedUnit = repository.getTemperatureUnit()
                _temperatureUnit.value = savedUnit?.let {
                    // Convert string to enum using uppercase to match enum case
                    TemperatureUnit.valueOf(it.uppercase())
                } ?: TemperatureUnit.CELSIUS
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Error loading temperature unit", e)
                _temperatureUnit.value = TemperatureUnit.CELSIUS
            }
        }
    }

    // Add this to your WeatherViewModel.kt
    fun initTemperatureUnit() {
        viewModelScope.launch {
            try {
                val savedUnit = repository.getTemperatureUnit()
                _temperatureUnit.value = savedUnit?.let {
                    // Convert string to enum using uppercase to match enum case
                    TemperatureUnit.valueOf(it.uppercase())
                } ?: TemperatureUnit.CELSIUS
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Error loading temperature unit", e)
                _temperatureUnit.value = TemperatureUnit.CELSIUS
            }
        }
    }
    // Save state to SharedPreferences when permissions change
    fun savePermissionState(context: Context, state: PermissionFlowState) {
        _permissionFlowState.value = state
        context.getSharedPreferences("permissions_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("permission_flow_state", state.name)
            .apply()
    }

    // Restore permission state on app start
    fun restorePermissionState(context: Context) {
        val savedState = context.getSharedPreferences("permissions_prefs", Context.MODE_PRIVATE)
            .getString("permission_flow_state", PermissionFlowState.INITIAL.name)
        _permissionFlowState.value = PermissionFlowState.valueOf(savedState ?: "INITIAL")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun fetchWeatherData(latitude: Double, longitude: Double) {
        fetchCurrentWeather(latitude, longitude)
        fetchHourlyForecast(latitude, longitude)
        fetchDailyForecast(latitude, longitude)
    }

    // Sử dụng hàm này khi bạn muốn load dữ liệu cho một vị trí khác không phải My Location
    @RequiresApi(Build.VERSION_CODES.O)
    fun fetchWeatherDataForSelectedCity(latitude: Double, longitude: Double) {
        fetchCurrentWeather(latitude, longitude)
        fetchHourlyForecast(latitude, longitude)
        fetchDailyForecast(latitude, longitude)
    }

    // Sử dụng hàm này khi bạn cập nhật vị trí hiện tại (My Location)
    @RequiresApi(Build.VERSION_CODES.O)
    fun updateMyLocationWeather(latitude: Double, longitude: Double) {
        saveMyLocation(latitude, longitude)
        fetchMyLocationWeatherData(latitude, longitude)
        // Also update current display
        fetchWeatherData(latitude, longitude)
    }

    // New - Fetch weather for My Location and store separately
    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchMyLocationWeatherData(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            try {
                // Get current weather for My Location
                repository.getCurrentWeather(latitude, longitude, apiKey).collect { result ->
                    result.fold(
                        onSuccess = { response ->
                            _myLocationWeather.value = response
                        },
                        onFailure = { error ->
                            Log.e("WeatherViewModel", "Error fetching My Location current weather", error)
                        }
                    )
                }

                // Get daily forecast for My Location
                repository.getDailyForecast(latitude, longitude, apiKey, cnt = 40).collect { result ->
                    result.fold(
                        onSuccess = { response ->
                            _myLocationDailyForecast.value = response
                        },
                        onFailure = { error ->
                            Log.e("WeatherViewModel", "Error fetching My Location daily forecast", error)
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Exception in fetchMyLocationWeatherData", e)
            }
        }
    }

    fun getMyLocationWeather(): WeatherResponse? {
        return _myLocationWeather.value
    }

    fun getMyLocationCoordinates(): Pair<Double, Double>? {
        return _myLocationCoordinates.value
    }

    fun getMyLocationDailyForecast(): DailyForecastResponse? {
        return _myLocationDailyForecast.value
    }

    // Cập nhật hàm getMyLocation() trong WeatherViewModel
    fun getMyLocation(): Pair<Double, Double> {
        // Sử dụng giá trị từ StateFlow đã lưu trữ trước đó
        return _myLocationCoordinates.value ?: Pair(0.0, 0.0) // Giá trị mặc định nếu chưa có tọa độ
    }

    private fun fetchCurrentWeather(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            _currentWeatherState.value = WeatherUiState.Loading
            repository.getCurrentWeather(latitude, longitude, apiKey).collect { result ->
                result.fold(
                    onSuccess = { response ->
                        _currentWeatherState.value = WeatherUiState.Success(response)
                    },
                    onFailure = { error ->
                        Log.e("WeatherViewModel", "Error fetching current weather", error)
                        _currentWeatherState.value =
                            WeatherUiState.Error(error.message ?: "Unknown error")
                    }
                )
            }
        }
    }

    private fun fetchHourlyForecast(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            _hourlyForecastState.value = WeatherUiState.Loading
            repository.getHourlyForecast(latitude, longitude, apiKey).collect { result ->
                result.fold(
                    onSuccess = { response ->
                        _hourlyForecastState.value = WeatherUiState.Success(response)
                    },
                    onFailure = { error ->
                        Log.e("WeatherViewModel", "Error fetching hourly forecast", error)
                        _hourlyForecastState.value =
                            WeatherUiState.Error(error.message ?: "Unknown error")
                    }
                )
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchDailyForecast(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            _dailyForecastState.value = WeatherUiState.Loading
            // Request more data points to ensure we get enough for 7 days
            // Use cnt=40 parameter to get more forecast data points
            repository.getDailyForecast(latitude, longitude, apiKey, cnt = 40).collect { result ->
                result.fold(
                    onSuccess = { response ->
                        // Log the response to verify data quantity
                        Log.d("WeatherViewModel", "Daily forecast data points: ${response.list.size}")

                        // Log a few sample dates to verify the data
                        response.list.take(5).forEach {
                            val timestamp = it.dt
                            val date = java.time.LocalDateTime.ofEpochSecond(
                                timestamp.toLong(), 0, java.time.ZoneOffset.UTC
                            )
                            Log.d("WeatherViewModel", "Forecast date: $date, temp: ${it.main.temp}")
                        }

                        _dailyForecastState.value = WeatherUiState.Success(response)
                    },
                    onFailure = { error ->
                        Log.e("WeatherViewModel", "Error fetching daily forecast", error)
                        _dailyForecastState.value =
                            WeatherUiState.Error(error.message ?: "Unknown error")
                    }
                )
            }
        }
    }

    suspend fun getCitySuggestions(query: String): List<GeocodingResult> {
        Log.d("WeatherViewModel", "Fetching suggestions for: $query")
        if (query.length < 2) return emptyList()

        return try {
            val resultList = mutableListOf<GeocodingResult>()

            repository.getGeocoding(query, 5, apiKey).collect { result ->
                result.fold(
                    onSuccess = { cities ->
                        Log.d("WeatherViewModel", "Success: found ${cities.size} cities")
                        resultList.addAll(cities)
                    },
                    onFailure = { error ->
                        Log.e("WeatherViewModel", "Failed to get cities", error)
                    }
                )
            }

            Log.d("WeatherViewModel", "Returning ${resultList.size} cities")
            resultList
        } catch (e: Exception) {
            Log.e("WeatherViewModel", "Exception in getCitySuggestions", e)
            emptyList()
        }
    }

    fun searchCity(query: String) {
        viewModelScope.launch {
            _geocodingState.value = WeatherUiState.Loading
            try {
                repository.getGeocoding(query, 5, apiKey).collect { result ->
                    result.fold(
                        onSuccess = { response ->
                            _geocodingState.value = WeatherUiState.Success(response)
                        },
                        onFailure = { error ->
                            Log.e("WeatherViewModel", "Error fetching geocoding", error)
                            _geocodingState.value =
                                WeatherUiState.Error(error.message ?: "Unknown error")
                        }
                    )
                }
            } catch (e: Exception) {
                _geocodingState.value = WeatherUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun saveMyLocation(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            _myLocationCoordinates.value = Pair(latitude, longitude)
            repository.getCurrentWeather(latitude, longitude, apiKey).collect { result ->
                result.fold(
                    onSuccess = { response ->
                        _myLocationWeather.value = response
                        _hasLocationBeenSet.value = true
                    },
                    onFailure = { error ->
                        Log.e("WeatherViewModel", "Error fetching My Location weather", error)
                    }
                )
            }
        }
    }

    // Add to WeatherViewModel
    fun getCurrentWeatherForBottomSheet(latitude: Double, longitude: Double, onResult: (WeatherResponse?) -> Unit) {
        viewModelScope.launch {
            try {
                repository.getCurrentWeather(latitude, longitude, apiKey).collect { result ->
                    result.fold(
                        onSuccess = { response ->
                            onResult(response)
                        },
                        onFailure = { error ->
                            Log.e("WeatherViewModel", "Error fetching weather for bottom sheet", error)
                            onResult(null)
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Exception in getCurrentWeatherForBottomSheet", e)
                onResult(null)
            }
        }
    }

    fun getDailyForecastForBottomSheet(latitude: Double, longitude: Double, onResult: (DailyForecastResponse?) -> Unit) {
        viewModelScope.launch {
            try {
                repository.getDailyForecast(latitude, longitude, apiKey).collect { result ->
                    result.fold(
                        onSuccess = { response ->
                            onResult(response)
                        },
                        onFailure = { error ->
                            Log.e("WeatherViewModel", "Error fetching daily forecast for bottom sheet", error)
                            onResult(null)
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Exception in getDailyForecastForBottomSheet", e)
                onResult(null)
            }
        }
    }
    // Add to WeatherViewModel
    // Add to WeatherViewModel
    fun saveLocationForNotifications(context: Context) {
        val coordinates = _myLocationCoordinates.value
        if (coordinates == null) {
            Log.e("WeatherViewModel", "No saved location coordinates")
            return
        }

        val latitude = coordinates.first
        val longitude = coordinates.second

        // Save coordinates to SharedPreferences for the Worker to access
        val sharedPreferences = context.getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().apply {
            putFloat("saved_latitude", latitude.toFloat())
            putFloat("saved_longitude", longitude.toFloat())
            apply()
        }
    }

    // Factory class to create ViewModel with parameters
    class Factory(private val repository: WeatherRepository, private val apiKey: String) :
        ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(WeatherViewModel::class.java)) {
                return WeatherViewModel(repository, apiKey) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

// UI state sealed class for handling different states
sealed class WeatherUiState<out T> {
    object Loading : WeatherUiState<Nothing>()
    data class Success<T>(val data: T) : WeatherUiState<T>()
    data class Error(val message: String) : WeatherUiState<Nothing>()
}