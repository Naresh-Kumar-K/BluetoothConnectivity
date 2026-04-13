package com.karbrusha.bluetoothlowenergy.presentation

import com.karbrusha.bluetoothlowenergy.domain.BleCharacteristicRef
import com.karbrusha.bluetoothlowenergy.domain.BleService
import com.karbrusha.bluetoothlowenergy.domain.BleScannedDevice
import com.karbrusha.bluetoothlowenergy.domain.GattConnectionState

data class BleScanConnectUiState(
    val bleScannedDevices: List<BleScannedDevice> = emptyList(),
    val isBleScanning: Boolean = false,
    val filterUnnamed: Boolean = false,
    val savedAddresses: Set<String> = emptySet(),

    val gattConnectionState: GattConnectionState = GattConnectionState(),
    val lastErrorMessage: String? = null,

    val characteristicValues: Map<BleCharacteristicRef, ByteArray> = emptyMap(),
    val gattServices: List<BleService> = emptyList(),
)

