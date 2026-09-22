package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.BankDepositEntity
import com.example.data.model.DailyReconciliationEntity
import com.example.data.model.OutletTransactionEntity
import com.example.data.model.PurchaseEntity
import com.example.data.model.ReconciliationStatus
import com.example.data.model.SaleEntity
import com.example.data.model.ShopEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.data.model.PurchaseRecordEntity
import com.example.data.model.SalesRecordEntity
import com.example.data.model.BankDepositRecordEntity
import com.example.data.model.DatabaseSyncState
import com.example.data.model.SyncConnectionLog
import com.example.data.model.SyncConnectionStatus
import com.example.data.model.SyncResult
import com.example.data.sync.RemoteSyncService
import com.example.data.repository.ILedgerRepository
import com.example.data.repository.LedgerRepository
import com.example.domain.DailyReconciliationSummary
import com.example.domain.DenominationTally
import com.example.domain.ReconciliationEngine
import com.example.security.AdminBiometricPreferences
import com.example.security.BiometricAuthManager
import com.example.security.BiometricCapability
import com.example.util.IndianCurrencyUtils
import com.example.util.NetworkConnectivityMonitor
import com.example.util.TransactionPermissionRules
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ScreenTab(val title: String) {
    PURCHASES("Purchase"),
    SALES("Sales"),
    BANK_DEPOSITS("Bank Deposit"),
    ADMIN_SHOPS("Manage Shops"),
    DASHBOARD("Overview"),
    RECONCILIATION("Reconciliation")
}

