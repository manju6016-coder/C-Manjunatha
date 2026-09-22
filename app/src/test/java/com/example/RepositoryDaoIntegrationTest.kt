package com.example

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.model.BankDepositEntity
import com.example.data.model.BankDepositRecordEntity
import com.example.data.model.OutletTransactionEntity
import com.example.data.model.PurchaseEntity
import com.example.data.model.PurchaseRecordEntity
import com.example.data.model.SaleEntity
import com.example.data.model.SalesRecordEntity
import com.example.data.model.ShopEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.data.repository.ILedgerRepository
import com.example.data.repository.LedgerRepository
import com.example.ui.viewmodel.LedgerViewModel
import com.example.ui.viewmodel.TransactionOperationsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RepositoryDaoIntegrationTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: ILedgerRepository
    private lateinit var context: Context
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = LedgerRepository.fromDatabase(db)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    @Test
    fun testAppDatabaseExposesAllIndividualDaos() {
        assertNotNull("PurchaseDao must be exposed", db.purchaseDao())
        assertNotNull("SalesDao must be exposed", db.salesDao())
        assertNotNull("BankDepositDao must be exposed", db.bankDepositDao())
        assertNotNull("OutletTransactionDao must be exposed", db.outletTransactionDao())
        assertNotNull("ShopDao must be exposed", db.shopDao())
        assertNotNull("UserDao must be exposed", db.userDao())
        assertNotNull("DailyReconciliationDao must be exposed", db.dailyReconciliationDao())
        assertNotNull("Unified LedgerDao must be exposed", db.ledgerDao())
    }

    @Test
    fun testShopAndUserDaoIntegrationViaRepository() = runBlocking {
        val testShop = ShopEntity(
            outletCode = "OUT-REPO-1",
            shopName = "Repo Test Shop",
            address = "Koramangala, Bangalore",
            assignedRicCode = "RIC-01",
            initialOpeningBalance = 15000L
        )
        repository.addShop(testShop)

        val retrievedShop = repository.getShopByOutletCode("OUT-REPO-1")
        assertNotNull("Shop should be retrieved via repository", retrievedShop)
        assertEquals("Repo Test Shop", retrievedShop?.shopName)

        val testUser = UserEntity(
            outletCode = "EMP-01",
            name = "Ramesh Kumar",
            mobileNumber = "9876543210",
            role = UserRole.SHOP_EMPLOYEE.code,
            assignedShopCodes = "OUT-REPO-1"
        )
        repository.addUser(testUser)

        val loggedInUser = repository.login("EMP-01", "9876543210")
        assertNotNull("User login via repository should succeed", loggedInUser)
        assertEquals("Ramesh Kumar", loggedInUser?.name)
    }

    @Test
    fun testPurchaseAndSalesRecordsViaRepository() = runBlocking {
        val shopId = "OUT-REPO-1"
        val txDate = 1700000000000L

        val purchaseRecord = PurchaseRecordEntity(
            shopId = shopId,
            transactionDate = txDate,
            slNo = 1L,
            openingBalance = 20000L,
            purchase = 10000L,
            margin10 = 1000L,
            aroed = 500L,
            totalValue = 31500L,
            notes = "Test Purchase"
        )
        val purchaseId = repository.insertPurchaseRecord(purchaseRecord)
        assertTrue(purchaseId > 0)

        val purchaseList = repository.getPurchaseRecordsForShop(shopId).first()
        assertEquals(1, purchaseList.size)
        assertEquals(31500L, purchaseList[0].totalValue)

        val salesRecord = SalesRecordEntity(
            shopId = shopId,
            transactionDate = txDate,
            slNo = 1L,
            totalValue = 31500L,
            cardSales = 6000L,
            cashSales = 12000L,
            totalSales = 18000L,
            damage = 500L,
            closingBalance = 13000L,
            notes = "Test Sales"
        )
        val salesId = repository.insertSalesRecord(salesRecord)
        assertTrue(salesId > 0)

        val salesList = repository.getSalesRecordsForShop(shopId).first()
        assertEquals(1, salesList.size)
        assertEquals(13000L, salesList[0].closingBalance)
    }

    @Test
    fun testBankDepositRecordViaRepository() = runBlocking {
        val shopId = "OUT-REPO-1"
        val txDate = 1700000000000L
        val nextDay = txDate + 86400000L

        val depositRecord = BankDepositRecordEntity(
            shopId = shopId,
            transactionDate = txDate,
            bankDepositDate = nextDay,
            depositAmount = 12000L,
            cardSales = 6000L,
            totalBankDeposit = 18000L,
            notes = "Cash & Card Reconciliation"
        )
        val depositId = repository.insertBankDepositRecord(depositRecord)
        assertTrue(depositId > 0)

        val deposits = repository.getBankDepositRecordsForShop(shopId).first()
        assertEquals(1, deposits.size)
        assertEquals(18000L, deposits[0].totalBankDeposit)
    }

    @Test
    fun testOutletTransactionPropagationViaRepository() = runBlocking {
        val shopId = "OUT-REPO-TX"
        val txDate = 1700000000000L

        val tx = OutletTransactionEntity(
            shopOutletCode = shopId,
            transactionDate = txDate,
            slNo = 1L,
            openingBalance = 50000L,
            purchase = 20000L,
            margin10 = 2000L,
            aroed = 1000L,
            totalValue = 73000L,
            cardSales = 15000L,
            cashSales = 25000L,
            totalSales = 40000L,
            damage = 0L,
            closingBalance = 33000L,
            bankDepositDate = txDate + 86400000L,
            depositAmount = 25000L,
            notes = "Daily transaction test"
        )

        val txId = repository.insertOutletTransaction(tx)
        assertTrue(txId > 0)

        // Verify propagation to all dedicated tables
        val purchases = repository.getPurchaseRecordsForShop(shopId).first()
        assertEquals(1, purchases.size)
        assertEquals(73000L, purchases[0].totalValue)

        val sales = repository.getSalesRecordsForShop(shopId).first()
        assertEquals(1, sales.size)
        assertEquals(33000L, sales[0].closingBalance)

        val deposits = repository.getBankDepositRecordsForShop(shopId).first()
        assertEquals(1, deposits.size)
        assertEquals(40000L, deposits[0].totalBankDeposit)
    }

    @Test
    fun testViewModelsInjectRepositoryProperly() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val ledgerVm = LedgerViewModel(app, repository)
        val opsVm = TransactionOperationsViewModel(app, repository)

        assertNotNull(ledgerVm)
        assertNotNull(opsVm)

        // Verify that the single-argument constructor called by AndroidViewModelFactory exists and instantiates cleanly
        val ledgerVmSingleArg = LedgerViewModel(app)
        assertNotNull(ledgerVmSingleArg)

        // Verify reflection lookup for AndroidViewModelFactory constructor
        val constructor = LedgerViewModel::class.java.getConstructor(Application::class.java)
        assertNotNull(constructor)
    }
}
