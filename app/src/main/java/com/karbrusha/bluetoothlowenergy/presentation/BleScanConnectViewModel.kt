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
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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

    init {
        // When Bluetooth transitions from OFF → ON, clear stale results and restart scan.
        bluetoothController.isBluetoothEnabled
            .drop(1) // skip the initial replay value; only react to actual OFF→ON transitions
            .filter { isEnabled -> isEnabled }
            .onEach {
                bluetoothController.stopScan()
                bluetoothController.clearBleScannedDevices()
                bluetoothController.startScan()
            }
            .launchIn(viewModelScope)
    }

    fun startScan() {
        bluetoothController.startScan()
    }

    fun stopScan() {
        bluetoothController.stopScan()
    }

    fun clearScanResults() {
        bluetoothController.clearBleScannedDevices()
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

    fun setNotificationsEnabled(characteristicRef: BleCharacteristicRef, enabled: Boolean) {
        bluetoothController.setNotificationsEnabled(characteristicRef, enabled)
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

