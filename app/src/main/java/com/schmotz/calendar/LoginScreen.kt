package com.schmotz.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun LoginScreen(
    auth: FirebaseAuth,
    onAuthenticated: (FirebaseUser) -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var statusMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var isError by rememberSaveable { mutableStateOf(false) }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun runAuthOperation(operation: suspend () -> FirebaseUser?) {
        if (email.isBlank()) {
            statusMessage = "Please enter an email address."
            isError = true
            return
        }
        if (password.isBlank()) {
            statusMessage = "Please enter a password."
            isError = true
            return
        }
        scope.launch {
            isLoading = true
            statusMessage = null
            val result = runCatching { operation() }
            isLoading = false
            result.onSuccess { user ->
                if (user != null) {
                    statusMessage = null
                    onAuthenticated(user)
                } else {
                    statusMessage = "Authentication failed."
                    isError = true
                }
            }.onFailure { throwable ->
                statusMessage = friendlyAuthError(throwable)
                isError = true
            }
        }
    }

    fun sendResetEmail() {
        if (email.isBlank()) {
            statusMessage = "Enter your email to reset your password."
            isError = true
            return
        }
        scope.launch {
            isLoading = true
            statusMessage = null
            val result = runCatching { auth.sendPasswordResetEmail(email.trim()).await() }
            isLoading = false
            result.onSuccess {
                statusMessage = "Password reset email sent."
                isError = false
            }.onFailure { throwable ->
                statusMessage = friendlyAuthError(throwable)
                isError = true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Welcome to Schmotz",
                    style = MaterialTheme.typography.headlineSmall
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation()
                )
                if (!statusMessage.isNullOrBlank()) {
                    Text(
                        text = statusMessage!!,
                        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Button(
                    onClick = {
                        runAuthOperation {
                            auth.signInWithEmailAndPassword(email.trim(), password).await().user
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isLoading) "Please wait…" else "Log in")
                }
                Button(
                    onClick = {
                        runAuthOperation {
                            auth.createUserWithEmailAndPassword(email.trim(), password).await().user
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Create account")
                }
                TextButton(
                    onClick = { if (!isLoading) sendResetEmail() },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reset password")
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

private fun friendlyAuthError(throwable: Throwable): String {
    val firebaseCode = (throwable as? FirebaseAuthException)?.errorCode
    return when (firebaseCode) {
        "ERROR_INVALID_EMAIL" -> "The email address is badly formatted."
        "ERROR_EMAIL_ALREADY_IN_USE" -> "An account already exists for this email."
        "ERROR_USER_NOT_FOUND" -> "No account found for this email address."
        "ERROR_WRONG_PASSWORD" -> "The password is incorrect."
        "ERROR_USER_DISABLED" -> "This account has been disabled."
        else -> throwable.message ?: "Authentication failed"
    }
}
