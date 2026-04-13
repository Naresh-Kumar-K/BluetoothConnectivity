package com.karbrusha.bluetoothlowenergy.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karbrusha.bluetoothlowenergy.domain.BluetoothDeviceDomain
import com.karbrusha.bluetoothlowenergy.domain.BleCharacteristicRef
import com.karbrusha.bluetoothlowenergy.domain.BluetoothController
import com.karbrusha.bluetoothlowenergy.domain.BleService
import com.karbrusha.bluetoothlowenergy.domain.GattConnectionState
import com.karbrusha.bluetoothlowenergy.domain.GattConnectionStatus
import com.karbrusha.bluetoothlowenergy.domain.BleScannedDevice
import com.karbrusha.bluetoothlowenergy.domain.SavedDevice
import com.karbrusha.bluetoothlowenergy.domain.SavedDevicesRepository
import com.karbrusha.bluetoothlowenergy.domain.resolveDeviceType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class BleScanConnectViewModel @Inject constructor(
    private val bluetoothController: BluetoothController,
    private val savedDevicesRepository: SavedDevicesRepository,
) : ViewModel() {

    private val _filterUnnamed = MutableStateFlow(false)

    val uiState = combine(
        bluetoothController.bleScannedDevices,
        bluetoothController.isBleScanning,
        bluetoothController.gattConnectionState,
        _filterUnnamed,
        savedDevicesRepository.savedDevices.map { list -> list.map { it.address }.toSet() },
    ) { scannedDevices, isScanning, gattState, filterUnnamed, savedAddresses ->
        BleScanConnectUiState(
            bleScannedDevices = scannedDevices,
            isBleScanning = isScanning,
            filterUnnamed = filterUnnamed,
            savedAddresses = savedAddresses,
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

    fun toggleFilterUnnamed() {
        _filterUnnamed.value = !_filterUnnamed.value
    }

    init {
        // When Bluetooth transitions from OFF → ON, clear stale results and restart scan.
        bluetoothController.isBluetoothEnabled
            .drop(1)
            .filter { isEnabled -> isEnabled }
            .onEach {
                bluetoothController.stopScan()
                bluetoothController.clearBleScannedDevices()
                bluetoothController.startScan()
            }
            .launchIn(viewModelScope)

        // Auto-connect to saved devices as soon as they appear in the scan results.
        combine(
            bluetoothController.bleScannedDevices,
            savedDevicesRepository.savedDevices.map { list -> list.map { it.address }.toSet() },
            bluetoothController.gattConnectionState,
        ) { scanned, savedAddresses, gattState ->
            Triple(scanned, savedAddresses, gattState)
        }
            .onEach { (scanned, savedAddresses, gattState) ->
                // Only auto-connect when not already connected/connecting
                val alreadyConnected = gattState.connectedDevice != null ||
                    gattState.status == com.karbrusha.bluetoothlowenergy.domain.GattConnectionStatus.Connecting
                if (alreadyConnected) return@onEach

                val target = scanned.firstOrNull { it.device.address in savedAddresses }
                if (target != null) {
                    bluetoothController.connect(target.device)
                }
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

    fun toggleSave(device: BluetoothDeviceDomain) {
        viewModelScope.launch {
            val currentlySaved = device.address in uiState.value.savedAddresses
            if (currentlySaved) {
                savedDevicesRepository.delete(device.address)
            } else {
                savedDevicesRepository.save(
                    SavedDevice(
                        address = device.address,
                        name = device.name ?: "Unknown Device",
                        deviceType = device.resolveDeviceType(),
                    )
                )
            }
        }
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

