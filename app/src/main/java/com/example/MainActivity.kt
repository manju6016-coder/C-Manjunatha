package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.OutletTransactionEntity
import com.example.data.model.UserRole
import com.example.notification.DailyReportReminderManager
import com.example.ui.components.AddOutletTransactionDialog
import com.example.ui.components.DailyReminderDialog
import com.example.ui.components.DatabaseBackupDialog
import com.example.ui.components.SyncStatusDialog
import com.example.ui.components.SyncTopBarIndicator
import com.example.ui.screens.AdminShopsScreen
import com.example.ui.screens.BankDepositTabScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.PurchaseTabScreen
import com.example.ui.screens.SalesTabScreen
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.PrimaryIndigoDark
import com.example.ui.theme.SaffronGold
import com.example.ui.viewmodel.LedgerViewModel
import com.example.ui.viewmodel.ScreenTab
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: LedgerViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return LedgerViewModel(application) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize local notification channel and restore scheduled reminders
        DailyReportReminderManager.createNotificationChannel(applicationContext)
        if (DailyReportReminderManager.isReminderEnabled(applicationContext)) {
            val hour = DailyReportReminderManager.getScheduledHour(applicationContext)
            val minute = DailyReportReminderManager.getScheduledMinute(applicationContext)
            DailyReportReminderManager.scheduleDailyReminder(applicationContext, hour, minute, true)
        }

        setContent {
            MyApplicationTheme {
                LedgerReconApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerReconApp(viewModel: LedgerViewModel) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val loginError by viewModel.loginError.collectAsStateWithLifecycle()
    val isOtpSent by viewModel.isOtpSent.collectAsStateWithLifecycle()
    val otpTargetMobile by viewModel.otpTargetMobile.collectAsStateWithLifecycle()
    val otpTargetUser by viewModel.otpTargetUser.collectAsStateWithLifecycle()
    val lastGeneratedOtp by viewModel.lastGeneratedOtp.collectAsStateWithLifecycle()
    val isOtpLoading by viewModel.isOtpLoading.collectAsStateWithLifecycle()

    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val selectedShop by viewModel.selectedShop.collectAsStateWithLifecycle()
    val accessibleShops by viewModel.accessibleShops.collectAsStateWithLifecycle()
    val allShops by viewModel.allShops.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()

    val shopTransactions by viewModel.shopOutletTransactions.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allOutletTransactions.collectAsStateWithLifecycle()
    val purchaseTabTotal by viewModel.purchaseTabTotalValue.collectAsStateWithLifecycle()
    val salesTabTotal by viewModel.salesTabTotalValue.collectAsStateWithLifecycle()
    val bankDepositTabTotal by viewModel.bankDepositTabTotalValue.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val syncConnectionLogs by viewModel.syncConnectionLogs.collectAsStateWithLifecycle()

    var showAddEntryDialog by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<OutletTransactionEntity?>(null) }
    var initialDialogSection by remember { mutableIntStateOf(0) }
    var isShopMenuExpanded by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var showSyncDialog by remember { mutableStateOf(false) }

    // If user is not logged in, display the Login Screen
    if (currentUser == null) {
        LoginScreen(
            loginError = loginError,
            isOtpSent = isOtpSent,
            otpTargetMobile = otpTargetMobile,
            otpTargetUser = otpTargetUser,
            lastGeneratedOtp = lastGeneratedOtp,
            isOtpLoading = isOtpLoading,
            onSendOtp = { mobile -> viewModel.sendLoginOtp(mobile) },
            onVerifyOtp = { code -> viewModel.verifyOtpAndLogin(code) },
            onResendOtp = { viewModel.resendLoginOtp() },
            onCancelOtp = { viewModel.cancelOtpFlow() },
            onLogin = { code, mobile -> viewModel.login(code, mobile) },
            onQuickLogin = { role -> viewModel.quickLogin(role) }
        )
        return
    }

    val user = currentUser!!
    val isAdmin = UserRole.fromCode(user.role) == UserRole.ADMIN

    // Add Entry Dialog
    if (showAddEntryDialog && selectedShop != null) {
        AddOutletTransactionDialog(
            shop = selectedShop!!,
            userRole = user.role,
            initialSectionIndex = initialDialogSection,
            initialTransaction = editingTransaction,
            onDismiss = {
                showAddEntryDialog = false
                editingTransaction = null
            },
            onFetchPreviousClosingBalance = { shopCode, date ->
                viewModel.getPreviousClosingBalance(shopCode, date)
            },
            onFetchNextSlNo = { shopCode ->
                viewModel.getNextSlNo(shopCode)
            },
            onUpdate = { updatedTx ->
                viewModel.updateOutletTransaction(updatedTx) {
                    showAddEntryDialog = false
                    editingTransaction = null
                }
            },
            onSubmit = { code, sl, date, openBal, isManual, pur, aroed, card, cash, dmg, depDate, depAmt, photo, notes ->
                viewModel.submitOutletTransaction(
                    shopOutletCode = code,
                    slNo = sl,
                    transactionDate = date,
                    openingBalance = openBal,
                    isOpeningBalanceManual = isManual,
                    purchase = pur,
                    aroed = aroed,
                    cardSales = card,
                    cashSales = cash,
                    damage = dmg,
                    bankDepositDate = depDate,
                    depositAmount = depAmt,
                    challanPhotoUri = photo,
                    notes = notes,
                    onComplete = {
                        showAddEntryDialog = false
                        editingTransaction = null
                    }
                )
            }
        )
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PrimaryIndigoDark)
                    .statusBarsPadding()
            ) {
                TopAppBar(
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // User Info / Badge
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            when (UserRole.fromCode(user.role)) {
                                                UserRole.ADMIN -> Color(0xFF3B82F6)
                                                UserRole.RIC -> SaffronGold
                                                UserRole.SHOP_EMPLOYEE -> EmeraldGreen
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = user.name.take(1).uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = user.name,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${UserRole.fromCode(user.role).displayName} • ${user.outletCode}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // SQLite Sync Indicator Pill Button
                                SyncTopBarIndicator(
                                    syncState = syncState,
                                    onClick = { showSyncDialog = true }
                                )
                                Spacer(modifier = Modifier.width(4.dp))

                                // Daily Report Reminder Notification Button
                                IconButton(
                                    onClick = { showReminderDialog = true },
                                    modifier = Modifier.testTag("btn_topbar_reminder")
                                ) {
                                    Icon(
                                        Icons.Default.NotificationsActive,
                                        contentDescription = "Daily Report Reminder",
                                        tint = Color(0xFFCBD5E1)
                                    )
                                }

                                if (isAdmin) {
                                    IconButton(
                                        onClick = { showBackupDialog = true },
                                        modifier = Modifier.testTag("btn_topbar_backup")
                                    ) {
                                        Icon(
                                            Icons.Default.Storage,
                                            contentDescription = "Database Backup",
                                            tint = Color(0xFFCBD5E1)
                                        )
                                    }
                                }

                                // Logout Button
                                IconButton(
                                    onClick = { viewModel.logout() },
                                    modifier = Modifier.testTag("btn_logout")
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Logout,
                                        contentDescription = "Logout",
                                        tint = Color(0xFFCBD5E1)
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = PrimaryIndigoDark,
                        titleContentColor = Color.White
                    )
                )

                // Shop Selector Bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF1E293B)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Storefront,
                                contentDescription = null,
                                tint = EmeraldGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Outlet:",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF94A3B8)
                            )
                            Spacer(modifier = Modifier.width(6.dp))

                            Box {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { isShopMenuExpanded = true }
                                        .background(Color(0xFF0F172A))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                        .testTag("shop_selector_dropdown")
                                ) {
                                    Text(
                                        text = selectedShop?.let { "${it.shopName} (${it.outletCode})" } ?: "Select Shop",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = isShopMenuExpanded,
                                    onDismissRequest = { isShopMenuExpanded = false }
                                ) {
                                    accessibleShops.forEach { shop ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(
                                                        text = shop.shopName,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = "Outlet Code: ${shop.outletCode}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            },
                                            onClick = {
                                                viewModel.selectShop(shop)
                                                isShopMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                // Purchase Tab
                NavigationBarItem(
                    selected = activeTab == ScreenTab.PURCHASES,
                    onClick = { viewModel.selectTab(ScreenTab.PURCHASES) },
                    icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Purchase Tab") },
                    label = { Text("Purchase") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryIndigo,
                        selectedTextColor = PrimaryIndigo,
                        indicatorColor = PrimaryIndigo.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_tab_purchase")
                )

                // Sales Tab
                NavigationBarItem(
                    selected = activeTab == ScreenTab.SALES,
                    onClick = { viewModel.selectTab(ScreenTab.SALES) },
                    icon = { Icon(Icons.Default.PointOfSale, contentDescription = "Sales Tab") },
                    label = { Text("Sales") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EmeraldGreen,
                        selectedTextColor = EmeraldGreen,
                        indicatorColor = EmeraldGreen.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_tab_sales")
                )

                // Bank Deposit Tab
                NavigationBarItem(
                    selected = activeTab == ScreenTab.BANK_DEPOSITS,
                    onClick = { viewModel.selectTab(ScreenTab.BANK_DEPOSITS) },
                    icon = { Icon(Icons.Default.AccountBalance, contentDescription = "Bank Deposit Tab") },
                    label = { Text("Bank Deposit") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SaffronGold,
                        selectedTextColor = SaffronGold,
                        indicatorColor = SaffronGold.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_tab_bank_deposit")
                )

                // Admin Manage Shops Tab (Only visible to Admin)
                if (isAdmin) {
                    NavigationBarItem(
                        selected = activeTab == ScreenTab.ADMIN_SHOPS,
                        onClick = { viewModel.selectTab(ScreenTab.ADMIN_SHOPS) },
                        icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = "Manage Shops") },
                        label = { Text("Manage Shops") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF3B82F6),
                            selectedTextColor = Color(0xFF3B82F6),
                            indicatorColor = Color(0xFF3B82F6).copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("nav_tab_admin_shops")
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) +
                        slideIntoContainer(
                            towards = if (targetState.ordinal > initialState.ordinal)
                                AnimatedContentTransitionScope.SlideDirection.Left
                            else
                                AnimatedContentTransitionScope.SlideDirection.Right,
                            animationSpec = tween(220)
                        ) togetherWith (
                        fadeOut(animationSpec = tween(180)) +
                            slideOutOfContainer(
                                towards = if (targetState.ordinal > initialState.ordinal)
                                    AnimatedContentTransitionScope.SlideDirection.Left
                                else
                                    AnimatedContentTransitionScope.SlideDirection.Right,
                                animationSpec = tween(180)
                            )
                    )
                },
                label = "MainScreenTabTransition"
            ) { targetTab ->
                when (targetTab) {
                    ScreenTab.PURCHASES -> {
                        PurchaseTabScreen(
                            shop = selectedShop,
                            transactions = shopTransactions,
                            purchaseTabTotalValue = purchaseTabTotal,
                            userRole = user.role,
                            onAddNewEntry = {
                                editingTransaction = null
                                initialDialogSection = 0
                                showAddEntryDialog = true
                            },
                            onEditTransaction = { tx ->
                                editingTransaction = tx
                                initialDialogSection = 0
                                showAddEntryDialog = true
                            },
                            onOpenReminderConfig = { showReminderDialog = true },
                            syncState = syncState,
                            onSyncNow = { viewModel.syncNow() },
                            onOpenSyncDialog = { showSyncDialog = true }
                        )
                    }
                    ScreenTab.SALES -> {
                        SalesTabScreen(
                            shop = selectedShop,
                            transactions = shopTransactions,
                            salesTabTotalValue = salesTabTotal,
                            userRole = user.role,
                            onUpdateDamage = { txId, dmg ->
                                viewModel.updateDamage(txId, dmg)
                            },
                            onRecordDailySales = { timestamp, cash, card, notes, onDone ->
                                selectedShop?.let { shop ->
                                    viewModel.recordDailySales(
                                        shopOutletCode = shop.outletCode,
                                        transactionDate = timestamp,
                                        cashSales = cash,
                                        cardSales = card,
                                        notes = notes,
                                        onComplete = onDone
                                    )
                                } ?: onDone(false)
                            },
                            onEditTransaction = { tx ->
                                editingTransaction = tx
                                initialDialogSection = 1
                                showAddEntryDialog = true
                            },
                            onAddNewEntry = {
                                editingTransaction = null
                                initialDialogSection = 1
                                showAddEntryDialog = true
                            }
                        )
                    }
                    ScreenTab.BANK_DEPOSITS -> {
                        BankDepositTabScreen(
                            shop = selectedShop,
                            transactions = shopTransactions,
                            bankDepositTabTotalValue = bankDepositTabTotal,
                            onAddNewEntry = {
                                editingTransaction = null
                                initialDialogSection = 2
                                showAddEntryDialog = true
                            }
                        )
                    }
                    ScreenTab.ADMIN_SHOPS -> {
                        AdminShopsScreen(
                            shops = allShops,
                            users = allUsers,
                            transactions = allTransactions,
                            userRole = user.role,
                            onAddShop = { code, name, addr, ric, emp, bal ->
                                viewModel.addShop(code, name, addr, ric, emp, bal) {}
                            },
                            onAssignShop = { shop, ric, emp ->
                                viewModel.assignShop(shop, ric, emp) {}
                            },
                            onUpdateShopDetails = { shop, name, addr, ric, emp, bal ->
                                viewModel.updateShopDetails(shop, name, addr, ric, emp, bal) {}
                            },
                            onDeleteShop = { shop ->
                                viewModel.deleteShop(shop) {}
                            },
                            onDeleteStaff = { userToDelete, replacementCode ->
                                viewModel.deleteStaff(userToDelete, replacementCode) {}
                            },
                            onReassignStaff = { staffUser, newShopCodes ->
                                viewModel.reassignStaffOutlets(staffUser, newShopCodes) {}
                            },
                            onAddUser = { code, name, mobile, role, assignedShops, password ->
                                viewModel.addUser(code, name, mobile, role, assignedShops, password) {}
                            },
                            onUpdateUserPassword = { userId, newPassword ->
                                viewModel.updateUserPassword(userId, newPassword) {}
                            },
                            onUpdateStaffCredentials = { userId, newMobile, newPassword ->
                                viewModel.updateStaffCredentials(userId, newMobile, newPassword) {}
                            }
                        )
                    }
                    else -> {
                        PurchaseTabScreen(
                            shop = selectedShop,
                            transactions = shopTransactions,
                            purchaseTabTotalValue = purchaseTabTotal,
                            userRole = user.role,
                            onAddNewEntry = {
                                editingTransaction = null
                                initialDialogSection = 0
                                showAddEntryDialog = true
                            },
                            onEditTransaction = { tx ->
                                editingTransaction = tx
                                initialDialogSection = 0
                                showAddEntryDialog = true
                            },
                            onOpenReminderConfig = { showReminderDialog = true },
                            syncState = syncState,
                            onSyncNow = { viewModel.syncNow() },
                            onOpenSyncDialog = { showSyncDialog = true }
                        )
                    }
                }
            }
        }

        // Database Backup Utility Dialog
        if (showBackupDialog) {
            DatabaseBackupDialog(
                onDismissRequest = { showBackupDialog = false }
            )
        }

        // Daily Report Reminder Configuration Dialog
        if (showReminderDialog) {
            DailyReminderDialog(
                currentShop = selectedShop,
                onDismiss = { showReminderDialog = false }
            )
        }

        // SQLite Sync & Remote Server Status Dialog
        if (showSyncDialog) {
            SyncStatusDialog(
                syncState = syncState,
                connectionLogs = syncConnectionLogs,
                onDismiss = { showSyncDialog = false },
                onSyncNow = { viewModel.syncNow() },
                onTestConnection = { viewModel.testServerConnection { } },
                onSimulateFailure = { viewModel.simulateDebugFailureLog() },
                onClearLogs = { viewModel.clearSyncConnectionLogs() }
            )
        }
    }
}
