package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.DatabaseSyncState
import com.example.data.model.FailedRecordDetail
import com.example.data.model.SyncConnectionLog
import com.example.data.model.SyncConnectionStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Compact TopAppBar sync indicator button.
 * Visually communicates SQLite storage sync status (Synced, Pending, Syncing, Offline, or Error)
 * and opens the detailed sync management and connection debug history dialog when tapped.
 */
@Composable
fun SyncTopBarIndicator(
    syncState: DatabaseSyncState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sync_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val (icon, tint, labelText) = when {
        syncState.isSyncing -> Triple(
            Icons.Default.Sync,
            Color(0xFF1976D2), // Blue
            "Syncing"
        )
        syncState.hasErrors -> Triple(
            Icons.Default.WarningAmber,
            Color(0xFFD32F2F), // Red
            if (syncState.failedRecordCount > 0) "${syncState.failedRecordCount} Failed" else "Sync Error"
        )
        !syncState.isOnline -> Triple(
            Icons.Default.CloudOff,
            Color(0xFF757575), // Neutral Gray
            "Offline"
        )
        syncState.hasPendingChanges -> Triple(
            Icons.Default.CloudUpload,
            Color(0xFFE65100), // Amber / Deep Orange
            "${syncState.pendingRecords} Pending"
        )
        else -> Triple(
            Icons.Default.CloudDone,
            Color(0xFF2E7D32), // Green
            "Synced"
        )
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("sync_top_bar_indicator"),
        color = tint.copy(alpha = 0.12f),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, tint.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (syncState.isSyncing) {
                    Icon(
                        imageVector = icon,
                        contentDescription = "Syncing with remote server",
                        tint = tint,
                        modifier = Modifier
                            .size(18.dp)
                            .rotate(rotation)
                    )
                } else {
                    BadgedBox(
                        badge = {
                            if (syncState.hasErrors) {
                                Badge(
                                    containerColor = Color(0xFFD32F2F),
                                    contentColor = Color.White
                                ) {
                                    Text(
                                        text = "${if (syncState.failedRecordCount > 0) syncState.failedRecordCount else '!'}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else if (syncState.hasPendingChanges) {
                                Badge(
                                    containerColor = Color(0xFFE65100),
                                    contentColor = Color.White
                                ) {
                                    Text(
                                        text = "${syncState.pendingRecords}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = "SQLite sync status: $labelText",
                            tint = tint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Text(
                text = labelText,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = tint
                )
            )
        }
    }
}

/**
 * Detailed SQLite Sync Management and Connection History Log Dialog.
 * Includes:
 * 1. Database & Sync Overview: Local SQLite storage metrics, active connection status, and manual sync action.
 * 2. Connection History Log: Comprehensive telemetry for each connection attempt, HTTP status code,
 *    round-trip latency, and deep diagnostic breakdown of why specific records may have failed to upload.
 */
@Composable
fun SyncStatusDialog(
    syncState: DatabaseSyncState,
    connectionLogs: List<SyncConnectionLog> = emptyList(),
    onDismiss: () -> Unit,
    onSyncNow: () -> Unit,
    onTestConnection: () -> Unit = {},
    onSimulateFailure: () -> Unit = {},
    onClearLogs: () -> Unit = {},
    initialTab: Int = 0,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    val lastSyncFormatted = syncState.lastSyncedAt?.let {
        dateFormat.format(Date(it))
    } ?: "Never synced yet"

    val failedLogsCount = connectionLogs.count { it.status == SyncConnectionStatus.FAILED || it.hasFailedRecords }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = modifier
            .fillMaxWidth(0.95f)
            .fillMaxHeight(0.88f)
            .testTag("sync_status_dialog"),
        title = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(
                                    color = if (syncState.hasErrors) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.primaryContainer,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (syncState.hasErrors) Icons.Default.WarningAmber else Icons.Default.Sync,
                                contentDescription = null,
                                tint = if (syncState.hasErrors) Color(0xFFC62828) else MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "SQLite Sync & Remote Diagnostics",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Local Storage & Cloud Connection Logs",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Top Navigation Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Overview & Storage", fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("Connection Logs", fontWeight = FontWeight.SemiBold)
                                if (failedLogsCount > 0) {
                                    Surface(
                                        color = Color(0xFFD32F2F),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = "$failedLogsCount Failed",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                } else if (connectionLogs.isNotEmpty()) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = "${connectionLogs.size}",
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        },
                        icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }
            }
        },
        text = {
            if (selectedTab == 0) {
                // TAB 0: OVERVIEW & STORAGE METRICS
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Network Status Indicator Card
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (syncState.isOnline) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (syncState.isOnline) Icons.Default.Wifi else Icons.Default.WifiOff,
                                contentDescription = if (syncState.isOnline) "Internet Active" else "Internet Disconnected",
                                tint = if (syncState.isOnline) Color(0xFF2E7D32) else Color(0xFFC62828),
                                modifier = Modifier.size(24.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (syncState.isOnline) "Active Internet Connection" else "Offline (No Internet)",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (syncState.isOnline) Color(0xFF1B5E20) else Color(0xFFB71C1C)
                                    )
                                )
                                Text(
                                    text = if (syncState.isOnline) {
                                        "Ready to push local SQLite transactions to remote server."
                                    } else {
                                        "Local records are safely preserved in SQLite on this device. Connect to network to sync."
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (syncState.isOnline) Color(0xFF2E7D32) else Color(0xFFC62828)
                                    )
                                )
                            }
                        }
                    }

                    // SQLite Storage Breakdown
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storage,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "SQLite Local Database Status",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            MetricRow(
                                label = "Total Records Stored:",
                                value = "${syncState.totalRecords} records",
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            MetricRow(
                                label = "Uploaded & Synced:",
                                value = "${syncState.syncedRecords} records",
                                color = Color(0xFF2E7D32)
                            )
                            MetricRow(
                                label = "Pending Upload:",
                                value = "${syncState.pendingRecords} records",
                                color = if (syncState.pendingRecords > 0) Color(0xFFE65100) else Color(0xFF2E7D32),
                                isHighlighted = syncState.pendingRecords > 0
                            )
                            if (syncState.failedRecordCount > 0) {
                                MetricRow(
                                    label = "Records Flagged With Issues:",
                                    value = "${syncState.failedRecordCount} records",
                                    color = Color(0xFFD32F2F),
                                    isHighlighted = true
                                )
                            }
                        }
                    }

                    // Last Sync & Server Details
                    Column(
                        modifier = Modifier.padding(horizontal = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Last Synced:",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Text(
                                text = lastSyncFormatted,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }

                        syncState.lastSyncMessage?.let { msg ->
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (msg.contains("failed", ignoreCase = true) || msg.contains("Cannot", ignoreCase = true)) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    },
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    // Quick Jump to Connection History Card
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedTab = 1 },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column {
                                    Text(
                                        text = "Connection History & Debug Logs",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "${connectionLogs.size} logs available • ${failedLogsCount} failure events",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = if (failedLogsCount > 0) Color(0xFFD32F2F) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                            Text(
                                text = "View Logs →",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            } else {
                // TAB 1: CONNECTION HISTORY LOG & RECORD FAILURE DEBUGGING
                ConnectionLogsDebugView(
                    logs = connectionLogs,
                    onTestConnection = onTestConnection,
                    onSimulateFailure = onSimulateFailure,
                    onClearLogs = onClearLogs,
                    onCopyDiagnostic = { diagText ->
                        clipboardManager.setText(AnnotatedString(diagText))
                        Toast.makeText(context, "Diagnostic details copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSyncNow,
                enabled = !syncState.isSyncing,
                modifier = Modifier.testTag("sync_now_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (syncState.isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Syncing...")
                } else {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (!syncState.isOnline) "Sync Now (Offline)" else "Sync Now"
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_sync_dialog_button")
            ) {
                Text("Close")
            }
        }
    )
}

/**
 * Detailed connection history log and failure debugger view.
 * Enables admins to filter logs, view HTTP response codes, latency,
 * and inspect specific records that failed along with root cause analysis and recommended fixes.
 */
@Composable
private fun ConnectionLogsDebugView(
    logs: List<SyncConnectionLog>,
    onTestConnection: () -> Unit,
    onSimulateFailure: () -> Unit,
    onClearLogs: () -> Unit,
    onCopyDiagnostic: (String) -> Unit
) {
    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredLogs = remember(logs, selectedFilter) {
        when (selectedFilter) {
            "FAILED" -> logs.filter { it.status == SyncConnectionStatus.FAILED || it.hasFailedRecords }
            "SUCCESS" -> logs.filter { it.status == SyncConnectionStatus.SUCCESS && !it.hasFailedRecords }
            "OFFLINE" -> logs.filter { it.status == SyncConnectionStatus.OFFLINE }
            else -> logs
        }
    }

    val failedCount = logs.count { it.status == SyncConnectionStatus.FAILED || it.hasFailedRecords }
    val successCount = logs.count { it.status == SyncConnectionStatus.SUCCESS && !it.hasFailedRecords }
    val offlineCount = logs.count { it.status == SyncConnectionStatus.OFFLINE }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Admin Tool Actions Strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = onTestConnection,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("btn_test_connection")
                ) {
                    Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Probe Server", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onSimulateFailure,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("btn_simulate_debug_failure")
                ) {
                    Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFD32F2F))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Simulate Error", fontSize = 12.sp, color = Color(0xFFD32F2F))
                }
            }

            if (logs.isNotEmpty()) {
                TextButton(
                    onClick = onClearLogs,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("btn_clear_sync_logs")
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear", fontSize = 12.sp)
                }
            }
        }

        // Filter Chips Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedFilter == "ALL",
                onClick = { selectedFilter = "ALL" },
                label = { Text("All (${logs.size})", fontSize = 11.sp) }
            )
            FilterChip(
                selected = selectedFilter == "FAILED",
                onClick = { selectedFilter = "FAILED" },
                label = { Text("Failed ($failedCount)", fontSize = 11.sp, color = if (failedCount > 0) Color(0xFFD32F2F) else Color.Unspecified) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFFFEBEE),
                    selectedLabelColor = Color(0xFFC62828)
                )
            )
            FilterChip(
                selected = selectedFilter == "SUCCESS",
                onClick = { selectedFilter = "SUCCESS" },
                label = { Text("Success ($successCount)", fontSize = 11.sp) }
            )
            FilterChip(
                selected = selectedFilter == "OFFLINE",
                onClick = { selectedFilter = "OFFLINE" },
                label = { Text("Offline ($offlineCount)", fontSize = 11.sp) }
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // History Log List
        if (filteredLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = if (logs.isEmpty()) "No connection logs yet." else "No logs match the selected filter.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Tap 'Sync Now' or 'Probe Server' to record telemetry.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredLogs, key = { it.id }) { log ->
                    ConnectionLogCard(
                        log = log,
                        onCopyDiagnostic = onCopyDiagnostic
                    )
                }
            }
        }
    }
}

