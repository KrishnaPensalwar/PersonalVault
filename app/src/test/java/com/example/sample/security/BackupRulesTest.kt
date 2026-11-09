package com.example.sample.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class BackupRulesTest {

    @Test
    fun fullBackupContentExcludesVaultSecretsOnly() {
        val excludes = excludePaths(xml("backup_rules.xml"), "full-backup-content")
        assertEquals(REQUIRED_EXCLUDES, excludes)
    }

    @Test
    fun cloudBackupAndDeviceTransferExcludeVaultSecrets() {
        val rules = xml("data_extraction_rules.xml")
        assertEquals(REQUIRED_EXCLUDES, excludePaths(rules, "cloud-backup"))
        assertEquals(REQUIRED_EXCLUDES, excludePaths(rules, "device-transfer"))
    }

    @Test
    fun manifestAllowsBackupAndPointsAtBothRuleFiles() {
        val manifest = xmlFile("AndroidManifest.xml").readText()
        assertTrue(manifest.contains("""android:allowBackup="true""""))
        assertTrue(manifest.contains("""android:fullBackupContent="@xml/backup_rules""""))
        assertTrue(manifest.contains("""android:dataExtractionRules="@xml/data_extraction_rules""""))
    }

    private fun excludePaths(root: Element, parentTag: String): Set<String> {
        val parent = if (root.tagName == parentTag) {
            root
        } else {
            root.getElementsByTagName(parentTag).item(0) as Element
        }
        val nodes = parent.getElementsByTagName("exclude")
        return buildSet {
            for (index in 0 until nodes.length) {
                val exclude = nodes.item(index) as Element
                add("${exclude.getAttribute("domain")}:${exclude.getAttribute("path")}")
            }
        }
    }

    private fun xml(name: String): Element =
        DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(xmlFile(name))
            .documentElement

    private fun xmlFile(name: String): File {
        val moduleRoot = File("src/main").takeIf { it.isDirectory } ?: File("app/src/main")
        val relative = if (name == "AndroidManifest.xml") name else "res/xml/$name"
        return File(moduleRoot, relative)
    }

    private companion object {
        val REQUIRED_EXCLUDES = setOf(
            "database:personal_vault_database",
            "file:vault",
            "sharedpref:app_lock_prefs.xml",
        )
    }
}
