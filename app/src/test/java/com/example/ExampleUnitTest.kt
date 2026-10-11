package com.example

import org.junit.Assert.*
import org.junit.Test
import com.example.data.normalizeDate
import com.example.ui.screens.parsePublishJadwalPostingDateToEpoch

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
    assertEquals("2026-08-26", normalizeDate("Rabu, 26 Agustus 2026"))
    assertEquals("2026-08-04", normalizeDate("Selasa, 4 Agustus 2026"))
  }

  @Test
  fun testExtractIdCandidates() {
    val urlText = "Lihat detail properti di https://raywhite.co.id/property/12193 atau link /11091"
    val candidates = com.example.ui.screens.extractIdCandidates(urlText)
    assertTrue(candidates.any { it.id == "12193" })
    assertTrue(candidates.any { it.id == "11091" })

    val projectionText = "Meeting Listing Admin: www.sfrd.com/listing/98452 - ME: John"
    val candidates2 = com.example.ui.screens.extractIdCandidates(projectionText)
    assertEquals("98452", candidates2.first().id)
  }

  @Test
  fun postingDatesSortFromOldestAndPutUnscheduledLast() {
    val dates = listOf(
      "Tue Oct 13 2026 00:00:00 GMT+0700 (Waktu Indonesia Barat)",
      "BELUM_DIJADWALKAN",
      "Sun Oct 11 2026 00:00:00 GMT+0700 (Waktu Indonesia Barat)",
      "Mon Oct 12 2026 00:00:00 GMT+0700 (Waktu Indonesia Barat)"
    )

    val sorted = dates.sortedBy(::parsePublishJadwalPostingDateToEpoch)

    assertTrue(sorted[0].startsWith("Sun"))
    assertTrue(sorted[1].startsWith("Mon"))
    assertTrue(sorted[2].startsWith("Tue"))
    assertEquals("BELUM_DIJADWALKAN", sorted[3])
  }
}

