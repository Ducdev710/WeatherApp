package com.example.jetpackdemoapp.notification

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object WeatherWorkScheduler {
    private const val WEATHER_WORK_NAME = "daily_weather_notification"

    @RequiresApi(Build.VERSION_CODES.O)
    fun scheduleDailyWeatherNotification(context: Context) {
        // Calculate initial delay to 7AM
        val initialDelay = calculateInitialDelay()

        // Set network constraints
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Create work request
        val dailyWorkRequest = PeriodicWorkRequestBuilder<WeatherNotificationWorker>(
            repeatInterval = 24,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        // Enqueue work
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WEATHER_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            dailyWorkRequest
        )

        Log.d("WeatherWorkScheduler", "Daily weather notification scheduled for 7AM")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun calculateInitialDelay(): Long {
        val now = LocalDateTime.now()
        val sevenAM = LocalDateTime.of(
            now.toLocalDate(),
            LocalTime.of(7, 0)
        )

        // If 7AM today has already passed, schedule for 7AM tomorrow
        var target = if (now.isAfter(sevenAM)) {
            sevenAM.plusDays(1)
        } else {
            sevenAM
        }

        return Duration.between(now, target).toMillis()
    }
    fun scheduleDailyWeatherNotificationForTesting(context: Context) {
        val data = workDataOf("isTest" to true)

        val testWorkRequest = OneTimeWorkRequestBuilder<WeatherNotificationWorker>()
            .setInitialDelay(30, TimeUnit.SECONDS)
            .setInputData(data)
            .addTag("test_weather_notification")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "test_weather_notification",
            ExistingWorkPolicy.REPLACE,
            testWorkRequest
        )

        Log.d("WeatherWorkScheduler", "Test notification scheduled with ID: ${testWorkRequest.id}")
    }
}