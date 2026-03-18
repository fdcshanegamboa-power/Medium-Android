package com.connect.medium.ui.main.fragments.create

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class CreatePostViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreatePostViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CreatePostViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}