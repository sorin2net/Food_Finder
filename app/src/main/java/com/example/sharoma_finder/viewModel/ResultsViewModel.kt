package com.example.sharoma_finder.viewModel

import androidx.lifecycle.LiveData
import com.example.sharoma_finder.domain.CategoryModel
import com.example.sharoma_finder.repository.ResultsRepository

class ResultsViewModel {
    private val repository=ResultsRepository()

    fun loadSubCategory(id:String):LiveData<MutableList<CategoryModel>>{
        return repository.loadSubCategory(id)
    }
}