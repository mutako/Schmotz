package com.schmotz.calendar

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObjects
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class Repo(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    companion object {
        private const val USERS = "users"
        private const val EVENTS = "events"
        private const val LINKS = "links"
    }

    suspend fun ensureProfile(): UserProfile {
        val user = requireNotNull(auth.currentUser) { "Not signed in" }
        val docRef = db.collection(USERS).document(user.uid)
        val snap = docRef.get().await()
        return if (snap.exists()) {
            snap.toObject(UserProfile::class.java) ?: UserProfile(
                uid = user.uid,
                displayName = user.displayName ?: user.email.orEmpty(),
                householdCode = user.uid.take(6)
            )
        } else {
            val profile = UserProfile(
                uid = user.uid,
                displayName = user.displayName ?: user.email.orEmpty(),
                householdCode = user.uid.take(6)
            )
            docRef.set(profile).await()
            profile
        }
    }

    // --- Events (stubs you can expand later) ---
    suspend fun upsertEvent(profile: UserProfile, event: Event) {
        val col = db.collection(USERS).document(profile.uid).collection(EVENTS)
        val id = if (event.id.isBlank()) col.document().id else event.id
        col.document(id).set(event.copy(id = id)).await()
    }

    // --- Links (lightweight so Links UI compiles; you can replace with real query later) ---
    fun observeLinks(profile: UserProfile): Flow<List<SharedLink>> {
        // Replace with snapshotFlow if/when you add live Firestore listeners
        return flowOf(emptyList())
    }

    // Quick metadata “fetcher” to unblock ShareReceiver/ShareActivity use-sites.
    // Replace with a real parser if you want thumbs/titles from the web.
    suspend fun fetchMetadata(url: String): Triple<String, String?, String?> {
        val title = if (url.length > 60) url.take(57) + "..." else url
        val description: String? = null
        val imageUrl: String? = null
        return Triple(title, description, imageUrl)
    }
}
