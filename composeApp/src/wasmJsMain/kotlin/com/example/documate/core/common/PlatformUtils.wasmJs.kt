package com.example.documate.core.common

import kotlin.js.Date

actual fun currentTimeMillis(): Long = Date.now().toLong()

actual fun formatTimestamp(timestampMillis: Long): String {
    val date = Date(timestampMillis.toDouble())
    val hours = date.getHours()
    val minutes = date.getMinutes()
    val period = if (hours >= 12) "PM" else "AM"
    val displayHours = if (hours % 12 == 0) 12 else hours % 12
    val displayMinutes = if (minutes < 10) "0$minutes" else "$minutes"
    return "$displayHours:$displayMinutes $period"
}
