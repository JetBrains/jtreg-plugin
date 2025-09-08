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

import com.intellij.execution.application.JavaSettingsEditorBase
import com.intellij.execution.configuration.EnvironmentVariablesComponent
import com.intellij.execution.ui.CommonJavaFragments
import com.intellij.execution.ui.CommonParameterFragments
import com.intellij.execution.ui.DefaultJreSelector
import com.intellij.execution.ui.JrePathEditor
import com.intellij.execution.ui.ModuleClasspathCombo
import com.intellij.execution.ui.SettingsEditorFragment
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.RawCommandLineEditor
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.TextComponentEmptyText
import com.intellij.util.ui.JBUI
import com.jetbrains.plugin.jtreg.BuildConfig
import com.jetbrains.plugin.jtreg.JTRegBundle
import com.jetbrains.plugin.jtreg.configuration.model.TestData
import com.jetbrains.plugin.jtreg.service.JTRegService
import com.jetbrains.plugin.jtreg.ui.ClasspathModifier
import com.jetbrains.plugin.jtreg.ui.VariableSelectionFragment
import com.jetbrains.plugin.jtreg.util.TestGroup
import com.jetbrains.plugin.jtreg.util.TestGroupUtils
import kotlinx.coroutines.runBlocking
import java.awt.BorderLayout
import java.util.function.Consumer
import javax.swing.DefaultComboBoxModel
import java.util.function.Function


class JTRegSettingsEditor(private val configuration: JTRegConfiguration): JavaSettingsEditorBase<JTRegConfiguration>(configuration) {

    private val jtRegService = ApplicationManager.getApplication().getService(JTRegService::class.java)

    init {
        val modalTask = object : Task.Modal(project, "Detecting Local JDK Builds", true) {
            override fun run(indicator: ProgressIndicator) {
                runBlocking {
                    SdkUtils.detectLocalJdkBuilds(project)
                }
            }
        }
        ProgressManager.getInstance().run(modalTask)
    }

    override fun customizeFragments(
        fragments: MutableList<SettingsEditorFragment<JTRegConfiguration, *>>,
        moduleClasspath: SettingsEditorFragment<JTRegConfiguration, ModuleClasspathCombo>,
        commonParameterFragments: CommonParameterFragments<JTRegConfiguration>
    ) {
        attachJreAndNativePathFragments(fragments)

        // tests environment
        attachTestEnvVarsFragment(fragments)
        attachJTRegVMOptions(fragments)
        attachTestModeFragment(fragments)

        // tests selection group
        attachTestKindFragment(fragments)
        attachTestCategoryFragment(fragments)
        attachKeywordFragment(fragments)
        attachExcludeListFragment(fragments)


        // jtreg settings
        if (BuildConfig.isJetBrainsVendor()) {
            attachRepeatModeFragment(fragments)
        }

        attachReportDirOverrideFragment(fragments)
        attachConcurrencySettings(fragments)
        attachTimeoutFactorOverride(fragments)
        attachTimeLimitOverride(fragments)
        attachIgnoreModeFragment(fragments)

        if (BuildConfig.isJetBrainsVendor() && SystemInfo.isLinux) {
            attachWestonSettingsFragment(fragments)
        }

        attachRunCmdFragment(fragments)
    }

    override fun isInplaceValidationSupported(): Boolean {
        return true
    }

    private fun attachTestEnvVarsFragment(fragments: MutableList<SettingsEditorFragment<JTRegConfiguration, *>>) {
        val envVarsComponent = EnvironmentVariablesComponent()
        envVarsComponent.text = ""
        envVarsComponent.toolTipText = ""
        envVarsComponent.component.textField.setFont(EditorUtil.getEditorFont(JBUI.Fonts.label().size))
        val labeledEnvComp = LabeledComponent.create(envVarsComponent, "Environment variables for tests", BorderLayout.WEST)
        val envVarsFragment: SettingsEditorFragment<JTRegConfiguration, LabeledComponent<EnvironmentVariablesComponent>> =
            SettingsEditorFragment<JTRegConfiguration, LabeledComponent<EnvironmentVariablesComponent>>(
                "testEnvVars", "Environment variables for tests", TESTS_ENVIRONMENT_GROUP,
                labeledEnvComp,
                { config, field -> field.component.envs = config.getTestEnvVars() },
                { config, field -> config.setTestEnvVars(field.component.envs) },
                { true }
            )
        fragments.add(envVarsFragment)
    }

