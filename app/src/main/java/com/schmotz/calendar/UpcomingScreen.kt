package com.schmotz.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun UpcomingScreen(repo: FirestoreRepository, profile: UserProfile) {
    val household = profile.householdCode.ifBlank { "FAMILY" }
    val all by repo.observeAllEvents(household).collectAsState(initial = emptyList())
    val today = LocalDate.now()
    val currentMonth = YearMonth.from(today)
    val zone = remember { ZoneId.systemDefault() }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()) }
    val upcoming = all.filter {
        val d = FirestoreRepository.millisToLocalDate(it.startEpochMillis)
        !d.isBefore(today) && YearMonth.from(d) == currentMonth
    }.sortedBy { it.startEpochMillis }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("Upcoming", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (upcoming.isEmpty()) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = true)
                    .padding(top = 24.dp),
                verticalArrangement = Arrangement.Top
            ) {
                Text("No more events scheduled for this month yet.", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f, fill = true),
                contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(upcoming.size) { i ->
                    val ev = upcoming[i]
                    ElevatedCard {
                        Column(Modifier.padding(12.dp)) {
                            val start = Instant.ofEpochMilli(ev.startEpochMillis).atZone(zone)
                            val end = Instant.ofEpochMilli(ev.endEpochMillis).atZone(zone)
                            val datePart = start.toLocalDate().format(dateFormatter)
                            val detail = if (ev.allDay) {
                                "$datePart · All day"
                            } else if (start.toLocalDate() == end.toLocalDate()) {
                                val startTime = start.toLocalTime().format(timeFormatter)
                                val endTime = end.toLocalTime().format(timeFormatter)
                                "$datePart · $startTime – $endTime"
                            } else {
                                val endPart = end.toLocalDate().format(dateFormatter)
                                val startTime = start.toLocalTime().format(timeFormatter)
                                val endTime = end.toLocalTime().format(timeFormatter)
                                "$datePart $startTime → $endPart $endTime"
                            }
                            Text(ev.title, style = MaterialTheme.typography.titleMedium)
                            Text(detail, style = MaterialTheme.typography.bodySmall)
                            ev.notes?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
            }
        }
    }
}
