package com.example.data.dao

import androidx.room.Dao

/**
 * Unified Room DAO extending all individual domain DAOs:
 * - [PurchaseDao]
 * - [SalesDao]
 * - [BankDepositDao]
 * - [OutletTransactionDao]
 * - [ShopDao]
 * - [UserDao]
 * - [DailyReconciliationDao]
 *
 * This maintains full backwards compatibility with any existing queries
 * while allowing modular DAO usage across the repository and data layer.
 */
@Dao
interface LedgerDao :
    PurchaseDao,
    SalesDao,
    BankDepositDao,
    OutletTransactionDao,
    ShopDao,
    UserDao,
    DailyReconciliationDao
