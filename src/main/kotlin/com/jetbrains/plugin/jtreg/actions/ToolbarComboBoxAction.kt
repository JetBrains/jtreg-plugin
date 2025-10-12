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
package com.jetbrains.plugin.jtreg.actions

import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionUtil
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.ComboBoxAction
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.jetbrains.plugin.jtreg.configuration.JTRegConfiguration
import com.jetbrains.plugin.jtreg.configuration.JTRegConfigurationType
import com.jetbrains.plugin.jtreg.configuration.model.TestData
import com.jetbrains.plugin.jtreg.util.TestGroup
import com.jetbrains.plugin.jtreg.util.TestGroupUtils
import javax.swing.JComponent


class ToolbarComboBoxAction: ComboBoxAction(), DumbAware {

    private var selectedJdk: Sdk? = null
    private var selectedTestGroup: TestGroup? = null
    private var selectedRunnerSettings: RunnerAndConfigurationSettings? = null

    init {
        templatePresentation.text = "Run JTReg Tests"
    }

    override fun createPopupActionGroup(button: JComponent, context: DataContext): DefaultActionGroup {
        val actionGroup = DefaultActionGroup()

        val jdkGroup = DefaultActionGroup("Select JDK", true)
        val jdks = ProjectJdkTable.getInstance().allJdks

        if (jdks.isNotEmpty()) {
            jdks.forEach { jdk ->
                jdkGroup.add(object : AnAction(jdk.name) {
                    override fun actionPerformed(e: AnActionEvent) {
                        selectedJdk = jdk
                    }
                })
            }
        }

        actionGroup.addSeparator()
        actionGroup.add(CurrentJdkLabelAction { selectedJdk })
        actionGroup.add(jdkGroup)

        actionGroup.addSeparator()
        actionGroup.add(CurrentTestGroupLabelAction {
            selectedTestGroup?.let {
                "Group: ${it.relativeTestDirectory}:${it.groupName}"
            } ?: run {
                selectedRunnerSettings?.let {
                    "Config: ${selectedRunnerSettings?.configuration?.name}"
                } ?: run { "" }
            }
        })
        actionGroup.add(createJTRegTestGroupsActionGroup())
        actionGroup.add(createExistingConfigurationsActionGroup())

        actionGroup.addSeparator()
        actionGroup.add(object : AnAction("Run") {
            override fun actionPerformed(e: AnActionEvent) {
                val project = e.project ?: return

                val error = generateErrorMessage()
                error?.let {
                    Messages.showErrorDialog(error, "Configuration Is Not Ready")
                } ?: run {
                    if (selectedTestGroup != null) {
                        produceAndRunTestGroup(project)
                    } else if (selectedRunnerSettings != null) {
                        runExistingJTRegConfiguration()
                    }
                }
            }

            override fun getActionUpdateThread() = ActionUpdateThread.EDT
        })

        actionGroup.add(object : AnAction("Debug") {
            override fun actionPerformed(e: AnActionEvent) {
                val project = e.project ?: return

                val error = generateErrorMessage()
                error?.let {
                    Messages.showErrorDialog(error, "Configuration Is Not Ready")
                } ?: run {
                    if (selectedTestGroup != null) {
                        produceAndRunTestGroup(project, true)
                    } else if (selectedRunnerSettings != null) {
                        runExistingJTRegConfiguration(true)
                    }
                }
            }

            override fun getActionUpdateThread() = ActionUpdateThread.EDT
        })

        return actionGroup
    }

    private fun generateErrorMessage(): String? {
        if (selectedJdk == null || (selectedTestGroup != null && selectedRunnerSettings != null)) {
            return "Please select JDK and either Test Group or Existing Configuration"
        }
        return null
    }

    private fun produceAndRunTestGroup(project: Project, isDebug: Boolean = false) {
        val factory = JTRegConfigurationType.Util.getInstance()
            .configurationFactories
            .first()

        val runManager = RunManager.getInstance(project)
        val configuration = runManager.createConfiguration(
            "JTReg Tests",
            factory
        )

        // Configure the settings
        val settings = configuration.configuration as JTRegConfiguration
        settings.alternativeJrePath = selectedJdk?.homePath
        settings.isAlternativeJrePathEnabled = true
        settings.setTestKind(TestData.TEST_GROUP)
        settings.setTestGroup(selectedTestGroup)

        val moduleFile = LocalFileSystem.getInstance().findFileByPath(selectedTestGroup?.testRootDirectory!!)
        val module = ModuleUtil.findModuleForFile(
            moduleFile!!,
            project
        ) ?: return

        settings.setModule(module)
        settings.name = "${selectedTestGroup?.relativeTestDirectory}:${selectedTestGroup?.groupName}"
        settings.classpathModifications = emptyList()
        runManager.addConfiguration(configuration)
        runManager.selectedConfiguration = configuration

        val executor = if (isDebug) DefaultDebugExecutor.getDebugExecutorInstance() else DefaultRunExecutor.getRunExecutorInstance()
        ExecutionUtil.runConfiguration(configuration, executor)
    }

    private fun runExistingJTRegConfiguration(isDebug: Boolean = false) {
        selectedRunnerSettings?.let {
            val jtregConfig = it.configuration as JTRegConfiguration
            jtregConfig.alternativeJrePath = selectedJdk?.homePath
            jtregConfig.isAlternativeJrePathEnabled = true
            val executor = if (isDebug) DefaultDebugExecutor.getDebugExecutorInstance() else DefaultRunExecutor.getRunExecutorInstance()
            ExecutionUtil.runConfiguration(it, executor)
        }
    }

    private fun createJTRegTestGroupsActionGroup(): AnAction {
        return object : ActionGroup("Choose From Test Groups", true) {

            override fun getChildren(e: AnActionEvent?): Array<AnAction> {
                val groups = TestGroupUtils.getTestGroups()
                if (groups.isEmpty()) {
                    return arrayOf(
                        object : AnAction("No Test Groups Found") {
                            override fun actionPerformed(e: AnActionEvent) {
                            }
                        }
                    )
                }
                return groups.map { group ->
                    object : AnAction("${group.relativeTestDirectory}:${group.groupName}") {
                        override fun actionPerformed(e: AnActionEvent) {
                            selectedTestGroup = group
                            selectedRunnerSettings = null
                        }
                    }
                }.toTypedArray()
            }
        }
    }

    private fun createExistingConfigurationsActionGroup(): AnAction {
        return object : ActionGroup("Choose From Saved Configurations", true) {

            override fun getChildren(e: AnActionEvent?): Array<AnAction> {
                val project = e?.project ?: return emptyArray()
                val runManager = RunManager.getInstance(project)

                val configurations = runManager
                    .allSettings
                    .filter { it.type is JTRegConfigurationType }

                if (configurations.isEmpty()) {
                    return arrayOf(
                        object : AnAction("No Configurations Found") {
                            override fun actionPerformed(e: AnActionEvent) {
                            }
                        }
                    )
                }

                return configurations.map { configSettings ->
                    object : AnAction(configSettings.name) {
                        override fun actionPerformed(e: AnActionEvent) {
                            selectedRunnerSettings = configSettings
                            selectedTestGroup = null
                        }
                    }
                }.toTypedArray()
            }
        }
    }
}