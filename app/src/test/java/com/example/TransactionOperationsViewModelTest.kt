package com.example

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.dao.LedgerDao
import com.example.data.database.AppDatabase
import com.example.data.model.BankDepositRecordEntity
import com.example.data.model.OutletTransactionEntity
import com.example.data.model.PurchaseRecordEntity
import com.example.data.model.SalesRecordEntity
import com.example.data.model.ShopEntity
import com.example.data.repository.LedgerRepository
import com.example.ui.viewmodel.TransactionOperationsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TransactionOperationsViewModelTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: LedgerDao
    private lateinit var repository: LedgerRepository
    private lateinit var viewModel: TransactionOperationsViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    private val testShop = ShopEntity(
        id = 99,
        outletCode = "OUT-TEST-99",
        shopName = "Bangalore Central Outlet",
        address = "MG Road, Bangalore",
        assignedRicCode = "RIC-01",
        initialOpeningBalance = 25000L
    )

    @Before
    fun setUp() = runBlocking {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.ledgerDao()
        repository = LedgerRepository(dao)

        dao.insertShop(testShop)

        val app = ApplicationProvider.getApplicationContext<Application>()
        viewModel = TransactionOperationsViewModel(app, repository)
        viewModel.setSelectedShop(testShop)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    @Test
    fun testPurchaseFormCalculationAndSave() = runBlocking {
        viewModel.preparePurchaseForm(
            shopId = "OUT-TEST-99",
            openingBalance = 25000L,
            nextSlNo = 1L
        )

        // 30000 purchase with 2500 AROED -> 10% margin = 3000 -> total = 25000 + 30000 + 3000 + 2500 = 60500
        viewModel.updatePurchaseForm(
            purchaseAmount = 30000L,
            aroed = 2500L,
            notes = "Opening batch restock"
        )

        val form = viewModel.purchaseFormState.value
        assertEquals(3000L, form.margin10)
        assertEquals(60500L, form.totalValue)
        assertTrue(form.isValid)

        val record = PurchaseRecordEntity(
            shopId = form.shopId,
            transactionDate = form.transactionDate,
            slNo = form.slNo,
            openingBalance = form.openingBalance,
            purchase = form.purchase,
            margin10 = form.margin10,
            aroed = form.aroed,
            totalValue = form.totalValue,
            notes = form.notes
        )

        viewModel.savePurchaseRecordSuspend(record)

        val purchases = viewModel.purchaseRecords.first { it.isNotEmpty() }
        assertEquals(1, purchases.size)
        assertEquals("OUT-TEST-99", purchases[0].shopId)
        assertEquals(60500L, purchases[0].totalValue)
        assertEquals(60500L, viewModel.totalPurchaseValue.first { it > 0L })
    }

    @Test
    fun testSalesFormCalculationAndSave() = runBlocking {
        viewModel.prepareSalesForm(
            shopId = "OUT-TEST-99",
            totalValue = 60500L,
            nextSlNo = 1L
        )

        // total value = 60500, card sales = 12000, cash sales = 28000 (total sales = 40000), damage = 500
        // closing balance = 60500 - 40000 - 500 = 20000
        viewModel.updateSalesForm(
            totalValue = 60500L,
            cardSales = 12000L,
            cashSales = 28000L,
            damage = 500L,
            notes = "Daily sales summary"
        )

        val form = viewModel.salesFormState.value
        assertEquals(40000L, form.totalSales)
        assertEquals(20000L, form.closingBalance)
        assertTrue(form.isValid)

        val record = SalesRecordEntity(
            shopId = "OUT-TEST-99",
            transactionDate = System.currentTimeMillis(),
            slNo = 1L,
            totalValue = form.totalValue,
            cardSales = form.cardSales,
            cashSales = form.cashSales,
            totalSales = form.totalSales,
            damage = form.damage,
            closingBalance = form.closingBalance,
            notes = form.notes
        )

        viewModel.saveSalesRecordSuspend(record)

        val sales = viewModel.salesRecords.first { it.isNotEmpty() }
        assertEquals(1, sales.size)
        assertEquals(40000L, sales[0].totalSales)
        assertEquals(20000L, sales[0].closingBalance)
        assertEquals(40000L, viewModel.totalSalesValue.first { it > 0L })
        assertEquals(28000L, viewModel.totalCashSales.first { it > 0L })
        assertEquals(12000L, viewModel.totalCardSales.first { it > 0L })
    }

    @Test
    fun testBankDepositFormAndNextDayValidation() = runBlocking {
        val now = System.currentTimeMillis()
        val nextDay = now + 86400000L

        viewModel.prepareBankDepositForm(
            shopId = "OUT-TEST-99",
            cardSales = 12000L,
            transactionDate = now
        )

        // Valid deposit with next-day date
        viewModel.updateBankDepositForm(
            depositAmount = 25000L,
            cardSales = 12000L,
            bankDepositDate = nextDay,
            challanPhotoUri = "content://media/external/images/media/1",
            notes = "SBI CDM Deposit"
        )

        val form = viewModel.bankDepositFormState.value
        assertEquals(37000L, form.totalBankDeposit)
        assertTrue(form.isDepositDateValid)

        val validRecord = BankDepositRecordEntity(
            shopId = "OUT-TEST-99",
            transactionDate = now,
            bankDepositDate = nextDay,
            depositAmount = 25000L,
            cardSales = 12000L,
            totalBankDeposit = 37000L,
            challanPhotoUri = form.challanPhotoUri ?: "",
            notes = form.notes
        )

        viewModel.saveBankDepositRecordSuspend(validRecord)

        val deposits = viewModel.bankDepositRecords.first { it.isNotEmpty() }
        assertEquals(1, deposits.size)
        assertEquals(37000L, deposits[0].totalBankDeposit)
        assertEquals(37000L, viewModel.totalBankDepositAmount.first { it > 0L })
    }

    @Test
    fun testUnifiedOutletTransactionBridge() = runBlocking {
        val now = System.currentTimeMillis()
        val nextDay = now + 86400000L

        val tx = OutletTransactionEntity(
            shopOutletCode = "OUT-TEST-99",
            slNo = 1L,
            transactionDate = now,
            openingBalance = 25000L,
            isOpeningBalanceManual = true,
            purchase = 30000L,
            margin10 = 3000L,
            aroed = 2500L,
            totalValue = 60500L,
            cardSales = 12000L,
            cashSales = 28000L,
            totalSales = 40000L,
            damage = 500L,
            closingBalance = 20000L,
            bankDepositDate = nextDay,
            depositAmount = 25000L,
            challanPhotoUri = "photo_url"
        )

        viewModel.saveCompleteOutletTransactionSuspend(tx)

        val txList = viewModel.outletTransactions.first { it.isNotEmpty() }
        assertEquals(1, txList.size)
        assertEquals("OUT-TEST-99", txList[0].shopOutletCode)
        assertEquals(60500L, txList[0].totalValue)
        assertEquals(40000L, txList[0].totalSales)
        assertEquals(37000L, txList[0].totalBankDeposit)

        // Verify dedicated records also populated via repository
        val purchases = viewModel.purchaseRecords.first { it.isNotEmpty() }
        val sales = viewModel.salesRecords.first { it.isNotEmpty() }
        val deposits = viewModel.bankDepositRecords.first { it.isNotEmpty() }

        assertEquals(1, purchases.size)
        assertEquals(60500L, purchases[0].totalValue)
        assertEquals(1, sales.size)
        assertEquals(40000L, sales[0].totalSales)
        assertEquals(1, deposits.size)
        assertEquals(37000L, deposits[0].totalBankDeposit)
    }
}
