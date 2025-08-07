package com.example.ems

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices


import android.Manifest



object LocationUtils {
    fun getCurrentLocation(context: Context, onLocation: (String) -> Unit) {
        val client = LocationServices.getFusedLocationProviderClient(context)
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            onLocation("Permission Denied")
            return
        }

        client.lastLocation.addOnSuccessListener { location ->
            location?.let {
                onLocation("${it.latitude}, ${it.longitude}")
            } ?: onLocation("Location not found")
        }
    }
}
