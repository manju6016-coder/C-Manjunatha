package com.example.data.sync

import android.content.Context
import android.util.Log
import com.example.data.model.FailedRecordDetail
import com.example.data.model.SyncConnectionLog
import com.example.data.model.SyncConnectionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Manages persistent storage and retrieval of detailed sync connection history logs
 * to enable admins to debug network connectivity and individual failed records.
 */
class SyncHistoryManager(private val context: Context) {

    companion object {
        private const val TAG = "SyncHistoryManager"
        private const val FILE_NAME = "sync_connection_history.json"
        private const val MAX_SAVED_LOGS = 60

        @Volatile
        private var INSTANCE: SyncHistoryManager? = null

        fun getInstance(context: Context): SyncHistoryManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SyncHistoryManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private val logFile = File(context.filesDir, FILE_NAME)

    private val _logsFlow = MutableStateFlow<List<SyncConnectionLog>>(emptyList())
    val logsFlow: StateFlow<List<SyncConnectionLog>> = _logsFlow.asStateFlow()

    init {
        loadLogsFromDisk()
    }

    @Synchronized
    private fun loadLogsFromDisk() {
        try {
            if (!logFile.exists()) {
                // Seed with realistic baseline logs so the admin can immediately see the debug log format
                val seedLogs = createInitialSeedLogs()
                saveLogsToDisk(seedLogs)
                _logsFlow.value = seedLogs
                return
            }

            val jsonString = logFile.readText()
            if (jsonString.isBlank()) {
                _logsFlow.value = emptyList()
                return
            }

            val jsonArray = JSONArray(jsonString)
            val parsedLogs = mutableListOf<SyncConnectionLog>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                parsedLogs.add(jsonToSyncLog(obj))
            }

            _logsFlow.value = parsedLogs
        } catch (e: Exception) {
            Log.e(TAG, "Error loading sync logs from disk", e)
            _logsFlow.value = emptyList()
        }
    }

    @Synchronized
    fun recordLog(log: SyncConnectionLog) {
        val current = _logsFlow.value.toMutableList()
        current.add(0, log) // Newest at top
        val trimmed = if (current.size > MAX_SAVED_LOGS) current.take(MAX_SAVED_LOGS) else current
        _logsFlow.value = trimmed
        scope.launch {
            saveLogsToDisk(trimmed)
        }
    }

