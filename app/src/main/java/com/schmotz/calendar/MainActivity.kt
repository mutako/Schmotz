package com.schmotz.calendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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
    val profile by repo.observeProfile().collectAsState(initial = null)

    LaunchedEffect(Unit) {
        if (user == null) {
            // Use anonymous sign-in to avoid Play Services UI issues.
            auth.signInAnonymously().addOnCompleteListener {
                user = auth.currentUser
            }
        }
    }

    if (user == null) {
        // Minimal splash while we sign in anonymously
        androidx.compose.material3.Text("Signing in…")
    } else {
        HomeScreen(
            repo = repo,
            profile = profile,
            onSignOut = {
                auth.signOut()
                user = null
            }
        )
    }
}
