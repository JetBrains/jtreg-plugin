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

import com.intellij.execution.configuration.EnvironmentVariablesComponent
import com.jetbrains.plugin.jtreg.configuration.model.TestData
import com.jetbrains.plugin.jtreg.service.JTRegService
import org.jdom.Element

object ConfigurationHelper {

    fun prepareTestData(data: TestData): Element {
        val jtreg = Element("jtreg")
        if (data.vmSettings.envVars.isNotEmpty()) {
            EnvironmentVariablesComponent.writeExternal(jtreg, data.vmSettings.envVars)
        }
        jtreg.setAttribute("testMode", data.testMode.toString())
        jtreg.setAttribute("testCategory", data.testCategory)
        jtreg.setAttribute("reportDir", data.reportDir)
        jtreg.setAttribute("excludeList", data.excludeList)
        jtreg.setAttribute("concurrency", data.concurrency.toString())
        jtreg.setAttribute("timeoutFactor", data.timeoutFactor.toString())
        jtreg.setAttribute("timeLimit", data.timeLimit.toString())
        jtreg.setAttribute("lockFile", data.lock)
        jtreg.setAttribute("ignoreMode", data.ignoreMode.toString())
        jtreg.setAttribute("keyword", data.keyword)


        val weston = Element("weston")
        weston.setAttribute("useWeston", data.weston.useWeston.toString())
        weston.setAttribute("westonScreens", data.weston.screensCount.toString())
        weston.setAttribute("westonScreenWidth", data.weston.screenWidth.toString())
        weston.setAttribute("westonScreenHeight", data.weston.screenHeight.toString())
        weston.setAttribute("wakeFieldPath", data.weston.wakefieldPath)
        jtreg.addContent(weston)

        val testVM = Element("testVM")
        testVM.setAttribute("allowSecurityManager", data.vmSettings.allowSecurityManager.toString())
        testVM.setAttribute("testJavaOptions", data.vmSettings.javaOptions)
        testVM.setAttribute("nativeTestLibraryPath", data.vmSettings.nativeLibPath)
        jtreg.addContent(testVM)

        val repeat = Element("repeat")
        repeat.setAttribute("repeatMode", data.repeat.mode)
        repeat.setAttribute("repeatCount", data.repeat.count.toString())
        repeat.setAttribute("maxRepeatCount", data.repeat.maxCount.toString())
        jtreg.addContent(repeat)

        return jtreg
    }

    fun readTestData(element: Element, settings: JTRegService): TestData {
        val data = TestData()
        element.getChild("jtreg")?.let { jtreg ->
            val envVars: MutableMap<String?, String?> = mutableMapOf()
            EnvironmentVariablesComponent.readExternal(jtreg, envVars)
            data.envVars = envVars

            data.testMode = getTestMode(jtreg, settings.testMode)
            data.testCategory = getTestCategory(jtreg)
            data.reportDir = jtreg.getAttributeValue("reportDir") ?: ""
            data.excludeList = jtreg.getAttributeValue("excludeList") ?: ""
            data.concurrency = jtreg.getAttributeValue("concurrency")?.toIntOrNull() ?: settings.concurrency
            data.timeoutFactor = jtreg.getAttributeValue("timeoutFactor")?.toFloatOrNull() ?: settings.timeoutFactor
            data.timeLimit = jtreg.getAttributeValue("timeLimit")?.toIntOrNull() ?: settings.timeLimit
            data.lock = jtreg.getAttributeValue("lockFile") ?: ""
            data.ignoreMode = IgnoreMode.valueOf(jtreg.getAttributeValue("ignoreMode") ?: settings.ignore.toString())
            data.keyword = jtreg.getAttributeValue("keyword") ?: ""

            jtreg.getChild("weston")?.let { weston ->
                data.weston.useWeston = weston.getAttributeValue("useWeston")?.toBoolean() == true
                data.weston.screensCount = weston.getAttributeValue("westonScreens")?.toIntOrNull() ?: settings.westonSettings.screensCount
                data.weston.screenWidth = weston.getAttributeValue("westonScreenWidth")?.toIntOrNull() ?: settings.westonSettings.screenWidth
                data.weston.screenHeight = weston.getAttributeValue("westonScreenHeight")?.toIntOrNull() ?: settings.westonSettings.screenHeight
                data.weston.wakefieldPath = weston.getAttributeValue("wakeFieldPath") ?: settings.westonSettings.wakefieldPath
            }
            jtreg.getChild("testVM")?.let { testVM ->
                data.vmSettings.allowSecurityManager = testVM.getAttributeValue("allowSecurityManager")?.toBoolean() ?: false
                data.vmSettings.javaOptions = testVM.getAttributeValue("testJavaOptions") ?: ""
                val testEnvVars: MutableMap<String?, String?> = mutableMapOf()
                EnvironmentVariablesComponent.readExternal(testVM, testEnvVars)
                data.vmSettings.envVars = testEnvVars
                data.vmSettings.nativeLibPath = testVM.getAttributeValue("nativeTestLibraryPath") ?: ""
            }
            jtreg.getChild("repeat")?.let { repeat ->
                data.repeat.mode = repeat.getAttributeValue("repeatMode") ?: RepeatCount.ONCE
                data.repeat.count = repeat.getAttributeValue("repeatCount")?.toIntOrNull() ?: 1
                data.repeat.maxCount = repeat.getAttributeValue("maxRepeatCount")?.toIntOrNull() ?: 100
            }
        }
        return data
    }

    private fun getTestMode(element: Element, defaultTestMode: TestMode): TestMode {
        val testModeStr = element.getAttributeValue("testMode") ?: return defaultTestMode
        try {
            val mode = TestMode.valueOf(testModeStr)
            return mode
        } catch (ex: IllegalArgumentException) {
            if (testModeStr == TestMode.AGENT_VM.cmdOption) return TestMode.AGENT_VM
            return TestMode.OTHER_VM
        }
    }

    private fun getTestCategory(element: Element): String {
        element.getAttributeValue("testCategory")?.let {
            return it
        } ?: run {
            return TestData.ONLY_AUTOMATIC
        }
    }

}