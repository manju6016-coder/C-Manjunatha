package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.PurchaseEntity
import com.example.data.model.PurchaseRecordEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Purchase transactions and dedicated Purchase tab records.
 */
@Dao
interface PurchaseDao {

    // --- Legacy / General Purchases ---
    @Query("SELECT * FROM purchases ORDER BY date DESC")
    fun getAllPurchases(): Flow<List<PurchaseEntity>>

    @Query("SELECT * FROM purchases WHERE date >= :startTime AND date <= :endTime ORDER BY date DESC")
    fun getPurchasesBetween(startTime: Long, endTime: Long): Flow<List<PurchaseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchase(purchase: PurchaseEntity): Long

    @Update
    suspend fun updatePurchase(purchase: PurchaseEntity)

    @Delete
    suspend fun deletePurchase(purchase: PurchaseEntity)

    // --- Dedicated Purchase Tab Records ---
    @Query("SELECT * FROM purchase_records ORDER BY transactionDate DESC, slNo DESC")
    fun getAllPurchaseRecords(): Flow<List<PurchaseRecordEntity>>

    @Query("SELECT * FROM purchase_records WHERE shopId = :shopId ORDER BY transactionDate DESC, slNo DESC")
    fun getPurchaseRecordsForShop(shopId: String): Flow<List<PurchaseRecordEntity>>

    @Query("SELECT * FROM purchase_records WHERE shopId = :shopId AND transactionDate >= :startTime AND transactionDate <= :endTime ORDER BY transactionDate DESC")
    fun getPurchaseRecordsForShopBetween(shopId: String, startTime: Long, endTime: Long): Flow<List<PurchaseRecordEntity>>

    @Query("SELECT * FROM purchase_records WHERE id = :id LIMIT 1")
    suspend fun getPurchaseRecordById(id: Long): PurchaseRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchaseRecord(record: PurchaseRecordEntity): Long

    @Update
    suspend fun updatePurchaseRecord(record: PurchaseRecordEntity)

    @Delete
    suspend fun deletePurchaseRecord(record: PurchaseRecordEntity)

    @Query("SELECT COUNT(*) FROM purchase_records")
    suspend fun getPurchaseRecordCount(): Int
}
