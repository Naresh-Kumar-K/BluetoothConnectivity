package com.karbrusha.bluetoothlowenergy.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_devices")
data class SavedDeviceEntity(
    @PrimaryKey val address: String,
    val name: String,
    val deviceType: String,
)
