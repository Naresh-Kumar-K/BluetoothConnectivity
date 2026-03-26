package com.karbrusha.bluetoothlowenergy.data

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import com.karbrusha.bluetoothlowenergy.domain.BluetoothDeviceDomain

//fun BluetoothDevice.toBluetoothDeviceDomain(); Blue {
//
//}

@SuppressLint("MissingPermission")
fun BluetoothDevice.toBluetoothDeviceDomain() : BluetoothDeviceDomain {
    return BluetoothDeviceDomain(
        name = name,
        address = address
    )
}