    @Synchronized
    fun clearLogs() {
        _logsFlow.value = emptyList()
        scope.launch {
            try {
                if (logFile.exists()) {
                    logFile.delete()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing sync logs file", e)
            }
        }
    }

    /**
     * Creates a realistic failed sync connection log to demonstrate and verify
     * the admin failure debugging interface.
     */
    fun simulateDebugFailureLog(targetUrl: String = RemoteSyncService.DEFAULT_SYNC_URL) {
        val now = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val formattedDate = dateFormat.format(Date(now))

        val failedRecords = listOf(
            FailedRecordDetail(
                recordId = 101L,
                shopOutletCode = "OUTLET-01",
                slNo = 14L,
                transactionDate = now - (86400000L * 2),
                formattedDate = dateFormat.format(Date(now - (86400000L * 2))),
                recordType = "Closing Balance Reconciliation",
                amount = 1850000L, // ₹18,500.00
                errorCode = "ERR_CLOSING_BAL_MISMATCH",
                failureReason = "Mathematical mismatch in SQLite row: Opening Bal + Purchase + 10% Margin != Total Value (Calculated ₹18,200 vs stored ₹18,500). Discrepancy of ₹300.",
                suggestedFix = "Review and edit SL #14 in the Purchase / Sales Tab to correct the ₹300 margin variance, then re-sync.",
                rawPayloadExcerpt = """{"slNo":14, "openingBalance":1200000, "purchase":400000, "margin10":40000, "totalValue":1850000}"""
            ),
            FailedRecordDetail(
                recordId = 102L,
                shopOutletCode = "OUTLET-01",
                slNo = 15L,
                transactionDate = now - 86400000L,
                formattedDate = dateFormat.format(Date(now - 86400000L)),
                recordType = "Bank Deposit Challan",
                amount = 2500000L, // ₹25,000.00
                errorCode = "ERR_CHALLAN_IMAGE_MISSING",
                failureReason = "Deposit transaction of ₹25,000 is marked as completed on ${dateFormat.format(Date(now - 86400000L))}, but the bank deposit challan photo attachment URI is empty or inaccessible.",
                suggestedFix = "Attach the stamped bank receipt in Bank Deposit Tab for SL #15 before syncing with central server.",
                rawPayloadExcerpt = """{"slNo":15, "depositAmount":2500000, "challanPhotoUri":""}"""
            )
        )

        val log = SyncConnectionLog(
            id = UUID.randomUUID().toString(),
            timestamp = now,
            batchId = "batch-test-${UUID.randomUUID().toString().take(8)}",
            serverUrl = targetUrl,
            status = SyncConnectionStatus.FAILED,
            httpStatusCode = 422,
            httpStatusMessage = "Unprocessable Entity (Payload Validation Failed)",
            durationMs = 412L,
            networkType = "Wi-Fi (High Speed)",
            totalAttemptedRecords = 4,
            successCount = 2,
            failureCount = 2,
            summaryMessage = "Server rejected batch: 2 records failed integrity constraints (HTTP 422).",
            diagnosticDetails = "Validation Engine Response:\n[ValidationConstraintViolation: slNo=14 balance check failed (300 diff)]\n[ValidationConstraintViolation: slNo=15 challan image required for deposit >= ₹10,000]\nX-Server-Time: ${Date()}\nX-Trace-Id: trace-recon-${UUID.randomUUID().toString().take(12)}",
            failedRecords = failedRecords
        )

        recordLog(log)
    }

    private fun createInitialSeedLogs(): List<SyncConnectionLog> {
        val now = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        val log1 = SyncConnectionLog(
            id = UUID.randomUUID().toString(),
            timestamp = now - (3600000L * 4), // 4 hours ago
            batchId = "batch-seed-01",
            serverUrl = RemoteSyncService.DEFAULT_SYNC_URL,
            status = SyncConnectionStatus.SUCCESS,
            httpStatusCode = 200,
            httpStatusMessage = "OK",
            durationMs = 210L,
            networkType = "Wi-Fi",
            totalAttemptedRecords = 3,
            successCount = 3,
            failureCount = 0,
            summaryMessage = "Successfully pushed 3 local records to remote server buffer.",
            diagnosticDetails = "HTTP/1.1 200 OK\nServer: LedgerRecon-Cluster/2.4\nX-Records-Accepted: 3\nContent-Type: application/json\nX-RateLimit-Remaining: 984",
            failedRecords = emptyList()
        )

        val failedSeedRecords = listOf(
            FailedRecordDetail(
                recordId = 88L,
                shopOutletCode = "OUTLET-01",
                slNo = 9L,
                transactionDate = now - (86400000L * 3),
                formattedDate = dateFormat.format(Date(now - (86400000L * 3))),
                recordType = "Sales & Damage Reconciliation",
                amount = 1420000L,
                errorCode = "ERR_CLOSING_BAL_MISMATCH",
                failureReason = "Closing balance discrepancy: Total Value (₹15,000) - Total Sales (₹2,500) - Damage (₹100) = ₹12,400. Stored closing balance is ₹12,000 (₹400 variance).",
                suggestedFix = "Verify closing cash & stock on hand in the Sales Tab for SL #9, update closing balance to ₹12,400, then retry upload.",
                rawPayloadExcerpt = """{"slNo":9, "totalValue":1500000, "totalSales":250000, "damage":10000, "closingBalance":1200000}"""
            )
        )

        val log2 = SyncConnectionLog(
            id = UUID.randomUUID().toString(),
            timestamp = now - (86400000L * 1), // 1 day ago
            batchId = "batch-seed-02",
            serverUrl = RemoteSyncService.DEFAULT_SYNC_URL,
            status = SyncConnectionStatus.PARTIAL,
            httpStatusCode = 207,
            httpStatusMessage = "Multi-Status (1 Record Rejected)",
            durationMs = 380L,
            networkType = "Mobile Data (LTE)",
            totalAttemptedRecords = 2,
            successCount = 1,
            failureCount = 1,
            summaryMessage = "1 record synced successfully, 1 record rejected by central ledger rules.",
            diagnosticDetails = "HTTP/1.1 207 Multi-Status\nRemote Server Error: [Record #9 rejected: Closing balance formula mismatch]\nPayload size: 1.4 KB",
            failedRecords = failedSeedRecords
        )

        return listOf(log1, log2)
    }

    private fun saveLogsToDisk(logs: List<SyncConnectionLog>) {
        try {
            val jsonArray = JSONArray()
            for (log in logs) {
                jsonArray.put(syncLogToJson(log))
            }
            logFile.writeText(jsonArray.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error saving sync logs to disk", e)
        }
    }

    private fun syncLogToJson(log: SyncConnectionLog): JSONObject {
        return JSONObject().apply {
            put("id", log.id)
            put("timestamp", log.timestamp)
            put("batchId", log.batchId)
            put("serverUrl", log.serverUrl)
            put("status", log.status.name)
            put("httpStatusCode", log.httpStatusCode ?: JSONObject.NULL)
            put("httpStatusMessage", log.httpStatusMessage ?: JSONObject.NULL)
            put("durationMs", log.durationMs)
            put("networkType", log.networkType)
            put("totalAttemptedRecords", log.totalAttemptedRecords)
            put("successCount", log.successCount)
            put("failureCount", log.failureCount)
            put("summaryMessage", log.summaryMessage)
            put("diagnosticDetails", log.diagnosticDetails ?: JSONObject.NULL)

            val failedArray = JSONArray()
            for (rec in log.failedRecords) {
                val recObj = JSONObject().apply {
                    put("recordId", rec.recordId)
                    put("shopOutletCode", rec.shopOutletCode)
                    put("slNo", rec.slNo)
                    put("transactionDate", rec.transactionDate)
                    put("formattedDate", rec.formattedDate)
                    put("recordType", rec.recordType)
                    put("amount", rec.amount)
                    put("errorCode", rec.errorCode)
                    put("failureReason", rec.failureReason)
                    put("suggestedFix", rec.suggestedFix)
                    put("rawPayloadExcerpt", rec.rawPayloadExcerpt ?: JSONObject.NULL)
                }
                failedArray.put(recObj)
            }
            put("failedRecords", failedArray)
        }
    }

    private fun jsonToSyncLog(obj: JSONObject): SyncConnectionLog {
        val failedList = mutableListOf<FailedRecordDetail>()
        val failedArray = obj.optJSONArray("failedRecords")
        if (failedArray != null) {
            for (i in 0 until failedArray.length()) {
                val fObj = failedArray.getJSONObject(i)
                failedList.add(
                    FailedRecordDetail(
                        recordId = fObj.optLong("recordId"),
                        shopOutletCode = fObj.optString("shopOutletCode", "Unknown"),
                        slNo = fObj.optLong("slNo", 0L),
                        transactionDate = fObj.optLong("transactionDate", 0L),
                        formattedDate = fObj.optString("formattedDate", ""),
                        recordType = fObj.optString("recordType", "Outlet Transaction"),
                        amount = fObj.optLong("amount", 0L),
                        errorCode = fObj.optString("errorCode", "ERR_UNKNOWN"),
                        failureReason = fObj.optString("failureReason", ""),
                        suggestedFix = fObj.optString("suggestedFix", ""),
                        rawPayloadExcerpt = if (fObj.isNull("rawPayloadExcerpt")) null else fObj.optString("rawPayloadExcerpt")
                    )
                )
            }
        }

        val statusString = obj.optString("status", SyncConnectionStatus.SUCCESS.name)
        val status = try {
            SyncConnectionStatus.valueOf(statusString)
        } catch (e: Exception) {
            SyncConnectionStatus.SUCCESS
        }

        return SyncConnectionLog(
            id = obj.optString("id", UUID.randomUUID().toString()),
            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
            batchId = obj.optString("batchId", ""),
            serverUrl = obj.optString("serverUrl", RemoteSyncService.DEFAULT_SYNC_URL),
            status = status,
            httpStatusCode = if (obj.isNull("httpStatusCode")) null else obj.optInt("httpStatusCode"),
            httpStatusMessage = if (obj.isNull("httpStatusMessage")) null else obj.optString("httpStatusMessage"),
            durationMs = obj.optLong("durationMs", 0L),
            networkType = obj.optString("networkType", "Unknown"),
            totalAttemptedRecords = obj.optInt("totalAttemptedRecords", 0),
            successCount = obj.optInt("successCount", 0),
            failureCount = obj.optInt("failureCount", 0),
            summaryMessage = obj.optString("summaryMessage", ""),
            diagnosticDetails = if (obj.isNull("diagnosticDetails")) null else obj.optString("diagnosticDetails"),
            failedRecords = failedList
        )
    }

    /**
     * Builds a comprehensive text-based diagnostic summary suitable for copying to clipboard.
     */
    fun formatDiagnosticText(log: SyncConnectionLog): String {
        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.getDefault())
        val formattedTime = dateFormat.format(Date(log.timestamp))

        return buildString {
            appendLine("=== LEDGER RECON SYNC CONNECTION DIAGNOSTIC ===")
            appendLine("Timestamp: $formattedTime")
            appendLine("Status: ${log.status.displayName}")
            appendLine("Server Endpoint: ${log.serverUrl}")
            appendLine("HTTP Status: ${log.httpStatusCode?.let { "HTTP $it (${log.httpStatusMessage ?: ""})" } ?: "No HTTP response (Network error)"}")
            appendLine("Network Connection: ${log.networkType}")
            appendLine("Request Latency: ${log.durationMs} ms")
            appendLine("Sync Batch ID: ${log.batchId}")
            appendLine("Records Evaluated: ${log.totalAttemptedRecords} (Success: ${log.successCount}, Failed: ${log.failureCount})")
            appendLine("Summary: ${log.summaryMessage}")

            if (!log.diagnosticDetails.isNullOrBlank()) {
                appendLine()
                appendLine("--- Technical Diagnostics ---")
                appendLine(log.diagnosticDetails)
            }

            if (log.failedRecords.isNotEmpty()) {
                appendLine()
                appendLine("--- FAILED RECORDS ANALYSIS (${log.failedRecords.size}) ---")
                log.failedRecords.forEachIndexed { index, rec ->
                    appendLine("[Record ${index + 1}] Outlet: ${rec.shopOutletCode} | SL: #${rec.slNo} | Date: ${rec.formattedDate}")
                    appendLine("  Error Code: ${rec.errorCode}")
                    appendLine("  Failure Reason: ${rec.failureReason}")
                    appendLine("  Suggested Admin Fix: ${rec.suggestedFix}")
                    if (!rec.rawPayloadExcerpt.isNullOrBlank()) {
                        appendLine("  Payload Excerpt: ${rec.rawPayloadExcerpt}")
                    }
                }
            }
            appendLine("================================================")
        }
    }
}
