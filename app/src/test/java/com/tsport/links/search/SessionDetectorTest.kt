package com.tsport.links.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionDetectorTest {

    @Test
    fun `detects login and signup urls`() {
        assertTrue(isLoginUrl("https://x.com/i/flow/login?redirect_after_login=%2Fsearch"))
        assertTrue(isLoginUrl("https://x.com/login"))
        assertTrue(isLoginUrl("https://x.com/i/flow/signup"))
    }

    @Test
    fun `normal search urls are not login urls`() {
        assertFalse(isLoginUrl("https://x.com/search?q=%23GsvBJK&f=live"))
        assertFalse(isLoginUrl("https://x.com/home"))
        assertFalse(isLoginUrl(null))
    }
}
