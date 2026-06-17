package com.example.flickfind_ltttbdd.navigation

import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

// Định nghĩa các màn hình trong ứng dụng
sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {

    // 3 Màn hình chính xuất hiện trên Bottom Navigation Bar
    object Home : Screen("home", "Khám phá", Icons.Default.Home)
    object Profile : Screen("profile", "Cá nhân", Icons.Default.Person)
    object Settings : Screen("settings", "Cài đặt", Icons.Default.Settings)
    object Login : Screen(
        "login", "Đăng nhập")
    object Register : Screen("register", "Đăng ký")

    // Màn hình phụ không xuất hiện trên Bottom Bar (nên không cần truyền Icon)
    object DeveloperInfo : Screen("developer_info", "Thông tin nhà phát triển")
    // Màn hình phụ không xuất hiện trên Bottom Bar
    object Filter : Screen("filter", "Lọc phim")
    object SearchResult : Screen("search_result?query={query}&genre={genre}&yearRange={yearRange}&sortBy={sortBy}", "Kết quả tìm kiếm") {
        val arguments = listOf(
            navArgument("query") { type = NavType.StringType; nullable = true; defaultValue = null },
            navArgument("genre") { type = NavType.StringType; nullable = true; defaultValue = null },
            navArgument("yearRange") { type = NavType.StringType; nullable = true; defaultValue = null },
            navArgument("sortBy") { type = NavType.StringType; nullable = true; defaultValue = null }
        )
        fun createRoute(query: String? = null, genre: String? = null, yearRange: String? = null, sortBy: String? = null): String {
            return "search_result?query=${query ?: ""}&genre=${genre ?: ""}&yearRange=${yearRange ?: ""}&sortBy=${sortBy ?: ""}"
        }
    }
    object Detail : Screen("detail/{movieId}", "Chi tiết") {
        fun createRoute(movieId: String) = "detail/$movieId"
    }
}

// Danh sách các mục sẽ xuất hiện dưới Bottom Navigation Bar
val bottomNavItems = listOf(
    Screen.Home,
    Screen.Profile,
    Screen.Settings
)