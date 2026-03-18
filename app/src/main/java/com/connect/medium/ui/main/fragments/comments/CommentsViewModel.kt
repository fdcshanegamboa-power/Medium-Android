package com.connect.medium.ui.main.fragments.comments

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.connect.medium.data.local.AppDatabase
import com.connect.medium.data.model.Comment
import com.connect.medium.data.model.User
import com.connect.medium.data.remote.FirestoreDataSource
import com.connect.medium.data.repository.AuthRepository
import com.connect.medium.data.repository.PostRepository
import com.connect.medium.data.repository.UserRepository
import com.connect.medium.utils.Resource
import kotlinx.coroutines.launch
import java.util.UUID

class CommentsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val firestoreDataSource = FirestoreDataSource()
    private val postRepository = PostRepository(firestoreDataSource, db.postDao())
    private val userRepository = UserRepository(firestoreDataSource, db.userDao(), db.followDao())
    private val authRepository = AuthRepository()

    val currentUid = authRepository.getCurrentUser()?.uid ?: ""

    private val _commentsState = MutableLiveData<Resource<List<Comment>>>()
    val commentsState: LiveData<Resource<List<Comment>>> = _commentsState

    private val _addCommentState = MutableLiveData<Resource<Unit>>()
    val addCommentState: LiveData<Resource<Unit>> = _addCommentState

    private val _currentUser = MutableLiveData<User?>()

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            userRepository.observeUser(currentUid)
                .collect { user -> _currentUser.value = user }
        }
    }

    fun loadComments(postId: String) {
        viewModelScope.launch {
            postRepository.observeComments(postId)
                .collect { comments ->
                    _commentsState.value = Resource.Success(comments)
                }
        }
    }
    fun addComment(postId: String, postAuthorUid: String, text: String) {
        if (text.isBlank()) return
        _addCommentState.value = Resource.Loading

        viewModelScope.launch {
            val user = _currentUser.value
            if (user == null) {
                _addCommentState.value = Resource.Error("User not found")
                return@launch
            }

            val comment = Comment(
                commentId = UUID.randomUUID().toString(),
                postId = postId,
                authorUid = currentUid,
                authorUsername = user.username,
                authorProfileImageUrl = user.profileImageUrl,
                text = text,
                createdAt = System.currentTimeMillis()
            )

            _addCommentState.value = postRepository.addComment(comment, postAuthorUid, user)
        }
    }
}