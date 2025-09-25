package com.schmotz.calendar

import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.*

@Composable
fun LinksScreen(
    repo: Repo,
    profile: UserProfile
) {
    // Placeholder – replace with observeLinks(repo.observeLinks(profile)) when ready
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Shared links will appear here.")
    }
}
