package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "purchases",
    indices = [Index(value = ["shopId"]), Index(value = ["date"])]
)
data class PurchaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shopId: String = "",
    val date: Long,
    val vendorName: String,
    val invoiceNo: String,
    val category: String,
    val amount: Double,
    val gstRate: Double = 0.0,
    val paymentMode: String, // from PaymentMode.name
    val paymentStatus: String = PaymentStatus.PAID.name,
    val notes: String = ""
)

@Entity(
    tableName = "sales",
    indices = [Index(value = ["shopId"]), Index(value = ["date"])]
)
data class SaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shopId: String = "",
    val date: Long,
    val customerName: String,
    val invoiceNo: String,
    val itemsSummary: String,
    val amount: Double,
    val gstRate: Double = 0.0,
    val paymentMode: String, // from PaymentMode.name
    val paymentStatus: String = PaymentStatus.PAID.name,
    val notes: String = ""
)

@Entity(
    tableName = "bank_deposits",
    indices = [Index(value = ["shopId"]), Index(value = ["date"])]
)
data class BankDepositEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shopId: String = "",
    val date: Long,
    val bankName: String,
    val accountNumberLast4: String,
    val amount: Double,
    val depositSource: String, // from DepositSource.name
    val referenceNumber: String,
    val denominationBreakdown: String = "", // e.g. "500x10, 200x5"
    val status: String = DepositStatus.CLEARED.name,
    val notes: String = ""
)

/**
 * Typealiases matching 'Purchase', 'Sales', and 'BankDeposit' requirements
 * to store transaction records locally.
 */
typealias Purchase = PurchaseEntity
typealias Sales = SaleEntity
typealias SalesEntity = SaleEntity
typealias BankDeposit = BankDepositEntity

typealias PurchaseRecord = PurchaseRecordEntity
typealias SalesRecord = SalesRecordEntity
typealias BankDepositRecord = BankDepositRecordEntity
typealias OutletTransaction = OutletTransactionEntity

@Entity(
    tableName = "daily_reconciliations",
    indices = [Index(value = ["dateEpochStart"], unique = true)]
)
data class DailyReconciliationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochStart: Long, // Start of day timestamp (unique per day)
    val openingCash: Double,
    val totalCashSales: Double,
    val totalDigitalSales: Double,
    val totalCashPurchases: Double,
    val totalBankPurchases: Double,
    val totalCashDepositedToBank: Double,
    val expectedClosingCash: Double,
    val actualCountedCash: Double,
    val discrepancy: Double,
    val expectedBankInflow: Double,
    val status: String, // from ReconciliationStatus.name
    val denominationTallyJson: String = "",
    val reconciledAt: Long = System.currentTimeMillis(),
    val auditorName: String = "Store Manager",
    val notes: String = ""
)

@Entity(
    tableName = "users",
    indices = [
        Index(value = ["outletCode", "mobileNumber"]),
        Index(value = ["outletCode"])
    ]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val outletCode: String,
    val name: String,
    val mobileNumber: String,
    val password: String = "", // Admin configured password; if blank, defaults to mobileNumber
    val role: String, // ADMIN, RIC, SHOP_EMPLOYEE
    val assignedShopCodes: String = "*" // Comma separated or *
)

@Entity(
    tableName = "shops",
    indices = [Index(value = ["outletCode"], unique = true)]
)
data class ShopEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val outletCode: String,
    val shopName: String,
    val address: String,
    val assignedRicCode: String = "",
    val assignedEmployeeCodes: String = "",
    val initialOpeningBalance: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Dedicated Room Entity for Purchase tab records.
 * Contains shop ID, transaction date, opening balance, purchase amount,
 * auto-calculated 10% margin, AROED, and auto-calculated total value.
 */
@Entity(
    tableName = "purchase_records",
    indices = [
        Index(value = ["shopId"]),
        Index(value = ["transactionDate"]),
        Index(value = ["shopId", "transactionDate"])
    ]
)
data class PurchaseRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shopId: String, // Outlet Code / Shop ID
    val transactionDate: Long, // Start of day epoch millis
    val slNo: Long = 1L,
    val openingBalance: Long = 0L,
    val purchase: Long = 0L,
    val margin10: Long = 0L, // Auto-calculated: purchase * 10%
    val aroed: Long = 0L, // Compulsory when purchase entered
    val totalValue: Long = 0L, // Auto-calculated: openingBalance + purchase + margin10 + aroed
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    val purchaseTotalValue: Long get() = purchase + margin10 + aroed
}

/**
 * Dedicated Room Entity for Sales tab records.
 * Contains shop ID, transaction date, card sales, compulsory cash sales,
 * auto-calculated total sales, admin damage deduction, and auto-calculated closing balance.
 */
