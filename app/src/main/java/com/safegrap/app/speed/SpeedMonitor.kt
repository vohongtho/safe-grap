package com.safegrap.app.speed

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle

class SpeedMonitor(context: Context, private val onSpeed: (Float?) -> Unit) : LocationListener {
    private val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    @SuppressLint("MissingPermission")
    fun start() {
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) { onSpeed(null); return }
        manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500L, 0f, this)
    }
    fun stop() = manager.removeUpdates(this)
    override fun onLocationChanged(location: Location) {
        onSpeed(if (location.hasSpeed() && location.accuracy <= 50f) location.speed * 3.6f else null)
    }
    @Deprecated("Deprecated in Java") override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
    override fun onProviderDisabled(provider: String) = onSpeed(null)
}
