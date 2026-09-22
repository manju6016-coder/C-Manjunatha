package com.example.domain

import com.example.data.model.BankDepositEntity
import com.example.data.model.DailyReconciliationEntity
import com.example.data.model.DepositSource
import com.example.data.model.PaymentMode
import com.example.data.model.PurchaseEntity
import com.example.data.model.ReconciliationStatus
import com.example.data.model.SaleEntity
import kotlin.math.abs

/**
 * Denomination tally counts for Indian currency notes and coins.
 */
data class DenominationTally(
    val n2000: Int = 0,
    val n500: Int = 0,
    val n200: Int = 0,
    val n100: Int = 0,
    val n50: Int = 0,
    val n20: Int = 0,
    val n10: Int = 0,
    val n5: Int = 0,
    val coins: Int = 0
) {
    fun calculateTotal(): Double {
        return (n2000 * 2000.0) +
                (n500 * 500.0) +
                (n200 * 200.0) +
                (n100 * 100.0) +
                (n50 * 50.0) +
                (n20 * 20.0) +
                (n10 * 10.0) +
                (n5 * 5.0) +
                (coins * 1.0)
    }

    fun toSummaryString(): String {
        val parts = mutableListOf<String>()
        if (n2000 > 0) parts.add("₹2000x$n2000")
        if (n500 > 0) parts.add("₹500x$n500")
        if (n200 > 0) parts.add("₹200x$n200")
        if (n100 > 0) parts.add("₹100x$n100")
        if (n50 > 0) parts.add("₹50x$n50")
        if (n20 > 0) parts.add("₹20x$n20")
        if (n10 > 0) parts.add("₹10x$n10")
        if (n5 > 0) parts.add("₹5x$n5")
        if (coins > 0) parts.add("Coins: ₹$coins")
        return parts.joinToString(", ")
    }
}

/**
 * Comprehensive automated daily reconciliation computation snapshot.
 */
data class DailyReconciliationSummary(
    val dateEpochStart: Long,
    val openingCash: Double,

    // Sales Aggregates
    val totalSalesAmount: Double,
    val totalCashSales: Double,
    val totalDigitalSales: Double,
    val totalCreditSales: Double,
    val salesCount: Int,

    // Purchases Aggregates
    val totalPurchasesAmount: Double,
    val totalCashPurchases: Double,
    val totalBankPurchases: Double,
    val totalCreditPurchases: Double,
    val purchasesCount: Int,

    // Deposits Aggregates
    val totalDepositsAmount: Double,
    val totalCashDepositedToBank: Double,
    val totalDigitalSettlementsToBank: Double,
    val depositsCount: Int,

    // Reconciled Drawer Projections
    val expectedClosingCash: Double,
    val actualCountedCash: Double,
    val discrepancy: Double,
    val status: ReconciliationStatus,

    // Bank Ledger Position
    val expectedBankInflow: Double,
    val netBankMovement: Double,

    // Saved state (if already locked)
    val existingReconciliation: DailyReconciliationEntity? = null,
    val isLocked: Boolean = existingReconciliation != null
)

object ReconciliationEngine {

    /**
     * Automatically computes daily reconciliation summary from raw ledger streams.
     */
    fun computeDailySummary(
        dateEpochStart: Long,
        openingCash: Double,
        sales: List<SaleEntity>,
        purchases: List<PurchaseEntity>,
        deposits: List<BankDepositEntity>,
        actualCountedCash: Double,
        existingReconciliation: DailyReconciliationEntity? = null
    ): DailyReconciliationSummary {
        // Sales breakdowns
        var totalSales = 0.0
        var cashSales = 0.0
        var digitalSales = 0.0
        var creditSales = 0.0

        for (s in sales) {
            totalSales += s.amount
            val mode = PaymentMode.fromString(s.paymentMode)
            when {
                mode.isCash -> cashSales += s.amount
                mode.isBankOrDigital -> digitalSales += s.amount
                mode == PaymentMode.CREDIT -> creditSales += s.amount
                else -> digitalSales += s.amount
            }
        }

        // Purchases breakdowns
        var totalPurchases = 0.0
        var cashPurchases = 0.0
        var bankPurchases = 0.0
        var creditPurchases = 0.0

        for (p in purchases) {
            totalPurchases += p.amount
            val mode = PaymentMode.fromString(p.paymentMode)
            when {
                mode.isCash -> cashPurchases += p.amount
                mode.isBankOrDigital -> bankPurchases += p.amount
                mode == PaymentMode.CREDIT -> creditPurchases += p.amount
                else -> bankPurchases += p.amount
            }
        }

        // Deposits breakdowns
        var totalDeposits = 0.0
        var cashDeposited = 0.0
        var digitalSettlements = 0.0

        for (d in deposits) {
            totalDeposits += d.amount
            val source = DepositSource.fromString(d.depositSource)
            if (source.isPhysicalCash) {
                cashDeposited += d.amount
            } else {
                digitalSettlements += d.amount
            }
        }

        // Automated Expected Closing Cash in Drawer formula:
        // Opening Float + Cash Sales - Cash Purchases - Physical Cash Deposited into Bank
        val expectedClosingCash = (openingCash + cashSales - cashPurchases - cashDeposited).coerceAtLeast(0.0)

        // Counted Cash
        val effectiveCountedCash = if (existingReconciliation != null) {
            existingReconciliation.actualCountedCash
        } else {
            actualCountedCash
        }

        val discrepancy = effectiveCountedCash - expectedClosingCash

        val status = when {
            existingReconciliation != null -> {
                ReconciliationStatus.entries.find { it.name == existingReconciliation.status }
                    ?: ReconciliationStatus.BALANCED
            }
            effectiveCountedCash <= 0.0 && expectedClosingCash > 0.0 -> ReconciliationStatus.PENDING
            abs(discrepancy) < 0.5 -> ReconciliationStatus.BALANCED
            discrepancy > 0 -> ReconciliationStatus.SURPLUS
            else -> ReconciliationStatus.SHORTAGE
        }

        // Expected Bank Inflow: Digital Sales + Bank Deposits
        val expectedBankInflow = digitalSales + totalDeposits
        // Net Bank Position = (Digital Sales + Bank Deposits) - Bank Purchases
        val netBankMovement = expectedBankInflow - bankPurchases

        return DailyReconciliationSummary(
            dateEpochStart = dateEpochStart,
            openingCash = openingCash,
            totalSalesAmount = totalSales,
            totalCashSales = cashSales,
            totalDigitalSales = digitalSales,
            totalCreditSales = creditSales,
            salesCount = sales.size,
            totalPurchasesAmount = totalPurchases,
            totalCashPurchases = cashPurchases,
            totalBankPurchases = bankPurchases,
            totalCreditPurchases = creditPurchases,
            purchasesCount = purchases.size,
            totalDepositsAmount = totalDeposits,
            totalCashDepositedToBank = cashDeposited,
            totalDigitalSettlementsToBank = digitalSettlements,
            depositsCount = deposits.size,
            expectedClosingCash = expectedClosingCash,
            actualCountedCash = effectiveCountedCash,
            discrepancy = discrepancy,
            status = status,
            expectedBankInflow = expectedBankInflow,
            netBankMovement = netBankMovement,
            existingReconciliation = existingReconciliation
        )
    }
}
