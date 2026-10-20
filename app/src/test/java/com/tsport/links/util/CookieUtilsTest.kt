package com.tsport.links.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CookieUtilsTest {

    @Test
    fun `detects auth token`() {
        assertTrue(CookieUtils.hasAuthToken("ct0=abc; auth_token=xyz; lang=tr"))
    }

    @Test
    fun `does not detect missing auth token`() {
        assertFalse(CookieUtils.hasAuthToken("ct0=abc; lang=tr"))
        assertFalse(CookieUtils.hasAuthToken(null))
        assertFalse(CookieUtils.hasAuthToken(""))
    }

    @Test
    fun `extracts csrf token`() {
        assertEquals("abc123", CookieUtils.csrfToken("auth_token=x; ct0=abc123; lang=tr"))
    }

    @Test
    fun `returns null when csrf token is missing`() {
        assertNull(CookieUtils.csrfToken("auth_token=x; lang=tr"))
        assertNull(CookieUtils.csrfToken(null))
    }
}
