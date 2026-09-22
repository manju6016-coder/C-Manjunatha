package com.example

import com.example.data.model.OutletTransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testPurchaseMarginAndTotalValueCalculations() {
        val openingBalance = 25000L
        val purchase = 30000L
        val margin10 = Math.round(purchase * 0.10)
        val aroed = 2500L
        val totalValue = openingBalance + purchase + margin10 + aroed

        assertEquals(3000L, margin10)
        assertEquals(60500L, totalValue)

        val tx = OutletTransactionEntity(
            shopOutletCode = "OUT-101",
            slNo = 1,
            transactionDate = 1000L,
            openingBalance = openingBalance,
            purchase = purchase,
            margin10 = margin10,
            aroed = aroed,
            totalValue = totalValue,
            cardSales = 12000L,
            cashSales = 28000L,
            totalSales = 40000L,
            damage = 500L,
            closingBalance = 20000L,
            bankDepositDate = 2000L,
            depositAmount = 25000L
        )

        // In Purchase Tab display Total value of (Purchase + 10% Margin + AROED)
        assertEquals(35500L, tx.purchaseTotalValue)
    }

    @Test
    fun testSalesAndClosingBalanceCalculations() {
        val totalValue = 60500L
        val cardSales = 12000L
        val cashSales = 28000L
        val totalSales = cardSales + cashSales
        val damage = 500L
        val closingBalance = totalValue - totalSales - damage

        assertEquals(40000L, totalSales)
        assertEquals(20000L, closingBalance)
    }

    @Test
    fun testBankDepositNextDateValidationAndDisplayTotal() {
        val transactionDate = 1700000000000L
        val nextDate = transactionDate + 86400000L
        val cardSales = 8500L
        val depositAmount = 15000L

        // Bank Deposit Date cannot be same or previous date
        assertTrue(nextDate > transactionDate)

        val tx = OutletTransactionEntity(
            shopOutletCode = "OUT-101",
            slNo = 2,
            transactionDate = transactionDate,
            openingBalance = 20000L,
            cardSales = cardSales,
            cashSales = 16500L,
            totalSales = 25000L,
            damage = 0L,
            closingBalance = 12700L,
            bankDepositDate = nextDate,
            depositAmount = depositAmount
        )

        // In Bank Deposit Tab display Total Bank Deposit (Bank Deposit + Card Sales)
        assertEquals(23500L, tx.totalBankDeposit)
    }
}

