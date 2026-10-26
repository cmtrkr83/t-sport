package com.tsport.links.matchday

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class IstanbulDateTest {

    private fun utc(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year, month - 1, day, hour, minute)
        }.timeInMillis

    @Test
    fun `uses europe istanbul date across utc midnight`() {
        assertEquals("2026-10-26", IstanbulDate.today(utc(2026, 10, 25, 22, 0)))
        assertEquals("2026-10-26", IstanbulDate.today(utc(2026, 10, 26, 20, 59)))
        assertEquals("2026-10-27", IstanbulDate.today(utc(2026, 10, 26, 21, 1)))
    }
}
