package com.aryan.gallery.ui.gallery

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatDurationTest {

    @Test
    fun `formatDuration formats sub-minute durations correctly`() {
        assertEquals("0:45", formatDuration(45_000L))
    }

    @Test
    fun `formatDuration formats minute durations correctly`() {
        assertEquals("3:05", formatDuration(185_000L))
    }

    @Test
    fun `formatDuration formats hour durations correctly`() {
        assertEquals("1:02:05", formatDuration(3725_000L))
    }
}
