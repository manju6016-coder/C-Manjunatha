package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.OutletTransactionEntity
import com.example.ui.theme.EmeraldGreen
import com.example.util.IndianCurrencyUtils

@Composable
fun EditDamageDialog(
    transaction: OutletTransactionEntity,
    onDismiss: () -> Unit,
    onSave: (newDamage: Long) -> Unit
) {
    var damageText by remember { mutableStateOf(transaction.damage.toString()) }
    val newDamage = damageText.toLongOrNull() ?: 0L
    val recalculatedClosing = remember(newDamage) {
        transaction.totalValue - transaction.totalSales - newDamage
    }

    val maxAllowedDamage = remember(transaction.totalValue, transaction.totalSales) {
        (transaction.totalValue - transaction.totalSales).coerceAtLeast(0L)
    }

    val damageValidation = remember(damageText, maxAllowedDamage) {
        FormValidators.validateAmountWithinMax(
            value = damageText,
            fieldName = "Damage",
            maxLimit = maxAllowedDamage,
            maxLimitLabel = "Net Total Value (Total Value - Sales)",
            isRequired = false,
            allowZero = true
        )
    }
    val damageErrors = remember(damageValidation) {
        listOfNotNull(damageValidation.errorMessage)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Damage (Admin Entry)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Sl. No. #${transaction.slNo} • ${IndianCurrencyUtils.formatDate(transaction.transactionDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Total Value:", style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = IndianCurrencyUtils.formatInr(transaction.totalValue),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Total Sales:", style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = "- ${IndianCurrencyUtils.formatInr(transaction.totalSales)}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Validated Damage Input Field
                ValidatedNumberField(
                    value = damageText,
                    onValueChange = { damageText = it },
                    label = "Damage (Positive Integer)",
                    validationResult = damageValidation,
                    isRequired = false,
                    placeholder = "0",
                    ruleBadgeText = "≤ ₹${IndianCurrencyUtils.formatInr(maxAllowedDamage)}",
                    supportingHelperText = "Max allowed damage based on closing balance: ₹${IndianCurrencyUtils.formatInr(maxAllowedDamage)}",
                    testTag = "edit_damage_input"
                )

                Spacer(modifier = Modifier.height(14.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (damageValidation.isValid) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "New Closing Balance:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (damageValidation.isValid) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = IndianCurrencyUtils.formatInr(recalculatedClosing),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = if (damageValidation.isValid) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                if (damageErrors.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    FormValidationSummaryBanner(errors = damageErrors)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (damageValidation.isValid) {
                        onSave(newDamage)
                    }
                },
                enabled = damageValidation.isValid,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                modifier = Modifier.testTag("btn_save_damage")
            ) {
                Text("Save Damage")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
