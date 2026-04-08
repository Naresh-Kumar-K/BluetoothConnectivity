package com.karbrusha.bluetoothlowenergy.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karbrusha.bluetoothlowenergy.presentation.BluetoothUiState
import com.karbrusha.bluetoothlowenergy.domain.BluetoothController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class BluetoothViewmodel @Inject constructor(
    private val bluetoothController: BluetoothController
): ViewModel() {

    private val _state = MutableStateFlow(BluetoothUiState())
    val state = combine(
        bluetoothController.scannedDevice,
        bluetoothController.pairedDevice,
        _state
    ) { scannedDevice, pairedDevice, state ->
        state.copy(scannedDevices = scannedDevice, pairedDevices = pairedDevice)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _state.value)


    fun startScan(){
         bluetoothController.startDiscovery()
    }

    fun stopScan() {
        bluetoothController.stopDiscovery()
    }

    fun clearScanResults() {
        bluetoothController.clearClassicScannedDevices()
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothController.release()
    }
}