package com.example.flickfind_ltttbdd.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_movies", primaryKeys = ["id", "userId"])
data class FavoriteMovieEntity(
    val id: String,
    val userId: String,
    val title: String,
    val posterPath: String,
    val backdropPath: String,
    val genre: String,
    val rating: Float,
    val runtime: Int,
    val isWatched: Boolean = false
)