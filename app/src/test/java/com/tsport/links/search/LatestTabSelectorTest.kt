package com.tsport.links.search

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LatestTabSelectorTest {

    @Test
    fun `scripts look up the latest tab by href and by label`() {
        assertTrue(LatestTabSelector.CLICK_JS.contains("f=live"))
        assertTrue(LatestTabSelector.CLICK_JS.contains("en son"))
        assertTrue(LatestTabSelector.CONFIRM_JS.contains("aria-selected"))
    }

    @Test
    fun `already selected tab returns selected without clicking`() = runTest {
        var calls = 0
        val selector = LatestTabSelector(
            evaluate = { calls++; "selected" },
            findIntervalMs = 0,
            confirmIntervalMs = 0
        )

        assertEquals(TabSelection.SELECTED, selector.select())
        assertEquals(1, calls)
    }

    @Test
    fun `clicked tab is confirmed by aria-selected`() = runTest {
        val scripts = mutableListOf<String>()
        var calls = 0
        val selector = LatestTabSelector(
            evaluate = { js ->
                scripts += js
                if (calls++ == 0) "clicked" else "selected"
            },
            findIntervalMs = 0,
            confirmIntervalMs = 0
        )

        assertEquals(TabSelection.CLICKED, selector.select())
        assertTrue(scripts.first().contains("tab.click()"))
        assertTrue(scripts.last().contains("aria-selected"))
    }

    @Test
    fun `click without confirmation fails`() = runTest {
        var calls = 0
        val selector = LatestTabSelector(
            evaluate = { calls++; if (calls == 1) "clicked" else "pending" },
            findIntervalMs = 0,
            confirmIntervalMs = 0,
            confirmAttempts = 3
        )

        assertEquals(TabSelection.FAILED, selector.select())
    }

    @Test
    fun `missing tab retries then fails`() = runTest {
        var calls = 0
        val selector = LatestTabSelector(
            evaluate = { calls++; "missing" },
            findAttempts = 3,
            findIntervalMs = 0
        )

        assertEquals(TabSelection.FAILED, selector.select())
        assertEquals(3, calls)
    }

    @Test
    fun `null evaluation retries then fails`() = runTest {
        var calls = 0
        val selector = LatestTabSelector(
            evaluate = { calls++; null },
            findAttempts = 2,
            findIntervalMs = 0
        )

        assertEquals(TabSelection.FAILED, selector.select())
        assertEquals(2, calls)
    }
}
