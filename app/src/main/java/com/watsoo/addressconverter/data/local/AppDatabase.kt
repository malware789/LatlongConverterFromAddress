package com.watsoo.addressconverter.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        AddressEntity::class,
        WorkerLogEntity::class,
        ExcelImportJobEntity::class,
        ExcelAddressCellEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun addressDao(): AddressDao
    abstract fun workerLogDao(): WorkerLogDao
    abstract fun excelDao(): ExcelDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `worker_logs` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `level` TEXT NOT NULL,
                        `message` TEXT NOT NULL
                    )"""
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `excel_import_jobs` (`jobId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `fileName` TEXT NOT NULL, `importedAt` INTEGER NOT NULL, `status` TEXT NOT NULL, `totalAddressCells` INTEGER NOT NULL, `convertedCount` INTEGER NOT NULL, `failedCount` INTEGER NOT NULL, `inputFileUri` TEXT, `outputFileUri` TEXT)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `excel_address_cells` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `jobId` INTEGER NOT NULL, `sheetName` TEXT NOT NULL, `rowIndex` INTEGER NOT NULL, `columnIndex` INTEGER NOT NULL, `dayName` TEXT NOT NULL, `tankerNo` TEXT NOT NULL, `fpl` TEXT NOT NULL, `address` TEXT NOT NULL, `normalizedAddress` TEXT NOT NULL, `latitude` REAL, `longitude` REAL, `latLongText` TEXT, `status` TEXT NOT NULL, `attempts` INTEGER NOT NULL, `nextRetryAt` INTEGER NOT NULL, `lastError` TEXT, `updatedAt` INTEGER NOT NULL)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_excel_address_cells_jobId` ON `excel_address_cells` (`jobId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_excel_address_cells_status` ON `excel_address_cells` (`status`)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "address_converter_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
