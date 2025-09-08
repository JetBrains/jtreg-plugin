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

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.observable.properties.AtomicProperty
import com.intellij.openapi.options.BaseConfigurable
import com.intellij.openapi.ui.TextComponentAccessor
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.SystemInfo
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.JBColor
import com.intellij.ui.TextFieldWithHistory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.*
import com.jetbrains.plugin.jtreg.BuildConfig
import com.jetbrains.plugin.jtreg.configuration.IgnoreMode
import com.jetbrains.plugin.jtreg.configuration.IgnoreModeComboBoxModel
import com.jetbrains.plugin.jtreg.configuration.TestMode
import com.jetbrains.plugin.jtreg.configuration.TestModeComboBoxModel
import com.jetbrains.plugin.jtreg.service.JTRegService
import com.jetbrains.plugin.jtreg.util.JTRegUtils
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.event.DocumentEvent

class JTRegServiceConfigurable: BaseConfigurable() {

    private val jtRegService = ApplicationManager.getApplication().getService(JTRegService::class.java)

    private val jtregHomeDirectory = AtomicProperty(jtRegService.jtregHomeDirectory)
    private val verboseOutput = AtomicProperty(jtRegService.verboseOutput)
    private val allowSecurityManager = AtomicProperty(jtRegService.allowSecurityManager)
    private val concurrency = AtomicProperty(jtRegService.concurrency)
    private val ignore = AtomicProperty(jtRegService.ignore)
    private val timeoutFactor = AtomicProperty(jtRegService.timeoutFactor)
    private val timeLimit = AtomicProperty(jtRegService.timeLimit)
    private val xmlReport = AtomicProperty(jtRegService.xmlReport)

    private val testMode = AtomicProperty(jtRegService.testMode)

    private val wakefieldPath = AtomicProperty(jtRegService.westonSettings.wakefieldPath)
    private val westonScreensCount = AtomicProperty(jtRegService.westonSettings.screensCount)
    private val westonScreenWidth = AtomicProperty(jtRegService.westonSettings.screenWidth)
    private val westonScreenHeight = AtomicProperty(jtRegService.westonSettings.screenHeight)

    private var timeoutFactorStringValue = jtRegService.timeoutFactor.toString()
    private val timeoutFactorProperty = AtomicProperty(timeoutFactorStringValue)

    private val jtregDirField = TextFieldWithHistory().apply {
        text = jtregHomeDirectory.get()
    }

    private var hasError = false
    private val timeoutFactorErrorLabel = JBLabel().apply {
        foreground = JBColor.RED
    }

    override fun createComponent(): JComponent {
        return panel {
            group("JTReg Location") {
                val jtregHomeTextField = TextFieldWithBrowseButton(jtregDirField)
                jtregHomeTextField.addBrowseFolderListener("Choose JTReg Home Directory", null, null,
                    FileChooserDescriptorFactory.createSingleFolderDescriptor(),
                    TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT)

                val jtregHomeMessageLabel = JBLabel("")

                jtregHomeTextField.textField.document.addDocumentListener(object : DocumentAdapter() {
                    override fun textChanged(e: DocumentEvent) {
                        val path = jtregHomeTextField.text
                        val jtregInfo = JTRegUtils.getJTRegInfo(path)
                        if (!jtregInfo.isValid) {
                            jtregHomeMessageLabel.text = jtregInfo.error
                            jtregHomeMessageLabel.foreground = JBColor.RED
                        } else {
                            val additionalFeatures = jtregInfo.additionalFeatures.map {
                                val os = if (it.value != "all") "${it.value} only" else "any OS"
                                return@map "${it.key}: $os"
                            }.joinToString("<br>")
                            val features = if (additionalFeatures.isNotEmpty()) "Additional features:<br>$additionalFeatures" else ""
                            jtregHomeMessageLabel.text = """
                                <html>
                                JTReg version: ${jtregInfo.version}
                                <br>
                                $features
                                </html>
                            """.trimIndent()
                            jtregHomeMessageLabel.foreground = JBColor.foreground()
                        }
                    }
                })

                row("JTReg Home Directory") {
                    cell(jtregHomeTextField)
                        .bindText(jtregHomeDirectory)
                        .align(AlignX.FILL)
                }
                row("") {
                    cell(jtregHomeMessageLabel)
                        .align(AlignX.FILL)
                }
            }
            group("Verbose Options") {
                row("Verbose Output") {
                    checkBox("Verbose output")
                        .bindSelected(verboseOutput)
                        .align(AlignX.FILL)
                }
            }
            group("General Options") {
                row("Allow Security Manager") {
                    checkBox("Allow agentVM tests to set a security manager")
                        .bindSelected(allowSecurityManager)
                        .align(AlignX.FILL)
                }
                row("Concurrency factor") {
                    textField()
                        .bindIntText(concurrency)
                        .align(AlignX.FILL)
                }
                row("Ignore Mode") {
                    val ignoreComboBox = JComboBox(IgnoreModeComboBoxModel())
                    ignoreComboBox.addItemListener { event ->
                        val selectedDescription = event.item as String
                        val selectedIgnoreMode = IgnoreMode.entries.first { it.description == selectedDescription }
                        ignore.set(selectedIgnoreMode)
                    }
                    ignoreComboBox.selectedIndex = IgnoreMode.entries.indexOf(ignore.get())
                    cell(ignoreComboBox)
                        .align(AlignX.FILL)
                }
                row("Timeout Factor") {
                    val timeoutFactorTextField = textField()
                        .bindText(timeoutFactorProperty)
                        .align(AlignX.FILL)

                    timeoutFactorTextField.component.document.addDocumentListener(object : DocumentAdapter() {
                        override fun textChanged(e: DocumentEvent) {
                            val text = timeoutFactorTextField.component.text
                            text.toFloatOrNull()?.let { factor ->
                                if (factor <= 0f) {
                                    timeoutFactorErrorLabel.text = "Timeout factor must be a positive value"
                                } else {
                                    timeoutFactorErrorLabel.text = ""
                                    timeoutFactor.set(factor)
                                }
                            } ?: run {
                                timeoutFactorErrorLabel.text = "Timeout factor must be a number"
                                hasError = true
                            }
                        }
                    })
                }
                row {
                    cell(timeoutFactorErrorLabel)
                        .align(AlignX.FILL)
                }
                row("Time Limit (seconds)") {
                    textField()
                        .bindIntText(timeLimit)
                        .align(AlignX.FILL)
                }
                row("XML Report") {
                    checkBox("Create ant/junit xml files into the workDir")
                        .bindSelected(xmlReport)
                        .align(AlignX.FILL)
                }
            }
            group("Test Mode Options") {
                row("Test Mode") {
                    val testModeComboBox = JComboBox(TestModeComboBoxModel())
                    testModeComboBox.addItemListener { event ->
                        val selectedDescription = event.item as String
                        val selectedTestMode = TestMode.entries.first { it.description == selectedDescription }
                        testMode.set(selectedTestMode)
                    }
                    testModeComboBox.selectedIndex = TestMode.entries.indexOf(testMode.get())
                    cell(testModeComboBox)
                        .align(AlignX.FILL)
                }
            }
            if (BuildConfig.isJetBrainsVendor() && SystemInfo.isLinux) {
                group("Weston Settings (JBR JTReg Only)") {
                    row("Wakefield library path") {
                        val wakefieldPathField = TextFieldWithBrowseButton(jtregDirField)
                        wakefieldPathField.addBrowseFolderListener("Choose Wakefield Library", null, null,
                            FileChooserDescriptorFactory.createSingleFileDescriptor(),
                            TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT)

                        cell(wakefieldPathField)
                            .bindText(wakefieldPath)
                            .align(AlignX.FILL)
                    }
                    row("Weston Screens Count") {
                        textField()
                            .bindIntText(westonScreensCount)
                            .align(AlignX.FILL)
                    }
                    row("Weston Screen Width") {
                        textField()
                            .bindIntText(westonScreenWidth)
                            .align(AlignX.FILL)
                    }
                    row("Weston Screen Height") {
                        textField()
                            .bindIntText(westonScreenHeight)
                            .align(AlignX.FILL)
                    }
                }
            }
        }
    }

