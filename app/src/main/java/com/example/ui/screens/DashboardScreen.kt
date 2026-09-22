package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingCart
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BankDepositEntity
import com.example.data.model.PurchaseEntity
import com.example.data.model.ReconciliationStatus
import com.example.data.model.SaleEntity
import com.example.domain.DailyReconciliationSummary
import com.example.ui.components.FinancialMetricCard
import com.example.ui.components.LedgerEmptyState
import com.example.ui.components.PaymentModeChip
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

sealed class LedgerActivityItem(val timestamp: Long) {
    data class SaleItem(val sale: SaleEntity) : LedgerActivityItem(sale.date)
    data class PurchaseItem(val purchase: PurchaseEntity) : LedgerActivityItem(purchase.date)
    data class DepositItem(val deposit: BankDepositEntity) : LedgerActivityItem(deposit.date)
}

@Composable
fun DashboardScreen(
    summary: DailyReconciliationSummary,
    sales: List<SaleEntity>,
    purchases: List<PurchaseEntity>,
    deposits: List<BankDepositEntity>,
    onOpenReconciliationTab: () -> Unit,
    onAddSaleClick: () -> Unit,
    onAddPurchaseClick: () -> Unit,
    onAddDepositClick: () -> Unit,
    onDeleteSale: (SaleEntity) -> Unit,
    onDeletePurchase: (PurchaseEntity) -> Unit,
    onDeleteDeposit: (BankDepositEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val combinedActivities = (
            sales.map { LedgerActivityItem.SaleItem(it) } +
                    purchases.map { LedgerActivityItem.PurchaseItem(it) } +
                    deposits.map { LedgerActivityItem.DepositItem(it) }
            ).sortedByDescending { it.timestamp }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Automated Daily Reconciliation Hero Banner
        item {
            DailyReconciliationHeroBanner(
                summary = summary,
                onReconcileClick = onOpenReconciliationTab
            )
        }

        // 2. Key Metrics Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FinancialMetricCard(
                        title = "Today's Sales",
                        amount = summary.totalSalesAmount,
                        subtitle = "Cash: ${IndianCurrencyUtils.formatInr(summary.totalCashSales)} | Digital: ${IndianCurrencyUtils.formatInr(summary.totalDigitalSales)}",
                        accentColor = EmeraldGreen,
                        icon = Icons.Default.PointOfSale,
                        modifier = Modifier.weight(1f)
                    )
                    FinancialMetricCard(
                        title = "Today's Purchases",
                        amount = summary.totalPurchasesAmount,
                        subtitle = "Cash: ${IndianCurrencyUtils.formatInr(summary.totalCashPurchases)} | Bank: ${IndianCurrencyUtils.formatInr(summary.totalBankPurchases)}",
                        accentColor = CrimsonRed,
                        icon = Icons.Default.ShoppingCart,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FinancialMetricCard(
                        title = "Bank Deposits",
                        amount = summary.totalDepositsAmount,
                        subtitle = "Cash to Bank: ${IndianCurrencyUtils.formatInr(summary.totalCashDepositedToBank)}",
                        accentColor = SaffronGold,
                        icon = Icons.Default.AccountBalance,
                        modifier = Modifier.weight(1f)
                    )
                    FinancialMetricCard(
                        title = "Expected Drawer Cash",
                        amount = summary.expectedClosingCash,
                        subtitle = if (summary.isLocked) "Reconciled & Locked" else "Pending physical count",
                        accentColor = PrimaryIndigo,
                        icon = Icons.Default.Payments,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenReconciliationTab
                    )
                }
            }
        }

        // 3. Quick Action Buttons
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Quick Actions",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onAddSaleClick,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_add_sale_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sale", fontSize = 13.sp)
                        }
                        Button(
                            onClick = onAddPurchaseClick,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_add_purchase_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Purchase", fontSize = 13.sp)
                        }
                        Button(
                            onClick = onAddDepositClick,
                            colors = ButtonDefaults.buttonColors(containerColor = SaffronGold),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_add_deposit_btn")
                        ) {
                            Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Deposit", fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // 4. Automated Daily Flow Breakdown
        item {
            DailyFlowSummaryCard(summary = summary, onReconcileClick = onOpenReconciliationTab)
        }

        // 5. Recent Activity Feed Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today's Ledger Activity (${combinedActivities.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
            }
        }

        // 6. Recent Activity List
        if (combinedActivities.isEmpty()) {
            item {
                LedgerEmptyState(
                    title = "No Entries for this Day",
                    subtitle = "Record sales, purchases, or bank deposits to begin automated daily reconciliation.",
                    icon = Icons.Default.ReceiptLong
                )
            }
        } else {
            items(combinedActivities) { item ->
                when (item) {
                    is LedgerActivityItem.SaleItem -> {
                        SaleActivityCard(sale = item.sale, onDelete = { onDeleteSale(item.sale) })
                    }
                    is LedgerActivityItem.PurchaseItem -> {
                        PurchaseActivityCard(purchase = item.purchase, onDelete = { onDeletePurchase(item.purchase) })
                    }
                    is LedgerActivityItem.DepositItem -> {
                        DepositActivityCard(deposit = item.deposit, onDelete = { onDeleteDeposit(item.deposit) })
                    }
                }
            }
        }
    }
}

/**
 * Automated Daily Reconciliation Top Hero Banner
 */
