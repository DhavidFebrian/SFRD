package com.example.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ScheduleAutofillResolverTest {

    @Test
    fun `autofill uses fresh web agent value`() {
        assertEquals("Ilham", resolveWebAutofillValue(" Ilham "))
    }

    @Test
    fun `autofill stays empty when web has not returned an agent`() {
        assertEquals("", resolveWebAutofillValue(null))
        assertEquals("", resolveWebAutofillValue("   "))
    }
}
