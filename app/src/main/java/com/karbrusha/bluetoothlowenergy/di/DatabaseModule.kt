package com.karbrusha.bluetoothlowenergy.di

import android.content.Context
import androidx.room.Room
import com.karbrusha.bluetoothlowenergy.data.SavedDevicesRepositoryImpl
import com.karbrusha.bluetoothlowenergy.data.room.SavedDevicesDao
import com.karbrusha.bluetoothlowenergy.data.room.SavedDevicesDatabase
import com.karbrusha.bluetoothlowenergy.domain.SavedDevicesRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DatabaseModule {

    @Binds
    @Singleton
    abstract fun bindSavedDevicesRepository(
        impl: SavedDevicesRepositoryImpl,
    ): SavedDevicesRepository

    companion object {
        @Provides
        @Singleton
        fun provideSavedDevicesDatabase(
            @ApplicationContext context: Context,
        ): SavedDevicesDatabase = Room.databaseBuilder(
            context,
            SavedDevicesDatabase::class.java,
            "saved_devices.db",
        ).build()

        @Provides
        @Singleton
        fun provideSavedDevicesDao(db: SavedDevicesDatabase): SavedDevicesDao =
            db.savedDevicesDao()
    }
}
