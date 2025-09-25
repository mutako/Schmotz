
package com.schmotz.calendar

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object FirebaseRefs {
    val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
}

suspend fun loadProfile(): UserProfile? {
    val uid = FirebaseRefs.auth.currentUser?.uid ?: return null
    val doc = FirebaseRefs.db.collection("users").document(uid).get().await()
    return doc.toObject(UserProfile::class.java)
}

suspend fun saveProfile(profile: UserProfile) {
    FirebaseRefs.db.collection("users").document(profile.uid).set(profile).await()
}

fun eventsCollection(householdCode: String) =
    FirebaseRefs.db.collection("households").document(householdCode).collection("events")

fun linksCollection(householdCode: String) =
    FirebaseRefs.db.collection("households").document(householdCode).collection("links")
