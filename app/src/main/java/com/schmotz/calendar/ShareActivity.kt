package com.schmotz.calendar

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.regex.Pattern

class ShareActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val repo by lazy { FirestoreRepository(auth, FirebaseFirestore.getInstance()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Only handle ACTION_SEND text.
        if (intent?.action != Intent.ACTION_SEND || intent.type != "text/plain") {
            finishWithOk()
            return
        }

        val rawText = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
        val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT).orEmpty()

        // Parse out the first URL if any.
        val url = firstUrl(rawText) ?: firstUrl(subject) ?: ""
        val title = if (subject.isNotBlank()) subject else rawText.take(80)
        val desc = if (rawText != url) rawText.take(500) else null

        // Require sign-in (same behavior as your compose screens).
        if (auth.currentUser == null) {
            // Tell the user to sign in in the main app first.
            startActivity(
                packageManager.getLaunchIntentForPackage(packageName)
                    ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            finishWithOk()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val profile = repo.ensureProfile()
                val (metaTitle, metaDesc, metaImage) = if (url.isNotBlank()) {
                    fetchMetadata(url)
                } else {
                    Triple(title.ifBlank { url }, desc, null)
                }
                val finalTitle = metaTitle.ifBlank { title.ifBlank { url } }
                val finalDesc = metaDesc ?: desc
                val collection = FirebaseFirestore.getInstance()
                    .collection("households").document(profile.householdCode)
                    .collection("links")
                val doc = collection.document()
                val link = SharedLink(
                    id = doc.id,
                    title = finalTitle,
                    url = url,
                    description = finalDesc,
                    imageUrl = metaImage,
                    sharedByUid = auth.currentUser?.uid.orEmpty(),
                    sharedByName = auth.currentUser?.displayName ?: auth.currentUser?.email.orEmpty(),
                    sharedAt = System.currentTimeMillis()
                )
                // Store under household links collection.
                doc.set(link).await()
            } catch (e: Exception) {
                // Swallow and just finish; could log if you add Crashlytics.
            } finally {
                finishWithOk()
            }
        }
    }

    private fun finishWithOk() {
        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun firstUrl(text: String): String? {
        val p = Pattern.compile("(https?://\\S+)")
        val m = p.matcher(text)
        return if (m.find()) m.group(1) else null
    }
}
