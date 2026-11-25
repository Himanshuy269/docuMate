package com.example.documate.core.common

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.timeIntervalSince1970

actual fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000.0).toLong()

actual fun formatTimestamp(timestampMillis: Long): String {
    val formatter = NSDateFormatter().apply {
        dateFormat = "h:mm a"
    }
    val date = NSDate.dateWithTimeIntervalSince1970(timestampMillis / 1000.0)
    return formatter.stringFromDate(date)
}
