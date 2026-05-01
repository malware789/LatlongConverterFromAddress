package com.watsoo.addressconverter.di

import com.watsoo.addressconverter.config.AppConfig
import com.watsoo.addressconverter.data.remote.AddressApi
import com.watsoo.addressconverter.data.remote.AddressRemoteDataSource
import com.watsoo.addressconverter.data.remote.FakeAddressRemoteDataSource
import com.watsoo.addressconverter.data.remote.RetrofitAddressRemoteDataSource
import com.watsoo.addressconverter.geocode.FakeGeocoderClient
import com.watsoo.addressconverter.geocode.GeocoderClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(AppConfig.API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(AppConfig.API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(AppConfig.API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(AppConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideAddressApi(retrofit: Retrofit): AddressApi {
        return retrofit.create(AddressApi::class.java)
    }

    @Provides
    @Singleton
    fun provideAddressRemoteDataSource(addressApi: AddressApi): AddressRemoteDataSource {
        return if (AppConfig.USE_FAKE_API) {
            FakeAddressRemoteDataSource()
        } else {
            RetrofitAddressRemoteDataSource(addressApi)
        }
    }

    @Provides
    @Singleton
    fun provideGeocoderClient(
        @dagger.hilt.android.qualifiers.ApplicationContext context: android.content.Context
    ): GeocoderClient {
        return when (AppConfig.GEOCODER_MODE) {
            AppConfig.GeocoderMode.FAKE -> com.watsoo.addressconverter.geocode.FakeGeocoderClient()
            AppConfig.GeocoderMode.ANDROID_GEOCODER -> com.watsoo.addressconverter.geocode.AndroidGeocoderClient(context)
        }
    }
}
