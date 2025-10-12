/*
 * Copyright (c) 2016, 2025, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2025 JetBrains s.r.o. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.jetbrains.plugin.jtreg.configuration

import com.intellij.execution.BeforeRunTask
import com.intellij.execution.Executor
import com.intellij.execution.JavaTestConfigurationBase
import com.intellij.execution.JavaTestFrameworkRunnableState
import com.intellij.execution.Location
import com.intellij.execution.application.ApplicationConfiguration
import com.intellij.execution.configuration.EnvironmentVariablesComponent
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.JavaRunConfigurationModule
import com.intellij.execution.configurations.ModuleBasedConfigurationOptions
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.TestSearchScope
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import com.intellij.execution.ui.DefaultJreSelector
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.modules
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.refactoring.listeners.RefactoringElementListener
import com.jetbrains.plugin.jtreg.configuration.model.TestData
import com.jetbrains.plugin.jtreg.service.JTRegService
import com.jetbrains.plugin.jtreg.util.TestGroup
import org.jdom.Element
import org.jetbrains.annotations.NonNls
import java.nio.file.Paths
import java.util.Objects

class JTRegConfiguration(name: String, project: Project, factory: ConfigurationFactory):
    JavaTestConfigurationBase(name, JavaRunConfigurationModule(project, true), factory) {

    private val settings: JTRegService = ApplicationManager.getApplication().getService(JTRegService::class.java)

    private var data: TestData = initTestData()
    private var alternativeClasspathEnabled = false
    private var alternativeJrePath: String = ""
    private var runCmd: String = ""

    override fun bePatternConfiguration(classes: List<PsiClass?>, method: PsiMethod) {
    }

    override fun beMethodConfiguration(location: Location<PsiMethod>) {
    }

    override fun beClassConfiguration(aClass: PsiClass) {
    }

    override fun isConfiguredByElement(element: PsiElement): Boolean {
        return false
    }

    override fun getTestType(): @NonNls String {
        return data.testKind
    }

    override fun getTestSearchScope(): TestSearchScope {
        return data.testSearchScope.scope
    }

    override fun setSearchScope(searchScope: TestSearchScope) {
        data.testSearchScope.scope = searchScope
    }

    override fun getState(executor: Executor, environment: ExecutionEnvironment): JavaTestFrameworkRunnableState<out JavaTestConfigurationBase> {
        return JTRegTestObject(this, environment)
    }

    override fun getValidModules(): Collection<Module?> {
        return emptyList()
    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration?> {
        return JTRegSettingsEditor(this)
    }

    override fun setVMParameters(value: String?) {
        value?.let {
            data.vmParameters = it
        }
    }

    override fun getVMParameters() = data.vmParameters

    override fun isAlternativeJrePathEnabled() = alternativeClasspathEnabled

    override fun setAlternativeJrePathEnabled(enabled: Boolean) {
        val changed = alternativeClasspathEnabled != enabled
        alternativeClasspathEnabled = enabled
        ApplicationConfiguration.onAlternativeJreChanged(changed, project)
    }

    override fun getAlternativeJrePath(): String? = alternativeJrePath

    override fun setAlternativeJrePath(path: String?) {
        path?.let {
            val changed = !Objects.equals(alternativeJrePath, path)
            alternativeJrePath = path
            ApplicationConfiguration.onAlternativeJreChanged(changed, project)
        }
    }

    override fun getRunClass() = data.className

    fun setRunClass(value: String) {
        data.className = value
    }

    override fun getPackage() = data.packageName

    fun setPackage(value: String) {
        data.packageName = value
    }

    fun getTestGroup() = data.testGroup

    fun setTestGroup(value: TestGroup?) {
        data.testGroup = value
    }

    override fun setProgramParameters(value: String?) {
        value?.let {
            data.parameters = it
        }
    }

    override fun getProgramParameters() = data.parameters

    override fun setWorkingDirectory(value: String?) {
        value?.let {
            data.workingDirectory = it
        }
    }

    override fun getWorkingDirectory() = data.workingDirectory

    override fun setEnvs(envs: Map<String?, String?>) {
        data.envVars = envs
    }

    override fun getEnvs() = data.envVars

    fun getTestMode() = data.testMode

    fun setTestMode(value: TestMode) {
        data.testMode = value
    }

    fun getRepeatMode() = data.repeat.mode

    fun setRepeatMode(value: String) {
        data.repeat.mode = value
    }

    fun getRepeatCount() = data.repeat.count

    fun setRepeatCount(value: Int) {
        data.repeat.count = value
    }

    fun getMaxRepeatCount() = data.repeat.maxCount

    fun setMaxRepeatCount(value: Int) {
        data.repeat.maxCount = value
    }

    fun getWestonScreens() = data.weston.screensCount

    fun setWestonScreens(value: Int) {
        data.weston.screensCount = value
    }

    fun getWestonScreenWidth() = data.weston.screenWidth

    fun setWestonScreenWidth(value: Int) {
        data.weston.screenWidth = value
    }

    fun getWestonScreenHeight() = data.weston.screenHeight

    fun setWestonScreenHeight(value: Int) {
        data.weston.screenHeight = value
    }

    fun getWakeFieldPath() = data.weston.wakefieldPath

    fun setWakeFieldPath(value: String) {
        data.weston.wakefieldPath = value
    }

    fun isUseWeston() = data.weston.useWeston

    fun setUseWeston(value: Boolean) {
        data.weston.useWeston = value
    }

    fun getNativeLibraryPath() = data.vmSettings.nativeLibPath

    fun setNativeLibraryPath(value: String) {
        data.vmSettings.nativeLibPath = value
    }

    override fun setPassParentEnvs(passParentEnvs: Boolean) {
        data.passParentEnvs = passParentEnvs
    }

    override fun isPassParentEnvs() = data.passParentEnvs

    fun getTestKind() = data.testKind

    fun setTestKind(value: String) {
        data.testKind = value
    }

    fun getTestCategory() = data.testCategory

    fun setTestCategory(value: String) {
        data.testCategory = value
    }

    fun getReportDir() = data.reportDir

    fun setReportDir(value: String) {
        data.reportDir = value
    }

    fun getExcludeList() = data.excludeList

    fun setExcludeList(value: String) {
        data.excludeList = value
    }

    fun getConcurrency() = data.concurrency

    fun setConcurrency(value: Int) {
        data.concurrency = value
    }

    fun getTimeoutFactor() = data.timeoutFactor

    fun setTimeoutFactor(value: Float) {
        data.timeoutFactor = value
    }

    fun getTimeLimit() = data.timeLimit

    fun setTimeLimit(value: Int) {
        data.timeLimit = value
    }

    fun getLockFile() = data.lock

    fun setLockFile(value: String) {
        data.lock = value
    }

    fun getIgnoreMode() = data.ignoreMode

    fun setIgnoreMode(value: IgnoreMode) {
        data.ignoreMode = value
    }

    fun getKeyword() = data.keyword

    fun setKeyword(value: String) {
        data.keyword = value
    }

    fun isAllowSecurityManager() = data.vmSettings.allowSecurityManager

    fun setAllowSecurityManager(value: Boolean) {
        data.vmSettings.allowSecurityManager = value
    }

    fun getTestJavaOptions() = data.vmSettings.javaOptions

    fun setTestJavaOptions(value: String) {
        data.vmSettings.javaOptions = value
    }

    fun getTestEnvVars() = data.vmSettings.envVars

    fun setTestEnvVars(value: Map<String?, String?>) {
        data.vmSettings.envVars = value
    }

    fun isVerboseOutput() = data.verboseOutput

    fun isGenerateXmlReport() = data.xmlReport

    fun getTestClasspath() = data.vmSettings.classPath

    fun setTestClasspath(value: List<ModuleBasedConfigurationOptions.ClasspathModification>) {
        data.vmSettings.classPath = value
    }

    fun isUseIdeaVMOptions() = data.vmSettings.useIdeaVMOptions

    fun setUseIdeaVMOptions(value: Boolean) {
        data.vmSettings.useIdeaVMOptions = value
    }

    fun getRunCmd() = runCmd

    fun setRunCmd(value: String) {
        runCmd = value
    }

    override fun getRefactoringElementListener(element: PsiElement?): RefactoringElementListener? {
        return null
    }

    override fun createTestConsoleProperties(executor: Executor): SMTRunnerConsoleProperties {
        return JTRegConfigurationConsoleProperties(executor, this)
    }

    fun getJDKString(): String? {
        if (alternativeClasspathEnabled) {
            alternativeJrePath.let {
                val sdk = ProjectJdkTable.getInstance().findJdk(it)
                sdk?.let {
                    return sdk.homePath
                } ?: run {
                    return alternativeJrePath
                }
            }
        } else {
            val defaultJdk = DefaultJreSelector.projectSdk(project).getNameAndDescription().first
            defaultJdk?.let {
                return ProjectJdkTable.getInstance().findJdk(defaultJdk)?.homePath
            }
        }
        return null
    }

    override fun getBeforeRunTasks(): List<BeforeRunTask<*>?> {
        return emptyList()
    }

    private fun initTestData(): TestData {
        val testData = TestData()

        testData.vmSettings.allowSecurityManager = settings.allowSecurityManager
        testData.concurrency = settings.concurrency
        testData.ignoreMode = settings.ignore
        testData.timeoutFactor = settings.timeoutFactor
        testData.timeLimit = settings.timeLimit
        testData.xmlReport = settings.xmlReport

        testData.weston = settings.westonSettings

        project.basePath?.let {
            val baseDir = Paths.get(it)
            val workDir = baseDir.resolve("JTWork").toString()
            testData.workingDirectory = workDir

            val reportDir = baseDir.resolve("JTReport").toString()
            testData.reportDir = reportDir
        }

        testData.testMode = settings.testMode

        val ret = mutableListOf<ModuleBasedConfigurationOptions.ClasspathModification>()
        project.modules.forEach { module ->
            ModuleRootManager.getInstance(module).orderEntries().recursively()
                .librariesOnly().allLibrariesAndSdkClassesRoots.forEach {
                    val path = it.path.replace("!/", "")
                    val cpm = ModuleBasedConfigurationOptions.ClasspathModification(path, false)
                    ret.add(cpm)
                }
        }
        testData.vmSettings.classPath = ret

        return testData
    }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)

        val jtreg = ConfigurationHelper.prepareTestData(data)

        element.setAttribute("configurationName", name)
        element.setAttribute("testKind", getTestKind())
        element.setAttribute("packageName", data.packageName)
        element.setAttribute("className", data.className)

        data.testGroup?.let {
            val testGroup = Element("testGroup")
            testGroup.setAttribute("testRootDirectory", it.testRootDirectory)
            testGroup.setAttribute("relativeTestDirectory", it.relativeTestDirectory)
            testGroup.setAttribute("groupName", it.groupName)
            element.addContent(testGroup)
        }

        element.setAttribute("alternativeClasspathEnabled", alternativeClasspathEnabled.toString())
        element.setAttribute("alternativeJrePath", alternativeJrePath)
        element.setAttribute("workingDirectory", data.workingDirectory)
        element.setAttribute("parameters", data.parameters)
        element.setAttribute("vmParameters", data.vmParameters)

        if (data.envVars.isNotEmpty()) {
            EnvironmentVariablesComponent.writeExternal(element, data.envVars)
        }

        element.setAttribute("runCmd", runCmd)
        element.addContent(jtreg)
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)

        data = ConfigurationHelper.readTestData(element, settings)

        `package` = element.getAttributeValue("packageName") ?: ""
        runClass = element.getAttributeValue("className") ?: ""

        element.getChild("testGroup")?.let { testGroup ->
            val testRootDirectory = testGroup.getAttributeValue("testRootDirectory") ?: ""
            val relativeTestDirectory = testGroup.getAttributeValue("relativeTestDirectory") ?: ""
            val groupName = testGroup.getAttributeValue("groupName") ?: ""
            val testGroupObject = TestGroup(testRootDirectory, relativeTestDirectory, groupName)
            setTestGroup(testGroupObject)
        }

        val altJrePath = element.getAttributeValue("alternativeJrePath") ?: ""
        val altJreEnabled = element.getAttributeBooleanValue("alternativeClasspathEnabled")
        if (altJrePath.isEmpty()) {
            alternativeJrePath = ""
            alternativeClasspathEnabled = false
        } else {
            alternativeJrePath = altJrePath
            alternativeClasspathEnabled = altJreEnabled
        }
        element.getAttributeValue("workingDirectory")?.let {
            data.workingDirectory = it
        } ?: run {
            project.basePath?.let {
                val baseDir = Paths.get(it)
                val workDir = baseDir.resolve("JTWork").toString()
                data.workingDirectory = workDir
            }
        }
        programParameters = element.getAttributeValue("parameters") ?: ""
        vmParameters = element.getAttributeValue("vmParameters") ?: ""

        val testKind = element.getAttributeValue("testKind")
        if (testKind != null && testKind.isNotEmpty()) {
            setTestKind(testKind)
        } else {
            if (`package`.isNotEmpty()) {
                setTestKind(TestData.TEST_DIRECTORY)
            } else if (runClass.isNotEmpty()) {
                setTestKind(TestData.TEST_CLASS)
            } else {
                setTestKind(TestData.TEST_GROUP)
            }
        }

        val configurationName = element.getAttributeValue("configurationName")
        if (configurationName != null && configurationName.isNotEmpty()) {
            name = configurationName
        } else {
            when (getTestKind()) {
                TestData.TEST_DIRECTORY -> {
                    name = `package`
                }
                TestData.TEST_GROUP -> {
                    name = getTestGroup()?.groupName ?: ""
                }
                TestData.TEST_CLASS -> {
                    name = runClass
                }
            }
        }

        element.getAttributeValue("runCmd")?.let { value ->
            setRunCmd(value)
        }
    }

}