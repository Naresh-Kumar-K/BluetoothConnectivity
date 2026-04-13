package com.karbrusha.bluetoothlowenergy.domain

enum class DeviceType {
    Oximeter,
    HeartRate,
    Generic,
}

fun BluetoothDeviceDomain.resolveDeviceType(): DeviceType {
    val n = name?.lowercase() ?: return DeviceType.Generic
    return when {
        n.contains("oxy") || n.contains("spo2") || n.contains("oximeter") || n.contains("pulox") -> DeviceType.Oximeter
        n.contains("heart") || n.contains("hr ") || n.contains("hrm") -> DeviceType.HeartRate
        else -> DeviceType.Generic
    }
}
