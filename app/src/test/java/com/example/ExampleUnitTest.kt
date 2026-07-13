package com.example

import org.junit.Assert.*
import org.junit.Test
import com.example.data.normalizeDate

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testNormalizeDate() {
    assertEquals("2026-06-24", normalizeDate("24/06/2026"))
    assertEquals("2026-06-24", normalizeDate("Rabu, 24 Juni 2026"))
    assertEquals("2026-06-24", normalizeDate(" Rabu, 24/06/2026 "))
    assertEquals("2026-06-24", normalizeDate("24 Juni"))
    assertEquals("2026-06-24", normalizeDate("24-06"))
    assertEquals("2026-06-24", normalizeDate("24/06"))
  }
}

