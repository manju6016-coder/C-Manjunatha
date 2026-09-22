package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.LedgerDao
import com.example.data.model.BankDeposit
import com.example.data.model.BankDepositEntity
import com.example.data.model.BankDepositRecordEntity
import com.example.data.model.DailyReconciliationEntity
import com.example.data.model.OutletTransactionEntity
import com.example.data.model.Purchase
import com.example.data.model.PurchaseEntity
import com.example.data.model.PurchaseRecordEntity
import com.example.data.model.SaleEntity
import com.example.data.model.Sales
import com.example.data.model.SalesRecordEntity
import com.example.data.model.ShopEntity
import com.example.data.model.UserEntity

/**
 * Typealias for TransactionDatabase to support unified access to Room Database.
 */
typealias TransactionDatabase = AppDatabase

/**
 * Main Room Database class for local storage of transaction records.
 *
 * Includes:
 * - [Purchase] ([PurchaseEntity]) & [PurchaseRecordEntity]
 * - [Sales] ([SaleEntity]) & [SalesRecordEntity]
 * - [BankDeposit] ([BankDepositEntity]) & [BankDepositRecordEntity]
 * - [OutletTransactionEntity]
 * - [DailyReconciliationEntity]
 * - [UserEntity]
 * - [ShopEntity]
 *
 * Current Database Version: 5
 * Migration Strategies:
 * - Version 1 -> 2: Added shopId column to purchases, sales, and bank_deposits; created users, shops, outlet_transactions tables.
 * - Version 2 -> 3: Created dedicated purchase_records, sales_records, bank_deposit_records tables; added indexing on shopId and dates.
 * - Version 3 -> 4: Added password column and index to users table.
 * - Version 4 -> 5: Added syncStatus and syncedAt columns with indexing to outlet_transactions table.
 * Fallback: [fallbackToDestructiveMigrationOnDowngrade] and [fallbackToDestructiveMigration] enabled for safety.
 */
