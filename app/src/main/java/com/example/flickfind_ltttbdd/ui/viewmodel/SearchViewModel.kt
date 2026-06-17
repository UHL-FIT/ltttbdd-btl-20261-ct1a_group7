package com.example.flickfind_ltttbdd.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flickfind_ltttbdd.data.MovieRepository
import com.example.flickfind_ltttbdd.data.local.FavoriteMovieEntity
import com.example.flickfind_ltttbdd.data.remote.MovieResponse
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val movies: List<MovieResponse> = emptyList(),
    val favoriteMovieIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val query: String? = null,
    val genre: String? = null,
    val yearRange: String? = null,
    val sortBy: String? = null,
    val isEndReached: Boolean = false
)

class SearchViewModel(
    private val repository: MovieRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var currentSearchJob: Job? = null
    private var favoritesJob: Job? = null

    private val authListener = FirebaseAuth.AuthStateListener { auth ->
        val userId = auth.currentUser?.uid
        favoritesJob?.cancel()
        if (userId != null) {
            observeFavorites(userId)
        } else {
            _uiState.update { it.copy(favoriteMovieIds = emptySet()) }
        }
    }

    init {
        Log.d("SearchViewModel", "==> Khởi tạo SearchViewModel")
        FirebaseAuth.getInstance().addAuthStateListener(authListener)
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("SearchViewModel", "==> Hủy SearchViewModel (onCleared)")
        FirebaseAuth.getInstance().removeAuthStateListener(authListener)
    }

    private fun observeFavorites(userId: String) {
        favoritesJob = viewModelScope.launch {
            repository.getAllFavorites(userId).collect { favoriteEntities ->
                _uiState.update { currentState ->
                    currentState.copy(favoriteMovieIds = favoriteEntities.map { it.id }.toSet())
                }
            }
        }
    }

    fun setFiltersAndSearch(query: String?, genre: String?, yearRange: String?, sortBy: String? = null) {
        Log.d("SearchViewModel", "Thiết lập bộ lọc: query=$query, genre=$genre, year=$yearRange, sort=$sortBy")
        // Chuẩn hóa: Nếu chuỗi rỗng hoặc chỉ có khoảng trắng thì coi như null
        val cleanQuery = query?.takeIf { it.isNotBlank() }
        val cleanGenre = genre?.takeIf { it.isNotBlank() }
        val cleanYear = yearRange?.takeIf { it.isNotBlank() }
        val cleanSort = sortBy?.takeIf { it.isNotBlank() }

        _uiState.update { 
            it.copy(
                query = cleanQuery, 
                genre = cleanGenre, 
                yearRange = cleanYear,
                sortBy = cleanSort,
                movies = emptyList(),
                isEndReached = false,
                isLoading = false,
                errorMessage = null
            )
        }
        
        // Hủy job tìm kiếm cũ nếu người dùng thay đổi bộ lọc liên tục
        currentSearchJob?.cancel()
        currentSearchJob = viewModelScope.launch {
            performSearch()
        }
    }

    private suspend fun performSearch() {
        Log.i("SearchViewModel", "==> Bắt đầu tìm kiếm với: query=${_uiState.value.query}, genre=${_uiState.value.genre}")
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        // Gọi API lọc theo từ khóa và thể loại trực tiếp từ server
        val result = repository.getMoviesFromApi(
            page = 1,
            limit = 50, // Lấy 50 kết quả phù hợp nhất từ Server
            search = _uiState.value.query,
            genre = _uiState.value.genre
        )

        result.onSuccess { apiMovies ->
            Log.d("SearchViewModel", "API trả về ${apiMovies.size} phim")
            // Thực hiện lọc bổ sung khoảng năm (nếu có) ở phía client
            val filtered = apiMovies.filter { movie ->
                val y = _uiState.value.yearRange
                y == null || matchesYearRange(movie.releaseDate, y)
            }
            Log.d("SearchViewModel", "Sau khi lọc năm: ${filtered.size} phim")

            // Sắp xếp lại danh sách kết quả (Rating) nếu người dùng yêu cầu
            val sorted = if (_uiState.value.sortBy == "rating") {
                filtered.sortedByDescending { it.rating }
            } else {
                filtered
            }

            _uiState.update { it.copy(
                isLoading = false, 
                movies = sorted,
                isEndReached = true // Đã lọc xong danh sách phim phù hợp
            ) }
        }.onFailure { e ->
            Log.e("SearchViewModel", "Lỗi khi tìm kiếm", e)
            val friendlyError = if (e is java.net.UnknownHostException || e.message?.contains("Unable to resolve host") == true) {
                "Không có kết nối mạng, vui lòng thử lại"
            } else {
                e.localizedMessage ?: "Không có kết nối mạng, vui lòng thử lại"
            }
            _uiState.update { it.copy(
                isLoading = false, 
                errorMessage = friendlyError
            ) }
        }
    }

    private fun matchesYearRange(releaseDate: String, range: String): Boolean {
        return try {
            // range: "2021 - 2025" -> start=2021, end=2025
            val years = range.split("-").map { it.trim().toIntOrNull() }
            val start = years.getOrNull(0) ?: 0
            val end = years.getOrNull(1) ?: 9999
            
            // Lấy 4 ký tự đầu của "YYYY-MM-DD"
            val movieYear = releaseDate.take(4).toIntOrNull() ?: 0
            movieYear in start..end
        } catch (e: Exception) {
            true // Nếu lỗi định dạng thì bỏ qua lọc năm này
        }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun loadNextMovies() {
        // Không cần loadNext nữa vì performSearch đã tải và lọc toàn bộ kho phim ngay lần đầu
    }

    fun toggleFavorite(movie: MovieResponse) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.w("SearchViewModel", "Thất bại: User chưa đăng nhập")
            _uiState.update { it.copy(errorMessage = "Vui lòng đăng nhập để thích phim") }
            return
        }
        val userId = currentUser.uid
        Log.d("SearchViewModel", "Toggle favorite: ${movie.title} cho user $userId")
        viewModelScope.launch {
            val isFav = repository.isMovieFavorite(movie.id, userId)
            val entity = FavoriteMovieEntity(
                id = movie.id,
                userId = userId,
                title = movie.title,
                posterPath = movie.posterPath,
                backdropPath = movie.backdropPath,
                genre = movie.genres.joinToString(", "),
                rating = movie.rating,
                runtime = movie.runtime,
                isWatched = false
            )

            if (isFav) {
                Log.v("SearchViewModel", "Đang xóa khỏi yêu thích...")
                repository.removeFromFavorite(entity)
            } else {
                Log.v("SearchViewModel", "Đang thêm vào yêu thích...")
                repository.addToFavorite(entity)
            }
        }
    }
}
