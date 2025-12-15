package com.example.sharoma_finder.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.sharoma_finder.domain.BannerModel
import com.example.sharoma_finder.domain.CacheMetadata // ✅ ADĂUGAT
import com.example.sharoma_finder.domain.CategoryModel
import com.example.sharoma_finder.domain.StoreModel
import com.example.sharoma_finder.domain.SubCategoryModel

@Database(
    entities = [
        StoreModel::class,
        CategoryModel::class,
        BannerModel::class,
        SubCategoryModel::class,
        CacheMetadata::class // ✅ ADĂUGAT: Tabel nou pentru cache
    ],
    version = 4, // ✅ ACTUALIZAT: Versiunea a crescut de la 3 la 4
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun storeDao(): StoreDao
    abstract fun categoryDao(): CategoryDao
    abstract fun bannerDao(): BannerDao
    abstract fun subCategoryDao(): SubCategoryDao
    abstract fun cacheMetadataDao(): CacheMetadataDao // ✅ ADĂUGAT: DAO nou

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * ✅ MIGRARE 1 → 2
         *
         * CE S-A SCHIMBAT: S-au adăugat tabelele "banners" și "categories"
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Log.d("AppDatabase", "🔄 Running migration 1→2")

                try {
                    // Creare tabel pentru bannere
                    database.execSQL("""
                        CREATE TABLE IF NOT EXISTS banners (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            image TEXT NOT NULL
                        )
                    """.trimIndent())

                    // Creare tabel pentru categorii
                    database.execSQL("""
                        CREATE TABLE IF NOT EXISTS categories (
                            Id INTEGER PRIMARY KEY NOT NULL,
                            ImagePath TEXT NOT NULL,
                            Name TEXT NOT NULL
                        )
                    """.trimIndent())

                    Log.d("AppDatabase", "✅ Migration 1→2 completed successfully")

                } catch (e: Exception) {
                    Log.e("AppDatabase", "❌ Migration 1→2 failed: ${e.message}")
                    throw e
                }
            }
        }

        /**
         * ✅ MIGRARE 2 → 3
         *
         * CE S-A SCHIMBAT: S-a adăugat tabelul "subcategories"
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Log.d("AppDatabase", "🔄 Running migration 2→3")

                try {
                    // Creare tabel pentru subcategorii
                    database.execSQL("""
                        CREATE TABLE IF NOT EXISTS subcategories (
                            Id INTEGER PRIMARY KEY NOT NULL,
                            CategoryId TEXT NOT NULL,
                            ImagePath TEXT NOT NULL,
                            Name TEXT NOT NULL
                        )
                    """.trimIndent())

                    // ✅ OPȚIONAL: Crează index pentru query-uri mai rapide
                    database.execSQL("""
                        CREATE INDEX IF NOT EXISTS index_subcategories_CategoryId 
                        ON subcategories(CategoryId)
                    """.trimIndent())

                    Log.d("AppDatabase", "✅ Migration 2→3 completed successfully")

                } catch (e: Exception) {
                    Log.e("AppDatabase", "❌ Migration 2→3 failed: ${e.message}")
                    throw e
                }
            }
        }

        /**
         * ✅ MIGRARE 3 → 4 (NOU)
         *
         * CE S-A SCHIMBAT: Adăugăm tabelul "cache_metadata" pentru expirarea datelor
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Log.d("AppDatabase", "🔄 Running migration 3→4")
                try {
                    database.execSQL("""
                        CREATE TABLE IF NOT EXISTS cache_metadata (
                            `key` TEXT PRIMARY KEY NOT NULL,
                            timestamp INTEGER NOT NULL,
                            expiresAt INTEGER NOT NULL,
                            itemCount INTEGER NOT NULL DEFAULT 0
                        )
                    """.trimIndent())
                    Log.d("AppDatabase", "✅ Migration 3→4 completed")
                } catch (e: Exception) {
                    Log.e("AppDatabase", "❌ Migration 3→4 failed: ${e.message}")
                    throw e
                }
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sharoma_database"
                )
                    // ✅ CRUCIAL: Adaugă toate migrările, inclusiv cea nouă
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)

                    // ✅ ALTERNATIVĂ SIGURĂ pentru production:
                    .fallbackToDestructiveMigrationOnDowngrade() // Șterge doar la downgrade

                    .build()

                INSTANCE = instance
                Log.d("AppDatabase", "✅ Database instance created with migrations")
                instance
            }
        }

        /**
         * ✅ BONUS: Funcție pentru debugging - verifică versiunea DB
         */
        fun getDatabaseVersion(context: Context): Int {
            return try {
                val db = getDatabase(context).openHelper.readableDatabase
                db.version
            } catch (e: Exception) {
                -1
            }
        }
    }
}