/**
 * Expandable Card displaying individual connection attempt telemetry
 * and full failure diagnostic breakdown for specific records.
 */
@Composable
private fun ConnectionLogCard(
    log: SyncConnectionLog,
    onCopyDiagnostic: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var isExpanded by remember { mutableStateOf(log.hasFailedRecords) }
    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.getDefault())
    val formattedTime = dateFormat.format(Date(log.timestamp))

    val (statusBg, statusBorder, statusText, statusIcon) = when (log.status) {
        SyncConnectionStatus.SUCCESS -> if (log.hasFailedRecords) {
            listOf(Color(0xFFFFF8E1), Color(0xFFFFB300), "PARTIAL SUCCESS", Icons.Default.WarningAmber)
        } else {
            listOf(Color(0xFFE8F5E9), Color(0xFF4CAF50), "SUCCESS", Icons.Default.CheckCircle)
        }
        SyncConnectionStatus.PARTIAL -> listOf(Color(0xFFFFF3E0), Color(0xFFFF9800), "PARTIAL", Icons.Default.WarningAmber)
        SyncConnectionStatus.FAILED -> listOf(Color(0xFFFFEBEE), Color(0xFFE53935), "FAILED", Icons.Default.ErrorOutline)
        SyncConnectionStatus.OFFLINE -> listOf(Color(0xFFF5F5F5), Color(0xFF9E9E9E), "OFFLINE", Icons.Default.WifiOff)
    }

    val containerColor = statusBg as Color
    val borderColor = statusBorder as Color
    val label = statusText as String
    val icon = statusIcon as androidx.compose.ui.graphics.vector.ImageVector

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("connection_log_card_${log.id.take(8)}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header: Status Pill, Time, Latency
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = borderColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = borderColor
                    )
                    log.httpStatusCode?.let { code ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = borderColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "HTTP $code",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = borderColor,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (log.durationMs > 0) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.Black.copy(alpha = 0.06f)
                        ) {
                            Text(
                                text = "${log.durationMs} ms",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }

                    Text(
                        text = formattedTime,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Summary Message
            Text(
                text = log.summaryMessage,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )

            // Telemetry snippet (Target URL, Network, Evaluated records)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${log.networkType} • ${log.totalAttemptedRecords} records attempted (${log.successCount} synced, ${log.failureCount} failed)",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { isExpanded = !isExpanded }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isExpanded) "Hide Details" else "Inspect (${log.failedRecords.size} failed)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = borderColor
                        )
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = borderColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Expanded Section: Connection Diagnostics & Failed Records Analysis
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(color = borderColor.copy(alpha = 0.3f))

                    // Connection Endpoint & Technical Trace Box
                    Surface(
                        color = Color.Black.copy(alpha = 0.04f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Connection Diagnostic Trace",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                TextButton(
                                    onClick = {
                                        val diag = buildString {
                                            appendLine("Target URL: ${log.serverUrl}")
                                            appendLine("Batch ID: ${log.batchId}")
                                            appendLine("Status: ${log.status.name} (HTTP ${log.httpStatusCode ?: "N/A"})")
                                            appendLine("Network: ${log.networkType} | Latency: ${log.durationMs} ms")
                                            appendLine("Records: ${log.totalAttemptedRecords} attempted, ${log.failureCount} failed")
                                            log.diagnosticDetails?.let { appendLine("Details:\n$it") }
                                        }
                                        onCopyDiagnostic(diag)
                                    },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy Trace", fontSize = 10.sp)
                                }
                            }

                            Text(
                                text = "Target: ${log.serverUrl}",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Batch ID: ${log.batchId}",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            log.diagnosticDetails?.let { details ->
                                Text(
                                    text = details,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // FAILED RECORDS BREAKDOWN
                    if (log.failedRecords.isNotEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WarningAmber,
                                    contentDescription = null,
                                    tint = Color(0xFFD32F2F),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Failed Record(s) Analysis (${log.failedRecords.size})",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFC62828)
                                    )
                                )
                            }

                            Text(
                                text = "The following individual records failed server validation or upload integrity rules. Inspect the root cause and recommended admin fix:",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )

                            // Failed Record Cards
                            log.failedRecords.forEachIndexed { index, record ->
                                FailedRecordItemCard(
                                    index = index + 1,
                                    record = record,
                                    onCopyRecordJson = { jsonStr ->
                                        clipboardManager.setText(AnnotatedString(jsonStr))
                                        Toast.makeText(context, "Record JSON copied to clipboard", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Detailed analysis card for an individual failed record.
 * Outlines the specific outlet, SL number, transaction date, error code,
 * explicit failure reason, and actionable fix for the admin.
 */
@Composable
private fun FailedRecordItemCard(
    index: Int,
    record: FailedRecordDetail,
    onCopyRecordJson: (String) -> Unit
) {
    var showRawJson by remember { mutableStateOf(false) }

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("failed_record_card_${record.recordId}"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE57373))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Header with Outlet Code, SL #, and Error Code Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = Color(0xFFFFEBEE),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "#$index",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC62828),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }

                    Text(
                        text = "Outlet: ${record.shopOutletCode} • SL #${record.slNo}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Surface(
                    color = Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = record.errorCode,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB71C1C),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Date & Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Date: ${record.formattedDate}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                if (record.amount > 0) {
                    Text(
                        text = "Amount: ₹${record.amount / 100.0}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFFFCDD2))

            // Root Cause / Failure Reason
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Root Cause (Why Record Failed):",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB71C1C)
                    )
                )
                Text(
                    text = record.failureReason,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF212121),
                        fontSize = 12.sp
                    )
                )
            }

            // Actionable Suggested Fix
            Surface(
                color = Color(0xFFE8F5E9),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(16.dp)
                    )
                    Column {
                        Text(
                            text = "Recommended Admin Action:",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B5E20)
                            )
                        )
                        Text(
                            text = record.suggestedFix,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF2E7D32),
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }

            // Rejected Payload Action
            record.rawPayloadExcerpt?.let { payload ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { showRawJson = !showRawJson },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (showRawJson) "Hide Payload JSON" else "View Payload JSON",
                            fontSize = 10.sp
                        )
                    }

                    TextButton(
                        onClick = { onCopyRecordJson(payload) },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy JSON", fontSize = 10.sp)
                    }
                }

                if (showRawJson) {
                    Surface(
                        color = Color(0xFFF5F5F5),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = payload,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(8.dp),
                            color = Color(0xFF37474F)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Inline Banner shown when there are pending SQLite changes to sync,
 * active syncing in progress, offline mode, or sync failure events.
 * Tapping opens the sync status and connection history debug dialog.
 */
@Composable
fun SyncStatusBarBanner(
    syncState: DatabaseSyncState,
    onSyncNow: () -> Unit,
    onOpenSyncDialog: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (syncState.pendingRecords == 0 && syncState.isOnline && !syncState.isSyncing && !syncState.hasErrors) {
        return
    }

    val (bg, contentColor, borderStroke) = when {
        syncState.isSyncing -> Triple(
            Color(0xFFE3F2FD),
            Color(0xFF0D47A1),
            Color(0xFF90CAF9)
        )
        syncState.hasErrors -> Triple(
            Color(0xFFFFEBEE),
            Color(0xFFC62828),
            Color(0xFFEF9A9A)
        )
        !syncState.isOnline -> Triple(
            Color(0xFFFFF3E0),
            Color(0xFFE65100),
            Color(0xFFFFCC80)
        )
        else -> Triple(
            Color(0xFFFBE9E7),
            Color(0xFFBF360C),
            Color(0xFFFFAB91)
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = onOpenSyncDialog != null) { onOpenSyncDialog?.invoke() }
            .testTag("sync_status_banner"),
        colors = CardDefaults.cardColors(containerColor = bg),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderStroke)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (syncState.isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = contentColor
                    )
                } else {
                    Icon(
                        imageVector = when {
                            syncState.hasErrors -> Icons.Default.WarningAmber
                            !syncState.isOnline -> Icons.Default.WifiOff
                            else -> Icons.Default.CloudUpload
                        },
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column {
                    Text(
                        text = when {
                            syncState.isSyncing -> "Syncing SQLite records to server..."
                            syncState.hasErrors -> "Sync Alert: ${syncState.failedRecordCount} record(s) flagged with issues"
                            !syncState.isOnline -> "Offline: ${syncState.pendingRecords} record(s) stored in SQLite"
                            else -> "${syncState.pendingRecords} record(s) pending remote sync"
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = contentColor
                        )
                    )
                    Text(
                        text = when {
                            syncState.isSyncing -> "Transferring payload over HTTPS..."
                            syncState.hasErrors -> "Tap to inspect connection logs & debug failed records."
                            !syncState.isOnline -> "Data is saved locally. Push when reconnected."
                            else -> "Tap 'Sync Now' to push pending local entries."
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = contentColor.copy(alpha = 0.85f),
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onSyncNow,
                enabled = !syncState.isSyncing,
                modifier = Modifier.testTag("banner_sync_now_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = contentColor,
                    contentColor = Color.White
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 12.dp,
                    vertical = 6.dp
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (syncState.hasErrors) "Retry Sync" else "Sync Now",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
private fun MetricRow(
    label: String,
    value: String,
    color: Color,
    isHighlighted: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
                color = color
            )
        )
    }
}
