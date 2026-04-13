package com.karbrusha.bluetoothlowenergy.data.room

import androidx.room.TypeConverter
import com.karbrusha.bluetoothlowenergy.domain.DeviceType

class DeviceTypeConverter {
    @TypeConverter
    fun fromDeviceType(value: DeviceType): String = value.name

    @TypeConverter
    fun toDeviceType(value: String): DeviceType = DeviceType.valueOf(value)
}
