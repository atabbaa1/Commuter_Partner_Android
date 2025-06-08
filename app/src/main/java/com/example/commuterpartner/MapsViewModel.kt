package com.example.commuterpartner

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MapsUIState (
    val targetAcquired: Boolean,
    val active: Boolean,
)

class MapsViewModel: ViewModel() {
    // Expose screen UI State
    private val _uiStateFlow = MutableStateFlow(MapsUIState(targetAcquired = false, active = false))
    val uiStateFlow: StateFlow<MapsUIState> = _uiStateFlow.asStateFlow()

    fun preserveUIState(targetAcquired: Boolean, active: Boolean) {
        _uiStateFlow.value = MapsUIState(targetAcquired, active)
    }
}