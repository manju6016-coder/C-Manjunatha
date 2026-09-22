package com.example.data.repository

import com.example.data.model.BankDepositEntity
import com.example.data.model.BankDepositRecordEntity
import com.example.data.model.DailyReconciliationEntity
import com.example.data.model.OutletTransactionEntity
import com.example.data.model.PurchaseEntity
import com.example.data.model.PurchaseRecordEntity
import com.example.data.model.SaleEntity
import com.example.data.model.SalesRecordEntity
import com.example.data.model.ShopEntity
import com.example.data.model.UserEntity
import kotlinx.coroutines.flow.Flow

/**
 * Interface defining the Repository Layer for the Ledger application.
 *
 * It completely abstracts Room DAOs and the SQLite persistence layer from ViewModels
 * and the UI layer, exposing reactive [Flow] data streams and suspend functions.
 */
interface ILedgerRepository {

    // ==========================================
    // --- 1. General & Legacy Ledger Streams ---
    // ==========================================
    val allPurchases: Flow<List<PurchaseEntity>>
    val allSales: Flow<List<SaleEntity>>
    val allDeposits: Flow<List<BankDepositEntity>>
    val allReconciliations: Flow<List<DailyReconciliationEntity>>

    fun getPurchasesForRange(startTime: Long, endTime: Long): Flow<List<PurchaseEntity>>
    fun getSalesForRange(startTime: Long, endTime: Long): Flow<List<SaleEntity>>
    fun getDepositsForRange(startTime: Long, endTime: Long): Flow<List<BankDepositEntity>>
    fun getReconciliationForDay(dateEpochStart: Long): Flow<DailyReconciliationEntity?>

    suspend fun insertPurchase(purchase: PurchaseEntity): Long
    suspend fun updatePurchase(purchase: PurchaseEntity)
    suspend fun deletePurchase(purchase: PurchaseEntity)

    suspend fun insertSale(sale: SaleEntity): Long
    suspend fun updateSale(sale: SaleEntity)
    suspend fun deleteSale(sale: SaleEntity)

    suspend fun insertDeposit(deposit: BankDepositEntity): Long
    suspend fun updateDeposit(deposit: BankDepositEntity)
    suspend fun deleteDeposit(deposit: BankDepositEntity)

    suspend fun getReconciliationForDaySync(dateEpochStart: Long): DailyReconciliationEntity?
    suspend fun saveReconciliation(recon: DailyReconciliationEntity): Long
    suspend fun deleteReconciliation(recon: DailyReconciliationEntity)

    // ==========================================
    // --- 2. Users & Authentication ---
    // ==========================================
    val allUsers: Flow<List<UserEntity>>

    suspend fun login(outletCode: String, mobileNumber: String): UserEntity?
    suspend fun getUserByOutletCode(outletCode: String): UserEntity?
    suspend fun getUserByMobile(mobileNumber: String): UserEntity?
    suspend fun addUser(user: UserEntity): Long
    suspend fun updateUser(user: UserEntity)
    suspend fun updateUserMobileAndPassword(userId: Long, mobileNumber: String, password: String)
    suspend fun updateUserPassword(userId: Long, password: String)
    suspend fun updateUserPasswordByOutletCode(outletCode: String, password: String)
    suspend fun deleteUser(user: UserEntity)

    // ==========================================
    // --- 3. Shops / Outlets ---
    // ==========================================
    val allShops: Flow<List<ShopEntity>>

    suspend fun getShopByOutletCode(outletCode: String): ShopEntity?
    suspend fun addShop(shop: ShopEntity): Long
    suspend fun updateShop(shop: ShopEntity)
    suspend fun deleteShop(shop: ShopEntity)
    suspend fun reassignRicInShops(oldRicCode: String, newRicCode: String)
    suspend fun reassignEmployeeInShops(oldEmpCode: String, newEmpCode: String)

    // ==========================================
    // --- 4. Daily Outlet Transactions ---
    // ==========================================
    val allOutletTransactions: Flow<List<OutletTransactionEntity>>

