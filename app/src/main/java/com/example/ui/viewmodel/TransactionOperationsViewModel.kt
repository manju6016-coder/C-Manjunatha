package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.BankDepositRecordEntity
import com.example.data.model.OutletTransactionEntity
import com.example.data.model.PurchaseRecordEntity
import com.example.data.model.SalesRecordEntity
import com.example.data.model.ShopEntity
import com.example.data.repository.ILedgerRepository
import com.example.data.repository.LedgerRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * UI State for Purchase Entry Form with automatic 10% margin calculation.
 */
data class PurchaseFormState(
    val shopId: String = "",
    val transactionDate: Long = System.currentTimeMillis(),
    val slNo: Long = 1L,
    val openingBalance: Long = 0L,
    val isOpeningBalanceManual: Boolean = false,
    val purchase: Long = 0L,
    val margin10: Long = 0L,
    val aroed: Long = 0L,
    val totalValue: Long = 0L,
    val notes: String = "",
    val errorMessage: String? = null
) {
    val isValid: Boolean get() = shopId.isNotBlank() && purchase >= 0L && totalValue >= 0L
}

/**
 * UI State for Sales Entry Form with Card + Cash separation and Closing Balance computation.
 */
data class SalesFormState(
    val shopId: String = "",
    val transactionDate: Long = System.currentTimeMillis(),
    val slNo: Long = 1L,
    val totalValue: Long = 0L,
    val cardSales: Long = 0L,
    val cashSales: Long = 0L,
    val totalSales: Long = 0L,
    val damage: Long = 0L,
    val closingBalance: Long = 0L,
    val notes: String = "",
    val errorMessage: String? = null
) {
    val isValid: Boolean get() = shopId.isNotBlank() && cardSales >= 0L && cashSales >= 0L
}

/**
 * UI State for Bank Deposit Entry Form with mandatory Next-Day date validation and receipt photo URI.
 */
data class BankDepositFormState(
    val shopId: String = "",
    val transactionDate: Long = System.currentTimeMillis(),
    val bankDepositDate: Long = System.currentTimeMillis() + 86400000L,
    val depositAmount: Long = 0L,
    val cardSales: Long = 0L,
    val totalBankDeposit: Long = 0L,
    val challanPhotoUri: String? = null,
    val notes: String = "",
    val errorMessage: String? = null
) {
    val isDepositDateValid: Boolean get() = bankDepositDate > transactionDate
    val isValid: Boolean get() = shopId.isNotBlank() && depositAmount >= 0L && isDepositDateValid
}

/**
 * Sealed class representing one-shot UI feedback events.
 */
sealed class OperationEvent {
    data class Success(val message: String) : OperationEvent()
    data class Error(val message: String) : OperationEvent()
}

/**
 * Date range filter for ledger queries.
 */
enum class LedgerDateFilter {
    ALL,
    TODAY,
    THIS_WEEK,
    THIS_MONTH
}

/**
 * TransactionOperationsViewModel
 *
 * Single Source of Truth for Purchase, Sales, Bank Deposit, and Unified Daily Outlet Transactions.
 * Bridges Jetpack Compose UI components with the local Room Database (LedgerRepository).
 */
