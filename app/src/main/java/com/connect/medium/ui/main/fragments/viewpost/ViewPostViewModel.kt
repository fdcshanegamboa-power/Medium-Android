package com.connect.medium.ui.main.fragments.viewpost

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.connect.medium.data.local.AppDatabase
import com.connect.medium.data.model.Comment
import com.connect.medium.data.model.Post
import com.connect.medium.data.model.User
import com.connect.medium.data.remote.FirestoreDataSource
import com.connect.medium.data.repository.AuthRepository
import com.connect.medium.data.repository.PostRepository
import com.connect.medium.data.repository.UserRepository
import com.connect.medium.utils.Resource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

class ViewPostViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val firestoreDataSource = FirestoreDataSource()
    private val postRepository = PostRepository(firestoreDataSource, db.postDao())
    private val userRepository = UserRepository(firestoreDataSource, db.userDao(), db.followDao())
    private val authRepository = AuthRepository()

    val currentUid = authRepository.getCurrentUser()?.uid ?: ""

    private val _postState = MutableLiveData<Resource<Post>>()
    val postState: LiveData<Resource<Post>> = _postState

    private val _commentsState = MutableLiveData<Resource<List<Comment>>>()
    val commentsState: LiveData<Resource<List<Comment>>> = _commentsState

    private val _addCommentState = MutableLiveData<Resource<Unit>>()
    val addCommentState: LiveData<Resource<Unit>> = _addCommentState

    private val _isLiked = MutableLiveData<Boolean>(false)
    val isLiked: LiveData<Boolean> = _isLiked

    private val _currentUser = MutableLiveData<User?>()

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            userRepository.observeUser(currentUid)
                .collect { _currentUser.value = it }
        }
    }

    fun loadPost(postId: String) {
        _postState.value = Resource.Loading
        viewModelScope.launch {
            val minDelay = launch { delay(800) }
            firestoreDataSource.observePost(postId)
                .collect { post ->
                    if (post != null) {
                        _postState.value = Resource.Success(post)
                        // check if liked
                        val liked = postRepository.isPostLikedByUser(postId, currentUid)
                        _isLiked.value = liked
                    } else {
                        _postState.value = Resource.Error("Post not found")
                    }
                }
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

    fun toggleLike(post: Post) {
        viewModelScope.launch {
            val isLiked = _isLiked.value ?: false
            _isLiked.value = !isLiked // optimistic

            val currentUser = _currentUser.value
            val result = if (isLiked) {
                postRepository.unlikePost(post.postId, currentUid)
            } else {
                if (currentUser != null) {
                    postRepository.likePost(post.postId, currentUid, post.authorUid, currentUser)
                } else Resource.Error("User not found")
            }

            if (result is Resource.Error) {
                _isLiked.value = isLiked // rollback
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