@Composable
fun DailyReconciliationHeroBanner(
    summary: DailyReconciliationSummary,
    onReconcileClick: () -> Unit
) {
    val (bgColor, borderColor, icon, title, subtitle, actionText) = when {
        summary.isLocked -> {
            Tuple6(
                EmeraldGreenLight,
                EmeraldGreen,
                Icons.Default.CheckCircle,
                "Daily Reconciliation Completed",
                "Drawer audited and balanced by ${summary.existingReconciliation?.auditorName ?: "Manager"}.",
                "View Audit"
            )
        }
        summary.status == ReconciliationStatus.BALANCED -> {
            Tuple6(
                EmeraldGreenLight,
                EmeraldGreen,
                Icons.Default.CheckCircle,
                "Drawer Cash Balanced (₹0 Diff)",
                "Counted physical cash matches expected closing cash perfectly.",
                "Confirm & Lock"
            )
        }
        summary.status == ReconciliationStatus.SHORTAGE -> {
            Tuple6(
                CrimsonLight,
                CrimsonRed,
                Icons.Default.Warning,
                "Cash Shortage: ${IndianCurrencyUtils.formatInr(Math.abs(summary.discrepancy))}",
                "Counted cash is less than expected transactions. Please audit drawer.",
                "Review Variance"
            )
        }
        summary.status == ReconciliationStatus.SURPLUS -> {
            Tuple6(
                SaffronLight,
                SaffronGold,
                Icons.Default.Warning,
                "Excess Cash: +${IndianCurrencyUtils.formatInr(summary.discrepancy)}",
                "Counted cash is greater than recorded receipts.",
                "Review Variance"
            )
        }
        else -> {
            Tuple6(
                Color(0xFFEFF6FF),
                PrimaryIndigoLight,
                Icons.Default.FactCheck,
                "Daily Reconciliation Pending",
                "Expected closing drawer cash is ${IndianCurrencyUtils.formatInr(summary.expectedClosingCash)}. Verify count.",
                "Reconcile Now"
            )
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(borderColor)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onReconcileClick() }
            .testTag("reconciliation_hero_banner")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(borderColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = borderColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedButton(
                onClick = onReconcileClick,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = borderColor),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(actionText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

data class Tuple6<A, B, C, D, E, F>(
    val a: A, val b: B, val c: C, val d: D, val e: E, val f: F
)

/**
 * Detailed Automated Daily Cash Flow & Bank Movement Card
 */
@Composable
fun DailyFlowSummaryCard(
    summary: DailyReconciliationSummary,
    onReconcileClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daily Reconciliation Matrix",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                ReconciliationStatusBadge(status = summary.status)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Cash Drawer Matrix
            Text(
                text = "1. Cash Drawer Movement",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = PrimaryIndigo
            )
            Spacer(modifier = Modifier.height(6.dp))

            FlowRowLine(label = "Opening Cash Float", value = summary.openingCash, isPositive = null)
            FlowRowLine(label = "(+) Cash Sales Collected", value = summary.totalCashSales, isPositive = true)
            FlowRowLine(label = "(-) Cash Purchases Paid", value = -summary.totalCashPurchases, isPositive = false)
            FlowRowLine(label = "(-) Cash Deposited to Bank", value = -summary.totalCashDepositedToBank, isPositive = false)

            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = BorderSubtle)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Expected Closing Cash:",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Text(
                    text = IndianCurrencyUtils.formatInr(summary.expectedClosingCash),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = PrimaryIndigo
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bank Position Matrix
            Text(
                text = "2. Bank & Digital Settlements",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = SaffronDark
            )
            Spacer(modifier = Modifier.height(6.dp))

            FlowRowLine(label = "UPI / Card / Bank Sales", value = summary.totalDigitalSales, isPositive = true)
            FlowRowLine(label = "Cash & Other Bank Deposits", value = summary.totalDepositsAmount, isPositive = true)
            FlowRowLine(label = "(-) Bank Purchases / Outflows", value = -summary.totalBankPurchases, isPositive = false)

            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = BorderSubtle)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Net Bank Position Today:",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Text(
                    text = IndianCurrencyUtils.formatInr(summary.netBankMovement),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (summary.netBankMovement >= 0) EmeraldGreen else CrimsonRed
                )
            }
        }
    }
}

@Composable
fun FlowRowLine(
    label: String,
    value: Double,
    isPositive: Boolean?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        val prefix = when (isPositive) {
            true -> "+ "
            false -> "- "
            null -> ""
        }
        val displayVal = Math.abs(value)
        Text(
            text = "$prefix${IndianCurrencyUtils.formatInr(displayVal)}",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = when (isPositive) {
                true -> EmeraldGreen
                false -> CrimsonRed
                null -> TextPrimary
            }
        )
    }
}

@Composable
fun SaleActivityCard(
    sale: SaleEntity,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(EmeraldGreenLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = null,
                    tint = EmeraldGreen,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = sale.customerName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    PaymentModeChip(paymentModeStr = sale.paymentMode)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${sale.invoiceNo} • ${sale.itemsSummary}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "+ ${IndianCurrencyUtils.formatInr(sale.amount)}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = EmeraldGreen
                )
                Text(
                    text = IndianCurrencyUtils.formatTime(sale.date),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun PurchaseActivityCard(
    purchase: PurchaseEntity,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(CrimsonLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = CrimsonRed,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = purchase.vendorName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    PaymentModeChip(paymentModeStr = purchase.paymentMode)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${purchase.invoiceNo} • ${purchase.category}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "- ${IndianCurrencyUtils.formatInr(purchase.amount)}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = CrimsonRed
                )
                Text(
                    text = IndianCurrencyUtils.formatTime(purchase.date),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun DepositActivityCard(
    deposit: BankDepositEntity,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(SaffronLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = SaffronGold,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = deposit.bankName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "A/C ••••${deposit.accountNumberLast4} • ${deposit.referenceNumber}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = IndianCurrencyUtils.formatInr(deposit.amount),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = SaffronDark
                )
                Text(
                    text = IndianCurrencyUtils.formatTime(deposit.date),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
