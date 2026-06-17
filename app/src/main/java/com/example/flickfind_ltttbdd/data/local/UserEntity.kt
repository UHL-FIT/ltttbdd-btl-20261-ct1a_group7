package com.example.flickfind_ltttbdd.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserEntity(
    @PrimaryKey val id: String, // ID từ Firebase
    val name: String,
    val avatarUrl: String // Đường dẫn ảnh đại diện (có thể để ảnh mặc định hoặc link ảnh)
)