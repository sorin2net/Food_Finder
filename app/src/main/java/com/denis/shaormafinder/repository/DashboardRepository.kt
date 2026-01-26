package com.denis.shaormafinder.repository

import androidx.lifecycle.LiveData
import com.denis.shaormafinder.data.BannerDao
import com.denis.shaormafinder.data.CategoryDao
import com.denis.shaormafinder.data.SubCategoryDao
import com.denis.shaormafinder.domain.BannerModel
import com.denis.shaormafinder.domain.CategoryModel
import com.denis.shaormafinder.domain.SubCategoryModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

class DashboardRepository(
    private val categoryDao: CategoryDao,
    private val bannerDao: BannerDao,
    private val subCategoryDao: SubCategoryDao
) {
    private val firebaseDatabase = FirebaseDatabase.getInstance()

    val allCategories: LiveData<List<CategoryModel>> = categoryDao.getAllCategories()
    val allBanners: LiveData<List<BannerModel>> = bannerDao.getAllBanners()

    suspend fun refreshCategories() {
        withContext(Dispatchers.IO) {
            try {
                val snapshot = withTimeoutOrNull(10000L) {
                    firebaseDatabase.getReference("Category").get().await()
                }

                if (snapshot == null) return@withContext

                val categories = mutableListOf<CategoryModel>()
                for (child in snapshot.children) {
                    child.getValue(CategoryModel::class.java)?.let { categories.add(it) }
                }

                if (categories.isNotEmpty()) {
                    categoryDao.insertAll(categories)
                }
            } catch (e: Exception) {
                // Fail silently or handle error without logging to production console
            }
        }
    }

    suspend fun refreshBanners() {
        withContext(Dispatchers.IO) {
            try {
                val snapshot = withTimeoutOrNull(10000L) {
                    firebaseDatabase.getReference("Banners").get().await()
                }

                if (snapshot == null) return@withContext

                val banners = mutableListOf<BannerModel>()
                for (child in snapshot.children) {
                    child.getValue(BannerModel::class.java)?.let { banners.add(it) }
                }

                if (banners.isNotEmpty()) {
                    bannerDao.insertAll(banners)
                }
            } catch (e: Exception) {
                com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e)
            }
        }
    }

    suspend fun refreshSubCategories() {
        withContext(Dispatchers.IO) {
            try {
                val snapshot = withTimeoutOrNull(10000L) {
                    firebaseDatabase.getReference("SubCategory").get().await()
                }

                if (snapshot == null) return@withContext

                val subCategories = mutableListOf<SubCategoryModel>()
                for (child in snapshot.children) {
                    val parsed = parseSubCategoryFromSnapshot(child)
                    if (parsed != null) {
                        subCategories.add(parsed)
                    }
                }

                if (subCategories.isNotEmpty()) {
                    subCategoryDao.insertAll(subCategories)
                }
            } catch (e: Exception) {
                com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e)
            }
        }
    }

    private fun parseSubCategoryFromSnapshot(snapshot: DataSnapshot): SubCategoryModel? {
        return try {
            val map = snapshot.value as? Map<*, *> ?: return null
            val categoryIds = convertToList(map["CategoryIds"] ?: map["CategoryId"])

            SubCategoryModel(
                Id = (map["Id"] as? Long)?.toInt() ?: 0,
                CategoryIds = categoryIds,
                ImagePath = map["ImagePath"] as? String ?: "",
                Name = map["Name"] as? String ?: ""
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun convertToList(data: Any?): List<String> {
        return when (data) {
            is List<*> -> data.mapNotNull { it?.toString() }
            is Long, is Int, is String -> listOf(data.toString())
            else -> emptyList()
        }
    }

    fun getSubCategoriesByCategory(categoryId: String): LiveData<List<SubCategoryModel>> {
        return subCategoryDao.getSubCategoriesByCategory(categoryId)
    }

    suspend fun hasCachedCategories(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                categoryDao.getCategoryCount() > 0
            } catch (e: Exception) {
                false
            }
        }
    }

    suspend fun hasCachedBanners(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                bannerDao.getBannerCount() > 0
            } catch (e: Exception) {
                false
            }
        }
    }

    suspend fun hasCachedSubCategories(categoryId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                subCategoryDao.getSubCategoryCount(categoryId) > 0
            } catch (e: Exception) {
                false
            }
        }
    }
}