    private fun attachJreAndNativePathFragments(fragments: MutableList<SettingsEditorFragment<JTRegConfiguration, *>>) {
        val jreSelector: DefaultJreSelector = DefaultJreSelector.projectSdk(project)
        val jrePath: SettingsEditorFragment<JTRegConfiguration, JrePathEditor> = CommonJavaFragments.createJrePath(jreSelector)
        fragments.add(jrePath)

        val classPathModifier = ClasspathModifier(configuration)
        fragments.add(classPathModifier)

        val nativeTestLibComponent = TextFieldWithBrowseButton(null, this)
        nativeTestLibComponent.addBrowseFolderListener("Native Test Library Path", null, project,
            FileChooserDescriptorFactory.createSingleFolderDescriptor())
        val nativeTestLibField = LabeledComponent.create(nativeTestLibComponent, "Native test library path", BorderLayout.WEST)

        val nativeTestLibFragment: SettingsEditorFragment<JTRegConfiguration, LabeledComponent<TextFieldWithBrowseButton>> =
            SettingsEditorFragment<JTRegConfiguration, LabeledComponent<TextFieldWithBrowseButton>>(
                "nativeTestLibraryName", "Native test library path", TESTS_ENVIRONMENT_GROUP,
                nativeTestLibField,
                { config, field ->
                    field.component.text = FileUtil.toSystemDependentName(config.getNativeLibraryPath()) },
                { config, field -> config.setNativeLibraryPath(FileUtil.toSystemIndependentName(field.component.text)) },
                { config -> StringUtil.isNotEmpty(config.getNativeLibraryPath()) }
            )
        fragments.add(nativeTestLibFragment)

        jrePath.addSettingsEditorListener {
            if (nativeTestLibComponent.text.isEmpty()) {
                val name = ((jrePath.component as JrePathEditor).jrePathOrName) ?: return@addSettingsEditorListener
                val sdk = ProjectJdkTable.getInstance().findJdk(name)
                val nativeLibPath = SdkUtils.getNativeSupportLibs(sdk?.homePath)
                nativeLibPath?.let {
                    nativeTestLibComponent.text = FileUtil.toSystemDependentName(nativeLibPath)
                }
            }
        }
    }

    private fun attachTestModeFragment(fragments: MutableList<SettingsEditorFragment<JTRegConfiguration, *>>) {
        val mode: VariableSelectionFragment = VariableSelectionFragment.createFragment(
            "testMode", "Test mode", TESTS_ENVIRONMENT_GROUP,
            { TestMode.MODES.map { it.name}.toTypedArray() },
            { config -> config.getTestMode().name },
            { config, mode -> config.setTestMode(TestMode.valueOf(mode)) },
            { true }
        )

        mode.variantNameProvider = Function<String, String> { JTRegBundle.message("jtreg.configuration.test.mode." + it.replace(' ', '.').lowercase()) }
        fragments.add(mode)
    }

    private fun attachJTRegVMOptions(fragments: MutableList<SettingsEditorFragment<JTRegConfiguration, *>>) {
        val vmOptions = RawCommandLineEditor()
        val vmOptionsField = LabeledComponent.create(vmOptions, "Java options", BorderLayout.WEST)
        vmOptions.textField.setFont(EditorUtil.getEditorFont(JBUI.Fonts.label().size))
        val message = "Test Java Options"
        vmOptions.editorField.emptyText.text = message
        TextComponentEmptyText.setupPlaceholderVisibility(vmOptions.editorField)
        val vmParameters: SettingsEditorFragment<JTRegConfiguration, LabeledComponent<RawCommandLineEditor>> =
            SettingsEditorFragment<JTRegConfiguration, LabeledComponent<RawCommandLineEditor>>(
                "testJavaOptions", "Test Java options", TESTS_ENVIRONMENT_GROUP,
                vmOptionsField,
                { config, field -> field.component.text = config.getTestJavaOptions() },
                { config, field -> config.setTestJavaOptions(field.component.text) },
                { config -> StringUtil.isNotEmpty(config.getTestJavaOptions()) }
            )
        fragments.add(vmParameters)
    }

