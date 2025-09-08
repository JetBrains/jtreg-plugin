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
package com.jetbrains.plugin.jtreg.ui

import com.intellij.execution.CantRunException
import com.intellij.execution.ExecutionBundle
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.ModuleBasedConfigurationOptions.ClasspathModification
import com.intellij.execution.ui.SettingsEditorFragment
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.AnActionButton
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.table.JBTable
import com.intellij.util.Function
import com.intellij.util.PathUtil
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.ListTableModel
import com.jetbrains.plugin.jtreg.configuration.JTRegConfiguration
import java.awt.BorderLayout
import java.util.function.BiConsumer
import java.util.function.Predicate
import javax.swing.JPanel
import javax.swing.JTable

class ClasspathModifier: SettingsEditorFragment<JTRegConfiguration, LabeledComponent<ClasspathModifier.ClasspathComponent>> {
    constructor(configuration: JTRegConfiguration) :
        super(
            "Test Classpath Modifier", "Modify test classpath", "Tests",
            LabeledComponent.create<ClasspathComponent?>(ClasspathComponent(configuration), "Modify test classpath"
            ),
            BiConsumer { options: JTRegConfiguration, component: LabeledComponent<ClasspathComponent> ->
                component.getComponent().myModel.setItems(
                    ArrayList<ClasspathModification?>(options!!.getTestClasspath())
                )
            },
            BiConsumer { options: JTRegConfiguration, component: LabeledComponent<ClasspathComponent> ->
                options.setTestClasspath(
                    component.getComponent().myModel.getItems()
                )
            },
            Predicate { options: JTRegConfiguration? -> !options!!.getTestClasspath().isEmpty() })



    class ClasspathComponent(configuration: JTRegConfiguration) : JPanel(BorderLayout()) {
        val myModel: ListTableModel<ClasspathModification>

        init {
            setBorder(JBUI.Borders.emptyTop(5))
            myModel =
                ListTableModel<ClasspathModification>(object : ColumnInfo<ClasspathModification?, String?>(null) {
                    override fun valueOf(item: ClasspathModification?): String? {
                        item?.let {
                            if (it.exclude) return "Exclude"
                        }
                        return "Include"
                    }
                }, object : ColumnInfo<ClasspathModification?, String?>(null) {
                    override fun valueOf(modification: ClasspathModification?): String? {
                        return modification?.path
                    }
                })

            val table = JBTable(myModel)
            table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN)
            val model = table.getTableHeader().getColumnModel()
            model.getColumn(0).setMinWidth(100)
            model.getColumn(1).setPreferredWidth(Int.Companion.MAX_VALUE)
            val decorator = ToolbarDecorator.createDecorator(table)
            val group = DefaultActionGroup(object : DumbAwareAction(ExecutionBundle.message("action.include.text")) {
                override fun actionPerformed(e: AnActionEvent) {
                    val project = configuration.project
                    val descriptor = FileChooserDescriptor(true, true, true, false, true, false)
                    val files =
                        FileChooser.chooseFiles(descriptor, this@ClasspathComponent, project, project.getBaseDir())
                    myModel.addRows(
                        ContainerUtil.map(
                            files,
                            Function { file: VirtualFile? ->
                                ClasspathModification(
                                    PathUtil.getLocalPath(file!!.getPath()), false
                                )
                            })
                    )
                }
            }, object : DumbAwareAction(ExecutionBundle.message("action.exclude.text")) {
                override fun actionPerformed(e: AnActionEvent) {
                    val parameters = JavaParameters()
                    val module = configuration.configurationModule.module
                    try {
                        if (module != null) {
                            parameters.configureByModule(module, JavaParameters.CLASSES_AND_TESTS)
                        } else {
                            parameters.configureByProject(
                                configuration.project,
                                JavaParameters.CLASSES_AND_TESTS,
                                null
                            )
                        }
                    } catch (ignore: CantRunException) {
                    }
                    val actionGroup = DefaultActionGroup(
                        ContainerUtil.map(
                            parameters.classPath.getPathList(),
                            Function { entry: String? ->
                                object : DumbAwareAction(entry) {
                                    //NON-NLS
                                    override fun actionPerformed(e: AnActionEvent) {
                                        myModel.addRow(ClasspathModification(entry!!, true))
                                    }
                                }
                            })
                    )
                    JBPopupFactory.getInstance().createActionGroupPopup(
                        null,
                        actionGroup,
                        e.dataContext,
                        JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                        true
                    ).showInBestPositionFor(e.dataContext)
                }
            })
            decorator.setAddAction { button: AnActionButton? ->
                JBPopupFactory.getInstance()
                    .createActionGroupPopup(
                        null, group, DataManager.getInstance().getDataContext(this),
                        JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, false
                    ).show(button!!.getPreferredPopupPoint())
            }
            add(decorator.createPanel())
        }
    }

}