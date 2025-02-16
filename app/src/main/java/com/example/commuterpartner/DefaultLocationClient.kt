package com.example.commuterpartner

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class DefaultLocationClient(
    private val context: Context,
    private val client: FusedLocationProviderClient): LocationClient {

    @SuppressLint("MissingPermission")
    override fun getLocationUpdates(interval: Long): Flow<Location> {
        return callbackFlow {
            if (!context.hasLocationPermission()) {
                throw LocationClient.locationException("Location Permission Denied")
            }
            // Check to see if there's enough network/ service to provide location
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            if (!isGpsEnabled && !isNetworkEnabled) {
                throw LocationClient.locationException("GPS is disabled")
            }
            // Now, actually fetch the location by creating a LocationRequest
            val request = LocationRequest.Builder(interval) // val request = LocationRequest.create().setInterval(interval)
                .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                .build()
            val locationCallback = object : LocationCallback() {
                // The below function is called whenever the FusedLocationProviderClient fetches a new location
                override fun onLocationResult(p0: LocationResult) {
                    super.onLocationResult(p0)
                    // p0.locations is a list of all the locations, with the last one being the most recent one
                    // If there is a location, we want to notify the Flow and send/ attach something with it
                    p0.locations.lastOrNull()?.let { location ->
                        launch { send(location) }
                    }
                }
            }

            client.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
            awaitClose {
                client.removeLocationUpdates(locationCallback)
            }
        }
    }
}