    private fun attachTestKindFragment(fragments: MutableList<SettingsEditorFragment<JTRegConfiguration, *>>) {
        val testKind: VariableSelectionFragment = VariableSelectionFragment.createFragment(
            "testKind", "Test kind", TESTS_SELECTION_GROUP,
            { TestData.TEST_KINDS },
            { config -> config.getTestKind() },
            { config, kind -> config.setTestKind(kind) },
            { true }
        )

        testKind.variantNameProvider = Function<String, String> { JTRegBundle.message("jtreg.configuration.test.kind." + it.replace(' ', '.').lowercase()) }
        fragments.add(testKind)

        val fileBrowseField = TextFieldWithBrowseButton(null, this)
        fileBrowseField.addBrowseFolderListener("Run Class", null, project,
            FileChooserDescriptorFactory.createSingleFileDescriptor())
        val fileField = LabeledComponent.create(fileBrowseField, "Class name", BorderLayout.WEST)

        val fileFragment: SettingsEditorFragment<JTRegConfiguration, LabeledComponent<TextFieldWithBrowseButton>> =
            SettingsEditorFragment<JTRegConfiguration, LabeledComponent<TextFieldWithBrowseButton>>(
                "className", "Test class", null,
                fileField,
                { config, field ->
                    field.component.text = FileUtil.toSystemDependentName(config.runClass)
                },
                { config, field ->
                    config.runClass = FileUtil.toSystemIndependentName(field.component.text)
                },
                { config -> config.getTestKind() == TestData.TEST_CLASS }
            )
        fragments.add(fileFragment)

        val directoryBrowseField = TextFieldWithBrowseButton(null, this)
        directoryBrowseField.addBrowseFolderListener("Directory", null, project,
            FileChooserDescriptorFactory.createSingleFolderDescriptor())
        val directoryField = LabeledComponent.create(directoryBrowseField, "Directory", BorderLayout.WEST)

        val directoryFragment: SettingsEditorFragment<JTRegConfiguration, LabeledComponent<TextFieldWithBrowseButton>> =
            SettingsEditorFragment<JTRegConfiguration, LabeledComponent<TextFieldWithBrowseButton>>(
                "packageName", "Test directory", null,
                directoryField,
                { config, field ->
                    field.component.text = FileUtil.toSystemDependentName(config.`package`)
                },
                { config, field ->
                    config.`package` = FileUtil.toSystemIndependentName(field.component.text)
                },
                { config -> config.getTestKind() == TestData.TEST_DIRECTORY }
            )
        fragments.add(directoryFragment)

        val testGroupComboBox = ComboBox<TestGroup>().apply {
            model = DefaultComboBoxModel(TestGroupUtils.getTestGroups())

            renderer = SimpleListCellRenderer.create<TestGroup>("") { value ->
                value?.let { "${it.relativeTestDirectory}:${it.groupName}" } ?: ""
            }
        }
        val testGroupField = LabeledComponent.create(testGroupComboBox, "Test group", BorderLayout.WEST)
        val testGroupFragment = SettingsEditorFragment<JTRegConfiguration, LabeledComponent<ComboBox<TestGroup>>>(
            "testGroup", "Test group", null,
            testGroupField,
            { config, field ->
                field.component.model = DefaultComboBoxModel(TestGroupUtils.getTestGroups())
                field.component.selectedItem = config.getTestGroup()
            },
            { config, field -> config.setTestGroup((field.component.selectedItem as? TestGroup)) },
            { config -> config.getTestKind() == TestData.TEST_GROUP }
        )
        fragments.add(testGroupFragment)

        testKind.addSettingsEditorListener {
            fileFragment.isSelected = TestData.TEST_CLASS == testKind.selectedVariant
            directoryFragment.isSelected = TestData.TEST_DIRECTORY == testKind.selectedVariant
            testGroupFragment.isSelected = TestData.TEST_GROUP == testKind.selectedVariant
        }

        testKind.toggleListener = Consumer<String> { s ->
            if (TestData.TEST_CLASS == s) {
                IdeFocusManager.getInstance(project).requestFocus(fileFragment.getEditorComponent(), false)
            } else if (TestData.TEST_DIRECTORY == s) {
                IdeFocusManager.getInstance(project).requestFocus(directoryFragment.getEditorComponent(), false)
            }
        }
    }

