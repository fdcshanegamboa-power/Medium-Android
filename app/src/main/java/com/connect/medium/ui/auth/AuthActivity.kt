package com.connect.medium.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.connect.medium.databinding.ActivityAuthBinding
import com.connect.medium.ui.main.MainActivity

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private val viewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        if(viewModel.isLoggedIn()){
            navigateToMain()
        }

        setContent {
            MaterialTheme {
                AuthNavGraph(viewModel = viewModel, onAuthSuccess = {
                    navigateToMain()
                })
            }
        }
    }
    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }
}