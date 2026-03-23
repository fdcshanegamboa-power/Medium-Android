package com.connect.medium.ui.main.fragments.create

import android.app.Application
import android.net.Uri
import android.util.Log
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
import kotlinx.coroutines.delay
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

    private val _uploadStatus = MutableLiveData<String>()
    val uploadStatus: LiveData<String> = _uploadStatus

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

    fun createPost(mediaItems: List<Pair<Uri, String>>, caption: String) {

        if (mediaItems.isEmpty()) {
            _createPostState.value = Resource.Error("Please select an image")
            return
        }

        val user = _currentUser.value
        if (user == null) {
            _createPostState.value = Resource.Error("User data not loaded yet, try again")
            return
        }

        Log.d("CreatePost", "Author profile image: '${user.profileImageUrl}'")


        _createPostState.value = Resource.Loading

        viewModelScope.launch {
            val uploadedUrls = mutableListOf<String>()
            val uploadedTypes = mutableListOf<String>()

            for ((index, item) in mediaItems.withIndex()) {
                val (uri, type) = item
                _uploadStatus.value = "Uploading ${index + 1} of ${mediaItems.size}..."

                val folder = if (type == "video") "posts/videos" else "posts/images"

                var success = false
                cloudinaryDataSource.uploadImage(
                    uri = uri,
                    folder = folder,
                    onSuccess = { url ->
                        uploadedUrls.add(url)
                        uploadedTypes.add(type)
                        success = true
                    },
                    onError = { error ->
                        _createPostState.postValue(Resource.Error("Failed to upload item ${index + 1}: $error"))
                    },
                    onProgress = { progress ->
                        _uploadProgress.postValue(progress)
                    }
                )

                // wait for upload to finish
                var waited = 0
                while (!success && waited < 60000) {
                    delay(100)
                    waited += 100
                }

                if (!success) {
                    _createPostState.value = Resource.Error("Upload timed out")
                    return@launch
                }
            }

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
                mediaUrls = uploadedUrls,
                mediaTypes = uploadedTypes,
                caption = caption,
                createdAt = System.currentTimeMillis()
            )

            _createPostState.value = postRepository.createPost(post)
        }
    }
}