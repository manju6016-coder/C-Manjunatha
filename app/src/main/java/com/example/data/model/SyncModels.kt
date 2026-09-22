package com.example.data.model

/**
 * Status of individual SQLite records in relation to remote server synchronization.
 */
enum class RecordSyncStatus(val displayName: String) {
    PENDING("Pending Sync"),
    SYNCING("Syncing..."),
    SYNCED("Synced"),
    FAILED("Sync Failed")
}

/**
 * High-level connection attempt status.
 */
enum class SyncConnectionStatus(val displayName: String) {
    SUCCESS("Success"),
    PARTIAL("Partial Success"),
    FAILED("Connection / Upload Failed"),
    OFFLINE("Offline (No Network)")
}

/**
 * Diagnostic breakdown for an individual record that failed remote synchronization.
 */
data class FailedRecordDetail(
    val recordId: Long,
    val shopOutletCode: String,
    val slNo: Long,
    val transactionDate: Long,
    val formattedDate: String,
    val recordType: String = "Outlet Transaction",
    val amount: Long = 0L,
    val errorCode: String,
    val failureReason: String,
    val suggestedFix: String,
    val rawPayloadExcerpt: String? = null
)

/**
 * Detailed log record of an individual remote server synchronization attempt.
 * Contains network telemetry, HTTP status code, request duration, and failed record details.
 */
data class SyncConnectionLog(
    val id: String,
    val timestamp: Long,
    val batchId: String,
    val serverUrl: String,
    val status: SyncConnectionStatus,
    val httpStatusCode: Int? = null,
    val httpStatusMessage: String? = null,
    val durationMs: Long = 0L,
    val networkType: String = "Unknown",
    val totalAttemptedRecords: Int = 0,
    val successCount: Int = 0,
    val failureCount: Int = 0,
    val summaryMessage: String = "",
    val diagnosticDetails: String? = null,
    val failedRecords: List<FailedRecordDetail> = emptyList()
) {
    val hasFailedRecords: Boolean get() = failedRecords.isNotEmpty() || failureCount > 0
}

/**
 * Snapshot of current SQLite sync state and remote connectivity.
 */
data class DatabaseSyncState(
    val totalRecords: Int = 0,
    val pendingRecords: Int = 0,
    val syncedRecords: Int = 0,
    val isOnline: Boolean = true,
    val isSyncing: Boolean = false,
    val lastSyncedAt: Long? = null,
    val lastSyncMessage: String? = null,
    val lastSyncStatus: SyncConnectionStatus? = null,
    val failedRecordCount: Int = 0
) {
    val isFullySynced: Boolean get() = pendingRecords == 0 && totalRecords > 0
    val hasPendingChanges: Boolean get() = pendingRecords > 0
    val hasErrors: Boolean get() = lastSyncStatus == SyncConnectionStatus.FAILED || failedRecordCount > 0
}

/**
 * Result returned from a manual or automatic remote push sync operation.
 */
sealed class SyncResult {
    data class Success(
        val pushedCount: Int,
        val message: String,
        val timestamp: Long,
        val logId: String? = null
    ) : SyncResult()
    data class Partial(
        val pushedCount: Int,
        val failedCount: Int,
        val message: String,
        val timestamp: Long,
        val logId: String? = null,
        val failedRecords: List<FailedRecordDetail> = emptyList()
    ) : SyncResult()
    data class NoData(val message: String) : SyncResult()
    data class Offline(val message: String) : SyncResult()
    data class Error(
        val message: String,
        val cause: Throwable? = null,
        val logId: String? = null,
        val failedRecords: List<FailedRecordDetail> = emptyList()
    ) : SyncResult()
}
