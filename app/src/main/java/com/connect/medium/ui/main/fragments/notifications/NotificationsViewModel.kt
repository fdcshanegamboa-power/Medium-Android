package com.connect.medium.ui.main.fragments.notifications

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.connect.medium.data.local.AppDatabase
import com.connect.medium.data.model.Notification
import com.connect.medium.data.remote.FirestoreDataSource
import com.connect.medium.data.repository.AuthRepository
import com.connect.medium.data.repository.NotificationRepository
import com.connect.medium.utils.Resource
import kotlinx.coroutines.launch

class NotificationsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val firestoreDataSource = FirestoreDataSource()
    private val notificationRepository = NotificationRepository(
        firestoreDataSource,
        db.notificationDao()
    )
    private val authRepository = AuthRepository()

    val currentUid = authRepository.getCurrentUser()?.uid ?: ""

    private val _notificationsState = MutableLiveData<Resource<List<Notification>>>()
    val notificationsState: LiveData<Resource<List<Notification>>> = _notificationsState

    val unreadCount: LiveData<Int> = notificationRepository
        .getUnreadCount(currentUid)
        .asLiveData()

    init {
        loadNotifications()
    }

    fun loadNotifications() {
        _notificationsState.value = Resource.Loading
        viewModelScope.launch {
            notificationRepository.observeNotifications(currentUid)
                .collect { notifications ->
                    _notificationsState.value = Resource.Success(notifications)
                }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            notificationRepository.markAllAsRead(currentUid)
        }
    }
}