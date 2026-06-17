package com.example.flickfind_ltttbdd.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieDao {
    // 1. Lấy toàn bộ danh sách phim yêu thích theo User (Trả về Flow để UI lắng nghe thời gian thực)
    @Query("SELECT * FROM favorite_movies WHERE userId = :userId")
    fun getAllFavorites(userId: String): Flow<List<FavoriteMovieEntity>>

    // 2. Thêm một bộ phim vào danh sách yêu thích (Nếu trùng ID sẽ ghi đè/cập nhật)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(movie: FavoriteMovieEntity): Long

    // 3. Xóa một bộ phim khỏi danh sách yêu thích
    @Delete
    suspend fun deleteFavorite(movie: FavoriteMovieEntity): Int

    // 4. Kiểm tra xem phim đã được thích chưa (trả về null nếu chưa có)
    @Query("SELECT * FROM favorite_movies WHERE id = :movieId AND userId = :userId")
    suspend fun getMovieById(movieId: String, userId: String): FavoriteMovieEntity?
}