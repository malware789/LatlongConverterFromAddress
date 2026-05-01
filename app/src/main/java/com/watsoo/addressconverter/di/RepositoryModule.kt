package com.watsoo.addressconverter.di

import com.watsoo.addressconverter.data.local.AddressDao
import com.watsoo.addressconverter.data.remote.AddressRemoteDataSource
import com.watsoo.addressconverter.data.repository.AddressRepository
import com.watsoo.addressconverter.geocode.GeocoderClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

import android.util.Log

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideAddressRepository(
        addressDao: AddressDao,
        remoteDataSource: AddressRemoteDataSource,
        geocoderClient: GeocoderClient
    ): AddressRepository {
        Log.d("HiltModule", "provideAddressRepository started")
        val repo = AddressRepository(addressDao, remoteDataSource, geocoderClient)
        Log.d("HiltModule", "provideAddressRepository completed")
        return repo
    }
}
