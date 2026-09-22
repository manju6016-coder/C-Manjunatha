package com.example.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.OutletTransactionEntity
import com.example.data.model.ShopEntity
import com.example.data.model.UserRole
import com.example.ui.components.AnimatedCurrencyText
import com.example.ui.components.AnimatedProgressBar
import com.example.ui.components.PulsingStatusDot
import com.example.ui.components.bounceClick
import com.example.ui.components.EditDamageDialog
import com.example.ui.components.RecordDailySalesDialog
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.SaffronGold
import com.example.util.IndianCurrencyUtils
import com.example.util.TransactionPermissionRules

@Composable
fun SalesTabScreen(
    shop: ShopEntity?,
    transactions: List<OutletTransactionEntity>,
    salesTabTotalValue: Long,
    userRole: String,
    onUpdateDamage: (txId: Long, damage: Long) -> Unit,
    onEditTransaction: ((OutletTransactionEntity) -> Unit)? = null,
    onRecordDailySales: ((timestamp: Long, cashSales: Long, cardSales: Long, notes: String, onDone: (Boolean) -> Unit) -> Unit)? = null,
    onAddNewEntry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var editingDamageTransaction by remember { mutableStateOf<OutletTransactionEntity?>(null) }
    var showRecordDailySalesDialog by remember { mutableStateOf(false) }
    var editingSalesTransaction by remember { mutableStateOf<OutletTransactionEntity?>(null) }

    val userRoleEnum = remember(userRole) { UserRole.fromCode(userRole) }
    val isAdminOrRic = remember(userRoleEnum) {
        userRoleEnum == UserRole.ADMIN || userRoleEnum == UserRole.RIC
    }

    // Sales Tracking Component calculations
    val totalCashSales = remember(transactions) { transactions.sumOf { it.cashSales } }
    val totalCardSales = remember(transactions) { transactions.sumOf { it.cardSales } }
    val totalSalesSum = remember(totalCashSales, totalCardSales) { totalCashSales + totalCardSales }
    val cashRatio = remember(totalSalesSum, totalCashSales) {
        if (totalSalesSum > 0) totalCashSales.toFloat() / totalSalesSum.toFloat() else 0.5f
    }
    val latestTimestamp = remember(transactions) {
        transactions.firstOrNull()?.createdAt?.takeIf { it > 0 }
            ?: transactions.firstOrNull()?.transactionDate
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("sales_tab_screen")
    ) {
        if (shop == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No shop selected.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            return
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // =================================================================
            // Top Sales Tracking Dashboard Card: Total Value, Cash vs Card split
            // =================================================================
            item {
                Spacer(modifier = Modifier.height(8.dp))
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sales_tab_total_banner"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = EmeraldGreen
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
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
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.PointOfSale,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "DAILY SALES TRACKING",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White.copy(alpha = 0.85f),
                                        letterSpacing = 1.sp
                                    )
                                    if (latestTimestamp != null) {
                                        Text(
                                            text = "Latest: ${IndianCurrencyUtils.formatDateTime(latestTimestamp)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.75f)
                                        )
                                    }
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "${transactions.size} Entries",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Total Recorded Sales (Cash + Card)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )

                        AnimatedCurrencyText(
                            amount = salesTabTotalValue,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Cash vs Card breakdown pills
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Cash Sales Pill
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Payments,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = "Cash Payments",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.8f)
                                        )
                                        AnimatedCurrencyText(
                                            amount = totalCashSales,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            // Card Sales Pill
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CreditCard,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = "Card Payments",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.8f)
                                        )
                                        AnimatedCurrencyText(
                                            amount = totalCardSales,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        // Progress split bar
                        if (totalSalesSum > 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            AnimatedProgressBar(
                                progress = cashRatio,
                                modifier = Modifier.fillMaxWidth(),
                                color = Color.White,
                                trackColor = PrimaryIndigo,
                                height = 6.dp
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Direct "Record Daily Sales" Action Button
                        Button(
                            onClick = {
                                editingSalesTransaction = null
                                showRecordDailySalesDialog = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .bounceClick()
                                .testTag("btn_record_daily_sales_banner"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = EmeraldGreen
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                Icons.Default.PointOfSale,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Record Daily Sales (Cash & Card)",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            if (transactions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
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
                                Icons.Default.PointOfSale,
                                contentDescription = null,
                                tint = EmeraldGreen,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No sales entries yet for ${shop.shopName}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Record daily cash and card payment totals with timestamps to track outlet sales.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                            )
                            Button(
                                onClick = {
                                    editingSalesTransaction = null
                                    showRecordDailySalesDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .bounceClick()
                                    .testTag("btn_record_first_sales")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Record Today's Sales", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                items(transactions, key = { it.id }) { tx ->
                    SalesTransactionCard(
                        tx = tx,
                        userRole = userRole,
                        onEditDamage = { editingDamageTransaction = tx },
                        onEditSales = {
                            editingSalesTransaction = tx
                            showRecordDailySalesDialog = true
                        },
                        onEditTransaction = onEditTransaction
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Floating Action Button to Record Daily Sales
        ExtendedFloatingActionButton(
            onClick = {
                editingSalesTransaction = null
                showRecordDailySalesDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .bounceClick()
                .testTag("fab_record_daily_sales"),
            containerColor = EmeraldGreen,
            contentColor = Color.White,
            icon = { Icon(Icons.Default.PointOfSale, contentDescription = null) },
            text = { Text("Record Sales", fontWeight = FontWeight.Bold) }
        )

        // Record Daily Sales Dialog (Dedicated Component with Cash vs Card, Timestamps, Room DB)
        if (showRecordDailySalesDialog || editingSalesTransaction != null) {
            RecordDailySalesDialog(
                shop = shop,
                existingTransaction = editingSalesTransaction,
                onDismiss = {
                    showRecordDailySalesDialog = false
                    editingSalesTransaction = null
                },
                onSaveSales = { timestamp, cash, card, notes ->
                    if (onRecordDailySales != null) {
                        onRecordDailySales(timestamp, cash, card, notes) { success ->
                            showRecordDailySalesDialog = false
                            editingSalesTransaction = null
                        }
                    } else {
                        showRecordDailySalesDialog = false
                        editingSalesTransaction = null
                    }
                }
            )
        }

        // Edit Damage Dialog for Admin & RIC
        editingDamageTransaction?.let { tx ->
            EditDamageDialog(
                transaction = tx,
                onDismiss = { editingDamageTransaction = null },
                onSave = { newDamage ->
                    onUpdateDamage(tx.id, newDamage)
                    editingDamageTransaction = null
                }
            )
        }
    }
}

@Composable
fun SalesTransactionCard(
    tx: OutletTransactionEntity,
    userRole: String,
    onEditDamage: () -> Unit,
    onEditSales: (() -> Unit)? = null,
    onEditTransaction: ((OutletTransactionEntity) -> Unit)? = null
) {
    val userRoleEnum = remember(userRole) { UserRole.fromCode(userRole) }
    val isAdminOrRic = remember(userRoleEnum) {
        userRoleEnum == UserRole.ADMIN || userRoleEnum == UserRole.RIC
    }
    val isPastRecord = remember(tx.transactionDate) {
        TransactionPermissionRules.isPastDate(tx.transactionDate)
    }
    val canModify = remember(userRoleEnum, tx.transactionDate) {
        TransactionPermissionRules.canModifyEntry(userRoleEnum, tx.transactionDate)
    }
    val canModifyDamage = isAdminOrRic && canModify

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .testTag("sales_card_${tx.slNo}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row with Date & Timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = EmeraldGreen.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Sl. No. #${tx.slNo}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = EmeraldGreen,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = IndianCurrencyUtils.formatDate(tx.transactionDate),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (tx.createdAt > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = IndianCurrencyUtils.formatTime(tx.createdAt),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isPastRecord) {
                        if (!canModify) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFFF3E0),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB74D)),
                                modifier = Modifier.padding(end = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = Color(0xFFE65100),
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Past • View Only",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = Color(0xFFE65100)
                                    )
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFE0F2FE),
                                modifier = Modifier.padding(end = 6.dp)
                            ) {
                                Text(
                                    text = "Past • Editable (${userRoleEnum.displayName})",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = Color(0xFF0369A1),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "Total: ${IndianCurrencyUtils.formatInr(tx.totalValue)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

            // Breakdown: Card Sales & Cash Sales
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Card Sales (Optional)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = IndianCurrencyUtils.formatInr(tx.cardSales),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Cash Sales (Compulsory)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = IndianCurrencyUtils.formatInr(tx.cashSales),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = EmeraldGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Total Sales Subtotal
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total Sales (Card + Cash):",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    AnimatedCurrencyText(
                        amount = tx.totalSales,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = EmeraldGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Damage Row (Admin & RIC entry; View-only for shop employees on past records)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Damage",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        if (isAdminOrRic) {
                            Text(
                                text = "• Admin & RIC Entry",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldGreen,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(11.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = if (isPastRecord) "Past Record (View Only)" else "Admin / RIC Only",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Text(
                        text = IndianCurrencyUtils.formatInr(tx.damage),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (tx.damage > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }

                if (canModifyDamage) {
                    OutlinedButton(
                        onClick = onEditDamage,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .bounceClick()
                            .testTag("btn_edit_damage_${tx.slNo}")
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Edit Damage",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Closing Balance Display (Auto calculation Total Value - Total Sales - Damage)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Closing Balance:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "Total Value - Total Sales - Damage",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    AnimatedCurrencyText(
                        amount = tx.closingBalance,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            // Record Inspection / Edit Sales Action Button Row
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onEditSales != null && canModify) {
                    OutlinedButton(
                        onClick = onEditSales,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .bounceClick()
                            .testTag("btn_edit_daily_sales_${tx.slNo}")
                    ) {
                        Icon(
                            Icons.Default.PointOfSale,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = EmeraldGreen
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Edit Sales",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = EmeraldGreen
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                if (onEditTransaction != null) {
                    if (canModify) {
                        OutlinedButton(
                            onClick = { onEditTransaction(tx) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("btn_edit_sales_tx_${tx.slNo}")
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Full Details",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    } else {
                        OutlinedButton(
                            onClick = { onEditTransaction(tx) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("btn_view_sales_tx_${tx.slNo}")
                        ) {
                            Icon(
                                Icons.Default.Visibility,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "View Record (Read-Only)",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium)
                            )
                        }
                    }
                }
            }
        }
    }
}
