package com.hanialjti.allchat.common.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.hanialjti.allchat.R
import com.hanialjti.allchat.presentation.conversation.UiText
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
    val localDateTime = Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()

    return localDateTime.format(DateTimeFormatter.ofPattern(pattern))
}

fun LocalDateTime.asString(pattern: String = DEFAULT_PATTERN): String {
    return format(DateTimeFormatter.ofPattern(pattern))
}

sealed class UiDate(
    private val dateTime: LocalDateTime = LocalDateTime.now()
) {
    class Today(dateTime: LocalDateTime) : UiDate(dateTime)
    class Yesterday(dateTime: LocalDateTime) : UiDate(dateTime)
    class LastMonth(dateTime: LocalDateTime) : UiDate(dateTime)
    class LastYear(dateTime: LocalDateTime) : UiDate(dateTime)
    class Other(dateTime: LocalDateTime) : UiDate(dateTime)


    fun asLastOnlineUiText(): UiText {
        return when (this) {
            is Today -> UiText.StringResource(R.string.last_online_today)
            is Yesterday -> UiText.StringResource(R.string.last_online_yesterday)
            is Other, is LastYear, is LastMonth -> {
                return UiText.StringResource(
                    R.string.last_online_other,
                    dateTime.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
                )
            }
        }
    }

    @Composable
    fun asSeparator(): String {
        return when (this) {
            is Today -> stringResource(id = R.string.separator_today)
            is Yesterday -> stringResource(id = R.string.separator_yesterday)
            is LastMonth -> stringResource(id = R.string.separator_last_month)
            is LastYear -> stringResource(id = R.string.separator_last_year)
            is Other -> {
                return dateTime.format(DateTimeFormatter.ofPattern(stringResource(id = R.string.separator_other_date_pattern)))
            }
        }
    }

}

fun shouldDisplayDateSeparator(
    lastDate: LocalDate?,
    currentDate: LocalDate?
) = lastDate == null || !lastDate.isEqual(currentDate)


fun LocalDateTime.asUiDate(): UiDate {
    val currentDate = LocalDate.now()
    val localDate = toLocalDate()

    return when {
        localDate.isEqual(currentDate) -> {
            UiDate.Today(this)
        }
        currentDate.minusDays(1).isEqual(this.toLocalDate()) -> {
            UiDate.Yesterday(this)
        }
        this.month == currentDate.minusMonths(1).month &&
                this.year == currentDate.minusMonths(1).year -> {
            UiDate.LastMonth(this)
        }
        this.year == currentDate.minusYears(1).year -> {
            UiDate.LastYear(this)
        }
        else -> {
            UiDate.Other(this)
        }
    }
}

fun Long.asLocalDateTime(): LocalDateTime =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDateTime()

fun Long.asLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()