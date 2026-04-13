package com.karbrusha.bluetoothlowenergy.domain

import kotlinx.coroutines.flow.StateFlow

interface BluetoothController {

    val isBluetoothEnabled: StateFlow<Boolean>

    val scannedDevice: StateFlow<List<BluetoothDeviceDomain>>
    val pairedDevice: StateFlow<List<BluetoothDeviceDomain>>

    // BLE scan state for the new scan/connect screen.
    val bleScannedDevices: StateFlow<List<BleScannedDevice>>
    val isBleScanning: StateFlow<Boolean>

    // Generic GATT explorer state.
    val gattConnectionState: StateFlow<GattConnectionState>

    fun startScan()
    fun stopScan()

    fun clearBleScannedDevices()
    fun clearClassicScannedDevices()

    fun connect(device: BluetoothDeviceDomain)
    fun disconnect(device: BluetoothDeviceDomain)

    fun pair(device: BluetoothDeviceDomain)
    fun unpair(device: BluetoothDeviceDomain)

    fun startDiscovery()
    fun stopDiscovery()

    fun readCharacteristic(characteristicRef: BleCharacteristicRef)
    fun writeCharacteristic(
        characteristicRef: BleCharacteristicRef,
        value: ByteArray
    )

    fun setNotificationsEnabled(
        characteristicRef: BleCharacteristicRef,
        enabled: Boolean,
    )

    fun release()
}