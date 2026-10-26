package com.tsport.links.search

import kotlinx.coroutines.delay

internal enum class TabSelection { SELECTED, CLICKED, FAILED }

internal class LatestTabSelector(
    private val evaluate: suspend (String) -> String?,
    private val findAttempts: Int = 6,
    private val findIntervalMs: Long = 300L,
    private val confirmAttempts: Int = 8,
    private val confirmIntervalMs: Long = 250L
) {

    suspend fun select(): TabSelection {
        for (attempt in 0 until findAttempts) {
            val status = evaluate(CLICK_JS)
            if (status?.contains("selected") == true) {
                return TabSelection.SELECTED
            }
            if (status?.contains("clicked") == true) {
                for (confirmAttempt in 0 until confirmAttempts) {
                    delay(confirmIntervalMs)
                    if (evaluate(CONFIRM_JS)?.contains("selected") == true) {
                        return TabSelection.CLICKED
                    }
                }
                return TabSelection.FAILED
            }
            delay(findIntervalMs)
        }
        return TabSelection.FAILED
    }

    companion object {
        private const val TAB_LOOKUP = """
            const tab = document.querySelector('a[role="tab"][href*="f=live"]')
              || document.querySelector('a[href*="f=live"]')
              || Array.from(document.querySelectorAll('[role="tab"]'))
                   .find((el) => /^(en son|latest)\b/i.test((el.textContent || '').trim()));
        """

        internal val CLICK_JS = """
            (function () {
              $TAB_LOOKUP
              if (!tab) return "missing";
              if (tab.getAttribute("aria-selected") === "true") return "selected";
              tab.click();
              return "clicked";
            })();
        """.trimIndent()

        internal val CONFIRM_JS = """
            (function () {
              $TAB_LOOKUP
              return tab && tab.getAttribute("aria-selected") === "true"
                ? "selected"
                : "pending";
            })();
        """.trimIndent()
    }
}
