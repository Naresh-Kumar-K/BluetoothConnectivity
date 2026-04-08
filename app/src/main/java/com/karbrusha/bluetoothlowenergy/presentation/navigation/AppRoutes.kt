package com.karbrusha.bluetoothlowenergy.presentation.navigation

object AppRoutes {
    const val Classic = "classic"
    const val Ble = "ble"
    const val Saved = "saved"

    const val BleDetail = "bleDetail"
    const val BleLiveData = "bleLiveData"
    const val ArgDeviceAddress = "deviceAddress"

    fun bleDetailRoute(deviceAddress: String): String = "$BleDetail/$deviceAddress"
    fun bleLiveDataRoute(deviceAddress: String): String = "$BleLiveData/$deviceAddress"
}

