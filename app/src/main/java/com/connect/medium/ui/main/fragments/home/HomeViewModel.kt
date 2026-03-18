package com.connect.medium.ui.main.fragments.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.connect.medium.data.local.AppDatabase
import com.connect.medium.data.model.Post
import com.connect.medium.data.remote.FirestoreDataSource
import com.connect.medium.data.repository.AuthRepository
import com.connect.medium.data.repository.PostRepository
import com.connect.medium.utils.Resource
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val firestoreDataSource = FirestoreDataSource()
    private val postRepository = PostRepository(firestoreDataSource, db.postDao())
    private val authRepository = AuthRepository()

    val currentUid = authRepository.getCurrentUser()?.uid ?: ""

    private val _postsState = MutableLiveData<Resource<List<Post>>>()
    val postsState: LiveData<Resource<List<Post>>> = _postsState

    private val _likeState = MutableLiveData<Resource<Unit>>()
    val likeState: LiveData<Resource<Unit>> = _likeState

    private val _likedPostIds = MutableLiveData<Set<String>>()
    val likedPostIds: LiveData<Set<String>> = _likedPostIds

    // tracks optimistic like state locally
    private val localLikedPosts = mutableSetOf<String>()

    init {
        loadFeed()
    }

    fun loadFeed() {
        _postsState.value = Resource.Loading
        viewModelScope.launch {
            postRepository.observeFeedPosts()
                .collect { posts ->
                    _postsState.value = Resource.Success(posts)
                }
        }
    }

    fun toggleLike(post: Post) {
        viewModelScope.launch {
            val isLiked = localLikedPosts.contains(post.postId)

            // optimistic update
            if (isLiked) {
                localLikedPosts.remove(post.postId)
            } else {
                localLikedPosts.add(post.postId)
            }
            _likedPostIds.value = localLikedPosts.toSet()

            // sync to Firestore
            val result = if (isLiked) {
                postRepository.unlikePost(post.postId, currentUid)
            } else {
                postRepository.likePost(post.postId, currentUid)
            }

            // rollback if failed
            if (result is Resource.Error) {
                if (isLiked) localLikedPosts.add(post.postId)
                else localLikedPosts.remove(post.postId)
                _likedPostIds.value = localLikedPosts.toSet()
                _likeState.value = result
            }
        }
    }

    fun checkLikedPosts(postIds: List<String>) {
        viewModelScope.launch {
            postIds.forEach { postId ->
                val isLiked = postRepository.isPostLikedByUser(postId, currentUid)
                if (isLiked) localLikedPosts.add(postId)
            }
            _likedPostIds.value = localLikedPosts.toSet()
        }
    }
}