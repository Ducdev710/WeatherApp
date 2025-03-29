package com.example.jetpackdemoapp.data.model.repository

import DailyForecastResponse
import com.example.jetpackdemoapp.data.model.model.HourlyForecastResponse
import com.example.jetpackdemoapp.data.model.model.WeatherResponse
import com.example.jetpackdemoapp.data.model.service.WeatherService
import com.example.jetpackdemoapp.weather.screen.GeocodingResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class WeatherRepository(private val weatherService: WeatherService) {

    suspend fun getCurrentWeather(latitude: Double, longitude: Double, apiKey: String): Flow<Result<WeatherResponse>> = flow {
        try {
            val response = weatherService.getCurrentWeatherByCoordinates(latitude, longitude, apiKey)
            emit(Result.success(response))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    suspend fun getHourlyForecast(latitude: Double, longitude: Double, apiKey: String): Flow<Result<HourlyForecastResponse>> = flow {
        try {
            val response = weatherService.getHourlyWeather(latitude, longitude, apiKey)
            emit(Result.success(response))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    suspend fun getDailyForecast(latitude: Double, longitude: Double, apiKey: String, cnt: Int = 40): Flow<Result<DailyForecastResponse>> = flow {
        try {
            val response = weatherService.getDailyWeather(latitude, longitude, apiKey, cnt = cnt)
            emit(Result.success(response))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    suspend fun getGeocoding(query: String, limit: Int, apiKey: String): Flow<Result<List<GeocodingResult>>> = flow {
        try {
            val response = weatherService.getGeocodingResults(query, limit, apiKey)
            emit(Result.success(response))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}