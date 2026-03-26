package com.karbrusha.bluetoothlowenergy.di

import android.content.Context
import com.karbrusha.bluetoothlowenergy.analytics.AnalyticsService
import com.karbrusha.bluetoothlowenergy.analytics.AnalyticsServiceImpl
import com.karbrusha.bluetoothlowenergy.data.AndroidBluetoothController
import com.karbrusha.bluetoothlowenergy.domain.BluetoothController
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AnalyticsModule {

    @Singleton
    @Binds
    abstract fun bindAnalyticsService(
        analyticsServiceImpl: AnalyticsServiceImpl
    ): AnalyticsService

//    @Provides
//    @Singleton
//    fun provideBluetoothController(@ApplicationContext context: Context): BluetoothController {
//        return AndroidBluetoothController(context)
//    }
}
