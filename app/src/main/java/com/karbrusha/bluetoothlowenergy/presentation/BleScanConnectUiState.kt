package com.karbrusha.bluetoothlowenergy.presentation

import com.karbrusha.bluetoothlowenergy.domain.BleCharacteristicRef
import com.karbrusha.bluetoothlowenergy.domain.BleService
import com.karbrusha.bluetoothlowenergy.domain.BluetoothDeviceDomain
import com.karbrusha.bluetoothlowenergy.domain.GattConnectionState

data class BleScanConnectUiState(
    val bleScannedDevices: List<BluetoothDeviceDomain> = emptyList(),
    val isBleScanning: Boolean = false,

    val gattConnectionState: GattConnectionState = GattConnectionState(),
    val lastErrorMessage: String? = null,

    // UI-friendly mirror (the UI displays values in hex).
    val characteristicValues: Map<BleCharacteristicRef, ByteArray> = emptyMap(),
    val gattServices: List<BleService> = emptyList(),
)

