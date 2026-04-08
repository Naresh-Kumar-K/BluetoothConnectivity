package com.karbrusha.bluetoothlowenergy.domain

data class BleScannedDevice(
    val device: BluetoothDeviceDomain,
    val rssi: Int? = null,
)

