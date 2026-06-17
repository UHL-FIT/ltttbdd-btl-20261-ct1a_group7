package com.example.flickfind_ltttbdd.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.flickfind_ltttbdd.ui.screens.AboutScreen
import com.example.flickfind_ltttbdd.ui.screens.DetailScreen
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.flickfind_ltttbdd.ui.viewmodel.AppViewModelProvider
import com.example.flickfind_ltttbdd.ui.viewmodel.HomeViewModel
import com.example.flickfind_ltttbdd.ui.viewmodel.SearchViewModel
import com.example.flickfind_ltttbdd.ui.screens.HomeScreen
import com.example.flickfind_ltttbdd.ui.screens.FilterScreen
import com.example.flickfind_ltttbdd.ui.screens.SearchResultScreen
import com.example.flickfind_ltttbdd.ui.screens.ProfileScreen
import com.example.flickfind_ltttbdd.ui.viewmodel.ProfileViewModel
import com.example.flickfind_ltttbdd.ui.screens.DeveloperInfoScreen
import com.example.flickfind_ltttbdd.ui.viewmodel.DetailViewModel
import com.example.flickfind_ltttbdd.ui.viewmodel.AuthViewModel
import com.example.flickfind_ltttbdd.ui.screens.*

@Composable
fun MainNavGraph(
    isDarkTheme: Boolean,
    onThemeToggle: (Boolean) -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val context = LocalContext.current
    val authViewModel: AuthViewModel = viewModel(factory = AppViewModelProvider(context))
    val authUiState by authViewModel.uiState.collectAsState()

    // Chỉ hiển thị Bottom Bar ở 3 màn hình chính
    val rootScreens = listOf(
        Screen.Home.route,
        Screen.Profile.route,
        Screen.Settings.route
    )
    val showBottomBar = currentRoute in rootScreens || currentRoute == Screen.Register.route

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                AppBottomNavigationBar(navController)
            }
        }
    ) { innerPadding ->
        val context = LocalContext.current

        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // 0. Màn hình Auth
            composable(Screen.Login.route) {
                LoginScreen(
                    viewModel = authViewModel,
                    isDarkTheme = isDarkTheme,
                    onNavigateToRegister = { navController.navigate(Screen.Register.route) }
                )
            }

            composable(Screen.Register.route) {
                RegisterScreen(
                    viewModel = authViewModel,
                    isDarkTheme = isDarkTheme,
                    onNavigateToLogin = { 
                        // Quay lại màn hình trước đó (Tab Cá nhân)
                        navController.popBackStack() 
                    }
                )
            }

            // 1. Màn hình Trang chủ (Công khai)
            composable(Screen.Home.route) {
                val homeViewModel: HomeViewModel = viewModel(
                    factory = AppViewModelProvider(context)
                )
                HomeScreen(
                    viewModel = homeViewModel, 
                    navController = navController,
                    isDarkTheme = isDarkTheme
                )
            }

            // 2. Màn hình Cá nhân (Yêu cầu đăng nhập)
            composable(Screen.Filter.route) {
                FilterScreen(
                    navController = navController,
                    onApplyFilters = { genre, yearRange ->
                        navController.navigate(Screen.SearchResult.createRoute(genre = genre, yearRange = yearRange))
                    }
                )
            }

            composable(
                route = Screen.SearchResult.route,
                arguments = Screen.SearchResult.arguments
            ) { backStackEntry ->
                val query = backStackEntry.arguments?.getString("query")
                val genre = backStackEntry.arguments?.getString("genre")
                val yearRange = backStackEntry.arguments?.getString("yearRange")
                val sortBy = backStackEntry.arguments?.getString("sortBy")

                val searchViewModel: SearchViewModel = viewModel(
                    factory = AppViewModelProvider(context)
                )

                SearchResultScreen(
                    query = query,
                    genre = genre,
                    yearRange = yearRange,
                    sortBy = sortBy,
                    viewModel = searchViewModel,
                    navController = navController,
                )
            }

            // Màn hình Chi tiết phim
            composable(
                route = Screen.Detail.route,
                arguments = listOf(navArgument("movieId") { type = NavType.StringType })
            ) { backStackEntry ->
                val movieId = backStackEntry.arguments?.getString("movieId")
                val context = LocalContext.current
                val detailViewModel: DetailViewModel = viewModel(factory = AppViewModelProvider(context))

                if (!movieId.isNullOrEmpty()) {
                    DetailScreen(
                        movieId = movieId,
                        viewModel = detailViewModel,
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }

            composable(Screen.Profile.route) {
                if (authUiState.isLoggedIn) {
                    // Nếu đã đăng nhập -> Hiện trang cá nhân
                    val profileViewModel: ProfileViewModel = viewModel(
                        factory = AppViewModelProvider(context)
                    )
                    ProfileScreen(
                        viewModel = profileViewModel,
                        navController = navController,
                        onLogout = {
                            authViewModel.logout()
                        }
                    )
                } else {
                    // Nếu chưa đăng nhập -> Hiện trang đăng nhập ngay tại tab Cá nhân
                    LoginScreen(
                        viewModel = authViewModel,
                        isDarkTheme = isDarkTheme,
                        onNavigateToRegister = { navController.navigate(Screen.Register.route) }
                    )
                }
            }

            // Màn hình 3: Cài đặt
            composable(Screen.Settings.route) {
                AboutScreen(
                    isDarkTheme = isDarkTheme,
                    onThemeToggle = onThemeToggle,
                    onNavigateToDeveloperInfo = {
                        navController.navigate(Screen.DeveloperInfo.route)
                    }
                )
            }

            composable(Screen.DeveloperInfo.route) {
                DeveloperInfoScreen(
                    isDarkTheme = isDarkTheme,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun AppBottomNavigationBar(navController: NavHostController) {
    val navigationItems = listOf(
        Screen.Home,
        Screen.Profile,
        Screen.Settings
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        // Lấy trạng thái màn hình hiện tại để check làm sáng nút
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        navigationItems.forEach { screen ->
            // ĐIỀU KIỆN LÀM SÁNG: Nếu route trùng khớp thì mục đó sẽ sáng lên
            val isSelected = currentRoute == screen.route

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (currentRoute != screen.route) {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                label = {
                    Text(
                        text = screen.title,
                        color = if (isSelected) MaterialTheme.colorScheme.primary 
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                icon = {
                    Icon(
                        imageVector = screen.icon ?: Icons.Default.Home,
                        contentDescription = screen.title,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary 
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                )
            )
        }
    }
}