    private fun attachTestCategoryFragment(fragments: MutableList<SettingsEditorFragment<JTRegConfiguration, *>>) {
        val category: VariableSelectionFragment = VariableSelectionFragment.createFragment(
            "category", "Test category", TESTS_SELECTION_GROUP,
            { TestData.TEST_TYPES },
            { config -> config.getTestCategory() },
            { config, category -> config.setTestCategory(category) },
            { true }
        )
        category.variantHintProvider = Function<String, String> { JTRegBundle.message("jtreg.configuration.test.category." + it.replace(' ', '.').lowercase()) }
        fragments.add(category)
    }

    private fun attachRepeatModeFragment(fragments: MutableList<SettingsEditorFragment<JTRegConfiguration, *>>) {
        val repeat: VariableSelectionFragment = VariableSelectionFragment.createFragment(
            "repeat", "Repeat (JBR JTReg only)", JTREG_SETTINGS_GROUP,
            { RepeatCount.REPEAT_TYPES },
            { config -> config.getRepeatMode() },
            { config, mode -> config.setRepeatMode(mode) },
            { true }
        )

        repeat.variantNameProvider = Function<String, String> { JTRegBundle.message("jtreg.configuration.repeat.mode." + it.replace(' ', '.').lowercase()) }
        fragments.add(repeat)

        val countField: LabeledComponent<JBTextField> = LabeledComponent.create(JBTextField(), "Test runs count", BorderLayout.WEST)
        val countFragment: SettingsEditorFragment<JTRegConfiguration, LabeledComponent<JBTextField>> =
            SettingsEditorFragment<JTRegConfiguration, LabeledComponent<JBTextField>>(
                "count", null, null, countField,
                { config, field -> field.component.text = config.getRepeatCount().toString() },
                { config, field -> config.setRepeatCount(field.component.text.toIntOrNull() ?: 1) },
                { config -> RepeatCount.N == config.getRepeatMode() }
            )
        fragments.add(countFragment)

        val maxCountField: LabeledComponent<JBTextField> = LabeledComponent.create(JBTextField(), "Max test runs count", BorderLayout.WEST)
        val maxCountFragment: SettingsEditorFragment<JTRegConfiguration, LabeledComponent<JBTextField>> =
            SettingsEditorFragment<JTRegConfiguration, LabeledComponent<JBTextField>>(
                "maxCount", null, null, maxCountField,
                { config, field -> field.component.text = config.getMaxRepeatCount().toString() },
                { config, field -> config.setMaxRepeatCount(field.component.text.toIntOrNull() ?: 1) },
                { config -> RepeatCount.UNTIL_FAILURE == config.getRepeatMode() || RepeatCount.UNTIL_SUCCESS == config.getRepeatMode() }
            )
        fragments.add(maxCountFragment)

        repeat.addSettingsEditorListener {
            val repeatN = RepeatCount.N == repeat.selectedVariant
            val repeatUntil = RepeatCount.UNTIL_FAILURE == repeat.selectedVariant || RepeatCount.UNTIL_SUCCESS == repeat.selectedVariant
            countFragment.isSelected = repeatN
            maxCountFragment.isSelected = repeatUntil
        }

        repeat.toggleListener = Consumer<String> { s ->
            if (RepeatCount.N == s) {
                IdeFocusManager.getInstance(project).requestFocus(countFragment.getEditorComponent(), false)
            } else if (RepeatCount.UNTIL_FAILURE == s || RepeatCount.UNTIL_SUCCESS == s) {
                IdeFocusManager.getInstance(project).requestFocus(maxCountFragment.getEditorComponent(), false)
            }
        }
    }

