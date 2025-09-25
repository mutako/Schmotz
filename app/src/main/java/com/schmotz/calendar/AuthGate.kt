package com.schmotz.calendar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun AuthGate(
    repo: FirestoreRepository,
    content: @Composable (UserProfile) -> Unit
) {
    var loading by rememberSaveable { mutableStateOf(true) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        runCatching { repo.ensureProfile() }
            .onSuccess {
                profile = it
                loading = false
            }
            .onFailure {
                error = it.message ?: "Failed to load profile"
                profile = null
                loading = false
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
                            .onFailure {
                                error = it.message ?: "Failed to load profile"
                                profile = null
                                loading = false
                            }
                    }
                }) { Text("Retry") }
            }
        }
        profile != null -> content(profile!!)
    }
}
