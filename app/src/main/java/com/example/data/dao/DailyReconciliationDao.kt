package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.DailyReconciliationEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Daily Reconciliations and Audits.
 */
@Dao
interface DailyReconciliationDao {

    @Query("SELECT * FROM daily_reconciliations ORDER BY dateEpochStart DESC")
    fun getAllReconciliations(): Flow<List<DailyReconciliationEntity>>

    @Query("SELECT * FROM daily_reconciliations WHERE dateEpochStart = :dateEpochStart LIMIT 1")
    fun getReconciliationForDay(dateEpochStart: Long): Flow<DailyReconciliationEntity?>

    @Query("SELECT * FROM daily_reconciliations WHERE dateEpochStart = :dateEpochStart LIMIT 1")
    suspend fun getReconciliationForDaySync(dateEpochStart: Long): DailyReconciliationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateReconciliation(recon: DailyReconciliationEntity): Long

    @Delete
    suspend fun deleteReconciliation(recon: DailyReconciliationEntity)
}
