package com.karbrusha.bluetoothlowenergy.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karbrusha.bluetoothlowenergy.domain.BluetoothDeviceDomain
import com.karbrusha.bluetoothlowenergy.domain.BleCharacteristicRef
import com.karbrusha.bluetoothlowenergy.domain.BluetoothController
import com.karbrusha.bluetoothlowenergy.domain.BleService
import com.karbrusha.bluetoothlowenergy.domain.GattConnectionState
import com.karbrusha.bluetoothlowenergy.domain.BleScannedDevice
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class BleScanConnectViewModel @Inject constructor(
    private val bluetoothController: BluetoothController,
) : ViewModel() {

    val uiState = combine(
        bluetoothController.bleScannedDevices,
        bluetoothController.isBleScanning,
        bluetoothController.gattConnectionState,
    ) { scannedDevices, isScanning, gattState ->
        BleScanConnectUiState(
            bleScannedDevices = scannedDevices,
            isBleScanning = isScanning,
            gattConnectionState = gattState,
            lastErrorMessage = gattState.errorMessage,
            characteristicValues = gattState.characteristicValues,
            gattServices = gattState.services,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BleScanConnectUiState(),
    )

    fun startScan() {
        bluetoothController.startScan()
    }

    fun stopScan() {
        bluetoothController.stopScan()
    }

    fun connect(device: BluetoothDeviceDomain) {
        bluetoothController.connect(device)
    }

    fun disconnect(device: BluetoothDeviceDomain) {
        bluetoothController.disconnect(device)
    }

    fun readCharacteristic(characteristicRef: BleCharacteristicRef) {
        bluetoothController.readCharacteristic(characteristicRef)
    }

    fun writeCharacteristicHex(characteristicRef: BleCharacteristicRef, hexString: String) {
        val bytes = hexString.toByteArrayFromHexOrNull() ?: return
        bluetoothController.writeCharacteristic(characteristicRef, bytes)
    }

    private fun String.toByteArrayFromHexOrNull(): ByteArray? {
        // Accept whitespace, e.g. "01 A0 FF".
        val normalized = trim().replace(Regex("\\s+"), "")
        if (normalized.isEmpty()) return null
        if (normalized.length % 2 != 0) return null

        return try {
            ByteArray(normalized.length / 2) { index ->
                val start = index * 2
                normalized.substring(start, start + 2).toInt(16).toByte()
            }
        } catch (_: Exception) {
            null
        }
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothController.release()
    }
}

