package com.schmotz.calendar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    repo: FirestoreRepository,
    profile: UserProfile,
    onSignOut: () -> Unit
) {
    var tab by rememberSaveable { mutableStateOf(0) }
    val tabs = listOf("Calendar", "Upcoming", "Links", "Search")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Schmotz Calendar") },
                actions = {
                    TextButton(onClick = onSignOut) { Text("Sign out") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tab) {
                tabs.forEachIndexed { i, title ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(title) })
                }
            }
            when (tab) {
                0 -> CalendarScreen(repo = repo, profile = profile)
                1 -> UpcomingScreen(repo = repo, profile = profile)
                2 -> LinksScreen(repo = repo, profile = profile)
                else -> SearchScreen(repo = repo, profile = profile)
            }
        }
    }
}
