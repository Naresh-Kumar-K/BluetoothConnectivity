package com.karbrusha.bluetoothlowenergy.domain

import kotlinx.coroutines.flow.StateFlow

interface BluetoothController {

    val scannedDevice: StateFlow<List<BluetoothDeviceDomain>>
    val pairedDevice: StateFlow<List<BluetoothDeviceDomain>>

    fun startScan()
    fun stopScan()

    fun connect(device: BluetoothDeviceDomain)
    fun disconnect(device: BluetoothDeviceDomain)

    fun pair(device: BluetoothDeviceDomain)
    fun unpair(device: BluetoothDeviceDomain)

    fun startDiscovery()
    fun stopDiscovery()

    fun release()
}