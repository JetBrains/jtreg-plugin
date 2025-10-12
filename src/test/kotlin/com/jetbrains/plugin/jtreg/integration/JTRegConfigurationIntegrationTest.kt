package com.jetbrains.plugin.jtreg.integration

import com.intellij.execution.RunManager
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.plugin.jtreg.configuration.JTRegConfiguration
import com.jetbrains.plugin.jtreg.configuration.JTRegConfigurationType
import com.jetbrains.plugin.jtreg.configuration.IgnoreMode
import com.jetbrains.plugin.jtreg.configuration.TestMode
import com.jetbrains.plugin.jtreg.configuration.model.TestData
import org.jdom.Element

class JTRegConfigurationIntegrationTest : LightPlatformTestCase() {

    private lateinit var factory: ConfigurationFactory

    override fun setUp() {
        super.setUp()
        factory = JTRegConfigurationType.Util.getInstance().configurationFactories.first()
    }

    fun testWriteReadExternalRoundTripPreservesCriticalFields() {
        val project = project
        val cfg = JTRegConfiguration("My JTReg", project, factory)
        cfg.setPackage("com.example.pkg")
        cfg.setRunClass("")
        cfg.setTestKind(TestData.TEST_DIRECTORY)
        cfg.setProgramParameters("-k:headful")
        cfg.setVMParameters("-Xmx512m")
        cfg.setIgnoreMode(IgnoreMode.RUN)
        cfg.setTestMode(TestMode.AGENT_VM)
        cfg.setReportDir("/tmp/report")
        cfg.setWorkingDirectory("/tmp/work")
        cfg.setAlternativeJrePathEnabled(true)
        cfg.setAlternativeJrePath("/jdk/fake")
        cfg.setRunCmd("/bin/bash -lc run")

        val element = Element("configuration")
        cfg.writeExternal(element)


        // Read into a new configuration instance to simulate IDE persistence
        val cfg2 = JTRegConfiguration("", project, factory)
        cfg2.readExternal(element)

        assertEquals("com.example.pkg", cfg2.`package`)
        assertEquals(TestData.TEST_DIRECTORY, cfg2.testType)
        assertEquals("-k:headful", cfg2.programParameters)
        assertEquals("-Xmx512m", cfg2.vmParameters)
        assertEquals(IgnoreMode.RUN, cfg2.getIgnoreMode())
        assertEquals(TestMode.AGENT_VM, cfg2.getTestMode())
        assertEquals("/tmp/report", cfg2.getReportDir())
        assertEquals("/tmp/work", cfg2.workingDirectory)
        assertTrue(cfg2.isAlternativeJrePathEnabled)
        assertEquals("/jdk/fake", cfg2.alternativeJrePath)
        assertEquals("/bin/bash -lc run", cfg2.getRunCmd())
    }

    fun testReadExternalFallbackWhenTestKindMissing() {
        val project = project
        val cfg = JTRegConfiguration("", project, factory)
        cfg.setPackage("")
        cfg.setRunClass("com.example.TestClass")

        val element = Element("configuration")
        cfg.writeExternal(element)
        // Simulate older or changed IDE dropping testKind attribute
        element.removeAttribute("testKind")

        val cfg2 = JTRegConfiguration("", project, factory)
        cfg2.readExternal(element)

        // Fallback rules from JTRegConfiguration.readExternal (lines 453–463)
        assertEquals(TestData.TEST_CLASS, cfg2.testType)
        assertEquals("com.example.TestClass", cfg2.runClass)
        assertEquals("com.example.TestClass", cfg2.name)
    }

    fun testRunManagerStoresConfiguration() {
        val runManager = RunManager.getInstance(project)
        val settings = runManager.createConfiguration("JTReg Settings", factory)
        val cfg = settings.configuration as JTRegConfiguration
        cfg.setPackage("p")
        cfg.setTestKind(TestData.TEST_DIRECTORY)

        runManager.addConfiguration(settings)
        runManager.selectedConfiguration = settings

        // Ensure it is discoverable and persists core attributes in memory
        val restored = runManager.allConfigurationsList.filterIsInstance<JTRegConfiguration>().first()
        assertEquals(TestData.TEST_DIRECTORY, restored.testType)
        assertEquals("p", restored.`package`)
    }
}
