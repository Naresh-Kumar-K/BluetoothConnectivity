package com.karbrusha.bluetoothlowenergy.presentation

import com.karbrusha.bluetoothlowenergy.domain.BluetoothDevice

data class BluetoothUiState(
    val scannedDevices: List<BluetoothDevice> = emptyList(),
    val pairedDevices: List<BluetoothDevice> = emptyList(),
)