package com.karbrusha.bluetoothlowenergy.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karbrusha.bluetoothlowenergy.domain.SavedDevicesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SavedDevicesViewModel @Inject constructor(
    private val repository: SavedDevicesRepository,
) : ViewModel() {

    val uiState = repository.savedDevices
        .map { devices -> SavedDevicesUiState(savedDevices = devices, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SavedDevicesUiState(isLoading = true),
        )

    fun removeDevice(address: String) {
        viewModelScope.launch {
            repository.delete(address)
        }
    }
}
