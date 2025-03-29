package com.example.jetpackdemoapp.data.model.Utils


import android.content.Context
import android.content.SharedPreferences
import com.example.jetpackdemoapp.weather.screen.SavedLocation
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class LocationPreferences(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREFERENCES_NAME, Context.MODE_PRIVATE
    )
    private val gson = Gson()

    fun getSavedLocations(): List<SavedLocation> {
        val locationsJson = sharedPreferences.getString(KEY_SAVED_LOCATIONS, null) ?: return emptyList()
        val type = object : TypeToken<List<SavedLocation>>() {}.type
        return gson.fromJson(locationsJson, type)
    }

    fun saveLocations(locations: List<SavedLocation>) {
        val filteredLocations = locations.filter { !it.isCurrentLocation }
        val locationsJson = gson.toJson(filteredLocations)
        sharedPreferences.edit().putString(KEY_SAVED_LOCATIONS, locationsJson).apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "weather_preferences"
        private const val KEY_SAVED_LOCATIONS = "saved_locations"
    }
}