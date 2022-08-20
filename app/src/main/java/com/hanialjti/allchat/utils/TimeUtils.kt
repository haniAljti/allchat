package com.hanialjti.allchat.utils

import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

val currentTimestamp: Long
    get() = System.currentTimeMillis()


const val TWO_DIGIT_FORMAT = "HH:mm"
const val DEFAULT_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS"
const val DATE_TIME_MONTH_SHORT_NO_TIME = "dd MMM. yyyy"
const val DATE_TIME_MONTH_SHORT = "dd MMM. yyyy - $TWO_DIGIT_FORMAT"
const val DATE_TIME_MONTH_LONG = "dd MMMM, yyyy - $TWO_DIGIT_FORMAT"

/**
 * @param pattern defaults to yyyy-MM-dd HH:mm:ss.SSS
 * @return a formatted timestamp string
 */
fun Long.formatTimestamp(pattern: String = DEFAULT_PATTERN): String {
    val simpleDateFormat = SimpleDateFormat(pattern, Locale.getDefault())
    return simpleDateFormat.format(Date(this))
}

fun formatDateSeparator(
    currentDate: Long,
    lastDate: Long?,
    pattern: String = DATE_TIME_MONTH_SHORT_NO_TIME
): String? {

    val now = LocalDateTime.now()
    val current = LocalDateTime.ofInstant(Instant.ofEpochMilli(currentDate), ZoneId.systemDefault())
    val last = LocalDateTime.ofInstant(
        Instant.ofEpochMilli(
            lastDate ?: 0
        ), ZoneId.systemDefault()
    )

    when {
        current.year == now.year && current.month == now.month && current.dayOfMonth == now.dayOfMonth -> {
            if (current.year != last.year || current.month != last.month || current.dayOfMonth != last.dayOfMonth) {
                return "Today"
            }
        }
        current.year == now.year && current.month == now.month && current.dayOfMonth == now.dayOfMonth - 1 -> {
            if (current.year != last.year || current.month != last.month || current.dayOfMonth != last.dayOfMonth) {
                return "Yesterday"
            }
        }
        else -> {
            if (current.year != last.year || current.month != last.month || current.dayOfMonth != last.dayOfMonth) {
                return current.format(DateTimeFormatter.ofPattern(pattern))
            }
        }
    }
    return null
}