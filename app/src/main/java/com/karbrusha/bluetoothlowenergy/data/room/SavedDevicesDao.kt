package com.karbrusha.bluetoothlowenergy.data.room

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedDevicesDao {
    @Query("SELECT * FROM saved_devices")
    fun observeAll(): Flow<List<SavedDeviceEntity>>

    @Upsert
    suspend fun upsert(entity: SavedDeviceEntity)

    @Query("DELETE FROM saved_devices WHERE address = :address")
    suspend fun deleteByAddress(address: String)
}
