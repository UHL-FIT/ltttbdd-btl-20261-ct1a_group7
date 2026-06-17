package com.example.flickfind_ltttbdd.data

import com.example.flickfind_ltttbdd.data.local.FavoriteMovieEntity
import com.example.flickfind_ltttbdd.data.local.MovieDao
import com.example.flickfind_ltttbdd.data.local.UserDao
import com.example.flickfind_ltttbdd.data.local.UserEntity
import com.example.flickfind_ltttbdd.data.remote.MovieApiService
import com.example.flickfind_ltttbdd.data.remote.MovieResponse
import kotlinx.coroutines.flow.Flow

class MovieRepository(
    private val apiService: MovieApiService,
    private val movieDao: MovieDao,
    private val userDao: UserDao
) {

    // ==========================================
    // 1. PHẦN XỬ LÝ MOCK API (REMOTE)
    // ==========================================

    // Hàm lấy danh sách phim phân trang từ API có kèm bộ lọc, bọc trong Result để bắt lỗi mạng
    suspend fun getMoviesFromApi(
        page: Int,
        limit: Int,
        search: String? = null,
        genre: String? = null
    ): Result<List<MovieResponse>> {
        return try {
            val response = apiService.getMovies(page, limit, search, genre)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Lấy chi tiết một bộ phim từ API theo ID
    suspend fun getMovieById(id: String): Result<MovieResponse> {
        return try {
            val response = apiService.getMovieById(id)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    // ==========================================
    // 2. PHẦN XỬ LÝ DANH SÁCH YÊU THÍCH (ROOM DB)
    // ==========================================

    // Lấy luồng dữ liệu danh sách phim yêu thích thời gian thực
    fun getAllFavorites(userId: String): Flow<List<FavoriteMovieEntity>> = movieDao.getAllFavorites(userId)

    // Thêm một phim vào danh sách yêu thích
    suspend fun addToFavorite(movie: FavoriteMovieEntity) {
        movieDao.insertFavorite(movie)
    }

    // Xóa một phim khỏi danh sách yêu thích (CRUD - Delete)
    suspend fun removeFromFavorite(movie: FavoriteMovieEntity) {
        movieDao.deleteFavorite(movie)
    }

    // Kiểm tra xem phim này đã được lưu trong Room chưa
    suspend fun isMovieFavorite(movieId: String, userId: String): Boolean {
        return movieDao.getMovieById(movieId, userId) != null
    }


    // ==========================================
    // 3. PHẦN XỬ LÝ THÔNG TIN CÁ NHÂN (USER PROFILE)
    // ==========================================

    // Lấy thông tin User profile
    fun getUserProfile(userId: String): Flow<UserEntity?> = userDao.getUserProfile(userId)

    // Cập nhật hoặc khởi tạo User
    suspend fun insertOrUpdateUser(user: UserEntity) {
        userDao.insertOrUpdateUser(user)
    }

    // Cập nhật thông tin User (CRUD - Update)
    suspend fun updateUserProfile(user: UserEntity) {
        userDao.insertOrUpdateUser(user)
    }
}