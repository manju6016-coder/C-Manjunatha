package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.OutletTransactionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Unified Daily Outlet Transactions.
 */
@Dao
interface OutletTransactionDao {

    @Query("SELECT * FROM outlet_transactions ORDER BY transactionDate DESC, slNo DESC")
    fun getAllOutletTransactions(): Flow<List<OutletTransactionEntity>>

    @Query("SELECT * FROM outlet_transactions WHERE shopOutletCode = :shopOutletCode ORDER BY transactionDate DESC, slNo DESC")
    fun getTransactionsForShop(shopOutletCode: String): Flow<List<OutletTransactionEntity>>

    @Query("SELECT * FROM outlet_transactions WHERE shopOutletCode = :shopOutletCode AND transactionDate >= :startTime AND transactionDate <= :endTime ORDER BY transactionDate DESC, slNo DESC")
    fun getTransactionsForShopBetween(shopOutletCode: String, startTime: Long, endTime: Long): Flow<List<OutletTransactionEntity>>

    @Query("SELECT * FROM outlet_transactions WHERE shopOutletCode = :shopOutletCode ORDER BY transactionDate DESC, slNo DESC LIMIT 1")
    suspend fun getLatestTransactionForShop(shopOutletCode: String): OutletTransactionEntity?

    @Query("SELECT * FROM outlet_transactions WHERE shopOutletCode = :shopOutletCode AND transactionDate < :date ORDER BY transactionDate DESC, slNo DESC LIMIT 1")
    suspend fun getLatestTransactionBeforeDate(shopOutletCode: String, date: Long): OutletTransactionEntity?

    @Query("SELECT MAX(slNo) FROM outlet_transactions WHERE shopOutletCode = :shopOutletCode")
    suspend fun getMaxSlNoForShop(shopOutletCode: String): Long?

    @Query("SELECT * FROM outlet_transactions WHERE id = :id LIMIT 1")
    suspend fun getOutletTransactionById(id: Long): OutletTransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOutletTransaction(tx: OutletTransactionEntity): Long

    @Update
    suspend fun updateOutletTransaction(tx: OutletTransactionEntity)

    @Delete
    suspend fun deleteOutletTransaction(tx: OutletTransactionEntity)

    @Query("UPDATE outlet_transactions SET damage = :damage, closingBalance = :closingBalance WHERE id = :id")
    suspend fun updateDamage(id: Long, damage: Long, closingBalance: Long)

    @Query("SELECT COUNT(*) FROM outlet_transactions")
    suspend fun getOutletTransactionsCount(): Int

    @Query("SELECT COUNT(*) FROM outlet_transactions")
    fun getTotalTransactionsCountFlow(): Flow<Int>

    @Query("SELECT * FROM outlet_transactions WHERE syncStatus = 'PENDING' ORDER BY transactionDate ASC")
    fun getPendingTransactionsFlow(): Flow<List<OutletTransactionEntity>>

    @Query("SELECT * FROM outlet_transactions WHERE syncStatus = 'PENDING' ORDER BY transactionDate ASC")
    suspend fun getPendingTransactions(): List<OutletTransactionEntity>

    @Query("SELECT COUNT(*) FROM outlet_transactions WHERE syncStatus = 'PENDING'")
    fun getPendingTransactionsCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM outlet_transactions WHERE syncStatus = 'PENDING'")
    suspend fun getPendingTransactionsCount(): Int

    @Query("SELECT COUNT(*) FROM outlet_transactions WHERE syncStatus = 'SYNCED'")
    fun getSyncedTransactionsCountFlow(): Flow<Int>

    @Query("UPDATE outlet_transactions SET syncStatus = 'SYNCED', syncedAt = :syncedAt WHERE id IN (:ids)")
    suspend fun markTransactionsAsSynced(ids: List<Long>, syncedAt: Long)

    @Query("UPDATE outlet_transactions SET syncStatus = 'SYNCED', syncedAt = :syncedAt")
    suspend fun markAllTransactionsAsSynced(syncedAt: Long)

    @Query("UPDATE outlet_transactions SET syncStatus = 'SYNCING' WHERE id IN (:ids)")
    suspend fun markTransactionsAsSyncing(ids: List<Long>)
}