    private fun attachWestonSettingsFragment(fragments: MutableList<SettingsEditorFragment<JTRegConfiguration, *>>) {
        val useWestonComponent = LabeledComponent.create(
            JBCheckBox("Use Weston (JBR JTReg only)", configuration.isUseWeston()),
            "Use Weston", BorderLayout.WEST
        )
        val useWestonFragment: SettingsEditorFragment<JTRegConfiguration, LabeledComponent<JBCheckBox>> =
            SettingsEditorFragment<JTRegConfiguration, LabeledComponent<JBCheckBox>>(
                "useWeston", "Use Weston", TESTS_ENVIRONMENT_GROUP,
                useWestonComponent,
                { config, field -> field.component.isSelected = config.isUseWeston() },
                { config, field -> config.setUseWeston(field.component.isSelected) },
                { true }
            )

        val wakeFieldBrowseField = TextFieldWithBrowseButton(null, this)
        wakeFieldBrowseField.addBrowseFolderListener(
            "WakeField Library Path", null, project,
            FileChooserDescriptorFactory.createSingleFileDescriptor()
        )
        val wakeFieldField = LabeledComponent.create(wakeFieldBrowseField, "WakeField Library path", BorderLayout.WEST)

        val wakeFieldFragment = SettingsEditorFragment<JTRegConfiguration, LabeledComponent<TextFieldWithBrowseButton>>(
            "wakeFieldPath", "WakeField library path", null,
            wakeFieldField,
            { config, field -> field.component.text = config.getWakeFieldPath() },
            { config, field -> config.setWakeFieldPath(field.component.text) },
            { config -> config.isUseWeston() }
        )

        val screensField = LabeledComponent.create(JBTextField(), "Weston screens count", BorderLayout.WEST)
        val screensFragment = SettingsEditorFragment<JTRegConfiguration, LabeledComponent<JBTextField>>(
            "westonScreens", "Weston screens count", null,
            screensField,
            { config, field -> field.component.text = config.getWestonScreens().toString() },
            { config, field -> config.setWestonScreens(field.component.text.toIntOrNull() ?: jtRegService.westonSettings.screensCount) },
            { config -> config.isUseWeston() }
        )

        val screenWidthField = LabeledComponent.create(JBTextField(), "Weston screen width", BorderLayout.WEST)
        val screenWidthFragment = SettingsEditorFragment<JTRegConfiguration, LabeledComponent<JBTextField>>(
            "westonWidth", "Weston screen width", null,
            screenWidthField,
            { config, field -> field.component.text = config.getWestonScreenWidth().toString() },
            { config, field -> config.setWestonScreenWidth(field.component.text.toIntOrNull() ?: jtRegService.westonSettings.screenWidth) },
            { config -> config.isUseWeston() }
        )

        val screenHeightField = LabeledComponent.create(JBTextField(), "Weston screen height", BorderLayout.WEST)
        val screenHeightFragment = SettingsEditorFragment<JTRegConfiguration, LabeledComponent<JBTextField>>(
            "westonHeight", "Weston screen height", null,
            screenHeightField,
            { config, field -> field.component.text = config.getWestonScreenHeight().toString() },
            { config, field -> config.setWestonScreenHeight(field.component.text.toIntOrNull() ?: jtRegService.westonSettings.screenHeight) },
            { config -> config.isUseWeston() },
        )

        fragments.add(useWestonFragment)
        fragments.add(wakeFieldFragment)
        fragments.add(screensFragment)
        fragments.add(screenWidthFragment)
        fragments.add(screenHeightFragment)

        useWestonFragment.addSettingsEditorListener {
            wakeFieldFragment.component.isVisible = useWestonComponent.component.isSelected
            screensFragment.component.isVisible = useWestonComponent.component.isSelected
            screenWidthFragment.component.isVisible = useWestonComponent.component.isSelected
            screenHeightFragment.component.isVisible = useWestonComponent.component.isSelected
        }
    }

    private fun attachExcludeListFragment(fragments: MutableList<SettingsEditorFragment<JTRegConfiguration, *>>) {
        val excludeListBrowseField = TextFieldWithBrowseButton(null, this)
        excludeListBrowseField.addBrowseFolderListener("Exclude List", null, project,
            FileChooserDescriptorFactory.createSingleFileDescriptor())
        val excludeListField = LabeledComponent.create(excludeListBrowseField, "Exclude list", BorderLayout.WEST)
        val excludeListFragment: SettingsEditorFragment<JTRegConfiguration, LabeledComponent<TextFieldWithBrowseButton>> =
            SettingsEditorFragment<JTRegConfiguration, LabeledComponent<TextFieldWithBrowseButton>>(
                "excludeList", "Set exclude list", TESTS_SELECTION_GROUP,
                excludeListField,
                { config, field -> field.component.text = FileUtil.toSystemDependentName(config.getExcludeList()) },
                { config, field -> config.setExcludeList(FileUtil.toSystemIndependentName(field.component.text)) },
                { config -> StringUtil.isNotEmpty(config.getExcludeList()) }
            )
        fragments.add(excludeListFragment)
    }

