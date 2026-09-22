package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.UserEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Users and Authentication.
 */
@Dao
interface UserDao {

    @Query("SELECT * FROM users ORDER BY name ASC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE LOWER(outletCode) = LOWER(:outletCode) AND mobileNumber = :mobileNumber LIMIT 1")
    suspend fun getUserByOutletCodeAndMobile(outletCode: String, mobileNumber: String): UserEntity?

    @Query("SELECT * FROM users WHERE LOWER(outletCode) = LOWER(:outletCode) LIMIT 1")
    suspend fun getUserByOutletCode(outletCode: String): UserEntity?

    @Query("SELECT * FROM users")
    suspend fun getAllUsersList(): List<UserEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET mobileNumber = :mobileNumber WHERE id = :userId")
    suspend fun updateUserMobileNumber(userId: Long, mobileNumber: String)

    @Query("UPDATE users SET mobileNumber = :mobileNumber, password = :password WHERE id = :userId")
    suspend fun updateUserMobileAndPassword(userId: Long, mobileNumber: String, password: String)

    @Query("UPDATE users SET password = :password WHERE id = :userId")
    suspend fun updateUserPassword(userId: Long, password: String)

    @Query("UPDATE users SET password = :password WHERE LOWER(outletCode) = LOWER(:outletCode)")
    suspend fun updateUserPasswordByOutletCode(outletCode: String, password: String)

    @Delete
    suspend fun deleteUser(user: UserEntity)

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUsersCount(): Int
}
