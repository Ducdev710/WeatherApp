package com.example.jetpackdemoapp.data.model.service

import android.content.Context
import com.example.jetpackdemoapp.data.model.repository.WeatherRepository
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    // Base URL for OpenWeatherMap API
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

    // Create a method that provides WeatherRepository with context
    fun getWeatherRepository(context: Context): WeatherRepository {
        return WeatherRepository(weatherService, context.applicationContext)
    }
}