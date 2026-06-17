package com.example.flickfind_ltttbdd.ui.screens

import android.util.Log
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.flickfind_ltttbdd.navigation.Screen
import com.example.flickfind_ltttbdd.ui.viewmodel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultScreen(
    query: String?,
    genre: String?,
    yearRange: String?,
    sortBy: String?,
    viewModel: SearchViewModel,
    navController: NavController
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Kiểm tra hướng màn hình để quyết định số cột (Dọc: 1 cột, Ngang: 2 cột)
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val columns = if (isLandscape) 2 else 1

    // Gọi API lọc mới mỗi khi tham số đầu vào thay đổi
    LaunchedEffect(query, genre, yearRange, sortBy) {
        Log.i("SearchResultScreen", "==> Khởi chạy SearchResult với: query=$query, genre=$genre, year=$yearRange, sort=$sortBy")
        viewModel.setFiltersAndSearch(query, genre, yearRange, sortBy)
    }

    val movies = uiState.movies

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Kết quả tìm kiếm", color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp)
                        val filterText = listOfNotNull(
                            query?.takeIf { it.isNotBlank() }?.let { "Từ khóa: $it" },
                            genre?.takeIf { it.isNotBlank() }?.let { "Thể loại: $it" },
                            yearRange?.takeIf { it.isNotBlank() }?.let { "Năm: $it" }
                        ).joinToString(" | ")
                        if (filterText.isNotBlank()) {
                            Text(filterText, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (uiState.errorMessage != null && movies.isEmpty() && !uiState.isLoading) {
                // Hiển thị lỗi mạng ở giữa màn hình nếu không có dữ liệu
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = uiState.errorMessage!!,
                        color = Color.Red,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.setFiltersAndSearch(query, genre, yearRange, sortBy) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38B6FF))
                    ) {
                        Text("Thử lại", color = Color.White)
                    }
                }
            } else if (movies.isEmpty() && !uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Không tìm thấy phim nào phù hợp", color = Color.Gray)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(movies) { movie ->
                        MovieHorizontalRowItem(
                            movie = movie,
                            isFavorite = uiState.favoriteMovieIds.contains(movie.id),
                            onFavoriteClick = {
                                Log.d("SearchResultScreen", "Click Yêu thích: ${movie.title}")
                                if (FirebaseAuth.getInstance().currentUser != null) {
                                    viewModel.toggleFavorite(movie)
                                } else {
                                    Toast.makeText(context, "Vui lòng đăng nhập để thích phim", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onCardClick = {
                                Log.d("SearchResultScreen", "Mở chi tiết từ kết quả tìm kiếm: ${movie.title}")
                                navController.navigate(Screen.Detail.createRoute(movie.id))
                            }
                        )
                    }

                    if (uiState.isLoading) {
                        item(span = { GridItemSpan(columns) }) {
                            Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}
