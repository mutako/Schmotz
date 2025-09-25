package com.schmotz.calendar

import android.app.TimePickerDialog
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import java.time.temporal.ChronoUnit

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
    val eventsByDate = events
        .groupBy { FirestoreRepository.millisToLocalDate(it.startEpochMillis) }
        .mapValues { (_, dayEvents) -> dayEvents.sortedBy { it.startEpochMillis } }
    val monthEventsByDate = eventsByDate.entries
        .filter { (date, _) ->
            date.year == currentYearMonth.year && date.month == currentYearMonth.month
        }
        .sortedBy { it.key }

    val colorScheme = MaterialTheme.colorScheme
    val defaultEventColor = colorScheme.primary
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

    var overviewDate by remember { mutableStateOf<LocalDate?>(null) }
    var editingDate by remember { mutableStateOf<LocalDate?>(null) }
    var editingEvent by remember { mutableStateOf<Event?>(null) }
    var newTitle by remember { mutableStateOf("") }
    var isAllDay by remember { mutableStateOf(false) }
    var startTime by remember { mutableStateOf(LocalTime.of(9, 0)) }
    var endTime by remember { mutableStateOf(LocalTime.of(10, 0)) }
    var repeatFrequency by remember { mutableStateOf(RepeatFrequency.NONE) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var showRepeatChooser by remember { mutableStateOf(false) }
    var showMonthOverview by remember { mutableStateOf(false) }
    var selectedColorInt by remember { mutableStateOf(0) }
    var pendingDeleteEvent by remember { mutableStateOf<Event?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault()) }

    fun beginCreateEvent(forDate: LocalDate) {
        editingDate = forDate
        editingEvent = null
        newTitle = ""
        isAllDay = false
        startTime = LocalTime.of(9, 0)
        endTime = LocalTime.of(10, 0)
        repeatFrequency = RepeatFrequency.NONE
        validationError = null
        selectedColorInt = defaultEventColor.toArgb()
    }

    fun beginEditEvent(event: Event) {
        val zone = ZoneId.systemDefault()
        val start = Instant.ofEpochMilli(event.startEpochMillis).atZone(zone)
        val end = Instant.ofEpochMilli(event.endEpochMillis).atZone(zone)

        editingEvent = event
        editingDate = start.toLocalDate()
        newTitle = event.title
        isAllDay = event.allDay
        if (event.allDay) {
            startTime = LocalTime.of(9, 0)
            endTime = LocalTime.of(10, 0)
        } else {
            startTime = start.toLocalTime()
            endTime = end.toLocalTime()
        }
        repeatFrequency = event.repeatFrequency
        validationError = null
        selectedColorInt = eventColorInt(event, defaultEventColor)
    }

    fun showTimePicker(initial: LocalTime, onTimeSelected: (LocalTime) -> Unit) {
        TimePickerDialog(
            context,
            { _, hour, minute -> onTimeSelected(LocalTime.of(hour, minute)) },
            initial.hour,
            initial.minute,
            true
        ).show()
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { currentYearMonth = currentYearMonth.minusMonths(1) }) { Text("<") }
            Spacer(Modifier.width(12.dp))
            Text(
                text = "${currentYearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentYearMonth.year}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { showMonthOverview = true }
            )
            Spacer(Modifier.width(12.dp))
            Button(onClick = { currentYearMonth = currentYearMonth.plusMonths(1) }) { Text(">") }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { beginCreateEvent(selectedDate) }) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "Add event")
            }
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
            eventsByDate = eventsByDate,
            defaultEventColor = defaultEventColor,
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
            Text(
                "No events scheduled yet for this day.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                dayEvents.forEach { event ->
                    EventCard(
                        event = event,
                        defaultColor = defaultEventColor,
                        onEventClick = { beginEditEvent(it) }
                    )
                }
            }
        }
    }

    overviewDate?.let { date ->
        val dayEvents = eventsByDate[date].orEmpty()
        AlertDialog(
            onDismissRequest = { overviewDate = null },
            title = { Text(date.format(dateFormatter)) },
            text = {
                DaySchedule(
                    date = date,
                    events = dayEvents,
                    defaultEventColor = defaultEventColor,
                    onEventClick = { event ->
                        overviewDate = null
                        beginEditEvent(event)
                    }
                )
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
            onDismissRequest = {
                editingDate = null
                editingEvent = null
                validationError = null
            },
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
                    editingEvent?.takeIf { it.id.isNotBlank() }?.let {
                        Spacer(Modifier.height(12.dp))
                        TextButton(
                            onClick = { pendingDeleteEvent = it },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Delete")
                        }
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
                    val chosenColorInt = if (selectedColorInt == 0) defaultEventColor.toArgb() else selectedColorInt
                    validationError = null
                    scope.launch {
                        runCatching {
                            val base = editingEvent
                            val event = (base ?: Event()).copy(
                                title = trimmedTitle,
                                startEpochMillis = startMillis,
                                endEpochMillis = endMillis,
                                allDay = isAllDay,
                                repeatFrequency = repeatFrequency,
                                colorArgb = colorIntToLong(chosenColorInt)
                            )
                            repo.upsertEvent(profile, event)
                        }.onSuccess {
                            newTitle = ""
                            repeatFrequency = RepeatFrequency.NONE
                            editingDate = null
                            editingEvent = null
                        }.onFailure {
                            validationError = it.message ?: "Unable to save event"
                        }
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = {
                    editingDate = null
                    editingEvent = null
                    validationError = null
                }) { Text("Cancel") }
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

    pendingDeleteEvent?.let { event ->
        AlertDialog(
            onDismissRequest = { pendingDeleteEvent = null },
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
                            pendingDeleteEvent = null
                            if (result.isSuccess) {
                                if (editingEvent?.id == event.id) {
                                    editingEvent = null
                                    editingDate = null
                                    validationError = null
                                }
                            } else {
                                validationError = result.exceptionOrNull()?.message
                                    ?: "Unable to delete event"
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteEvent = null }) { Text("Cancel") }
            }
        )
    }

    if (showMonthOverview) {
        val monthTitle = "${currentYearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentYearMonth.year}"
        AlertDialog(
            onDismissRequest = { showMonthOverview = false },
            title = { Text(monthTitle) },
            text = {
                if (monthEventsByDate.isEmpty()) {
                    Text("No events this month.")
                } else {
                    Column(
                        Modifier
                            .heightIn(max = 400.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        monthEventsByDate.forEach { (date, eventsForDay) ->
                            Text(
                                date.format(dateFormatter),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(4.dp))
                            eventsForDay.forEach { event ->
                                EventCard(
                                    event = event,
                                    defaultColor = defaultEventColor,
                                    onEventClick = { selected ->
                                        showMonthOverview = false
                                        beginEditEvent(selected)
                                    }
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMonthOverview = false }) { Text("Close") }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MonthGrid(
    yearMonth: YearMonth,
    firstDayOfWeek: DayOfWeek,
    selectedDate: LocalDate,
    today: LocalDate,
    eventsByDate: Map<LocalDate, List<Event>>,
    defaultEventColor: Color,
    onSelect: (LocalDate) -> Unit
) {
    val cells = remember(yearMonth, firstDayOfWeek) { buildMonthCells(yearMonth, firstDayOfWeek) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(cells) { cell ->
            if (cell == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                )
            } else {
                val isToday = cell == today
                val isSelected = cell == selectedDate
                val eventsForDay = eventsByDate[cell].orEmpty()

                val bg = when {
                    isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    isToday -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.08f)
                }
                val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 96.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(bg)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = borderColor,
                            shape = MaterialTheme.shapes.small
                        )
                        .clickable { onSelect(cell) }
                        .padding(6.dp)
                ) {
                    Column(Modifier.fillMaxSize()) {
                        Row(Modifier.fillMaxWidth()) {
                            Spacer(Modifier.weight(1f))
                            Text(
                                text = cell.dayOfMonth.toString(),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (eventsForDay.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                        }
                        eventsForDay.take(3).forEach { event ->
                            val eventColor = eventColor(event, defaultEventColor)
                            val labelColor = contrastingTextColor(eventColor)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(eventColor),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = event.title,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = labelColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun buildMonthCells(
    yearMonth: YearMonth,
    firstDayOfWeek: DayOfWeek
): List<LocalDate?> {
    val firstOfMonth = yearMonth.atDay(1)
    val daysInMonth = yearMonth.lengthOfMonth()
    val leadingEmpty = ((firstOfMonth.dayOfWeek.value - firstDayOfWeek.value + 7) % 7)
    val dates = (1..daysInMonth).map { day -> yearMonth.atDay(day) }
    val trailingEmpty = (7 - (leadingEmpty + daysInMonth) % 7) % 7

    return List(leadingEmpty) { null } + dates + List(trailingEmpty) { null }
}

@Composable
private fun ColorPickerRow(
    colors: List<Color>,
    selectedColorInt: Int,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        colors.forEach { color ->
            val colorInt = color.toArgb()
            ColorSwatch(
                color = color,
                isSelected = colorInt == selectedColorInt,
                onClick = { onSelect(colorInt) }
            )
        }
    }
}

@Composable
private fun ColorSwatch(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val outlineColor = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.outlineVariant
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .border(width = if (isSelected) 3.dp else 1.dp, color = outlineColor, shape = CircleShape)
            .background(color)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = contrastingTextColor(color)
            )
        }
    }
}

@Composable
private fun DaySchedule(
    date: LocalDate,
    events: List<Event>,
    defaultEventColor: Color,
    onEventClick: (Event) -> Unit
) {
    val zone = remember { ZoneId.systemDefault() }
    val eventSpans = remember(events, date) {
        events.mapNotNull { event ->
            eventSpanWithinDay(event, date, zone)?.let { span -> event to span }
        }
    }
    val rowHeight = 48.dp
    val outline = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    val emptyBackground = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 420.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        if (eventSpans.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No events yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        items(24) { hour ->
            val rowStart = hour * 60
            val rowEnd = rowStart + 60
            val covering = eventSpans.filter { (_, span) ->
                span.first < rowEnd && span.second > rowStart
            }
            val primary = covering.minByOrNull { it.second.first }
            val isStartHour = primary?.second?.let { span ->
                span.first in rowStart until rowEnd
            } ?: false

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(rowHeight)
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .width(64.dp)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopStart
                ) {
                    Text(
                        text = String.format(Locale.getDefault(), "%02d:00", hour),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    val baseModifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, outline, RoundedCornerShape(12.dp))

                    if (primary == null) {
                        Box(baseModifier.background(emptyBackground))
                    } else {
                        val event = primary.first
                        val eventColor = eventColor(event, defaultEventColor)
                        val textColor = contrastingTextColor(eventColor)
                        Box(
                            modifier = baseModifier
                                .background(eventColor.copy(alpha = 0.9f))
                                .clickable { onEventClick(event) }
                                .padding(10.dp),
                            contentAlignment = Alignment.TopStart
                        ) {
                            if (isStartHour) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = event.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = textColor,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = formatEventTimeRangeShort(event, zone),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = textColor.copy(alpha = 0.9f)
                                    )
                                    if (covering.size > 1) {
                                        Text(
                                            text = "+${covering.size - 1} more",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = textColor.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Divider(
                        modifier = Modifier.align(Alignment.BottomStart),
                        thickness = 0.5.dp,
                        color = outline
                    )
                }
            }
        }
    }
}

@Composable
private fun EventCard(
    event: Event,
    defaultColor: Color,
    modifier: Modifier = Modifier,
    onEventClick: (Event) -> Unit = {}
) {
    val eventColor = eventColor(event, defaultColor)
    val accent = eventColor.copy(alpha = 0.2f)
    val indicatorColor = eventColor

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = { onEventClick(event) },
        colors = CardDefaults.cardColors(containerColor = accent)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(indicatorColor)
                )
                Spacer(Modifier.width(8.dp))
                Text(event.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
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
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

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

private fun formatEventTimeRangeShort(event: Event, zone: ZoneId): String {
    if (event.allDay) return "All day"
    val formatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    val start = Instant.ofEpochMilli(event.startEpochMillis).atZone(zone)
    val end = Instant.ofEpochMilli(event.endEpochMillis).atZone(zone)
    val startText = start.toLocalTime().format(formatter)
    val endText = end.toLocalTime().format(formatter)
    return "$startText – $endText"
}

private fun eventSpanWithinDay(event: Event, date: LocalDate, zone: ZoneId): Pair<Int, Int>? {
    if (event.allDay) return 0 to 24 * 60

    val startInstant = Instant.ofEpochMilli(event.startEpochMillis)
    val endInstant = Instant.ofEpochMilli(event.endEpochMillis)
    val dayStart = date.atStartOfDay(zone).toInstant()
    val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant()

    if (!startInstant.isBefore(dayEnd) || !endInstant.isAfter(dayStart)) {
        return null
    }

    val clampedStart = if (startInstant.isBefore(dayStart)) dayStart else startInstant
    val clampedEnd = if (endInstant.isAfter(dayEnd)) dayEnd else endInstant

    val startMinutes = ChronoUnit.MINUTES.between(dayStart, clampedStart).toInt().coerceIn(0, 24 * 60)
    val endMinutes = ChronoUnit.MINUTES.between(dayStart, clampedEnd).toInt().coerceIn(0, 24 * 60)

    if (endMinutes <= startMinutes) return null

    return startMinutes to endMinutes
}

private fun eventColor(event: Event, defaultColor: Color): Color {
    return if (event.colorArgb != 0L) Color(event.colorArgb.toInt()) else defaultColor
}

private fun eventColorInt(event: Event, defaultColor: Color): Int {
    return if (event.colorArgb != 0L) event.colorArgb.toInt() else defaultColor.toArgb()
}

private fun contrastingTextColor(color: Color): Color {
    return if (color.luminance() > 0.5f) Color.Black else Color.White
}

private fun colorIntToLong(colorInt: Int): Long = colorInt.toLong() and 0xFFFFFFFFL
