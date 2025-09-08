package com.jetbrains.plugin.jtreg.configuration

import com.intellij.execution.configurations.ModuleBasedConfigurationOptions.ClasspathModification
import com.jetbrains.plugin.jtreg.configuration.model.RepeatSettings
import com.jetbrains.plugin.jtreg.configuration.model.TestData
import com.jetbrains.plugin.jtreg.configuration.model.TestVMSettings
import com.jetbrains.plugin.jtreg.configuration.model.WestonSettings
import com.jetbrains.plugin.jtreg.service.JTRegService
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

class ConfigurationHelperTest {

    //  @Test
    fun testDataSerializationTest() {
        val serviceMock = mock<JTRegService>()
        whenever(serviceMock.concurrency).thenReturn(2)
        whenever(serviceMock.timeoutFactor).thenReturn(3.0f)
        whenever(serviceMock.timeLimit).thenReturn(2000)
        whenever(serviceMock.ignore).thenReturn(IgnoreMode.QUIET)
        whenever(serviceMock.westonSettings).thenReturn(WestonSettings())
        whenever(serviceMock.testMode).thenReturn(TestMode.OTHER_VM)

        val originalData = TestData()
        originalData.envVars = mapOf("A" to "anime")
        originalData.parameters = "some=value"
        originalData.vmParameters = "<vmParameters"
        originalData.workingDirectory = "/home/user/work"
        originalData.passParentEnvs = false

        originalData.testKind = TestData.TEST_DIRECTORY
        originalData.packageName = "com.jetbrains.plugin.jtreg"
        originalData.className = ""
        originalData.testGroup = null

        originalData.testMode = TestMode.AGENT_VM
        originalData.testCategory = TestData.ALL_TESTS

        originalData.reportDir = "/home/user/report"
        originalData.excludeList = "/home/user/exclude.txt"
        originalData.concurrency = 3
        originalData.ignoreMode = IgnoreMode.RUN
        originalData.lock = "/home/user/lock.file"
        originalData.timeoutFactor = 2.0f
        originalData.timeLimit = 1000
        originalData.xmlReport = true
        originalData.keyword = "headful"

        originalData.weston = WestonSettings(true, 1, 512, 384, "/home/user/wakefield")
        originalData.repeat = RepeatSettings(RepeatCount.N, 99, 200)
        originalData.vmSettings = TestVMSettings(
            allowSecurityManager = true,
            javaOptions = "-Djava.library.path=/home/user/work/java",
            nativeLibPath = "/home/user/nativeLib",
            envVars = mapOf("B" to "anime"),
            classPath = listOf(ClasspathModification("/path1", false), ClasspathModification("/path2", false)),
            useIdeaVMOptions = true,
        )

        val element = ConfigurationHelper.prepareTestData(originalData)
        println(element)
        val loadedData = ConfigurationHelper.readTestData(element, serviceMock)

        println(loadedData)
    }

}