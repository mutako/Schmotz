
package com.schmotz.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var displayName by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var info by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val p = loadProfile()
        profile = p
        if (p != null) {
            displayName = p.displayName
            code = p.householdCode
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = displayName, onValueChange = { displayName = it }, label = { Text("Account name") })
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Access code") })
        Spacer(Modifier.height(12.dp))
        Button(onClick = {
            scope.launch {
                val p = profile ?: return@launch
                val updated = p.copy(displayName = displayName, householdCode = code)
                saveProfile(updated)
                info = "Saved"
            }
        }) { Text("Save") }

        Spacer(Modifier.height(12.dp))
        Button(onClick = { Firebase.auth.signOut() }) { Text("Sign out") }

        info?.let { Text(it) }
    }
}
