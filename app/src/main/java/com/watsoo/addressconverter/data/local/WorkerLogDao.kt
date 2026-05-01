package com.watsoo.addressconverter.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WorkerLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: WorkerLogEntity)

    @Query("SELECT * FROM worker_logs ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentLogs(limit: Int = 100): List<WorkerLogEntity>

    @Query("DELETE FROM worker_logs")
    suspend fun deleteAll()

    @Query("DELETE FROM worker_logs WHERE id NOT IN (SELECT id FROM worker_logs ORDER BY timestamp DESC LIMIT :keepCount)")
    suspend fun trimOldLogs(keepCount: Int = 500)
}
