package com.connect.medium.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.connect.medium.data.local.AppDatabase
import com.connect.medium.data.model.User
import com.connect.medium.data.remote.FirestoreDataSource
import com.connect.medium.data.repository.AuthRepository
import com.connect.medium.data.repository.UserRepository
import com.connect.medium.utils.Resource
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()
    private val userRepository = UserRepository(
        FirestoreDataSource(),
        AppDatabase.getInstance(application).userDao(),
        AppDatabase.getInstance(application).followDao()
    )

    private val _authState = MutableLiveData<Resource<FirebaseUser>>()
    val authState: LiveData<Resource<FirebaseUser>> = _authState

    fun login(email: String, password: String) {
        _authState.value = Resource.Loading
        viewModelScope.launch {
            val result = authRepository.login(email, password)
            if (result is Resource.Success) {
                userRepository.syncFollowingList(result.data.uid)
            }
            _authState.value = result
        }
    }

    fun register(email: String, password: String, username: String, displayName: String) {
        _authState.value = Resource.Loading
        viewModelScope.launch {
            // 1. create firebase auth account
            val result = authRepository.register(email, password)

            if (result is Resource.Success) {
                // 2. create user document in firestore
                val user = User(
                    uid = result.data.uid,
                    username = username,
                    displayName = displayName,
                    createdAt = System.currentTimeMillis()
                )
                val createResult = userRepository.createUser(user)

                if (createResult is Resource.Error) {
                    _authState.value = Resource.Error(createResult.message)
                    return@launch
                }
            }

            _authState.value = result
        }
    }

    fun isLoggedIn() = authRepository.isLoggedIn()
    fun getCurrentUser() = authRepository.getCurrentUser()
}