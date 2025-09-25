package com.schmotz.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * Visual month calendar with:
 * - Today highlighted
 * - Select any date
 * - Add a simple all-day event on the selected date (saved via repo.upsertEvent)
 *
 * IMPORTANT:
 * Keep atStartOfDayMillis() and atEndOfDayMillis() ONLY in Utils.kt.
 * Do NOT duplicate them in Data.kt or here.
 */

@Composable
fun CalendarScreen(
    repo: Repo,
    profile: UserProfile
) {
    var currentYearMonth by remember { mutableStateOf(YearMonth.now()) }
    val today = remember { LocalDate.now() }
    var selectedDate by remember { mutableStateOf(today) }

    // Local cache so the list updates instantly; we also persist via repo.
    val eventsByDate = remember { mutableStateMapOf<LocalDate, MutableList<Event>>() }

    var showAddDialog by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

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
            Button(onClick = { showAddDialog = true }) { Text("Add event") }
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
            onSelect = { selectedDate = it }
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

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add event") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("On: $selectedDate")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val title = newTitle.trim()
                    if (title.isNotEmpty()) {
                        val start = selectedDate.atStartOfDayMillis()
                        val end = selectedDate.atEndOfDayMillis()

                        val event = Event(
                            id = "",
                            title = title,
                            startEpochMillis = start,
                            endEpochMillis = end
                        )

                        // Update local cache immediately
                        val bucket = eventsByDate.getOrPut(selectedDate) { mutableListOf() }
                        bucket.add(event)

                        // Persist
                        scope.launch {
                            runCatching { repo.upsertEvent(profile, event) }.onFailure {
                                // Optional: show a Snackbar or remove the optimistic item
                            }
                        }
                    }
                    newTitle = ""
                    showAddDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = {
                    newTitle = ""
                    showAddDialog = false
                }) { Text("Cancel") }
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
                "${event.startEpochMillis} – ${event.endEpochMillis}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!event.notes.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(event.notes!!, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
