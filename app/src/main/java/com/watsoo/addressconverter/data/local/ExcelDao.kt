package com.watsoo.addressconverter.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExcelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJob(job: ExcelImportJobEntity): Long

    @Update
    suspend fun updateJob(job: ExcelImportJobEntity)

    @Query("SELECT * FROM excel_import_jobs ORDER BY importedAt DESC")
    fun getAllJobsFlow(): Flow<List<ExcelImportJobEntity>>

    @Query("SELECT * FROM excel_import_jobs WHERE jobId = :jobId")
    suspend fun getJobById(jobId: Long): ExcelImportJobEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCells(cells: List<ExcelAddressCellEntity>)

    @Update
    suspend fun updateCell(cell: ExcelAddressCellEntity)

    @Query("SELECT * FROM excel_address_cells WHERE jobId = :jobId ORDER BY rowIndex, columnIndex")
    suspend fun getCellsByJob(jobId: Long): List<ExcelAddressCellEntity>

    @Query("SELECT * FROM excel_address_cells WHERE status = 'PENDING_GEOCODE' OR status = 'FAILED_TEMP' LIMIT :limit")
    suspend fun getPendingCells(limit: Int): List<ExcelAddressCellEntity>

    @Query("UPDATE excel_address_cells SET status = :status WHERE id = :cellId")
    suspend fun updateCellStatus(cellId: Long, status: ExcelStatus)

    @Query("SELECT COUNT(*) FROM excel_address_cells WHERE jobId = :jobId AND status = :status")
    suspend fun countCellsByStatus(jobId: Long, status: ExcelStatus): Int

    @Query("SELECT COUNT(*) FROM excel_address_cells WHERE jobId = :jobId")
    suspend fun countTotalCells(jobId: Long): Int

    @Query("DELETE FROM excel_address_cells WHERE jobId = :jobId")
    suspend fun deleteCellsByJob(jobId: Long)

    @Query("DELETE FROM excel_import_jobs WHERE jobId = :jobId")
    suspend fun deleteJob(jobId: Long)

    @Transaction
    suspend fun deleteJobAndCells(jobId: Long) {
        deleteCellsByJob(jobId)
        deleteJob(jobId)
    }

    @Query("SELECT * FROM excel_address_cells WHERE jobId = :jobId AND status = 'GEOCODED'")
    suspend fun getGeocodedCells(jobId: Long): List<ExcelAddressCellEntity>

    @Query("UPDATE excel_address_cells SET status = 'PENDING_GEOCODE' WHERE status = 'GEOCODING'")
    suspend fun releaseStaleLocks(): Int
}
