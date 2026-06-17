package com.example.flickfind_ltttbdd.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


object RetrofitClient {
    // Đường dẫn gốc của API (bỏ phần "api/v1/movies" đi vì đã khai báo ở @GET)
    private const val BASE_URL = "https://6a08bf5ae7e3f433d482ce25.mockapi.io/"

    val instance: MovieApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create()) // Tự động chuyển JSON thành MovieResponse nhờ thư viện Gson
            .build()
            .create(MovieApiService::class.java)
    }
}
