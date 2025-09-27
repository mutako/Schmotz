package com.schmotz.calendar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun EventEditorScreen(profile: UserProfile? = null) {
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var startText by remember { mutableStateOf(System.currentTimeMillis().toString()) }
    var endText by remember { mutableStateOf((System.currentTimeMillis() + 60 * 60 * 1000).toString()) }
    var info by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") })
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") })
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = startText,
            onValueChange = { startText = it },
            label = { Text("Start (epoch ms)") }
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = endText,
            onValueChange = { endText = it },
            label = { Text("End (epoch ms)") }
        )
        Spacer(Modifier.height(12.dp))
        Button(onClick = {
            scope.launch {
                info = null
                error = null
                val activeProfile = profile ?: loadProfile()
                if (activeProfile == null) {
                    error = "Please sign in first"
                    return@launch
                }
                val startMillis = startText.toLongOrNull()
                val endMillis = endText.toLongOrNull()
                if (title.isBlank()) {
                    error = "Title required"
                    return@launch
                }
                if (startMillis == null || endMillis == null) {
                    error = "Invalid time values"
                    return@launch
                }
                if (endMillis < startMillis) {
                    error = "End must be after start"
                    return@launch
                }
                runCatching {
                    val doc = eventsCollection(activeProfile.householdCode).document()
                    val event = Event(
                        id = doc.id,
                        title = title.trim(),
                        startEpochMillis = startMillis,
                        endEpochMillis = endMillis,
                        notes = notes.trim().takeIf { it.isNotBlank() }
                    )
                    doc.set(event).await()
                }.onSuccess {
                    info = "Saved"
                    title = ""
                    notes = ""
                }.onFailure {
                    error = it.message ?: "Failed to save"
                }
            }
        }) { Text("Save") }
        info?.let { Text(it) }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}
