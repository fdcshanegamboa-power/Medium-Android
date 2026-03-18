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

    private val _userState = MutableLiveData<Resource<User>>()
    val userState: LiveData<Resource<User>> = _userState

    private val _postsState = MutableLiveData<Resource<List<Post>>>()
    val postsState: LiveData<Resource<List<Post>>> = _postsState

    private val _followState = MutableLiveData<Resource<Unit>>()
    val followState: LiveData<Resource<Unit>> = _followState

    private val _isFollowing = MutableLiveData<Boolean>()
    val isFollowing: LiveData<Boolean> = _isFollowing

    fun loadUser(uid: String) {
        viewModelScope.launch {
            userRepository.observeUser(uid)
                .collect { user ->
                    if (user != null) {
                        _userState.value = Resource.Success(user)
                    } else {
                        _userState.value = Resource.Error("User not found")
                    }
                }
        }
    }

    fun loadUserPosts(uid: String) {
        viewModelScope.launch {
            postRepository.observeUserPosts(uid)
                .collect { posts ->
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

    fun followUser(targetUid: String) {
        viewModelScope.launch {
            _followState.value = userRepository.followUser(currentUid, targetUid)
        }
    }

    fun unfollowUser(targetUid: String) {
        viewModelScope.launch {
            _followState.value = userRepository.unfollowUser(currentUid, targetUid)
        }
    }

    fun logout() = authRepository.logout()
}