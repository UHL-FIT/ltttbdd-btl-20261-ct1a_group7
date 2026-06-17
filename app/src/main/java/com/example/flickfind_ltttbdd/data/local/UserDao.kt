package com.example.flickfind_ltttbdd.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    // 1. Lấy thông tin người dùng theo ID
    @Query("SELECT * FROM user_profile WHERE id = :userId")
    fun getUserProfile(userId: String): Flow<UserEntity?>

    // 2. Cập nhật hoặc khởi tạo thông tin người dùng
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUser(user: UserEntity): Long
}