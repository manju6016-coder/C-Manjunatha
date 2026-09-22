package com.example.data.model

enum class PaymentMode(val label: String, val isCash: Boolean, val isBankOrDigital: Boolean) {
    CASH("Cash", isCash = true, isBankOrDigital = false),
    UPI("UPI / QR", isCash = false, isBankOrDigital = true),
    BANK_TRANSFER("Bank Transfer", isCash = false, isBankOrDigital = true),
    CARD("Card / POS", isCash = false, isBankOrDigital = true),
    CHEQUE("Cheque", isCash = false, isBankOrDigital = true),
    CREDIT("Credit / Due", isCash = false, isBankOrDigital = false);

    companion object {
        fun fromString(value: String): PaymentMode {
            return entries.find { it.name.equals(value, ignoreCase = true) || it.label.equals(value, ignoreCase = true) } ?: CASH
        }
    }
}

enum class PaymentStatus(val label: String) {
    PAID("Paid"),
    PARTIAL("Partial"),
    UNPAID("Unpaid")
}

enum class DepositSource(val label: String, val isPhysicalCash: Boolean) {
    CASH_COUNTER("Cash Counter Deposit", isPhysicalCash = true),
    UPI_SETTLEMENT("Daily UPI Settlement", isPhysicalCash = false),
    CHEQUE_CLEARANCE("Cheque Deposit", isPhysicalCash = false),
    CARD_SWIPE_SETTLEMENT("POS Card Settlement", isPhysicalCash = false),
    DIRECT_TRANSFER("Direct Account Transfer", isPhysicalCash = false);

    companion object {
        fun fromString(value: String): DepositSource {
            return entries.find { it.name.equals(value, ignoreCase = true) || it.label.equals(value, ignoreCase = true) } ?: CASH_COUNTER
        }
    }
}

enum class DepositStatus(val label: String) {
    CLEARED("Cleared"),
    IN_TRANSIT("In Transit"),
    FAILED("Failed")
}

enum class ReconciliationStatus(val label: String) {
    BALANCED("Balanced"),
    SURPLUS("Excess Cash (Surplus)"),
    SHORTAGE("Missing Cash (Shortage)"),
    PENDING("Pending Verification")
}

enum class PurchaseCategory(val label: String) {
    INVENTORY("Inventory / Goods"),
    RAW_MATERIAL("Raw Materials"),
    STORE_EXPENSES("Store Expenses"),
    UTILITIES("Utilities / Rent"),
    LOGISTICS("Logistics & Transport"),
    MAINTENANCE("Maintenance & Repairs"),
    OTHER("Other")
}

enum class UserRole(val code: String, val displayName: String) {
    ADMIN("ADMIN", "Admin"),
    RIC("RIC", "RIC (Regional In-Charge)"),
    SHOP_EMPLOYEE("SHOP_EMPLOYEE", "Shop Employee");

    companion object {
        fun fromCode(code: String): UserRole {
            return entries.find { it.code.equals(code, ignoreCase = true) || it.name.equals(code, ignoreCase = true) } ?: SHOP_EMPLOYEE
        }
    }
}

