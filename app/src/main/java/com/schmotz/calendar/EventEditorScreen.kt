
package com.schmotz.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch

@Composable
fun EventEditorScreen(profile: UserProfile? = null) {
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf(TextFieldValue("")) }
    var desc by remember { mutableStateOf(TextFieldValue("")) }
    var start by remember { mutableStateOf(System.currentTimeMillis()) }
    var end by remember { mutableStateOf(System.currentTimeMillis() + 60*60*1000) }
    var info by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        OutlinedTextField(title, onValueChange = { title = it }, label = { Text("Title") })
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(desc, onValueChange = { desc = it }, label = { Text("Description") })
        Spacer(Modifier.height(8.dp))
        // (For brevity) Simple numeric inputs
        OutlinedTextField(value = start.toString(), onValueChange = { start = it.toLongOrNull() ?: start }, label = { Text("Start (epoch ms)") })
        OutlinedTextField(value = end.toString(), onValueChange = { end = it.toLongOrNull() ?: end }, label = { Text("End (epoch ms)") })
        Spacer(Modifier.height(12.dp))
        Button(onClick = {
            scope.launch {
                val p = profile ?: loadProfile() ?: return@launch
                val uid = Firebase.auth.currentUser?.uid ?: return@launch
                val ev = Event(
                    title = title.text,
                    description = desc.text,
                    start = start,
                    end = end,
                    createdByUid = uid,
                    createdByName = p.displayName,
                    createdAt = System.currentTimeMillis()
                )
                val ref = eventsCollection(p.householdCode).add(ev).await()
                info = "Saved: ${ref.id}"
            }
        }) { Text("Save") }
        if (info != null) Text(info!!)
    }
}
