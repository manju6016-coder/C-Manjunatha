package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ShopEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Retail Shops / Outlets.
 */
@Dao
interface ShopDao {

    @Query("SELECT * FROM shops ORDER BY outletCode ASC")
    fun getAllShops(): Flow<List<ShopEntity>>

    @Query("SELECT * FROM shops WHERE LOWER(outletCode) = LOWER(:outletCode) LIMIT 1")
    suspend fun getShopByOutletCode(outletCode: String): ShopEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShop(shop: ShopEntity): Long

    @Update
    suspend fun updateShop(shop: ShopEntity)

    @Delete
    suspend fun deleteShop(shop: ShopEntity)

    @Query("UPDATE shops SET assignedRicCode = :newRicCode WHERE LOWER(assignedRicCode) = LOWER(:oldRicCode)")
    suspend fun reassignRicInShops(oldRicCode: String, newRicCode: String)

    @Query("UPDATE shops SET assignedEmployeeCodes = :newEmpCode WHERE LOWER(assignedEmployeeCodes) = LOWER(:oldEmpCode)")
    suspend fun reassignEmployeeInShops(oldEmpCode: String, newEmpCode: String)

    @Query("SELECT COUNT(*) FROM shops")
    suspend fun getShopsCount(): Int
}
