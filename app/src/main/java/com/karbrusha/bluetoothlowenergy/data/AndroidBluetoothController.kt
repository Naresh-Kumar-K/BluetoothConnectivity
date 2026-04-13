package com.karbrusha.bluetoothlowenergy.data

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.util.Log
import com.karbrusha.bluetoothlowenergy.domain.BluetoothController
import com.karbrusha.bluetoothlowenergy.domain.BluetoothDeviceDomain
import com.karbrusha.bluetoothlowenergy.domain.BleCharacteristic
import com.karbrusha.bluetoothlowenergy.domain.BleCharacteristicProperties
import com.karbrusha.bluetoothlowenergy.domain.BleCharacteristicRef
import com.karbrusha.bluetoothlowenergy.domain.BleService
import com.karbrusha.bluetoothlowenergy.domain.BleScannedDevice
import com.karbrusha.bluetoothlowenergy.domain.GattConnectionState
import com.karbrusha.bluetoothlowenergy.domain.GattConnectionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

@SuppressLint("MissingPermission")
class AndroidBluetoothController(
    private val context: Context,
) : BluetoothController {

    private companion object {
        private const val TAG = "AndroidBluetoothController"
        private const val LOG_MAX_BYTES = 64
    }

    private val cccdUuid: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private val bluetoothManager by lazy {
        context.getSystemService(BluetoothManager::class.java) }

    private val bluetoothAdapter by lazy {
        bluetoothManager?.adapter
    }

    private val _isBluetoothEnabled = MutableStateFlow(bluetoothAdapter?.isEnabled == true)
    override val isBluetoothEnabled: StateFlow<Boolean> get() = _isBluetoothEnabled.asStateFlow()

    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != BluetoothAdapter.ACTION_STATE_CHANGED) return
            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
            _isBluetoothEnabled.update { state == BluetoothAdapter.STATE_ON }
        }
    }

    private val _scannedDevice = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    override val scannedDevice: StateFlow<List<BluetoothDeviceDomain>>
        get() = _scannedDevice.asStateFlow()

    private val _pairedDevice = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    override val pairedDevice: StateFlow<List<BluetoothDeviceDomain>>
        get() = _pairedDevice.asStateFlow()

    private val _bleScannedDevices = MutableStateFlow<List<BleScannedDevice>>(emptyList())
    override val bleScannedDevices: StateFlow<List<BleScannedDevice>>
        get() = _bleScannedDevices.asStateFlow()

    private val _isBleScanning = MutableStateFlow(false)
    override val isBleScanning: StateFlow<Boolean>
        get() = _isBleScanning.asStateFlow()

    private val _gattConnectionState = MutableStateFlow(GattConnectionState())
    override val gattConnectionState: StateFlow<GattConnectionState>
        get() = _gattConnectionState.asStateFlow()

    private val foundDeviceReceiver = FoundDeviceReceiver{device ->
        _scannedDevice.update { devices ->
            val newDevice = device.toBluetoothDeviceDomain()
            if(newDevice in devices) devices else devices +newDevice
        }
    }

    private var isClassicReceiverRegistered: Boolean = false

    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var scanCallback: ScanCallback? = null

    private var bluetoothGatt: BluetoothGatt? = null

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(
            gatt: BluetoothGatt,
            status: Int,
            newState: Int,
        ) {
            val address = gatt.device?.address

            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(TAG, "GATT connected device=$address status=$status")
                    val deviceDomain = address?.let { addr ->
                        bluetoothAdapter?.getRemoteDevice(addr)?.toBluetoothDeviceDomain()
                    }
                    _gattConnectionState.update {
                        it.copy(
                            status = GattConnectionStatus.Connected,
                            connectedDevice = deviceDomain,
                            services = emptyList(),
                            characteristicValues = emptyMap(),
                            errorMessage = null,
                        )
                    }
                    gatt.discoverServices()
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG, "GATT disconnected device=$address status=$status")
                    _gattConnectionState.update {
                        it.copy(
                            status = GattConnectionStatus.Disconnected,
                            connectedDevice = null,
                            services = emptyList(),
                            characteristicValues = emptyMap(),
                            errorMessage = null,
                        )
                    }
                }

                BluetoothProfile.STATE_DISCONNECTING -> {
                    _gattConnectionState.update { it.copy(status = GattConnectionStatus.Disconnecting) }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                _gattConnectionState.update {
                    it.copy(
                        status = GattConnectionStatus.Error,
                        errorMessage = "Services discovery failed: $status",
                        services = emptyList(),
                        characteristicValues = emptyMap(),
                    )
                }
                return
            }

            val services = gatt.services?.map { it.toBleService() }.orEmpty()
            Log.d(TAG, "Services discovered count=${services.size}")
            _gattConnectionState.update { it.copy(status = GattConnectionStatus.Connected, services = services) }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                _gattConnectionState.update {
                    it.copy(status = GattConnectionStatus.Error, errorMessage = "Characteristic read failed: $status")
                }
                return
            }

            val value = characteristic.value ?: return
            val serviceUuid = gatt.services
                ?.firstOrNull { svc -> svc.getCharacteristic(characteristic.uuid) != null }
                ?.uuid
                ?.toString()
                ?: return

            val ref = BleCharacteristicRef(
                serviceUuid = serviceUuid,
                characteristicUuid = characteristic.uuid.toString(),
            )

            Log.d(
                TAG,
                "READ ${shortUuid(serviceUuid)}/${shortUuid(characteristic.uuid.toString())} bytes=${value.size} hex=${value.toHexTruncated()}",
            )
            _gattConnectionState.update { state ->
                state.copy(
                    status = GattConnectionStatus.Connected,
                    errorMessage = null,
                    characteristicValues = state.characteristicValues + (ref to value.copyOf()),
                )
            }
        }

        @Deprecated("Use onCharacteristicChanged(gatt, characteristic, value) on newer APIs")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            val value = characteristic.value ?: return
            handleCharacteristicUpdate(gatt = gatt, characteristic = characteristic, value = value)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            handleCharacteristicUpdate(gatt = gatt, characteristic = characteristic, value = value)
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                _gattConnectionState.update {
                    it.copy(status = GattConnectionStatus.Error, errorMessage = "Characteristic write failed: $status")
                }
                return
            }

            val value = characteristic.value ?: return
            val serviceUuid = gatt.services
                ?.firstOrNull { svc -> svc.getCharacteristic(characteristic.uuid) != null }
                ?.uuid
                ?.toString()
                ?: return

            val ref = BleCharacteristicRef(
                serviceUuid = serviceUuid,
                characteristicUuid = characteristic.uuid.toString(),
            )

            Log.d(
                TAG,
                "WRITE ${shortUuid(serviceUuid)}/${shortUuid(characteristic.uuid.toString())} bytes=${value.size} hex=${value.toHexTruncated()}",
            )
            _gattConnectionState.update { state ->
                state.copy(
                    status = GattConnectionStatus.Connected,
                    errorMessage = null,
                    characteristicValues = state.characteristicValues + (ref to value.copyOf()),
                )
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int,
        ) {
            val charUuid = descriptor.characteristic?.uuid?.toString()
            val value = descriptor.value
            Log.d(
                TAG,
                "DESCRIPTOR_WRITE status=$status desc=${shortUuid(descriptor.uuid.toString())} char=${charUuid?.let { shortUuid(it) }} bytes=${value?.size ?: 0} hex=${value?.toHexTruncated()}",
            )
        }
    }

    init {
        updatePairedDevice()
        context.registerReceiver(
            bluetoothStateReceiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        )
    }

    override fun startScan() {
        if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) return
        if (_isBleScanning.value) return

        bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
        val scanner = bluetoothLeScanner ?: return

        // IMPORTANT: Don't wipe the current GATT connection when starting a new scan.
        // Users may want to keep a device connected while discovering other devices.
        val connected = _gattConnectionState.value.connectedDevice
        _bleScannedDevices.update {
            if (connected != null) listOf(BleScannedDevice(device = connected, rssi = null)) else emptyList()
        }

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device ?: return
                val newDevice = device.toBluetoothDeviceDomain()
                _bleScannedDevices.update { devices ->
                    val idx = devices.indexOfFirst { it.device.address == newDevice.address }
                    val updated = BleScannedDevice(device = newDevice, rssi = result.rssi)
                    if (idx == -1) {
                        devices + updated
                    } else {
                        devices.toMutableList().also { it[idx] = updated }
                    }
                }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.w("AndroidBluetoothController", "BLE scan failed: $errorCode")
                _gattConnectionState.update {
                    it.copy(status = GattConnectionStatus.Error, errorMessage = "BLE scan failed: $errorCode")
                }
            }
        }

        scanCallback = callback

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        // Some Android API levels require the (filters, settings, callback) overload.
        scanner.startScan(mutableListOf(), settings, callback)
        _isBleScanning.update { true }
    }

    override fun stopScan() {
        val scanner = bluetoothLeScanner ?: return
        val callback = scanCallback ?: return
        if (!_isBleScanning.value) return

        try {
            scanner.stopScan(callback)
        } catch (_: Exception) {
            // Ignore: stopping scan can throw if the system already stopped it.
        } finally {
            scanCallback = null
            _isBleScanning.update { false }
        }
    }

    override fun clearBleScannedDevices() {
        _bleScannedDevices.update { emptyList() }
    }

    override fun connect(device: BluetoothDeviceDomain) {
        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            _gattConnectionState.update {
                it.copy(status = GattConnectionStatus.Error, errorMessage = "Missing BLUETOOTH_CONNECT permission")
            }
            return
        }

        val adapter = bluetoothAdapter ?: return
        val remoteDevice = try {
            adapter.getRemoteDevice(device.address)
        } catch (_: Exception) {
            null
        } ?: return

        // Keep only one connection at a time to simplify state.
        bluetoothGatt?.let { oldGatt ->
            try {
                oldGatt.disconnect()
            } catch (_: Exception) {
                // ignore
            }
            try {
                oldGatt.close()
            } catch (_: Exception) {
                // ignore
            }
        }

        bluetoothGatt = null

        _gattConnectionState.update {
            it.copy(
                status = GattConnectionStatus.Connecting,
                connectedDevice = device,
                services = emptyList(),
                characteristicValues = emptyMap(),
                errorMessage = null,
            )
        }

        bluetoothGatt = remoteDevice.connectGatt(context, false, gattCallback)
    }

    override fun disconnect(device: BluetoothDeviceDomain) {
        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) return

        val current = _gattConnectionState.value.connectedDevice
        if (current?.address != device.address) return

        _gattConnectionState.update { it.copy(status = GattConnectionStatus.Disconnecting, errorMessage = null) }

        val gatt = bluetoothGatt ?: return
        try {
            gatt.disconnect()
        } catch (_: Exception) {
            // ignore
        } finally {
            try {
                gatt.close()
            } catch (_: Exception) {
                // ignore
            }
            bluetoothGatt = null
        }

        _gattConnectionState.update {
            it.copy(
                status = GattConnectionStatus.Disconnected,
                connectedDevice = null,
                services = emptyList(),
                characteristicValues = emptyMap(),
                errorMessage = null,
            )
        }
    }

    override fun pair(device: BluetoothDeviceDomain) {
        TODO("Not yet implemented")
    }

    override fun unpair(device: BluetoothDeviceDomain) {
        TODO("Not yet implemented")
    }

    override fun startDiscovery() {
        if(!hasPermission(Manifest.permission.BLUETOOTH_SCAN)){
            return
        }
        if (!isClassicReceiverRegistered) {
            context.registerReceiver(
                foundDeviceReceiver,
                IntentFilter(BluetoothDevice.ACTION_FOUND)
            )
            isClassicReceiverRegistered = true
        }
        updatePairedDevice()
        bluetoothAdapter?.startDiscovery()

    }

    override fun stopDiscovery() {
        // Cancelling discovery requires BLUETOOTH_SCAN; receiver cleanup does not.
        if (hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
            bluetoothAdapter?.cancelDiscovery()
        }

        if (isClassicReceiverRegistered) {
            try {
                context.unregisterReceiver(foundDeviceReceiver)
            } catch (_: IllegalArgumentException) {
                // Ignore: receiver might already be unregistered.
            } finally {
                isClassicReceiverRegistered = false
            }
        }
    }

    override fun clearClassicScannedDevices() {
        _scannedDevice.update { emptyList() }
    }

    override fun release() {
        if (_isBleScanning.value) stopScan()

        bluetoothGatt?.let { gatt ->
            try {
                gatt.disconnect()
            } catch (_: Exception) {
                // ignore
            }
            try {
                gatt.close()
            } catch (_: Exception) {
                // ignore
            }
        }
        bluetoothGatt = null

        if (isClassicReceiverRegistered) {
            try {
                context.unregisterReceiver(foundDeviceReceiver)
            } catch (_: IllegalArgumentException) {
                // ignore
            } finally {
                isClassicReceiverRegistered = false
            }
        }

        try {
            context.unregisterReceiver(bluetoothStateReceiver)
        } catch (_: IllegalArgumentException) {
            // ignore
        }
    }

    override fun readCharacteristic(characteristicRef: BleCharacteristicRef) {
        val gatt = bluetoothGatt ?: return
        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) return

        val service = try {
            gatt.getService(UUID.fromString(characteristicRef.serviceUuid))
        } catch (_: Exception) {
            null
        } ?: return

        val characteristic = try {
            service.getCharacteristic(UUID.fromString(characteristicRef.characteristicUuid))
        } catch (_: Exception) {
            null
        } ?: return

        gatt.readCharacteristic(characteristic)
    }

    override fun writeCharacteristic(characteristicRef: BleCharacteristicRef, value: ByteArray) {
        val gatt = bluetoothGatt ?: return
        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) return

        val service = try {
            gatt.getService(UUID.fromString(characteristicRef.serviceUuid))
        } catch (_: Exception) {
            null
        } ?: return

        val characteristic = try {
            service.getCharacteristic(UUID.fromString(characteristicRef.characteristicUuid))
        } catch (_: Exception) {
            null
        } ?: return

        characteristic.value = value

        // Choose the most compatible write type based on supported properties.
        val canWriteNoResponse =
            characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0
        val canWriteDefault = characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0
        characteristic.writeType = when {
            canWriteNoResponse && !canWriteDefault -> BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            else -> BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        }

        gatt.writeCharacteristic(characteristic)
    }

    override fun setNotificationsEnabled(characteristicRef: BleCharacteristicRef, enabled: Boolean) {
        val gatt = bluetoothGatt ?: return
        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) return

        val service = try {
            gatt.getService(UUID.fromString(characteristicRef.serviceUuid))
        } catch (_: Exception) {
            null
        } ?: return

        val characteristic = try {
            service.getCharacteristic(UUID.fromString(characteristicRef.characteristicUuid))
        } catch (_: Exception) {
            null
        } ?: return

        val notifySetOk = try {
            gatt.setCharacteristicNotification(characteristic, enabled)
        } catch (_: Exception) {
            false
        }

        if (!notifySetOk) {
            _gattConnectionState.update { it.copy(errorMessage = "Failed to set notifications") }
            return
        }

        val cccd = characteristic.getDescriptor(cccdUuid)
        if (cccd == null) {
            _gattConnectionState.update { it.copy(errorMessage = "Missing CCCD descriptor (0x2902)") }
            return
        }

        val supportsIndicate =
            characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0
        val supportsNotify =
            characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0

        val value = when {
            enabled && supportsIndicate && !supportsNotify -> BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            enabled -> BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            else -> BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
        }

        Log.d(
            TAG,
            "CCCD ${if (enabled) "ENABLE" else "DISABLE"} ${shortUuid(characteristicRef.serviceUuid)}/${shortUuid(characteristicRef.characteristicUuid)} " +
                "supportsNotify=$supportsNotify supportsIndicate=$supportsIndicate value=${value.toHexTruncated()}",
        )
        cccd.value = value
        val wrote = try {
            gatt.writeDescriptor(cccd)
        } catch (_: Exception) {
            false
        }

        if (!wrote) {
            _gattConnectionState.update { it.copy(errorMessage = "Failed to write CCCD descriptor") }
            return
        }

        _gattConnectionState.update { state ->
            val updated = if (enabled) state.notifyingCharacteristics + characteristicRef
            else state.notifyingCharacteristics - characteristicRef
            state.copy(notifyingCharacteristics = updated, errorMessage = null)
        }
    }

    private fun handleCharacteristicUpdate(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
    ) {
        val serviceUuid = gatt.services
            ?.firstOrNull { svc -> svc.getCharacteristic(characteristic.uuid) != null }
            ?.uuid
            ?.toString()
            ?: return

        val ref = BleCharacteristicRef(
            serviceUuid = serviceUuid,
            characteristicUuid = characteristic.uuid.toString(),
        )

        Log.d(
            TAG,
            "NOTIFY ${shortUuid(serviceUuid)}/${shortUuid(characteristic.uuid.toString())} bytes=${value.size} hex=${value.toHexTruncated()}",
        )
        _gattConnectionState.update { state ->
            state.copy(
                status = GattConnectionStatus.Connected,
                errorMessage = null,
                characteristicValues = state.characteristicValues + (ref to value.copyOf()),
            )
        }
    }

    private fun BluetoothGattService.toBleService(): BleService {
        return BleService(
            uuid = uuid.toString(),
            characteristics = characteristics.map { characteristic ->
                val properties = BleCharacteristicProperties(
                    readable = characteristic.properties and BluetoothGattCharacteristic.PROPERTY_READ != 0,
                    writable =
                        characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0 ||
                            characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0,
                    writeWithoutResponse =
                        characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0,
                    notifiable = characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0,
                    indicatable = characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0,
                )

                BleCharacteristic(
                    uuid = characteristic.uuid.toString(),
                    properties = properties,
                )
            },
        )
    }

    private fun updatePairedDevice() {
        if(!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)){
            return
        }
        bluetoothAdapter
            ?.bondedDevices
            ?.map { it.toBluetoothDeviceDomain()  }
            ?.also{ device ->
                _pairedDevice.update { device }
            }
    }

    private fun hasPermission(permission: String): Boolean{
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun shortUuid(uuid: String): String {
        return uuid.takeLast(4).uppercase()
    }

    private fun ByteArray.toHexTruncated(): String {
        val sliced = if (size > LOG_MAX_BYTES) copyOfRange(0, LOG_MAX_BYTES) else this
        val hex = sliced.joinToString("") { "%02X".format(it) }
        return if (size > LOG_MAX_BYTES) "$hex…(+${size - LOG_MAX_BYTES}b)" else hex
    }
}