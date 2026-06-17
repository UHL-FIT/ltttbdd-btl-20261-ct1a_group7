package com.example.flickfind_ltttbdd

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.flickfind_ltttbdd.navigation.MainNavGraph
import com.example.flickfind_ltttbdd.ui.theme.FlickFindLTTTBDDTheme
import com.example.flickfind_ltttbdd.ui.viewmodel.AppViewModelProvider
import com.example.flickfind_ltttbdd.ui.viewmodel.ThemeViewModel
import android.view.WindowManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "onCreate")

        // 1. Kích hoạt Edge-to-Edge
        enableEdgeToEdge()

        // 2. Cho phép tràn vào vùng tai thỏ khi xoay ngang
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        setContent {
            val context = LocalContext.current
            val themeViewModel: ThemeViewModel = viewModel(
                factory = AppViewModelProvider(context)
            )
            val isDarkModeState by themeViewModel.isDarkMode.collectAsState()

            // Nếu chưa có giá trị trong DataStore (null), sử dụng theme của hệ thống
            val isDarkTheme = isDarkModeState ?: androidx.compose.foundation.isSystemInDarkTheme()

            FlickFindLTTTBDDTheme(darkTheme = isDarkTheme) {
                MainNavGraph(
                    isDarkTheme = isDarkTheme,
                    onThemeToggle = { checked: Boolean -> themeViewModel.onThemeChanged(checked) }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d("MainActivity", "onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.d("MainActivity", "onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.d("MainActivity", "onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d("MainActivity", "onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MainActivity", "onDestroy")
    }
}