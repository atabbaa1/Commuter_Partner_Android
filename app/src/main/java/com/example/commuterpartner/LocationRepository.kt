// This object is needed to transfer info between LocationService.kt and MapsActivity.kt
// MapsActivity transfers its circle data. LocationService uses that to determine if the
// user has entered the circle, sending a boolean flag when the user enters the circle.
// MapsActivity reads this boolean flag and updates the UI accordingly.
// Originally, MapsActivity strictly received data (user location) from LocationServices
// via LocationRepository and computed the user entering circle itself. However,
// MapsActivity doesn't perform that in the background, so the code which occurs
// when the user enters the circle wouldn't occur until the app is reopened
// This procedure uses a Flow, where this object is updated and read in
// LocationService.kt and MapsActivity.kt
// This principle of having a special class/ state holder manage the UI state and logic is called
// Unidirectional Data Flow (UDF)
// A ViewModel was unsuitable because LocationService and MapsActivity are on separate lifecycles
// Therefore, LocationService was updating this ViewModel, but MapsActivity wasn't reading the changes.

package com.example.commuterpartner

import android.net.Uri
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class LocationData(
    val lat: Double,
    val long: Double,
    val radius: Double,
    val arrived: Boolean
)

object LocationRepository {
    // A flow for location data
    private val _locationFlow = MutableStateFlow(LocationData(0.0, 0.0, 0.0, false))
    val locationFlow: StateFlow<LocationData> = _locationFlow
    // A flow for the selected ringtone URI
    private val _ringtoneFlow = MutableStateFlow<Uri?>(null)
    val ringtoneFlow: StateFlow<Uri?> = _ringtoneFlow


    fun updateLocation(lat: Double, long: Double, radius: Double, arrived: Boolean) {
        _locationFlow.value = LocationData(lat, long, radius, arrived)
    }

    fun updateRingtone(ringtone: Uri) {
        _ringtoneFlow.value = ringtone
        Log.d("LocationRepository", "Updated ringtone")
    }
}

