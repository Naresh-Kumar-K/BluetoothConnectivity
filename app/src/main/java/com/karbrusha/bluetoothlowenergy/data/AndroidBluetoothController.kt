package com.karbrusha.bluetoothlowenergy.data

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import com.karbrusha.bluetoothlowenergy.domain.BluetoothController
import com.karbrusha.bluetoothlowenergy.domain.BluetoothDeviceDomain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@SuppressLint("MissingPermission")
class AndroidBluetoothController(
    private val context: Context,
) : BluetoothController {

    private val bluetoothManager by lazy {
        context.getSystemService(BluetoothManager::class.java) }

    private val bluetoothAdapter by lazy {
        bluetoothManager?.adapter
    }

    private val _scannedDevice = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    override val scannedDevice: StateFlow<List<BluetoothDeviceDomain>>
        get() = _scannedDevice.asStateFlow()

    private val _pairedDevice = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    override val pairedDevice: StateFlow<List<BluetoothDeviceDomain>>
        get() = _pairedDevice.asStateFlow()

    private val foundDeviceReceiver = FoundDeviceReceiver{device ->
        _scannedDevice.update { devices ->
            val newDevice = device.toBluetoothDeviceDomain()
            if(newDevice in devices) devices else devices +newDevice
        }
    }

    init {
        updatePairedDevice()
    }

    override fun startScan() {
    }

    override fun stopScan() {
        TODO("Not yet implemented")
    }

    override fun connect(device: BluetoothDeviceDomain) {
        TODO("Not yet implemented")
    }

    override fun disconnect(device: BluetoothDeviceDomain) {
        TODO("Not yet implemented")
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
        context.registerReceiver(
            foundDeviceReceiver,
            IntentFilter(BluetoothDevice.ACTION_FOUND)
        )
        updatePairedDevice()
        bluetoothAdapter?.startDiscovery()

    }

    override fun stopDiscovery() {
        if(!hasPermission(Manifest.permission.BLUETOOTH_SCAN)){
            return
        }
        bluetoothAdapter?.cancelDiscovery()
    }

    override fun release() {
       context.unregisterReceiver(foundDeviceReceiver)
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
}