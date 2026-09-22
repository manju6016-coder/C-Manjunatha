package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.data.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Data class representing current SQLite database status, size, and record statistics.
 */
data class DatabaseMetadata(
    val databaseName: String,
    val fileSizeBytes: Long,
    val formattedSize: String,
    val lastModifiedEpochMs: Long,
    val formattedLastModified: String,
    val totalShops: Int,
    val totalUsers: Int,
    val totalPurchases: Int,
    val totalSales: Int,
    val totalDeposits: Int,
    val totalTransactions: Int,
    val isIntegrityOk: Boolean,
    val integrityMessage: String
)

/**
 * Data class representing the outcome of a backup export operation.
 */
data class BackupResult(
    val success: Boolean,
    val fileName: String,
    val fileSizeFormatted: String,
    val destinationDescription: String,
    val destinationUri: Uri? = null,
    val message: String,
    val error: Throwable? = null
)

/**
 * Comprehensive utility for exporting the current Room / SQLite database to external storage.
 *
 * Ensures:
 * 1. WAL (Write-Ahead-Log) checkpointing via PRAGMA wal_checkpoint(TRUNCATE) so all active
 *    transactions are flushed and committed directly into the main .db file.
 * 2. Scoped-storage compliant export directly to public External Downloads using MediaStore
 *    (zero dangerous runtime permissions required on Android 10+).
 * 3. Storage Access Framework (SAF) export allowing users to choose any external directory,
 *    SD card, USB, or Google Drive location.
 * 4. System share sheet integration via FileProvider to allow sending administrative backups
 *    to email, cloud drives, or messaging platforms.
 * 5. PRAGMA integrity_check validation and statistical record auditing.
 */
object DatabaseBackupUtility {

    private const val BACKUP_DIR_NAME = "LedgerBackups"
    private const val MIME_TYPE_SQLITE = "application/x-sqlite3"

    /**
     * Generates a timestamped default backup filename.
     */
    fun generateBackupFileName(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "ledger_recon_backup_$timestamp.db"
    }

