package com.schmotz.calendar

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun AuthGate(
    repo: Repo,
    content: @Composable (UserProfile) -> Unit
) {
    var loading by rememberSaveable { mutableStateOf(true) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            runCatching { repo.ensureProfile() }
                .onSuccess {
                    profile = it
                    loading = false
                }
                .onFailure {
                    error = it.message ?: "Failed to load profile"
                    loading = false
                }
        }
    }

    when {
        loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Error: $error")
                Spacer(Modifier.height(12.dp))
                Button(onClick = {
                    loading = true; error = null
                    scope.launch {
                        runCatching { repo.ensureProfile() }
                            .onSuccess { profile = it; loading = false }
                            .onFailure { error = it.message; loading = false }
                    }
                }) { Text("Retry") }
            }
        }
        profile != null -> content(profile!!)
    }
}
