package com.schmotz.calendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var repo: FirestoreRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        repo = FirestoreRepository(auth, FirebaseFirestore.getInstance())

        setContent {
            MaterialTheme {
                Surface {
                    AppRoot(auth = auth, repo = repo)
                }
            }
        }
    }
}

@Composable
private fun AppRoot(auth: FirebaseAuth, repo: FirestoreRepository) {
    var user by remember { mutableStateOf(auth.currentUser) }
    var isSigningIn by remember { mutableStateOf(user == null) }
    var error by remember { mutableStateOf<String?>(null) }
    var authAttempt by remember { mutableStateOf(0) }

    LaunchedEffect(user, authAttempt) {
        if (user == null) {
            isSigningIn = true
            error = null
            runCatching { auth.signInAnonymously().await() }
                .onSuccess { result ->
                    user = result.user ?: auth.currentUser
                }
                .onFailure { error = it.message ?: "Sign-in failed" }
            isSigningIn = false
        } else {
            isSigningIn = false
            error = null
        }
    }

    when {
        user == null && isSigningIn -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        user == null -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(error ?: "Unable to sign in")
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { authAttempt++ }) {
                        Text("Retry")
                    }
                }
            }
        }
        else -> {
            AuthGate(repo = repo) { profile ->
                HomeScreen(
                    repo = repo,
                    profile = profile,
                    onSignOut = {
                        auth.signOut()
                        user = null
                        authAttempt++
                    }
                )
            }
        }
    }
}
