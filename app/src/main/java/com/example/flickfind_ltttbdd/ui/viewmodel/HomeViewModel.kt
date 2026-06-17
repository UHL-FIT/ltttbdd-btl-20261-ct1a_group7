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

// 1. Định nghĩa trạng thái giao diện (UI State) theo chuẩn UDF
data class HomeUiState(
    val movies: List<MovieResponse> = emptyList(), // Danh sách phim cho trang chủ (phân trang)
    val popularMovies: List<MovieResponse> = emptyList(), // Danh sách 10 phim phổ biến (điểm cao)
    val favoriteMovieIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val currentPage: Int = 1,
    val isEndReached: Boolean = false,
    val searchSuggestions: List<MovieResponse> = emptyList() // Danh sách gợi ý tìm kiếm
)

class HomeViewModel(
    private val repository: MovieRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val pageLimit = 10
    private var searchJob: kotlinx.coroutines.Job? = null
    private var favoritesJob: kotlinx.coroutines.Job? = null

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
        Log.d("HomeViewModel", "==> Khởi tạo HomeViewModel")
        loadNextMovies()
        FirebaseAuth.getInstance().addAuthStateListener(authListener)
        fetchPopularMovies()
    }

    override fun onCleared() {
        super.onCleared()
        FirebaseAuth.getInstance().removeAuthStateListener(authListener)
    }

    private fun fetchPopularMovies() {
        Log.d("HomeViewModel", "Đang tải danh sách phim phổ biến...")
        viewModelScope.launch {
            // Tải 20 phim để lọc ra 10 phim rating cao nhất làm "Phổ biến"
            // Giúp khởi động app nhanh hơn nhiều so với việc tải 100 phim
            repository.getMoviesFromApi(page = 1, limit = 20).onSuccess { movies ->
                Log.i("HomeViewModel", "Tải thành công ${movies.size} phim để lọc phổ biến")
                val popular = movies.sortedByDescending { it.rating }.take(10)
                _uiState.update { it.copy(popularMovies = popular) }
            }.onFailure { e ->
                Log.e("HomeViewModel", "Lỗi khi tải phim phổ biến", e)
            }
        }
    }

    fun updateSearchSuggestions(query: String) {
        val cleanQuery = query.trim()
        if (cleanQuery.length < 2) {
            _uiState.update { it.copy(searchSuggestions = emptyList()) }
            return
        }
        
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            // Tìm kiếm trực tiếp từ API thay vì lọc từ danh sách tải sẵn
            repository.getMoviesFromApi(page = 1, limit = 5, search = cleanQuery).onSuccess { results ->
                _uiState.update { it.copy(searchSuggestions = results) }
            }
        }
    }

    fun clearSuggestions() {
        _uiState.update { it.copy(searchSuggestions = emptyList()) }
    }

    // Làm mới toàn bộ danh sách
    fun refreshMovies() {
        _uiState.update { it.copy(
            currentPage = 1,
            movies = emptyList(),
            isEndReached = false,
            isLoading = true,
            errorMessage = null
        ) }
        loadNextMovies()
        fetchPopularMovies()
    }

    // 2. Logic Phân trang (Pagination) thủ công cực kỳ trực quan
    fun loadNextMovies() {
        // Nếu đang tải hoặc đã hết phim thì không gọi API nữa
        if (_uiState.value.isLoading && _uiState.value.currentPage > 1 || _uiState.value.isEndReached) return

        Log.d("HomeViewModel", "Đang tải trang ${_uiState.value.currentPage}...")
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val result = repository.getMoviesFromApi(
                page = _uiState.value.currentPage,
                limit = pageLimit
            )

            result.onSuccess { newMovies ->
                Log.i("HomeViewModel", "Tải thành công trang ${_uiState.value.currentPage} với ${newMovies.size} phim")
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        movies = if (currentState.currentPage == 1) newMovies else currentState.movies + newMovies,
                        currentPage = currentState.currentPage + 1,
                        isEndReached = newMovies.size < pageLimit
                    )
                }
            }.onFailure { exception ->
                Log.e("HomeViewModel", "Lỗi khi tải trang ${_uiState.value.currentPage}", exception)
                val friendlyError = if (exception is java.net.UnknownHostException || exception.message?.contains("Unable to resolve host") == true) {
                    "Không có kết nối mạng, vui lòng thử lại"
                } else {
                    exception.localizedMessage ?: "Lỗi kết nối API"
                }
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = friendlyError)
                }
            }
        }
    }

    // 3. Theo dõi danh sách phim đã lưu trong Room DB theo userId
    private fun observeFavorites(userId: String) {
        favoritesJob = viewModelScope.launch {
            repository.getAllFavorites(userId).collect { favoriteEntities ->
                _uiState.update { currentState ->
                    // Chuyển danh sách thực thể thành một bộ Set<Int> chứa ID để tìm kiếm siêu nhanh (O(1))
                    currentState.copy(favoriteMovieIds = favoriteEntities.map { it.id }.toSet())
                }
            }
        }
    }

    // 4. Tính năng Click vào nút "Thích" (CRUD - Thêm/Xóa khỏi Room DB trực tiếp từ danh sách)
    fun toggleFavorite(movie: MovieResponse) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.w("HomeViewModel", "Thất bại: User chưa đăng nhập nhưng bấm nút yêu thích")
            _uiState.update { it.copy(errorMessage = "Vui lòng đăng nhập để thích phim") }
            return
        }
        val userId = currentUser.uid
        Log.d("HomeViewModel", "Toggle favorite cho phim: ${movie.title} (User: $userId)")
        viewModelScope.launch {
            val isFav = repository.isMovieFavorite(movie.id, userId)
            // Chuyển đổi dữ liệu từ dạng API Response sang thực thể Room DB
            val entity = FavoriteMovieEntity(
                id = movie.id,
                userId = userId,
                title = movie.title,
                posterPath = movie.posterPath,
                backdropPath = movie.backdropPath,
                genre = movie.genres.joinToString(", "), // Gộp mảng List<String> thành một chuỗi String đơn
                rating = movie.rating,
                runtime = movie.runtime,
                isWatched = false // Mặc định khi ấn thích từ màn chính là chưa xem
            )

            if (isFav) {
                Log.v("HomeViewModel", "Đang xóa khỏi yêu thích...")
                repository.removeFromFavorite(entity)
            } else {
                Log.v("HomeViewModel", "Đang thêm vào yêu thích...")
                repository.addToFavorite(entity)
            }
        }
    }
}
