package com.example.sharoma_finder.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.sharoma_finder.data.SubCategoryDao
import com.example.sharoma_finder.domain.SubCategoryModel


class ResultsRepository(private val subCategoryDao: SubCategoryDao) {


    fun loadSubCategory(id: String): LiveData<Resource<MutableList<SubCategoryModel>>> {
        val listData = MutableLiveData<Resource<MutableList<SubCategoryModel>>>()
        listData.value = Resource.Loading()

        try {
            val liveData = subCategoryDao.getSubCategoriesByCategory(id)

            liveData.observeForever { subCategories ->
                if (subCategories != null) {
                    val mutableList = subCategories.toMutableList()
                    listData.value = Resource.Success(mutableList)
                } else {
                    listData.value = Resource.Success(mutableListOf())
                }
            }
        } catch (e: Exception) {
            listData.value = Resource.Success(mutableListOf())
        }

        return listData
    }
}