@OptIn(ExperimentalCoroutinesApi::class)
class LedgerViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: ILedgerRepository = LedgerRepository.fromDatabase(AppDatabase.getDatabase(application))
) : AndroidViewModel(application) {

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }
    }

    // =========================================================================
    // --- Authentication & User Session ---
    // =========================================================================
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    // --- OTP Login State ---
    private val _isOtpSent = MutableStateFlow(false)
    val isOtpSent: StateFlow<Boolean> = _isOtpSent.asStateFlow()

    private val _otpTargetMobile = MutableStateFlow("")
    val otpTargetMobile: StateFlow<String> = _otpTargetMobile.asStateFlow()

    private val _otpTargetUser = MutableStateFlow<UserEntity?>(null)
    val otpTargetUser: StateFlow<UserEntity?> = _otpTargetUser.asStateFlow()

    private val _lastGeneratedOtp = MutableStateFlow<String?>(null)
    val lastGeneratedOtp: StateFlow<String?> = _lastGeneratedOtp.asStateFlow()

    private val _isOtpLoading = MutableStateFlow(false)
    val isOtpLoading: StateFlow<Boolean> = _isOtpLoading.asStateFlow()

    val allUsers: StateFlow<List<UserEntity>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allShops: StateFlow<List<ShopEntity>> = repository.allShops
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // =========================================================================
    // --- SQLite Sync Status & Remote Synchronization ---
    // =========================================================================
    private val connectivityMonitor = NetworkConnectivityMonitor(application)
    private val remoteSyncService = RemoteSyncService(application)

    val isOnline: StateFlow<Boolean> = connectivityMonitor.isOnlineFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, connectivityMonitor.isOnline())

    val totalLocalRecords: StateFlow<Int> = repository.totalTransactionsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val pendingLocalRecords: StateFlow<Int> = repository.pendingTransactionsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val syncedLocalRecords: StateFlow<Int> = repository.syncedTransactionsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncMessage = MutableStateFlow<String?>(remoteSyncService.getLastSyncMessage())
    val lastSyncMessage: StateFlow<String?> = _lastSyncMessage.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow<Long?>(remoteSyncService.getLastSyncTimestamp())
    val lastSyncTimestamp: StateFlow<Long?> = _lastSyncTimestamp.asStateFlow()

    val syncConnectionLogs: StateFlow<List<SyncConnectionLog>> = remoteSyncService.historyManager.logsFlow

    val syncState: StateFlow<DatabaseSyncState> = combine(
        combine(totalLocalRecords, pendingLocalRecords, syncedLocalRecords) { total, pending, synced ->
            Triple(total, pending, synced)
        },
        combine(isOnline, isSyncing) { online, syncing ->
            Pair(online, syncing)
        },
        combine(lastSyncTimestamp, lastSyncMessage, syncConnectionLogs) { ts, msg, logs ->
            Triple(ts, msg, logs)
        }
    ) { (total, pending, synced), (online, syncing), (lastSync, message, logs) ->
        val latestLog = logs.firstOrNull()
        val totalFailedRecords = latestLog?.failureCount ?: 0
        DatabaseSyncState(
            totalRecords = total,
            pendingRecords = pending,
            syncedRecords = synced,
            isOnline = online,
            isSyncing = syncing,
            lastSyncedAt = lastSync,
            lastSyncMessage = message,
            lastSyncStatus = latestLog?.status,
            failedRecordCount = totalFailedRecords
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DatabaseSyncState())

    fun clearSyncConnectionLogs() {
        remoteSyncService.historyManager.clearLogs()
    }

    fun simulateDebugFailureLog() {
        remoteSyncService.historyManager.simulateDebugFailureLog()
    }

    fun testServerConnection(onResult: (SyncConnectionLog) -> Unit) {
        viewModelScope.launch {
            val log = remoteSyncService.testServerConnection()
            onResult(log)
        }
    }

    fun syncNow(onResult: ((SyncResult) -> Unit)? = null) {
        viewModelScope.launch {
            val result = syncNowSuspend()
            onResult?.invoke(result)
        }
    }

    suspend fun syncNowSuspend(): SyncResult {
        if (!connectivityMonitor.isOnline()) {
            val msg = "Cannot sync: No active internet connection. Local SQLite records remain safely stored."
            _lastSyncMessage.value = msg
            return SyncResult.Offline(msg)
        }

        _isSyncing.value = true
        _lastSyncMessage.value = "Connecting to remote server..."

        return try {
            val pending = repository.getPendingTransactions()
            if (pending.isEmpty()) {
                val now = System.currentTimeMillis()
                val msg = "All local SQLite records are up to date with remote server."
                _lastSyncTimestamp.value = now
                _lastSyncMessage.value = msg
                remoteSyncService.saveSyncResult(now, msg)
                SyncResult.NoData(msg)
            } else {
                val result = remoteSyncService.pushTransactionsToRemote(pending)
                if (result is SyncResult.Success) {
                    val pushedIds = pending.map { it.id }
                    repository.markTransactionsAsSynced(pushedIds, result.timestamp)
                    _lastSyncTimestamp.value = result.timestamp
                    _lastSyncMessage.value = result.message
                } else if (result is SyncResult.Error) {
                    _lastSyncMessage.value = result.message
                }
                result
            }
        } catch (e: Exception) {
            val errorMsg = "Sync failed: ${e.localizedMessage ?: "Unknown network error"}"
            _lastSyncMessage.value = errorMsg
            SyncResult.Error(errorMsg, e)
        } finally {
            _isSyncing.value = false
        }
    }

    // Accessible shops for logged-in user:
    // - Admin: All shops
    // - RIC: Assigned RIC shops
    // - Employee: Assigned Employee shops
    val accessibleShops: StateFlow<List<ShopEntity>> = combine(
        _currentUser,
        allShops
    ) { user, shops ->
        if (user == null) {
            emptyList()
        } else {
            when (UserRole.fromCode(user.role)) {
                UserRole.ADMIN -> shops
                UserRole.RIC -> shops.filter {
                    it.assignedRicCode.equals(user.outletCode, ignoreCase = true) ||
                            user.assignedShopCodes == "*" ||
                            user.assignedShopCodes.split(",").map { c -> c.trim() }.contains(it.outletCode)
                }
                UserRole.SHOP_EMPLOYEE -> shops.filter {
                    it.assignedEmployeeCodes.split(",").map { c -> c.trim() }
                        .any { c -> c.equals(user.outletCode, ignoreCase = true) } ||
                            user.assignedShopCodes.split(",").map { c -> c.trim() }.contains(it.outletCode)
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected Active Shop
    private val _selectedShop = MutableStateFlow<ShopEntity?>(null)
    val selectedShop: StateFlow<ShopEntity?> = _selectedShop.asStateFlow()

    fun selectShop(shop: ShopEntity) {
        _selectedShop.value = shop
    }

    suspend fun loginSuspend(outletCode: String, credential: String): Boolean {
        _loginError.value = null
        if (outletCode.isBlank() || credential.isBlank()) {
            _loginError.value = "Please enter both Outlet Code and Password."
            return false
        }
        val user = repository.login(outletCode, credential)
        return if (user != null) {
            setCurrentLoggedInUser(user)
            true
        } else {
            _loginError.value = "Invalid Outlet Code or Password. Please check credentials."
            false
        }
    }

    private fun setCurrentLoggedInUser(user: UserEntity) {
        _currentUser.value = user
        _loginError.value = null
        _isOtpSent.value = false
        _lastGeneratedOtp.value = null
        _otpTargetUser.value = null
        _otpTargetMobile.value = ""

        val shops = allShops.value
        val initialShop = when (UserRole.fromCode(user.role)) {
            UserRole.ADMIN -> shops.firstOrNull()
            UserRole.RIC -> shops.firstOrNull { it.assignedRicCode.equals(user.outletCode, ignoreCase = true) } ?: shops.firstOrNull()
            UserRole.SHOP_EMPLOYEE -> shops.firstOrNull { it.assignedEmployeeCodes.contains(user.outletCode) } ?: shops.firstOrNull()
        }
        _selectedShop.value = initialShop
        _activeTab.value = ScreenTab.PURCHASES
    }

    /**
     * Initiates OTP login by looking up the user via entered mobile number.
     * Admin number is fixed to +91 8686122299 (configured by system),
     * RIC and Employees mobile numbers are configured by the Admin.
     */
    fun sendLoginOtp(mobileNumber: String, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            _loginError.value = null
            _isOtpLoading.value = true
            val trimmed = mobileNumber.trim()
            val digits = trimmed.filter { it.isDigit() }

            if (digits.length < 10) {
                _isOtpLoading.value = false
                val msg = "Please enter a valid 10-digit mobile number."
                _loginError.value = msg
                onResult(false, msg)
                return@launch
            }

            // Lookup user in repository
            var matchedUser = repository.getUserByMobile(trimmed)

            // Special check: If digits match Admin number 8686122299, ensure it resolves to Admin
            if (matchedUser == null && digits.endsWith("8686122299")) {
                val admin = repository.getUserByOutletCode("ADMIN")
                if (admin != null) {
                    matchedUser = admin
                }
            }

            if (matchedUser == null) {
                _isOtpLoading.value = false
                val msg = "No registered user found for mobile number +91 ${digits.takeLast(10)}. Please contact Admin."
                _loginError.value = msg
                onResult(false, msg)
                return@launch
            }

            // Generate and send OTP via OtpAuthService
            val otpCode = com.example.notification.OtpAuthService.sendOtp(
                context = getApplication(),
                mobileNumber = matchedUser.mobileNumber.ifBlank { trimmed },
                userName = matchedUser.name,
                userRole = matchedUser.role
            )

            _otpTargetMobile.value = matchedUser.mobileNumber.ifBlank { trimmed }
            _otpTargetUser.value = matchedUser
            _lastGeneratedOtp.value = otpCode
            _isOtpSent.value = true
            _isOtpLoading.value = false
            _loginError.value = null

            val successMsg = "OTP sent to +91 ${digits.takeLast(10)} for ${matchedUser.name} (${matchedUser.role})"
            onResult(true, successMsg)
        }
    }

    /**
     * Verifies entered OTP and logs the user in upon success.
     */
    fun verifyOtpAndLogin(enteredOtp: String, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            val user = _otpTargetUser.value
            val mobile = _otpTargetMobile.value
            if (user == null || mobile.isBlank()) {
                val msg = "Session expired. Please request a new OTP."
                _loginError.value = msg
                onResult(false, msg)
                return@launch
            }

            val isValid = com.example.notification.OtpAuthService.verifyOtp(mobile, enteredOtp)
            if (isValid) {
                setCurrentLoggedInUser(user)
                onResult(true, "Login successful as ${user.name}")
            } else {
                val msg = "Invalid or expired OTP. Please try again."
                _loginError.value = msg
                onResult(false, msg)
            }
        }
    }

    /**
     * Resends OTP to the currently active target user.
     */
    fun resendLoginOtp(onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        val mobile = _otpTargetMobile.value
        if (mobile.isNotBlank()) {
            sendLoginOtp(mobile, onResult)
        }
    }

    /**
     * Cancels the active OTP flow and returns to mobile number input screen.
     */
    fun cancelOtpFlow() {
        _isOtpSent.value = false
        _lastGeneratedOtp.value = null
        _otpTargetUser.value = null
        _otpTargetMobile.value = ""
        _loginError.value = null
        com.example.notification.OtpAuthService.clearOtp()
    }

    fun login(outletCode: String, mobileNumber: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val result = loginSuspend(outletCode, mobileNumber)
            onResult(result)
        }
    }

    fun quickLogin(role: UserRole) {
        viewModelScope.launch {
            val users = allUsers.value
            val user = users.find { it.role == role.name || it.role == role.code }
            if (user != null) {
                _currentUser.value = user
                _loginError.value = null
                val shops = allShops.value
                val targetShop = when (role) {
                    UserRole.ADMIN -> shops.firstOrNull()
                    UserRole.RIC -> shops.firstOrNull { it.assignedRicCode.equals(user.outletCode, ignoreCase = true) } ?: shops.firstOrNull()
                    UserRole.SHOP_EMPLOYEE -> shops.firstOrNull { it.assignedEmployeeCodes.contains(user.outletCode) } ?: shops.firstOrNull()
                }
                _selectedShop.value = targetShop
                _activeTab.value = ScreenTab.PURCHASES
            }
        }
    }

    fun logout() {
        _currentUser.value = null
        _selectedShop.value = null
        _loginError.value = null
        _activeTab.value = ScreenTab.PURCHASES
        biometricPreferences.clearSession()
        _isAdminBiometricUnlocked.value = false
    }

    // =========================================================================
    // --- Admin Biometric Security State ---
    // =========================================================================
    private val biometricPreferences = AdminBiometricPreferences(application)

    private val _isBiometricSecurityEnabled = MutableStateFlow(biometricPreferences.isBiometricLockEnabled)
    val isBiometricSecurityEnabled: StateFlow<Boolean> = _isBiometricSecurityEnabled.asStateFlow()

    private val _isAdminBiometricUnlocked = MutableStateFlow(false)
    val isAdminBiometricUnlocked: StateFlow<Boolean> = _isAdminBiometricUnlocked.asStateFlow()

    fun checkBiometricCapability(): BiometricCapability {
        return BiometricAuthManager.checkBiometricCapability(getApplication())
    }

    fun onBiometricAuthSuccess() {
        biometricPreferences.lastAuthenticatedTimestamp = System.currentTimeMillis()
        _isAdminBiometricUnlocked.value = true
    }

    fun lockAdminBiometric() {
        biometricPreferences.clearSession()
        _isAdminBiometricUnlocked.value = false
    }

    fun setBiometricSecurityEnabled(enabled: Boolean) {
        biometricPreferences.isBiometricLockEnabled = enabled
        _isBiometricSecurityEnabled.value = enabled
        if (!enabled) {
            // When disabled, access is immediately open
            _isAdminBiometricUnlocked.value = true
        }
    }

    // =========================================================================
    // --- Outlet Transactions & Tab-Specific Computations ---
    // =========================================================================
    val allOutletTransactions: StateFlow<List<OutletTransactionEntity>> = repository.allOutletTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shopOutletTransactions: StateFlow<List<OutletTransactionEntity>> = _selectedShop.flatMapLatest { shop ->
        if (shop == null) flowOf(emptyList())
        else repository.getTransactionsForShop(shop.outletCode)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Purchase Tab Total Value: Display Total value of (Purchase + 10% Margin + AROED)
    val purchaseTabTotalValue: StateFlow<Long> = shopOutletTransactions.combine(_selectedShop) { txs, _ ->
        txs.sumOf { it.purchaseTotalValue }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    // Sales Tab Total Value: Display Total Value of (Card sales + Cash Sales)
    val salesTabTotalValue: StateFlow<Long> = shopOutletTransactions.combine(_selectedShop) { txs, _ ->
        txs.sumOf { it.totalSales }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    // Bank Deposit Tab Total Value: Display Total Bank Deposit (Bank Deposit + Card Sales)
    val bankDepositTabTotalValue: StateFlow<Long> = shopOutletTransactions.combine(_selectedShop) { txs, _ ->
        txs.sumOf { it.totalBankDeposit }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    // Dedicated Tab Records StateFlows
    val shopPurchaseRecords: StateFlow<List<com.example.data.model.PurchaseRecordEntity>> = _selectedShop.flatMapLatest { shop ->
        if (shop == null) flowOf(emptyList())
        else repository.getPurchaseRecordsForShop(shop.outletCode)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shopSalesRecords: StateFlow<List<com.example.data.model.SalesRecordEntity>> = _selectedShop.flatMapLatest { shop ->
        if (shop == null) flowOf(emptyList())
        else repository.getSalesRecordsForShop(shop.outletCode)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shopBankDepositRecords: StateFlow<List<com.example.data.model.BankDepositRecordEntity>> = _selectedShop.flatMapLatest { shop ->
        if (shop == null) flowOf(emptyList())
        else repository.getBankDepositRecordsForShop(shop.outletCode)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun getPreviousClosingBalance(shopOutletCode: String, transactionDate: Long): Long? {
        return repository.getPreviousClosingBalance(shopOutletCode, transactionDate)
    }

    suspend fun getNextSlNo(shopOutletCode: String): Long {
        return repository.getNextSlNo(shopOutletCode)
    }

    fun submitOutletTransaction(
        shopOutletCode: String,
        slNo: Long,
        transactionDate: Long,
        openingBalance: Long,
        isOpeningBalanceManual: Boolean,
        purchase: Long,
        aroed: Long,
        cardSales: Long,
        cashSales: Long,
        damage: Long,
        bankDepositDate: Long,
        depositAmount: Long,
        challanPhotoUri: String,
        notes: String = "",
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            val user = _currentUser.value
            val userRole = UserRole.fromCode(user?.role ?: "")

            // RBAC Rule: Admin & RIC can create past date entries. Shop employee can only record today's entry.
            if (TransactionPermissionRules.isPastDate(transactionDate) && !TransactionPermissionRules.canSelectPastDateForNewEntry(userRole)) {
                _lastSyncMessage.value = "Unauthorized: Only Admin and RIC can create past date entries."
                return@launch
            }

            val margin10 = Math.round(purchase * 0.10)
            val totalValue = openingBalance + purchase + margin10 + aroed
            val totalSales = cardSales + cashSales
            val closingBalance = totalValue - totalSales - damage

            val entity = OutletTransactionEntity(
                shopOutletCode = shopOutletCode,
                slNo = slNo,
                transactionDate = transactionDate,
                openingBalance = openingBalance,
                isOpeningBalanceManual = isOpeningBalanceManual,
                purchase = purchase,
                margin10 = margin10,
                aroed = aroed,
                totalValue = totalValue,
                cardSales = cardSales,
                cashSales = cashSales,
                totalSales = totalSales,
                damage = damage,
                closingBalance = closingBalance,
                bankDepositDate = bankDepositDate,
                depositAmount = depositAmount,
                challanPhotoUri = challanPhotoUri,
                notes = notes,
                submittedBy = user?.name ?: "Unknown User",
                submittedByRole = user?.role ?: "SHOP_EMPLOYEE",
                createdAt = System.currentTimeMillis()
            )
            repository.insertOutletTransaction(entity)
            onComplete()
        }
    }

    /**
     * RBAC Rule: Admin and RIC can modify past date entries.
     * Shop employee can only view past records and cannot modify them.
     */
    fun updateOutletTransaction(
        updatedTx: OutletTransactionEntity,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val user = _currentUser.value
            val userRole = UserRole.fromCode(user?.role ?: "")

            if (!TransactionPermissionRules.canModifyEntry(userRole, updatedTx.transactionDate)) {
                _lastSyncMessage.value = "Unauthorized: Shop employees can only view past date records."
                return@launch
            }

            val entity = updatedTx.copy(
                syncStatus = "PENDING",
                submittedBy = user?.name ?: updatedTx.submittedBy,
                submittedByRole = user?.role ?: updatedTx.submittedByRole,
                createdAt = System.currentTimeMillis()
            )
            repository.updateOutletTransaction(entity)
            _lastSyncMessage.value = "Transaction #${entity.slNo} updated successfully."
            onComplete()
        }
    }

    fun updateDamage(txId: Long, newDamage: Long) {
        viewModelScope.launch {
            val tx = shopOutletTransactions.value.find { it.id == txId } ?: return@launch
            val user = _currentUser.value
            val userRole = UserRole.fromCode(user?.role ?: "")

            if (!TransactionPermissionRules.canModifyEntry(userRole, tx.transactionDate)) {
                _lastSyncMessage.value = "Unauthorized: Shop employees can only view past date records."
                return@launch
            }

            val newClosing = tx.totalValue - tx.totalSales - newDamage
            repository.updateDamage(txId, newDamage, newClosing)
        }
    }

    fun deleteOutletTransaction(tx: OutletTransactionEntity) {
        viewModelScope.launch {
            val user = _currentUser.value
            val userRole = UserRole.fromCode(user?.role ?: "")
            if (!TransactionPermissionRules.canModifyEntry(userRole, tx.transactionDate)) {
                _lastSyncMessage.value = "Unauthorized: Only Admin and RIC can modify or delete past date records."
                return@launch
            }
            repository.deleteOutletTransaction(tx)
        }
    }

    /**
     * Records or updates daily sales totals with explicit fields for cash vs card payments and timestamps,
     * persisting data directly to local storage via Room.
     */
    fun recordDailySales(
        shopOutletCode: String,
        transactionDate: Long,
        cashSales: Long,
        cardSales: Long,
        notes: String = "",
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val user = _currentUser.value
                val userRole = UserRole.fromCode(user?.role ?: "")
                val startOfDay = IndianCurrencyUtils.getStartOfDay(transactionDate)

                // Check past date permissions
                if (TransactionPermissionRules.isPastDate(startOfDay) &&
                    !TransactionPermissionRules.canSelectPastDateForNewEntry(userRole)
                ) {
                    _lastSyncMessage.value = "Unauthorized: Only Admin and RIC can record past date entries."
                    onComplete(false)
                    return@launch
                }

                val existingList = repository.getTransactionsForShop(shopOutletCode).first()
                val existing = existingList.find { IndianCurrencyUtils.isSameDay(it.transactionDate, startOfDay) }

                val totalSales = cashSales + cardSales
                if (existing != null) {
                    val closingBalance = existing.totalValue - totalSales - existing.damage
                    val updated = existing.copy(
                        cashSales = cashSales,
                        cardSales = cardSales,
                        totalSales = totalSales,
                        closingBalance = closingBalance,
                        notes = if (notes.isNotBlank()) notes else existing.notes,
                        submittedBy = user?.name ?: existing.submittedBy,
                        submittedByRole = user?.role ?: existing.submittedByRole,
                        createdAt = transactionDate,
                        syncStatus = "PENDING"
                    )
                    repository.updateOutletTransaction(updated)
                } else {
                    val nextSl = repository.getNextSlNo(shopOutletCode)
                    val prevClosing = repository.getPreviousClosingBalance(shopOutletCode, startOfDay) ?: 0L
                    val totalValue = prevClosing
                    val closingBalance = totalValue - totalSales

                    val newTx = OutletTransactionEntity(
                        shopOutletCode = shopOutletCode,
                        slNo = nextSl,
                        transactionDate = startOfDay,
                        openingBalance = prevClosing,
                        isOpeningBalanceManual = false,
                        purchase = 0L,
                        margin10 = 0L,
                        aroed = 0L,
                        totalValue = totalValue,
                        cardSales = cardSales,
                        cashSales = cashSales,
                        totalSales = totalSales,
                        damage = 0L,
                        closingBalance = closingBalance,
                        bankDepositDate = IndianCurrencyUtils.getStartOfDay(startOfDay + 86400000L),
                        depositAmount = 0L,
                        challanPhotoUri = "",
                        notes = notes,
                        submittedBy = user?.name ?: "Staff",
                        submittedByRole = user?.role ?: "SHOP_EMPLOYEE",
                        createdAt = transactionDate,
                        syncStatus = "PENDING"
                    )
                    repository.insertOutletTransaction(newTx)
                }

                // Also store granular record in Room's sales table
                val saleEntity = SaleEntity(
                    shopId = shopOutletCode,
                    date = transactionDate,
                    customerName = "Daily Counter Sales",
                    invoiceNo = "DS-${shopOutletCode}-${startOfDay / 1000}",
                    itemsSummary = "Cash: ₹$cashSales | Card: ₹$cardSales",
                    amount = totalSales.toDouble(),
                    gstRate = 0.0,
                    paymentMode = if (cashSales > 0 && cardSales > 0) "SPLIT" else if (cardSales > 0) "CARD" else "CASH",
                    paymentStatus = "PAID",
                    notes = notes
                )
                repository.insertSale(saleEntity)

                _lastSyncMessage.value = "Daily sales of ${IndianCurrencyUtils.formatInr(totalSales)} recorded successfully to Room database."
                onComplete(true)
            } catch (e: Exception) {
                e.printStackTrace()
                _lastSyncMessage.value = "Failed to record daily sales: ${e.localizedMessage}"
                onComplete(false)
            }
        }
    }

    // =========================================================================
    // --- Admin Operations: Add Shops & Assign Shops to RIC / Employee ---
    // =========================================================================
    fun addShop(
        outletCode: String,
        shopName: String,
        address: String,
        assignedRicCode: String,
        assignedEmployeeCodes: String,
        initialOpeningBalance: Long,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            val user = _currentUser.value
            val userRole = UserRole.fromCode(user?.role ?: "")
            if (!TransactionPermissionRules.canAddOutlet(userRole)) {
                _lastSyncMessage.value = "Unauthorized: Only Admin can add outlets."
                return@launch
            }

            val shop = ShopEntity(
                outletCode = outletCode.trim().uppercase(),
                shopName = shopName.trim(),
                address = address.trim(),
                assignedRicCode = assignedRicCode.trim(),
                assignedEmployeeCodes = assignedEmployeeCodes.trim(),
                initialOpeningBalance = initialOpeningBalance,
                createdAt = System.currentTimeMillis()
            )
            repository.addShop(shop)
            // If current user is Admin, auto-select this new shop
            if (_currentUser.value?.role == UserRole.ADMIN.name) {
                _selectedShop.value = shop
            }
            onComplete()
        }
    }

    fun assignShop(
        shop: ShopEntity,
        newRicCode: String,
        newEmployeeCodes: String,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            val user = _currentUser.value
            val userRole = UserRole.fromCode(user?.role ?: "")
            if (!TransactionPermissionRules.canAssignRic(userRole)) {
                _lastSyncMessage.value = "Unauthorized: Only Admin can assign RIC to outlets."
                return@launch
            }

            val updated = shop.copy(
                assignedRicCode = newRicCode.trim(),
                assignedEmployeeCodes = newEmployeeCodes.trim()
            )
            repository.updateShop(updated)
            if (_selectedShop.value?.id == shop.id) {
                _selectedShop.value = updated
            }
            onComplete()
        }
    }

    fun updateShopDetails(
        shop: ShopEntity,
        newShopName: String,
        newAddress: String,
        newRicCode: String,
        newEmployeeCodes: String,
        newOpeningBalance: Long,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val user = _currentUser.value
            val userRole = UserRole.fromCode(user?.role ?: "")
            if (!TransactionPermissionRules.canAssignRic(userRole)) {
                _lastSyncMessage.value = "Unauthorized: Only Admin can modify outlet details."
                return@launch
            }

            val updated = shop.copy(
                shopName = newShopName.trim(),
                address = newAddress.trim(),
                assignedRicCode = newRicCode.trim(),
                assignedEmployeeCodes = newEmployeeCodes.trim(),
                initialOpeningBalance = newOpeningBalance
            )
            repository.updateShop(updated)
            if (_selectedShop.value?.id == shop.id) {
                _selectedShop.value = updated
            }
            _lastSyncMessage.value = "Outlet '${updated.shopName}' updated successfully."
            onComplete()
        }
    }

    fun addUser(
        outletCode: String,
        name: String,
        mobileNumber: String,
        role: UserRole,
        assignedShopCodes: String,
        password: String = "",
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            val user = _currentUser.value
            val userRole = UserRole.fromCode(user?.role ?: "")
            if (!TransactionPermissionRules.canSetPassword(userRole)) {
                _lastSyncMessage.value = "Unauthorized: Only Admin can create staff accounts and set initial passwords."
                return@launch
            }

            val newUser = UserEntity(
                outletCode = outletCode.trim().uppercase(),
                name = name.trim(),
                mobileNumber = mobileNumber.trim(),
                password = password.trim(),
                role = role.name,
                assignedShopCodes = assignedShopCodes.trim()
            )
            repository.addUser(newUser)
            onComplete()
        }
    }

    fun deleteShop(shop: ShopEntity, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val user = _currentUser.value
            val userRole = UserRole.fromCode(user?.role ?: "")
            if (!TransactionPermissionRules.canDeleteOutlet(userRole)) {
                _lastSyncMessage.value = "Unauthorized: Only Admin can delete outlets."
                return@launch
            }

            try {
                repository.deleteShop(shop)

                // Clean up assignedShopCodes across users
                val currentUsers = repository.allUsers.first()
                for (u in currentUsers) {
                    if (u.role != UserRole.ADMIN.name && u.assignedShopCodes != "*") {
                        val codes = u.assignedShopCodes.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        if (codes.any { it.equals(shop.outletCode, ignoreCase = true) }) {
                            val newCodes = codes.filterNot { it.equals(shop.outletCode, ignoreCase = true) }.joinToString(",")
                            repository.updateUser(u.copy(assignedShopCodes = newCodes))
                        }
                    }
                }

                // If deleted shop was selected, switch to another shop
                if (_selectedShop.value?.id == shop.id) {
                    val remainingShops = repository.allShops.first()
                    _selectedShop.value = remainingShops.firstOrNull()
                }

                _lastSyncMessage.value = "Outlet '${shop.shopName}' (${shop.outletCode}) deleted successfully."
            } catch (e: Exception) {
                _lastSyncMessage.value = "Failed to delete outlet: ${e.message}"
            } finally {
                onComplete()
            }
        }
    }

    fun deleteStaff(userToDelete: UserEntity, replacementCode: String? = null, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val currentUser = _currentUser.value
            val currentRole = UserRole.fromCode(currentUser?.role ?: "")
            if (!TransactionPermissionRules.canDeleteStaff(currentRole)) {
                _lastSyncMessage.value = "Unauthorized: Only Admin can delete staff accounts."
                return@launch
            }

            if (userToDelete.role == UserRole.ADMIN.name) {
                _lastSyncMessage.value = "Security Alert: Admin root account cannot be deleted."
                return@launch
            }

            try {
                val isRic = userToDelete.role == UserRole.RIC.name || userToDelete.role == UserRole.RIC.code
                val isEmp = userToDelete.role == UserRole.SHOP_EMPLOYEE.name || userToDelete.role == UserRole.SHOP_EMPLOYEE.code
                val targetReplacement = replacementCode?.trim().orEmpty()

                if (isRic) {
                    repository.reassignRicInShops(userToDelete.outletCode, targetReplacement)
                } else if (isEmp) {
                    repository.reassignEmployeeInShops(userToDelete.outletCode, targetReplacement)
                }

                repository.deleteUser(userToDelete)
                val reassignedMsg = if (targetReplacement.isNotBlank()) " and reassigned to $targetReplacement" else ""
                _lastSyncMessage.value = "Staff account '${userToDelete.name}' (${userToDelete.outletCode}) deleted$reassignedMsg."
            } catch (e: Exception) {
                _lastSyncMessage.value = "Failed to delete staff: ${e.message}"
            } finally {
                onComplete()
            }
        }
    }

    fun reassignStaffOutlets(staffUser: UserEntity, newShopCodes: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val currentUser = _currentUser.value
            val currentRole = UserRole.fromCode(currentUser?.role ?: "")
            if (!TransactionPermissionRules.canReassignStaff(currentRole)) {
                _lastSyncMessage.value = "Unauthorized: Only Admin can reassign staff outlets."
                return@launch
            }

            try {
                val cleanedShopCodes = newShopCodes.split(",")
                    .map { it.trim().uppercase() }
                    .filter { it.isNotEmpty() }
                    .joinToString(",")

                // Update user entity
                repository.updateUser(staffUser.copy(assignedShopCodes = cleanedShopCodes))

                // Update corresponding shops
                val allShops = repository.allShops.first()
                val selectedSet = cleanedShopCodes.split(",").map { it.trim().uppercase() }.toSet()
                val isRic = staffUser.role == UserRole.RIC.name || staffUser.role == UserRole.RIC.code
                val isEmp = staffUser.role == UserRole.SHOP_EMPLOYEE.name || staffUser.role == UserRole.SHOP_EMPLOYEE.code

                for (shop in allShops) {
                    val codeUpper = shop.outletCode.uppercase()
                    if (selectedSet.contains(codeUpper)) {
                        // Assign to this staff member
                        if (isRic && !shop.assignedRicCode.equals(staffUser.outletCode, ignoreCase = true)) {
                            repository.updateShop(shop.copy(assignedRicCode = staffUser.outletCode))
                        } else if (isEmp && !shop.assignedEmployeeCodes.equals(staffUser.outletCode, ignoreCase = true)) {
                            repository.updateShop(shop.copy(assignedEmployeeCodes = staffUser.outletCode))
                        }
                    } else {
                        // If this shop was previously assigned to this user, unassign them
                        if (isRic && shop.assignedRicCode.equals(staffUser.outletCode, ignoreCase = true)) {
                            repository.updateShop(shop.copy(assignedRicCode = ""))
                        } else if (isEmp && shop.assignedEmployeeCodes.equals(staffUser.outletCode, ignoreCase = true)) {
                            repository.updateShop(shop.copy(assignedEmployeeCodes = ""))
                        }
                    }
                }

                _lastSyncMessage.value = "Outlets successfully reassigned for ${staffUser.name}."
            } catch (e: Exception) {
                _lastSyncMessage.value = "Failed to reassign outlets: ${e.message}"
            } finally {
                onComplete()
            }
        }
    }

    suspend fun updateUserPasswordSuspend(userId: Long, newPassword: String) {
        val user = _currentUser.value
        val userRole = UserRole.fromCode(user?.role ?: "")
        val isSelf = user?.id == userId
        if (!TransactionPermissionRules.canSetPassword(userRole) && !isSelf) {
            _lastSyncMessage.value = "Unauthorized: Only Admin can set passwords for RIC and Shop Employees."
            return
        }
        repository.updateUserPassword(userId, newPassword.trim())
    }

    suspend fun updateStaffCredentialsSuspend(userId: Long, mobileNumber: String, password: String) {
        val user = _currentUser.value
        val userRole = UserRole.fromCode(user?.role ?: "")
        if (!TransactionPermissionRules.canSetPassword(userRole)) {
            _lastSyncMessage.value = "Unauthorized: Only Admin can set mobile numbers and passwords for RIC and Shop Employees."
            return
        }
        repository.updateUserMobileAndPassword(userId, mobileNumber.trim(), password.trim())
    }

    fun updateStaffCredentials(
        userId: Long,
        mobileNumber: String,
        password: String,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                updateStaffCredentialsSuspend(userId, mobileNumber, password)
            } finally {
                onComplete()
            }
        }
    }

    suspend fun updateUserPasswordByOutletCodeSuspend(outletCode: String, newPassword: String) {
        val user = _currentUser.value
        val userRole = UserRole.fromCode(user?.role ?: "")
        if (!TransactionPermissionRules.canSetPassword(userRole)) {
            _lastSyncMessage.value = "Unauthorized: Only Admin can set passwords for RIC and Shop Employees."
            return
        }
        repository.updateUserPasswordByOutletCode(outletCode.trim(), newPassword.trim())
    }

    fun updateUserPassword(
        userId: Long,
        newPassword: String,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                updateUserPasswordSuspend(userId, newPassword)
            } finally {
                onComplete()
            }
        }
    }

    fun updateUserPasswordByOutletCode(
        outletCode: String,
        newPassword: String,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                updateUserPasswordByOutletCodeSuspend(outletCode, newPassword)
            } finally {
                onComplete()
            }
        }
    }

    // =========================================================================
    // --- Navigation & Filter States ---
    // =========================================================================
    private val _selectedDate = MutableStateFlow(System.currentTimeMillis())
    val selectedDate: StateFlow<Long> = _selectedDate.asStateFlow()

    private val _activeTab = MutableStateFlow(ScreenTab.PURCHASES)
    val activeTab: StateFlow<ScreenTab> = _activeTab.asStateFlow()

    fun selectTab(tab: ScreenTab) {
        _activeTab.value = tab
    }

    fun selectDate(timestamp: Long) {
        _selectedDate.value = timestamp
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _paymentFilter = MutableStateFlow("ALL")
    val paymentFilter: StateFlow<String> = _paymentFilter.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setPaymentFilter(filter: String) {
        _paymentFilter.value = filter
    }

    // Retained for Overview / Reconciliation if needed
    private val _openingCash = MutableStateFlow(5000.0)
    val openingCash: StateFlow<Double> = _openingCash.asStateFlow()

    private val _denominationTally = MutableStateFlow(DenominationTally())
    val denominationTally: StateFlow<DenominationTally> = _denominationTally.asStateFlow()

    val allSales: StateFlow<List<SaleEntity>> = repository.allSales
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPurchases: StateFlow<List<PurchaseEntity>> = repository.allPurchases
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDeposits: StateFlow<List<BankDepositEntity>> = repository.allDeposits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allReconciliations: StateFlow<List<DailyReconciliationEntity>> = repository.allReconciliations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val selectedDayRange = _selectedDate.flatMapLatest { date ->
        val start = IndianCurrencyUtils.getStartOfDay(date)
        val end = IndianCurrencyUtils.getEndOfDay(date)
        combine(
            repository.getSalesForRange(start, end),
            repository.getPurchasesForRange(start, end),
            repository.getDepositsForRange(start, end),
            repository.getReconciliationForDay(start)
        ) { sales, purchases, deposits, recon ->
            DailyLedgerData(start, end, sales, purchases, deposits, recon)
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DailyLedgerData(
            IndianCurrencyUtils.getStartOfDay(System.currentTimeMillis()),
            IndianCurrencyUtils.getEndOfDay(System.currentTimeMillis()),
            emptyList(),
            emptyList(),
            emptyList(),
            null
        )
    )

    val selectedDaySales: StateFlow<List<SaleEntity>> = combine(selectedDayRange, _searchQuery, _paymentFilter) { data, query, filter ->
        data.sales.filter { sale ->
            val matchesQuery = query.isBlank() ||
                    sale.customerName.contains(query, ignoreCase = true) ||
                    sale.invoiceNo.contains(query, ignoreCase = true) ||
                    sale.itemsSummary.contains(query, ignoreCase = true)
            val matchesFilter = filter == "ALL" || sale.paymentMode.equals(filter, ignoreCase = true)
            matchesQuery && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedDayPurchases: StateFlow<List<PurchaseEntity>> = combine(selectedDayRange, _searchQuery, _paymentFilter) { data, query, filter ->
        data.purchases.filter { purchase ->
            val matchesQuery = query.isBlank() ||
                    purchase.vendorName.contains(query, ignoreCase = true) ||
                    purchase.invoiceNo.contains(query, ignoreCase = true) ||
                    purchase.category.contains(query, ignoreCase = true)
            val matchesFilter = filter == "ALL" || purchase.paymentMode.equals(filter, ignoreCase = true)
            matchesQuery && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedDayDeposits: StateFlow<List<BankDepositEntity>> = combine(selectedDayRange, _searchQuery) { data, query ->
        data.deposits.filter { dep ->
            query.isBlank() ||
                    dep.bankName.contains(query, ignoreCase = true) ||
                    dep.referenceNumber.contains(query, ignoreCase = true) ||
                    dep.accountNumberLast4.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailyReconciliationSummary: StateFlow<DailyReconciliationSummary> = combine(
        selectedDayRange,
        _openingCash,
        _denominationTally
    ) { data, opening, tally ->
        val countedCash = tally.calculateTotal()
        ReconciliationEngine.computeDailySummary(
            dateEpochStart = data.startOfDay,
            openingCash = opening,
            sales = data.sales,
            purchases = data.purchases,
            deposits = data.deposits,
            actualCountedCash = countedCash,
            existingReconciliation = data.savedReconciliation
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DailyReconciliationSummary(
            dateEpochStart = IndianCurrencyUtils.getStartOfDay(System.currentTimeMillis()),
            openingCash = 5000.0,
            totalSalesAmount = 0.0,
            totalCashSales = 0.0,
            totalDigitalSales = 0.0,
            totalCreditSales = 0.0,
            salesCount = 0,
            totalPurchasesAmount = 0.0,
            totalCashPurchases = 0.0,
            totalBankPurchases = 0.0,
            totalCreditPurchases = 0.0,
            purchasesCount = 0,
            totalDepositsAmount = 0.0,
            totalCashDepositedToBank = 0.0,
            totalDigitalSettlementsToBank = 0.0,
            depositsCount = 0,
            expectedClosingCash = 5000.0,
            actualCountedCash = 0.0,
            discrepancy = 0.0,
            status = ReconciliationStatus.PENDING,
            expectedBankInflow = 0.0,
            netBankMovement = 0.0
        )
    )

    fun setOpeningCash(amount: Double) { _openingCash.value = amount }
    fun updateDenominationTally(tally: DenominationTally) { _denominationTally.value = tally }
    fun resetDenominations() { _denominationTally.value = DenominationTally() }

    fun addSale(name: String, inv: String, items: String, amt: Double, gst: Double, mode: String, status: String, notes: String) {
        viewModelScope.launch {
            val sale = SaleEntity(
                date = _selectedDate.value,
                customerName = name.ifBlank { "Counter Customer" },
                invoiceNo = inv.ifBlank { "BILL-${System.currentTimeMillis() % 10000}" },
                itemsSummary = items.ifBlank { "General Items" },
                amount = amt,
                gstRate = gst,
                paymentMode = mode,
                paymentStatus = status,
                notes = notes
            )
            repository.insertSale(sale)
        }
    }

    fun deleteSale(sale: SaleEntity) {
        viewModelScope.launch { repository.deleteSale(sale) }
    }

    fun addPurchase(vendor: String, inv: String, cat: String, amt: Double, gst: Double, mode: String, status: String, notes: String) {
        viewModelScope.launch {
            val p = PurchaseEntity(
                date = _selectedDate.value,
                vendorName = vendor.ifBlank { "Vendor / Supplier" },
                invoiceNo = inv.ifBlank { "INV-${System.currentTimeMillis() % 10000}" },
                category = cat,
                amount = amt,
                gstRate = gst,
                paymentMode = mode,
                paymentStatus = status,
                notes = notes
            )
            repository.insertPurchase(p)
        }
    }

    fun deletePurchase(p: PurchaseEntity) {
        viewModelScope.launch { repository.deletePurchase(p) }
    }

    fun addDeposit(bank: String, acc: String, amt: Double, src: String, ref: String, notesBreakdown: String, notes: String) {
        viewModelScope.launch {
            val d = BankDepositEntity(
                date = _selectedDate.value,
                bankName = bank.ifBlank { "State Bank of India" },
                accountNumberLast4 = acc.ifBlank { "0000" },
                amount = amt,
                depositSource = src,
                referenceNumber = ref.ifBlank { "DEP-${System.currentTimeMillis() % 10000}" },
                denominationBreakdown = notesBreakdown,
                notes = notes
            )
            repository.insertDeposit(d)
        }
    }

    fun deleteDeposit(d: BankDepositEntity) {
        viewModelScope.launch { repository.deleteDeposit(d) }
    }

    fun confirmAndLockDailyReconciliation(auditorName: String, notes: String) {
        viewModelScope.launch {
            val summary = dailyReconciliationSummary.value
            val tally = _denominationTally.value
            val tallyString = tally.toSummaryString()
            val entity = DailyReconciliationEntity(
                id = summary.existingReconciliation?.id ?: 0,
                dateEpochStart = summary.dateEpochStart,
                openingCash = summary.openingCash,
                totalCashSales = summary.totalCashSales,
                totalDigitalSales = summary.totalDigitalSales,
                totalCashPurchases = summary.totalCashPurchases,
                totalBankPurchases = summary.totalBankPurchases,
                totalCashDepositedToBank = summary.totalCashDepositedToBank,
                expectedClosingCash = summary.expectedClosingCash,
                actualCountedCash = summary.actualCountedCash,
                discrepancy = summary.discrepancy,
                expectedBankInflow = summary.expectedBankInflow,
                status = summary.status.name,
                denominationTallyJson = tallyString,
                reconciledAt = System.currentTimeMillis(),
                auditorName = auditorName.ifBlank { "Store Auditor" },
                notes = notes
            )
            repository.saveReconciliation(entity)
        }
    }

    fun unlockOrDeleteReconciliation(recon: DailyReconciliationEntity) {
        viewModelScope.launch { repository.deleteReconciliation(recon) }
    }

    // --- Dedicated Tab Records ---
    fun savePurchaseRecord(record: PurchaseRecordEntity, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                repository.insertPurchaseRecord(record)
                onComplete?.invoke(true)
            } catch (e: Exception) {
                onComplete?.invoke(false)
            }
        }
    }

    fun deletePurchaseRecord(record: PurchaseRecordEntity) {
        viewModelScope.launch {
            repository.deletePurchaseRecord(record)
        }
    }

    fun saveSalesRecord(record: SalesRecordEntity, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                repository.insertSalesRecord(record)
                onComplete?.invoke(true)
            } catch (e: Exception) {
                onComplete?.invoke(false)
            }
        }
    }

    fun deleteSalesRecord(record: SalesRecordEntity) {
        viewModelScope.launch {
            repository.deleteSalesRecord(record)
        }
    }

    fun saveBankDepositRecord(record: BankDepositRecordEntity, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                repository.insertBankDepositRecord(record)
                onComplete?.invoke(true)
            } catch (e: Exception) {
                onComplete?.invoke(false)
            }
        }
    }

    fun deleteBankDepositRecord(record: BankDepositRecordEntity) {
        viewModelScope.launch {
            repository.deleteBankDepositRecord(record)
        }
    }
}

data class DailyLedgerData(
    val startOfDay: Long,
    val endOfDay: Long,
    val sales: List<SaleEntity>,
    val purchases: List<PurchaseEntity>,
    val deposits: List<BankDepositEntity>,
    val savedReconciliation: DailyReconciliationEntity?
)
