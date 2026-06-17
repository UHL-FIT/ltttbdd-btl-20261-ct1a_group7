package com.example.flickfind_ltttbdd.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [FavoriteMovieEntity::class, UserEntity::class],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    // Khai báo các đường dẫn để lấy DAO
    abstract fun movieDao(): MovieDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Hàm khởi tạo Database dạng Singleton (đảm bảo toàn app chỉ có 1 thực thể DB độc nhất)
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "flick_find_database" // Tên file database lưu trên điện thoại
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Chèn dữ liệu mẫu khi database được tạo lần đầu
                            CoroutineScope(Dispatchers.IO).launch {
                                // Lấy instance vừa tạo để seed data
                                getDatabase(context).let { database ->
                                    seedDatabase(database.userDao(), database.movieDao())
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun seedDatabase(userDao: UserDao, movieDao: MovieDao) {
            // Seed data hiện tại không cần thiết cho hệ thống đa người dùng dùng Firebase ID
            // Hoặc có thể tạo một user mặc định nếu muốn
        }
    }
}