    private fun attachReportDirOverrideFragment(fragments: MutableList<SettingsEditorFragment<JTRegConfiguration, *>>) {
        val reportDirBrowseField = TextFieldWithBrowseButton(null, this)
        reportDirBrowseField.addBrowseFolderListener("Report Directory", null, project,
            FileChooserDescriptorFactory.createSingleFolderDescriptor())
        val reportDirField = LabeledComponent.create(reportDirBrowseField, "Report directory", BorderLayout.WEST)
        val reportDirFragment: SettingsEditorFragment<JTRegConfiguration, LabeledComponent<TextFieldWithBrowseButton>> =
            SettingsEditorFragment<JTRegConfiguration, LabeledComponent<TextFieldWithBrowseButton>>(
                "reportDir", "Override report directory", JTREG_SETTINGS_GROUP,
                reportDirField,
                { config, field -> field.component.text = FileUtil.toSystemDependentName(config.getReportDir()) },
                { config, field -> config.setReportDir(FileUtil.toSystemIndependentName(field.component.text)) },
                { config -> StringUtil.isNotEmpty(config.getReportDir()) }
            )
        fragments.add(reportDirFragment)
    }


    private fun attachTimeoutFactorOverride(fragments: MutableList<SettingsEditorFragment<JTRegConfiguration, *>>) {
        val textField = LabeledComponent.create(JBTextField(), "Timeout factor", BorderLayout.WEST)
        val textFragment: SettingsEditorFragment<JTRegConfiguration, LabeledComponent<JBTextField>> =
            SettingsEditorFragment<JTRegConfiguration, LabeledComponent<JBTextField>>(
                "timeoutFactor", "Override timeout factor", JTREG_SETTINGS_GROUP,
                textField,
                { config, field -> field.component.text = config.getTimeoutFactor().toString() },
                { config, field -> config.setTimeoutFactor(field.component.text.toFloatOrNull() ?: 1.0f) },
                { config -> config.getTimeoutFactor() != jtRegService.timeoutFactor }
            )
        textFragment.actionDescription = configuration.getTimeoutFactor().toString()
        textField.addPropertyChangeListener {
            textFragment.actionDescription = configuration.getTimeoutFactor().toString()
        }
        fragments.add(textFragment)
    }

    private fun attachTimeLimitOverride(fragments: MutableList<SettingsEditorFragment<JTRegConfiguration, *>>) {
        val textField = LabeledComponent.create(JBTextField(), "Time limit (seconds)", BorderLayout.WEST)
        val textFragment: SettingsEditorFragment<JTRegConfiguration, LabeledComponent<JBTextField>> =
            SettingsEditorFragment<JTRegConfiguration, LabeledComponent<JBTextField>>(
                "timeLimit", "Override time limit", JTREG_SETTINGS_GROUP,
                textField,
                { config, field -> field.component.text = config.getTimeLimit().toString() },
                { config, field -> config.setTimeLimit(field.component.text.toIntOrNull() ?: 3600) },
                { config -> config.getTimeLimit() != jtRegService.timeLimit }
            )
        textFragment.actionDescription = "${configuration.getTimeLimit()} seconds"
        textField.addPropertyChangeListener {
            textFragment.actionDescription = "${configuration.getTimeLimit()} seconds"
        }
        fragments.add(textFragment)
    }

