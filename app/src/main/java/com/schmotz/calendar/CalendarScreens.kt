package com.schmotz.calendar

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Visual month calendar with:
 * - Today highlighted
 * - Tap any date to review or schedule events for that day
 * - Support for timed or all-day events with optional repeat rules
 *
 * IMPORTANT:
 * Keep atStartOfDayMillis() and atEndOfDayMillis() ONLY in Utils.kt.
 * Do NOT duplicate them in Data.kt or here.
 */

@Composable
fun CalendarScreen(
    repo: FirestoreRepository,
    profile: UserProfile
) {
    var currentYearMonth by remember { mutableStateOf(YearMonth.now()) }
    val today = remember { LocalDate.now() }
    var selectedDate by remember { mutableStateOf(today) }

    val events by repo.observeAllEvents(profile.householdCode).collectAsState(initial = emptyList())
    val eventsByDate = remember(events) {
        events
            .groupBy { FirestoreRepository.millisToLocalDate(it.startEpochMillis) }
            .mapValues { (_, dayEvents) -> dayEvents.sortedBy { it.startEpochMillis } }
    }

    var overviewDate by remember { mutableStateOf<LocalDate?>(null) }
    var editingDate by remember { mutableStateOf<LocalDate?>(null) }
    var newTitle by remember { mutableStateOf("") }
    var isAllDay by remember { mutableStateOf(true) }
    var startTime by remember { mutableStateOf(LocalTime.of(9, 0)) }
    var endTime by remember { mutableStateOf(LocalTime.of(10, 0)) }
    var repeatFrequency by remember { mutableStateOf(RepeatFrequency.NONE) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var showRepeatChooser by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault()) }

    fun beginCreateEvent(forDate: LocalDate) {
        editingDate = forDate
        newTitle = ""
        isAllDay = true
        startTime = LocalTime.of(9, 0)
        endTime = LocalTime.of(10, 0)
        repeatFrequency = RepeatFrequency.NONE
        validationError = null
    }

    fun showTimePicker(initial: LocalTime, onTimeSelected: (LocalTime) -> Unit) {
        TimePickerDialog(
            context,
            { _, hour, minute -> onTimeSelected(LocalTime.of(hour, minute)) },
            initial.hour,
            initial.minute,
            false
        ).show()
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { currentYearMonth = currentYearMonth.minusMonths(1) }) { Text("<") }
            Spacer(Modifier.width(12.dp))
            Text(
                text = "${currentYearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentYearMonth.year}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.width(12.dp))
            Button(onClick = { currentYearMonth = currentYearMonth.plusMonths(1) }) { Text(">") }
            Spacer(Modifier.weight(1f))
            Button(onClick = { beginCreateEvent(selectedDate) }) { Text("Add event") }
        }

        Spacer(Modifier.height(8.dp))
        Divider()
        Spacer(Modifier.height(8.dp))

        val firstDayOfWeek = DayOfWeek.MONDAY
        WeekdayHeader(firstDayOfWeek)

        Spacer(Modifier.height(4.dp))

        MonthGrid(
            yearMonth = currentYearMonth,
            firstDayOfWeek = firstDayOfWeek,
            selectedDate = selectedDate,
            today = today,
            onSelect = { date ->
                selectedDate = date
                val dayEvents = eventsByDate[date].orEmpty()
                if (dayEvents.isNotEmpty()) {
                    overviewDate = date
                } else {
                    beginCreateEvent(date)
                }
            }
        )

        Spacer(Modifier.height(16.dp))
        Divider()
        Spacer(Modifier.height(8.dp))

        Text(
            "Events on $selectedDate",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))

        val dayEvents = eventsByDate[selectedDate].orEmpty()
        if (dayEvents.isEmpty()) {
            Text("No events yet.")
        } else {
            LazyColumn {
                items(dayEvents) { ev -> EventCard(ev) }
            }
        }
    }

    overviewDate?.let { date ->
        val dayEvents = eventsByDate[date].orEmpty()
        AlertDialog(
            onDismissRequest = { overviewDate = null },
            title = { Text(date.format(dateFormatter)) },
            text = {
                if (dayEvents.isEmpty()) {
                    Text("No events yet.")
                } else {
                    Column(Modifier.heightIn(max = 320.dp)) {
                        dayEvents.forEach { event ->
                            EventCard(event)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { overviewDate = null }) { Text("Close") }
            },
            dismissButton = {
                TextButton(onClick = {
                    overviewDate = null
                    beginCreateEvent(date)
                }) { Text("Add event") }
            }
        )
    }

    editingDate?.let { date ->
        AlertDialog(
            onDismissRequest = { editingDate = null },
            title = { Text(date.format(dateFormatter)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = {
                            newTitle = it
                            if (validationError != null) validationError = null
                        },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("All-day", style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.weight(1f))
                        Switch(
                            checked = isAllDay,
                            onCheckedChange = { isAllDay = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                        )
                    }
                    if (!isAllDay) {
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
                                    "${repeatFrequency.displayName}",
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
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val trimmedTitle = newTitle.trim()
                    if (trimmedTitle.isEmpty()) {
                        validationError = "Please enter a title"
                        return@TextButton
                    }
                    val (startMillis, endMillis) = if (isAllDay) {
                        date.atStartOfDayMillis() to date.atEndOfDayMillis()
                    } else {
                        if (!endTime.isAfter(startTime)) {
                            validationError = "End time must be after start time"
                            return@TextButton
                        }
                        date.atTimeMillis(startTime) to date.atTimeMillis(endTime)
                    }
                    validationError = null
                    scope.launch {
                        runCatching {
                            val event = Event(
                                id = "",
                                title = trimmedTitle,
                                startEpochMillis = startMillis,
                                endEpochMillis = endMillis,
                                allDay = isAllDay,
                                repeatFrequency = repeatFrequency
                            )
                            repo.upsertEvent(profile, event)
                        }.onSuccess {
                            newTitle = ""
                            repeatFrequency = RepeatFrequency.NONE
                            editingDate = null
                        }.onFailure {
                            validationError = it.message ?: "Unable to save event"
                        }
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { editingDate = null }) { Text("Cancel") }
            }
        )
    }

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
}

@Composable
private fun WeekdayHeader(firstDayOfWeek: DayOfWeek) {
    val days = DayOfWeek.entries
        .dropWhile { it != firstDayOfWeek } + DayOfWeek.entries.takeWhile { it != firstDayOfWeek }

    Row(Modifier.fillMaxWidth()) {
        days.forEach { dow ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = dow.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun MonthGrid(
    yearMonth: YearMonth,
    firstDayOfWeek: DayOfWeek,
    selectedDate: LocalDate,
    today: LocalDate,
    onSelect: (LocalDate) -> Unit
) {
    val cells = remember(yearMonth, firstDayOfWeek) { buildMonthCells(yearMonth, firstDayOfWeek) }

    Column {
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    val isToday = date == today
                    val isSelected = date == selectedDate
                    val withinMonth = date.month == yearMonth.month

                    val bg = when {
                        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        isToday -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (withinMonth) 0.08f else 0.03f)
                    }
                    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(2.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(bg)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = borderColor,
                                shape = MaterialTheme.shapes.small
                            )
                            .clickable { onSelect(date) },
                        contentAlignment = Alignment.TopEnd
                    ) {
                        Text(
                            text = date.dayOfMonth.toString(),
                            modifier = Modifier.padding(6.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (withinMonth) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }
}

private fun buildMonthCells(
    yearMonth: YearMonth,
    firstDayOfWeek: DayOfWeek
): List<LocalDate> {
    val firstOfMonth = yearMonth.atDay(1)
    var firstCell = firstOfMonth
    while (firstCell.dayOfWeek != firstDayOfWeek) {
        firstCell = firstCell.minusDays(1)
    }
    // Keep a stable grid (6 rows × 7 days)
    return (0 until 42).map { firstCell.plusDays(it.toLong()) }
}

@Composable
private fun EventCard(event: Event) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(event.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                formatEventTimeRange(event),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (event.repeatFrequency != RepeatFrequency.NONE) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Repeat: ${event.repeatFrequency.displayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!event.notes.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(event.notes!!, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun formatEventTimeRange(event: Event): String {
    val zone = ZoneId.systemDefault()
    val start = Instant.ofEpochMilli(event.startEpochMillis).atZone(zone)
    val end = Instant.ofEpochMilli(event.endEpochMillis).atZone(zone)
    val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

    if (event.allDay) {
        val date = start.toLocalDate().format(dateFormatter)
        return "$date  All day"
    }

    return if (start.toLocalDate() == end.toLocalDate()) {
        val date = start.toLocalDate().format(dateFormatter)
        val startTimeText = start.toLocalTime().format(timeFormatter)
        val endTimeText = end.toLocalTime().format(timeFormatter)
        "$date  $startTimeText – $endTimeText"
    } else {
        val startText = "${start.toLocalDate().format(dateFormatter)} ${start.toLocalTime().format(timeFormatter)}"
        val endText = "${end.toLocalDate().format(dateFormatter)} ${end.toLocalTime().format(timeFormatter)}"
        "$startText → $endText"
    }
}
