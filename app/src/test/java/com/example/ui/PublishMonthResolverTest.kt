package com.example.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class PublishMonthResolverTest {
    @Test
    fun `publish months include current November and remain newest first`() {
        val november2026 = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.NOVEMBER, 3)
        }

        val months = buildPublishMonths(november2026)

        assertEquals("Semua Bulan", months.first())
        assertEquals("November 2026", months[1])
        assertTrue(months.contains("Oktober 2026"))
        assertEquals("Juni 2026", months.last())
    }
}
