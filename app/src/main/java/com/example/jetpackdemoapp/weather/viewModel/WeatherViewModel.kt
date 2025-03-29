package com.example.jetpackdemoapp.weather.viewModel

import DailyForecastResponse
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.jetpackdemoapp.data.model.model.HourlyForecastResponse
import com.example.jetpackdemoapp.data.model.model.WeatherResponse
import com.example.jetpackdemoapp.data.model.repository.WeatherRepository
import com.example.jetpackdemoapp.weather.screen.GeocodingResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WeatherViewModel(
    private val repository: WeatherRepository,
    private val apiKey: String
) : ViewModel() {

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

    @RequiresApi(Build.VERSION_CODES.O)
    fun fetchWeatherData(latitude: Double, longitude: Double) {
        fetchCurrentWeather(latitude, longitude)
        fetchHourlyForecast(latitude, longitude)
        fetchDailyForecast(latitude, longitude)
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