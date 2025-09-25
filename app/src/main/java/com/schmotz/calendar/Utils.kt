package com.schmotz.calendar

import java.time.LocalDate
import java.time.ZoneId

fun LocalDate.atStartOfDayMillis(): Long =
    this.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

fun LocalDate.atEndOfDayMillis(): Long =
    this.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1

suspend fun fetchMetadata(url: String): Triple<String, String?, String?> {
    val title = if (url.length > 60) url.take(57) + "..." else url
    val description: String? = null
    val imageUrl: String? = null
    return Triple(title, description, imageUrl)
}
