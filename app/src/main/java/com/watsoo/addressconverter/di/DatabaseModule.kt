package com.watsoo.addressconverter.di

import android.util.Log

import android.content.Context
import com.watsoo.addressconverter.data.local.AddressDao
import com.watsoo.addressconverter.data.local.AppDatabase
import com.watsoo.addressconverter.data.local.WorkerLogDao
import com.watsoo.addressconverter.data.local.ExcelDao
import com.watsoo.addressconverter.data.excel.ExcelFileProcessor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        Log.d("HiltModule", "provideDatabase started")
        val db = AppDatabase.getDatabase(context)
        Log.d("HiltModule", "provideDatabase completed (database opened)")
        return db
    }

    @Provides
    fun provideAddressDao(database: AppDatabase): AddressDao {
        return database.addressDao()
    }

    @Provides
    fun provideWorkerLogDao(database: AppDatabase): WorkerLogDao {
        return database.workerLogDao()
    }

    @Provides
    fun provideExcelDao(database: AppDatabase): ExcelDao {
        return database.excelDao()
    }

    @Provides
    @Singleton
    fun provideExcelFileProcessor(@ApplicationContext context: Context): ExcelFileProcessor {
        return ExcelFileProcessor(context)
    }
}
