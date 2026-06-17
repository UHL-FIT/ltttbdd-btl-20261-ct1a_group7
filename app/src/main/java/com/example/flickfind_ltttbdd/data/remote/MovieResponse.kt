package com.example.flickfind_ltttbdd.data.remote

import com.google.gson.annotations.SerializedName


data class MovieResponse(
    @SerializedName("id") val id: String, // Chuyển thành String theo yêu cầu
    @SerializedName("title") val title: String,
    @SerializedName("poster_path") val posterPath: String,   // Khớp với JSON
    @SerializedName("backdrop_path") val backdropPath: String, // Khớp với JSON
    @SerializedName("genres") val genres: List<String>,      // Chuyển thành List<String>
    @SerializedName("rating") val rating: Float,
    @SerializedName("runtime") val runtime: Int,             // Thời gian phim (phút)
    @SerializedName("director") val director: String,
    @SerializedName("cast") val cast: String,
    @SerializedName("release_date") val releaseDate: String,
    @SerializedName("overview") val overview: String
    // Phần comments tạm thời bỏ qua nếu ở danh sách chưa cần dùng đến,
    // hoặc bạn có thể giữ cấu trúc phẳng này để tối ưu.
)