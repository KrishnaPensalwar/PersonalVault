package com.example.sample.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class FileProviderPathsTest {

    @Test
    fun fileProviderExposesOnlyVaultDocumentsScansAndShareCache() {
        val moduleRoot = File("src/main").takeIf { it.isDirectory } ?: File("app/src/main")
        val document = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(File(moduleRoot, "res/xml/file_paths.xml"))
        val nodes = document.documentElement.childNodes
        val paths = buildList {
            for (index in 0 until nodes.length) {
                val node = nodes.item(index)
                if (node.nodeType != org.w3c.dom.Node.ELEMENT_NODE) continue
                add("${node.nodeName}:${node.attributes.getNamedItem("path").nodeValue}")
            }
        }
        assertEquals(
            listOf(
                "files-path:vault/documents/",
                "cache-path:scans/",
                "cache-path:vault-share/",
            ),
            paths,
        )
        assertFalse(paths.any { it.endsWith(":.") })
    }
}
