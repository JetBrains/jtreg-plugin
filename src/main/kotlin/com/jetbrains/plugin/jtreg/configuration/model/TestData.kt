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
package com.jetbrains.plugin.jtreg.configuration.model

import com.intellij.execution.testframework.TestSearchScope
import com.intellij.openapi.util.Comparing
import com.jetbrains.plugin.jtreg.configuration.IgnoreMode
import com.jetbrains.plugin.jtreg.configuration.TestMode
import com.jetbrains.plugin.jtreg.util.TestGroup
import java.util.*


data class TestData(
    var envVars: Map<String?, String?> = mapOf(),
    var parameters: String = "",
    var vmParameters: String = "",
    var workingDirectory: String = "",
    var passParentEnvs: Boolean = true,
    var testSearchScope: TestSearchScope.Wrapper = TestSearchScope.Wrapper(), // why?

    var testKind: String = TEST_CLASS,
    var packageName: String = "",
    var className: String = "",
    var testGroup: TestGroup? = null,

    var testMode: TestMode = TestMode.OTHER_VM,
    var testCategory: String = ONLY_AUTOMATIC,

    var reportDir: String = "",
    var excludeList: String = "",
    var concurrency: Int = 1,
    var ignoreMode: IgnoreMode = IgnoreMode.ERROR,
    var lock: String = "",
    var timeoutFactor: Float = 1.0f,
    var timeLimit: Int = 3600,
    var xmlReport: Boolean = false,
    var keyword: String = "",

    var weston: WestonSettings = WestonSettings(),
    var repeat: RepeatSettings = RepeatSettings(),
    var vmSettings: TestVMSettings = TestVMSettings(),

    var verboseOutput: Boolean = false,
): Cloneable {

    override fun equals(other: Any?): Boolean {
        if (other !is TestData) return false
        return Objects.equals(envVars, other.envVars) &&
                Objects.equals(parameters, other.parameters) &&
                Objects.equals(vmParameters, other.vmParameters) &&
                Objects.equals(workingDirectory, other.workingDirectory) &&
                Objects.equals(passParentEnvs, other.passParentEnvs) &&
                Objects.equals(testSearchScope, other.testSearchScope) &&
                Objects.equals(testKind, other.testKind) &&
                Objects.equals(packageName, other.packageName) &&
                Objects.equals(className, other.className) &&
                Objects.equals(testGroup, other.testGroup) &&
                Objects.equals(testMode, other.testMode) &&
                Objects.equals(testCategory, other.testCategory) &&
                Objects.equals(reportDir, other.reportDir) &&
                Objects.equals(excludeList, other.excludeList) &&
                Objects.equals(concurrency, other.concurrency) &&
                Objects.equals(ignoreMode, other.ignoreMode) &&
                Objects.equals(lock, other.lock) &&
                Objects.equals(timeoutFactor, other.timeoutFactor) &&
                Objects.equals(timeLimit, other.timeLimit) &&
                Objects.equals(xmlReport, other.xmlReport) &&
                Objects.equals(keyword, other.keyword) &&
                Objects.equals(weston, other.weston) &&
                Objects.equals(repeat, other.repeat) &&
                Objects.equals(vmSettings, other.vmSettings) &&
                Objects.equals(verboseOutput, other.verboseOutput)
    }

    override fun hashCode(): Int {
        return Comparing.hashcode(envVars) xor
                Comparing.hashcode(parameters) xor
                Comparing.hashcode(vmParameters) xor
                Comparing.hashcode(workingDirectory) xor
                Comparing.hashcode(passParentEnvs) xor
                Comparing.hashcode(testSearchScope) xor
                Comparing.hashcode(testKind) xor
                Comparing.hashcode(packageName) xor
                Comparing.hashcode(className) xor
                Comparing.hashcode(testGroup) xor
                Comparing.hashcode(testMode) xor
                Comparing.hashcode(testCategory) xor
                Comparing.hashcode(reportDir) xor
                Comparing.hashcode(excludeList) xor
                Comparing.hashcode(concurrency) xor
                Comparing.hashcode(ignoreMode) xor
                Comparing.hashcode(lock) xor
                Comparing.hashcode(timeoutFactor) xor
                Comparing.hashcode(timeLimit) xor
                Comparing.hashcode(xmlReport) xor
                Comparing.hashcode(keyword) xor
                Comparing.hashcode(weston) xor
                Comparing.hashcode(vmSettings) xor
                Comparing.hashcode(verboseOutput) xor
                Comparing.hashcode(repeat)
    }

    public override fun clone(): TestData {
        try {
            val data = super.clone() as TestData
            data.envVars = envVars
            return data
        } catch (e: CloneNotSupportedException) {
            throw RuntimeException(e)
        }
    }

    companion object {

        const val TEST_CLASS = "class"
        const val TEST_DIRECTORY = "directory"
        const val TEST_GROUP = "group"

        val TEST_KINDS = arrayOf(TEST_CLASS, TEST_DIRECTORY, TEST_GROUP)

        const val ONLY_MANUAL = "manual"
        const val ONLY_AUTOMATIC = "automatic"
        const val ALL_TESTS = "all"

        val TEST_TYPES = arrayOf(ONLY_MANUAL, ONLY_AUTOMATIC, ALL_TESTS)

    }

}