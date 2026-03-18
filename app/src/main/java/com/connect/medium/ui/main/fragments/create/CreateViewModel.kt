package com.connect.medium.ui.main.fragments.create

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.connect.medium.data.local.AppDatabase
import com.connect.medium.data.model.Post
import com.connect.medium.data.model.User
import com.connect.medium.data.remote.CloudinaryDataSource
import com.connect.medium.data.remote.FirestoreDataSource
import com.connect.medium.data.repository.AuthRepository
import com.connect.medium.data.repository.PostRepository
import com.connect.medium.data.repository.UserRepository
import com.connect.medium.utils.Resource
import kotlinx.coroutines.launch
import java.util.UUID

class CreatePostViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val firestoreDataSource = FirestoreDataSource()
    private val postRepository = PostRepository(firestoreDataSource, db.postDao())
    private val userRepository = UserRepository(firestoreDataSource, db.userDao(), db.followDao())
    private val authRepository = AuthRepository()
    private val cloudinaryDataSource = CloudinaryDataSource(application)

    val currentUid = authRepository.getCurrentUser()?.uid ?: ""

    private val _uploadProgress = MutableLiveData<Int>()
    val uploadProgress: LiveData<Int> = _uploadProgress

    private val _createPostState = MutableLiveData<Resource<Unit>>()
    val createPostState: LiveData<Resource<Unit>> = _createPostState

    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            userRepository.observeUser(currentUid)
                .collect { user ->
                    _currentUser.value = user
                }
        }
    }

    fun createPost(imageUri: Uri, caption: String) {
        _createPostState.value = Resource.Loading

        cloudinaryDataSource.uploadImage(
            uri = imageUri,
            folder = "posts",
            onSuccess = { imageUrl ->
                viewModelScope.launch {
                    val user = _currentUser.value
                    if (user == null) {
                        _createPostState.value = Resource.Error("User not found")
                        return@launch
                    }

                    val post = Post(
                        postId = UUID.randomUUID().toString(),
                        authorUid = currentUid,
                        authorUsername = user.username,
                        authorProfileImageUrl = user.profileImageUrl,
                        imageUrl = imageUrl,
                        caption = caption,
                        createdAt = System.currentTimeMillis()
                    )

                    _createPostState.value = postRepository.createPost(post)
                }
            },
            onError = { error ->
                _createPostState.value = Resource.Error(error)
            },
            onProgress = { progress ->
                _uploadProgress.postValue(progress)
            }
        )
    }
}