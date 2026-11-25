package com.example.documate.core.common

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun formatTimestamp(timestampMillis: Long): String {
    return SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestampMillis))
}
