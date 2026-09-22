package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BankDepositEntity
import com.example.data.model.DepositSource
import com.example.ui.components.LedgerEmptyState
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.SaffronDark
import com.example.ui.theme.SaffronGold
import com.example.ui.theme.SaffronLight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.IndianCurrencyUtils

@Composable
fun BankDepositsScreen(
    deposits: List<BankDepositEntity>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onAddDepositClick: () -> Unit,
    onDeleteDeposit: (BankDepositEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalDeposits = deposits.sumOf { it.amount }
    val cashDeposits = deposits.filter { DepositSource.fromString(it.depositSource).isPhysicalCash }.sumOf { it.amount }
    val settlements = deposits.filter { !DepositSource.fromString(it.depositSource).isPhysicalCash }.sumOf { it.amount }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddDepositClick,
                containerColor = SaffronGold,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("add_deposit_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Record Deposit")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Deposit", fontWeight = FontWeight.Bold)
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Summary Header Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(14.dp),
                color = SaffronLight,
                border = CardDefaults.outlinedCardBorder()
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
                            text = "Total Bank Inflow",
                            style = MaterialTheme.typography.labelMedium,
                            color = SaffronDark
                        )
                        Text(
                            text = IndianCurrencyUtils.formatInr(totalDeposits),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = SaffronDark
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Cash to Branch: ${IndianCurrencyUtils.formatInr(cashDeposits)}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = SaffronDark
                        )
                        Text(
                            text = "Auto Settlements: ${IndianCurrencyUtils.formatInr(settlements)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SaffronDark
                        )
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search bank, reference no, account...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("deposits_search_input")
            )

            Spacer(modifier = Modifier.height(6.dp))

            // List of Deposits
            if (deposits.isEmpty()) {
                LedgerEmptyState(
                    title = "No Bank Deposits Recorded",
                    subtitle = "Record branch cash deposits, CDM machine slips, or UPI auto-settlements.",
                    icon = Icons.Default.AccountBalance,
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp, top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(deposits) { deposit ->
                        DepositItemCard(deposit = deposit, onDelete = { onDeleteDeposit(deposit) })
                    }
                }
            }
        }
    }
}

@Composable
fun DepositItemCard(
    deposit: BankDepositEntity,
    onDelete: () -> Unit
) {
    val source = DepositSource.fromString(deposit.depositSource)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = deposit.bankName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = "A/C •••• ${deposit.accountNumberLast4}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }

                Text(
                    text = IndianCurrencyUtils.formatInr(deposit.amount),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = SaffronDark
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (source.isPhysicalCash) SaffronLight else Color(0xFFDBEAFE)
                ) {
                    Text(
                        text = source.label,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (source.isPhysicalCash) SaffronDark else PrimaryIndigo,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                if (deposit.denominationBreakdown.isNotBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = deposit.denominationBreakdown,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ref: ${deposit.referenceNumber} • ${IndianCurrencyUtils.formatDateTime(deposit.date)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
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
}
