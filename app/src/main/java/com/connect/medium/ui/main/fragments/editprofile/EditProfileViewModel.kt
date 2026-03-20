package com.connect.medium.ui.main.fragments.editprofile

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.connect.medium.data.local.AppDatabase
import com.connect.medium.data.model.User
import com.connect.medium.data.remote.CloudinaryDataSource
import com.connect.medium.data.remote.FirestoreDataSource
import com.connect.medium.data.repository.AuthRepository
import com.connect.medium.data.repository.UserRepository
import com.connect.medium.utils.Resource
import kotlinx.coroutines.launch

class EditProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val firestoreDataSource = FirestoreDataSource()
    private val userRepository = UserRepository(firestoreDataSource, db.userDao(), db.followDao())
    private val authRepository = AuthRepository()
    private val cloudinaryDataSource = CloudinaryDataSource(application)

    val currentUid = authRepository.getCurrentUser()?.uid ?: ""

    private val _userState = MutableLiveData<Resource<User>>()
    val userState: LiveData<Resource<User>> = _userState

    private val _updateState = MutableLiveData<Resource<Unit>>()
    val updateState: LiveData<Resource<Unit>> = _updateState

    private val _uploadProgress = MutableLiveData<Int>()
    val uploadProgress: LiveData<Int> = _uploadProgress

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            userRepository.observeUser(currentUid)
                .collect { user ->
                    if (user != null) _userState.value = Resource.Success(user)
                }
        }
    }

    fun updateProfile(
        displayName: String,
        bio: String,
        imageUri: Uri? = null
    ) {
        _updateState.value = Resource.Loading

        viewModelScope.launch {
            if (imageUri != null) {
                // upload new profile image first
                cloudinaryDataSource.uploadImage(
                    uri = imageUri,
                    folder = "profiles",
                    onSuccess = { imageUrl ->
                        viewModelScope.launch {
                            saveProfile(displayName, bio, imageUrl)
                        }
                    },
                    onError = { error ->
                        _updateState.postValue(Resource.Error(error))
                    },
                    onProgress = { progress ->
                        _uploadProgress.postValue(progress)
                    }
                )
            } else {
                saveProfile(displayName, bio, null)
            }
        }
    }

    private suspend fun saveProfile(
        displayName: String,
        bio: String,
        imageUrl: String?
    ) {
        val fields = mutableMapOf<String, Any>(
            "displayName" to displayName,
            "bio" to bio
        )
        imageUrl?.let { fields["profileImageUrl"] = it }

        _updateState.value = userRepository.updateUser(currentUid, fields)
    }
}