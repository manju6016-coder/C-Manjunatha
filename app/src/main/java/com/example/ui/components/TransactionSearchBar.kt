package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.OutletTransactionEntity
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.PrimaryIndigoDark
import com.example.util.IndianCurrencyUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Quick date filters for narrowing transaction entries.
 */
enum class QuickDateFilter(val label: String) {
    ALL("All Dates"),
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month")
}

/**
 * Utility functions for filtering transactions by query (shop name, outlet code, date)
 * and date ranges.
 */
object TransactionFilterUtils {

    /**
     * Checks whether an outlet transaction matches the user's search query.
     * Supports matching against:
     * - Shop Name (case-insensitive substring)
     * - Outlet Code (e.g. "SH-101", "OUT-01")
     * - Sl. No. (e.g. "1", "#1", "Sl 1")
     * - Transaction Date in multiple common formats:
     *   "18/09/2026", "18/09", "18-09-2026", "2026-09-18", "18 Sep", "September", "Sep 18 2026"
     */
    fun matchesQuery(
        tx: OutletTransactionEntity,
        shopName: String,
        query: String
    ): Boolean {
        if (query.isBlank()) return true
        val q = query.trim().lowercase(Locale.ENGLISH)

        // 1. Shop Name or Outlet Code match
        if (shopName.lowercase(Locale.ENGLISH).contains(q)) return true
        if (tx.shopOutletCode.lowercase(Locale.ENGLISH).contains(q)) return true

        // 2. Sl No match
        if (tx.slNo.toString() == q || "#${tx.slNo}".contains(q) || "sl ${tx.slNo}".contains(q)) return true

        // 3. Date matches
        val date = Date(tx.transactionDate)
        val formats = listOf(
            SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH),
            SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH),
            SimpleDateFormat("d/M/yyyy", Locale.ENGLISH),
            SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH),
            SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH),
            SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH),
            SimpleDateFormat("MMMM", Locale.ENGLISH),
            SimpleDateFormat("MMM", Locale.ENGLISH),
            SimpleDateFormat("dd MMM", Locale.ENGLISH),
            SimpleDateFormat("dd/MM", Locale.ENGLISH)
        )

        for (sdf in formats) {
            if (sdf.format(date).lowercase(Locale.ENGLISH).contains(q)) {
                return true
            }
        }

        return false
    }

    /**
     * Checks whether a transaction falls into the selected quick date filter.
     */
    fun matchesDateFilter(
        txDate: Long,
        filter: QuickDateFilter
    ): Boolean {
        if (filter == QuickDateFilter.ALL) return true
        val now = System.currentTimeMillis()
        val startOfToday = IndianCurrencyUtils.getStartOfDay(now)
        val endOfToday = IndianCurrencyUtils.getEndOfDay(now)

        return when (filter) {
            QuickDateFilter.ALL -> true
            QuickDateFilter.TODAY -> txDate in startOfToday..endOfToday
            QuickDateFilter.THIS_WEEK -> {
                val cal = Calendar.getInstance()
                cal.timeInMillis = now
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                val startOfWeek = IndianCurrencyUtils.getStartOfDay(cal.timeInMillis)
                val rollingWeekStart = IndianCurrencyUtils.getStartOfDay(now - (6 * 24 * 60 * 60 * 1000L))
                val effectiveStart = minOf(startOfWeek, rollingWeekStart)
                txDate in effectiveStart..endOfToday
            }
            QuickDateFilter.THIS_MONTH -> {
                val cal = Calendar.getInstance()
                cal.timeInMillis = now
                cal.set(Calendar.DAY_OF_MONTH, 1)
                val startOfMonth = IndianCurrencyUtils.getStartOfDay(cal.timeInMillis)
                txDate in startOfMonth..endOfToday
            }
        }
    }
}

/**
 * A search bar placed at the top of transaction logs that enables administrators
 * and users to quickly filter transaction entries by shop name or date.
 */
@Composable
fun TransactionSearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    selectedDateFilter: QuickDateFilter = QuickDateFilter.ALL,
    onDateFilterChange: ((QuickDateFilter) -> Unit)? = null,
    totalCount: Int = 0,
    filteredCount: Int = 0,
    placeholderText: String = "Search by shop name or date (e.g. 18/09/2026)...",
    testTag: String = "transaction_search_bar"
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag(testTag)
    ) {
        // Search Input Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("${testTag}_input"),
            placeholder = {
                Text(
                    text = placeholderText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search Transactions",
                    tint = if (searchQuery.isNotBlank()) PrimaryIndigo else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                AnimatedVisibility(
                    visible = searchQuery.isNotBlank(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    IconButton(
                        onClick = { onSearchQueryChange("") },
                        modifier = Modifier.testTag("${testTag}_clear_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryIndigo,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Quick Date Filter Chips & Match Count
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (onDateFilterChange != null) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )

                    QuickDateFilter.values().forEach { filter ->
                        val isSelected = filter == selectedDateFilter
                        FilterChip(
                            selected = isSelected,
                            onClick = { onDateFilterChange(filter) },
                            label = {
                                Text(
                                    text = filter.label,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryIndigoDark.copy(alpha = 0.15f),
                                selectedLabelColor = PrimaryIndigo
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                selectedBorderColor = PrimaryIndigo,
                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                borderWidth = 1.dp,
                                selectedBorderWidth = 1.5.dp
                            ),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("chip_filter_${filter.name.lowercase()}")
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Results Counter Badge
            if (totalCount > 0) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (searchQuery.isNotBlank() || selectedDateFilter != QuickDateFilter.ALL) {
                        PrimaryIndigo.copy(alpha = 0.12f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    }
                ) {
                    Text(
                        text = if (searchQuery.isNotBlank() || selectedDateFilter != QuickDateFilter.ALL) {
                            "$filteredCount of $totalCount"
                        } else {
                            "$totalCount entries"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (searchQuery.isNotBlank() || selectedDateFilter != QuickDateFilter.ALL) {
                            PrimaryIndigo
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
