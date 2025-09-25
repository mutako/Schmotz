package com.schmotz.calendar

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

fun LocalDate.atStartOfDayMillis(): Long =
    this.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

fun LocalDate.atEndOfDayMillis(): Long =
    this.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1

fun LocalDate.atTimeMillis(time: LocalTime): Long =
    this.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

suspend fun fetchMetadata(url: String): Triple<String, String?, String?> {
    val fallbackTitle = if (url.length > 60) url.take(57) + "..." else url

    return withContext(Dispatchers.IO) {
        runCatching {
            val document = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (compatible; SchmotzCalendar/1.0)")
                .timeout(10_000)
                .get()

            val title = document.selectFirst("meta[property=og:title]")?.attr("content")
                ?.ifBlank { null }
                ?: document.title().takeIf { it.isNotBlank() }
                ?: fallbackTitle

            val description = listOf(
                document.selectFirst("meta[property=og:description]")?.attr("content"),
                document.selectFirst("meta[name=description]")?.attr("content"),
                document.selectFirst("meta[name=twitter:description]")?.attr("content")
            ).firstOrNull { !it.isNullOrBlank() }

            val image = listOf(
                document.selectFirst("meta[property=og:image]")?.attr("content"),
                document.selectFirst("meta[name=twitter:image]")?.attr("content")
            ).firstOrNull { !it.isNullOrBlank() }

            Triple(title, description?.trim(), image?.trim())
        }.getOrElse {
            Triple(fallbackTitle, null, null)
        }
    }
}
