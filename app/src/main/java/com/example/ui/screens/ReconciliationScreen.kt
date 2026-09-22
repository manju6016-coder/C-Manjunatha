package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.data.model.DailyReconciliationEntity
import com.example.data.model.ReconciliationStatus
import com.example.domain.DailyReconciliationSummary
import com.example.domain.DenominationTally
import com.example.ui.components.CoinsRow
import com.example.ui.components.ConfirmReconciliationDialog
import com.example.ui.components.DenominationRow
import com.example.ui.components.ReconciliationStatusBadge
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CrimsonDark
import com.example.ui.theme.CrimsonLight
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.EmeraldGreenDark
import com.example.ui.theme.EmeraldGreenLight
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.PrimaryIndigoLight
import com.example.ui.theme.SaffronDark
import com.example.ui.theme.SaffronGold
import com.example.ui.theme.SaffronLight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.IndianCurrencyUtils

@Composable
fun ReconciliationScreen(
    summary: DailyReconciliationSummary,
    denominationTally: DenominationTally,
    allPastReconciliations: List<DailyReconciliationEntity>,
    onOpeningCashChange: (Double) -> Unit,
    onDenominationChange: (DenominationTally) -> Unit,
    onResetDenominations: () -> Unit,
    onConfirmReconciliation: (auditorName: String, notes: String) -> Unit,
    onUnlockReconciliation: (DailyReconciliationEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var showConfirmDialog by remember { mutableStateOf(false) }
    var editingOpeningCash by remember { mutableStateOf(false) }
    var openingCashInput by remember { mutableStateOf(summary.openingCash.toInt().toString()) }

    if (showConfirmDialog) {
        ConfirmReconciliationDialog(
            expectedClosingCash = summary.expectedClosingCash,
            actualCountedCash = summary.actualCountedCash,
            discrepancy = summary.discrepancy,
            onDismiss = { showConfirmDialog = false },
            onConfirm = { auditor, notes ->
                onConfirmReconciliation(auditor, notes)
                showConfirmDialog = false
            }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Reconciliation Overview Header Card
        item {
            ReconciliationHeaderCard(
                summary = summary,
                editingOpeningCash = editingOpeningCash,
                openingCashInput = openingCashInput,
                onOpeningCashInput = { openingCashInput = it },
                onSaveOpeningCash = {
                    val amount = openingCashInput.toDoubleOrNull() ?: 0.0
                    onOpeningCashChange(amount)
                    editingOpeningCash = false
                },
                onToggleEditOpeningCash = { editingOpeningCash = !editingOpeningCash }
            )
        }

        // 2. Physical Drawer Cash Count Section (Interactive Indian Denominations)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Physical Cash Drawer Verification",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Text(
                                text = "Tally physical ₹ notes and coins in register",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }

                        if (!summary.isLocked) {
                            TextButton(onClick = onResetDenominations) {
                                Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear", fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (summary.isLocked) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Reconciled & Locked: ${summary.existingReconciliation?.denominationTallyJson ?: "Physical cash verified"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary
                                )
                            }
                        }
                    } else {
                        // Interactive Denomination Rows
                        DenominationRow(
                            denomValue = 2000,
                            count = denominationTally.n2000,
                            onCountChange = { onDenominationChange(denominationTally.copy(n2000 = it)) }
                        )
                        DenominationRow(
                            denomValue = 500,
                            count = denominationTally.n500,
                            onCountChange = { onDenominationChange(denominationTally.copy(n500 = it)) }
                        )
                        DenominationRow(
                            denomValue = 200,
                            count = denominationTally.n200,
                            onCountChange = { onDenominationChange(denominationTally.copy(n200 = it)) }
                        )
                        DenominationRow(
                            denomValue = 100,
                            count = denominationTally.n100,
                            onCountChange = { onDenominationChange(denominationTally.copy(n100 = it)) }
                        )
                        DenominationRow(
                            denomValue = 50,
                            count = denominationTally.n50,
                            onCountChange = { onDenominationChange(denominationTally.copy(n50 = it)) }
                        )
                        DenominationRow(
                            denomValue = 20,
                            count = denominationTally.n20,
                            onCountChange = { onDenominationChange(denominationTally.copy(n20 = it)) }
                        )
                        DenominationRow(
                            denomValue = 10,
                            count = denominationTally.n10,
                            onCountChange = { onDenominationChange(denominationTally.copy(n10 = it)) }
                        )
                        DenominationRow(
                            denomValue = 5,
                            count = denominationTally.n5,
                            onCountChange = { onDenominationChange(denominationTally.copy(n5 = it)) }
                        )
                        CoinsRow(
                            count = denominationTally.coins,
                            onCountChange = { onDenominationChange(denominationTally.copy(coins = it)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Counted Cash Total Banner
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFEFF6FF),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Total Counted Physical Cash",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PrimaryIndigo
                                )
                                Text(
                                    text = IndianCurrencyUtils.formatInr(summary.actualCountedCash),
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = PrimaryIndigo
                                )
                                if (summary.actualCountedCash > 0) {
                                    Text(
                                        text = IndianCurrencyUtils.amountInWords(summary.actualCountedCash),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PrimaryIndigoLight,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Automated Reconciliation Result & Variance Card
        item {
            VarianceAnalysisCard(summary = summary)
        }

        // 4. Lock & Confirm Button
        item {
            if (summary.isLocked) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = EmeraldGreenLight,
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Audited & Verified",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = EmeraldGreenDark
                            )
                            Text(
                                text = "By ${summary.existingReconciliation?.auditorName ?: "Manager"} • ${IndianCurrencyUtils.formatDateTime(summary.existingReconciliation?.reconciledAt ?: System.currentTimeMillis())}",
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldGreenDark
                            )
                        }

                        summary.existingReconciliation?.let { recon ->
                            OutlinedButton(
                                onClick = { onUnlockReconciliation(recon) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonDark)
                            ) {
                                Text("Re-Audit", fontSize = 12.sp)
                            }
                        }
                    }
                }
            } else {
                Button(
                    onClick = { showConfirmDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (summary.status) {
                            ReconciliationStatus.BALANCED -> EmeraldGreen
                            ReconciliationStatus.SHORTAGE -> CrimsonRed
                            else -> PrimaryIndigo
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("finalize_reconciliation_button")
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Confirm & Lock Today's Reconciliation",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 5. Historical Reconciliations Log Header
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.History, contentDescription = null, tint = PrimaryIndigo)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Past Reconciliation Audits (${allPastReconciliations.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
            }
        }

        // 6. Historical Reconciliations List
        if (allPastReconciliations.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No prior audit history available.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(allPastReconciliations) { recon ->
                HistoricalReconciliationItem(recon = recon)
            }
        }
    }
}

@Composable
fun ReconciliationHeaderCard(
    summary: DailyReconciliationSummary,
    editingOpeningCash: Boolean,
    openingCashInput: String,
    onOpeningCashInput: (String) -> Unit,
    onSaveOpeningCash: () -> Unit,
    onToggleEditOpeningCash: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Automated Cash Flow Matrix",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = IndianCurrencyUtils.formatDate(summary.dateEpochStart),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
                ReconciliationStatusBadge(status = summary.status)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Opening Cash with Edit Option
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Opening Cash Float:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    if (!summary.isLocked) {
                        IconButton(onClick = onToggleEditOpeningCash, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Opening Float", tint = PrimaryIndigoLight, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                if (editingOpeningCash) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = openingCashInput,
                            onValueChange = onOpeningCashInput,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.width(100.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(onClick = onSaveOpeningCash, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                            Text("OK", fontSize = 11.sp)
                        }
                    }
                } else {
                    Text(
                        text = IndianCurrencyUtils.formatInr(summary.openingCash),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Formula Breakdown
            FormulaRow(label = "(+) Cash Sales Collected", value = summary.totalCashSales, isPositive = true)
            FormulaRow(label = "(-) Cash Purchases Paid Out", value = summary.totalCashPurchases, isPositive = false)
            FormulaRow(label = "(-) Cash Deposited to Bank", value = summary.totalCashDepositedToBank, isPositive = false)

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = BorderSubtle)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Expected Closing Cash:",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Text(
                    text = IndianCurrencyUtils.formatInr(summary.expectedClosingCash),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = PrimaryIndigo
                )
            }
        }
    }
}

