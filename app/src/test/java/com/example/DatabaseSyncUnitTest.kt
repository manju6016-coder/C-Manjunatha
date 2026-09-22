package com.example

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.model.DatabaseSyncState
import com.example.data.model.OutletTransactionEntity
import com.example.data.model.SyncResult
import com.example.data.repository.LedgerRepository
import com.example.data.sync.RemoteSyncService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DatabaseSyncUnitTest {

    private lateinit var app: Application
    private lateinit var database: AppDatabase
    private lateinit var repository: LedgerRepository
    private lateinit var syncService: RemoteSyncService

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = LedgerRepository.fromDatabase(database)
        syncService = RemoteSyncService(app)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testDatabaseSyncStateProperties() {
        val stateSynced = DatabaseSyncState(
            totalRecords = 10,
            pendingRecords = 0,
            syncedRecords = 10,
            isOnline = true,
            isSyncing = false
        )
        assertFalse(stateSynced.hasPendingChanges)
        assertTrue(stateSynced.isFullySynced)

        val statePending = DatabaseSyncState(
            totalRecords = 10,
            pendingRecords = 3,
            syncedRecords = 7,
            isOnline = true,
            isSyncing = false
        )
        assertTrue(statePending.hasPendingChanges)
        assertFalse(statePending.isFullySynced)
    }

    @Test
    fun testSyncServiceSaveAndRestoreLastSyncResult() {
        val testTime = 1716000000000L
        val testMsg = "Successfully pushed 5 transaction record(s) to server."

        syncService.saveSyncResult(testTime, testMsg)

        assertEquals(testTime, syncService.getLastSyncTimestamp())
        assertEquals(testMsg, syncService.getLastSyncMessage())
    }

    @Test
    fun testRepositoryPendingTransactionsLifecycle() = runBlocking {
        repository.seedInitialDataIfEmpty()

        // Count initial transactions
        val initialTotal = repository.totalTransactionsCount.first()
        assertTrue(initialTotal > 0)

        // Seeded transactions should be SYNCED
        val pendingInitial = repository.pendingTransactionsCount.first()
        assertEquals(0, pendingInitial)

        // Insert a new transaction that is pending sync
        val newTx = OutletTransactionEntity(
            shopOutletCode = "SHOP001",
            slNo = 99,
            transactionDate = 1716000000000L,
            openingBalance = 10000L,
            isOpeningBalanceManual = true,
            purchase = 5000L,
            margin10 = 500L,
            aroed = 200L,
            totalValue = 15700L,
            cardSales = 2000L,
            cashSales = 3000L,
            totalSales = 5000L,
            damage = 100L,
            closingBalance = 10600L,
            bankDepositDate = 1716000000000L,
            depositAmount = 5000L,
            syncStatus = "PENDING",
            syncedAt = null,
            createdAt = System.currentTimeMillis()
        )
        repository.insertOutletTransaction(newTx)

        val pendingAfterInsert = repository.pendingTransactionsCount.first()
        assertEquals(1, pendingAfterInsert)

        val pendingList = repository.getPendingTransactions()
        assertEquals(1, pendingList.size)
        assertEquals(99, pendingList[0].slNo)
        assertEquals("PENDING", pendingList[0].syncStatus)

        // Mark as synced
        val syncTime = System.currentTimeMillis()
        repository.markTransactionsAsSynced(listOf(pendingList[0].id), syncTime)

        val pendingAfterSync = repository.pendingTransactionsCount.first()
        assertEquals(0, pendingAfterSync)

        val syncedCount = repository.syncedTransactionsCount.first()
        assertEquals(initialTotal + 1, syncedCount)
    }

    @Test
    fun testSyncResultVariants() {
        val success = SyncResult.Success(10, "Pushed 10 records", 1716000000000L)
        assertEquals(10, success.pushedCount)
        assertEquals("Pushed 10 records", success.message)

        val offline = SyncResult.Offline("No network")
        assertEquals("No network", offline.message)

        val noData = SyncResult.NoData("No pending records")
        assertEquals("No pending records", noData.message)

        val error = SyncResult.Error("HTTP 500 Server Error")
        assertEquals("HTTP 500 Server Error", error.message)
    }

    @Test
    fun testSyncHistoryManagerAndDiagnostics() {
        val historyManager = syncService.historyManager
        historyManager.clearLogs()
        assertEquals(0, historyManager.logsFlow.value.size)

        // Generate simulated failure log
        historyManager.simulateDebugFailureLog()
        val logs = historyManager.logsFlow.value
        assertEquals(1, logs.size)

        val failedLog = logs[0]
        assertTrue(failedLog.hasFailedRecords)
        assertEquals(2, failedLog.failedRecords.size)

        val firstRecord = failedLog.failedRecords[0]
        assertEquals("ERR_CLOSING_BAL_MISMATCH", firstRecord.errorCode)
        assertTrue(firstRecord.failureReason.contains("discrepancy", ignoreCase = true) || firstRecord.failureReason.contains("mismatch", ignoreCase = true))
        assertTrue(firstRecord.suggestedFix.isNotEmpty())

        // Test diagnostic text export
        val exportText = historyManager.formatDiagnosticText(failedLog)
        assertTrue(exportText.contains("=== LEDGER RECON SYNC CONNECTION DIAGNOSTIC ==="))
        assertTrue(exportText.contains("ERR_CLOSING_BAL_MISMATCH"))
        assertTrue(exportText.contains("FAILED RECORDS ANALYSIS"))
    }

    @Test
    fun testDatabaseSyncStateHasErrors() {
        val stateOk = DatabaseSyncState(
            totalRecords = 10,
            pendingRecords = 0,
            syncedRecords = 10,
            failedRecordCount = 0
        )
        assertFalse(stateOk.hasErrors)

        val stateWithError = DatabaseSyncState(
            totalRecords = 10,
            pendingRecords = 2,
            syncedRecords = 8,
            failedRecordCount = 2
        )
        assertTrue(stateWithError.hasErrors)
    }
}
