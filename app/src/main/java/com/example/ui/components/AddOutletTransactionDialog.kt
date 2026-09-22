package com.example.ui.components

import android.app.DatePickerDialog
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.OutletTransactionEntity
import com.example.data.model.ShopEntity
import com.example.data.model.UserRole
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.SaffronDark
import com.example.ui.theme.SaffronGold
import com.example.util.FileUtils
import com.example.util.IndianCurrencyUtils
import com.example.util.TransactionPermissionRules
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

@Composable
fun AddOutletTransactionDialog(
    shop: ShopEntity,
    userRole: String,
    onDismiss: () -> Unit,
    onFetchPreviousClosingBalance: suspend (shopCode: String, date: Long) -> Long?,
    onFetchNextSlNo: suspend (shopCode: String) -> Long,
    initialSectionIndex: Int = 0,
    initialTransaction: OutletTransactionEntity? = null,
    onUpdate: ((OutletTransactionEntity) -> Unit)? = null,
    onSubmit: (
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
        notes: String
    ) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val isEditMode = initialTransaction != null
    val userRoleEnum = remember(userRole) { UserRole.fromCode(userRole) }

    // Form Step/Section
    var selectedSectionIndex by remember { mutableIntStateOf(initialSectionIndex.coerceIn(0, 2)) }
    val sections = listOf("1. Purchase", "2. Sales", "3. Bank Deposit")

    // General meta
    var slNo by remember { mutableLongStateOf(initialTransaction?.slNo ?: 1L) }
    var transactionDate by remember {
        mutableLongStateOf(initialTransaction?.transactionDate ?: IndianCurrencyUtils.getStartOfDay(System.currentTimeMillis()))
    }
    var isLoadingSlNo by remember { mutableStateOf(initialTransaction == null) }

    // RBAC: Admin & RIC can modify past date entries; Shop employees can only view past records
    val isPastRecord = remember(initialTransaction, transactionDate) {
        val targetDate = initialTransaction?.transactionDate ?: transactionDate
        TransactionPermissionRules.isPastDate(targetDate)
    }

    val canModify = remember(userRoleEnum, initialTransaction, transactionDate) {
        if (initialTransaction != null) {
            TransactionPermissionRules.canModifyEntry(userRoleEnum, initialTransaction.transactionDate)
        } else {
            true
        }
    }
    val isReadOnly = isEditMode && !canModify

    // Can user select past dates when creating a new entry?
    val canSelectPastDateForNewEntry = remember(userRoleEnum) {
        TransactionPermissionRules.canSelectPastDateForNewEntry(userRoleEnum)
    }

    // Section 1: Purchase Tab Fields
    var openingBalanceText by remember { mutableStateOf(initialTransaction?.openingBalance?.toString() ?: "") }
    var isOpeningBalanceFetched by remember { mutableStateOf(initialTransaction != null) }
    var purchaseText by remember { mutableStateOf(initialTransaction?.purchase?.toString() ?: "") }
    var aroedText by remember { mutableStateOf(initialTransaction?.aroed?.toString() ?: "") }

    // Section 2: Sales Tab Fields
    var cardSalesText by remember { mutableStateOf(initialTransaction?.cardSales?.toString() ?: "") }
    var cashSalesText by remember { mutableStateOf(initialTransaction?.cashSales?.toString() ?: "") }
    var damageText by remember { mutableStateOf(initialTransaction?.damage?.toString() ?: "0") }

    // Section 3: Bank Deposit Tab Fields
    // Defaults to Transaction Date + 1 Day (86400000L)
    var bankDepositDate by remember {
        mutableLongStateOf(
            initialTransaction?.bankDepositDate
                ?: IndianCurrencyUtils.getStartOfDay(System.currentTimeMillis() + 86400000L)
        )
    }
    var depositAmountText by remember { mutableStateOf(initialTransaction?.depositAmount?.toString() ?: "") }
    var challanPhotoUri by remember { mutableStateOf(initialTransaction?.challanPhotoUri ?: "") }
    var notesText by remember { mutableStateOf(initialTransaction?.notes ?: "") }

    var validationError by remember { mutableStateOf<String?>(null) }

    val isAdmin = remember(userRole) {
        UserRole.fromCode(userRole) == UserRole.ADMIN
    }

    // Photo launchers
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val path = FileUtils.saveBitmapToInternalStorage(context, bitmap)
            challanPhotoUri = path
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val path = FileUtils.copyUriToInternalStorage(context, uri)
            challanPhotoUri = path
        }
    }

    // Fetch Sl. No. and Previous Day Closing Balance on launch or transactionDate change
    LaunchedEffect(transactionDate, shop.outletCode) {
        if (initialTransaction != null) {
            isLoadingSlNo = false
            return@LaunchedEffect
        }
        isLoadingSlNo = true
        val nextSl = onFetchNextSlNo(shop.outletCode)
        slNo = nextSl

        val prevClosing = onFetchPreviousClosingBalance(shop.outletCode, transactionDate)
        if (prevClosing != null) {
            openingBalanceText = prevClosing.toString()
            isOpeningBalanceFetched = true
        } else {
            // First time entry
            isOpeningBalanceFetched = false
            if (openingBalanceText.isBlank() && shop.initialOpeningBalance > 0) {
                openingBalanceText = shop.initialOpeningBalance.toString()
            }
        }
        isLoadingSlNo = false

        // Automatically set bank deposit date to transaction date + 1 day if it's less or equal
        if (bankDepositDate <= transactionDate) {
            bankDepositDate = transactionDate + 86400000L
        }
    }

    // Numeric calculations
    val openingBalance = remember(openingBalanceText) { openingBalanceText.toLongOrNull() ?: 0L }
    val purchase = remember(purchaseText) { purchaseText.toLongOrNull() ?: 0L }
    // 10% Margin: Auto calculation: Purchase * 10% (Read only)
    val margin10 = remember(purchase) { Math.round(purchase * 0.10) }
    val aroed = remember(aroedText) { aroedText.toLongOrNull() ?: 0L }
    // Total Value: Auto calculation Sum Opening Balance, Purchase, 10% Margin, AROED
    val totalValue = remember(openingBalance, purchase, margin10, aroed) {
        openingBalance + purchase + margin10 + aroed
    }
    // Subtotal to display in Purchase tab: Purchase + 10% Margin + AROED
    val purchaseTabDisplayTotal = remember(purchase, margin10, aroed) {
        purchase + margin10 + aroed
    }

    val cardSales = remember(cardSalesText) { cardSalesText.toLongOrNull() ?: 0L }
    val cashSales = remember(cashSalesText) { cashSalesText.toLongOrNull() ?: 0L }
    // Total Sales: Auto calculation Sum of Card Sales & Cash Sales
    val totalSales = remember(cardSales, cashSales) { cardSales + cashSales }
    val damage = remember(damageText) { damageText.toLongOrNull() ?: 0L }
    // Closing Balance: Auto calculation Total Value - Total Sales - Damage
    val closingBalance = remember(totalValue, totalSales, damage) {
        totalValue - totalSales - damage
    }

    val depositAmount = remember(depositAmountText) { depositAmountText.toLongOrNull() ?: 0L }
    // Total Bank Deposit to display in Bank Deposit tab: Bank Deposit + Card Sales
    val bankDepositTabDisplayTotal = remember(depositAmount, cardSales) {
        depositAmount + cardSales
    }

    // Cash Balance for deposit validation: Opening Balance + Cash Sales - Damage
    val availableCashForDeposit = remember(openingBalance, cashSales, damage) {
        (openingBalance + cashSales - damage).coerceAtLeast(0L)
    }

    // --- REAL-TIME VALIDATIONS FOR VISUAL FEEDBACK ---
    // Section 0 (Purchase) Validations
    val openingBalanceValidation = remember(openingBalanceText, isOpeningBalanceFetched) {
        if (isOpeningBalanceFetched) {
            ValidationResult.Valid
        } else {
            FormValidators.validatePositiveInteger(openingBalanceText, "Opening Balance", isRequired = true)
        }
    }
    val purchaseValidation = remember(purchaseText) {
        FormValidators.validateNonNegativeInteger(purchaseText, "Purchase", isRequired = false)
    }
    val aroedValidation = remember(aroedText, purchase) {
        if (purchase > 0) {
            FormValidators.validatePositiveInteger(aroedText, "AROED", isRequired = true)
        } else {
            FormValidators.validateNonNegativeInteger(aroedText, "AROED", isRequired = false)
        }
    }
    val damageValidation = remember(damageText, totalValue) {
        FormValidators.validateAmountWithinMax(
            value = damageText,
            fieldName = "Damage / Shortage",
            maxLimit = totalValue,
            maxLimitLabel = "Total Purchase Value",
            isRequired = false,
            allowZero = true
        )
    }

    // Section 1 (Sales) Validations
    val cashSalesValidation = remember(cashSalesText) {
        FormValidators.validatePositiveInteger(cashSalesText, "Cash Sales", isRequired = true)
    }
    val cardSalesValidation = remember(cardSalesText) {
        FormValidators.validateNonNegativeInteger(cardSalesText, "Card Sales", isRequired = false)
    }

    // Section 2 (Bank Deposit) Validations
    val bankDepositDateValidation = remember(bankDepositDate, transactionDate) {
        if (bankDepositDate <= transactionDate) {
            ValidationResult.error("Bank Deposit Date must be after transaction date (minimum next day).")
        } else {
            ValidationResult.Valid
        }
    }
    val depositAmountValidation = remember(depositAmountText, availableCashForDeposit) {
        if (availableCashForDeposit > 0) {
            FormValidators.validateAmountWithinMax(
                value = depositAmountText,
                fieldName = "Deposit Amount",
                maxLimit = availableCashForDeposit,
                maxLimitLabel = "Available Cash Balance",
                isRequired = true,
                allowZero = false
            )
        } else {
            FormValidators.validatePositiveInteger(depositAmountText, "Deposit Amount", isRequired = true)
        }
    }

    val section0Errors = remember(openingBalanceValidation, purchaseValidation, aroedValidation, damageValidation) {
        listOfNotNull(
            openingBalanceValidation.errorMessage,
            purchaseValidation.errorMessage,
            aroedValidation.errorMessage,
            damageValidation.errorMessage
        )
    }
    val section1Errors = remember(cashSalesValidation, cardSalesValidation) {
        listOfNotNull(
            cashSalesValidation.errorMessage,
            cardSalesValidation.errorMessage
        )
    }
    val section2Errors = remember(depositAmountValidation, bankDepositDateValidation) {
        listOfNotNull(
            depositAmountValidation.errorMessage,
            bankDepositDateValidation.errorMessage
        )
    }
    val currentSectionErrors = when (selectedSectionIndex) {
        0 -> section0Errors
        1 -> section1Errors
        2 -> section2Errors
        else -> emptyList()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .testTag("add_outlet_transaction_dialog"),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
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
                                text = when {
                                    isReadOnly -> "View Past Record (#$slNo)"
                                    isEditMode && isPastRecord -> "Modify Past Date Entry (#$slNo)"
                                    isEditMode -> "Modify Daily Entry (#$slNo)"
                                    else -> "New Daily Entry"
                                },
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = when {
                                    isReadOnly -> "${shop.shopName} (${shop.outletCode}) • Past Record (Read-Only)"
                                    isEditMode && isPastRecord -> "${shop.shopName} (${shop.outletCode}) • Admin & RIC Authorized"
                                    isEditMode -> "${shop.shopName} (${shop.outletCode}) • Editing Record #$slNo"
                                    else -> "${shop.shopName} (${shop.outletCode}) • Sl. No. #$slNo"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // RBAC Permission Banner
                if (isReadOnly) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = SaffronDark,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Past Record • View-Only Mode",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Shop employees can only view past records. Admin & RIC authorization is required to modify past records.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                } else if (isEditMode && isPastRecord) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = PrimaryIndigo.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                tint = PrimaryIndigo,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Past Date Modification Allowed",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = PrimaryIndigo
                                )
                                Text(
                                    text = "Authorized as ${userRoleEnum.displayName} to edit past date entries.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Section Tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedSectionIndex,
                    edgePadding = 0.dp,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedSectionIndex]),
                            color = EmeraldGreen
                        )
                    }
                ) {
                    sections.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedSectionIndex == index,
                            onClick = { selectedSectionIndex = index },
                            text = {
                                Text(
                                    text = title,
                                    fontWeight = if (selectedSectionIndex == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedSectionIndex == index) EmeraldGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }

                // Error Banner
                if (!validationError.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = validationError!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Content per Section
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    when (selectedSectionIndex) {
                        0 -> {
                            // ==========================================
                            // --- SECTION 1: PURCHASE TAB ---
                            // ==========================================
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    // Sl. No & Outlet Info
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Sl. No. (Auto Generated)",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "#$slNo",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = EmeraldGreen
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = PrimaryIndigo.copy(alpha = 0.1f)
                                        ) {
                                            Text(
                                                text = "Outlet: ${shop.outletCode}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = PrimaryIndigo,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Transaction Date Selector with Business Rules
                                    RestrictedDatePickerField(
                                        label = "Transaction Date",
                                        selectedDate = transactionDate,
                                        onDateSelected = { newDate ->
                                            if (canSelectPastDateForNewEntry || isEditMode || !TransactionPermissionRules.isPastDate(newDate)) {
                                                transactionDate = newDate
                                                if (bankDepositDate <= newDate) {
                                                    bankDepositDate = IndianCurrencyUtils.getStartOfDay(newDate + 86400000L)
                                                }
                                            }
                                        },
                                        maxDateMillis = IndianCurrencyUtils.getEndOfDay(System.currentTimeMillis()),
                                        minDateMillis = if (canSelectPastDateForNewEntry || isEditMode) {
                                            IndianCurrencyUtils.getStartOfDay(System.currentTimeMillis() - 180L * 86400000L)
                                        } else {
                                            IndianCurrencyUtils.getStartOfDay(System.currentTimeMillis())
                                        },
                                        ruleBadgeText = when {
                                            isReadOnly -> "Past Record • View Only"
                                            isEditMode && canModify -> "Admin & RIC Authorized"
                                            !canSelectPastDateForNewEntry -> "Shop Employee • Today Only"
                                            else -> "Past Dates Permitted"
                                        },
                                        ruleDescription = when {
                                            isReadOnly -> "Shop employees can only view past date records. Admin or RIC authorization is required to modify past records."
                                            isEditMode && canModify -> "Authorized: As an ${userRoleEnum.displayName}, you have permission to modify past date entries."
                                            !canSelectPastDateForNewEntry -> "Shop employee rule: Transactions can only be created for the current business date (Today). Past date entry requires Admin or RIC privileges."
                                            else -> "Admin & RIC Rule: Authorized to select past business dates for entry or modification."
                                        },
                                        quickOptions = if (canSelectPastDateForNewEntry && !isReadOnly) listOf(
                                            QuickDateOption("Today", IndianCurrencyUtils.getStartOfDay(System.currentTimeMillis())),
                                            QuickDateOption("Yesterday", IndianCurrencyUtils.getStartOfDay(System.currentTimeMillis() - 86400000L)),
                                            QuickDateOption("2 Days Ago", IndianCurrencyUtils.getStartOfDay(System.currentTimeMillis() - 2 * 86400000L))
                                        ) else listOf(
                                            QuickDateOption("Today", IndianCurrencyUtils.getStartOfDay(System.currentTimeMillis()))
                                        ),
                                        enabled = !isReadOnly && (canSelectPastDateForNewEntry || isEditMode),
                                        testTagPrefix = "tx_date_picker"
                                    )

                                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                                    // Opening Balance (Real-time validated)
                                    ValidatedNumberField(
                                        value = openingBalanceText,
                                        onValueChange = {
                                            if (!isOpeningBalanceFetched && !isReadOnly) {
                                                openingBalanceText = it
                                            }
                                        },
                                        label = "Opening Balance (Positive Integer)",
                                        validationResult = openingBalanceValidation,
                                        isRequired = !isOpeningBalanceFetched && !isReadOnly,
                                        readOnly = isOpeningBalanceFetched || isReadOnly,
                                        placeholder = "0",
                                        ruleBadgeText = if (isReadOnly) "Read Only" else if (isOpeningBalanceFetched) "Fetched" else "Positive Integer",
                                        supportingHelperText = if (isOpeningBalanceFetched) "✓ Fetched from previous day Closing Balance" else "ℹ One-time initial entry for this outlet",
                                        testTag = "input_opening_balance"
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Purchase (Real-time validated)
                                    ValidatedNumberField(
                                        value = purchaseText,
                                        onValueChange = { if (!isReadOnly) purchaseText = it },
                                        label = "Purchase (Positive Integer & Optional)",
                                        validationResult = purchaseValidation,
                                        isRequired = false,
                                        readOnly = isReadOnly,
                                        placeholder = "Enter purchase amount or 0",
                                        ruleBadgeText = if (isReadOnly) "Read Only" else "Optional • ≥ 0",
                                        testTag = "input_purchase"
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // 10% Margin (Auto calculation Purchase * 10% & Read only)
                                    Text(
                                        text = "10% Margin (Auto calculation: Purchase × 10% • Read-Only)",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = EmeraldGreen
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedTextField(
                                        value = margin10.toString(),
                                        onValueChange = {},
                                        readOnly = true,
                                        prefix = { Text("₹ ") },
                                        supportingText = {
                                            Text(
                                                "Calculated as 10% of ₹${IndianCurrencyUtils.formatInr(purchase, false)}",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("input_margin_10"),
                                        shape = RoundedCornerShape(10.dp)
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // AROED (Real-time validated: Compulsory when purchase entered)
                                    ValidatedNumberField(
                                        value = aroedText,
                                        onValueChange = { if (!isReadOnly) aroedText = it },
                                        label = "AROED (Positive Integer)",
                                        validationResult = aroedValidation,
                                        isRequired = purchase > 0 && !isReadOnly,
                                        readOnly = isReadOnly,
                                        placeholder = "0",
                                        ruleBadgeText = if (isReadOnly) "Read Only" else if (purchase > 0) "Required (Purchase > 0)" else "Optional",
                                        supportingHelperText = if (purchase == 0L) "Optional if no purchase entered" else null,
                                        testTag = "input_aroed"
                                    )

                                    Spacer(modifier = Modifier.height(18.dp))

                                    // Summary Card for Purchase Tab
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "Total Value of (Purchase + 10% Margin + AROED):",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                                Text(
                                                    text = IndianCurrencyUtils.formatInr(purchaseTabDisplayTotal),
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }

                                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "Total Value (Opening Bal + Purchase + Margin + AROED):",
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = IndianCurrencyUtils.formatInr(totalValue),
                                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        1 -> {
                            // ==========================================
                            // --- SECTION 2: SALES TAB ---
                            // ==========================================
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    // Total Value brought forward
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Total Value (from Purchase):",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                        Text(
                                            text = IndianCurrencyUtils.formatInr(totalValue),
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }

                                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                                    // Card Sales (Real-time validated)
                                    ValidatedNumberField(
                                        value = cardSalesText,
                                        onValueChange = { if (!isReadOnly) cardSalesText = it },
                                        label = "Card Sales (Positive Integer & Optional)",
                                        validationResult = cardSalesValidation,
                                        isRequired = false,
                                        readOnly = isReadOnly,
                                        placeholder = "0",
                                        ruleBadgeText = if (isReadOnly) "Read Only" else "Optional • ≥ 0",
                                        testTag = "input_card_sales"
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Cash Sales (Real-time validated: Compulsory)
                                    ValidatedNumberField(
                                        value = cashSalesText,
                                        onValueChange = { if (!isReadOnly) cashSalesText = it },
                                        label = "Cash Sales (Positive Integer)",
                                        validationResult = cashSalesValidation,
                                        isRequired = !isReadOnly,
                                        readOnly = isReadOnly,
                                        placeholder = "Required amount > 0",
                                        ruleBadgeText = if (isReadOnly) "Read Only" else "Compulsory • > 0",
                                        testTag = "input_cash_sales"
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Total Sales Display (Auto calculation Sum of Card Sales & Cash Sales)
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(10.dp)
                                     ) {
                                         Row(
                                             modifier = Modifier
                                                 .fillMaxWidth()
                                                 .padding(12.dp),
                                             horizontalArrangement = Arrangement.SpaceBetween,
                                             verticalAlignment = Alignment.CenterVertically
                                         ) {
                                             Text(
                                                 text = "Total Sales (Card + Cash):",
                                                 style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                             )
                                             Text(
                                                 text = IndianCurrencyUtils.formatInr(totalSales),
                                                 style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                 color = MaterialTheme.colorScheme.secondary
                                             )
                                         }
                                     }

                                     Spacer(modifier = Modifier.height(14.dp))

                                     // Damage (Real-time validated: Positive integer & Admin / RIC entry)
                                     val canEditDamage = (isAdmin || userRoleEnum == UserRole.RIC) && !isReadOnly
                                     ValidatedNumberField(
                                         value = damageText,
                                         onValueChange = {
                                             if (canEditDamage) {
                                                 damageText = it
                                             }
                                         },
                                         label = "Damage (Positive Integer)",
                                         validationResult = damageValidation,
                                         isRequired = false,
                                         readOnly = !canEditDamage,
                                         placeholder = "0",
                                         ruleBadgeText = if (isReadOnly) "Read Only" else if (canEditDamage) "Admin & RIC Entry • ≤ Total Value" else "Admin / RIC Only",
                                         supportingHelperText = if (isReadOnly) "Read-only past record view." else if (!canEditDamage) "This field can only be entered/modified by an Admin or Regional In-Charge (RIC)." else "Cannot exceed Total Value: ₹${IndianCurrencyUtils.formatInr(totalValue)}",
                                         testTag = "input_damage"
                                     )

                                    Spacer(modifier = Modifier.height(18.dp))

                                    // Closing Balance Display (Auto calculation: Total Value - Total Sales - Damage)
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "Closing Balance Formula:",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                                )
                                                Text(
                                                    text = "Total Value - Total Sales - Damage",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Closing Balance:",
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                                )
                                                Text(
                                                    text = IndianCurrencyUtils.formatInr(closingBalance),
                                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        2 -> {
                            // ==========================================
                            // --- SECTION 3: BANK DEPOSIT TAB ---
                            // ==========================================
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    // Transaction Date Display
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Transaction Date:",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                        Text(
                                            text = IndianCurrencyUtils.formatDate(transactionDate),
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                    }

                                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                                    // Bank Deposit Date Selector with Business Rules
                                    val nextDayStart = remember(transactionDate) {
                                        IndianCurrencyUtils.getStartOfDay(transactionDate + 86400000L)
                                    }
                                    RestrictedDatePickerField(
                                        label = "Bank Deposit Date",
                                        selectedDate = bankDepositDate,
                                        onDateSelected = { newDate ->
                                            if (newDate <= transactionDate) {
                                                validationError = "Bank Deposit Date must be next date! Cannot select same date or previous date."
                                            } else {
                                                bankDepositDate = newDate
                                                validationError = null
                                            }
                                        },
                                        minDateMillis = nextDayStart,
                                        ruleBadgeText = if (isReadOnly) "Read Only" else "Next Day (Strict)",
                                        ruleDescription = "Deposit date must be after transaction date (minimum next day). Same-day and previous dates are prohibited.",
                                        quickOptions = if (!isReadOnly) listOf(
                                            QuickDateOption("Next Day", nextDayStart),
                                            QuickDateOption("+2 Days", nextDayStart + 86400000L),
                                            QuickDateOption("+3 Days", nextDayStart + 2 * 86400000L)
                                        ) else emptyList(),
                                        enabled = !isReadOnly,
                                        testTagPrefix = "dep_date_picker"
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Deposit Amount (Real-time validated: Positive integers & compulsory)
                                    ValidatedNumberField(
                                        value = depositAmountText,
                                        onValueChange = { if (!isReadOnly) depositAmountText = it },
                                        label = "Deposit Amount (Positive Integer)",
                                        validationResult = depositAmountValidation,
                                        isRequired = !isReadOnly,
                                        readOnly = isReadOnly,
                                        placeholder = "Required amount > 0",
                                        ruleBadgeText = if (isReadOnly) "Read Only" else "Compulsory • ≤ Cash Balance",
                                        supportingHelperText = if (availableCashForDeposit > 0) "Available Cash Balance: ₹${IndianCurrencyUtils.formatInr(availableCashForDeposit)}" else null,
                                        testTag = "input_deposit_amount"
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Upload Challan Photo (Camera or Gallery)
                                    Text(
                                        text = "Challan Photo Upload",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = if (isReadOnly) "Bank deposit counterfoil / challan receipt" else "Upload bank deposit counterfoil / challan receipt",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    if (!isReadOnly) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = { cameraLauncher.launch() },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("btn_camera_capture"),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.CameraAlt,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Camera")
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    galleryLauncher.launch(
                                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                    )
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("btn_gallery_upload"),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.PhotoLibrary,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Gallery")
                                            }
                                        }
                                    }

                                    // Challan Photo Preview if attached
                                    if (challanPhotoUri.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                AsyncImage(
                                                    model = File(challanPhotoUri),
                                                    contentDescription = "Challan Preview",
                                                    modifier = Modifier
                                                        .size(56.dp)
                                                        .clip(RoundedCornerShape(8.dp)),
                                                    contentScale = ContentScale.Crop
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "Challan Attached",
                                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = EmeraldGreen
                                                    )
                                                    Text(
                                                        text = File(challanPhotoUri).name,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                if (!isReadOnly) {
                                                    IconButton(onClick = { challanPhotoUri = "" }) {
                                                        Icon(
                                                            Icons.Default.Delete,
                                                            contentDescription = "Remove photo",
                                                            tint = MaterialTheme.colorScheme.error
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    } else if (isReadOnly) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "No challan document uploaded.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Total Bank Deposit Display: (Bank Deposit + Card Sales)
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "Bank Deposit Amount:",
                                                    style = MaterialTheme.typography.labelMedium
                                                )
                                                Text(
                                                    text = IndianCurrencyUtils.formatInr(depositAmount),
                                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                                )
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "Card Sales (Digital Settlement):",
                                                    style = MaterialTheme.typography.labelMedium
                                                )
                                                Text(
                                                    text = IndianCurrencyUtils.formatInr(cardSales),
                                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                                )
                                            }

                                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Total Bank Deposit (Deposit + Card):",
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = IndianCurrencyUtils.formatInr(bankDepositTabDisplayTotal),
                                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Optional Notes
                                    OutlinedTextField(
                                        value = notesText,
                                        onValueChange = { if (!isReadOnly) notesText = it },
                                        readOnly = isReadOnly,
                                        label = { Text("Notes / Remarks (Optional)") },
                                        placeholder = { Text("Any remark regarding deposit or cash") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Real-time Form Validation Feedback Banner for current section (only when editable)
                if (!isReadOnly) {
                    FormValidationSummaryBanner(
                        errors = currentSectionErrors,
                        title = "Attention needed in ${sections[selectedSectionIndex]} Section:"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Navigation & Submit Bottom Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectedSectionIndex > 0) {
                        OutlinedButton(
                            onClick = {
                                validationError = null
                                selectedSectionIndex -= 1
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Previous")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(10.dp))
                    }

                    if (selectedSectionIndex < sections.size - 1) {
                        Button(
                            onClick = {
                                if (!isReadOnly && currentSectionErrors.isNotEmpty()) {
                                    validationError = currentSectionErrors.first()
                                } else {
                                    validationError = null
                                    selectedSectionIndex += 1
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                        ) {
                            Text("Next Step")
                        }
                    } else if (isReadOnly) {
                        // View-Only Close Button
                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.testTag("btn_close_view_only")
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Close (View Only)",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        // Submit or Update Button
                        Button(
                            onClick = {
                                // Comprehensive validations across all 3 sections
                                if (section0Errors.isNotEmpty()) {
                                    validationError = section0Errors.first()
                                    selectedSectionIndex = 0
                                    return@Button
                                }
                                if (section1Errors.isNotEmpty()) {
                                    validationError = section1Errors.first()
                                    selectedSectionIndex = 1
                                    return@Button
                                }
                                if (section2Errors.isNotEmpty()) {
                                    validationError = section2Errors.first()
                                    selectedSectionIndex = 2
                                    return@Button
                                }

                                validationError = null
                                if (isEditMode && onUpdate != null) {
                                    val updatedEntity = (initialTransaction ?: OutletTransactionEntity(
                                        shopOutletCode = shop.outletCode,
                                        slNo = slNo,
                                        transactionDate = transactionDate,
                                        openingBalance = openingBalance,
                                        isOpeningBalanceManual = !isOpeningBalanceFetched,
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
                                        notes = notesText.trim()
                                    )).copy(
                                        transactionDate = transactionDate,
                                        openingBalance = openingBalance,
                                        isOpeningBalanceManual = !isOpeningBalanceFetched,
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
                                        notes = notesText.trim()
                                    )
                                    onUpdate(updatedEntity)
                                } else {
                                    onSubmit(
                                        shop.outletCode,
                                        slNo,
                                        transactionDate,
                                        openingBalance,
                                        !isOpeningBalanceFetched,
                                        purchase,
                                        aroed,
                                        cardSales,
                                        cashSales,
                                        damage,
                                        bankDepositDate,
                                        depositAmount,
                                        challanPhotoUri,
                                        notesText.trim()
                                    )
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isEditMode) PrimaryIndigo else EmeraldGreen
                            ),
                            modifier = Modifier.testTag("btn_submit_transaction")
                        ) {
                            Icon(
                                if (isEditMode) Icons.Default.Save else Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isEditMode) "Save Modifications" else "Submit Entry",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
