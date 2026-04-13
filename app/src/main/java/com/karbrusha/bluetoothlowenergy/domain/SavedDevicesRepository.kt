package com.karbrusha.bluetoothlowenergy.domain

import kotlinx.coroutines.flow.Flow

interface SavedDevicesRepository {
    val savedDevices: Flow<List<SavedDevice>>
    suspend fun save(device: SavedDevice)
    suspend fun delete(address: String)
}
