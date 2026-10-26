package com.tsport.links.ui

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Document

class ControlFocusRingTest {

    private val androidNamespace = "http://schemas.android.com/apk/res/android"
    private val focusForeground = "@drawable/button_focus_foreground"
    private val focusableControls =
        listOf("broadcastsCheck", "linksOnlyCheck", "tabPopular", "tabLatest")

    @Test
    fun `option and tab controls have a visible focus foreground in both layouts`() {
        for (layout in listOf("layout/activity_main.xml", "layout-land/activity_main.xml")) {
            val document = parseLayout(layout)
            for (id in focusableControls) {
                val node = document.findView(id)
                assertTrue("$layout is missing $id", node != null)
                assertEquals(
                    "$layout/$id must draw the 3dp focus ring",
                    focusForeground,
                    node!!.attributes.getNamedItemNS(androidNamespace, "foreground")?.nodeValue
                )
            }
        }
    }

    private fun Document.findView(id: String): org.w3c.dom.Element? {
        val nodes = getElementsByTagName("*")
        for (index in 0 until nodes.length) {
            val element = nodes.item(index) as? org.w3c.dom.Element ?: continue
            if (element.getAttributeNS(androidNamespace, "id") == "@+id/$id") {
                return element
            }
        }
        return null
    }

    private fun parseLayout(path: String): Document {
        val candidates = listOf(
            File("src/main/res/$path"),
            File("app/src/main/res/$path")
        )
        val file = candidates.firstOrNull { it.isFile }
            ?: error("layout not found from ${File(".").absolutePath}: $path")
        return DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }.newDocumentBuilder().parse(file)
    }
}
