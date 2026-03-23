package com.connect.medium.ui.main.fragments.profile

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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val firestoreDataSource = FirestoreDataSource()

    private val userRepository = UserRepository(
        firestoreDataSource,
        db.userDao(),
        db.followDao()
    )
    private val postRepository = PostRepository(firestoreDataSource, db.postDao())
    private val authRepository = AuthRepository()

    val currentUid = authRepository.getCurrentUser()?.uid ?: ""

    private val _isFollowedBy = MutableLiveData<Boolean>(false)
    val isFollowedBy: LiveData<Boolean> = _isFollowedBy

    private val _userState = MutableLiveData<Resource<User>>()
    val userState: LiveData<Resource<User>> = _userState

    private val _postsState = MutableLiveData<Resource<List<Post>>>()
    val postsState: LiveData<Resource<List<Post>>> = _postsState

    private val _followState = MutableLiveData<Resource<Unit>>()
    val followState: LiveData<Resource<Unit>> = _followState

    private val _isFollowing = MutableLiveData<Boolean>()
    val isFollowing: LiveData<Boolean> = _isFollowing

    fun loadUser(uid: String) {
        _userState.value = Resource.Loading
        viewModelScope.launch {
            val minDelay = launch { kotlinx.coroutines.delay(600) }
            userRepository.observeUser(uid)
                .collect { user ->
                    minDelay.join()
                    _userState.value = if (user != null)
                        Resource.Success(user)
                    else
                        Resource.Error("User not found")
                }
        }
    }

    fun loadUserPosts(uid: String) {
        _postsState.value = Resource.Loading
        viewModelScope.launch {
            val minDelay = launch { delay(800) }
            postRepository.observeUserPosts(uid)
                .collect { posts ->
                    minDelay.join()
                    _postsState.value = Resource.Success(posts)
                }
        }
    }

    fun observeIsFollowing(targetUid: String) {
        viewModelScope.launch {
            userRepository.isFollowing(currentUid, targetUid)
                .collect { _isFollowing.value = it }
        }
    }
    fun checkIsFollowedBy(targetUid: String) {
        viewModelScope.launch {
            userRepository.observeIsFollowedBy(currentUid, targetUid)
                .collect { _isFollowedBy.value = it }
        }
    }

    fun followUser(targetUid: String) {
        viewModelScope.launch {
            val user = _userState.value
            if (user is Resource.Success) {
                val currentUser = userRepository.getCachedUser(currentUid).first()
                if (currentUser != null) {
                    _followState.value = userRepository.followUser(currentUid, targetUid, currentUser)
                }
            }        }
    }

    fun unfollowUser(targetUid: String) {
        viewModelScope.launch {
            _followState.value = userRepository.unfollowUser(currentUid, targetUid)
        }
    }

    fun logout() = authRepository.logout()
}