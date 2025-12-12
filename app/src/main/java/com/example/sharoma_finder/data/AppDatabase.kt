package com.example.sharoma_finder.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.sharoma_finder.domain.StoreModel

@Database(entities = [StoreModel::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class) // ✅ Folosim convertorul creat la Pasul 1
abstract class AppDatabase : RoomDatabase() {
    abstract fun storeDao(): StoreDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sharoma_database"
                )
                    .fallbackToDestructiveMigration() // Resetează DB dacă schimbăm structura (ok pt cache)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}