@Entity(
    tableName = "sales_records",
    indices = [
        Index(value = ["shopId"]),
        Index(value = ["transactionDate"]),
        Index(value = ["shopId", "transactionDate"])
    ]
)
data class SalesRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shopId: String, // Outlet Code / Shop ID
    val transactionDate: Long, // Start of day epoch millis
    val slNo: Long = 1L,
    val totalValue: Long = 0L, // Inherited from Purchase Tab Total Value
    val cardSales: Long = 0L, // Optional
    val cashSales: Long = 0L, // Compulsory
    val totalSales: Long = 0L, // Auto-calculated: cardSales + cashSales
    val damage: Long = 0L, // Admin only entry
    val closingBalance: Long = 0L, // Auto-calculated: totalValue - totalSales - damage
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Dedicated Room Entity for Bank Deposit tab records.
 * Contains shop ID, transaction date, bank deposit date (validated next-day),
 * deposit amount, card sales, auto-calculated total bank deposit, and challan photo URI.
 */
@Entity(
    tableName = "bank_deposit_records",
    indices = [
        Index(value = ["shopId"]),
        Index(value = ["transactionDate"]),
        Index(value = ["bankDepositDate"]),
        Index(value = ["shopId", "transactionDate"])
    ]
)
data class BankDepositRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shopId: String, // Outlet Code / Shop ID
    val transactionDate: Long, // Transaction Date
    val bankDepositDate: Long, // Must be next date, cannot be same or previous
    val depositAmount: Long = 0L, // Compulsory positive integer
    val cardSales: Long = 0L,
    val totalBankDeposit: Long = 0L, // Auto-calculated: depositAmount + cardSales
    val challanPhotoUri: String = "", // Camera or Gallery photo URI
    val bankName: String = "Designated Outlet Bank",
    val referenceNumber: String = "",
    val notes: String = "",
    val submittedBy: String = "",
    val submittedByRole: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Comprehensive Daily Outlet Transaction Entity that unites Purchase, Sales,
 * and Bank Deposit tabs for an outlet code on a given transaction date.
 */
@Entity(
    tableName = "outlet_transactions",
    indices = [
        Index(value = ["shopOutletCode"]),
        Index(value = ["transactionDate"]),
        Index(value = ["shopOutletCode", "transactionDate"]),
        Index(value = ["syncStatus"])
    ]
)
data class OutletTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shopOutletCode: String, // Shop ID / Outlet Code
    val slNo: Long, // auto generated
    val transactionDate: Long, // Start of day timestamp
    // Purchase Tab fields
    val openingBalance: Long, // positive integers, One time entry & Fetch previous day Closing Balance
    val isOpeningBalanceManual: Boolean = false,
    val purchase: Long = 0L, // positive integers & Optional
    val margin10: Long = 0L, // auto calculation Purchase * 10%, & Read only
    val aroed: Long = 0L, // positive integers & compulsory when purchase entered
    val totalValue: Long = 0L, // auto calculation: Sum Opening Balance, Purchase, 10% Margin, AROED
    // Sales Tab fields
    val cardSales: Long = 0L, // positive integers & Optional
    val cashSales: Long = 0L, // positive integers & compulsory
    val totalSales: Long = 0L, // auto calculation Sum of Card Sales & Cash Sales
    val damage: Long = 0L, // positive integer & Only for admin entry
    val closingBalance: Long = 0L, // auto calculation Total Value - Total Sales - Damage
    // Bank Deposit Tab fields
    val bankDepositDate: Long = 0L, // select transaction next date & cannot select same date & Previous Date
    val depositAmount: Long = 0L, // positive integers & compulsory
    val challanPhotoUri: String = "", // upload challan photo using camera capture or upload from Gallery
    val notes: String = "",
    val submittedBy: String = "",
    val submittedByRole: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "PENDING", // PENDING, SYNCING, SYNCED, FAILED
    val syncedAt: Long? = null
) {
    // Calculated helpers for tab display
    val purchaseTotalValue: Long get() = purchase + margin10 + aroed
    val totalBankDeposit: Long get() = depositAmount + cardSales

    fun toPurchaseRecord(): PurchaseRecordEntity = PurchaseRecordEntity(
        shopId = shopOutletCode,
        transactionDate = transactionDate,
        slNo = slNo,
        openingBalance = openingBalance,
        purchase = purchase,
        margin10 = margin10,
        aroed = aroed,
        totalValue = totalValue,
        notes = notes,
        createdAt = createdAt
    )

    fun toSalesRecord(): SalesRecordEntity = SalesRecordEntity(
        shopId = shopOutletCode,
        transactionDate = transactionDate,
        slNo = slNo,
        totalValue = totalValue,
        cardSales = cardSales,
        cashSales = cashSales,
        totalSales = totalSales,
        damage = damage,
        closingBalance = closingBalance,
        notes = notes,
        createdAt = createdAt
    )

    fun toBankDepositRecord(): BankDepositRecordEntity = BankDepositRecordEntity(
        shopId = shopOutletCode,
        transactionDate = transactionDate,
        bankDepositDate = bankDepositDate,
        depositAmount = depositAmount,
        cardSales = cardSales,
        totalBankDeposit = totalBankDeposit,
        challanPhotoUri = challanPhotoUri,
        notes = notes,
        submittedBy = submittedBy,
        submittedByRole = submittedByRole,
        createdAt = createdAt
    )
}

