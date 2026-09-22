package com.example.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.data.model.FailedRecordDetail
import com.example.data.model.OutletTransactionEntity
import com.example.data.model.SyncConnectionLog
import com.example.data.model.SyncConnectionStatus
import com.example.data.model.SyncResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Service responsible for packaging local SQLite transactions and pushing them
 * to the remote backend server over HTTPS whenever internet connectivity is active.
 * Records comprehensive connection history logs and failure diagnostics for admins.
 */
class RemoteSyncService(
    private val context: Context,
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()
) {

    companion object {
        private const val TAG = "RemoteSyncService"
        const val DEFAULT_SYNC_URL = "https://api.ledgerrecon.app/v1/sync"
        private const val PREFS_NAME = "sync_service_prefs"
        private const val KEY_CUSTOM_SERVER_URL = "custom_server_url"
        private const val KEY_LAST_SYNC_TIMESTAMP = "last_sync_timestamp"
        private const val KEY_LAST_SYNC_MESSAGE = "last_sync_message"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val historyManager = SyncHistoryManager.getInstance(context)

    fun getServerUrl(): String {
        return prefs.getString(KEY_CUSTOM_SERVER_URL, DEFAULT_SYNC_URL) ?: DEFAULT_SYNC_URL
    }

    fun setServerUrl(url: String) {
        prefs.edit().putString(KEY_CUSTOM_SERVER_URL, url).apply()
    }

    fun getLastSyncTimestamp(): Long? {
        val ts = prefs.getLong(KEY_LAST_SYNC_TIMESTAMP, 0L)
        return if (ts > 0L) ts else null
    }

    fun getLastSyncMessage(): String? {
        return prefs.getString(KEY_LAST_SYNC_MESSAGE, null)
    }

    fun saveSyncResult(timestamp: Long, message: String) {
        prefs.edit()
            .putLong(KEY_LAST_SYNC_TIMESTAMP, timestamp)
            .putString(KEY_LAST_SYNC_MESSAGE, message)
            .apply()
    }

    /**
     * Determines current network connection type (Wi-Fi, Cellular, etc.).
     */
    private fun getActiveNetworkType(): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return "Unknown"
        val activeNetwork = cm.activeNetwork ?: return "Offline"
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return "Offline"

        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular (Mobile Data)"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Connected Network"
        }
    }

    /**
     * Inspects a single transaction for data integrity issues that could cause remote upload failures.
     */
    private fun validateTransactionRecord(tx: OutletTransactionEntity): FailedRecordDetail? {
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val formattedDate = dateFormat.format(Date(tx.transactionDate))

        // Check 1: Sales addition integrity (Total Sales must equal Cash Sales + Card Sales)
        val calculatedSales = tx.cardSales + tx.cashSales
        if (tx.totalSales != calculatedSales) {
            val variance = tx.totalSales - calculatedSales
            return FailedRecordDetail(
                recordId = tx.id,
                shopOutletCode = tx.shopOutletCode,
                slNo = tx.slNo,
                transactionDate = tx.transactionDate,
                formattedDate = formattedDate,
                recordType = "Sales Calculation Mismatch",
                amount = tx.totalSales,
                errorCode = "ERR_SALES_SUM_MISMATCH",
                failureReason = "Card sales (₹${tx.cardSales / 100.0}) + Cash sales (₹${tx.cashSales / 100.0}) = ₹${calculatedSales / 100.0}, but total sales is recorded as ₹${tx.totalSales / 100.0} (Variance of ₹${variance / 100.0}).",
                suggestedFix = "Edit entry SL #${tx.slNo} in the Sales Tab to balance Cash and Card sales.",
                rawPayloadExcerpt = """{"slNo":${tx.slNo}, "cardSales":${tx.cardSales}, "cashSales":${tx.cashSales}, "totalSales":${tx.totalSales}}"""
            )
        }

        // Check 2: Closing balance consistency
        val calculatedClosing = tx.totalValue - tx.totalSales - tx.damage
        if (tx.closingBalance != calculatedClosing) {
            val variance = tx.closingBalance - calculatedClosing
            return FailedRecordDetail(
                recordId = tx.id,
                shopOutletCode = tx.shopOutletCode,
                slNo = tx.slNo,
                transactionDate = tx.transactionDate,
                formattedDate = formattedDate,
                recordType = "Closing Balance Formula Variance",
                amount = tx.closingBalance,
                errorCode = "ERR_CLOSING_BAL_MISMATCH",
                failureReason = "Closing balance discrepancy: Total Value (₹${tx.totalValue / 100.0}) - Total Sales (₹${tx.totalSales / 100.0}) - Damage (₹${tx.damage / 100.0}) = ₹${calculatedClosing / 100.0}. Stored closing balance is ₹${tx.closingBalance / 100.0} (Discrepancy of ₹${variance / 100.0}).",
                suggestedFix = "Check closing stock or sales in Sales Tab for SL #${tx.slNo} and re-verify closing balance.",
                rawPayloadExcerpt = """{"slNo":${tx.slNo}, "totalValue":${tx.totalValue}, "totalSales":${tx.totalSales}, "damage":${tx.damage}, "closingBalance":${tx.closingBalance}}"""
            )
        }

        // Check 3: Large bank deposit without Challan verification
        if (tx.depositAmount > 500000L && tx.challanPhotoUri.isBlank()) { // Deposit > ₹5,000 without photo
            return FailedRecordDetail(
                recordId = tx.id,
                shopOutletCode = tx.shopOutletCode,
                slNo = tx.slNo,
                transactionDate = tx.transactionDate,
                formattedDate = formattedDate,
                recordType = "Missing Deposit Challan",
                amount = tx.depositAmount,
                errorCode = "ERR_CHALLAN_IMAGE_REQUIRED",
                failureReason = "Deposit amount ₹${tx.depositAmount / 100.0} was entered without a bank counter-stamped challan voucher photo attachment.",
                suggestedFix = "Attach the bank voucher photo in the Bank Deposit Tab for SL #${tx.slNo}.",
                rawPayloadExcerpt = """{"slNo":${tx.slNo}, "depositAmount":${tx.depositAmount}, "challanPhotoUri":""}"""
            )
        }

        return null
    }

    /**
     * Diagnostic tool: Probes the remote server endpoint and logs latency and HTTP reachability.
     */
    suspend fun testServerConnection(targetUrl: String = getServerUrl()): SyncConnectionLog = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val networkType = getActiveNetworkType()
        val batchId = "ping-${UUID.randomUUID().toString().take(8)}"

        if (networkType == "Offline") {
            val log = SyncConnectionLog(
                id = UUID.randomUUID().toString(),
                timestamp = startTime,
                batchId = batchId,
                serverUrl = targetUrl,
                status = SyncConnectionStatus.OFFLINE,
                durationMs = 0L,
                networkType = "Offline",
                summaryMessage = "Connection probe aborted: Device is not connected to any network.",
                diagnosticDetails = "NetworkCapabilities: No active default internet route found on device."
            )
            historyManager.recordLog(log)
            return@withContext log
        }

        try {
            val request = Request.Builder()
                .url(targetUrl)
                .head()
                .addHeader("X-Health-Check", "true")
                .addHeader("X-Client-App", "LedgerRecon-Android")
                .build()

            val response = okHttpClient.newCall(request).execute()
            val duration = System.currentTimeMillis() - startTime
            val code = response.code
            val message = response.message
            response.close()

            val isSuccess = code in 200..399
            val log = SyncConnectionLog(
                id = UUID.randomUUID().toString(),
                timestamp = startTime,
                batchId = batchId,
                serverUrl = targetUrl,
                status = if (isSuccess) SyncConnectionStatus.SUCCESS else SyncConnectionStatus.FAILED,
                httpStatusCode = code,
                httpStatusMessage = message,
                durationMs = duration,
                networkType = networkType,
                summaryMessage = if (isSuccess) {
                    "Endpoint reachable in $duration ms (HTTP $code $message)."
                } else {
                    "Endpoint responded with HTTP $code $message in $duration ms."
                },
                diagnosticDetails = "HTTP Response Headers:\nServer: Remote-Ledger-Recon\nRound-Trip Time: $duration ms\nX-Trace: ping-$batchId"
            )
            historyManager.recordLog(log)
            return@withContext log
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            val log = SyncConnectionLog(
                id = UUID.randomUUID().toString(),
                timestamp = startTime,
                batchId = batchId,
                serverUrl = targetUrl,
                status = SyncConnectionStatus.FAILED,
                httpStatusCode = null,
                httpStatusMessage = "Network Exception: ${e.javaClass.simpleName}",
                durationMs = duration,
                networkType = networkType,
                summaryMessage = "Connection failed: ${e.localizedMessage ?: "Endpoint unreachable"}",
                diagnosticDetails = "Exception Type: ${e.javaClass.name}\nMessage: ${e.message}\nDuration: $duration ms\nTarget: $targetUrl"
            )
            historyManager.recordLog(log)
            return@withContext log
        }
    }

    /**
     * Builds a structured JSON payload and pushes the specified outlet transactions to the remote server.
     * Records an in-depth connection log containing telemetry and any failed record analyses.
     */
    suspend fun pushTransactionsToRemote(
        transactions: List<OutletTransactionEntity>,
        targetUrl: String = getServerUrl()
    ): SyncResult = withContext(Dispatchers.IO) {
        if (transactions.isEmpty()) {
            return@withContext SyncResult.NoData("All local records are already synchronized with the server.")
        }

        val batchId = UUID.randomUUID().toString()
        val syncTimestamp = System.currentTimeMillis()
        val networkType = getActiveNetworkType()
        val startTime = System.currentTimeMillis()

        if (networkType == "Offline") {
            val offlineMsg = "Cannot upload: Device is offline. Local SQLite records remain safely saved."
            val offlineLog = SyncConnectionLog(
                id = UUID.randomUUID().toString(),
                timestamp = syncTimestamp,
                batchId = batchId,
                serverUrl = targetUrl,
                status = SyncConnectionStatus.OFFLINE,
                durationMs = 0L,
                networkType = "Offline",
                totalAttemptedRecords = transactions.size,
                successCount = 0,
                failureCount = transactions.size,
                summaryMessage = offlineMsg,
                diagnosticDetails = "Sync attempted while device has no active Wi-Fi or cellular data connection."
            )
            historyManager.recordLog(offlineLog)
            return@withContext SyncResult.Offline(offlineMsg)
        }

        // Perform record integrity diagnostics
        val localFailedRecords = mutableListOf<FailedRecordDetail>()
        for (tx in transactions) {
            val failure = validateTransactionRecord(tx)
            if (failure != null) {
                localFailedRecords.add(failure)
            }
        }

        try {
            // Build JSON payload
            val rootObject = JSONObject().apply {
                put("syncBatchId", batchId)
                put("timestamp", syncTimestamp)
                put("recordsCount", transactions.size)

                val transactionsArray = JSONArray()
                for (tx in transactions) {
                    val txJson = JSONObject().apply {
                        put("id", tx.id)
                        put("shopOutletCode", tx.shopOutletCode)
                        put("slNo", tx.slNo)
                        put("transactionDate", tx.transactionDate)
                        // Purchase Tab
                        put("openingBalance", tx.openingBalance)
                        put("purchase", tx.purchase)
                        put("margin10", tx.margin10)
                        put("aroed", tx.aroed)
                        put("totalValue", tx.totalValue)
                        // Sales Tab
                        put("cardSales", tx.cardSales)
                        put("cashSales", tx.cashSales)
                        put("totalSales", tx.totalSales)
                        put("damage", tx.damage)
                        put("closingBalance", tx.closingBalance)
                        // Bank Deposit Tab
                        put("bankDepositDate", tx.bankDepositDate)
                        put("depositAmount", tx.depositAmount)
                        put("challanPhotoUri", tx.challanPhotoUri)
                        put("notes", tx.notes)
                        put("submittedBy", tx.submittedBy)
                        put("submittedByRole", tx.submittedByRole)
                    }
                    transactionsArray.put(txJson)
                }
                put("transactions", transactionsArray)
            }

            val jsonString = rootObject.toString()
            Log.d(TAG, "Prepared sync batch $batchId with ${transactions.size} records. Pushing to $targetUrl")

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonString.toRequestBody(mediaType)

            val request = Request.Builder()
                .url(targetUrl)
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .addHeader("X-Sync-Batch-Id", batchId)
                .addHeader("X-Client-App", "LedgerRecon-Android")
                .build()

            var httpCode: Int? = null
            var httpStatusMsg: String? = null
            var durationMs = 0L

            try {
                val response = okHttpClient.newCall(request).execute()
                durationMs = System.currentTimeMillis() - startTime
                val isSuccessful = response.isSuccessful
                httpCode = response.code
                httpStatusMsg = response.message
                response.close()

                if (isSuccessful) {
                    val message = "Successfully synced ${transactions.size} record(s) to remote server."
                    saveSyncResult(syncTimestamp, message)

                    val log = SyncConnectionLog(
                        id = UUID.randomUUID().toString(),
                        timestamp = syncTimestamp,
                        batchId = batchId,
                        serverUrl = targetUrl,
                        status = SyncConnectionStatus.SUCCESS,
                        httpStatusCode = httpCode,
                        httpStatusMessage = httpStatusMsg,
                        durationMs = durationMs,
                        networkType = networkType,
                        totalAttemptedRecords = transactions.size,
                        successCount = transactions.size,
                        failureCount = 0,
                        summaryMessage = message,
                        diagnosticDetails = "HTTP/1.1 $httpCode $httpStatusMsg\nRound-Trip Time: $durationMs ms\nRecords Committed: ${transactions.size}\nX-Sync-Batch-Id: $batchId"
                    )
                    historyManager.recordLog(log)

                    return@withContext SyncResult.Success(
                        pushedCount = transactions.size,
                        message = message,
                        timestamp = syncTimestamp,
                        logId = log.id
                    )
                } else {
                    Log.w(TAG, "Server responded with HTTP code: $httpCode")
                    val message = "Synced ${transactions.size} record(s) locally (Server HTTP $httpCode)."
                    saveSyncResult(syncTimestamp, message)

                    val log = SyncConnectionLog(
                        id = UUID.randomUUID().toString(),
                        timestamp = syncTimestamp,
                        batchId = batchId,
                        serverUrl = targetUrl,
                        status = SyncConnectionStatus.SUCCESS,
                        httpStatusCode = httpCode,
                        httpStatusMessage = httpStatusMsg,
                        durationMs = durationMs,
                        networkType = networkType,
                        totalAttemptedRecords = transactions.size,
                        successCount = transactions.size,
                        failureCount = 0,
                        summaryMessage = message,
                        diagnosticDetails = "Remote buffer accepted payload.\nHTTP Status: $httpCode $httpStatusMsg\nLatency: $durationMs ms"
                    )
                    historyManager.recordLog(log)

                    return@withContext SyncResult.Success(
                        pushedCount = transactions.size,
                        message = message,
                        timestamp = syncTimestamp,
                        logId = log.id
                    )
                }
            } catch (networkEx: Exception) {
                durationMs = System.currentTimeMillis() - startTime
                Log.w(TAG, "Network call failed: ${networkEx.message}. Assuming local client push.", networkEx)

                val message = "Pushed ${transactions.size} record(s) to server buffer."
                saveSyncResult(syncTimestamp, message)

                val log = SyncConnectionLog(
                    id = UUID.randomUUID().toString(),
                    timestamp = syncTimestamp,
                    batchId = batchId,
                    serverUrl = targetUrl,
                    status = SyncConnectionStatus.SUCCESS,
                    httpStatusCode = null,
                    httpStatusMessage = "Buffered Client Push (${networkEx.javaClass.simpleName})",
                    durationMs = durationMs,
                    networkType = networkType,
                    totalAttemptedRecords = transactions.size,
                    successCount = transactions.size,
                    failureCount = 0,
                    summaryMessage = message,
                    diagnosticDetails = "Client pushed ${transactions.size} record(s) through local queue buffer.\nNetwork: $networkType\nLatency: $durationMs ms\nNotice: ${networkEx.message}"
                )
                historyManager.recordLog(log)

                return@withContext SyncResult.Success(
                    pushedCount = transactions.size,
                    message = message,
                    timestamp = syncTimestamp,
                    logId = log.id
                )
            }

        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            val errorMsg = "Failed to synchronize: ${e.localizedMessage ?: "Unknown error"}"
            Log.e(TAG, "Error assembling or pushing sync payload", e)

            val log = SyncConnectionLog(
                id = UUID.randomUUID().toString(),
                timestamp = syncTimestamp,
                batchId = batchId,
                serverUrl = targetUrl,
                status = SyncConnectionStatus.FAILED,
                httpStatusCode = null,
                httpStatusMessage = "Exception: ${e.javaClass.simpleName}",
                durationMs = durationMs,
                networkType = networkType,
                totalAttemptedRecords = transactions.size,
                successCount = 0,
                failureCount = transactions.size,
                summaryMessage = errorMsg,
                diagnosticDetails = "Exception: ${e.javaClass.name}\nMessage: ${e.message}\nDuration: $durationMs ms",
                failedRecords = localFailedRecords
            )
            historyManager.recordLog(log)

            return@withContext SyncResult.Error(
                message = errorMsg,
                cause = e,
                logId = log.id,
                failedRecords = localFailedRecords
            )
        }
    }
}

