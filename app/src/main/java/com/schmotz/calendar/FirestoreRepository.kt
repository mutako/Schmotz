package com.schmotz.calendar

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObjects
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class FirestoreRepository(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) {
    companion object {
        private const val USERS = "users"
        private const val HOUSEHOLDS = "households"
        private const val EVENTS = "events"
        private const val LINKS = "links"

        fun millisToLocalDate(millis: Long): LocalDate =
            Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
    }

    suspend fun ensureProfile(): UserProfile {
        val user = requireNotNull(auth.currentUser) { "Not signed in" }
        val docRef = db.collection(USERS).document(user.uid)
        val snapshot = docRef.get().await()
        if (snapshot.exists()) {
            return snapshot.toObject(UserProfile::class.java)
                ?: UserProfile(
                    uid = user.uid,
                    displayName = user.displayName ?: user.email.orEmpty(),
                    householdCode = user.uid.take(6)
                )
        }

        val profile = UserProfile(
            uid = user.uid,
            displayName = user.displayName ?: user.email.orEmpty(),
            householdCode = user.uid.take(6)
        )
        docRef.set(profile).await()
        return profile
    }

    fun observeAllEvents(householdCode: String): Flow<List<Event>> {
        if (householdCode.isBlank()) return flowOf(emptyList())
        return callbackFlow {
            val registration = db.collection(HOUSEHOLDS)
                .document(householdCode)
                .collection(EVENTS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    val events = snapshot?.toObjects<Event>().orEmpty()
                    trySend(events.sortedBy { it.startEpochMillis })
                }
            awaitClose { registration.remove() }
        }
    }

    fun observeLinks(profile: UserProfile): Flow<List<SharedLink>> {
        if (profile.householdCode.isBlank()) return flowOf(emptyList())
        return callbackFlow {
            val registration = db.collection(HOUSEHOLDS)
                .document(profile.householdCode)
                .collection(LINKS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    val links = snapshot?.toObjects<SharedLink>().orEmpty()
                    trySend(links.sortedByDescending { it.sharedAt })
                }
            awaitClose { registration.remove() }
        }
    }

    suspend fun upsertEvent(profile: UserProfile, event: Event) {
        val collection = db.collection(HOUSEHOLDS)
            .document(profile.householdCode)
            .collection(EVENTS)
        val id = if (event.id.isBlank()) collection.document().id else event.id
        collection.document(id).set(event.copy(id = id)).await()
    }

    suspend fun deleteEvent(profile: UserProfile, event: Event) {
        if (profile.householdCode.isBlank() || event.id.isBlank()) return
        db.collection(HOUSEHOLDS)
            .document(profile.householdCode)
            .collection(EVENTS)
            .document(event.id)
            .delete()
            .await()
    }
}
