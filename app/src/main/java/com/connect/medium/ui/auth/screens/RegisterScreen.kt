package com.connect.medium.ui.auth.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.connect.medium.R
import com.connect.medium.ui.auth.AuthViewModel
import com.connect.medium.utils.Resource

@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit,
    onAuthSuccess: () -> Unit
) {
    val authState by viewModel.authState.observeAsState()

    var displayName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayNameError by remember { mutableStateOf<String?>(null) }
    var usernameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }

    val isLoading = authState is Resource.Loading

    LaunchedEffect(authState) {
        if (authState is Resource.Success) onAuthSuccess()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Image(
            painter = painterResource(id = R.drawable.ic_logo_raw),
            contentDescription = "App Logo",
            modifier = Modifier.size(120.dp),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Create Account", fontSize = 28.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(8.dp))

        Text(text = "Sign up to get started", fontSize = 14.sp, color = Color.Gray)

        Spacer(modifier = Modifier.height(40.dp))

        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it; displayNameError = null },
            label = { Text("Display Name") },
            isError = displayNameError != null,
            supportingText = { displayNameError?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it; usernameError = null },
            label = { Text("Username") },
            isError = usernameError != null,
            supportingText = { usernameError?.let { Text(it) } },
            keyboardOptions = KeyboardOptions(autoCorrect = false),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it; emailError = null },
            label = { Text("Email") },
            isError = emailError != null,
            supportingText = { emailError?.let { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it; passwordError = null },
            label = { Text("Password") },
            isError = passwordError != null,
            supportingText = { passwordError?.let { Text(it) } },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                var valid = true
                if (displayName.isBlank()) { displayNameError = "Display name is required"; valid = false }
                if (username.isBlank()) { usernameError = "Username is required"; valid = false }
                else if (username.contains(" ")) { usernameError = "Username cannot contain spaces"; valid = false }
                if (email.isBlank()) { emailError = "Email is required"; valid = false }
                if (password.isBlank()) { passwordError = "Password is required"; valid = false }
                else if (password.length < 6) { passwordError = "Password must be at least 6 characters"; valid = false }
                if (valid) viewModel.register(email.trim(), password.trim(), username.trim(), displayName.trim())
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Register")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) CircularProgressIndicator()

        if (authState is Resource.Error) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = (authState as Resource.Error).message ?: "An error occurred",
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        TextButton(onClick = onNavigateToLogin) {
            Text("Already have an account? Login", color = MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}