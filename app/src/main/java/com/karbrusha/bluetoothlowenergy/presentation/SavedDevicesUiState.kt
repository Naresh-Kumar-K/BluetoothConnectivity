package com.karbrusha.bluetoothlowenergy.presentation

import com.karbrusha.bluetoothlowenergy.domain.SavedDevice

data class SavedDevicesUiState(
    val savedDevices: List<SavedDevice> = emptyList(),
    val isLoading: Boolean = true,
)
