package com.example.jetpackdemoapp.data.model.model

data class HourlyForecastResponse(
    //val list: List<HourlyForecastItem>
    val list: List<ListItem>
) {
    data class ListItem(
        val dt: Long,
        val main: Main,
        val weather: List<Weather>,
    )
}

data class HourlyForecastItem(
    val dt: Long,
    val main: Main,
    val weather: List<Weather>
)

data class HourlyWeather(
    val dt: Long,
    val temp: Double,
    val weather: List<Weather>,
    val picPath: String
)
