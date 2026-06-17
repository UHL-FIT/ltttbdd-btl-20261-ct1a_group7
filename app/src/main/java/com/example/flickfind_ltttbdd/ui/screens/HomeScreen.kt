package com.example.flickfind_ltttbdd.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.ui.platform.LocalContext
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import coil.compose.AsyncImage
import com.example.flickfind_ltttbdd.R
import com.example.flickfind_ltttbdd.data.remote.MovieResponse
import com.example.flickfind_ltttbdd.navigation.Screen
import com.example.flickfind_ltttbdd.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    navController: NavController,
    isDarkTheme: Boolean
) {
    // Lắng nghe trạng thái UI State từ ViewModel phát ra
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var isSuggestionsVisible by remember { mutableStateOf(false) }

    // Kiểm tra hướng màn hình để quyết định số cột (Dọc: 1 cột, Ngang: 2 cột)
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val columns = if (isLandscape) 2 else 1

    // Xử lý Debounce tìm kiếm: Chỉ tìm khi nhập > 2 ký tự và dừng gõ 1 giây (1000ms)
    LaunchedEffect(searchQuery) {
        val cleanQuery = searchQuery.trim()
        if (cleanQuery.length >= 2) {
            Log.v("HomeScreen", "Đang đợi người dùng dừng gõ để gợi ý cho: $cleanQuery")
            kotlinx.coroutines.delay(1000)
            viewModel.updateSearchSuggestions(cleanQuery)
            isSuggestionsVisible = true
        } else {
            viewModel.clearSuggestions()
            isSuggestionsVisible = false
        }
    }

    val pullToRefreshState = rememberPullToRefreshState()
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        Log.d("HomeScreen", "==> Khởi chạy HomeScreen")
    }

    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading) {
            isRefreshing = false
        }
    }

    // Giao diện tổng thể
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                viewModel.refreshMovies()
            },
            state = pullToRefreshState,
            modifier = Modifier.fillMaxSize()
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. KHỐI LOGO & CHỮ (Căn giữa - Đồng bộ thương hiệu)
                item(span = { GridItemSpan(columns) }) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(id = if (isDarkTheme) R.drawable.logo_v1 else R.drawable.logo_v3),
                            contentDescription = "Logo FlickFind",
                            modifier = Modifier
                                .width(280.dp)
                                .height(100.dp),
                            contentScale = ContentScale.Fit
                        )

                    }
                }

                // 2. THANH TÌM KIẾM (Search Bar) - Chiếm hết số cột
                item(span = { GridItemSpan(columns) }) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                if (it.length <= 3) {
                                    isSuggestionsVisible = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Thanh tìm kiếm...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                            leadingIcon = {
                                IconButton(onClick = {
                                    val cleanQuery = searchQuery.trim()
                                    Log.i("HomeScreen", "Người dùng nhấn icon tìm kiếm cho: $cleanQuery")
                                    if (cleanQuery.isNotEmpty()) {
                                        navController.navigate(Screen.SearchResult.createRoute(query = cleanQuery))
                                    }
                                }) {
                                    Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            trailingIcon = {
                                IconButton(onClick = { navController.navigate(Screen.Filter.route) }) {
                                    Icon(
                                        imageVector = Icons.Default.FilterList,
                                        contentDescription = "Filter",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    val cleanQuery = searchQuery.trim()
                                    if (cleanQuery.isNotBlank()) {
                                        navController.navigate(Screen.SearchResult.createRoute(query = cleanQuery))
                                        isSuggestionsVisible = false
                                    }
                                }
                            )
                        )

                        // Hiển thị danh sách gợi ý
                        if (isSuggestionsVisible && uiState.searchSuggestions.isNotEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(8.dp),
                                elevation = CardDefaults.cardElevation(8.dp)
                            ) {
                                Column {
                                    uiState.searchSuggestions.forEach { movie ->
                                        Text(
                                            text = movie.title,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    navController.navigate(Screen.Detail.createRoute(movie.id))
                                                    isSuggestionsVisible = false
                                                }
                                                .padding(12.dp),
                                            fontSize = 14.sp
                                        )
                                        HorizontalDivider(color = MaterialTheme.colorScheme.background, thickness = 1.dp)
                                    }

                                    Button(
                                        onClick = {
                                            val cleanQuery = searchQuery.trim()
                                            if (cleanQuery.isNotBlank()) {
                                                navController.navigate(Screen.SearchResult.createRoute(query = cleanQuery))
                                                isSuggestionsVisible = false
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(40.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
                                    ) {
                                        Text("Xem tất cả kết quả", color = MaterialTheme.colorScheme.onPrimary, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                val allMovies = uiState.movies

                // 3. MỤC PHIM PHỔ BIẾN
                if (allMovies.isNotEmpty()) {
                    item(span = { GridItemSpan(columns) }) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Phim Phổ Biến",
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "xem thêm...",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 14.sp,
                                    modifier = Modifier.clickable {
                                        navController.navigate(Screen.SearchResult.createRoute(sortBy = "rating"))
                                    }
                                )
                            }

                            val popularMovies = uiState.popularMovies
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(popularMovies) { movie ->
                                    MovieItemCard(
                                        movie = movie,
                                        isFavorite = uiState.favoriteMovieIds.contains(movie.id),
                                        onFavoriteClick = {
                                            Log.d("HomeScreen", "Click Yêu thích (Phổ biến): ${movie.title}")
                                            if (FirebaseAuth.getInstance().currentUser != null) {
                                                viewModel.toggleFavorite(movie)
                                            } else {
                                                Toast.makeText(context, "Vui lòng đăng nhập để thích phim", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        onCardClick = {
                                            Log.d("HomeScreen", "Mở chi tiết (Phổ biến): ${movie.title}")
                                            navController.navigate(Screen.Detail.createRoute(movie.id))
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

            // 4. MỤC PHIM HOT
            if (allMovies.isNotEmpty()) {
                item(span = { GridItemSpan(columns) }) {
                    Text(
                        text = "Phim Hot",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                    itemsIndexed(allMovies) { index, movie ->
                        if (index >= allMovies.lastIndex - 2 && !uiState.isLoading && !uiState.isEndReached) {
                            LaunchedEffect(key1 = allMovies.size) {
                                viewModel.loadNextMovies()
                            }
                        }

                        Box(modifier = Modifier.padding(vertical = 4.dp)) {
                            MovieHorizontalRowItem(
                                movie = movie,
                                isFavorite = uiState.favoriteMovieIds.contains(movie.id),
                                onFavoriteClick = {
                                    Log.d("HomeScreen", "Click Yêu thích (Hot): ${movie.title}")
                                    if (FirebaseAuth.getInstance().currentUser != null) {
                                        viewModel.toggleFavorite(movie)
                                    } else {
                                        Toast.makeText(context, "Vui lòng đăng nhập để thích phim", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onCardClick = {
                                    Log.d("HomeScreen", "Mở chi tiết (Hot): ${movie.title}")
                                    navController.navigate(Screen.Detail.createRoute(movie.id))
                                }
                            )
                        }
                    }
                }

            // 5. TRẠNG THÁI LOADING - Chiếm hết số cột
            if (uiState.isLoading) {
                item(span = { GridItemSpan(columns) }) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

                // 6. THÔNG BÁO LỖI
                uiState.errorMessage?.let { error ->
                    item(span = { GridItemSpan(columns) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// COMPONENTS 1: Thẻ Phim Cuộn Ngang (Phim Phổ Biến)
@Composable
fun MovieItemCard(
    movie: MovieResponse,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onCardClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .clickable { onCardClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column {
            Box(modifier = Modifier.height(180.dp).fillMaxWidth()) {
                // Thư viện Coil tự tải ảnh từ URL cực kỳ mượt mà
                AsyncImage(
                    model = movie.posterPath,
                    contentDescription = movie.title,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
                    contentScale = ContentScale.Crop
                )
                // Nút trái tim yêu thích bọc trên góc ảnh
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) Color.Red else Color.White
                    )
                }
            }
            // Khối nội dung chữ bên dưới ảnh giống y hệt Wireframe
            Column(modifier = Modifier.padding(8.dp)) {
                Text(text = movie.title, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = movie.genres.joinToString(", "), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = "★ ${movie.rating}", color = Color(0xFFFFC107), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// COMPONENTS 2: Dòng Phim Cuộn Dọc (Phim Hot)
@Composable
fun MovieHorizontalRowItem(
    movie: MovieResponse,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onCardClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable { onCardClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = movie.posterPath,
                contentDescription = movie.title,
                modifier = Modifier.width(80.dp).fillMaxHeight(),
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(12.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = movie.title, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = movie.genres.joinToString(", "), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = "★ ${movie.rating}", color = Color(0xFFFFC107), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
                IconButton(onClick = onFavoriteClick) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) Color.Red else Color.White
                    )
                }
            }
        }
    }
}