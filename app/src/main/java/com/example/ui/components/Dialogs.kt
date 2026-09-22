package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.window.Dialog
import com.example.data.model.DepositSource
import com.example.data.model.DepositStatus
import com.example.data.model.PaymentMode
import com.example.data.model.PaymentStatus
import com.example.data.model.PurchaseCategory
import com.example.domain.DenominationTally
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.EmeraldGreenLight
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.SaffronDark
import com.example.ui.theme.SaffronGold
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.IndianCurrencyUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddSaleDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        customerName: String,
        invoiceNo: String,
        itemsSummary: String,
        amount: Double,
        gstRate: Double,
        paymentMode: String,
        paymentStatus: String,
        notes: String
    ) -> Unit
) {
    var customerName by remember { mutableStateOf("") }
    var invoiceNo by remember { mutableStateOf("BILL-${(System.currentTimeMillis() / 1000) % 100000}") }
    var itemsSummary by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var selectedGst by remember { mutableDoubleStateOf(5.0) }
    var selectedMode by remember { mutableStateOf(PaymentMode.CASH) }
    var selectedStatus by remember { mutableStateOf(PaymentStatus.PAID) }
    var notes by remember { mutableStateOf("") }

    val amountValue = amountStr.toDoubleOrNull() ?: 0.0
    val wordsText = if (amountValue > 0) IndianCurrencyUtils.amountInWords(amountValue) else ""

    val customerNameValidation = remember(customerName) {
        FormValidators.validateRequiredText(customerName, "Customer / Party Name")
    }
    val amountValidation = remember(amountStr) {
        FormValidators.validatePositiveDecimal(amountStr, "Sale Amount", isRequired = true)
    }
    val saleFormErrors = remember(customerNameValidation, amountValidation) {
        listOfNotNull(customerNameValidation.errorMessage, amountValidation.errorMessage)
    }
    val isFormValid = customerNameValidation.isValid && amountValidation.isValid

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Record New Sale",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Validated Customer Name
                ValidatedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = "Customer / Party Name",
                    validationResult = customerNameValidation,
                    isRequired = true,
                    placeholder = "e.g. Counter Sale / Rahul Traders",
                    testTag = "sale_customer_input"
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = invoiceNo,
                    onValueChange = { invoiceNo = it },
                    label = { Text("Invoice / Bill No.") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Validated Sale Amount
                ValidatedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = "Sale Amount",
                    validationResult = amountValidation,
                    isRequired = true,
                    prefix = "₹ ",
                    placeholder = "0.00",
                    keyboardType = KeyboardType.Decimal,
                    ruleBadgeText = "Compulsory • > 0",
                    testTag = "sale_amount_input"
                )

                if (wordsText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = wordsText,
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldGreen,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = itemsSummary,
                    onValueChange = { itemsSummary = it },
                    label = { Text("Items / Description") },
                    placeholder = { Text("e.g. 2x Shirts, Fabric Roll") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "GST Rate",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(0.0, 5.0, 12.0, 18.0, 28.0).forEach { rate ->
                        FilterChip(
                            selected = selectedGst == rate,
                            onClick = { selectedGst = rate },
                            label = { Text("${rate.toInt()}%") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryIndigo,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Payment Mode",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PaymentMode.entries.forEach { mode ->
                        FilterChip(
                            selected = selectedMode == mode,
                            onClick = { selectedMode = mode },
                            label = { Text(mode.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (mode.isCash) EmeraldGreen else PrimaryIndigo,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Reference (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                FormValidationSummaryBanner(
                    errors = saleFormErrors,
                    title = "Please resolve before saving:"
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            if (isFormValid) {
                                onConfirm(
                                    customerName,
                                    invoiceNo,
                                    itemsSummary,
                                    amountValue,
                                    selectedGst,
                                    selectedMode.name,
                                    selectedStatus.name,
                                    notes
                                )
                                onDismiss()
                            }
                        },
                        enabled = isFormValid,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        modifier = Modifier.testTag("sale_submit_button")
                    ) {
                        Text("Save Sale")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddPurchaseDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        vendorName: String,
        invoiceNo: String,
        category: String,
        amount: Double,
        gstRate: Double,
        paymentMode: String,
        paymentStatus: String,
        notes: String
    ) -> Unit
) {
    var vendorName by remember { mutableStateOf("") }
    var invoiceNo by remember { mutableStateOf("INV-${(System.currentTimeMillis() / 1000) % 100000}") }
    var selectedCategory by remember { mutableStateOf(PurchaseCategory.INVENTORY.label) }
    var amountStr by remember { mutableStateOf("") }
    var selectedGst by remember { mutableDoubleStateOf(5.0) }
    var selectedMode by remember { mutableStateOf(PaymentMode.BANK_TRANSFER) }
    var selectedStatus by remember { mutableStateOf(PaymentStatus.PAID) }
    var notes by remember { mutableStateOf("") }

    val amountValue = amountStr.toDoubleOrNull() ?: 0.0
    val wordsText = if (amountValue > 0) IndianCurrencyUtils.amountInWords(amountValue) else ""

    val vendorNameValidation = remember(vendorName) {
        FormValidators.validateRequiredText(vendorName, "Vendor / Supplier Name")
    }
    val amountValidation = remember(amountStr) {
        FormValidators.validatePositiveDecimal(amountStr, "Purchase Amount", isRequired = true)
    }
    val purchaseFormErrors = remember(vendorNameValidation, amountValidation) {
        listOfNotNull(vendorNameValidation.errorMessage, amountValidation.errorMessage)
    }
    val isFormValid = vendorNameValidation.isValid && amountValidation.isValid

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Record Purchase",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Validated Vendor Name
                ValidatedTextField(
                    value = vendorName,
                    onValueChange = { vendorName = it },
                    label = "Vendor / Supplier Name",
                    validationResult = vendorNameValidation,
                    isRequired = true,
                    placeholder = "e.g. Surat Textiles Ltd",
                    testTag = "purchase_vendor_input"
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = invoiceNo,
                    onValueChange = { invoiceNo = it },
                    label = { Text("Invoice / Bill No.") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Validated Purchase Amount
                ValidatedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = "Purchase Amount",
                    validationResult = amountValidation,
                    isRequired = true,
                    prefix = "₹ ",
                    placeholder = "0.00",
                    keyboardType = KeyboardType.Decimal,
                    ruleBadgeText = "Compulsory • > 0",
                    testTag = "purchase_amount_input"
                )

                if (wordsText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = wordsText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PurchaseCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat.label,
                            onClick = { selectedCategory = cat.label },
                            label = { Text(cat.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryIndigo,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Payment Mode",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PaymentMode.entries.forEach { mode ->
                        FilterChip(
                            selected = selectedMode == mode,
                            onClick = { selectedMode = mode },
                            label = { Text(mode.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryIndigo,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Goods Description") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                FormValidationSummaryBanner(
                    errors = purchaseFormErrors,
                    title = "Please resolve before saving:"
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            if (isFormValid) {
                                onConfirm(
                                    vendorName,
                                    invoiceNo,
                                    selectedCategory,
                                    amountValue,
                                    selectedGst,
                                    selectedMode.name,
                                    selectedStatus.name,
                                    notes
                                )
                                onDismiss()
                            }
                        },
                        enabled = isFormValid,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("purchase_submit_button")
                    ) {
                        Text("Save Purchase")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddDepositDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        bankName: String,
        accountNumberLast4: String,
        amount: Double,
        depositSource: String,
        referenceNumber: String,
        denominationBreakdown: String,
        notes: String,
        depositDate: Long
    ) -> Unit
) {
    var bankName by remember { mutableStateOf("State Bank of India (SBI)") }
    var accountNumberLast4 by remember { mutableStateOf("4920") }
    var selectedSource by remember { mutableStateOf(DepositSource.CASH_COUNTER) }
    var amountStr by remember { mutableStateOf("") }
    var referenceNumber by remember { mutableStateOf("UTR-${(System.currentTimeMillis() / 1000) % 100000}") }
    var denominationBreakdown by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var showDenomHelper by remember { mutableStateOf(false) }

    // Business rule: Bank deposits cannot be recorded on past or same-day dates.
    // Minimum allowed deposit date is strictly the next day (tomorrow) start of day.
    val minDepositDate = remember {
        IndianCurrencyUtils.getStartOfDay(System.currentTimeMillis() + 86400000L)
    }
    var depositDate by remember { mutableLongStateOf(minDepositDate) }

    val amountValue = amountStr.toDoubleOrNull() ?: 0.0
    val wordsText = if (amountValue > 0) IndianCurrencyUtils.amountInWords(amountValue) else ""

    val bankNameValidation = remember(bankName) {
        FormValidators.validateRequiredText(bankName, "Bank Name")
    }
    val amountValidation = remember(amountStr) {
        FormValidators.validatePositiveDecimal(amountStr, "Deposit Amount", isRequired = true)
    }
    val dateValidation = remember(depositDate, minDepositDate) {
        if (depositDate < minDepositDate) {
            ValidationResult.error("Bank deposits cannot be recorded on past or same-day dates. Please select tomorrow or a later date.")
        } else {
            ValidationResult.Valid
        }
    }
    val depositFormErrors = remember(bankNameValidation, amountValidation, dateValidation) {
        listOfNotNull(bankNameValidation.errorMessage, amountValidation.errorMessage, dateValidation.errorMessage)
    }
    val isFormValid = bankNameValidation.isValid && amountValidation.isValid && dateValidation.isValid

    if (showDenomHelper) {
        DenominationCalculatorDialog(
            initialTally = DenominationTally(),
            onDismiss = { showDenomHelper = false },
            onApply = { tally ->
                val calcTotal = tally.calculateTotal()
                amountStr = calcTotal.toString()
                denominationBreakdown = tally.toSummaryString()
                showDenomHelper = false
            }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Record Bank Deposit",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Date Picker Field restricting past or same-day dates
                RestrictedDatePickerField(
                    label = "Bank Deposit Date",
                    selectedDate = depositDate,
                    onDateSelected = { depositDate = it },
                    minDateMillis = minDepositDate,
                    ruleBadgeText = "Next Day or Later",
                    ruleDescription = "Bank deposits must be scheduled on the next day or later. Past and same-day dates are prohibited by business rule.",
                    quickOptions = listOf(
                        QuickDateOption("Tomorrow (Next Day)", minDepositDate),
                        QuickDateOption("+2 Days", minDepositDate + 86400000L),
                        QuickDateOption("+3 Days", minDepositDate + 2 * 86400000L)
                    ),
                    testTagPrefix = "add_deposit_date_picker"
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Deposit Source",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    DepositSource.entries.forEach { src ->
                        FilterChip(
                            selected = selectedSource == src,
                            onClick = { selectedSource = src },
                            label = { Text(src.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (src.isPhysicalCash) SaffronGold else PrimaryIndigo,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(modifier = Modifier.weight(1.5f)) {
                        ValidatedTextField(
                            value = bankName,
                            onValueChange = { bankName = it },
                            label = "Bank Name",
                            validationResult = bankNameValidation,
                            isRequired = true,
                            placeholder = "e.g. SBI, HDFC",
                            testTag = "deposit_bank_input"
                        )
                    }
                    OutlinedTextField(
                        value = accountNumberLast4,
                        onValueChange = { if (it.length <= 4) accountNumberLast4 = it },
                        label = { Text("A/C Last 4") },
                        placeholder = { Text("1234") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(modifier = Modifier.weight(1.2f)) {
                        ValidatedTextField(
                            value = amountStr,
                            onValueChange = { amountStr = it },
                            label = "Deposit Amount",
                            validationResult = amountValidation,
                            isRequired = true,
                            prefix = "₹ ",
                            placeholder = "0.00",
                            keyboardType = KeyboardType.Decimal,
                            ruleBadgeText = "Compulsory • > 0",
                            testTag = "deposit_amount_input"
                        )
                    }

                    if (selectedSource == DepositSource.CASH_COUNTER) {
                        OutlinedButton(
                            onClick = { showDenomHelper = true },
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .height(56.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Tally", fontSize = 12.sp)
                        }
                    }
                }

                if (wordsText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = wordsText,
                        style = MaterialTheme.typography.labelSmall,
                        color = SaffronDark,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (denominationBreakdown.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Notes: $denominationBreakdown",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = referenceNumber,
                    onValueChange = { referenceNumber = it },
                    label = { Text("Ref / Slip / UTR No.") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                FormValidationSummaryBanner(
                    errors = depositFormErrors,
                    title = "Please resolve before saving deposit:"
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            if (isFormValid) {
                                onConfirm(
                                    bankName,
                                    accountNumberLast4,
                                    amountValue,
                                    selectedSource.name,
                                    referenceNumber,
                                    denominationBreakdown,
                                    notes,
                                    depositDate
                                )
                                onDismiss()
                            }
                        },
                        enabled = isFormValid,
                        colors = ButtonDefaults.buttonColors(containerColor = SaffronGold),
                        modifier = Modifier.testTag("deposit_submit_button")
                    ) {
                        Text("Save Deposit")
                    }
                }
            }
        }
    }
}

/**
 * Backward-compatible overload for AddDepositDialog with 7 parameters.
 */
@Composable
fun AddDepositDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        bankName: String,
        accountNumberLast4: String,
        amount: Double,
        depositSource: String,
        referenceNumber: String,
        denominationBreakdown: String,
        notes: String
    ) -> Unit
) {
    AddDepositDialog(
        onDismiss = onDismiss,
        onConfirm = { bankName, accLast4, amount, source, ref, denom, notes, _ ->
            onConfirm(bankName, accLast4, amount, source, ref, denom, notes)
        }
    )
}

/**
 * Interactive Denomination Calculator for Indian Currency notes (₹2000, ₹500, ₹200, ₹100, ₹50, ₹20, ₹10, ₹5, Coins).
 */
@Composable
fun DenominationCalculatorDialog(
    initialTally: DenominationTally,
    onDismiss: () -> Unit,
    onApply: (DenominationTally) -> Unit
) {
    var n2000 by remember { mutableIntStateOf(initialTally.n2000) }
    var n500 by remember { mutableIntStateOf(initialTally.n500) }
    var n200 by remember { mutableIntStateOf(initialTally.n200) }
    var n100 by remember { mutableIntStateOf(initialTally.n100) }
    var n50 by remember { mutableIntStateOf(initialTally.n50) }
    var n20 by remember { mutableIntStateOf(initialTally.n20) }
    var n10 by remember { mutableIntStateOf(initialTally.n10) }
    var n5 by remember { mutableIntStateOf(initialTally.n5) }
    var coins by remember { mutableIntStateOf(initialTally.coins) }

    val currentTally = DenominationTally(n2000, n500, n200, n100, n50, n20, n10, n5, coins)
    val totalAmount = currentTally.calculateTotal()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Cash Denomination Tally",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = "Count physical notes & coins",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Denomination Rows
                DenominationRow(denomValue = 2000, count = n2000, onCountChange = { n2000 = it })
                DenominationRow(denomValue = 500, count = n500, onCountChange = { n500 = it })
                DenominationRow(denomValue = 200, count = n200, onCountChange = { n200 = it })
                DenominationRow(denomValue = 100, count = n100, onCountChange = { n100 = it })
                DenominationRow(denomValue = 50, count = n50, onCountChange = { n50 = it })
                DenominationRow(denomValue = 20, count = n20, onCountChange = { n20 = it })
                DenominationRow(denomValue = 10, count = n10, onCountChange = { n10 = it })
                DenominationRow(denomValue = 5, count = n5, onCountChange = { n5 = it })
                CoinsRow(count = coins, onCountChange = { coins = it })

                Spacer(modifier = Modifier.height(14.dp))

                // Total Box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = EmeraldGreenLight,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Total Counted Cash",
                            style = MaterialTheme.typography.labelMedium,
                            color = EmeraldGreen
                        )
                        Text(
                            text = IndianCurrencyUtils.formatInr(totalAmount),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = EmeraldGreen
                        )
                        if (totalAmount > 0) {
                            Text(
                                text = IndianCurrencyUtils.amountInWords(totalAmount),
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldGreen,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = {
                        n2000 = 0
                        n500 = 0
                        n200 = 0
                        n100 = 0
                        n50 = 0
                        n20 = 0
                        n10 = 0
                        n5 = 0
                        coins = 0
                    }) {
                        Text("Reset All", color = MaterialTheme.colorScheme.error)
                    }

                    Row {
                        OutlinedButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onApply(currentTally)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                        ) {
                            Text("Apply Total")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DenominationRow(
    denomValue: Int,
    count: Int,
    onCountChange: (Int) -> Unit
) {
    val rowTotal = denomValue * count

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .width(62.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFE2E8F0))
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "₹$denomValue",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { if (count > 0) onCountChange(count - 1) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
            }

            Box(
                modifier = Modifier
                    .width(48.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(6.dp))
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
            }

            IconButton(
                onClick = { onCountChange(count + 1) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
            }
        }

        Text(
            text = IndianCurrencyUtils.formatInr(rowTotal.toDouble()),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (rowTotal > 0) TextPrimary else TextMuted,
            modifier = Modifier.width(80.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

@Composable
fun CoinsRow(
    count: Int,
    onCountChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .width(62.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFFEF3C7))
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Coins (₹)",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = SaffronDark
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { if (count >= 5) onCountChange(count - 5) else onCountChange(0) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
            }

            Box(
                modifier = Modifier
                    .width(48.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(6.dp))
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
            }

            IconButton(
                onClick = { onCountChange(count + 5) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
            }
        }

        Text(
            text = IndianCurrencyUtils.formatInr(count.toDouble()),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (count > 0) TextPrimary else TextMuted,
            modifier = Modifier.width(80.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

/**
 * Confirmation dialog for locking the Daily Reconciliation audit.
 */
@Composable
fun ConfirmReconciliationDialog(
    expectedClosingCash: Double,
    actualCountedCash: Double,
    discrepancy: Double,
    onDismiss: () -> Unit,
    onConfirm: (auditorName: String, notes: String) -> Unit
) {
    var auditorName by remember { mutableStateOf("Store Manager") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Finalize Daily Reconciliation",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column {
                Text(
                    text = "This will lock and audit today's cash & bank tallies.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Expected Cash:", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                    Text(IndianCurrencyUtils.formatInr(expectedClosingCash), fontWeight = FontWeight.SemiBold)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Counted Physical Cash:", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                    Text(IndianCurrencyUtils.formatInr(actualCountedCash), fontWeight = FontWeight.SemiBold)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Variance / Discrepancy:", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                    Text(
                        text = IndianCurrencyUtils.formatInr(discrepancy),
                        fontWeight = FontWeight.Bold,
                        color = when {
                            Math.abs(discrepancy) < 0.5 -> EmeraldGreen
                            discrepancy > 0 -> SaffronGold
                            else -> MaterialTheme.colorScheme.error
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = auditorName,
                    onValueChange = { auditorName = it },
                    label = { Text("Reconciled By (Name / Role)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Audit Remarks / Notes") },
                    placeholder = { Text("e.g. All drawer cash verified") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(auditorName, notes)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                modifier = Modifier.testTag("confirm_recon_submit_button")
            ) {
                Text("Confirm & Lock")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
