package com.example.data.database

import androidx.room.TypeConverter
import com.example.data.model.DepositSource
import com.example.data.model.DepositStatus
import com.example.data.model.PaymentMode
import com.example.data.model.PaymentStatus
import com.example.data.model.PurchaseCategory
import com.example.data.model.ReconciliationStatus
import com.example.data.model.UserRole
import java.util.Date

/**
 * Room TypeConverters for seamless handling of custom domain types,
 * dates, string collections, and business enums.
 */
class Converters {

    // --- Date & Timestamp Converters ---
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    // --- String List (e.g., assigned shop codes, photo URIs, tags) ---
    @TypeConverter
    fun fromStringList(list: List<String>?): String? {
        return list?.joinToString(",")
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        if (value == null) return null
        return if (value.isBlank()) emptyList() else value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    // --- User Role Enum ---
    @TypeConverter
    fun fromUserRole(role: UserRole?): String? {
        return role?.code ?: role?.name
    }

    @TypeConverter
    fun toUserRole(value: String?): UserRole? {
        return value?.let { UserRole.fromCode(it) }
    }

    // --- Payment Mode Enum ---
    @TypeConverter
    fun fromPaymentMode(mode: PaymentMode?): String? {
        return mode?.name
    }

    @TypeConverter
    fun toPaymentMode(value: String?): PaymentMode? {
        return value?.let { PaymentMode.fromString(it) }
    }

    // --- Payment Status Enum ---
    @TypeConverter
    fun fromPaymentStatus(status: PaymentStatus?): String? {
        return status?.name
    }

    @TypeConverter
    fun toPaymentStatus(value: String?): PaymentStatus? {
        if (value == null) return null
        return runCatching { enumValueOf<PaymentStatus>(value) }.getOrDefault(PaymentStatus.PAID)
    }

    // --- Deposit Source Enum ---
    @TypeConverter
    fun fromDepositSource(source: DepositSource?): String? {
        return source?.name
    }

    @TypeConverter
    fun toDepositSource(value: String?): DepositSource? {
        return value?.let { DepositSource.fromString(it) }
    }

    // --- Deposit Status Enum ---
    @TypeConverter
    fun fromDepositStatus(status: DepositStatus?): String? {
        return status?.name
    }

    @TypeConverter
    fun toDepositStatus(value: String?): DepositStatus? {
        if (value == null) return null
        return runCatching { enumValueOf<DepositStatus>(value) }.getOrDefault(DepositStatus.CLEARED)
    }

    // --- Reconciliation Status Enum ---
    @TypeConverter
    fun fromReconciliationStatus(status: ReconciliationStatus?): String? {
        return status?.name
    }

    @TypeConverter
    fun toReconciliationStatus(value: String?): ReconciliationStatus? {
        if (value == null) return null
        return runCatching { enumValueOf<ReconciliationStatus>(value) }.getOrDefault(ReconciliationStatus.PENDING)
    }

    // --- Purchase Category Enum ---
    @TypeConverter
    fun fromPurchaseCategory(category: PurchaseCategory?): String? {
        return category?.name
    }

    @TypeConverter
    fun toPurchaseCategory(value: String?): PurchaseCategory? {
        if (value == null) return null
        return runCatching { enumValueOf<PurchaseCategory>(value) }.getOrDefault(PurchaseCategory.INVENTORY)
    }
}
