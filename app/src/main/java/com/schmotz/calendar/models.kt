package com.schmotz.calendar

import java.io.Serializable

data class UserProfile(
    val uid: String = "",
    val displayName: String = "",
    val householdCode: String = ""
) : Serializable

data class Event(
    val id: String = "",
    val title: String = "",
    val startEpochMillis: Long = 0L,
    val endEpochMillis: Long = 0L,
    val notes: String? = null,
    val personTag: String? = null
) : Serializable

data class SharedLink(
    val id: String = "",
    val title: String = "",
    val url: String = "",
    val description: String? = null,
    val imageUrl: String? = null,
    val sharedByUid: String = "",
    val sharedByName: String = "",
    val sharedAt: Long = System.currentTimeMillis()
) : Serializable
