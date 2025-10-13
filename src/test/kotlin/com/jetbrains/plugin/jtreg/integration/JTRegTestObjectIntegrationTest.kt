package com.jetbrains.plugin.jtreg.integration

import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.plugin.jtreg.configuration.JTRegConfiguration
import com.jetbrains.plugin.jtreg.configuration.JTRegConfigurationType
import com.jetbrains.plugin.jtreg.configuration.JTRegTestObject
import com.jetbrains.plugin.jtreg.service.JTRegService
import java.nio.file.Files
import java.nio.file.Path

class JTRegTestObjectIntegrationTest : BasePlatformTestCase() {

    private lateinit var tempHome: Path

    override fun setUp() {
        super.setUp()
        // Prepare a fake jtreg home with a lib jar so classpath wiring works
        tempHome = Files.createTempDirectory("jtregHome")
        val lib = Files.createDirectories(tempHome.resolve("lib"))
        Files.createFile(lib.resolve("dummy.jar"))

        // Configure the real application-level service for the test
        val service = com.intellij.openapi.application.ApplicationManager.getApplication().getService(JTRegService::class.java)
        service.jtregHomeDirectory = tempHome.toString()
    }

    override fun tearDown() {
        try {
            // Cleanup
            tempHome.toFile().deleteRecursively()
        } finally {
            super.tearDown()
        }
    }

    fun testConfigureRTClasspathAddsJtregLibs() {
        val factory = JTRegConfigurationType.Util.getInstance().configurationFactories.first()
        val cfg = JTRegConfiguration("cfg", project, factory)

        val executor = DefaultRunExecutor.getRunExecutorInstance()
        val env = ExecutionEnvironmentBuilder.create(project, executor, cfg).build()

        val state = JTRegTestObject(cfg, env)

        val params = JavaParameters()
        val method = JTRegTestObject::class.java.getDeclaredMethod(
            "configureRTClasspath",
            com.intellij.execution.configurations.JavaParameters::class.java,
            com.intellij.openapi.module.Module::class.java
        )
        method.isAccessible = true
        method.invoke(state, params, module)

        val cp = params.classPath.pathList
        assertTrue(cp.any { it.endsWith("dummy.jar") })
    }
}
