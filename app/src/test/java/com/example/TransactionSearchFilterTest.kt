package com.example

import com.example.data.model.OutletTransactionEntity
import com.example.ui.components.QuickDateFilter
import com.example.ui.components.TransactionFilterUtils
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class TransactionSearchFilterTest {

    private val sampleTx = OutletTransactionEntity(
        id = 1,
        shopOutletCode = "OUT-BLR-01",
        slNo = 101,
        transactionDate = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 18, 10, 30, 0)
        }.timeInMillis,
        openingBalance = 50000L,
        isOpeningBalanceManual = false,
        purchase = 20000L,
        margin10 = 2000L,
        aroed = 500L,
        totalValue = 72500L,
        cardSales = 15000L,
        cashSales = 25000L,
        totalSales = 40000L,
        damage = 0L,
        bankDepositDate = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 18, 16, 0, 0)
        }.timeInMillis,
        depositAmount = 30000L,
        closingBalance = 2500L
    )

    private val shopName = "Downtown Bangalore Superstore"

    @Test
    fun testSearchByShopNameExactAndPartial() {
        // Full match
        assertTrue(TransactionFilterUtils.matchesQuery(sampleTx, shopName, "Downtown Bangalore Superstore"))
        // Case-insensitive match
        assertTrue(TransactionFilterUtils.matchesQuery(sampleTx, shopName, "downtown"))
        assertTrue(TransactionFilterUtils.matchesQuery(sampleTx, shopName, "bangalore"))
        assertTrue(TransactionFilterUtils.matchesQuery(sampleTx, shopName, "SUPERSTORE"))
        // Substring
        assertTrue(TransactionFilterUtils.matchesQuery(sampleTx, shopName, "town"))
    }

    @Test
    fun testSearchByShopOutletCode() {
        assertTrue(TransactionFilterUtils.matchesQuery(sampleTx, shopName, "OUT-BLR-01"))
        assertTrue(TransactionFilterUtils.matchesQuery(sampleTx, shopName, "blr"))
        assertTrue(TransactionFilterUtils.matchesQuery(sampleTx, shopName, "01"))
    }

    @Test
    fun testSearchByTransactionDateFormats() {
        // dd/MM/yyyy
        assertTrue(TransactionFilterUtils.matchesQuery(sampleTx, shopName, "18/09/2026"))
        // dd-MM-yyyy
        assertTrue(TransactionFilterUtils.matchesQuery(sampleTx, shopName, "18-09-2026"))
        // Day and month
        assertTrue(TransactionFilterUtils.matchesQuery(sampleTx, shopName, "18/09"))
        // Month name
        assertTrue(TransactionFilterUtils.matchesQuery(sampleTx, shopName, "Sep"))
        assertTrue(TransactionFilterUtils.matchesQuery(sampleTx, shopName, "September"))
        // Year
        assertTrue(TransactionFilterUtils.matchesQuery(sampleTx, shopName, "2026"))
    }

    @Test
    fun testSearchQueryEmptyOrBlank() {
        assertTrue(TransactionFilterUtils.matchesQuery(sampleTx, shopName, ""))
        assertTrue(TransactionFilterUtils.matchesQuery(sampleTx, shopName, "   "))
    }

    @Test
    fun testSearchQueryNoMatch() {
        assertFalse(TransactionFilterUtils.matchesQuery(sampleTx, shopName, "Mumbai Express"))
        assertFalse(TransactionFilterUtils.matchesQuery(sampleTx, shopName, "25/12/2026"))
        assertFalse(TransactionFilterUtils.matchesQuery(sampleTx, shopName, "OUT-DEL-99"))
    }

    @Test
    fun testQuickDateFilterAll() {
        assertTrue(TransactionFilterUtils.matchesDateFilter(sampleTx.transactionDate, QuickDateFilter.ALL))
    }

    @Test
    fun testQuickDateFilterToday() {
        val now = System.currentTimeMillis()
        assertTrue(TransactionFilterUtils.matchesDateFilter(now, QuickDateFilter.TODAY))

        // 3 days ago should not match TODAY
        val threeDaysAgo = now - (3 * 24 * 60 * 60 * 1000L)
        assertFalse(TransactionFilterUtils.matchesDateFilter(threeDaysAgo, QuickDateFilter.TODAY))
    }

    @Test
    fun testQuickDateFilterThisWeek() {
        val now = System.currentTimeMillis()
        assertTrue(TransactionFilterUtils.matchesDateFilter(now, QuickDateFilter.THIS_WEEK))

        // 3 days ago should match THIS_WEEK
        val threeDaysAgo = now - (3 * 24 * 60 * 60 * 1000L)
        assertTrue(TransactionFilterUtils.matchesDateFilter(threeDaysAgo, QuickDateFilter.THIS_WEEK))

        // 20 days ago should not match THIS_WEEK
        val twentyDaysAgo = now - (20 * 24 * 60 * 60 * 1000L)
        assertFalse(TransactionFilterUtils.matchesDateFilter(twentyDaysAgo, QuickDateFilter.THIS_WEEK))
    }
}
