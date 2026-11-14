package com.khanbashi.darb.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Reads speed and trip distance from the device GPS. No car data (OBD2) is needed. */
object SpeedTracker {
    private val _speedKmh = MutableStateFlow(0f)
    val speedKmh: StateFlow<Float> = _speedKmh

    private val _tripKm = MutableStateFlow(0f)
    val tripKm: StateFlow<Float> = _tripKm

    private val _hasFix = MutableStateFlow(false)
    val hasFix: StateFlow<Boolean> = _hasFix

    private var last: Location? = null
    private var started = false

    fun hasPermission(ctx: Context): Boolean =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            _hasFix.value = true
            val kmh = if (location.hasSpeed()) location.speed * 3.6f else 0f
            _speedKmh.value = if (kmh < 2f) 0f else kmh
            val prev = last
            if (prev != null && kmh > 3f) {
                val meters = prev.distanceTo(location)
                if (meters < 500f) _tripKm.value = _tripKm.value + meters / 1000f
            }
            last = location
        }

        override fun onProviderEnabled(provider: String) {}

        override fun onProviderDisabled(provider: String) {
            _hasFix.value = false
            _speedKmh.value = 0f
        }

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    }

    @SuppressLint("MissingPermission")
    fun start(ctx: Context) {
        if (started || !hasPermission(ctx)) return
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        try {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, listener, Looper.getMainLooper())
            started = true
        } catch (_: Exception) {
            // Some head units have no GPS provider; the speedometer then stays at 0.
        }
    }

    fun resetTrip() {
        _tripKm.value = 0f
    }
}