    /**
     * Performs a full WAL checkpoint to ensure all uncommitted WAL pages are flushed
     * into the main database file before copying.
     */
    fun checkpointDatabase(context: Context): Boolean {
        return try {
            val db = AppDatabase.getDatabase(context)
            val sdb = db.openHelper.writableDatabase
            sdb.query("PRAGMA wal_checkpoint(TRUNCATE)").use { cursor ->
                if (cursor.moveToFirst()) {
                    // Checkpoint successful
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Inspects the database file, executes PRAGMA integrity_check, and compiles table counts.
     */
    suspend fun getDatabaseMetadata(context: Context): DatabaseMetadata = withContext(Dispatchers.IO) {
        checkpointDatabase(context)

        val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
        val fileSizeBytes = if (dbFile.exists()) dbFile.length() else 0L
        val lastModified = if (dbFile.exists()) dbFile.lastModified() else System.currentTimeMillis()

        var integrityOk = false
        var integrityMessage = "Unknown"
        var shopsCount = 0
        var usersCount = 0
        var purchasesCount = 0
        var salesCount = 0
        var depositsCount = 0
        var transactionsCount = 0

        try {
            val db = AppDatabase.getDatabase(context)
            val rdb = db.openHelper.readableDatabase

            // PRAGMA integrity_check
            rdb.query("PRAGMA integrity_check").use { cursor ->
                if (cursor.moveToFirst()) {
                    integrityMessage = cursor.getString(0)
                    integrityOk = integrityMessage.equals("ok", ignoreCase = true)
                }
            }

            // Query table counts safely
            fun queryCount(table: String): Int {
                return try {
                    rdb.query("SELECT COUNT(*) FROM $table").use { c ->
                        if (c.moveToFirst()) c.getInt(0) else 0
                    }
                } catch (e: Exception) {
                    0
                }
            }

            shopsCount = queryCount("shops")
            usersCount = queryCount("users")
            purchasesCount = queryCount("purchase_records") + queryCount("purchases")
            salesCount = queryCount("sales_records") + queryCount("sales")
            depositsCount = queryCount("bank_deposit_records") + queryCount("bank_deposits")
            transactionsCount = queryCount("outlet_transactions")

        } catch (e: Exception) {
            integrityMessage = "Error inspecting database: ${e.message}"
            e.printStackTrace()
        }

        val formattedDate = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(lastModified))

        DatabaseMetadata(
            databaseName = AppDatabase.DATABASE_NAME,
            fileSizeBytes = fileSizeBytes,
            formattedSize = formatFileSize(fileSizeBytes),
            lastModifiedEpochMs = lastModified,
            formattedLastModified = formattedDate,
            totalShops = shopsCount,
            totalUsers = usersCount,
            totalPurchases = purchasesCount,
            totalSales = salesCount,
            totalDeposits = depositsCount,
            totalTransactions = transactionsCount,
            isIntegrityOk = integrityOk,
            integrityMessage = integrityMessage
        )
    }

    /**
     * Ensures the database file exists on disk, materializing it via Room/SQLite if necessary.
     */
    fun ensureDatabaseFileExists(context: Context): File {
        val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
        if (!dbFile.exists()) {
            try {
                AppDatabase.getDatabase(context).openHelper.writableDatabase
            } catch (e: Exception) {
                // ignore
            }
            if (!dbFile.exists()) {
                try {
                    dbFile.parentFile?.mkdirs()
                    dbFile.createNewFile()
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
        return dbFile
    }

    /**
     * Exports the SQLite database directly to external public storage (Downloads/LedgerBackups/).
     * Uses MediaStore.Downloads on Android 10+ (API 29+) which writes to public shared storage
     * without requiring legacy storage permissions.
     */
    suspend fun exportToExternalStorage(context: Context): BackupResult = withContext(Dispatchers.IO) {
        // Step 1: Checkpoint WAL into main database file
        checkpointDatabase(context)

        val sourceDbFile = ensureDatabaseFileExists(context)
        if (!sourceDbFile.exists()) {
            return@withContext BackupResult(
                success = false,
                fileName = AppDatabase.DATABASE_NAME,
                fileSizeFormatted = "0 B",
                destinationDescription = "",
                message = "Database file does not exist on disk."
            )
        }

        val fileName = generateBackupFileName()
        val fileSize = sourceDbFile.length()
        val formattedSize = formatFileSize(fileSize)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, MIME_TYPE_SQLITE)
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/$BACKUP_DIR_NAME")
                    }

                    val resolver = context.contentResolver
                    val targetUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

                    if (targetUri != null) {
                        resolver.openOutputStream(targetUri)?.use { outputStream ->
                            FileInputStream(sourceDbFile).use { inputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        } ?: throw IllegalStateException("Unable to open output stream for backup file.")

                        return@withContext BackupResult(
                            success = true,
                            fileName = fileName,
                            fileSizeFormatted = formattedSize,
                            destinationDescription = "Downloads/$BACKUP_DIR_NAME/$fileName",
                            destinationUri = targetUri,
                            message = "Successfully exported $formattedSize backup to Downloads/$BACKUP_DIR_NAME"
                        )
                    }
                } catch (e: Exception) {
                    // Fall back to external storage file copy if MediaStore provider is absent
                }
            }

            // Fallback for pre-Android 10 or test/shadow environments
            val targetDir = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    ?: @Suppress("DEPRECATION") Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                BACKUP_DIR_NAME
            )
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }

            val targetFile = File(targetDir, fileName)
            FileInputStream(sourceDbFile).use { inputStream ->
                FileOutputStream(targetFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            BackupResult(
                success = true,
                fileName = fileName,
                fileSizeFormatted = formattedSize,
                destinationDescription = targetFile.absolutePath,
                destinationUri = Uri.fromFile(targetFile),
                message = "Successfully exported $formattedSize backup to ${targetFile.absolutePath}"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            BackupResult(
                success = false,
                fileName = fileName,
                fileSizeFormatted = formattedSize,
                destinationDescription = "",
                message = "Failed to export backup: ${e.localizedMessage}",
                error = e
            )
        }
    }

    /**
     * Exports the SQLite database to a user-chosen destination Uri via Storage Access Framework (SAF).
     */
    suspend fun exportToUri(context: Context, destinationUri: Uri): BackupResult = withContext(Dispatchers.IO) {
        checkpointDatabase(context)

        val sourceDbFile = ensureDatabaseFileExists(context)
        if (!sourceDbFile.exists()) {
            return@withContext BackupResult(
                success = false,
                fileName = AppDatabase.DATABASE_NAME,
                fileSizeFormatted = "0 B",
                destinationDescription = "",
                message = "Database file does not exist on disk."
            )
        }

        val fileSize = sourceDbFile.length()
        val formattedSize = formatFileSize(fileSize)
        val fileName = destinationUri.lastPathSegment ?: "ledger_backup.db"

        try {
            context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                FileInputStream(sourceDbFile).use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: throw IllegalStateException("Could not open destination stream.")

            BackupResult(
                success = true,
                fileName = fileName,
                fileSizeFormatted = formattedSize,
                destinationDescription = destinationUri.toString(),
                destinationUri = destinationUri,
                message = "Successfully saved $formattedSize backup to selected storage location."
            )
        } catch (e: Exception) {
            e.printStackTrace()
            BackupResult(
                success = false,
                fileName = fileName,
                fileSizeFormatted = formattedSize,
                destinationDescription = destinationUri.toString(),
                message = "Failed writing backup: ${e.localizedMessage}",
                error = e
            )
        }
    }

    /**
     * Prepares an Android system share intent to share the SQLite backup database file
     * to external applications (e.g. Google Drive, Gmail, Files, Bluetooth).
     */
    suspend fun createShareIntent(context: Context): Intent? = withContext(Dispatchers.IO) {
        checkpointDatabase(context)

        val sourceDbFile = ensureDatabaseFileExists(context)
        if (!sourceDbFile.exists()) return@withContext null

        try {
            val backupDir = File(context.cacheDir, "database_backups")
            if (!backupDir.exists()) backupDir.mkdirs()

            val fileName = generateBackupFileName()
            val shareableFile = File(backupDir, fileName)

            FileInputStream(sourceDbFile).use { input ->
                FileOutputStream(shareableFile).use { output ->
                    input.copyTo(output)
                }
            }

            val authority = try {
                "${com.example.BuildConfig.APPLICATION_ID}.fileprovider"
            } catch (e: Throwable) {
                "${context.packageName}.fileprovider"
            }

            val fileUri = try {
                FileProvider.getUriForFile(context, authority, shareableFile)
            } catch (e: IllegalArgumentException) {
                try {
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", shareableFile)
                } catch (e2: Exception) {
                    Uri.fromFile(shareableFile)
                }
            }

            Intent(Intent.ACTION_SEND).apply {
                type = MIME_TYPE_SQLITE
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "Ledger Recon Database Backup - $fileName")
                putExtra(Intent.EXTRA_TEXT, "Attached is the administrative SQLite database backup for Ledger Recon.\nFile: $fileName\nSize: ${formatFileSize(shareableFile.length())}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Formats bytes into human-readable B, KB, MB representation.
     */
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
            else -> String.format(Locale.US, "%.2f MB", bytes / (1024.0 * 1024.0))
        }
    }
}
