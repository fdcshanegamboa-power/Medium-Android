package com.connect.medium.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import com.connect.medium.R
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import com.connect.medium.databinding.ActivityMainBinding
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.connect.medium.ui.main.fragments.notifications.NotificationsViewModel
import com.connect.medium.ui.main.fragments.notifications.NotificationsViewModelFactory

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var notificationsViewModel: NotificationsViewModel

    private val prefs by lazy {
        getSharedPreferences("app_prefs", MODE_PRIVATE)
    }
    private var hasShownRationale: Boolean
        get() = prefs.getBoolean("has_shown_notification_rationale", false)
        set(value) = prefs.edit().putBoolean("has_shown_notification_rationale", value).apply()


    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (isGranted) {
                    Log.d("MainActivity", "Notification permission granted")
                    hasShownRationale = false
                } else {
                    Log.d("MainActivity", "Notification permission denied")
                    if (!shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                        Log.d("MainActivity", "Permanent denial detected - showing settings dialog")
                        showGoToSettingsDialog()
                    }

                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.main_nav_host) as NavHostFragment
        navController = navHostFragment.navController

        binding.bottomNav.setupWithNavController(navController)
        binding.bottomNav.menu.findItem(R.id.placeholder)?.apply {
            isEnabled = false
        }
        binding.fabCreate.setOnClickListener {
            navController.navigate(R.id.createPostFragment)
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.createPostFragment, R.id.settingsFragment, R.id.userProfileFragment, R.id.viewPostFragment-> {
                    binding.fabCreate.hide()
                    binding.bottomNav.visibility = View.GONE
                }
                R.id.profileFragment -> {
                    val isRootTab = navController.previousBackStackEntry?.destination?.id == null ||
                            navController.previousBackStackEntry?.destination?.id == R.id.homeFragment ||
                            navController.previousBackStackEntry?.destination?.id == R.id.notificationsFragment ||
                            navController.previousBackStackEntry?.destination?.id == R.id.searchFragment
                    if (isRootTab) {
                        binding.fabCreate.show()
                        binding.bottomNav.visibility = View.VISIBLE
                    } else {
                        binding.fabCreate.hide()
                        binding.bottomNav.visibility = View.GONE
                    }
                }
                R.id.notificationsFragment -> {
                    requestNotificationPermission()
                }
                R.id.commentsFragment -> {
                    binding.fabCreate.hide()
                    binding.bottomNav.visibility = View.VISIBLE
                    binding.bottomNav.menu.findItem(R.id.homeFragment)?.isChecked = true
                }
                else -> {
                    binding.fabCreate.show()
                    binding.bottomNav.visibility = View.VISIBLE
                }
            }
        }

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

    private fun showNotificationRationale() {
        AlertDialog.Builder(this)
            .setTitle("Why notifications matter")
            .setMessage("Get notified when someone likes, comments, or follows you. This helps you stay connected with your community.")
            .setPositiveButton("Allow") { _, _ ->
                hasShownRationale = true
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            .setNegativeButton("Not Now") { _, _ ->
                Log.d("MainActivity", "User declined rationale")
                hasShownRationale = true
            }.show()
    }

    private fun showGoToSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Notifications are disabled")
            .setMessage("You've permanently disabled notifications. To receive updates, please enable them in Settings.")
            .setPositiveButton("Open Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

     fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED -> {
                Log.d("MainActivity", "Permission already granted")
            }
            shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) && !hasShownRationale-> {
                Log.d("MainActivity", "Showing rationale (user denied before without 'never ask again')")
                showNotificationRationale()
            }
            else -> {
                Log.d("MainActivity", "First time request OR permanent denial detected")
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                Log.d("MainActivity", "Notification permission granted in settings")
                hasShownRationale = false

            }
        }
    }
}