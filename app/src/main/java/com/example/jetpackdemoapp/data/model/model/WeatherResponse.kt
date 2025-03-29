package com.example.jetpackdemoapp.data.model.model

data class WeatherResponse(
    val main: Main,
    val weather: List<Weather>,
    val wind: Wind,
    val rain: Rain?,
    val name: String,
    val visibility: Int?,
    val sys: Sys?,
    val hourly: List<HourlyWeather>,
    val daily: List<FutureWeather>,
)

data class Main(
    val temp: Double,
    val feels_like: Double,
    val temp_min: Double,
    val temp_max: Double,
    val pressure: Int,
    val humidity: Int
)

data class Wind(
    val speed: Double
)

data class Rain(
    val the1h: Double?
)

data class Sys(
    val sunset: Int?,
    val sunrise: Int?
)

data class FutureWeather(
    val day: String,
    val picPath: String,
    val status: String,
    val highTemp: Int,
    val lowTemp: Int,
    val uvi: Double? = null
)
data class Weather(
    val description: String,
    val icon: String
)