package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.dao.LedgerDao
import com.example.data.database.AppDatabase
import com.example.data.model.BankDeposit
import com.example.data.model.BankDepositRecordEntity
import com.example.data.model.OutletTransactionEntity
import com.example.data.model.Purchase
import com.example.data.model.PurchaseRecordEntity
import com.example.data.model.Sales
import com.example.data.model.SalesRecordEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RoomDatabaseSchemaTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: LedgerDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.ledgerDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testPurchaseRecordSchemaAndCalculations() = runBlocking {
        val shopId = "OUT-101"
        val txDate = 1700000000000L
        val openingBalance = 25000L
        val purchase = 30000L
        val margin10 = Math.round(purchase * 0.10)
        val aroed = 2500L
        val totalValue = openingBalance + purchase + margin10 + aroed

        val purchaseRecord = PurchaseRecordEntity(
            shopId = shopId,
            transactionDate = txDate,
            slNo = 1L,
            openingBalance = openingBalance,
            purchase = purchase,
            margin10 = margin10,
            aroed = aroed,
            totalValue = totalValue,
            notes = "Initial stock"
        )

        val id = dao.insertPurchaseRecord(purchaseRecord)
        assertTrue(id > 0)

        val records = dao.getPurchaseRecordsForShop(shopId).first()
        assertEquals(1, records.size)

        val retrieved = records[0]
        assertEquals(shopId, retrieved.shopId)
        assertEquals(txDate, retrieved.transactionDate)
        assertEquals(3000L, retrieved.margin10)
        assertEquals(60500L, retrieved.totalValue)
        assertEquals(35500L, retrieved.purchaseTotalValue)
    }

    @Test
    fun testSalesRecordSchemaAndCalculations() = runBlocking {
        val shopId = "OUT-101"
        val txDate = 1700000000000L
        val totalValue = 60500L
        val cardSales = 12000L
        val cashSales = 28000L
        val totalSales = cardSales + cashSales
        val damage = 500L
        val closingBalance = totalValue - totalSales - damage

        val salesRecord = SalesRecordEntity(
            shopId = shopId,
            transactionDate = txDate,
            slNo = 1L,
            totalValue = totalValue,
            cardSales = cardSales,
            cashSales = cashSales,
            totalSales = totalSales,
            damage = damage,
            closingBalance = closingBalance,
            notes = "Day 1 Sales"
        )

        val id = dao.insertSalesRecord(salesRecord)
        assertTrue(id > 0)

        val records = dao.getSalesRecordsForShop(shopId).first()
        assertEquals(1, records.size)

        val retrieved = records[0]
        assertEquals(shopId, retrieved.shopId)
        assertEquals(txDate, retrieved.transactionDate)
        assertEquals(40000L, retrieved.totalSales)
        assertEquals(500L, retrieved.damage)
        assertEquals(20000L, retrieved.closingBalance)
    }

    @Test
    fun testBankDepositRecordSchemaAndCalculations() = runBlocking {
        val shopId = "OUT-101"
        val txDate = 1700000000000L
        val depositDate = txDate + 86400000L
        val depositAmount = 25000L
        val cardSales = 12000L
        val totalBankDeposit = depositAmount + cardSales

        val depositRecord = BankDepositRecordEntity(
            shopId = shopId,
            transactionDate = txDate,
            bankDepositDate = depositDate,
            depositAmount = depositAmount,
            cardSales = cardSales,
            totalBankDeposit = totalBankDeposit,
            challanPhotoUri = "file:///data/user/0/com.example/files/challan_1.jpg",
            notes = "Cash deposited in CDM"
        )

        val id = dao.insertBankDepositRecord(depositRecord)
        assertTrue(id > 0)

        val records = dao.getBankDepositRecordsForShop(shopId).first()
        assertEquals(1, records.size)

        val retrieved = records[0]
        assertEquals(shopId, retrieved.shopId)
        assertEquals(txDate, retrieved.transactionDate)
        assertTrue(retrieved.bankDepositDate > retrieved.transactionDate)
        assertEquals(37000L, retrieved.totalBankDeposit)
        assertEquals("file:///data/user/0/com.example/files/challan_1.jpg", retrieved.challanPhotoUri)
    }

    @Test
    fun testOutletTransactionUnifiedSchemaConversion() = runBlocking {
        val shopId = "OUT-102"
        val txDate = 1700000000000L
        val nextDate = txDate + 86400000L

        val tx = OutletTransactionEntity(
            shopOutletCode = shopId,
            slNo = 1L,
            transactionDate = txDate,
            openingBalance = 18000L,
            isOpeningBalanceManual = true,
            purchase = 22000L,
            margin10 = 2200L,
            aroed = 1800L,
            totalValue = 44000L,
            cardSales = 9500L,
            cashSales = 19500L,
            totalSales = 29000L,
            damage = 0L,
            closingBalance = 15000L,
            bankDepositDate = nextDate,
            depositAmount = 18000L,
            challanPhotoUri = ""
        )

        val id = dao.insertOutletTransaction(tx)
        assertTrue(id > 0)

        val retrieved = dao.getLatestTransactionForShop(shopId)
        assertNotNull(retrieved)
        assertEquals(shopId, retrieved?.shopOutletCode)
        assertEquals(44000L, retrieved?.totalValue)
        assertEquals(26000L, retrieved?.purchaseTotalValue)
        assertEquals(27500L, retrieved?.totalBankDeposit)

        // Verify conversion to dedicated record entities
        val purchaseRec = tx.toPurchaseRecord()
        assertEquals(shopId, purchaseRec.shopId)
        assertEquals(44000L, purchaseRec.totalValue)

        val salesRec = tx.toSalesRecord()
        assertEquals(shopId, salesRec.shopId)
        assertEquals(15000L, salesRec.closingBalance)

        val depositRec = tx.toBankDepositRecord()
        assertEquals(shopId, depositRec.shopId)
        assertEquals(27500L, depositRec.totalBankDeposit)
    }

    @Test
    fun testTypeConverters() {
        val converters = com.example.data.database.Converters()

        // Date converters
        val nowMillis = 1710000000000L
        val date = converters.fromTimestamp(nowMillis)
        assertNotNull(date)
        assertEquals(nowMillis, converters.dateToTimestamp(date))

        // String list converters
        val shopCodes = listOf("OUT-101", "OUT-102", "OUT-103")
        val joined = converters.fromStringList(shopCodes)
        assertEquals("OUT-101,OUT-102,OUT-103", joined)
        val parsed = converters.toStringList(joined)
        assertEquals(shopCodes, parsed)

        // Enum converters
        val role = com.example.data.model.UserRole.RIC
        val roleStr = converters.fromUserRole(role)
        assertEquals("RIC", roleStr)
        assertEquals(role, converters.toUserRole(roleStr))

        val mode = com.example.data.model.PaymentMode.UPI
        val modeStr = converters.fromPaymentMode(mode)
        assertEquals("UPI", modeStr)
        assertEquals(mode, converters.toPaymentMode(modeStr))

        val status = com.example.data.model.PaymentStatus.PAID
        val statusStr = converters.fromPaymentStatus(status)
        assertEquals("PAID", statusStr)
        assertEquals(status, converters.toPaymentStatus(statusStr))

        val source = com.example.data.model.DepositSource.CASH_COUNTER
        val sourceStr = converters.fromDepositSource(source)
        assertEquals("CASH_COUNTER", sourceStr)
        assertEquals(source, converters.toDepositSource(sourceStr))

        val reconStatus = com.example.data.model.ReconciliationStatus.BALANCED
        val reconStatusStr = converters.fromReconciliationStatus(reconStatus)
        assertEquals("BALANCED", reconStatusStr)
        assertEquals(reconStatus, converters.toReconciliationStatus(reconStatusStr))
    }

    @Test
    fun testDatabaseSingletonAndMigrationsConfigured() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val instance = AppDatabase.getDatabase(context)
        assertNotNull(instance)
        assertNotNull(AppDatabase.MIGRATION_1_2)
        assertNotNull(AppDatabase.MIGRATION_2_3)
        assertNotNull(AppDatabase.MIGRATION_3_4)
        assertNotNull(AppDatabase.MIGRATION_4_5)
        assertEquals(4, AppDatabase.ALL_MIGRATIONS.size)
        assertEquals(1, AppDatabase.MIGRATION_1_2.startVersion)
        assertEquals(2, AppDatabase.MIGRATION_1_2.endVersion)
        assertEquals(2, AppDatabase.MIGRATION_2_3.startVersion)
        assertEquals(3, AppDatabase.MIGRATION_2_3.endVersion)
        assertEquals(3, AppDatabase.MIGRATION_3_4.startVersion)
        assertEquals(4, AppDatabase.MIGRATION_3_4.endVersion)
        assertEquals(4, AppDatabase.MIGRATION_4_5.startVersion)
        assertEquals(5, AppDatabase.MIGRATION_4_5.endVersion)

        // Verify TransactionDatabase alias resolves to AppDatabase
        val txDb: com.example.data.database.TransactionDatabase = instance
        assertNotNull(txDb)
    }

    @Test
    fun testPurchaseSalesAndBankDepositEntitiesStoredLocally() = runBlocking {
        val shopId = "OUT-LOCAL-TEST"
        val now = 1715000000000L

        // 1. Purchase Entity
        val purchase = Purchase(
            shopId = shopId,
            date = now,
            vendorName = "Amul Dairy Dist",
            invoiceNo = "INV-PUR-901",
            category = "Dairy",
            amount = 12500.0,
            paymentMode = "CASH",
            notes = "Morning stock purchase"
        )
        val purchaseId = dao.insertPurchase(purchase)
        assertTrue("Purchase ID must be generated", purchaseId > 0)

        val purchases = dao.getPurchasesBetween(now - 1000, now + 1000).first()
        assertEquals(1, purchases.size)
        assertEquals("Amul Dairy Dist", purchases[0].vendorName)
        assertEquals(12500.0, purchases[0].amount, 0.01)

        // 2. Sales Entity
        val sales = Sales(
            shopId = shopId,
            date = now,
            customerName = "Retail Customer",
            invoiceNo = "BILL-SALES-101",
            itemsSummary = "Beverages and Snacks",
            amount = 18450.0,
            paymentMode = "UPI",
            notes = "Evening counter sales"
        )
        val salesId = dao.insertSale(sales)
        assertTrue("Sales ID must be generated", salesId > 0)

        val allSales = dao.getSalesBetween(now - 1000, now + 1000).first()
        assertEquals(1, allSales.size)
        assertEquals("Retail Customer", allSales[0].customerName)
        assertEquals(18450.0, allSales[0].amount, 0.01)

        // 3. BankDeposit Entity
        val deposit = BankDeposit(
            shopId = shopId,
            date = now + 86400000L,
            bankName = "State Bank of India",
            accountNumberLast4 = "4321",
            amount = 15000.0,
            depositSource = "CASH_COUNTER",
            referenceNumber = "CDM-TXN-7788",
            denominationBreakdown = "500x30",
            notes = "Daily cash collection deposit"
        )
        val depositId = dao.insertBankDeposit(deposit)
        assertTrue("BankDeposit ID must be generated", depositId > 0)

        val allDeposits = dao.getBankDepositsBetween(now, now + 100000000L).first()
        assertEquals(1, allDeposits.size)
        assertEquals("State Bank of India", allDeposits[0].bankName)
        assertEquals(15000.0, allDeposits[0].amount, 0.01)
        assertEquals("CDM-TXN-7788", allDeposits[0].referenceNumber)
    }
}
