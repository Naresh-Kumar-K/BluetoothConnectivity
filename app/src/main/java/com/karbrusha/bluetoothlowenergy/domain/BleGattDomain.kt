package com.karbrusha.bluetoothlowenergy.domain

/**
 * Domain models for BLE scanning + generic GATT explorer.
 *
 * This app keeps the existing classic Bluetooth discovery logic intact; these types
 * are for the new BLE scan/connect screen only.
 */

enum class GattConnectionStatus {
    Disconnected,
    Connecting,
    Connected,
    Disconnecting,
    Error
}

data class BleCharacteristicRef(
    val serviceUuid: String,
    val characteristicUuid: String,
)

data class BleCharacteristicProperties(
    val readable: Boolean,
    val writable: Boolean,
    val writeWithoutResponse: Boolean,
    val notifiable: Boolean,
    val indicatable: Boolean,
)

data class BleCharacteristic(
    val uuid: String,
    val properties: BleCharacteristicProperties,
)

data class BleService(
    val uuid: String,
    val characteristics: List<BleCharacteristic>,
)

data class GattConnectionState(
    val status: GattConnectionStatus = GattConnectionStatus.Disconnected,
    val connectedDevice: BluetoothDeviceDomain? = null,
    val services: List<BleService> = emptyList(),
    // Last known characteristic values (updated on read/write callbacks).
    val characteristicValues: Map<BleCharacteristicRef, ByteArray> = emptyMap(),
    val errorMessage: String? = null,
)

