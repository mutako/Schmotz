
package com.schmotz.calendar

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class ShareReceiverActivity : Activity() {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val action = intent?.action
        val type = intent?.type

        if (Intent.ACTION_SEND == action && type != null) {
            handleSend(intent)
        } else {
            finish()
        }
    }

    private fun handleSend(intent: Intent) {
        val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
        val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)

        scope.launch {
            val user = Firebase.auth.currentUser
            val profile = loadProfile()
            if (user == null || profile == null) {
                Toast.makeText(this@ShareReceiverActivity, "Please open the app and sign in first.", Toast.LENGTH_LONG).show()
                finish(); return@launch
            }
            val url = when {
                text.startsWith("http") -> text
                uri != null -> uri.toString()
                else -> text
            }

            val (title, desc, image) = fetchMetadata(url)
            val link = SharedLink(
                url = url,
                title = title,
                description = desc,
                imageUrl = image,
                category = "",
                sharedByUid = user.uid,
                sharedByName = profile.displayName,
                sharedAt = System.currentTimeMillis()
            )
            linksCollection(profile.householdCode).add(link).await()
            Toast.makeText(this@ShareReceiverActivity, "Saved to Schmotz", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
