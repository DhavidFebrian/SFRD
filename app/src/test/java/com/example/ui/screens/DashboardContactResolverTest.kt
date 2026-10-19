package com.example.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardContactResolverTest {

    @Test
    fun `web listing contact overrides different spreadsheet contact`() {
        assertEquals(
            "Briand",
            resolveListingContactName(
                spreadsheetName = "Dini",
                webListingName = "Briand"
            )
        )
    }

    @Test
    fun `spreadsheet contact remains fallback while web contact is unavailable`() {
        assertEquals(
            "Dini",
            resolveListingContactName(
                spreadsheetName = " Dini ",
                webListingName = " "
            )
        )
    }
}
