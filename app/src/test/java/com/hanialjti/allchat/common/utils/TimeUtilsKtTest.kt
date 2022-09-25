package com.hanialjti.allchat.common.utils

import org.junit.Assert.*

import org.junit.Test
import java.time.LocalDateTime

class TimeUtilsKtTest {

    @Test
    fun formatDateSeparator_givenLastTimestampNull_shouldReturnFormattedCurrentDate() {
//        val currentTimestamp: Long = 1653755507000
//        val lastTimestamp: Long? = null
//        val expected = "28 May. 2022"
//
//        val formattedDateString = formatDateSeparator(currentTimestamp, lastTimestamp)
//
//        assertEquals(expected, formattedDateString)
    }

    @Test
    fun formatDateSeparator_givenLastTimestampYesterday_shouldReturnToday() {
//        val currentTimestamp: Long = 1653755507000
//        val lastTimestamp: Long = 1653669107000
//        val expected = "Today"
//
//        val formattedDateString = formatDateSeparator(currentTimestamp, lastTimestamp)
//
//        assertEquals(expected, formattedDateString)
    }

    @Test
    fun formatDateSeparator_givenLastTimestamp2DaysAgoAndCurrentTimestampYesterday_shouldReturnYesterday() {
//        val currentTimestamp: Long = 1653669107000
//        val lastTimestamp: Long = 1653582707000
//        val expected = "Yesterday"
//
//        val formattedDateString = formatDateSeparator(currentTimestamp, lastTimestamp)
//
//        assertEquals(expected, formattedDateString)
    }

    @Test
    fun `given local date from yesterday _ should return Yesterday`() {
        val yesterday = LocalDateTime.now().minusDays(1)
        val expected = UiDate.Yesterday(yesterday)

        assertEquals(expected, yesterday.asUiDate())
    }

    @Test
    fun `given local date from today _ should return Today`() {
        val today = LocalDateTime.now()
        val expected = UiDate.Today(today)

        assertEquals(expected, today.asUiDate())
    }

    @Test
    fun `given local date from last month _ should return LastMonth`() {
        val lastMonth = LocalDateTime.now().minusMonths(1)
        val expected = UiDate.LastMonth(lastMonth)

        assertEquals(expected, lastMonth.asUiDate())
    }

    @Test
    fun `given local date from last year _ should return LastYear`() {
        val lastYear = LocalDateTime.now().minusYears(1)
        val expected = UiDate.LastYear(lastYear)

        assertEquals(expected, lastYear.asUiDate())
    }

    @Test
    fun `given local date from 2 years _ should return other`() {
        val twoYearsAgo = LocalDateTime.now().minusYears(2)
        val expected = UiDate.Other(twoYearsAgo)

        assertEquals(expected, twoYearsAgo.asUiDate())
    }
}