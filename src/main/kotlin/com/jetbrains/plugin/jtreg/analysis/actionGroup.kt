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
package com.jetbrains.plugin.jtreg.analysis

import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.EditRunConfigurationsAction
import com.intellij.execution.actions.RunConfigurationProducer
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionUtil
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.jetbrains.plugin.jtreg.configuration.JTRegClassConfigurationProducer
import java.awt.Component
import java.awt.Point


fun showActionGroupPopup(actionGroupId: String, dataContext: DataContext,
                         component: Component, point: Point) {
    val actionGroup = ActionManager.getInstance().getAction(actionGroupId) as? ActionGroup ?: return

    // Create and show the popup for the action group
    JBPopupFactory.getInstance()
        .createActionGroupPopup(
            null,
            actionGroup,
            dataContext,
            JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
            true
        )
        .showInScreenCoordinates(component, point)

}


private fun actionGroup(id: String) = ActionManager.getInstance().getAction(id) as ActionGroup

class RunSingleJtregTest: DumbAwareAction() {

    override fun actionPerformed(e: AnActionEvent) {
        runOrDebugTest(e, isDebug = false)
    }

    override fun update(e: AnActionEvent) {
        val runCmd = e.dataContext.getData(JTREG_RUN_CMD) ?: return
        e.presentation.text = "Run $runCmd"
    }

    override fun getActionUpdateThread() = ActionUpdateThread.EDT

}

class DebugSingleJtregTest: DumbAwareAction() {

    override fun actionPerformed(e: AnActionEvent) {
        runOrDebugTest(e, isDebug = true)
    }

    override fun update(e: AnActionEvent) {
        val runCmd = e.dataContext.getData(JTREG_RUN_CMD) ?: return
        e.presentation.text = "Debug $runCmd"
    }

    override fun getActionUpdateThread() = ActionUpdateThread.EDT

}

class ModifyJtregTestConfig: DumbAwareAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val settings = produceConfiguration(e, project) ?: return

        val runManager = RunManager.getInstance(project)
        runManager.createConfiguration(settings.configuration, settings.factory)
        runManager.addConfiguration(settings)
        runManager.setUniqueNameIfNeeded(settings)

        runManager.selectedConfiguration = runManager.findSettings(settings.configuration)

        EditRunConfigurationsAction().actionPerformed(e)
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

}

private fun runOrDebugTest(e: AnActionEvent, isDebug: Boolean = false) {
    val project = e.project ?: return
    val settings = produceConfiguration(e, project) ?: return

    val runManager = RunManager.getInstance(project)
    runManager.setTemporaryConfiguration(settings)
    val executor = if (isDebug) DefaultDebugExecutor.getDebugExecutorInstance() else DefaultRunExecutor.getRunExecutorInstance()
    ExecutionUtil.runConfiguration(settings, executor)
}

private fun produceConfiguration(e: AnActionEvent, project: Project): RunnerAndConfigurationSettings? {
    val runClass = e.dataContext.getData(JTREG_RUN_CLASS) ?: return null
    val runCmd = e.dataContext.getData(JTREG_RUN_CMD) ?: return null
    val psiElement = e.dataContext.getData(CommonDataKeys.PSI_ELEMENT) ?: return null

    val fullContext = SimpleDataContext.builder()
        .setParent(SimpleDataContext.getProjectContext(project))
        .add(JTREG_RUN_CLASS, runClass)
        .add(JTREG_RUN_CMD, runCmd)
        .add(CommonDataKeys.PSI_ELEMENT, psiElement)
        .build()

    val ctx = ConfigurationContext.getFromContext(fullContext)

    val producer = RunConfigurationProducer.getInstance(JTRegClassConfigurationProducer::class.java)
    val settings = producer.createConfigurationFromContext(ctx)?.configurationSettings
    return settings
}

val JTREG_RUN_CLASS: DataKey<String> = DataKey.create("JTREG_TEST_FILE")
val JTREG_RUN_CMD: DataKey<String> = DataKey.create("JTREG_RUN_CMD")