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

package com.example.commuterpartner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class LocationViewModel: ViewModel() {
    private val _locationFlow = MutableSharedFlow<Pair<Double, Double>>(replay = 1)
    val locationFlow = _locationFlow.asSharedFlow()

    fun updateLocation(lat: Double, long: Double) {
        viewModelScope.launch {
            _locationFlow.emit(Pair(lat, long))
        }
    }
}