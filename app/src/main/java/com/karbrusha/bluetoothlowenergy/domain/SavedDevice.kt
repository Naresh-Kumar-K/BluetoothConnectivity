package com.karbrusha.bluetoothlowenergy.domain

data class SavedDevice(
    val address: String,
    val name: String,
    val deviceType: DeviceType,
)