@Database(
    entities = [
        Purchase::class,
        Sales::class,
        BankDeposit::class,
        DailyReconciliationEntity::class,
        UserEntity::class,
        ShopEntity::class,
        OutletTransactionEntity::class,
        PurchaseRecordEntity::class,
        SalesRecordEntity::class,
        BankDepositRecordEntity::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ledgerDao(): LedgerDao
    abstract fun purchaseDao(): com.example.data.dao.PurchaseDao
    abstract fun salesDao(): com.example.data.dao.SalesDao
    abstract fun bankDepositDao(): com.example.data.dao.BankDepositDao
    abstract fun outletTransactionDao(): com.example.data.dao.OutletTransactionDao
    abstract fun shopDao(): com.example.data.dao.ShopDao
    abstract fun userDao(): com.example.data.dao.UserDao
    abstract fun dailyReconciliationDao(): com.example.data.dao.DailyReconciliationDao

    companion object {
        const val DATABASE_NAME = "ledger_recon_database"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Migration from version 1 to 2:
         * - Added shopId column to purchases, sales, and bank_deposits.
         * - Created users, shops, and outlet_transactions tables with necessary indices.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add shopId to existing transaction tables
                db.execSQL("ALTER TABLE purchases ADD COLUMN shopId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE sales ADD COLUMN shopId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE bank_deposits ADD COLUMN shopId TEXT NOT NULL DEFAULT ''")

                // Create users table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS users (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        outletCode TEXT NOT NULL,
                        name TEXT NOT NULL,
                        mobileNumber TEXT NOT NULL,
                        role TEXT NOT NULL,
                        assignedShopCodes TEXT NOT NULL DEFAULT '*'
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_users_outletCode_mobileNumber ON users(outletCode, mobileNumber)")

                // Create shops table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS shops (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        outletCode TEXT NOT NULL,
                        shopName TEXT NOT NULL,
                        address TEXT NOT NULL,
                        assignedRicCode TEXT NOT NULL DEFAULT '',
                        assignedEmployeeCodes TEXT NOT NULL DEFAULT '',
                        initialOpeningBalance INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_shops_outletCode ON shops(outletCode)")

                // Create outlet_transactions table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS outlet_transactions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        shopOutletCode TEXT NOT NULL,
                        slNo INTEGER NOT NULL,
                        transactionDate INTEGER NOT NULL,
                        openingBalance INTEGER NOT NULL,
                        isOpeningBalanceManual INTEGER NOT NULL DEFAULT 0,
                        purchase INTEGER NOT NULL DEFAULT 0,
                        margin10 INTEGER NOT NULL DEFAULT 0,
                        aroed INTEGER NOT NULL DEFAULT 0,
                        totalValue INTEGER NOT NULL DEFAULT 0,
                        cardSales INTEGER NOT NULL DEFAULT 0,
                        cashSales INTEGER NOT NULL DEFAULT 0,
                        totalSales INTEGER NOT NULL DEFAULT 0,
                        damage INTEGER NOT NULL DEFAULT 0,
                        closingBalance INTEGER NOT NULL DEFAULT 0,
                        bankDepositDate INTEGER NOT NULL DEFAULT 0,
                        depositAmount INTEGER NOT NULL DEFAULT 0,
                        challanPhotoUri TEXT NOT NULL DEFAULT '',
                        notes TEXT NOT NULL DEFAULT '',
                        submittedBy TEXT NOT NULL DEFAULT '',
                        submittedByRole TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_outlet_transactions_shopOutletCode ON outlet_transactions(shopOutletCode)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_outlet_transactions_transactionDate ON outlet_transactions(transactionDate)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_outlet_transactions_shopOutletCode_transactionDate ON outlet_transactions(shopOutletCode, transactionDate)")
            }
        }

        /**
         * Migration from version 2 to 3:
         * - Created dedicated tables for purchase_records, sales_records, and bank_deposit_records.
         * - Added indices to improve performance for queries by shop ID, date, and ranges.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create purchase_records table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS purchase_records (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        shopId TEXT NOT NULL,
                        transactionDate INTEGER NOT NULL,
                        slNo INTEGER NOT NULL DEFAULT 1,
                        openingBalance INTEGER NOT NULL DEFAULT 0,
                        purchase INTEGER NOT NULL DEFAULT 0,
                        margin10 INTEGER NOT NULL DEFAULT 0,
                        aroed INTEGER NOT NULL DEFAULT 0,
                        totalValue INTEGER NOT NULL DEFAULT 0,
                        notes TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_purchase_records_shopId ON purchase_records(shopId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_purchase_records_transactionDate ON purchase_records(transactionDate)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_purchase_records_shopId_transactionDate ON purchase_records(shopId, transactionDate)")

                // Create sales_records table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS sales_records (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        shopId TEXT NOT NULL,
                        transactionDate INTEGER NOT NULL,
                        slNo INTEGER NOT NULL DEFAULT 1,
                        totalValue INTEGER NOT NULL DEFAULT 0,
                        cardSales INTEGER NOT NULL DEFAULT 0,
                        cashSales INTEGER NOT NULL DEFAULT 0,
                        totalSales INTEGER NOT NULL DEFAULT 0,
                        damage INTEGER NOT NULL DEFAULT 0,
                        closingBalance INTEGER NOT NULL DEFAULT 0,
                        notes TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sales_records_shopId ON sales_records(shopId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sales_records_transactionDate ON sales_records(transactionDate)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sales_records_shopId_transactionDate ON sales_records(shopId, transactionDate)")

                // Create bank_deposit_records table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS bank_deposit_records (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        shopId TEXT NOT NULL,
                        transactionDate INTEGER NOT NULL,
                        bankDepositDate INTEGER NOT NULL,
                        depositAmount INTEGER NOT NULL DEFAULT 0,
                        cardSales INTEGER NOT NULL DEFAULT 0,
                        totalBankDeposit INTEGER NOT NULL DEFAULT 0,
                        challanPhotoUri TEXT NOT NULL DEFAULT '',
                        bankName TEXT NOT NULL DEFAULT 'Designated Outlet Bank',
                        referenceNumber TEXT NOT NULL DEFAULT '',
                        notes TEXT NOT NULL DEFAULT '',
                        submittedBy TEXT NOT NULL DEFAULT '',
                        submittedByRole TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_bank_deposit_records_shopId ON bank_deposit_records(shopId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_bank_deposit_records_transactionDate ON bank_deposit_records(transactionDate)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_bank_deposit_records_bankDepositDate ON bank_deposit_records(bankDepositDate)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_bank_deposit_records_shopId_transactionDate ON bank_deposit_records(shopId, transactionDate)")

                // Indices on legacy tables
                db.execSQL("CREATE INDEX IF NOT EXISTS index_purchases_shopId ON purchases(shopId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_purchases_date ON purchases(date)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sales_shopId ON sales(shopId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sales_date ON sales(date)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_bank_deposits_shopId ON bank_deposits(shopId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_bank_deposits_date ON bank_deposits(date)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_daily_reconciliations_dateEpochStart ON daily_reconciliations(dateEpochStart)")
            }
        }

        /**
         * Migration from version 3 to 4:
         * - Added password column to users table for RIC In-Charge and Shop Employee password management.
         * - Added index on outletCode in users table.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN password TEXT NOT NULL DEFAULT ''")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_users_outletCode ON users(outletCode)")
            }
        }

        /**
         * Migration from version 4 to 5:
         * - Added syncStatus and syncedAt columns to outlet_transactions table for remote sync tracking.
         * - Added index on syncStatus in outlet_transactions table.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE outlet_transactions ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")
                db.execSQL("ALTER TABLE outlet_transactions ADD COLUMN syncedAt INTEGER")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_outlet_transactions_syncStatus ON outlet_transactions(syncStatus)")
            }
        }

        /**
         * List of all defined incremental schema migrations for the database.
         */
        val ALL_MIGRATIONS: Array<Migration> = arrayOf(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5
        )

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(*ALL_MIGRATIONS)
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

