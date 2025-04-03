package com.example.jetpackdemoapp.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
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

    // Update the doWork() function in WeatherNotificationWorker.kt
    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "Weather notification worker started at ${System.currentTimeMillis()}")

        // Check if this is a test run
        val isTest = inputData.getBoolean("isTest", false)

        if (isTest) {
            Log.d(TAG, "Running in TEST mode - sending test notification")
            sendTestNotification("Test notification at ${System.currentTimeMillis()}")
            return@withContext Result.success()
        }

        try {
            // Get saved location coordinates
            val sharedPrefs = context.getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)
            var latitude = sharedPrefs.getFloat("saved_latitude", 0f).toDouble()
            var longitude = sharedPrefs.getFloat("saved_longitude", 0f).toDouble()

            Log.d(TAG, "Using coordinates for weather: lat=$latitude, lon=$longitude")

            // Try a direct API call instead of using the repository pattern
            try {
                val weatherService = RetrofitInstance.weatherService
                val response = weatherService.getCurrentWeatherDirect(
                    latitude,
                    longitude,
                    "metric", // Use metric units
                    API_KEY
                )

                Log.d(TAG, "API call successful: ${response.weather.firstOrNull()?.description}")

                // Create notification with response data
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
                    Log.d(TAG, "Weather notification sent successfully: $message")
                } catch (e: SecurityException) {
                    Log.e(TAG, "No notification permission: ${e.message}", e)
                    sendTestNotification("Weather: $message")
                }

                return@withContext Result.success()

            } catch (e: Exception) {
                Log.e(TAG, "Direct API call failed: ${e.message}", e)
                sendTestNotification("Weather API error: ${e.javaClass.simpleName}")
                return@withContext Result.failure()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to send weather notification: ${e.message}", e)
            Log.e(TAG, "Exception stack trace:", e)
            sendTestNotification("Weather service error: ${e.javaClass.simpleName}. Will try again tomorrow.")
            return@withContext Result.failure()
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
    @SuppressLint("ServiceCast")
    private fun getLastKnownLocation(): Location? {
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.e(TAG, "Location permission not granted")
                return null
            }

            val providers = locationManager.getProviders(true)
            var bestLocation: Location? = null
            for (provider in providers) {
                val location = locationManager.getLastKnownLocation(provider) ?: continue
                if (bestLocation == null || location.accuracy < bestLocation.accuracy) {
                    bestLocation = location
                }
            }
            return bestLocation
        } catch (e: Exception) {
            Log.e(TAG, "Error getting location", e)
            return null
        }
    }

    private fun sendNotification(title: String, message: String) {
        createNotificationChannel()

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.baseline_notifications_active_24)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            Log.d(TAG, "Notification sent successfully")
        } catch (e: SecurityException) {
            Log.e(TAG, "No notification permission", e)
        }
    }

    private fun sendTestNotification(message: String = "This is a test notification") {
        createNotificationChannel()

        val workerPrefs = context.getSharedPreferences("worker_prefs", Context.MODE_PRIVATE)
        val title = workerPrefs.getString("notification_title", "Today's Weather Forecast")
            ?: "Today's Weather Forecast"

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.baseline_notifications_active_24)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID + 100, notification)
            Log.d(TAG, "Test notification sent successfully with message: $message")
        } catch (e: SecurityException) {
            Log.e(TAG, "No notification permission", e)
        }
    }
}