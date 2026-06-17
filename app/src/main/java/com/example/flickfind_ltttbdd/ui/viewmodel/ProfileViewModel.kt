package com.example.flickfind_ltttbdd.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flickfind_ltttbdd.data.MovieRepository
import com.example.flickfind_ltttbdd.data.local.FavoriteMovieEntity
import com.example.flickfind_ltttbdd.data.local.UserEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class ProfileUiState(
    val user: UserEntity? = null,
    val favoriteMovies: List<FavoriteMovieEntity> = emptyList(),
    val genreDistribution: Map<String, Int> = emptyMap(),
    val totalWatchTime: Int = 0,
    val mostWatchedGenre: String = "Chưa có",
    val leastWatchedGenre: String = "Chưa có"
)

class ProfileViewModel(
    private val repository: MovieRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private var userDataJob: Job? = null
    private var favoritesDataJob: Job? = null

    // [GHI CHÚ]: Lắng nghe sự thay đổi tài khoản. Tự động reset và tải lại dữ liệu mới.
    private val authListener = FirebaseAuth.AuthStateListener { auth ->
        val userId = auth.currentUser?.uid
        if (userId != null) {
            // [GHI CHÚ]: Reset UI state về rỗng trước khi tải dữ liệu của tài khoản mới
            _uiState.value = ProfileUiState()
            loadProfileData(userId)
        } else {
            // Hủy các kết nối và xóa sạch dữ liệu khi đăng xuất
            userDataJob?.cancel()
            favoritesDataJob?.cancel()
            _uiState.value = ProfileUiState()
        }
    }

    init {
        Log.d("ProfileViewModel", "==> Khởi tạo ProfileViewModel")
        FirebaseAuth.getInstance().addAuthStateListener(authListener)
    }

    private fun loadProfileData(userId: String) {
        Log.i("ProfileViewModel", "Đang tải dữ liệu Profile cho user: $userId")
        userDataJob?.cancel()
        favoritesDataJob?.cancel()

        userDataJob = viewModelScope.launch {
            repository.getUserProfile(userId).collect { user ->
                _uiState.update { it.copy(user = user) }
            }
        }

        favoritesDataJob = viewModelScope.launch {
            repository.getAllFavorites(userId).collect { favorites ->
                Log.d("ProfileViewModel", "Cập nhật thống kê từ ${favorites.size} phim yêu thích")
                val watchedMovies = favorites.filter { it.isWatched }
                val totalTime = watchedMovies.sumOf { it.runtime }
                
                val genreCounts = watchedMovies
                    .flatMap { it.genre.split(",").map { g -> g.trim() } }
                    .filter { it.isNotEmpty() }
                    .groupingBy { it }
                    .eachCount()
                
                val mostWatched = genreCounts.maxByOrNull { it.value }?.key ?: "Chưa có"
                val leastWatched = genreCounts.minByOrNull { it.value }?.key ?: "Chưa có"

                Log.i("ProfileViewModel", "Thống kê: Đã xem ${watchedMovies.size} phim, Tổng thời gian: $totalTime phút")
                Log.v("ProfileViewModel", "Phân bổ thể loại cho biểu đồ: $genreCounts")

                _uiState.update { 
                    it.copy(
                        favoriteMovies = favorites,
                        totalWatchTime = totalTime,
                        genreDistribution = genreCounts,
                        mostWatchedGenre = mostWatched,
                        leastWatchedGenre = leastWatched
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("ProfileViewModel", "==> Hủy ProfileViewModel (onCleared)")
        FirebaseAuth.getInstance().removeAuthStateListener(authListener)
    }

    fun toggleWatched(movie: FavoriteMovieEntity) {
        Log.d("ProfileViewModel", "Toggle trạng thái xem: ${movie.title} (Hiện tại: ${movie.isWatched})")
        viewModelScope.launch {
            repository.addToFavorite(movie.copy(isWatched = !movie.isWatched))
        }
    }
    
    fun deleteFavorite(movie: FavoriteMovieEntity) {
        Log.d("ProfileViewModel", "Xóa phim khỏi yêu thích: ${movie.title}")
        viewModelScope.launch {
            repository.removeFromFavorite(movie)
        }
    }

    fun updateAvatar(url: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        Log.i("ProfileViewModel", "Cập nhật Avatar mới: $url")
        viewModelScope.launch {
            try {
                currentUser.updateProfile(
                    UserProfileChangeRequest.Builder()
                        .setPhotoUri(android.net.Uri.parse(url))
                        .build()
                ).await()
                
                repository.insertOrUpdateUser(
                    UserEntity(
                        id = currentUser.uid,
                        name = currentUser.displayName ?: "Người dùng",
                        avatarUrl = url
                    )
                )
                Log.d("ProfileViewModel", "Cập nhật Avatar thành công")
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Lỗi khi cập nhật Avatar", e)
                e.printStackTrace()
            }
        }
    }

    fun updateName(name: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        Log.i("ProfileViewModel", "Cập nhật tên mới: $name")
        viewModelScope.launch {
            try {
                currentUser.updateProfile(
                    UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()
                ).await()
                
                repository.insertOrUpdateUser(
                    UserEntity(
                        id = currentUser.uid,
                        name = name,
                        avatarUrl = currentUser.photoUrl?.toString() ?: ""
                    )
                )
                Log.d("ProfileViewModel", "Cập nhật tên thành công")
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Lỗi khi cập nhật tên", e)
                e.printStackTrace()
            }
        }
    }
}
