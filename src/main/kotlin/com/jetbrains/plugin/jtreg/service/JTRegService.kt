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
package com.jetbrains.plugin.jtreg.service

import com.intellij.openapi.components.*
import com.jetbrains.plugin.jtreg.configuration.IgnoreMode
import com.jetbrains.plugin.jtreg.configuration.TestMode
import com.jetbrains.plugin.jtreg.configuration.model.WestonSettings
import com.jetbrains.plugin.jtreg.ui.JTRegConfig
import com.jetbrains.plugin.jtreg.util.JTRegInfo
import com.jetbrains.plugin.jtreg.util.JTRegUtils

@State(name = "JTRegService", storages = [(Storage(value = "jtreg.xml"))], category = SettingsCategory.TOOLS)
class JTRegService : PersistentStateComponent<JTRegConfig> {

    var jtregHomeDirectory = ""
    var jtregInfo: JTRegInfo? = null

    var verboseOutput: Boolean = true
    var allowSecurityManager = false
    var concurrency = 1
    var ignore = IgnoreMode.ERROR
    var timeoutFactor: Float = 1.0f
    var timeLimit = 3600
    var xmlReport = false

    var testMode = TestMode.OTHER_VM

    val westonSettings = WestonSettings()

    override fun getState(): JTRegConfig {
        jtregInfo = JTRegUtils.getJTRegInfo(jtregHomeDirectory)
        return JTRegConfig(
            jtregDir = jtregHomeDirectory,
            verboseOutput = verboseOutput,
            allowSecurityManager = allowSecurityManager,
            concurrency = concurrency,
            ignore = ignore,
            timeoutFactor = timeoutFactor,
            timeLimit = timeLimit,
            xmlReport = xmlReport,
            vmTestMode = testMode,
            wakeFieldPath = westonSettings.wakefieldPath,
            westonDefaultScreenWidth = westonSettings.screenWidth,
            westonDefaultScreenHeight = westonSettings.screenHeight,
            westonScreensCount = westonSettings.screensCount,
        )
    }

    override fun loadState(state: JTRegConfig) {
        jtregHomeDirectory = state.jtregDir
        verboseOutput = state.verboseOutput
        allowSecurityManager = state.allowSecurityManager
        concurrency = state.concurrency
        ignore = state.ignore
        timeoutFactor = state.timeoutFactor
        timeLimit = state.timeLimit
        xmlReport = state.xmlReport
        testMode = state.vmTestMode
        westonSettings.screenWidth = state.westonDefaultScreenWidth
        westonSettings.screenHeight = state.westonDefaultScreenHeight
        westonSettings.screensCount = state.westonScreensCount
        westonSettings.wakefieldPath = state.wakeFieldPath
        jtregInfo = JTRegUtils.getJTRegInfo(jtregHomeDirectory)
    }

}