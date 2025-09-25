package com.schmotz.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun UpcomingScreen(repo: FirestoreRepository, profile: UserProfile) {
    val household = profile.householdCode.ifBlank { "FAMILY" }
    val all by repo.observeAllEvents(household).collectAsState(initial = emptyList())
    val today = LocalDate.now()
    val upcoming = all.filter {
        val d = FirestoreRepository.millisToLocalDate(it.startEpochMillis)
        !d.isBefore(today)
    }.sortedBy { it.startEpochMillis }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("Upcoming", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        LazyColumn(
            contentPadding = PaddingValues(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(upcoming.size) { i ->
                val ev = upcoming[i]
                ElevatedCard {
                    Column(Modifier.padding(12.dp)) {
                        val dateStr = Instant.ofEpochMilli(ev.startEpochMillis)
                            .atZone(ZoneId.systemDefault()).toLocalDate().toString()
                        Text(ev.title, style = MaterialTheme.typography.titleMedium)
                        Text(dateStr, style = MaterialTheme.typography.bodySmall)
                        ev.notes?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        }
    }
}