class TransactionOperationsViewModel(
    application: Application,
    private val repository: ILedgerRepository = LedgerRepository.fromDatabase(
        AppDatabase.getDatabase(application)
    )
) : AndroidViewModel(application) {

    // --- Active Selected Shop Filter ---
    private val _selectedShop = MutableStateFlow<ShopEntity?>(null)
    val selectedShop: StateFlow<ShopEntity?> = _selectedShop.asStateFlow()

    // --- Active Date Filter ---
    private val _dateFilter = MutableStateFlow(LedgerDateFilter.ALL)
    val dateFilter: StateFlow<LedgerDateFilter> = _dateFilter.asStateFlow()

    // --- UI Events & Loading Indicator ---
    private val _operationEvents = MutableSharedFlow<OperationEvent>()
    val operationEvents: SharedFlow<OperationEvent> = _operationEvents.asSharedFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // --- Available Shops List ---
    val allShops: StateFlow<List<ShopEntity>> = repository.allShops
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        // Auto-select first shop if none selected
        viewModelScope.launch {
            repository.allShops.collect { shops ->
                if (_selectedShop.value == null && shops.isNotEmpty()) {
                    _selectedShop.value = shops.first()
                }
            }
        }
    }

    fun setSelectedShop(shop: ShopEntity?) {
        _selectedShop.value = shop
    }

    fun setDateFilter(filter: LedgerDateFilter) {
        _dateFilter.value = filter
    }

    private val filterBoundaries: StateFlow<Pair<Long, Long>?> = _dateFilter.map { filter ->
        val now = System.currentTimeMillis()
        when (filter) {
            LedgerDateFilter.ALL -> null
            LedgerDateFilter.TODAY -> {
                val cal = java.util.Calendar.getInstance()
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
                cal.set(java.util.Calendar.MINUTE, 59)
                cal.set(java.util.Calendar.SECOND, 59)
                cal.set(java.util.Calendar.MILLISECOND, 999)
                Pair(start, cal.timeInMillis)
            }
            LedgerDateFilter.THIS_WEEK -> {
                val cal = java.util.Calendar.getInstance()
                cal.firstDayOfWeek = java.util.Calendar.MONDAY
                cal.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                val start = cal.timeInMillis
                Pair(start, now)
            }
            LedgerDateFilter.THIS_MONTH -> {
                val cal = java.util.Calendar.getInstance()
                cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                val start = cal.timeInMillis
                Pair(start, now)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    // =========================================================================
    // --- 1. Purchase Data Stream & Operations ---
    // =========================================================================
    val purchaseRecords: StateFlow<List<PurchaseRecordEntity>> = combine(
        _selectedShop,
        filterBoundaries
    ) { shop, bounds ->
        Pair(shop, bounds)
    }.flatMapLatest { (shop, bounds) ->
        if (shop == null) {
            flowOf(emptyList())
        } else {
            repository.getPurchaseRecordsForShop(shop.outletCode).map { records ->
                if (bounds == null) records
                else records.filter { it.transactionDate in bounds.first..bounds.second }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val totalPurchaseValue: StateFlow<Long> = purchaseRecords.map { list ->
        list.sumOf { it.totalValue }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0L)

    private val _purchaseFormState = MutableStateFlow(PurchaseFormState())
    val purchaseFormState: StateFlow<PurchaseFormState> = _purchaseFormState.asStateFlow()

    fun preparePurchaseForm(
        shopId: String,
        openingBalance: Long,
        nextSlNo: Long,
        isManualOpening: Boolean = false
    ) {
        _purchaseFormState.value = PurchaseFormState(
            shopId = shopId,
            openingBalance = openingBalance,
            isOpeningBalanceManual = isManualOpening,
            slNo = nextSlNo,
            transactionDate = System.currentTimeMillis()
        )
    }

    fun updatePurchaseForm(
        purchaseAmount: Long,
        aroed: Long,
        notes: String = _purchaseFormState.value.notes
    ) {
        val current = _purchaseFormState.value
        val margin10 = Math.round(purchaseAmount * 0.10)
        val totalValue = current.openingBalance + purchaseAmount + margin10 + aroed
        _purchaseFormState.value = current.copy(
            purchase = purchaseAmount,
            margin10 = margin10,
            aroed = aroed,
            totalValue = totalValue,
            notes = notes,
            errorMessage = null
        )
    }

    fun savePurchaseRecord(
        record: PurchaseRecordEntity,
        onComplete: ((Boolean) -> Unit)? = null
    ): Job = viewModelScope.launch {
        _isLoading.value = true
        try {
            repository.insertPurchaseRecord(record)
            _operationEvents.emit(OperationEvent.Success("Purchase record saved successfully"))
            onComplete?.invoke(true)
        } catch (e: Exception) {
            _operationEvents.emit(OperationEvent.Error("Failed to save purchase record: ${e.message}"))
            onComplete?.invoke(false)
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun savePurchaseRecordSuspend(record: PurchaseRecordEntity): Long =
        repository.insertPurchaseRecord(record)

    fun deletePurchaseRecord(record: PurchaseRecordEntity) {
        viewModelScope.launch {
            try {
                repository.deletePurchaseRecord(record)
                _operationEvents.emit(OperationEvent.Success("Purchase record deleted"))
            } catch (e: Exception) {
                _operationEvents.emit(OperationEvent.Error("Delete failed: ${e.message}"))
            }
        }
    }

    // =========================================================================
    // --- 2. Sales Data Stream & Operations ---
    // =========================================================================
    val salesRecords: StateFlow<List<SalesRecordEntity>> = combine(
        _selectedShop,
        filterBoundaries
    ) { shop, bounds ->
        Pair(shop, bounds)
    }.flatMapLatest { (shop, bounds) ->
        if (shop == null) {
            flowOf(emptyList())
        } else {
            repository.getSalesRecordsForShop(shop.outletCode).map { records ->
                if (bounds == null) records
                else records.filter { it.transactionDate in bounds.first..bounds.second }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val totalSalesValue: StateFlow<Long> = salesRecords.map { list ->
        list.sumOf { it.totalSales }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0L)

    val totalCashSales: StateFlow<Long> = salesRecords.map { list ->
        list.sumOf { it.cashSales }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0L)

    val totalCardSales: StateFlow<Long> = salesRecords.map { list ->
        list.sumOf { it.cardSales }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0L)

    private val _salesFormState = MutableStateFlow(SalesFormState())
    val salesFormState: StateFlow<SalesFormState> = _salesFormState.asStateFlow()

    fun prepareSalesForm(shopId: String, totalValue: Long, nextSlNo: Long) {
        _salesFormState.value = SalesFormState(
            shopId = shopId,
            totalValue = totalValue,
            slNo = nextSlNo,
            transactionDate = System.currentTimeMillis()
        )
    }

    fun updateSalesForm(
        totalValue: Long,
        cardSales: Long,
        cashSales: Long,
        damage: Long = 0L,
        shopId: String = _salesFormState.value.shopId.ifBlank { _selectedShop.value?.outletCode ?: "" },
        notes: String = _salesFormState.value.notes
    ) {
        val current = _salesFormState.value
        val totalSales = cardSales + cashSales
        val closingBalance = (totalValue - totalSales - damage).coerceAtLeast(0L)
        _salesFormState.value = current.copy(
            shopId = shopId,
            totalValue = totalValue,
            cardSales = cardSales,
            cashSales = cashSales,
            totalSales = totalSales,
            damage = damage,
            closingBalance = closingBalance,
            notes = notes,
            errorMessage = null
        )
    }

    fun saveSalesRecord(
        record: SalesRecordEntity,
        onComplete: ((Boolean) -> Unit)? = null
    ): Job = viewModelScope.launch {
        _isLoading.value = true
        try {
            repository.insertSalesRecord(record)
            _operationEvents.emit(OperationEvent.Success("Sales record saved successfully"))
            onComplete?.invoke(true)
        } catch (e: Exception) {
            _operationEvents.emit(OperationEvent.Error("Failed to save sales record: ${e.message}"))
            onComplete?.invoke(false)
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun saveSalesRecordSuspend(record: SalesRecordEntity): Long =
        repository.insertSalesRecord(record)

    fun deleteSalesRecord(record: SalesRecordEntity) {
        viewModelScope.launch {
            try {
                repository.deleteSalesRecord(record)
                _operationEvents.emit(OperationEvent.Success("Sales record deleted"))
            } catch (e: Exception) {
                _operationEvents.emit(OperationEvent.Error("Delete failed: ${e.message}"))
            }
        }
    }

    // =========================================================================
    // --- 3. Bank Deposit Data Stream & Operations ---
    // =========================================================================
    val bankDepositRecords: StateFlow<List<BankDepositRecordEntity>> = combine(
        _selectedShop,
        filterBoundaries
    ) { shop, bounds ->
        Pair(shop, bounds)
    }.flatMapLatest { (shop, bounds) ->
        if (shop == null) {
            flowOf(emptyList())
        } else {
            repository.getBankDepositRecordsForShop(shop.outletCode).map { records ->
                if (bounds == null) records
                else records.filter { it.transactionDate in bounds.first..bounds.second }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val totalBankDepositAmount: StateFlow<Long> = bankDepositRecords.map { list ->
        list.sumOf { it.totalBankDeposit }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0L)

    private val _bankDepositFormState = MutableStateFlow(BankDepositFormState())
    val bankDepositFormState: StateFlow<BankDepositFormState> = _bankDepositFormState.asStateFlow()

    fun prepareBankDepositForm(
        shopId: String,
        cardSales: Long,
        transactionDate: Long = System.currentTimeMillis()
    ) {
        _bankDepositFormState.value = BankDepositFormState(
            shopId = shopId,
            cardSales = cardSales,
            transactionDate = transactionDate,
            bankDepositDate = transactionDate + 86400000L
        )
    }

    fun updateBankDepositForm(
        depositAmount: Long,
        cardSales: Long,
        bankDepositDate: Long,
        shopId: String = _bankDepositFormState.value.shopId.ifBlank { _selectedShop.value?.outletCode ?: "" },
        challanPhotoUri: String? = _bankDepositFormState.value.challanPhotoUri,
        notes: String = _bankDepositFormState.value.notes
    ) {
        val current = _bankDepositFormState.value
        val isDateValid = bankDepositDate > current.transactionDate
        val totalDeposit = depositAmount + cardSales
        _bankDepositFormState.value = current.copy(
            shopId = shopId,
            depositAmount = depositAmount,
            cardSales = cardSales,
            totalBankDeposit = totalDeposit,
            bankDepositDate = bankDepositDate,
            challanPhotoUri = challanPhotoUri,
            notes = notes,
            errorMessage = if (!isDateValid) "Bank deposit date must be after transaction date" else null
        )
    }

    fun saveBankDepositRecord(
        record: BankDepositRecordEntity,
        onComplete: ((Boolean) -> Unit)? = null
    ): Job = viewModelScope.launch {
        if (record.bankDepositDate <= record.transactionDate) {
            _operationEvents.emit(OperationEvent.Error("Bank deposit date must be strictly after the transaction date"))
            onComplete?.invoke(false)
            return@launch
        }
        _isLoading.value = true
        try {
            repository.insertBankDepositRecord(record)
            _operationEvents.emit(OperationEvent.Success("Bank deposit record saved successfully"))
            onComplete?.invoke(true)
        } catch (e: Exception) {
            _operationEvents.emit(OperationEvent.Error("Failed to save bank deposit record: ${e.message}"))
            onComplete?.invoke(false)
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun saveBankDepositRecordSuspend(record: BankDepositRecordEntity): Long =
        repository.insertBankDepositRecord(record)

    fun deleteBankDepositRecord(record: BankDepositRecordEntity) {
        viewModelScope.launch {
            try {
                repository.deleteBankDepositRecord(record)
                _operationEvents.emit(OperationEvent.Success("Bank deposit record deleted"))
            } catch (e: Exception) {
                _operationEvents.emit(OperationEvent.Error("Delete failed: ${e.message}"))
            }
        }
    }

    // =========================================================================
    // --- 4. Unified Daily Outlet Transactions ---
    // =========================================================================
    val outletTransactions: StateFlow<List<OutletTransactionEntity>> = combine(
        _selectedShop,
        filterBoundaries
    ) { shop, bounds ->
        Pair(shop, bounds)
    }.flatMapLatest { (shop, bounds) ->
        if (shop == null) {
            flowOf(emptyList())
        } else {
            repository.getTransactionsForShop(shop.outletCode).map { records ->
                if (bounds == null) records
                else records.filter { it.transactionDate in bounds.first..bounds.second }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    suspend fun getPreviousClosingBalance(shopOutletCode: String, transactionDate: Long): Long? {
        return repository.getPreviousClosingBalance(shopOutletCode, transactionDate)
    }

    suspend fun getNextSlNo(shopOutletCode: String): Long {
        return repository.getNextSlNo(shopOutletCode)
    }

    fun saveCompleteOutletTransaction(
        transaction: OutletTransactionEntity,
        onComplete: ((Boolean) -> Unit)? = null
    ): Job = viewModelScope.launch {
        if (transaction.bankDepositDate <= transaction.transactionDate) {
            _operationEvents.emit(OperationEvent.Error("Bank deposit date must be next day or after transaction date"))
            onComplete?.invoke(false)
            return@launch
        }
        _isLoading.value = true
        try {
            repository.insertOutletTransaction(transaction)
            _operationEvents.emit(OperationEvent.Success("Transaction recorded and synced across all ledger tabs"))
            onComplete?.invoke(true)
        } catch (e: Exception) {
            _operationEvents.emit(OperationEvent.Error("Failed to save transaction: ${e.message}"))
            onComplete?.invoke(false)
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun saveCompleteOutletTransactionSuspend(transaction: OutletTransactionEntity): Long =
        repository.insertOutletTransaction(transaction)

    fun updateDamage(id: Long, damage: Long, newClosingBalance: Long) {
        viewModelScope.launch {
            try {
                repository.updateDamage(id, damage, newClosingBalance)
                _operationEvents.emit(OperationEvent.Success("Damage entry updated by Admin"))
            } catch (e: Exception) {
                _operationEvents.emit(OperationEvent.Error("Failed to update damage: ${e.message}"))
            }
        }
    }
}
