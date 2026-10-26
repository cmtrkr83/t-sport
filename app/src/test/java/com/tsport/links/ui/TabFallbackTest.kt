package com.tsport.links.ui

import com.tsport.links.search.SearchOptions
import com.tsport.links.search.SearchTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TabFallbackTest {

    @Test
    fun `warning is shown only after a latest search that could not switch tabs`() {
        assertTrue(shouldShowTabFallback(SearchOptions("tag", tab = SearchTab.LATEST), true))
        assertFalse(shouldShowTabFallback(SearchOptions("tag", tab = SearchTab.LATEST), false))
        assertFalse(shouldShowTabFallback(SearchOptions("tag", tab = SearchTab.POPULAR), true))
        assertFalse(shouldShowTabFallback(null, true))
    }

    @Test
    fun `warning is appended to success and empty messages`() {
        assertEquals("3 link bulundu · Sekme uyarısı", withTabWarning("3 link bulundu", "Sekme uyarısı"))
        assertEquals("Sonuç yok · Sekme uyarısı", withTabWarning("Sonuç yok", "Sekme uyarısı"))
        assertEquals("Sonuç yok", withTabWarning("Sonuç yok", null))
        assertNull(withTabWarning(null, "Sekme uyarısı"))
    }
}
