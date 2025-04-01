package com.example.jetpackdemoapp.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.jetpackdemoapp.R
import com.example.jetpackdemoapp.data.model.service.RetrofitInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class WeatherNotificationWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "weather_notifications"
        const val NOTIFICATION_ID = 1
        const val API_KEY = "7a85369669e56294a7779bb9f8be8563"
        const val TAG = "WeatherNotificationWorker"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "Weather notification worker started")

        // Check if this is a test run
        val isTest = inputData.getBoolean("isTest", false)

        if (isTest) {
            Log.d(TAG, "Running in TEST mode - sending test notification")
            sendTestNotification()
            return@withContext Result.success()
        }

        try {
            // Get saved location coordinates
            val sharedPrefs = context.getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)
            val latitude = sharedPrefs.getFloat("saved_latitude", 0f).toDouble()
            val longitude = sharedPrefs.getFloat("saved_longitude", 0f).toDouble()

            if (latitude == 0.0 && longitude == 0.0) {
                Log.e(TAG, "No saved location found")
                // Send a fallback notification rather than failing
                sendTestNotification("No location saved yet")
                return@withContext Result.failure()
            }

            Log.d(TAG, "Fetching weather for lat: $latitude, lon: $longitude")

            // Fetch current weather
            val repository = RetrofitInstance.weatherRepository
            val result = repository.getCurrentWeather(latitude, longitude, API_KEY).first()

            result.fold(
                onSuccess = { response ->
                    // Create notification
                    createNotificationChannel()

                    val weatherDescription = response.weather.firstOrNull()?.description?.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                    } ?: "Unknown"
                    val temperature = response.main.temp.toInt()
                    val cityName = response.name ?: "your location"

                    val formattedDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("E, MMM d"))
                    val title = "Today's Weather Forecast"
                    val message = "$formattedDate: $weatherDescription, $temperature°C in $cityName"

                    val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                        .setSmallIcon(R.drawable.baseline_notifications_active_24)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .build()

                    try {
                        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
                        Log.d(TAG, "Weather notification sent: $message")
                    } catch (e: SecurityException) {
                        Log.e(TAG, "No notification permission", e)
                    }

                    Result.success()
                },
                onFailure = { error ->
                    Log.e(TAG, "Error fetching weather: ${error.message}", error)
                    sendTestNotification("Weather fetch failed")
                    Result.retry()
                }
            )

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send weather notification: ${e.message}", e)
            sendTestNotification("Error: ${e.message}")
            Result.failure()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Weather Notifications"
            val descriptionText = "Daily weather updates"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendTestNotification(message: String = "This is a test notification") {
        createNotificationChannel()

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.baseline_notifications_active_24)
            .setContentTitle("Weather App Test")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID + 100, notification)
            Log.d(TAG, "Test notification sent successfully")
        } catch (e: SecurityException) {
            Log.e(TAG, "No notification permission", e)
        }
    }
}