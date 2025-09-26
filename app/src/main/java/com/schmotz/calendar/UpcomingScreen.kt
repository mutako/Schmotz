@file:OptIn(ExperimentalLayoutApi::class)

package com.schmotz.calendar

import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun UpcomingScreen(
    repo: FirestoreRepository,
    profile: UserProfile
) {
    val household = profile.householdCode.ifBlank { "FAMILY" }
    val all by repo.observeAllEvents(household).collectAsState(initial = emptyList())
    val zone = remember { ZoneId.systemDefault() }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()) }
    val nowMillis = System.currentTimeMillis()
    var selectedEvent by remember { mutableStateOf<Event?>(null) }

    val upcoming = all
        .distinctBy { it.id }
        .filter { event -> event.endEpochMillis >= nowMillis }
        .sortedBy { it.startEpochMillis }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("Upcoming", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (upcoming.isEmpty()) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = true)
                    .padding(top = 24.dp),
                verticalArrangement = Arrangement.Top
            ) {
                Text("No upcoming events yet.", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f, fill = true),
                contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(upcoming.size) { i ->
                    val ev = upcoming[i]
                    ElevatedCard(
                        modifier = Modifier.clickable { selectedEvent = ev }
                    ) {
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

    selectedEvent?.let { event ->
        UpcomingEventDialog(
            event = event,
            repo = repo,
            profile = profile,
            onDismiss = { selectedEvent = null }
        )
    }
}

@Composable
private fun UpcomingEventDialog(
    event: Event,
    repo: FirestoreRepository,
    profile: UserProfile,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme
    val eventColorChoices = remember(colorScheme) {
        listOf(
            colorScheme.primary,
            colorScheme.secondary,
            colorScheme.tertiary,
            colorScheme.inversePrimary,
            colorScheme.error,
            Color(0xFF00897B),
            Color(0xFF6D4C41)
        )
    }

    val zone = remember { ZoneId.systemDefault() }
    val startZoned = remember(event.id) { Instant.ofEpochMilli(event.startEpochMillis).atZone(zone) }
    val endZoned = remember(event.id) { Instant.ofEpochMilli(event.endEpochMillis).atZone(zone) }
    val eventDate = remember(event.id) { startZoned.toLocalDate() }

    var title by remember(event.id) { mutableStateOf(event.title) }
    var isAllDay by remember(event.id) { mutableStateOf(event.allDay) }
    var startTime by remember(event.id) {
        mutableStateOf(
            if (event.allDay) LocalTime.of(9, 0) else startZoned.toLocalTime()
        )
    }
    var endTime by remember(event.id) {
        mutableStateOf(
            if (event.allDay) LocalTime.of(10, 0) else endZoned.toLocalTime()
        )
    }
    var repeatFrequency by remember(event.id) { mutableStateOf(event.repeatFrequency) }
    var selectedColorInt by remember(event.id) { mutableStateOf(eventColorInt(event, colorScheme.primary)) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var showRepeatChooser by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault()) }

    fun showTimePicker(initial: LocalTime, onTimeSelected: (LocalTime) -> Unit) {
        TimePickerDialog(
            context,
            { _, hour, minute -> onTimeSelected(LocalTime.of(hour, minute)) },
            initial.hour,
            initial.minute,
            true
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(eventDate.format(dateFormatter)) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (validationError != null) validationError = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Title") },
                    singleLine = true
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("All-day", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.weight(1f))
                    Switch(
                        checked = isAllDay,
                        onCheckedChange = { isAllDay = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = colorScheme.primary)
                    )
                }
                if (!isAllDay) {
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Start", style = MaterialTheme.typography.labelMedium)
                            Spacer(Modifier.height(4.dp))
                            OutlinedButton(onClick = { showTimePicker(startTime) { startTime = it } }) {
                                Text(startTime.format(timeFormatter))
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("End", style = MaterialTheme.typography.labelMedium)
                            Spacer(Modifier.height(4.dp))
                            OutlinedButton(onClick = { showTimePicker(endTime) { endTime = it } }) {
                                Text(endTime.format(timeFormatter))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Color", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                ColorPickerRow(
                    colors = eventColorChoices,
                    selectedColorInt = selectedColorInt,
                    onSelect = { selectedColorInt = it }
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = repeatFrequency != RepeatFrequency.NONE,
                        onCheckedChange = { checked ->
                            if (checked) {
                                if (repeatFrequency == RepeatFrequency.NONE) {
                                    repeatFrequency = RepeatFrequency.DAILY
                                }
                                showRepeatChooser = true
                            } else {
                                repeatFrequency = RepeatFrequency.NONE
                            }
                        }
                    )
                    Column {
                        Text("Repeat", style = MaterialTheme.typography.bodyLarge)
                        if (repeatFrequency != RepeatFrequency.NONE) {
                            Text(
                                repeatFrequency.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                validationError?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
                if (event.id.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    TextButton(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Delete")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val trimmed = title.trim()
                if (trimmed.isEmpty()) {
                    validationError = "Please enter a title"
                    return@TextButton
                }
                val (startMillis, endMillis) = if (isAllDay) {
                    eventDate.atStartOfDayMillis() to eventDate.atEndOfDayMillis()
                } else {
                    if (!endTime.isAfter(startTime)) {
                        validationError = "End time must be after start time"
                        return@TextButton
                    }
                    eventDate.atTimeMillis(startTime) to eventDate.atTimeMillis(endTime)
                }
                val chosenColor = if (selectedColorInt == 0) colorScheme.primary.toArgb() else selectedColorInt
                validationError = null
                scope.launch {
                    val updated = event.copy(
                        title = trimmed,
                        startEpochMillis = startMillis,
                        endEpochMillis = endMillis,
                        allDay = isAllDay,
                        repeatFrequency = repeatFrequency,
                        colorArgb = colorIntToLong(chosenColor)
                    )
                    val result = runCatching { repo.upsertEvent(profile, updated) }
                    result.onSuccess { onDismiss() }
                        .onFailure { error ->
                            validationError = error.message ?: "Unable to save event"
                        }
                }
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showRepeatChooser) {
        AlertDialog(
            onDismissRequest = { showRepeatChooser = false },
            title = { Text("Repeat how often?") },
            text = {
                Column {
                    RepeatFrequency.entries
                        .filter { it != RepeatFrequency.NONE }
                        .forEach { frequency ->
                            TextButton(onClick = {
                                repeatFrequency = frequency
                                showRepeatChooser = false
                            }) {
                                Text(frequency.displayName)
                            }
                        }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRepeatChooser = false }) { Text("Close") }
            },
            dismissButton = {
                TextButton(onClick = {
                    repeatFrequency = RepeatFrequency.NONE
                    showRepeatChooser = false
                }) { Text("No repeat") }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete this event?") },
            text = {
                Text(
                    text = "Are you sure you want to remove \"${event.title}\"?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            val result = runCatching { repo.deleteEvent(profile, event) }
                            showDeleteConfirm = false
                            result.onSuccess { onDismiss() }
                                .onFailure { error ->
                                    validationError = error.message ?: "Unable to delete event"
                                }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}
