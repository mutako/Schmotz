package com.schmotz.calendar

import java.time.LocalDate
import java.time.ZoneId

// Public date helpers used by some older calls.
// Safe to keep in a shared place without clashing with file-private ones elsewhere.
fun LocalDate.atStartOfDayMillis(): Long =
    this.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

fun LocalDate.atEndOfDayMillis(): Long =
    this.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
