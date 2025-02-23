// This class is needed to transfer the updated user location
// from LocationService.kt to MapsActivity.kt
// This procedure uses a Flow, where LocationViewModel is updated in
// LocationService.kt and read in MapsActivity.kt
// This principle of having a special class/ state holder manage the UI state and logic is called
// Unidirectional Data Flow (UDF)
// When instantiated, ViewModels are fed an object that implements the ViewModelStoreOwner interface
// The ViewModel is scoped to the Lifecycle of this object. If the object goes away permanently,
// the ViewModel will disappear from memory. If the object is destroyed, asynchronous work in the
// ViewModel persists. This is called SavedStateHandle

// This ViewModel eventually didn't work for transferring the user location between the LocationService and the MapsActivity
// The MapsActivity and LocationService create different instances of this ViewModel because a ViewModel
// is tied to an Activity or Fragment lifecycle whereas Services have their own independent lifecycle.
// Therefore, LocationService was updating this ViewModel, but MapsActivity wasn't reading the changes.

package com.example.commuterpartner

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LocationViewModel: ViewModel() {
    private val _locationFlow = MutableStateFlow(Pair(0.0, 0.0)) // Holds latest value
    // private val _locationFlow = MutableSharedFlow<Pair<Double, Double>>(replay = 1)
    val locationFlow = _locationFlow.asSharedFlow()

    fun updateLocation(lat: Double, long: Double) {
        /*
        * For MutableSharedFlow
        val newLocation = Pair(lat, long)
        Log.d("LocationViewModel", "updateLocation() called with: ($lat, $long)")

        viewModelScope.launch {
            _locationFlow.emit(newLocation)
            Log.d("LocationViewModel", "locationFlow emitted: ($lat, $long)")
        }
         */

        Log.d("LocationViewModel", "ViewModel instance: ${this.hashCode()}")
        val newLocation = Pair(lat, long)
        if (_locationFlow.value != newLocation) {  // Only update if different!
            _locationFlow.value = newLocation
            Log.d("LocationViewModel", "Updated location in LocationViewModel: ($lat, $long)")
        } else {
            Log.d("LocationViewModel", "Duplicate location in LocationViewModel, not updating")
        }
    }
}