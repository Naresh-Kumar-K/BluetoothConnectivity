package com.karbrusha.bluetoothlowenergy.data.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [SavedDeviceEntity::class], version = 1, exportSchema = false)
@TypeConverters(DeviceTypeConverter::class)
abstract class SavedDevicesDatabase : RoomDatabase() {
    abstract fun savedDevicesDao(): SavedDevicesDao
}
