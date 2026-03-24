package com.connect.medium.ui.main.fragments.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.connect.medium.data.local.AppDatabase
import com.connect.medium.data.model.Post
import com.connect.medium.data.model.User
import com.connect.medium.data.remote.FirestoreDataSource
import com.connect.medium.data.repository.AuthRepository
import com.connect.medium.data.repository.PostRepository
import com.connect.medium.data.repository.UserRepository
import com.connect.medium.utils.Resource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val firestoreDataSource = FirestoreDataSource()
    private val postRepository = PostRepository(firestoreDataSource, db.postDao())
    private val authRepository = AuthRepository()
    private val userRepository = UserRepository(firestoreDataSource, db.userDao(), db.followDao())
    private val _currentUser = MutableLiveData<User?>()

    val currentUid = authRepository.getCurrentUser()?.uid ?: ""

    private val _postsState = MutableLiveData<Resource<List<Post>>>()
    val postsState: LiveData<Resource<List<Post>>> = _postsState

    private val _paginationState = MutableLiveData<Resource<Unit>>()
    val paginationState: LiveData<Resource<Unit>> = _paginationState

    private val _likeState = MutableLiveData<Resource<Unit>>()
    val likeState: LiveData<Resource<Unit>> = _likeState

    private val _likedPostIds = MutableLiveData<Set<String>>()
    val likedPostIds: LiveData<Set<String>> = _likedPostIds

    private val localLikedPosts = mutableSetOf<String>()

    // pagination state
    private val allPosts = mutableListOf<Post>()
    private var lastDocument: com.google.firebase.firestore.DocumentSnapshot? = null
    private var isLoadingMore = false
    var hasMorePosts = true
        private set

    init {
        loadFeed()
        loadCurrentUser()
    }

    fun loadFeed() {
        // reset pagination
        allPosts.clear()
        lastDocument = null
        hasMorePosts = true
        isLoadingMore = false

        _postsState.value = Resource.Loading
        viewModelScope.launch {
            val minDelay = launch { delay(800) }
            try {
                val (posts, lastDoc) = postRepository.getFeedPostsPaginated(limit = 5)
                postRepository.observeFeedPosts()
                    .collect { posts ->
                        minDelay.join()
                        _postsState.value = Resource.Success(posts)
                    }
                allPosts.addAll(posts)
                lastDocument = lastDoc
                hasMorePosts = posts.size >= 5
                _postsState.value = Resource.Success(allPosts.toList())
            } catch (e: Exception) {
                _postsState.value = Resource.Error(e.message ?: "Failed to load feed")
            }
        }
    }

    fun loadMorePosts() {
        if (isLoadingMore || !hasMorePosts) return

        isLoadingMore = true
        _paginationState.value = Resource.Loading

        viewModelScope.launch {
            try {
                val (posts, lastDoc) = postRepository.getFeedPostsPaginated(
                    limit = 5,
                    lastDocument = lastDocument
                )
                if (posts.isEmpty()) {
                    hasMorePosts = false
                    _paginationState.value = Resource.Success(Unit)
                    return@launch
                }
                allPosts.addAll(posts)
                lastDocument = lastDoc
                hasMorePosts = posts.size >= 5
                _postsState.value = Resource.Success(allPosts.toList())
                _paginationState.value = Resource.Success(Unit)
            } catch (e: Exception) {
                _paginationState.value = Resource.Error(e.message ?: "Failed to load more")
            } finally {
                isLoadingMore = false
            }
        }
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            userRepository.observeUser(currentUid)
                .collect { _currentUser.value = it }
        }
    }

    fun toggleLike(post: Post) {
        viewModelScope.launch {
            val isLiked = localLikedPosts.contains(post.postId)
            if (isLiked) localLikedPosts.remove(post.postId)
            else localLikedPosts.add(post.postId)
            _likedPostIds.value = localLikedPosts.toSet()

            val currentUser = _currentUser.value
            val result = if (isLiked) {
                postRepository.unlikePost(post.postId, currentUid)
            } else {
                if (currentUser != null) {
                    postRepository.likePost(post.postId, currentUid, post.authorUid, currentUser)
                } else {
                    Resource.Error("User not found")
                }
            }

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