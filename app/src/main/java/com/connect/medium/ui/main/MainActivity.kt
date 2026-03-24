package com.connect.medium.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.connect.medium.R
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import com.connect.medium.databinding.ActivityMainBinding
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.connect.medium.ui.main.fragments.notifications.NotificationsViewModel
import com.connect.medium.ui.main.fragments.notifications.NotificationsViewModelFactory

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var notificationsViewModel: NotificationsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.main_nav_host) as NavHostFragment
        navController = navHostFragment.navController

        val appBarConfig = AppBarConfiguration(
            setOf(
                R.id.homeFragment,
                R.id.searchFragment,
                R.id.notificationsFragment,
                R.id.profileFragment
            )
        )

        binding.bottomNav.setupWithNavController(navController)
        binding.bottomNav.menu.findItem(R.id.placeholder)?.apply {
            isEnabled = false
        }
        binding.fabCreate.setOnClickListener {
            navController.navigate(R.id.createPostFragment)
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            Log.d("NAV", "destination: ${destination.label}, id: ${destination.id}, userProfileId: ${R.id.userProfileFragment}")
            when (destination.id) {
                R.id.createPostFragment, R.id.settingsFragment, R.id.userProfileFragment, R.id.commentsFragment, R.id.viewPostFragment-> {
                    binding.fabCreate.hide()
                    binding.bottomNav.visibility = View.GONE
                }
                else -> {
                    binding.fabCreate.show()
                    binding.bottomNav.visibility = View.VISIBLE
                }
            }
        }
        requestNotificationPermission()
        setupNotificationBadge()
        handleNotificationIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent) {
        val type = intent.getStringExtra("notification_type") ?: return
        val postId = intent.getStringExtra("notification_post_id") ?: ""
        val fromUid = intent.getStringExtra("notification_from_uid") ?: ""

        // wait for nav controller to be ready
        binding.root.post {
            when (type) {
                "FOLLOW" -> {
                    if (fromUid.isNotEmpty()) {
                        navController.navigate(
                            R.id.userProfileFragment,
                            Bundle().apply { putString("uid", fromUid) }
                        )
                    }
                }
                "LIKE", "COMMENT" -> {
                    if (postId.isNotEmpty()) {
                        navController.navigate(
                            R.id.viewPostFragment,
                            Bundle().apply { putString("postId", postId) }
                        )
                    }
                }
            }
        }
    }
    private fun setupNotificationBadge(){
        notificationsViewModel = ViewModelProvider(
            this,
            NotificationsViewModelFactory(application)
        )[NotificationsViewModel::class.java]

        notificationsViewModel.unreadCount.observe(this) { count ->
            val badge = binding.bottomNav.getOrCreateBadge(R.id.notificationsFragment)
            if (count > 0) {
                badge.isVisible = true
                badge.number = count
            } else {
                badge.isVisible = false
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    100
                )
            }
        }
    }
}