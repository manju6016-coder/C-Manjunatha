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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.SaffronDark
import com.example.ui.theme.SaffronGold
import com.example.util.IndianCurrencyUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Quick date selection shortcut option.
 */
data class QuickDateOption(
    val label: String,
    val dateMillis: Long
)

/**
 * Converts a local timestamp into UTC midnight millis for Material 3 DatePicker.
 */
fun localTimestampToUtcMidnight(localMillis: Long): Long {
    val localCal = Calendar.getInstance().apply { timeInMillis = localMillis }
    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(
            localCal.get(Calendar.YEAR),
            localCal.get(Calendar.MONTH),
            localCal.get(Calendar.DAY_OF_MONTH),
            0,
            0,
            0
        )
    }
    return utcCal.timeInMillis
}

/**
 * Converts a UTC midnight timestamp from Material 3 DatePicker into local start-of-day millis.
 */
fun utcMidnightToLocalStartOfDay(utcMillis: Long): Long {
    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
    val localCal = Calendar.getInstance().apply {
        clear()
        set(
            utcCal.get(Calendar.YEAR),
            utcCal.get(Calendar.MONTH),
            utcCal.get(Calendar.DAY_OF_MONTH),
            0,
            0,
            0
        )
    }
    return localCal.timeInMillis
}

/**
 * Reusable Form Date Picker Field with built-in trigger button, status chips,
 * and business rule restrictions banner.
 */
