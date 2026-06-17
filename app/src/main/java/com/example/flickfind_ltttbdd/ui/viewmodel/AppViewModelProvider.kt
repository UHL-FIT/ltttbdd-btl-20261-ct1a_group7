package com.example.flickfind_ltttbdd.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.flickfind_ltttbdd.data.MovieRepository
import com.example.flickfind_ltttbdd.data.datastore.ThemePreferencesRepository
import com.example.flickfind_ltttbdd.data.local.AppDatabase
import com.example.flickfind_ltttbdd.data.remote.RetrofitClient
import kotlin.jvm.java

class AppViewModelProvider(private val context: Context) : ViewModelProvider.Factory {

    companion object {
        @Volatile
        private var repository: MovieRepository? = null
        @Volatile
        private var themeRepository: ThemePreferencesRepository? = null

        fun getRepository(context: Context): MovieRepository {
            return repository ?: synchronized(this) {
                val database = AppDatabase.getDatabase(context)
                val repo = MovieRepository(
                    apiService = RetrofitClient.instance,
                    movieDao = database.movieDao(),
                    userDao = database.userDao()
                )

                repository = repo
                repo
            }
        }

        fun getThemeRepository(context: Context): ThemePreferencesRepository {
            return themeRepository ?: synchronized(this) {
                val repo = ThemePreferencesRepository(context)
                themeRepository = repo
                repo
            }
        }
    }

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repo = getRepository(context)
        val themeRepo = getThemeRepository(context)

        if (modelClass.isAssignableFrom(ThemeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ThemeViewModel(themeRepo) as T
        }

        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repo) as T
        }

        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SearchViewModel(repo) as T
        }

        if(modelClass.isAssignableFrom(DetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DetailViewModel(repo) as T
        }

        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(repo) as T
        }

        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(repo) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
