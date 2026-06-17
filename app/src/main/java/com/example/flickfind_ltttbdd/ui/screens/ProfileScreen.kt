package com.example.flickfind_ltttbdd.ui.screens

import android.util.Log
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.flickfind_ltttbdd.data.local.FavoriteMovieEntity
import com.example.flickfind_ltttbdd.ui.viewmodel.ProfileViewModel
import com.example.flickfind_ltttbdd.navigation.Screen
import com.example.flickfind_ltttbdd.data.local.UserEntity
import com.google.firebase.auth.FirebaseAuth

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    navController: NavController,
    onLogout: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var isEditMode by remember { mutableStateOf(false) }
    var showNameDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        Log.d("ProfileScreen", "==> Hiển thị màn hình Cá nhân")
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let { 
                // Cấp quyền truy cập lâu dài cho URI này để ảnh không bị mất khi khởi động lại app
                try {
                    context.contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {}
                viewModel.updateAvatar(it.toString()) 
            }
        }
    )

    if (showNameDialog) {
        NameEditDialog(
            currentName = uiState.user?.name ?: "",
            onDismiss = { showNameDialog = false },
            onConfirm = { newName ->
                viewModel.updateName(newName)
                showNameDialog = false
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header: Thông tin tài khoản
        item {
            ProfileHeader(
                name = uiState.user?.name ?: "Người dùng",
                avatarUrl = uiState.user?.avatarUrl ?: "",
                onLogout = onLogout,
                onEditAvatar = {
                    Log.d("ProfileScreen", "Người dùng chọn đổi ảnh đại diện")
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onEditName = { 
                    Log.d("ProfileScreen", "Người dùng chọn đổi tên")
                    showNameDialog = true 
                }
            )
        }

        // 2. Dòng chữ tiêu đề
        item {
            Text(
                text = "Bảng thống kê thể loại phim đã xem",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                textAlign = TextAlign.Center
            )
        }

        // 3. Statistics: Biểu đồ cột ngang
        item {
            GenreBarChartSection(
                genreDistribution = uiState.genreDistribution,
                totalTime = uiState.totalWatchTime
            )
        }

        // 4. Title Section for Favorites with Edit Toggle
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Danh sách yêu thích",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    onClick = { 
                        isEditMode = !isEditMode 
                        Log.d("ProfileScreen", "Chế độ chỉnh sửa: $isEditMode")
                    },
                    color = if (isEditMode) Color(0xFFE91E63) else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isEditMode) "Xong" else "Sửa",
                            color = if (isEditMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (isEditMode) Icons.Default.Done else Icons.Default.Edit,
                            contentDescription = null,
                            tint = if (isEditMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        // 5. Horizontal Favorite Movie List
        items(uiState.favoriteMovies) { movie ->
            HorizontalFavoriteMovieItem(
                movie = movie,
                isEditMode = isEditMode,
                onToggleWatched = { 
                    Log.d("ProfileScreen", "Bấm toggle 'Đã xem' cho: ${movie.title}")
                    viewModel.toggleWatched(movie) 
                },
                onDelete = { 
                    Log.d("ProfileScreen", "Bấm xóa khỏi yêu thích: ${movie.title}")
                    viewModel.deleteFavorite(movie) 
                },
                onClick = {
                    if (!isEditMode) {
                        Log.d("ProfileScreen", "Mở chi tiết phim từ Profile: ${movie.title}")
                        navController.navigate(Screen.Detail.createRoute(movie.id))
                    }
                }
            )
        }
    }
}

@Composable
fun HorizontalFavoriteMovieItem(
    movie: FavoriteMovieEntity,
    isEditMode: Boolean,
    onToggleWatched: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Ảnh phim bên trái
            val imageUrl = if (movie.posterPath.startsWith("/")) {
                "https://image.tmdb.org/t/p/w500${movie.posterPath}"
            } else {
                movie.posterPath
            }

            AsyncImage(
                model = imageUrl,
                contentDescription = movie.title,
                modifier = Modifier
                    .size(width = 80.dp, height = 110.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 2. Thông tin phim ở giữa
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = movie.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = movie.genre,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(Icons.Default.Star, null, tint = Color.Yellow, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = movie.rating.toString(), color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = "${movie.runtime}p", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }

                // Chế độ chỉnh sửa trạng thái 1 trong 2
                if (isEditMode) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatusChoiceChip(
                            text = "Đã xem",
                            isSelected = movie.isWatched,
                            onClick = { if (!movie.isWatched) onToggleWatched() }
                        )
                        StatusChoiceChip(
                            text = "Chưa xem",
                            isSelected = !movie.isWatched,
                            onClick = { if (movie.isWatched) onToggleWatched() }
                        )
                    }
                }
            }

            // 3. Góc phải dưới: Trạng thái hoặc Nút xóa
            Box(
                modifier = Modifier.fillMaxHeight(),
                contentAlignment = Alignment.BottomEnd
            ) {
                if (isEditMode) {
                    // Nút xóa màu đỏ (ở trên cùng bên phải khi sửa)
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.align(Alignment.TopEnd).size(28.dp)
                    ) {
                        Icon(Icons.Default.Close, null, tint = Color.Red, modifier = Modifier.size(20.dp))
                    }
                } else {
                    // Trạng thái hiển thị ở góc phải dưới khi xem bình thường
                    Surface(
                        color = if (movie.isWatched) Color(0xFF00BFA5) else Color(0xFFE91E63),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(top = 60.dp)
                    ) {
                        Text(
                            text = if (movie.isWatched) "Đã xem" else "Chưa xem",
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatusChoiceChip(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, 
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Text(
            text = text,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 9.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun GenreBarChartSection(genreDistribution: Map<String, Int>, totalTime: Int) {
    LaunchedEffect(genreDistribution) {
        if (genreDistribution.isNotEmpty()) {
            Log.d("ProfileScreen", "Thống kê biểu đồ: Nhận được ${genreDistribution.size} thể loại. Tổng thời gian: $totalTime phút")
        }
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Phân bổ thể loại", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(text = "Tổng: $totalTime phút", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (genreDistribution.isEmpty()) {
                Text("Chưa có dữ liệu", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            } else {
                val maxCount = genreDistribution.values.maxOrNull()?.toFloat() ?: 1f
                genreDistribution.entries.sortedByDescending { it.value }.take(4).forEach { (genre, count) ->
                    GenreBarItem(genre, count, count / maxCount)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun GenreBarItem(genre: String, count: Int, ratio: Float) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = genre, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
            Text(text = "$count phim", color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))) {
            Box(modifier = Modifier.fillMaxWidth(ratio).fillMaxHeight().clip(CircleShape).background(MaterialTheme.colorScheme.primary))
        }
    }
}

@Composable
fun ProfileHeader(name: String, avatarUrl: String, onLogout: () -> Unit, onEditAvatar: () -> Unit, onEditName: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { onEditAvatar() }, 
                contentAlignment = Alignment.Center
            ) {
                if (avatarUrl.isNotEmpty()) {
                    AsyncImage(model = avatarUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = name, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Sửa tên",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp).clickable { onEditName() }
                    )
                }
                Text(text = "ID: ${FirebaseAuth.getInstance().currentUser?.uid?.take(8)}...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
            }
            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("Đăng xuất", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun NameEditDialog(currentName: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Đổi tên tài khoản", color = MaterialTheme.colorScheme.onSurface) },
        containerColor = MaterialTheme.colorScheme.surface,
        text = {
            Column {
                Text("Nhập tên mới:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) {
                Text("Cập nhật", color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text("Hủy", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

