package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BankDepositEntity
import com.example.data.model.BankDepositRecordEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Bank Deposit transactions and dedicated Bank Deposit tab records.
 */
@Dao
interface BankDepositDao {

    // --- Legacy / General Bank Deposits ---
    @Query("SELECT * FROM bank_deposits ORDER BY date DESC")
    fun getAllBankDeposits(): Flow<List<BankDepositEntity>>

    @Query("SELECT * FROM bank_deposits WHERE date >= :startTime AND date <= :endTime ORDER BY date DESC")
    fun getBankDepositsBetween(startTime: Long, endTime: Long): Flow<List<BankDepositEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBankDeposit(deposit: BankDepositEntity): Long

    @Update
    suspend fun updateBankDeposit(deposit: BankDepositEntity)

    @Delete
    suspend fun deleteBankDeposit(deposit: BankDepositEntity)

    // --- Dedicated Bank Deposit Tab Records ---
    @Query("SELECT * FROM bank_deposit_records ORDER BY transactionDate DESC, id DESC")
    fun getAllBankDepositRecords(): Flow<List<BankDepositRecordEntity>>

    @Query("SELECT * FROM bank_deposit_records WHERE shopId = :shopId ORDER BY transactionDate DESC, id DESC")
    fun getBankDepositRecordsForShop(shopId: String): Flow<List<BankDepositRecordEntity>>

    @Query("SELECT * FROM bank_deposit_records WHERE shopId = :shopId AND transactionDate >= :startTime AND transactionDate <= :endTime ORDER BY transactionDate DESC")
    fun getBankDepositRecordsForShopBetween(shopId: String, startTime: Long, endTime: Long): Flow<List<BankDepositRecordEntity>>

    @Query("SELECT * FROM bank_deposit_records WHERE id = :id LIMIT 1")
    suspend fun getBankDepositRecordById(id: Long): BankDepositRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBankDepositRecord(record: BankDepositRecordEntity): Long

    @Update
    suspend fun updateBankDepositRecord(record: BankDepositRecordEntity)

    @Delete
    suspend fun deleteBankDepositRecord(record: BankDepositRecordEntity)

    @Query("SELECT COUNT(*) FROM bank_deposit_records")
    suspend fun getBankDepositRecordCount(): Int
}
