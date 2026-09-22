package com.example.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AssignmentInd
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.runtime.mutableIntStateOf
import com.example.data.model.OutletTransactionEntity
import com.example.security.BiometricCapability
import com.example.ui.components.AdminBiometricLockGateway
import com.example.ui.components.AdminBiometricSecurityCard
import com.example.ui.components.ChallanViewDialog
import com.example.ui.components.QuickDateFilter
import com.example.ui.components.TransactionFilterUtils
import com.example.ui.components.TransactionSearchBar
import com.example.data.model.ShopEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.ui.components.DatabaseBackupDialog
import com.example.ui.components.FormValidationSummaryBanner
import com.example.ui.components.FormValidators
import com.example.ui.components.ValidatedNumberField
import com.example.ui.components.ValidatedTextField
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.PrimaryIndigoDark
import com.example.ui.theme.SaffronGold
import com.example.util.IndianCurrencyUtils
import com.example.util.TransactionPermissionRules

@Composable
fun AdminShopsScreen(
    shops: List<ShopEntity>,
    users: List<UserEntity>,
    transactions: List<OutletTransactionEntity> = emptyList(),
    userRole: String = UserRole.ADMIN.code,
    onAddShop: (
        outletCode: String,
        shopName: String,
        address: String,
        assignedRicCode: String,
        assignedEmployeeCodes: String,
        initialOpeningBalance: Long
    ) -> Unit,
    onAssignShop: (
        shop: ShopEntity,
        newRicCode: String,
        newEmployeeCodes: String
    ) -> Unit,
    onUpdateShopDetails: (
        shop: ShopEntity,
        newShopName: String,
        newAddress: String,
        newRicCode: String,
        newEmployeeCodes: String,
        newOpeningBalance: Long
    ) -> Unit = { shop, name, addr, ric, emp, bal ->
        onAssignShop(shop, ric, emp)
    },
    onAddUser: (
        outletCode: String,
        name: String,
        mobileNumber: String,
        role: UserRole,
        assignedShopCodes: String,
        password: String
    ) -> Unit,
    onUpdateUserPassword: (userId: Long, newPassword: String) -> Unit = { _, _ -> },
    onUpdateStaffCredentials: (userId: Long, newMobile: String, newPassword: String) -> Unit = { _, _, _ -> },
    onDeleteShop: (ShopEntity) -> Unit = {},
    onDeleteStaff: (UserEntity, String?) -> Unit = { _, _ -> },
    onReassignStaff: (UserEntity, String) -> Unit = { _, _ -> },
    isBiometricSecurityEnabled: Boolean = true,
    isAdminBiometricUnlocked: Boolean = true,
    biometricCapability: BiometricCapability = BiometricCapability.Available,
    onRequestBiometricAuth: () -> Unit = {},
    onToggleBiometricSecurity: (Boolean) -> Unit = {},
    onLockBiometric: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val userRoleEnum = remember(userRole) { UserRole.fromCode(userRole) }
    val canAddOutlet = remember(userRoleEnum) { TransactionPermissionRules.canAddOutlet(userRoleEnum) }
    val canAssignRic = remember(userRoleEnum) { TransactionPermissionRules.canAssignRic(userRoleEnum) }
    val canSetPassword = remember(userRoleEnum) { TransactionPermissionRules.canSetPassword(userRoleEnum) }
    val canDeleteOutlet = remember(userRoleEnum) { TransactionPermissionRules.canDeleteOutlet(userRoleEnum) }
    val canDeleteStaff = remember(userRoleEnum) { TransactionPermissionRules.canDeleteStaff(userRoleEnum) }
    val canReassignStaff = remember(userRoleEnum) { TransactionPermissionRules.canReassignStaff(userRoleEnum) }

    var selectedAdminTab by remember { mutableIntStateOf(0) }
    var transactionSearchQuery by remember { mutableStateOf("") }
    var transactionDateFilter by remember { mutableStateOf(QuickDateFilter.ALL) }
    var viewingChallanTx by remember { mutableStateOf<OutletTransactionEntity?>(null) }

    var isAddShopOpen by remember { mutableStateOf(false) }
    var isAddUserOpen by remember { mutableStateOf(false) }
    var isBackupOpen by remember { mutableStateOf(false) }
    var editingShop by remember { mutableStateOf<ShopEntity?>(null) }
    var passwordTargetUser by remember { mutableStateOf<UserEntity?>(null) }
    var shopToDelete by remember { mutableStateOf<ShopEntity?>(null) }
    var staffToDelete by remember { mutableStateOf<UserEntity?>(null) }
    var staffToReassign by remember { mutableStateOf<UserEntity?>(null) }

    val shopMap = remember(shops) {
        shops.associateBy { it.outletCode }
    }

    val filteredTransactions = remember(transactions, transactionSearchQuery, transactionDateFilter, shopMap) {
        transactions.filter { tx ->
            val shopName = shopMap[tx.shopOutletCode]?.shopName ?: tx.shopOutletCode
            TransactionFilterUtils.matchesQuery(tx, shopName, transactionSearchQuery) &&
                    TransactionFilterUtils.matchesDateFilter(tx.transactionDate, transactionDateFilter)
        }
    }

    val ricUsers = remember(users) {
        users.filter { it.role == UserRole.RIC.name || it.role == UserRole.RIC.code }
    }
    val employeeUsers = remember(users) {
        users.filter { it.role == UserRole.SHOP_EMPLOYEE.name || it.role == UserRole.SHOP_EMPLOYEE.code }
    }
    val fieldStaffUsers = remember(users) {
        users.filter { it.role != UserRole.ADMIN.name }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("admin_shops_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Admin Sub-Navigation Tabs: Outlets & Staff vs Transaction Logs
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                TabRow(
                    selectedTabIndex = selectedAdminTab,
                    containerColor = Color.Transparent,
                    contentColor = PrimaryIndigo,
                    indicator = {},
                    divider = {},
                    modifier = Modifier.padding(4.dp)
                ) {
                    Tab(
                        selected = selectedAdminTab == 0,
                        onClick = { selectedAdminTab = 0 },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (selectedAdminTab == 0) MaterialTheme.colorScheme.surface else Color.Transparent
                            )
                            .testTag("admin_tab_shops"),
                        text = {
                            Text(
                                text = "Outlets & Staff",
                                fontWeight = if (selectedAdminTab == 0) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedAdminTab == 0) PrimaryIndigo else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = null,
                                tint = if (selectedAdminTab == 0) PrimaryIndigo else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )

                    Tab(
                        selected = selectedAdminTab == 1,
                        onClick = { selectedAdminTab = 1 },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (selectedAdminTab == 1) MaterialTheme.colorScheme.surface else Color.Transparent
                            )
                            .testTag("admin_tab_transaction_logs"),
                        text = {
                            Text(
                                text = if (transactions.isNotEmpty()) "Transaction Logs (${transactions.size})" else "Transaction Logs",
                                fontWeight = if (selectedAdminTab == 1) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedAdminTab == 1) PrimaryIndigo else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                contentDescription = null,
                                tint = if (selectedAdminTab == 1) PrimaryIndigo else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (selectedAdminTab == 0) {
                    // TAB 0: OUTLETS & STAFF MANAGEMENT
                    // Header Info Card
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.elevatedCardColors(containerColor = PrimaryIndigo)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(Color.White.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.AdminPanelSettings,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "ADMIN SHOP MANAGEMENT",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Add Shops & Assign to RIC and Shop Employees",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = "Registered Outlets: ${shops.size} | Registered Staff: ${users.size}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.padding(top = 2.dp)
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                if (!canAddOutlet) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.White.copy(alpha = 0.15f),
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Lock,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Admin Access Only: Add outlets, assign RIC, and set staff passwords.",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = { isAddShopOpen = true },
                                        enabled = canAddOutlet,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("btn_admin_add_shop"),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = EmeraldGreen,
                                            contentColor = Color.White,
                                            disabledContainerColor = Color.White.copy(alpha = 0.25f),
                                            disabledContentColor = Color.White.copy(alpha = 0.5f)
                                        )
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Add Shop", fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = { isAddUserOpen = true },
                                        enabled = canSetPassword,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("btn_admin_add_user"),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = Color.White,
                                            disabledContentColor = Color.White.copy(alpha = 0.4f)
                                        )
                                    ) {
                                        Icon(
                                            Icons.Default.PersonAdd,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Add Staff", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Database Backup & Export Card
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("card_admin_backup_utility"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(PrimaryIndigoDark.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Storage,
                                            contentDescription = "Database Storage",
                                            tint = PrimaryIndigo,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "SQLite Database Backup Utility",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Export SQLite database file to external storage for record safety",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Button(
                                    onClick = { isBackupOpen = true },
                                    modifier = Modifier.testTag("btn_admin_backup_export"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PrimaryIndigoDark,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.Download,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Backup", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Shops List Section
                    item {
                        Text(
                            text = "Configured Retail Outlets",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    items(shops, key = { "shop_${it.id}" }) { shop ->
                        ShopConfigCard(
                            shop = shop,
                            ricUsers = ricUsers,
                            employeeUsers = employeeUsers,
                            canAssignRic = canAssignRic,
                            canDeleteOutlet = canDeleteOutlet,
                            canSetPassword = canSetPassword,
                            onAssign = { editingShop = shop },
                            onDelete = { shopToDelete = shop },
                            onSetPassword = { user -> passwordTargetUser = user }
                        )
                    }

                    // Staff & Employee Accounts (RIC & Shop Employees)
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Staff & Employee Accounts",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Set passwords for RIC In-Charges & Shop Employees",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            FilledTonalButton(
                                onClick = { isAddUserOpen = true },
                                enabled = canSetPassword,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("btn_staff_section_add_user")
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Staff", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    if (fieldStaffUsers.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "No staff members registered yet. Click 'Add Staff' to create an RIC In-Charge or Shop Employee.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    } else {
                        items(fieldStaffUsers, key = { "staff_${it.id}" }) { staffUser ->
                            StaffUserCard(
                                user = staffUser,
                                shops = shops,
                                canSetPassword = canSetPassword,
                                canDeleteStaff = canDeleteStaff,
                                canReassignStaff = canReassignStaff,
                                onSetPassword = { passwordTargetUser = staffUser },
                                onReassign = { staffToReassign = staffUser },
                                onDelete = { staffToDelete = staffUser }
                            )
                        }
                    }
                } else {
                    // TAB 1: MASTER TRANSACTION LOGS WITH SEARCH BAR
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        // Prominent Search Bar at the very top of Transaction Logs
                        TransactionSearchBar(
                            searchQuery = transactionSearchQuery,
                            onSearchQueryChange = { transactionSearchQuery = it },
                            selectedDateFilter = transactionDateFilter,
                            onDateFilterChange = { transactionDateFilter = it },
                            totalCount = transactions.size,
                            filteredCount = filteredTransactions.size,
                            placeholderText = "Search by shop name or date (e.g. 18/09/2026)...",
                            testTag = "admin_transaction_search_bar"
                        )
                    }

                    // Financial Summary Banner
                    item {
                        val totalSales = remember(filteredTransactions) { filteredTransactions.sumOf { it.totalSales } }
                        val totalPurchases = remember(filteredTransactions) { filteredTransactions.sumOf { it.purchaseTotalValue } }
                        val totalDeposits = remember(filteredTransactions) { filteredTransactions.sumOf { it.totalBankDeposit } }

                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_logs_summary_card"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = PrimaryIndigoDark
                            ),
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "CONSOLIDATED LOGS AUDIT",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White.copy(alpha = 0.8f),
                                        letterSpacing = 1.sp
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color.White.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "${filteredTransactions.size} Records Found",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "Total Sales",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.8f)
                                        )
                                        Text(
                                            text = IndianCurrencyUtils.formatInr(totalSales),
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = "Total Purchases",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.8f)
                                        )
                                        Text(
                                            text = IndianCurrencyUtils.formatInr(totalPurchases),
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = "Total Deposits",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.8f)
                                        )
                                        Text(
                                            text = IndianCurrencyUtils.formatInr(totalDeposits),
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Transactions List
                    if (transactions.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ReceiptLong,
                                        contentDescription = null,
                                        tint = PrimaryIndigo,
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "No Transaction Logs Found",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "Daily entries submitted by shops will appear here for administrative review and auditing.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    } else if (filteredTransactions.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp)
                                    .testTag("admin_logs_empty_search_card"),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.SearchOff,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "No Entries Match '$transactionSearchQuery'",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "Try searching by specific shop name (e.g. Downtown) or transaction date (e.g. 18/09/2026 or 18 Sep).",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    OutlinedButton(
                                        onClick = {
                                            transactionSearchQuery = ""
                                            transactionDateFilter = QuickDateFilter.ALL
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("btn_clear_search_filters")
                                    ) {
                                        Text("Clear Search & Filters")
                                    }
                                }
                            }
                        }
                    } else {
                        items(filteredTransactions, key = { "tx_${it.id}" }) { tx ->
                            val currentShopName = shopMap[tx.shopOutletCode]?.shopName ?: "Shop (${tx.shopOutletCode})"
                            AdminTransactionDetailCard(
                                tx = tx,
                                shopName = currentShopName,
                                onViewChallan = { viewingChallanTx = tx }
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }

        // Add Shop Dialog
        if (isAddShopOpen) {
            AddShopDialog(
                ricUsers = ricUsers,
                employeeUsers = employeeUsers,
                onDismiss = { isAddShopOpen = false },
                onAdd = { code, name, addr, ric, emp, bal ->
                    onAddShop(code, name, addr, ric, emp, bal)
                    isAddShopOpen = false
                }
            )
        }

        // Add Staff/User Dialog
        if (isAddUserOpen) {
            AddUserDialog(
                shops = shops,
                onDismiss = { isAddUserOpen = false },
                onAdd = { code, name, mobile, role, assignedShops, password ->
                    onAddUser(code, name, mobile, role, assignedShops, password)
                    isAddUserOpen = false
                }
            )
        }

        // Set Staff Mobile & Password Dialog for RIC & Shop Employee
        passwordTargetUser?.let { targetUser ->
            SetStaffCredentialsDialog(
                user = targetUser,
                onDismiss = { passwordTargetUser = null },
                onSave = { newMobile, newPassword ->
                    onUpdateStaffCredentials(targetUser.id, newMobile, newPassword)
                    onUpdateUserPassword(targetUser.id, newPassword)
                    passwordTargetUser = null
                }
            )
        }

        // Modify Outlet Details Dialog (Rename Outlet, Address, RIC, Employee, Opening Balance)
        editingShop?.let { shop: ShopEntity ->
            EditShopDetailsDialog(
                shop = shop,
                ricUsers = ricUsers,
                employeeUsers = employeeUsers,
                onDismiss = { editingShop = null },
                onSave = { newName, newAddr, newRic, newEmp, newBal ->
                    onUpdateShopDetails(shop, newName, newAddr, newRic, newEmp, newBal)
                    editingShop = null
                }
            )
        }

        // Database Backup Utility Dialog
        if (isBackupOpen) {
            DatabaseBackupDialog(
                onDismissRequest = { isBackupOpen = false }
            )
        }

        // Challan Photo View Dialog
        viewingChallanTx?.let { tx ->
            ChallanViewDialog(
                transaction = tx,
                onDismiss = { viewingChallanTx = null }
            )
        }

        // Delete Shop Dialog
        shopToDelete?.let { shop: ShopEntity ->
            DeleteShopDialog(
                shop = shop,
                onDismiss = { shopToDelete = null },
                onConfirm = {
                    onDeleteShop(shop)
                    shopToDelete = null
                }
            )
        }

        // Delete Staff Dialog
        staffToDelete?.let { user: UserEntity ->
            DeleteStaffDialog(
                user = user,
                shops = shops,
                ricUsers = ricUsers,
                employeeUsers = employeeUsers,
                onDismiss = { staffToDelete = null },
                onConfirm = { replacementCode: String? ->
                    onDeleteStaff(user, replacementCode)
                    staffToDelete = null
                }
            )
        }

        // Reassign Staff Outlets Dialog
        staffToReassign?.let { user: UserEntity ->
            ReassignStaffDialog(
                staffUser = user,
                shops = shops,
                onDismiss = { staffToReassign = null },
                onConfirm = { newShopCodes: String ->
                    onReassignStaff(user, newShopCodes)
                    staffToReassign = null
                }
            )
        }
    }
}

@Composable
fun ShopConfigCard(
    shop: ShopEntity,
    ricUsers: List<UserEntity>,
    employeeUsers: List<UserEntity>,
    canAssignRic: Boolean = true,
    canDeleteOutlet: Boolean = true,
    canSetPassword: Boolean = true,
    onAssign: () -> Unit,
    onDelete: () -> Unit = {},
    onSetPassword: (UserEntity) -> Unit = {}
) {
    val assignedRic = remember(shop.assignedRicCode, ricUsers) {
        ricUsers.find { it.outletCode.equals(shop.assignedRicCode, ignoreCase = true) }
    }
    val assignedEmp = remember(shop.assignedEmployeeCodes, employeeUsers) {
        employeeUsers.find { it.outletCode.equals(shop.assignedEmployeeCodes, ignoreCase = true) }
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .testTag("shop_card_${shop.outletCode}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(EmeraldGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Storefront,
                            contentDescription = null,
                            tint = EmeraldGreen
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = shop.shopName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "Code: ${shop.outletCode}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = onAssign,
                        enabled = canAssignRic,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("btn_assign_shop_${shop.outletCode}")
                    ) {
                        Icon(
                            if (canAssignRic) Icons.Default.Edit else Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (canAssignRic) "Edit Details" else "Admin Only", style = MaterialTheme.typography.labelSmall)
                    }

                    if (canDeleteOutlet) {
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("btn_delete_shop_${shop.outletCode}")
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "Delete Outlet ${shop.shopName}",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (shop.address.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = shop.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

            // Assigned RIC and Employee display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Assigned RIC",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = assignedRic?.let { "${it.name} (${it.outletCode})" } ?: (shop.assignedRicCode.ifBlank { "Not assigned" }),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = SaffronGold,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (assignedRic != null && canSetPassword) {
                            IconButton(
                                onClick = { onSetPassword(assignedRic) },
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("btn_shop_set_ric_password_${shop.outletCode}")
                            ) {
                                Icon(
                                    Icons.Default.LockReset,
                                    contentDescription = "Set Password for ${assignedRic.name}",
                                    tint = SaffronGold,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Assigned Shop Employee",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = assignedEmp?.let { "${it.name} (${it.outletCode})" } ?: (shop.assignedEmployeeCodes.ifBlank { "Not assigned" }),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = EmeraldGreen,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (assignedEmp != null && canSetPassword) {
                            IconButton(
                                onClick = { onSetPassword(assignedEmp) },
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("btn_shop_set_emp_password_${shop.outletCode}")
                            ) {
                                Icon(
                                    Icons.Default.LockReset,
                                    contentDescription = "Set Password for ${assignedEmp.name}",
                                    tint = EmeraldGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Initial Opening Balance: ${IndianCurrencyUtils.formatInr(shop.initialOpeningBalance)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddShopDialog(
    ricUsers: List<UserEntity>,
    employeeUsers: List<UserEntity>,
    onDismiss: () -> Unit,
    onAdd: (
        outletCode: String,
        shopName: String,
        address: String,
        assignedRicCode: String,
        assignedEmployeeCodes: String,
        initialOpeningBalance: Long
    ) -> Unit
) {
    var outletCode by remember { mutableStateOf("") }
    var shopName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var selectedRic by remember { mutableStateOf(ricUsers.firstOrNull()?.outletCode ?: "") }
    var selectedEmployee by remember { mutableStateOf(employeeUsers.firstOrNull()?.outletCode ?: "") }
    var initialBalanceText by remember { mutableStateOf("15000") }
    var errorText by remember { mutableStateOf<String?>(null) }

    val outletCodeValidation = remember(outletCode) {
        FormValidators.validateRequiredText(outletCode, "Outlet Code")
    }
    val shopNameValidation = remember(shopName) {
        FormValidators.validateRequiredText(shopName, "Shop Name")
    }
    val balanceValidation = remember(initialBalanceText) {
        FormValidators.validatePositiveLong(initialBalanceText, "Initial Opening Balance", isRequired = true, allowZero = true)
    }
    val shopFormErrors = remember(outletCodeValidation, shopNameValidation, balanceValidation) {
        listOfNotNull(outletCodeValidation.errorMessage, shopNameValidation.errorMessage, balanceValidation.errorMessage)
    }
    val isFormValid = outletCodeValidation.isValid && shopNameValidation.isValid && balanceValidation.isValid

    var isRicDropdownExpanded by remember { mutableStateOf(false) }
    var isEmpDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add New Retail Shop",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (!errorText.isNullOrBlank()) {
                    Text(
                        text = errorText!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Validated Outlet Code
                ValidatedTextField(
                    value = outletCode,
                    onValueChange = { outletCode = it },
                    label = "Outlet Code",
                    validationResult = outletCodeValidation,
                    isRequired = true,
                    placeholder = "e.g. OUT-103",
                    testTag = "admin_new_shop_code"
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Validated Shop Name
                ValidatedTextField(
                    value = shopName,
                    onValueChange = { shopName = it },
                    label = "Shop Name",
                    validationResult = shopNameValidation,
                    isRequired = true,
                    placeholder = "e.g. Whitefield Central Outlet",
                    testTag = "admin_new_shop_name"
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Address
                Text(text = "Address", style = MaterialTheme.typography.labelMedium)
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    placeholder = { Text("Location / Landmark") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Assign RIC Dropdown
                Text(text = "Assign to RIC", style = MaterialTheme.typography.labelMedium)
                ExposedDropdownMenuBox(
                    expanded = isRicDropdownExpanded,
                    onExpandedChange = { isRicDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = ricUsers.find { it.outletCode == selectedRic }?.let { "${it.name} (${it.outletCode})" } ?: selectedRic.ifBlank { "Select RIC" },
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isRicDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = isRicDropdownExpanded,
                        onDismissRequest = { isRicDropdownExpanded = false }
                    ) {
                        ricUsers.forEach { ric ->
                            DropdownMenuItem(
                                text = { Text("${ric.name} (${ric.outletCode})") },
                                onClick = {
                                    selectedRic = ric.outletCode
                                    isRicDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Assign Employee Dropdown
                Text(text = "Assign to Shop Employee", style = MaterialTheme.typography.labelMedium)
                ExposedDropdownMenuBox(
                    expanded = isEmpDropdownExpanded,
                    onExpandedChange = { isEmpDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = employeeUsers.find { it.outletCode == selectedEmployee }?.let { "${it.name} (${it.outletCode})" } ?: selectedEmployee.ifBlank { "Select Employee" },
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isEmpDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = isEmpDropdownExpanded,
                        onDismissRequest = { isEmpDropdownExpanded = false }
                    ) {
                        employeeUsers.forEach { emp ->
                            DropdownMenuItem(
                                text = { Text("${emp.name} (${emp.outletCode})") },
                                onClick = {
                                    selectedEmployee = emp.outletCode
                                    isEmpDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Validated Initial Opening Balance
                ValidatedNumberField(
                    value = initialBalanceText,
                    onValueChange = { initialBalanceText = it },
                    label = "Initial Opening Balance",
                    validationResult = balanceValidation,
                    isRequired = true,
                    placeholder = "0",
                    ruleBadgeText = "Non-negative integer",
                    testTag = "admin_new_shop_balance"
                )

                if (shopFormErrors.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    FormValidationSummaryBanner(errors = shopFormErrors)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isFormValid) {
                        val bal = initialBalanceText.toLongOrNull() ?: 0L
                        onAdd(outletCode, shopName, address, selectedRic, selectedEmployee, bal)
                    }
                },
                enabled = isFormValid,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                modifier = Modifier.testTag("btn_confirm_add_shop")
            ) {
                Text("Add Shop")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditShopDetailsDialog(
    shop: ShopEntity,
    ricUsers: List<UserEntity>,
    employeeUsers: List<UserEntity>,
    onDismiss: () -> Unit,
    onSave: (
        newShopName: String,
        newAddress: String,
        newRicCode: String,
        newEmployeeCode: String,
        newOpeningBalance: Long
    ) -> Unit
) {
    var shopName by remember { mutableStateOf(shop.shopName) }
    var address by remember { mutableStateOf(shop.address) }
    var selectedRic by remember { mutableStateOf(shop.assignedRicCode) }
    var selectedEmployee by remember { mutableStateOf(shop.assignedEmployeeCodes) }
    var initialBalanceText by remember { mutableStateOf(shop.initialOpeningBalance.toString()) }
    var isRicDropdownExpanded by remember { mutableStateOf(false) }
    var isEmpDropdownExpanded by remember { mutableStateOf(false) }

    val shopNameValidation = remember(shopName) {
        FormValidators.validateRequiredText(shopName, "Outlet Name")
    }
    val balanceValidation = remember(initialBalanceText) {
        FormValidators.validatePositiveLong(initialBalanceText, "Initial Opening Balance", isRequired = true, allowZero = true)
    }
    val isFormValid = shopNameValidation.isValid && balanceValidation.isValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Storefront,
                    contentDescription = null,
                    tint = PrimaryIndigo,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Modify Outlet Details",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Outlet Code: ${shop.outletCode}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Rename Outlet Name
                ValidatedTextField(
                    value = shopName,
                    onValueChange = { shopName = it },
                    label = "Outlet Name",
                    validationResult = shopNameValidation,
                    isRequired = true,
                    placeholder = "e.g. Hyderabad Central Store",
                    modifier = Modifier.testTag("input_edit_shop_name")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Modify Address / Location
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Outlet Address / Location") },
                    placeholder = { Text("e.g. Banjara Hills, Road #2") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_edit_shop_address")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Assigned RIC Dropdown
                Text(text = "Assigned Regional In-Charge (RIC)", style = MaterialTheme.typography.labelMedium)
                ExposedDropdownMenuBox(
                    expanded = isRicDropdownExpanded,
                    onExpandedChange = { isRicDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = ricUsers.find { it.outletCode == selectedRic }?.let { "${it.name} (${it.outletCode})" } ?: selectedRic.ifBlank { "Unassigned" },
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isRicDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = isRicDropdownExpanded,
                        onDismissRequest = { isRicDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("None (Unassigned)") },
                            onClick = {
                                selectedRic = ""
                                isRicDropdownExpanded = false
                            }
                        )
                        ricUsers.forEach { ric ->
                            DropdownMenuItem(
                                text = { Text("${ric.name} (${ric.outletCode})") },
                                onClick = {
                                    selectedRic = ric.outletCode
                                    isRicDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Assigned Shop Employee Dropdown
                Text(text = "Assigned Shop Employee", style = MaterialTheme.typography.labelMedium)
                ExposedDropdownMenuBox(
                    expanded = isEmpDropdownExpanded,
                    onExpandedChange = { isEmpDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = employeeUsers.find { it.outletCode == selectedEmployee }?.let { "${it.name} (${it.outletCode})" } ?: selectedEmployee.ifBlank { "Unassigned" },
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isEmpDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = isEmpDropdownExpanded,
                        onDismissRequest = { isEmpDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("None (Unassigned)") },
                            onClick = {
                                selectedEmployee = ""
                                isEmpDropdownExpanded = false
                            }
                        )
                        employeeUsers.forEach { emp ->
                            DropdownMenuItem(
                                text = { Text("${emp.name} (${emp.outletCode})") },
                                onClick = {
                                    selectedEmployee = emp.outletCode
                                    isEmpDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Initial Opening Balance
                ValidatedNumberField(
                    value = initialBalanceText,
                    onValueChange = { initialBalanceText = it },
                    label = "Initial Opening Balance",
                    validationResult = balanceValidation,
                    isRequired = true,
                    placeholder = "0",
                    ruleBadgeText = "Non-negative integer",
                    testTag = "input_edit_shop_balance"
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isFormValid) {
                        val bal = initialBalanceText.toLongOrNull() ?: 0L
                        onSave(shopName, address, selectedRic, selectedEmployee, bal)
                    }
                },
                enabled = isFormValid,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                modifier = Modifier.testTag("btn_confirm_edit_shop")
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignShopDialog(
    shop: ShopEntity,
    ricUsers: List<UserEntity>,
    employeeUsers: List<UserEntity>,
    onDismiss: () -> Unit,
    onSave: (newRicCode: String, newEmployeeCode: String) -> Unit
) {
    EditShopDetailsDialog(
        shop = shop,
        ricUsers = ricUsers,
        employeeUsers = employeeUsers,
        onDismiss = onDismiss,
        onSave = { _, _, ric, emp, _ -> onSave(ric, emp) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddUserDialog(
    shops: List<ShopEntity>,
    onDismiss: () -> Unit,
    onAdd: (
        outletCode: String,
        name: String,
        mobileNumber: String,
        role: UserRole,
        assignedShopCodes: String,
        password: String
    ) -> Unit
) {
    var outletCode by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var mobileNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf(UserRole.SHOP_EMPLOYEE) }
    var selectedShopCode by remember { mutableStateOf(shops.firstOrNull()?.outletCode ?: "") }
    var errorText by remember { mutableStateOf<String?>(null) }

    val outletCodeValidation = remember(outletCode) {
        FormValidators.validateRequiredText(outletCode, "User Code / Identifier")
    }
    val nameValidation = remember(name) {
        FormValidators.validateRequiredText(name, "Full Name")
    }
    val mobileValidation = remember(mobileNumber) {
        FormValidators.validateMobileNumber(mobileNumber, "Mobile Number")
    }
    val userFormErrors = remember(outletCodeValidation, nameValidation, mobileValidation) {
        listOfNotNull(outletCodeValidation.errorMessage, nameValidation.errorMessage, mobileValidation.errorMessage)
    }
    val isFormValid = outletCodeValidation.isValid && nameValidation.isValid && mobileValidation.isValid

    var isRoleExpanded by remember { mutableStateOf(false) }
    var isShopExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Staff User (RIC / Employee)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (!errorText.isNullOrBlank()) {
                    Text(
                        text = errorText!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Validated User Code
                ValidatedTextField(
                    value = outletCode,
                    onValueChange = { outletCode = it },
                    label = "User Code / Identifier",
                    validationResult = outletCodeValidation,
                    isRequired = true,
                    placeholder = "e.g. EMP103 or RIC02"
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Validated Full Name
                ValidatedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Full Name",
                    validationResult = nameValidation,
                    isRequired = true,
                    placeholder = "e.g. Rajesh Sharma"
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Validated Mobile Number
                ValidatedTextField(
                    value = mobileNumber,
                    onValueChange = { mobileNumber = it.filter { ch -> ch.isDigit() } },
                    label = "Registered Mobile Number",
                    validationResult = mobileValidation,
                    isRequired = true,
                    placeholder = "10-digit mobile number",
                    keyboardType = KeyboardType.Phone,
                    ruleBadgeText = "10 digits"
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Optional Custom Password
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Account Password (Optional)") },
                    placeholder = { Text("Leave blank to use Mobile Number") },
                    leadingIcon = {
                        Icon(Icons.Default.Key, contentDescription = null, tint = EmeraldGreen)
                    },
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle password visibility"
                            )
                        }
                    },
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_add_user_password")
                )
                Text(
                    text = "If left empty, the 10-digit mobile number will be the initial login password.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Role Dropdown
                Text(text = "Role", style = MaterialTheme.typography.labelMedium)
                ExposedDropdownMenuBox(
                    expanded = isRoleExpanded,
                    onExpandedChange = { isRoleExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedRole.displayName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isRoleExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = isRoleExpanded,
                        onDismissRequest = { isRoleExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(UserRole.RIC.displayName) },
                            onClick = {
                                selectedRole = UserRole.RIC
                                isRoleExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(UserRole.SHOP_EMPLOYEE.displayName) },
                            onClick = {
                                selectedRole = UserRole.SHOP_EMPLOYEE
                                isRoleExpanded = false
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Assigned Shop
                Text(text = "Assigned Shop", style = MaterialTheme.typography.labelMedium)
                ExposedDropdownMenuBox(
                    expanded = isShopExpanded,
                    onExpandedChange = { isShopExpanded = it }
                ) {
                    OutlinedTextField(
                        value = shops.find { it.outletCode == selectedShopCode }?.let { "${it.shopName} (${it.outletCode})" } ?: selectedShopCode,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isShopExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = isShopExpanded,
                        onDismissRequest = { isShopExpanded = false }
                    ) {
                        shops.forEach { s ->
                            DropdownMenuItem(
                                text = { Text("${s.shopName} (${s.outletCode})") },
                                onClick = {
                                    selectedShopCode = s.outletCode
                                    isShopExpanded = false
                                }
                            )
                        }
                    }
                }

                if (userFormErrors.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    FormValidationSummaryBanner(errors = userFormErrors)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isFormValid) {
                        onAdd(outletCode, name, mobileNumber, selectedRole, selectedShopCode, password)
                    }
                },
                enabled = isFormValid,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
            ) {
                Text("Register User")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AdminTransactionDetailCard(
    tx: OutletTransactionEntity,
    shopName: String,
    onViewChallan: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_tx_card_${tx.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Shop Name & Code badge, Date & Sl No
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = shopName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = PrimaryIndigo.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "Code: ${tx.shopOutletCode}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = PrimaryIndigo,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = EmeraldGreen.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Sl #${tx.slNo}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = EmeraldGreen,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = IndianCurrencyUtils.formatDate(tx.transactionDate),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // Section 1: Opening Balance & Purchases
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Opening Balance",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = IndianCurrencyUtils.formatInr(tx.openingBalance),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        if (tx.isOpeningBalanceManual) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = SaffronGold.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "Manual",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = SaffronGold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Purchase + Margin",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${IndianCurrencyUtils.formatInr(tx.purchase)} (+10%: ${IndianCurrencyUtils.formatInr(tx.margin10)})",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Total Value",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = IndianCurrencyUtils.formatInr(tx.totalValue),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = PrimaryIndigo
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Section 2: Sales & Damage
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Card Sales",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = IndianCurrencyUtils.formatInr(tx.cardSales),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Cash Sales",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = IndianCurrencyUtils.formatInr(tx.cashSales),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Total Sales",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = IndianCurrencyUtils.formatInr(tx.totalSales),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = EmeraldGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Section 3: Bank Deposit & Closing Balance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1.5f)) {
                    Text(
                        text = "Bank Deposit",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${IndianCurrencyUtils.formatInr(tx.depositAmount)} on ${IndianCurrencyUtils.formatDate(tx.bankDepositDate)}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Closing Balance",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = IndianCurrencyUtils.formatInr(tx.closingBalance),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = EmeraldGreen
                    )
                }
            }

            // Damage & Notes or Challan button if available
            if (tx.damage > 0 || tx.notes.isNotBlank() || tx.challanPhotoUri.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (tx.damage > 0) {
                            Text(
                                text = "Damage: ${IndianCurrencyUtils.formatInr(tx.damage)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        if (tx.notes.isNotBlank()) {
                            Text(
                                text = "Notes: ${tx.notes}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (tx.challanPhotoUri.isNotBlank()) {
                        OutlinedButton(
                            onClick = onViewChallan,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("btn_view_challan_${tx.id}")
                        ) {
                            Icon(
                                Icons.Default.Visibility,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Challan", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StaffUserCard(
    user: UserEntity,
    shops: List<ShopEntity>,
    canSetPassword: Boolean = true,
    canDeleteStaff: Boolean = true,
    canReassignStaff: Boolean = true,
    onSetPassword: () -> Unit,
    onReassign: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val role = UserRole.fromCode(user.role)
    val roleColor = if (role == UserRole.RIC) SaffronGold else EmeraldGreen
    val assignedShopNames = remember(user.assignedShopCodes, shops) {
        if (user.assignedShopCodes == "*") {
            "All Shops (Central)"
        } else {
            val codes = user.assignedShopCodes.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            if (codes.isEmpty()) "None"
            else codes.joinToString(", ") { code ->
                shops.find { it.outletCode.equals(code, ignoreCase = true) }?.shopName ?: code
            }
        }
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .testTag("staff_card_${user.outletCode}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Name, Code badge, and Role Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(roleColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (role == UserRole.RIC) Icons.Default.Badge else Icons.Default.Person,
                            contentDescription = null,
                            tint = roleColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = user.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "Code: ${user.outletCode}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = roleColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = role.displayName,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = roleColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Details: Mobile & Assigned Outlets
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = user.mobileNumber,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "Outlets: $assignedShopNames",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Password Status & Set Password Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        if (user.password.isNotBlank()) Icons.Default.Lock else Icons.Default.Key,
                        contentDescription = null,
                        tint = if (user.password.isNotBlank()) EmeraldGreen else SaffronGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = if (user.password.isNotBlank()) "Password: Custom Configured" else "Password: Default (Mobile Number)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (user.password.isNotBlank()) EmeraldGreen else SaffronGold
                        )
                        Text(
                            text = if (user.password.isNotBlank()) "Protected with admin-set credential" else "Staff logs in using 10-digit mobile number",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (canReassignStaff && user.role != UserRole.ADMIN.name) {
                        OutlinedButton(
                            onClick = onReassign,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("btn_reassign_staff_${user.outletCode}")
                        ) {
                            Icon(
                                Icons.Default.SwapHoriz,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reassign", style = MaterialTheme.typography.labelSmall)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    Button(
                        onClick = onSetPassword,
                        enabled = canSetPassword,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("btn_set_password_${user.outletCode}")
                    ) {
                        Icon(
                            if (canSetPassword) Icons.Default.LockReset else Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (!canSetPassword) "Admin Only" else "Edit Login",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    if (canDeleteStaff && user.role != UserRole.ADMIN.name) {
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("btn_delete_staff_${user.outletCode}")
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "Delete staff ${user.name}",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SetStaffCredentialsDialog(
    user: UserEntity,
    onDismiss: () -> Unit,
    onSave: (newMobile: String, newPassword: String) -> Unit
) {
    var mobileNumber by remember { mutableStateOf(user.mobileNumber) }
    var newPassword by remember { mutableStateOf(user.password) }
    var confirmPassword by remember { mutableStateOf(user.password) }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    val role = UserRole.fromCode(user.role)
    val roleColor = if (role == UserRole.RIC) SaffronGold else EmeraldGreen

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(roleColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        tint = roleColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Manage Staff Login",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${user.name} (${user.outletCode})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Info Box
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Role: ${role.displayName}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = roleColor
                            )
                            Text(
                                text = "Code: ${user.outletCode}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Admin sets the mobile number for OTP login, and optional password.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (!errorText.isNullOrBlank()) {
                    Text(
                        text = errorText!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                // Mobile Number Input (Configured by Admin for OTP login)
                OutlinedTextField(
                    value = mobileNumber,
                    onValueChange = {
                        if (it.length <= 15) {
                            mobileNumber = it
                            errorText = null
                        }
                    },
                    label = { Text("Registered Mobile Number *") },
                    placeholder = { Text("10-digit mobile number") },
                    leadingIcon = {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = roleColor)
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_staff_mobile")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // New Password Input
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = {
                        newPassword = it
                        errorText = null
                    },
                    label = { Text("Password (or OTP Backup)") },
                    placeholder = { Text("Min 4 characters") },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = roleColor)
                    },
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle password visibility"
                            )
                        }
                    },
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_new_password")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Confirm Password Input
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        errorText = null
                    },
                    label = { Text("Confirm Password") },
                    placeholder = { Text("Re-enter password") },
                    leadingIcon = {
                        Icon(Icons.Default.LockClock, contentDescription = null, tint = roleColor)
                    },
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_confirm_password")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmedMobile = mobileNumber.trim()
                    val digits = trimmedMobile.filter { it.isDigit() }
                    val trimmed = newPassword.trim()
                    val trimmedConfirm = confirmPassword.trim()
                    when {
                        digits.length < 10 -> {
                            errorText = "Please enter a valid 10-digit mobile number"
                        }
                        trimmed.isNotBlank() && trimmed.length < 4 -> {
                            errorText = "Password must be at least 4 characters"
                        }
                        trimmed != trimmedConfirm -> {
                            errorText = "Passwords do not match"
                        }
                        else -> {
                            val finalPassword = if (trimmed.isBlank()) digits.takeLast(10) else trimmed
                            onSave(digits.takeLast(10), finalPassword)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = roleColor),
                modifier = Modifier.testTag("btn_save_password")
            ) {
                Text("Save Credentials", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DeleteShopDialog(
    shop: ShopEntity,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.WarningAmber,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Delete Outlet: ${shop.shopName}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column {
                Text(
                    text = "Are you sure you want to delete outlet ${shop.shopName} (Code: ${shop.outletCode})?",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This will remove the outlet from active records. Staff assigned to this outlet will be unassigned.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag("btn_confirm_delete_shop")
            ) {
                Text("Delete Outlet")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteStaffDialog(
    user: UserEntity,
    shops: List<ShopEntity>,
    ricUsers: List<UserEntity>,
    employeeUsers: List<UserEntity>,
    onDismiss: () -> Unit,
    onConfirm: (replacementCode: String?) -> Unit
) {
    val assignedShops = remember(user, shops) {
        val codes = user.assignedShopCodes.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        shops.filter { codes.contains(it.outletCode) || (user.role == UserRole.RIC.name && it.assignedRicCode == user.outletCode) || (user.role == UserRole.SHOP_EMPLOYEE.name && it.assignedEmployeeCodes == user.outletCode) }
    }
    val role = UserRole.fromCode(user.role)
    val candidateReplacements = if (role == UserRole.RIC) ricUsers.filter { it.outletCode != user.outletCode } else employeeUsers.filter { it.outletCode != user.outletCode }

    var selectedReplacement by remember { mutableStateOf("") }
    var isReplacementDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.PersonRemove,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Delete Staff: ${user.name}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Are you sure you want to remove ${user.name} (${user.role} - ${user.outletCode})?",
                    style = MaterialTheme.typography.bodyMedium
                )

                if (assignedShops.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "This staff member is currently assigned to ${assignedShops.size} outlet(s): ${assignedShops.joinToString(", ") { it.shopName }}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Optional: Reassign outlets to new ${role.displayName}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )

                    ExposedDropdownMenuBox(
                        expanded = isReplacementDropdownExpanded,
                        onExpandedChange = { isReplacementDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = candidateReplacements.find { it.outletCode == selectedReplacement }?.let { "${it.name} (${it.outletCode})" } ?: if (selectedReplacement.isBlank()) "Leave Unassigned" else selectedReplacement,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isReplacementDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = isReplacementDropdownExpanded,
                            onDismissRequest = { isReplacementDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Leave Unassigned") },
                                onClick = {
                                    selectedReplacement = ""
                                    isReplacementDropdownExpanded = false
                                }
                            )
                            candidateReplacements.forEach { candidate ->
                                DropdownMenuItem(
                                    text = { Text("${candidate.name} (${candidate.outletCode})") },
                                    onClick = {
                                        selectedReplacement = candidate.outletCode
                                        isReplacementDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedReplacement.ifBlank { null }) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag("btn_confirm_delete_staff")
            ) {
                Text("Delete Staff")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ReassignStaffDialog(
    staffUser: UserEntity,
    shops: List<ShopEntity>,
    onDismiss: () -> Unit,
    onConfirm: (newShopCodes: String) -> Unit
) {
    val role = UserRole.fromCode(staffUser.role)
    val initialAssigned = remember(staffUser.assignedShopCodes) {
        staffUser.assignedShopCodes.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }
    val selectedShopMap = remember {
        mutableStateMapOf<String, Boolean>().apply {
            shops.forEach { shop ->
                put(shop.outletCode, initialAssigned.contains(shop.outletCode))
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.SwapHoriz,
                contentDescription = null,
                tint = PrimaryIndigo,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Reassign Outlets: ${staffUser.name}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Select outlets assigned to this ${role.displayName} (${staffUser.outletCode}):",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                ) {
                    items(shops) { shop ->
                        val isSelected = selectedShopMap[shop.outletCode] == true
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (role == UserRole.SHOP_EMPLOYEE) {
                                        // Single selection for Shop Employee
                                        shops.forEach { s -> selectedShopMap[s.outletCode] = false }
                                        selectedShopMap[shop.outletCode] = !isSelected
                                    } else {
                                        // Multiple selection for RIC
                                        selectedShopMap[shop.outletCode] = !isSelected
                                    }
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    if (role == UserRole.SHOP_EMPLOYEE) {
                                        shops.forEach { s -> selectedShopMap[s.outletCode] = false }
                                        selectedShopMap[shop.outletCode] = checked
                                    } else {
                                        selectedShopMap[shop.outletCode] = checked
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(shop.shopName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                                Text(
                                    text = "Code: ${shop.outletCode} • ${shop.address.ifBlank { "No address" }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val assignedList = selectedShopMap.filter { it.value }.keys.toList()
                    val codesString = assignedList.joinToString(",")
                    onConfirm(codesString)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                modifier = Modifier.testTag("btn_confirm_reassign_staff")
            ) {
                Text("Confirm Reassignment")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
