package com.schmotz.calendar

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    repo: Repo,
    profile: UserProfile
) {
    var tab by rememberSaveable { mutableStateOf(0) }
    val tabs = listOf("Calendar", "Upcoming", "Search")

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Schmotz Calendar") })
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            TabRow(selectedTabIndex = tab) {
                tabs.forEachIndexed { i, title ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(title) })
                }
            }
            when (tab) {
                0 -> CalendarScreen(repo = repo, profile = profile)
                1 -> UpcomingScreen(repo = repo, profile = profile)
                2 -> SearchScreen(repo = repo, profile = profile)
            }
        }
    }
}