    fun getTransactionsForShop(shopOutletCode: String): Flow<List<OutletTransactionEntity>>
    fun getTransactionsForShopBetween(shopOutletCode: String, startTime: Long, endTime: Long): Flow<List<OutletTransactionEntity>>

    suspend fun getOutletTransactionById(id: Long): OutletTransactionEntity?
    suspend fun getLatestTransactionForShop(shopOutletCode: String): OutletTransactionEntity?
    suspend fun getPreviousClosingBalance(shopOutletCode: String, transactionDate: Long): Long?
    suspend fun getNextSlNo(shopOutletCode: String): Long

    suspend fun insertOutletTransaction(tx: OutletTransactionEntity): Long
    suspend fun updateOutletTransaction(tx: OutletTransactionEntity)
    suspend fun deleteOutletTransaction(tx: OutletTransactionEntity)
    suspend fun updateDamage(id: Long, damage: Long, newClosingBalance: Long)

    // SQLite Sync Streams & Methods
    val totalTransactionsCount: Flow<Int>
    val pendingTransactionsCount: Flow<Int>
    val syncedTransactionsCount: Flow<Int>
    val pendingTransactionsFlow: Flow<List<OutletTransactionEntity>>
    suspend fun getPendingTransactions(): List<OutletTransactionEntity>
    suspend fun markTransactionsAsSynced(ids: List<Long>, syncedAt: Long)
    suspend fun markAllTransactionsAsSynced(syncedAt: Long)

    // ==========================================
    // --- 5. Dedicated Purchase Tab Records ---
    // ==========================================
    val allPurchaseRecords: Flow<List<PurchaseRecordEntity>>

    fun getPurchaseRecordsForShop(shopId: String): Flow<List<PurchaseRecordEntity>>
    fun getPurchaseRecordsForShopBetween(shopId: String, startTime: Long, endTime: Long): Flow<List<PurchaseRecordEntity>>
    suspend fun getPurchaseRecordById(id: Long): PurchaseRecordEntity?
    suspend fun insertPurchaseRecord(record: PurchaseRecordEntity): Long
    suspend fun updatePurchaseRecord(record: PurchaseRecordEntity)
    suspend fun deletePurchaseRecord(record: PurchaseRecordEntity)

    // ==========================================
    // --- 6. Dedicated Sales Tab Records ---
    // ==========================================
    val allSalesRecords: Flow<List<SalesRecordEntity>>

    fun getSalesRecordsForShop(shopId: String): Flow<List<SalesRecordEntity>>
    fun getSalesRecordsForShopBetween(shopId: String, startTime: Long, endTime: Long): Flow<List<SalesRecordEntity>>
    suspend fun getSalesRecordById(id: Long): SalesRecordEntity?
    suspend fun insertSalesRecord(record: SalesRecordEntity): Long
    suspend fun updateSalesRecord(record: SalesRecordEntity)
    suspend fun deleteSalesRecord(record: SalesRecordEntity)

    // ==========================================
    // --- 7. Dedicated Bank Deposit Tab Records ---
    // ==========================================
    val allBankDepositRecords: Flow<List<BankDepositRecordEntity>>

    fun getBankDepositRecordsForShop(shopId: String): Flow<List<BankDepositRecordEntity>>
    fun getBankDepositRecordsForShopBetween(shopId: String, startTime: Long, endTime: Long): Flow<List<BankDepositRecordEntity>>
    suspend fun getBankDepositRecordById(id: Long): BankDepositRecordEntity?
    suspend fun insertBankDepositRecord(record: BankDepositRecordEntity): Long
    suspend fun updateBankDepositRecord(record: BankDepositRecordEntity)
    suspend fun deleteBankDepositRecord(record: BankDepositRecordEntity)

    // ==========================================
    // --- 8. Seeding & Utilities ---
    // ==========================================
    suspend fun seedInitialDataIfEmpty()
}