    override fun isModified(): Boolean {
        if (jtregHomeDirectory.get() != jtRegService.jtregHomeDirectory) return true
        if (verboseOutput.get() != jtRegService.verboseOutput) return true
        if (allowSecurityManager.get() != jtRegService.allowSecurityManager) return true
        if (concurrency.get() != jtRegService.concurrency) return true
        if (ignore.get() != jtRegService.ignore) return true
        if (timeoutFactor.get() != jtRegService.timeoutFactor) return true
        if (timeLimit.get() != jtRegService.timeLimit) return true
        if (xmlReport.get() != jtRegService.xmlReport) return true
        if (testMode.get() != jtRegService.testMode) return true
        if (wakefieldPath.get() != jtRegService.westonSettings.wakefieldPath) return true
        if (westonScreensCount.get() != jtRegService.westonSettings.screensCount) return true
        if (westonScreenWidth.get() != jtRegService.westonSettings.screenWidth) return true
        if (westonScreenHeight.get() != jtRegService.westonSettings.screenHeight) return true
        return false
    }

    override fun apply() {
        jtRegService.jtregHomeDirectory = jtregHomeDirectory.get()
        jtRegService.verboseOutput = verboseOutput.get()
        jtRegService.allowSecurityManager = allowSecurityManager.get()
        jtRegService.concurrency = concurrency.get()
        jtRegService.ignore = ignore.get()
        jtRegService.timeoutFactor = timeoutFactor.get()
        jtRegService.timeLimit = timeLimit.get()
        jtRegService.xmlReport = xmlReport.get()
        jtRegService.testMode = testMode.get()
        jtRegService.westonSettings.screensCount = westonScreensCount.get()
        jtRegService.westonSettings.screenWidth = westonScreenWidth.get()
        jtRegService.westonSettings.screenHeight = westonScreenHeight.get()
        jtRegService.westonSettings.wakefieldPath = wakefieldPath.get()
    }

    override fun reset() {
        jtregHomeDirectory.set(jtRegService.jtregHomeDirectory)
        verboseOutput.set(jtRegService.verboseOutput)
        allowSecurityManager.set(jtRegService.allowSecurityManager)
        concurrency.set(jtRegService.concurrency)
        ignore.set(jtRegService.ignore)
        timeoutFactor.set(jtRegService.timeoutFactor)
        timeLimit.set(jtRegService.timeLimit)
        xmlReport.set(jtRegService.xmlReport)
        testMode.set(jtRegService.testMode)
        timeoutFactorProperty.set(timeoutFactor.get().toString())
        hasError = false
        timeoutFactorErrorLabel.text = ""
        wakefieldPath.set(jtRegService.westonSettings.wakefieldPath)
        westonScreenWidth.set(jtRegService.westonSettings.screenWidth)
        westonScreenHeight.set(jtRegService.westonSettings.screenHeight)
        westonScreensCount.set(jtRegService.westonSettings.screensCount)
    }

    override fun getDisplayName(): String {
        return "JTReg Settings"
    }

}