@Composable
fun RestrictedDatePickerField(
    label: String,
    selectedDate: Long,
    onDateSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
    minDateMillis: Long? = null,
    maxDateMillis: Long? = null,
    ruleBadgeText: String? = null,
    ruleDescription: String? = null,
    quickOptions: List<QuickDateOption> = emptyList(),
    testTagPrefix: String = "date_picker",
    enabled: Boolean = true
) {
    var showDialog by remember { mutableStateOf(false) }

    // Check if the current selected date violates business rules
    val isViolatingMin = minDateMillis != null && selectedDate < minDateMillis
    val isViolatingMax = maxDateMillis != null && selectedDate > maxDateMillis
    val isInvalid = isViolatingMin || isViolatingMax

    val errorMessage = when {
        isViolatingMin -> "Date cannot be on or before restricted minimum date"
        isViolatingMax -> "Future dates are not permitted"
        else -> null
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )

            if (ruleBadgeText != null) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isInvalid) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        SaffronGold.copy(alpha = 0.15f)
                    }
                ) {
                    Text(
                        text = ruleBadgeText,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isInvalid) MaterialTheme.colorScheme.error else SaffronDark,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Interactive Trigger Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(enabled = enabled) { showDialog = true }
                .border(
                    width = if (isInvalid) 1.5.dp else 1.dp,
                    color = if (isInvalid) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    },
                    shape = RoundedCornerShape(12.dp)
                )
                .testTag("${testTagPrefix}_field"),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isInvalid) {
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                } else {
                                    PrimaryIndigo.copy(alpha = 0.12f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Select Date",
                            tint = if (isInvalid) MaterialTheme.colorScheme.error else PrimaryIndigo,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = IndianCurrencyUtils.formatDate(selectedDate),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (isInvalid) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                        val weekdayFormatter = remember { SimpleDateFormat("EEEE", Locale.ENGLISH) }
                        Text(
                            text = weekdayFormatter.format(selectedDate),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PrimaryIndigo.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "Change",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = PrimaryIndigo,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Business Rule Notice or Error Caption
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else if (ruleDescription != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "ℹ $ruleDescription",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }

    if (showDialog) {
        RestrictedDatePickerDialog(
            initialDateMillis = selectedDate,
            minDateMillis = minDateMillis,
            maxDateMillis = maxDateMillis,
            title = "Select $label",
            ruleDescription = ruleDescription,
            quickOptions = quickOptions,
            testTagPrefix = testTagPrefix,
            onDismiss = { showDialog = false },
            onDateSelected = { newDate ->
                onDateSelected(newDate)
                showDialog = false
            }
        )
    }
}

/**
 * Material 3 Date Picker Dialog enforcing application business rules
 * via SelectableDates, quick selection chips, and live validation.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RestrictedDatePickerDialog(
    initialDateMillis: Long,
    minDateMillis: Long?,
    maxDateMillis: Long?,
    title: String,
    ruleDescription: String?,
    onDismiss: () -> Unit,
    onDateSelected: (Long) -> Unit,
    quickOptions: List<QuickDateOption> = emptyList(),
    testTagPrefix: String = "date_picker"
) {
    val minUtc = remember(minDateMillis) {
        minDateMillis?.let { localTimestampToUtcMidnight(it) }
    }
    val maxUtc = remember(maxDateMillis) {
        maxDateMillis?.let { localTimestampToUtcMidnight(it) }
    }

    // SelectableDates implementation strictly restricting dates at the M3 component level
    val selectableDates = remember(minUtc, maxUtc) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                if (minUtc != null && utcTimeMillis < minUtc) return false
                if (maxUtc != null && utcTimeMillis > maxUtc) return false
                return true
            }

            override fun isSelectableYear(year: Int): Boolean = true
        }
    }

    // Initial date adjusted to fall within valid range if needed
    val initialUtcMillis = remember(initialDateMillis, minUtc, maxUtc) {
        var utc = localTimestampToUtcMidnight(initialDateMillis)
        if (minUtc != null && utc < minUtc) {
            utc = minUtc
        }
        if (maxUtc != null && utc > maxUtc) {
            utc = maxUtc
        }
        utc
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialUtcMillis,
        selectableDates = selectableDates
    )

    val selectedLocalMillis by remember {
        derivedStateOf {
            datePickerState.selectedDateMillis?.let {
                utcMidnightToLocalStartOfDay(it)
            }
        }
    }

    // Live validation against business rules
    val isSelectionValid by remember {
        derivedStateOf {
            val local = selectedLocalMillis
            if (local == null) {
                false
            } else {
                val passesMin = minDateMillis == null || local >= minDateMillis
                val passesMax = maxDateMillis == null || local <= maxDateMillis
                passesMin && passesMax
            }
        }
    }

    val validationErrorMessage by remember {
        derivedStateOf {
            val local = selectedLocalMillis
            when {
                local == null -> "Please select a date"
                minDateMillis != null && local < minDateMillis -> "Selected date cannot be in the past or on the prohibited date."
                maxDateMillis != null && local > maxDateMillis -> "Selected date cannot be in the future."
                else -> null
            }
        }
    }

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    selectedLocalMillis?.let { onDateSelected(it) }
                },
                enabled = isSelectionValid,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                modifier = Modifier.testTag("${testTagPrefix}_confirm_btn")
            ) {
                Text("Confirm Date")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("${testTagPrefix}_cancel_btn")
            ) {
                Text("Cancel")
            }
        },
        modifier = Modifier.testTag("${testTagPrefix}_dialog")
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp)
        ) {
            // Header Info & Business Rule Banner
            if (!ruleDescription.isNullOrBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("${testTagPrefix}_rule_banner"),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = SaffronGold.copy(alpha = 0.12f))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = SaffronDark,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = ruleDescription,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = SaffronDark
                        )
                    }
                }
            }

            // Quick Selection Chips
            if (quickOptions.isNotEmpty()) {
                Text(
                    text = "Quick Select:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )

                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickOptions.forEach { opt ->
                        val optUtc = localTimestampToUtcMidnight(opt.dateMillis)
                        val isAllowed = (minUtc == null || optUtc >= minUtc) && (maxUtc == null || optUtc <= maxUtc)
                        val isSelected = datePickerState.selectedDateMillis == optUtc

                        FilterChip(
                            selected = isSelected,
                            enabled = isAllowed,
                            onClick = {
                                if (isAllowed) {
                                    datePickerState.selectedDateMillis = optUtc
                                }
                            },
                            label = {
                                Text(
                                    text = opt.label,
                                    fontSize = 12.sp
                                )
                            },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldGreen,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("${testTagPrefix}_quick_${opt.label.lowercase().replace(" ", "_")}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
            }

            // Material 3 DatePicker Calendar
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = EmeraldGreen,
                    todayDateBorderColor = PrimaryIndigo
                ),
                showModeToggle = false
            )

            // Validation Error Banner if user selected something out of bounds
            if (!isSelectionValid && validationErrorMessage != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("${testTagPrefix}_error_banner"),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = validationErrorMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}