    private fun attachConcurrencySettings(fragments: MutableList<SettingsEditorFragment<JTRegConfiguration, *>>) {
        val textField = LabeledComponent.create(JBTextField(), "Concurrency", BorderLayout.WEST)
        val textFragment: SettingsEditorFragment<JTRegConfiguration, LabeledComponent<JBTextField>> =
            SettingsEditorFragment<JTRegConfiguration, LabeledComponent<JBTextField>>(
                "concurrency", "Override concurrency factor", JTREG_SETTINGS_GROUP,
                textField,
                { config, field -> field.component.text = config.getConcurrency().toString() },
                { config, field -> config.setConcurrency(field.component.text.toIntOrNull() ?: 1) },
                { config -> config.getConcurrency() != jtRegService.concurrency }
            )
        textFragment.actionDescription = configuration.getConcurrency().toString()

        textField.addPropertyChangeListener {
            textFragment.actionDescription = configuration.getConcurrency().toString()
        }

        fragments.add(textFragment)

        val lockFileBrowseField = TextFieldWithBrowseButton(null, this)
        lockFileBrowseField.addBrowseFolderListener(
            "Lock File", null, project,
            FileChooserDescriptorFactory.createSingleFileDescriptor()
        )
        val lockFileField = LabeledComponent.create(lockFileBrowseField, "Lock file", BorderLayout.WEST)
        val lockFileFragment: SettingsEditorFragment<JTRegConfiguration, LabeledComponent<TextFieldWithBrowseButton>> =
            SettingsEditorFragment<JTRegConfiguration, LabeledComponent<TextFieldWithBrowseButton>>(
                "lockFile", "Lock file to use for tests in \"exclusive access\"", null,
                lockFileField,
                { config, field -> field.component.text = FileUtil.toSystemDependentName(config.getLockFile()) },
                { config, field -> config.setLockFile(FileUtil.toSystemIndependentName(field.component.text)) },
                { config -> config.getConcurrency() != 1 }
            )

        textFragment.addSettingsEditorListener {
            lockFileFragment.isSelected = (textField.component.text.toIntOrNull() ?: 1) > 1
        }

        fragments.add(lockFileFragment)
    }

    private fun attachIgnoreModeFragment(fragments: MutableList<SettingsEditorFragment<JTRegConfiguration, *>>) {
        val ignoreMode: VariableSelectionFragment = VariableSelectionFragment.createFragment(
            "ignoreMode", "Ignore mode", JTREG_SETTINGS_GROUP,
            { IgnoreMode.MODES.map { it.name}.toTypedArray() },
            { config -> config.getIgnoreMode().name },
            { config, mode -> config.setIgnoreMode(IgnoreMode.valueOf(mode)) },
            { config -> config.getIgnoreMode() != jtRegService.ignore }
        )

        ignoreMode.variantNameProvider = Function<String, String> { JTRegBundle.message("jtreg.configuration.test.ignore." + it.replace(' ', '.').lowercase()) }
        fragments.add(ignoreMode)
    }

    private fun attachKeywordFragment(fragments: MutableList<SettingsEditorFragment<JTRegConfiguration, *>>) {
        val textField = LabeledComponent.create(JBTextField(), "Keyword", BorderLayout.WEST)
        val textFragment: SettingsEditorFragment<JTRegConfiguration, LabeledComponent<JBTextField>> =
            SettingsEditorFragment<JTRegConfiguration, LabeledComponent<JBTextField>>(
                "keyword", "Keyword", TESTS_SELECTION_GROUP,
                textField,
                { config, field -> field.component.text = config.getKeyword() },
                { config, field -> config.setKeyword(field.component.text) },
                { config -> StringUtil.isNotEmpty(config.getKeyword()) }
            )
        textFragment.actionDescription = configuration.getKeyword()
        textField.addPropertyChangeListener {
            textFragment.actionDescription = configuration.getKeyword()
        }
        fragments.add(textFragment)
    }

    private fun attachRunCmdFragment(fragments: MutableList<SettingsEditorFragment<JTRegConfiguration, *>>) {
        val textField = LabeledComponent.create(JBTextField(), "Test @run command", BorderLayout.WEST)
        textField.isEnabled = false
        val textFragment: SettingsEditorFragment<JTRegConfiguration, LabeledComponent<JBTextField>> =
            SettingsEditorFragment<JTRegConfiguration, LabeledComponent<JBTextField>>(
                "runCmd", "@run command", TESTS_SELECTION_GROUP,
                textField,
                { config, field -> field.component.text = config.getRunCmd() },
                { config, field -> config.setRunCmd(field.component.text) },
                { config -> StringUtil.isNotEmpty(config.getRunCmd()) }
            )
        textFragment.actionDescription = configuration.getRunCmd()
        fragments.add(textFragment)
    }

    companion object {

        private const val TESTS_ENVIRONMENT_GROUP = "Tests Environment"
        private const val TESTS_SELECTION_GROUP = "Tests Selection"
        private const val JTREG_SETTINGS_GROUP = "JTReg Settings"

    }

}