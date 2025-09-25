package com.schmotz.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun SearchScreen(
    repo: FirestoreRepository,
    profile: UserProfile
) {
    var query by rememberSaveable { mutableStateOf("") }
    var mode by rememberSaveable { mutableStateOf(SearchMode.Events) }

    val events by repo.observeAllEvents(profile.householdCode).collectAsState(initial = emptyList())
    val links by repo.observeLinks(profile).collectAsState(initial = emptyList())

    val locale = remember { Locale.getDefault() }
    val zone = remember { ZoneId.systemDefault() }
    val dateFormatter = remember(locale) { DateTimeFormatter.ofPattern("MMM d yyyy", locale) }
    val timeFormatter = remember(locale) { DateTimeFormatter.ofPattern("HH:mm", locale) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text(if (mode == SearchMode.Events) "Search events" else "Search links") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilterChip(
                selected = mode == SearchMode.Events,
                onClick = { mode = SearchMode.Events },
                label = { Text("Events") }
            )
            FilterChip(
                selected = mode == SearchMode.Links,
                onClick = { mode = SearchMode.Links },
                label = { Text("Links") }
            )
        }
        Spacer(Modifier.height(16.dp))

        val trimmed = query.trim()
        val normalized = trimmed.lowercase(locale)

        val eventResults = remember(normalized, events) {
            if (normalized.isBlank()) {
                emptyList<Event>()
            } else {
                events.filter { event ->
                    val fields = listOf(
                        event.title,
                        event.notes.orEmpty(),
                        event.personTag.orEmpty()
                    ).joinToString(" ").lowercase(locale)
                    fields.contains(normalized)
                }.sortedBy { it.startEpochMillis }
            }
        }

        val linkResults = remember(normalized, links) {
            if (normalized.isBlank()) {
                emptyList<SharedLink>()
            } else {
                links.filter { link ->
                    listOf(
                        link.title,
                        link.url,
                        link.description.orEmpty(),
                        link.category
                    ).joinToString(" ")
                        .lowercase(locale)
                        .contains(normalized)
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f, fill = true)
                .fillMaxWidth()
        ) {
            when {
                trimmed.isBlank() -> {
                    Text("Type to search your shared events or links.")
                }
                mode == SearchMode.Events && eventResults.isEmpty() -> {
                    Text("No matching events found.")
                }
                mode == SearchMode.Links && linkResults.isEmpty() -> {
                    Text("No matching links found.")
                }
                mode == SearchMode.Events -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(eventResults) { event ->
                            EventResultCard(event, zone, dateFormatter, timeFormatter)
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(linkResults) { link ->
                            LinkCard(link)
                        }
                    }
                }
            }
        }
    }
}

private enum class SearchMode { Events, Links }

@Composable
private fun EventResultCard(
    event: Event,
    zone: ZoneId,
    dateFormatter: DateTimeFormatter,
    timeFormatter: DateTimeFormatter
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(event.title.ifBlank { "(Untitled event)" })

            val start = Instant.ofEpochMilli(event.startEpochMillis).atZone(zone)
            val end = Instant.ofEpochMilli(event.endEpochMillis).atZone(zone)
            val datePart = start.toLocalDate().format(dateFormatter)
            val detail = if (event.allDay) {
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

            Spacer(Modifier.height(4.dp))
            Text(detail)

            event.notes?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(4.dp))
                Text(it)
            }
        }
    }
}
