package com.karbrusha.bluetoothlowenergy.data

import com.karbrusha.bluetoothlowenergy.data.room.SavedDeviceEntity
import com.karbrusha.bluetoothlowenergy.data.room.SavedDevicesDao
import com.karbrusha.bluetoothlowenergy.domain.DeviceType
import com.karbrusha.bluetoothlowenergy.domain.SavedDevice
import com.karbrusha.bluetoothlowenergy.domain.SavedDevicesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SavedDevicesRepositoryImpl @Inject constructor(
    private val dao: SavedDevicesDao,
) : SavedDevicesRepository {

    override val savedDevices: Flow<List<SavedDevice>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun save(device: SavedDevice) {
        dao.upsert(device.toEntity())
    }

    override suspend fun delete(address: String) {
        dao.deleteByAddress(address)
    }

    private fun SavedDeviceEntity.toDomain() = SavedDevice(
        address = address,
        name = name,
        deviceType = DeviceType.valueOf(deviceType),
    )

    private fun SavedDevice.toEntity() = SavedDeviceEntity(
        address = address,
        name = name,
        deviceType = deviceType.name,
    )
}
