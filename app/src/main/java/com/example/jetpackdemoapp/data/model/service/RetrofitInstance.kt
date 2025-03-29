package com.example.jetpackdemoapp.data.model.service

import com.example.jetpackdemoapp.data.model.repository.WeatherRepository
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    // This is the issue - your base URL doesn't include the geocoding API path
    private const val BASE_URL = "https://api.openweathermap.org/"

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val weatherService: WeatherService by lazy {
        retrofit.create(WeatherService::class.java)
    }

    val weatherRepository: WeatherRepository by lazy {
        WeatherRepository(weatherService)
    }
}