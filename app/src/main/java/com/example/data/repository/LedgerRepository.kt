package com.example.data.repository

import com.example.data.dao.BankDepositDao
import com.example.data.dao.DailyReconciliationDao
import com.example.data.dao.LedgerDao
import com.example.data.dao.OutletTransactionDao
import com.example.data.dao.PurchaseDao
import com.example.data.dao.SalesDao
import com.example.data.dao.ShopDao
import com.example.data.dao.UserDao
import com.example.data.database.AppDatabase
import com.example.data.model.BankDepositEntity
import com.example.data.model.BankDepositRecordEntity
import com.example.data.model.DailyReconciliationEntity
import com.example.data.model.DepositSource
import com.example.data.model.DepositStatus
import com.example.data.model.OutletTransactionEntity
import com.example.data.model.PaymentMode
import com.example.data.model.PaymentStatus
import com.example.data.model.PurchaseCategory
import com.example.data.model.PurchaseEntity
import com.example.data.model.PurchaseRecordEntity
import com.example.data.model.ReconciliationStatus
import com.example.data.model.SaleEntity
import com.example.data.model.SalesRecordEntity
import com.example.data.model.ShopEntity
import com.example.data.model.UserEntity
import com.example.util.IndianCurrencyUtils
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class LedgerRepository(
    private val ledgerDao: LedgerDao,
    private val purchaseDao: PurchaseDao = ledgerDao,
    private val salesDao: SalesDao = ledgerDao,
    private val bankDepositDao: BankDepositDao = ledgerDao,
    private val outletTransactionDao: OutletTransactionDao = ledgerDao,
    private val shopDao: ShopDao = ledgerDao,
    private val userDao: UserDao = ledgerDao,
    private val reconciliationDao: DailyReconciliationDao = ledgerDao
) : ILedgerRepository {

    constructor(dao: LedgerDao) : this(
        ledgerDao = dao,
        purchaseDao = dao,
        salesDao = dao,
        bankDepositDao = dao,
        outletTransactionDao = dao,
        shopDao = dao,
        userDao = dao,
        reconciliationDao = dao
    )

    companion object {
        fun fromDatabase(db: AppDatabase): LedgerRepository {
            return LedgerRepository(
                ledgerDao = db.ledgerDao(),
                purchaseDao = db.purchaseDao(),
                salesDao = db.salesDao(),
                bankDepositDao = db.bankDepositDao(),
                outletTransactionDao = db.outletTransactionDao(),
                shopDao = db.shopDao(),
                userDao = db.userDao(),
                reconciliationDao = db.dailyReconciliationDao()
            )
        }
    }

    val dao: LedgerDao get() = ledgerDao

    override val allPurchases: Flow<List<PurchaseEntity>> = purchaseDao.getAllPurchases()
    override val allSales: Flow<List<SaleEntity>> = salesDao.getAllSales()
    override val allDeposits: Flow<List<BankDepositEntity>> = bankDepositDao.getAllBankDeposits()
    override val allReconciliations: Flow<List<DailyReconciliationEntity>> = reconciliationDao.getAllReconciliations()

    // --- Users & Authentication ---
    override val allUsers: Flow<List<UserEntity>> = userDao.getAllUsers()
    override suspend fun login(outletCode: String, mobileNumber: String): UserEntity? {
        val user = userDao.getUserByOutletCode(outletCode.trim()) ?: return null
        val entered = mobileNumber.trim()
        val isValid = when {
            user.password.isNotBlank() -> user.password == entered || user.mobileNumber == entered
            else -> user.mobileNumber == entered
        }
        return if (isValid) user else null
    }
    override suspend fun getUserByOutletCode(outletCode: String): UserEntity? =
        userDao.getUserByOutletCode(outletCode.trim())

    override suspend fun getUserByMobile(mobileNumber: String): UserEntity? {
        val targetDigits = mobileNumber.filter { it.isDigit() }.takeLast(10)
        if (targetDigits.isEmpty()) return null
        val users = userDao.getAllUsersList()
        return users.find { user ->
            val userDigits = user.mobileNumber.filter { it.isDigit() }.takeLast(10)
            userDigits == targetDigits
        }
    }

    override suspend fun addUser(user: UserEntity): Long = userDao.insertUser(user)
    override suspend fun updateUser(user: UserEntity) = userDao.updateUser(user)
    override suspend fun updateUserMobileAndPassword(userId: Long, mobileNumber: String, password: String) {
        userDao.updateUserMobileAndPassword(userId, mobileNumber.trim(), password.trim())
    }
    override suspend fun updateUserPassword(userId: Long, password: String) =
        userDao.updateUserPassword(userId, password.trim())
    override suspend fun updateUserPasswordByOutletCode(outletCode: String, password: String) =
        userDao.updateUserPasswordByOutletCode(outletCode.trim(), password.trim())
    override suspend fun deleteUser(user: UserEntity) = userDao.deleteUser(user)

    // --- Shops ---
    override val allShops: Flow<List<ShopEntity>> = shopDao.getAllShops()
    override suspend fun getShopByOutletCode(outletCode: String): ShopEntity? =
        shopDao.getShopByOutletCode(outletCode.trim())
    override suspend fun addShop(shop: ShopEntity): Long = shopDao.insertShop(shop)
    override suspend fun updateShop(shop: ShopEntity) = shopDao.updateShop(shop)
    override suspend fun deleteShop(shop: ShopEntity) = shopDao.deleteShop(shop)
    override suspend fun reassignRicInShops(oldRicCode: String, newRicCode: String) =
        shopDao.reassignRicInShops(oldRicCode.trim(), newRicCode.trim())
    override suspend fun reassignEmployeeInShops(oldEmpCode: String, newEmpCode: String) =
        shopDao.reassignEmployeeInShops(oldEmpCode.trim(), newEmpCode.trim())

    // --- Outlet Transactions ---
    override val allOutletTransactions: Flow<List<OutletTransactionEntity>> =
        outletTransactionDao.getAllOutletTransactions()
    override fun getTransactionsForShop(shopOutletCode: String): Flow<List<OutletTransactionEntity>> =
        outletTransactionDao.getTransactionsForShop(shopOutletCode)

    override fun getTransactionsForShopBetween(shopOutletCode: String, startTime: Long, endTime: Long): Flow<List<OutletTransactionEntity>> =
        outletTransactionDao.getTransactionsForShopBetween(shopOutletCode, startTime, endTime)

    override suspend fun getOutletTransactionById(id: Long): OutletTransactionEntity? =
        outletTransactionDao.getOutletTransactionById(id)

    override suspend fun getLatestTransactionForShop(shopOutletCode: String): OutletTransactionEntity? =
        outletTransactionDao.getLatestTransactionForShop(shopOutletCode)

    override suspend fun getPreviousClosingBalance(shopOutletCode: String, transactionDate: Long): Long? {
        val prev = outletTransactionDao.getLatestTransactionBeforeDate(shopOutletCode, transactionDate)
        return prev?.closingBalance
    }

    override suspend fun getNextSlNo(shopOutletCode: String): Long {
        val max = outletTransactionDao.getMaxSlNoForShop(shopOutletCode) ?: 0L
        return max + 1L
    }

    override suspend fun insertOutletTransaction(tx: OutletTransactionEntity): Long {
        val id = outletTransactionDao.insertOutletTransaction(tx)
        // Also persist to dedicated Purchase, Sales, and Bank Deposit records
        purchaseDao.insertPurchaseRecord(tx.toPurchaseRecord())
        salesDao.insertSalesRecord(tx.toSalesRecord())
        bankDepositDao.insertBankDepositRecord(tx.toBankDepositRecord())
        return id
    }

    override suspend fun updateOutletTransaction(tx: OutletTransactionEntity) {
        outletTransactionDao.updateOutletTransaction(tx)
        purchaseDao.insertPurchaseRecord(tx.toPurchaseRecord())
        salesDao.insertSalesRecord(tx.toSalesRecord())
        bankDepositDao.insertBankDepositRecord(tx.toBankDepositRecord())
    }

    override suspend fun deleteOutletTransaction(tx: OutletTransactionEntity) =
        outletTransactionDao.deleteOutletTransaction(tx)

    override suspend fun updateDamage(id: Long, damage: Long, newClosingBalance: Long) =
        outletTransactionDao.updateDamage(id, damage, newClosingBalance)

    // ==========================================
    // --- SQLite Sync Methods ---
    // ==========================================
    override val totalTransactionsCount: Flow<Int> =
        outletTransactionDao.getTotalTransactionsCountFlow()

    override val pendingTransactionsCount: Flow<Int> =
        outletTransactionDao.getPendingTransactionsCountFlow()

    override val syncedTransactionsCount: Flow<Int> =
        outletTransactionDao.getSyncedTransactionsCountFlow()

    override val pendingTransactionsFlow: Flow<List<OutletTransactionEntity>> =
        outletTransactionDao.getPendingTransactionsFlow()

    override suspend fun getPendingTransactions(): List<OutletTransactionEntity> =
        outletTransactionDao.getPendingTransactions()

    override suspend fun markTransactionsAsSynced(ids: List<Long>, syncedAt: Long) =
        outletTransactionDao.markTransactionsAsSynced(ids, syncedAt)

    override suspend fun markAllTransactionsAsSynced(syncedAt: Long) =
        outletTransactionDao.markAllTransactionsAsSynced(syncedAt)

    // ==========================================
    // --- Dedicated Records Repository ---
    // ==========================================
    override val allPurchaseRecords: Flow<List<PurchaseRecordEntity>> =
        purchaseDao.getAllPurchaseRecords()
    override fun getPurchaseRecordsForShop(shopId: String): Flow<List<PurchaseRecordEntity>> =
        purchaseDao.getPurchaseRecordsForShop(shopId)
    override fun getPurchaseRecordsForShopBetween(shopId: String, startTime: Long, endTime: Long): Flow<List<PurchaseRecordEntity>> =
        purchaseDao.getPurchaseRecordsForShopBetween(shopId, startTime, endTime)
    override suspend fun getPurchaseRecordById(id: Long): PurchaseRecordEntity? =
        purchaseDao.getPurchaseRecordById(id)
    override suspend fun insertPurchaseRecord(record: PurchaseRecordEntity): Long =
        purchaseDao.insertPurchaseRecord(record)
    override suspend fun updatePurchaseRecord(record: PurchaseRecordEntity) =
        purchaseDao.updatePurchaseRecord(record)
    override suspend fun deletePurchaseRecord(record: PurchaseRecordEntity) =
        purchaseDao.deletePurchaseRecord(record)

    override val allSalesRecords: Flow<List<SalesRecordEntity>> =
        salesDao.getAllSalesRecords()
    override fun getSalesRecordsForShop(shopId: String): Flow<List<SalesRecordEntity>> =
        salesDao.getSalesRecordsForShop(shopId)
    override fun getSalesRecordsForShopBetween(shopId: String, startTime: Long, endTime: Long): Flow<List<SalesRecordEntity>> =
        salesDao.getSalesRecordsForShopBetween(shopId, startTime, endTime)
    override suspend fun getSalesRecordById(id: Long): SalesRecordEntity? =
        salesDao.getSalesRecordById(id)
    override suspend fun insertSalesRecord(record: SalesRecordEntity): Long =
        salesDao.insertSalesRecord(record)
    override suspend fun updateSalesRecord(record: SalesRecordEntity) =
        salesDao.updateSalesRecord(record)
    override suspend fun deleteSalesRecord(record: SalesRecordEntity) =
        salesDao.deleteSalesRecord(record)

    override val allBankDepositRecords: Flow<List<BankDepositRecordEntity>> =
        bankDepositDao.getAllBankDepositRecords()
    override fun getBankDepositRecordsForShop(shopId: String): Flow<List<BankDepositRecordEntity>> =
        bankDepositDao.getBankDepositRecordsForShop(shopId)
    override fun getBankDepositRecordsForShopBetween(shopId: String, startTime: Long, endTime: Long): Flow<List<BankDepositRecordEntity>> =
        bankDepositDao.getBankDepositRecordsForShopBetween(shopId, startTime, endTime)
    override suspend fun getBankDepositRecordById(id: Long): BankDepositRecordEntity? =
        bankDepositDao.getBankDepositRecordById(id)
    override suspend fun insertBankDepositRecord(record: BankDepositRecordEntity): Long =
        bankDepositDao.insertBankDepositRecord(record)
    override suspend fun updateBankDepositRecord(record: BankDepositRecordEntity) =
        bankDepositDao.updateBankDepositRecord(record)
    override suspend fun deleteBankDepositRecord(record: BankDepositRecordEntity) =
        bankDepositDao.deleteBankDepositRecord(record)

    override fun getPurchasesForRange(startTime: Long, endTime: Long): Flow<List<PurchaseEntity>> =
        purchaseDao.getPurchasesBetween(startTime, endTime)

    override fun getSalesForRange(startTime: Long, endTime: Long): Flow<List<SaleEntity>> =
        salesDao.getSalesBetween(startTime, endTime)

    override fun getDepositsForRange(startTime: Long, endTime: Long): Flow<List<BankDepositEntity>> =
        bankDepositDao.getBankDepositsBetween(startTime, endTime)

    override fun getReconciliationForDay(dateEpochStart: Long): Flow<DailyReconciliationEntity?> =
        reconciliationDao.getReconciliationForDay(dateEpochStart)

    override suspend fun getReconciliationForDaySync(dateEpochStart: Long): DailyReconciliationEntity? =
        reconciliationDao.getReconciliationForDaySync(dateEpochStart)

    // --- Write Operations ---
    override suspend fun insertPurchase(purchase: PurchaseEntity): Long = purchaseDao.insertPurchase(purchase)
    override suspend fun updatePurchase(purchase: PurchaseEntity) = purchaseDao.updatePurchase(purchase)
    override suspend fun deletePurchase(purchase: PurchaseEntity) = purchaseDao.deletePurchase(purchase)

    override suspend fun insertSale(sale: SaleEntity): Long = salesDao.insertSale(sale)
    override suspend fun updateSale(sale: SaleEntity) = salesDao.updateSale(sale)
    override suspend fun deleteSale(sale: SaleEntity) = salesDao.deleteSale(sale)

    override suspend fun insertDeposit(deposit: BankDepositEntity): Long = bankDepositDao.insertBankDeposit(deposit)
    override suspend fun updateDeposit(deposit: BankDepositEntity) = bankDepositDao.updateBankDeposit(deposit)
    override suspend fun deleteDeposit(deposit: BankDepositEntity) = bankDepositDao.deleteBankDeposit(deposit)

    override suspend fun saveReconciliation(recon: DailyReconciliationEntity): Long =
        reconciliationDao.insertOrUpdateReconciliation(recon)

    override suspend fun deleteReconciliation(recon: DailyReconciliationEntity) =
        reconciliationDao.deleteReconciliation(recon)

    /**
     * Seeds initial realistic Indian business transactions if the database is fresh/empty.
     */
    override suspend fun seedInitialDataIfEmpty() {
        if (dao.getSalesCount() > 0) return

        val now = System.currentTimeMillis()
        val todayStart = IndianCurrencyUtils.getStartOfDay(now)

        // Today's Sales
        dao.insertSale(
            SaleEntity(
                date = todayStart + (9 * 3600 * 1000) + (30 * 60 * 1000), // 09:30 AM
                customerName = "Walk-in Retail Customer",
                invoiceNo = "BILL-1001",
                itemsSummary = "2x Silk Sarees, 1x Kurta Set",
                amount = 7450.00,
                gstRate = 5.0,
                paymentMode = PaymentMode.CASH.name,
                paymentStatus = PaymentStatus.PAID.name,
                notes = "Morning opening sale"
            )
        )
        dao.insertSale(
            SaleEntity(
                date = todayStart + (11 * 3600 * 1000) + (15 * 60 * 1000), // 11:15 AM
                customerName = "Rahul Sharma",
                invoiceNo = "BILL-1002",
                itemsSummary = "Cotton Bedlinen & Drapes",
                amount = 4800.00,
                gstRate = 12.0,
                paymentMode = PaymentMode.UPI.name,
                paymentStatus = PaymentStatus.PAID.name,
                notes = "PhonePe QR scan at counter"
            )
        )
        dao.insertSale(
            SaleEntity(
                date = todayStart + (13 * 3600 * 1000), // 01:00 PM
                customerName = "Priya Enterprises",
                invoiceNo = "BILL-1003",
                itemsSummary = "Bulk Fabric Rolls (100m)",
                amount = 28500.00,
                gstRate = 5.0,
                paymentMode = PaymentMode.BANK_TRANSFER.name,
                paymentStatus = PaymentStatus.PAID.name,
                notes = "NEFT direct account transfer"
            )
        )
        dao.insertSale(
            SaleEntity(
                date = todayStart + (15 * 3600 * 1000) + (45 * 60 * 1000), // 03:45 PM
                customerName = "Amit Verma",
                invoiceNo = "BILL-1004",
                itemsSummary = "3x Designer Kurtis",
                amount = 5200.00,
                gstRate = 5.0,
                paymentMode = PaymentMode.CASH.name,
                paymentStatus = PaymentStatus.PAID.name,
                notes = "Cash counter"
            )
        )
        dao.insertSale(
            SaleEntity(
                date = todayStart + (17 * 3600 * 1000) + (10 * 60 * 1000), // 05:10 PM
                customerName = "Sunita Rao",
                invoiceNo = "BILL-1005",
                itemsSummary = "Embroidered Dupattas",
                amount = 3600.00,
                gstRate = 5.0,
                paymentMode = PaymentMode.CARD.name,
                paymentStatus = PaymentStatus.PAID.name,
                notes = "HDFC POS machine swipe"
            )
        )

        // Today's Purchases
        dao.insertPurchase(
            PurchaseEntity(
                date = todayStart + (10 * 3600 * 1000), // 10:00 AM
                vendorName = "Surat Textiles Syndicate",
                invoiceNo = "INV-ST-482",
                category = PurchaseCategory.INVENTORY.label,
                amount = 18500.00,
                gstRate = 5.0,
                paymentMode = PaymentMode.BANK_TRANSFER.name,
                paymentStatus = PaymentStatus.PAID.name,
                notes = "Direct RTGS supplier payment"
            )
        )
        dao.insertPurchase(
            PurchaseEntity(
                date = todayStart + (12 * 3600 * 1000) + (30 * 60 * 1000), // 12:30 PM
                vendorName = "City Logistics & Courier",
                invoiceNo = "LR-9921",
                category = PurchaseCategory.LOGISTICS.label,
                amount = 1250.00,
                gstRate = 18.0,
                paymentMode = PaymentMode.CASH.name,
                paymentStatus = PaymentStatus.PAID.name,
                notes = "Paid cash on delivery for consignment parcel"
            )
        )
        dao.insertPurchase(
            PurchaseEntity(
                date = todayStart + (14 * 3600 * 1000), // 02:00 PM
                vendorName = "Modern Packaging Co",
                invoiceNo = "PKG-2041",
                category = PurchaseCategory.STORE_EXPENSES.label,
                amount = 3400.00,
                gstRate = 12.0,
                paymentMode = PaymentMode.UPI.name,
                paymentStatus = PaymentStatus.PAID.name,
                notes = "Carry bags & cartons"
            )
        )

        // Today's Bank Deposit
        dao.insertBankDeposit(
            BankDepositEntity(
                date = todayStart + (14 * 3600 * 1000) + (30 * 60 * 1000), // 02:30 PM
                bankName = "State Bank of India",
                accountNumberLast4 = "4920",
                amount = 8000.00,
                depositSource = DepositSource.CASH_COUNTER.name,
                referenceNumber = "SBI-CDM-88401",
                denominationBreakdown = "₹500 x 16",
                status = DepositStatus.CLEARED.name,
                notes = "Mid-day cash deposit at branch CDM machine"
            )
        )

        // Yesterday's Reconciled Record for continuity
        val yesterdayStart = todayStart - (24 * 3600 * 1000)
        dao.insertOrUpdateReconciliation(
            DailyReconciliationEntity(
                dateEpochStart = yesterdayStart,
                openingCash = 5000.00,
                totalCashSales = 15200.00,
                totalDigitalSales = 22400.00,
                totalCashPurchases = 2200.00,
                totalBankPurchases = 14000.00,
                totalCashDepositedToBank = 12000.00,
                expectedClosingCash = 6000.00, // 5000 + 15200 - 2200 - 12000
                actualCountedCash = 6000.00,
                discrepancy = 0.0,
                expectedBankInflow = 34400.00,
                status = ReconciliationStatus.BALANCED.name,
                denominationTallyJson = "₹500x10, ₹200x4, ₹100x2",
                reconciledAt = yesterdayStart + (21 * 3600 * 1000),
                auditorName = "Store Manager",
                notes = "Closed evening cash register. All tallies matched 100%."
            )
        )

        // Seed Users
        if (dao.getUsersCount() == 0) {
            dao.insertUser(
                com.example.data.model.UserEntity(
                    outletCode = "ADMIN",
                    name = "Central Administrator",
                    mobileNumber = "8686122299",
                    password = "admin",
                    role = com.example.data.model.UserRole.ADMIN.name,
                    assignedShopCodes = "*"
                )
            )
            dao.insertUser(
                com.example.data.model.UserEntity(
                    outletCode = "RIC01",
                    name = "Vikram Singh (RIC)",
                    mobileNumber = "9876543211",
                    password = "ric123",
                    role = com.example.data.model.UserRole.RIC.name,
                    assignedShopCodes = "OUT-101,OUT-102"
                )
            )
            dao.insertUser(
                com.example.data.model.UserEntity(
                    outletCode = "EMP101",
                    name = "Deepak Kumar (Cashier)",
                    mobileNumber = "9876543212",
                    password = "emp123",
                    role = com.example.data.model.UserRole.SHOP_EMPLOYEE.name,
                    assignedShopCodes = "OUT-101"
                )
            )
            dao.insertUser(
                com.example.data.model.UserEntity(
                    outletCode = "EMP102",
                    name = "Meera Nair (Cashier)",
                    mobileNumber = "9876543213",
                    password = "emp123",
                    role = com.example.data.model.UserRole.SHOP_EMPLOYEE.name,
                    assignedShopCodes = "OUT-102"
                )
            )
        } else {
            // Ensure Admin mobile number is updated to +91 8686122299 if previously seeded with old number
            val adminUser = dao.getUserByOutletCode("ADMIN")
            if (adminUser != null && !adminUser.mobileNumber.contains("8686122299")) {
                dao.updateUser(adminUser.copy(mobileNumber = "8686122299"))
            }
        }

        // Seed Shops
        if (dao.getShopsCount() == 0) {
            dao.insertShop(
                com.example.data.model.ShopEntity(
                    outletCode = "OUT-101",
                    shopName = "Metro Central Retail",
                    address = "Brigade Road, Commercial Hub",
                    assignedRicCode = "RIC01",
                    assignedEmployeeCodes = "EMP101",
                    initialOpeningBalance = 25000L
                )
            )
            dao.insertShop(
                com.example.data.model.ShopEntity(
                    outletCode = "OUT-102",
                    shopName = "Suburban Express Outlet",
                    address = "Sector 14, Main Market",
                    assignedRicCode = "RIC01",
                    assignedEmployeeCodes = "EMP102",
                    initialOpeningBalance = 18000L
                )
            )
        }

        // Seed Outlet Transactions
        if (dao.getOutletTransactionsCount() == 0) {
            // OUT-101 Day 1 (Yesterday)
            val yesterdayDepositDate = yesterdayStart + (24 * 3600 * 1000)
            dao.insertOutletTransaction(
                com.example.data.model.OutletTransactionEntity(
                    shopOutletCode = "OUT-101",
                    slNo = 1,
                    transactionDate = yesterdayStart,
                    openingBalance = 25000L,
                    isOpeningBalanceManual = true,
                    purchase = 30000L,
                    margin10 = 3000L, // 30000 * 10%
                    aroed = 2500L, // compulsory with purchase
                    totalValue = 60500L, // 25000 + 30000 + 3000 + 2500
                    cardSales = 12000L,
                    cashSales = 28000L,
                    totalSales = 40000L, // 12000 + 28000
                    damage = 500L, // Admin entry
                    closingBalance = 20000L, // 60500 - 40000 - 500
                    bankDepositDate = yesterdayDepositDate,
                    depositAmount = 25000L,
                    challanPhotoUri = "",
                    notes = "Day 1 regular register closing",
                    submittedBy = "Deepak Kumar",
                    submittedByRole = "SHOP_EMPLOYEE",
                    syncStatus = "SYNCED",
                    syncedAt = System.currentTimeMillis()
                )
            )

            // OUT-101 Day 2 (Today)
            val todayDepositDate = todayStart + (24 * 3600 * 1000)
            dao.insertOutletTransaction(
                com.example.data.model.OutletTransactionEntity(
                    shopOutletCode = "OUT-101",
                    slNo = 2,
                    transactionDate = todayStart,
                    openingBalance = 20000L, // Auto-fetched from Day 1 closing balance
                    isOpeningBalanceManual = false,
                    purchase = 15000L,
                    margin10 = 1500L, // 15000 * 10%
                    aroed = 1200L, // compulsory
                    totalValue = 37700L, // 20000 + 15000 + 1500 + 1200
                    cardSales = 8500L,
                    cashSales = 16500L,
                    totalSales = 25000L, // 8500 + 16500
                    damage = 0L,
                    closingBalance = 12700L, // 37700 - 25000 - 0
                    bankDepositDate = todayDepositDate,
                    depositAmount = 15000L,
                    challanPhotoUri = "",
                    notes = "Mid-week inventory restocked",
                    submittedBy = "Deepak Kumar",
                    submittedByRole = "SHOP_EMPLOYEE",
                    syncStatus = "SYNCED",
                    syncedAt = System.currentTimeMillis()
                )
            )

            // OUT-102 Day 1 (Today)
            dao.insertOutletTransaction(
                com.example.data.model.OutletTransactionEntity(
                    shopOutletCode = "OUT-102",
                    slNo = 1,
                    transactionDate = todayStart,
                    openingBalance = 18000L,
                    isOpeningBalanceManual = true,
                    purchase = 22000L,
                    margin10 = 2200L, // 22000 * 10%
                    aroed = 1800L,
                    totalValue = 44000L, // 18000 + 22000 + 2200 + 1800
                    cardSales = 9500L,
                    cashSales = 19500L,
                    totalSales = 29000L,
                    damage = 0L,
                    closingBalance = 15000L, // 44000 - 29000 - 0
                    bankDepositDate = todayDepositDate,
                    depositAmount = 18000L,
                    challanPhotoUri = "",
                    notes = "Opening day entries verified",
                    submittedBy = "Meera Nair",
                    submittedByRole = "SHOP_EMPLOYEE",
                    syncStatus = "SYNCED",
                    syncedAt = System.currentTimeMillis()
                )
            )
        }

        // Seed dedicated records (PurchaseRecordEntity, SalesRecordEntity, BankDepositRecordEntity)
        if (dao.getPurchaseRecordCount() == 0) {
            val yesterdayDepositDate = yesterdayStart + (24 * 3600 * 1000)
            val todayDepositDate = todayStart + (24 * 3600 * 1000)

            dao.insertPurchaseRecord(
                com.example.data.model.PurchaseRecordEntity(
                    shopId = "OUT-101",
                    transactionDate = yesterdayStart,
                    slNo = 1,
                    openingBalance = 25000L,
                    purchase = 30000L,
                    margin10 = 3000L,
                    aroed = 2500L,
                    totalValue = 60500L,
                    notes = "Day 1 opening inventory"
                )
            )
            dao.insertSalesRecord(
                com.example.data.model.SalesRecordEntity(
                    shopId = "OUT-101",
                    transactionDate = yesterdayStart,
                    slNo = 1,
                    totalValue = 60500L,
                    cardSales = 12000L,
                    cashSales = 28000L,
                    totalSales = 40000L,
                    damage = 500L,
                    closingBalance = 20000L,
                    notes = "Day 1 sales summary"
                )
            )
            dao.insertBankDepositRecord(
                com.example.data.model.BankDepositRecordEntity(
                    shopId = "OUT-101",
                    transactionDate = yesterdayStart,
                    bankDepositDate = yesterdayDepositDate,
                    depositAmount = 25000L,
                    cardSales = 12000L,
                    totalBankDeposit = 37000L,
                    notes = "Challan deposited in SBI branch"
                )
            )

            dao.insertPurchaseRecord(
                com.example.data.model.PurchaseRecordEntity(
                    shopId = "OUT-101",
                    transactionDate = todayStart,
                    slNo = 2,
                    openingBalance = 20000L,
                    purchase = 15000L,
                    margin10 = 1500L,
                    aroed = 1200L,
                    totalValue = 37700L,
                    notes = "Mid-week restock"
                )
            )
            dao.insertSalesRecord(
                com.example.data.model.SalesRecordEntity(
                    shopId = "OUT-101",
                    transactionDate = todayStart,
                    slNo = 2,
                    totalValue = 37700L,
                    cardSales = 8500L,
                    cashSales = 16500L,
                    totalSales = 25000L,
                    damage = 0L,
                    closingBalance = 12700L,
                    notes = "Today's sales summary"
                )
            )
            dao.insertBankDepositRecord(
                com.example.data.model.BankDepositRecordEntity(
                    shopId = "OUT-101",
                    transactionDate = todayStart,
                    bankDepositDate = todayDepositDate,
                    depositAmount = 15000L,
                    cardSales = 8500L,
                    totalBankDeposit = 23500L,
                    notes = "CDM counter deposit"
                )
            )

            dao.insertPurchaseRecord(
                com.example.data.model.PurchaseRecordEntity(
                    shopId = "OUT-102",
                    transactionDate = todayStart,
                    slNo = 1,
                    openingBalance = 18000L,
                    purchase = 22000L,
                    margin10 = 2200L,
                    aroed = 1800L,
                    totalValue = 44000L,
                    notes = "Opening day purchase"
                )
            )
            dao.insertSalesRecord(
                com.example.data.model.SalesRecordEntity(
                    shopId = "OUT-102",
                    transactionDate = todayStart,
                    slNo = 1,
                    totalValue = 44000L,
                    cardSales = 9500L,
                    cashSales = 19500L,
                    totalSales = 29000L,
                    damage = 0L,
                    closingBalance = 15000L,
                    notes = "Opening day sales"
                )
            )
            dao.insertBankDepositRecord(
                com.example.data.model.BankDepositRecordEntity(
                    shopId = "OUT-102",
                    transactionDate = todayStart,
                    bankDepositDate = todayDepositDate,
                    depositAmount = 18000L,
                    cardSales = 9500L,
                    totalBankDeposit = 27500L,
                    notes = "Opening day bank deposit"
                )
            )
        }
    }
}
