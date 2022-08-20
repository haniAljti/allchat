package com.hanialjti.allchat.utils

import org.junit.Assert
import org.junit.Assert.*

import org.junit.Test

class TimeUtilsKtTest {

    @Test
    fun formatDateSeparator_givenLastTimestampNull_shouldReturnFormattedCurrentDate() {
        val currentTimestamp: Long = 1653755507000
        val lastTimestamp: Long? = null
        val expected = "28 May. 2022"

        val formattedDateString = formatDateSeparator(currentTimestamp, lastTimestamp)

        assertEquals(expected, formattedDateString)
    }

    @Test
    fun formatDateSeparator_givenLastTimestampYesterday_shouldReturnToday() {
        val currentTimestamp: Long = 1653755507000
        val lastTimestamp: Long = 1653669107000
        val expected = "Today"

        val formattedDateString = formatDateSeparator(currentTimestamp, lastTimestamp)

        assertEquals(expected, formattedDateString)
    }

    @Test
    fun formatDateSeparator_givenLastTimestamp2DaysAgoAndCurrentTimestampYesterday_shouldReturnYesterday() {
        val currentTimestamp: Long = 1653669107000
        val lastTimestamp: Long = 1653582707000
        val expected = "Yesterday"

        val formattedDateString = formatDateSeparator(currentTimestamp, lastTimestamp)

        assertEquals(expected, formattedDateString)
    }
}