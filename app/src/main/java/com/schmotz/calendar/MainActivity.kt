package com.schmotz.calendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    if (user == null) {
        LoginScreen(
            auth = auth,
            onAuthenticated = { authenticated -> user = authenticated }
        )
    } else {
        AuthGate(repo = repo) { profile ->
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
}
