// This object is needed to transfer the updated user location
// from LocationService.kt to MapsActivity.kt
// This procedure uses a Flow, where this object is updated in
// LocationService.kt and read in MapsActivity.kt
// This principle of having a special class/ state holder manage the UI state and logic is called
// Unidirectional Data Flow (UDF)
// A ViewModel was unsuitable because LocationService and MapsActivity are on separate lifecycles
// Therefore, LocationService was updating this ViewModel, but MapsActivity wasn't reading the changes.

package com.example.commuterpartner

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object LocationRepository {
    private val _locationFlow = MutableStateFlow(Pair(0.0, 0.0))
    val locationFlow: StateFlow<Pair<Double, Double>> = _locationFlow

    fun updateLocation(lat: Double, long: Double) {
        _locationFlow.value = Pair(lat, long)
    }
}