package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.SaleEntity
import com.example.data.model.SalesRecordEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Sales transactions and dedicated Sales tab records.
 */
@Dao
interface SalesDao {

    // --- Legacy / General Sales ---
    @Query("SELECT * FROM sales ORDER BY date DESC")
    fun getAllSales(): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE date >= :startTime AND date <= :endTime ORDER BY date DESC")
    fun getSalesBetween(startTime: Long, endTime: Long): Flow<List<SaleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: SaleEntity): Long

    @Update
    suspend fun updateSale(sale: SaleEntity)

    @Delete
    suspend fun deleteSale(sale: SaleEntity)

    @Query("SELECT COUNT(*) FROM sales")
    suspend fun getSalesCount(): Int

    // --- Dedicated Sales Tab Records ---
    @Query("SELECT * FROM sales_records ORDER BY transactionDate DESC, slNo DESC")
    fun getAllSalesRecords(): Flow<List<SalesRecordEntity>>

    @Query("SELECT * FROM sales_records WHERE shopId = :shopId ORDER BY transactionDate DESC, slNo DESC")
    fun getSalesRecordsForShop(shopId: String): Flow<List<SalesRecordEntity>>

    @Query("SELECT * FROM sales_records WHERE shopId = :shopId AND transactionDate >= :startTime AND transactionDate <= :endTime ORDER BY transactionDate DESC")
    fun getSalesRecordsForShopBetween(shopId: String, startTime: Long, endTime: Long): Flow<List<SalesRecordEntity>>

    @Query("SELECT * FROM sales_records WHERE id = :id LIMIT 1")
    suspend fun getSalesRecordById(id: Long): SalesRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSalesRecord(record: SalesRecordEntity): Long

    @Update
    suspend fun updateSalesRecord(record: SalesRecordEntity)

    @Delete
    suspend fun deleteSalesRecord(record: SalesRecordEntity)

    @Query("SELECT COUNT(*) FROM sales_records")
    suspend fun getSalesRecordCount(): Int
}