@Composable
fun FormulaRow(
    label: String,
    value: Double,
    isPositive: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        val prefix = if (isPositive) "+ " else "- "
        Text(
            text = "$prefix${IndianCurrencyUtils.formatInr(value)}",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = if (isPositive) EmeraldGreen else CrimsonRed
        )
    }
}

@Composable
fun VarianceAnalysisCard(summary: DailyReconciliationSummary) {
    val discrepancy = summary.discrepancy
    val (cardColor, borderColor, icon, title, desc) = when {
        Math.abs(discrepancy) < 0.5 -> {
            Tuple5(
                EmeraldGreenLight,
                EmeraldGreen,
                Icons.Default.CheckCircle,
                "PERFECTLY BALANCED (₹0 Discrepancy)",
                "Physical cash counted in drawer matches expected cash from today's transactions exactly."
            )
        }
        discrepancy > 0 -> {
            Tuple5(
                SaffronLight,
                SaffronGold,
                Icons.Default.Warning,
                "SURPLUS / EXCESS CASH (+${IndianCurrencyUtils.formatInr(discrepancy)})",
                "Physical cash in drawer exceeds recorded sales and deposits. Verify if any cash sale or inflow was unbilled."
            )
        }
        else -> {
            Tuple5(
                CrimsonLight,
                CrimsonRed,
                Icons.Default.Error,
                "SHORTAGE / DEFICIT (${IndianCurrencyUtils.formatInr(discrepancy)})",
                "Physical cash in drawer is LESS than expected transactions. Check for missing cash, unrecorded expenses, or counter mistakes."
            )
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(borderColor)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = borderColor, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Expected Cash: ${IndianCurrencyUtils.formatInr(summary.expectedClosingCash)}", style = MaterialTheme.typography.labelMedium)
                Text("Counted Cash: ${IndianCurrencyUtils.formatInr(summary.actualCountedCash)}", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

data class Tuple5<A, B, C, D, E>(
    val a: A, val b: B, val c: C, val d: D, val e: E
)

@Composable
fun HistoricalReconciliationItem(recon: DailyReconciliationEntity) {
    val status = ReconciliationStatus.entries.find { it.name == recon.status } ?: ReconciliationStatus.BALANCED

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = IndianCurrencyUtils.formatDate(recon.dateEpochStart),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                ReconciliationStatusBadge(status = status)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Auditor: ${recon.auditorName} • Reconciled on ${IndianCurrencyUtils.formatDateTime(recon.reconciledAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Expected: ${IndianCurrencyUtils.formatInr(recon.expectedClosingCash)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    text = "Counted: ${IndianCurrencyUtils.formatInr(recon.actualCountedCash)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    text = "Variance: ${IndianCurrencyUtils.formatInr(recon.discrepancy)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = when {
                        Math.abs(recon.discrepancy) < 0.5 -> EmeraldGreen
                        recon.discrepancy > 0 -> SaffronGold
                        else -> CrimsonRed
                    }
                )
            }

            if (recon.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Remarks: ${recon.notes}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
        }
    }
}
