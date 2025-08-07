package com.example.ems.utils

import android.content.Context
import android.content.SharedPreferences

class PrefsManager private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "ems_prefs"
        private const val KEY_API_URL = "api_url"
        
        @Volatile private var instance: PrefsManager? = null
        
        fun getInstance(context: Context): PrefsManager {
            return instance ?: synchronized(this) {
                instance ?: PrefsManager(context.applicationContext).also { instance = it }
            }
        }
    }

    var apiUrl: String?
        get() = prefs.getString(KEY_API_URL, null)
        set(value) = prefs.edit().putString(KEY_API_URL, value).apply()
        
    fun clearApiUrl() {
        prefs.edit().remove(KEY_API_URL).apply()
    }
    
    fun hasApiUrl(): Boolean = apiUrl != null
}
