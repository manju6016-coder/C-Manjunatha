package com.example

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.util.DatabaseBackupUtility
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DatabaseBackupUtilityTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // Ensure database is initialized
        val db = AppDatabase.getDatabase(context)
        db.openHelper.writableDatabase.execSQL("CREATE TABLE IF NOT EXISTS test_backup_marker (id INTEGER PRIMARY KEY, value TEXT)")
        db.openHelper.writableDatabase.execSQL("INSERT OR REPLACE INTO test_backup_marker (id, value) VALUES (1, 'active_record')")
        DatabaseBackupUtility.ensureDatabaseFileExists(context)
    }

    @Test
    fun testGenerateBackupFileNameFormat() {
        val fileName = DatabaseBackupUtility.generateBackupFileName()
        assertTrue("Filename should start with prefix", fileName.startsWith("ledger_recon_backup_"))
        assertTrue("Filename should end with .db", fileName.endsWith(".db"))
    }

    @Test
    fun testCheckpointDatabase() {
        val checkpointSuccess = DatabaseBackupUtility.checkpointDatabase(context)
        assertTrue("WAL checkpoint should succeed without error", checkpointSuccess)
    }

    @Test
    fun testGetDatabaseMetadata() = runBlocking {
        val metadata = DatabaseBackupUtility.getDatabaseMetadata(context)
        assertEquals(AppDatabase.DATABASE_NAME, metadata.databaseName)
        assertTrue("Integrity check should pass", metadata.isIntegrityOk)
        assertTrue("File size should be formatted", metadata.formattedSize.isNotBlank())
        assertTrue("Date should be formatted", metadata.formattedLastModified.isNotBlank())
    }

    @Test
    fun testExportToExternalStorage() = runBlocking {
        val result = DatabaseBackupUtility.exportToExternalStorage(context)
        assertTrue("Export operation should succeed: ${result.message}", result.success)
        assertTrue("Backup file name should not be blank", result.fileName.isNotBlank())
        assertNotNull("Destination description or URI should be present", result.destinationDescription)
    }

    @Test
    fun testCreateShareIntent() = runBlocking {
        val intent = DatabaseBackupUtility.createShareIntent(context)
        assertNotNull("Share intent should not be null", intent)
        assertEquals(Intent.ACTION_SEND, intent?.action)
        assertEquals("application/x-sqlite3", intent?.type)
        assertTrue("Intent should grant read URI permission", (intent?.flags ?: 0) and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
    }

    @Test
    fun testFormatFileSize() {
        assertEquals("500 B", DatabaseBackupUtility.formatFileSize(500L))
        assertEquals("1.0 KB", DatabaseBackupUtility.formatFileSize(1024L))
        assertEquals("2.50 MB", DatabaseBackupUtility.formatFileSize((2.5 * 1024 * 1024).toLong()))
    }
}
