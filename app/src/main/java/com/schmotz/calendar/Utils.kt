package com.schmotz.calendar

import java.time.LocalDate
import java.time.ZoneId

fun LocalDate.atStartOfDayMillis(): Long =
    this.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

fun LocalDate.atEndOfDayMillis(): Long =
    this.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
