package com.connect.medium.ui.main.fragments.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.connect.medium.data.local.AppDatabase
import com.connect.medium.data.model.User
import com.connect.medium.data.remote.FirestoreDataSource
import com.connect.medium.data.repository.UserRepository
import com.connect.medium.utils.Resource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val firestoreDataSource = FirestoreDataSource()
    private val userRepository = UserRepository(firestoreDataSource, db.userDao(), db.followDao())

    private val _searchState = MutableLiveData<Resource<List<User>>>()
    val searchState: LiveData<Resource<List<User>>> = _searchState

    private var searchJob: Job? = null

    fun search(query: String) {
        // cancel previous search if user is still typing
        searchJob?.cancel()

        if (query.isBlank()) {
            _searchState.value = Resource.Success(emptyList())
            return
        }

        searchJob = viewModelScope.launch {
            // debounce — wait 300ms after user stops typing
            delay(300)
            _searchState.value = Resource.Loading
            _searchState.value = userRepository.searchUsers(query.lowercase().trim())
        }
    }
}