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

import com.intellij.execution.configurations.ParametersList
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.runners.ProgramRunner
import com.intellij.util.PathUtil
import com.intellij.util.PathsList
import com.jetbrains.plugin.jtreg.BuildConfig
import com.jetbrains.plugin.jtreg.configuration.model.TestData
import java.io.File
import java.nio.file.Paths

object TestObjectHelper {
    
    fun createJTRegProgramParameters(configuration: JTRegConfiguration, classPath: PathsList, runner: ProgramRunner<out RunnerSettings>): ParametersList {
        val params = ParametersList()

        // set verbose options
        if (configuration.isVerboseOutput()) {
            params.add("-v")
        }

        // set general options
        if (configuration.isAllowSecurityManager()) {
            params.add("-allowSetSecurityManager")
        }
        if (runner !is JTRegDebuggerRunner) {
            if (configuration.getConcurrency() > 1) {
                params.add("-concurrency:${configuration.getConcurrency()}")
                if (configuration.getLockFile().isNotEmpty()) {
                    params.add("-lock:${configuration.getLockFile()}")
                }
            }
        }

        params.add("-ignore:${configuration.getIgnoreMode().name.lowercase()}")
        params.add("-timeoutFactor:${configuration.getTimeoutFactor()}")
        params.add("-timelimit:${configuration.getTimeLimit()}")
        if (configuration.isGenerateXmlReport()) {
            params.add("-xml")
        }

        // repeat mode
        if (runner !is JTRegDebuggerRunner) {
            if (BuildConfig.isJetBrainsVendor()) {
                if (configuration.getRepeatMode() != RepeatCount.ONCE) {
                    RepeatCount.cmdValues[configuration.getRepeatMode()]?.let {
                        params.add("-repeat:${it}")
                    }
                }
                if (configuration.getRepeatMode() == RepeatCount.N) {
                    params.add("-repeatCount:${configuration.getRepeatCount()}")
                } else if (configuration.getRepeatMode() == RepeatCount.UNTIL_FAILURE || configuration.getRepeatMode() == RepeatCount.UNTIL_SUCCESS) {
                    params.add("-repeatCount:${configuration.getMaxRepeatCount()}")
                }
            }
        }

        // weston mode
        if (BuildConfig.isJetBrainsVendor()) {
            if (configuration.isUseWeston()) {
                params.add("-useWeston")
                params.add("-libwakefield:${configuration.getWakeFieldPath()}")
                params.add("-westonScreensCount:${configuration.getWestonScreens()}")
                params.add("-westonScreenWidth:${configuration.getWestonScreenWidth()}")
                params.add("-westonScreenHeight:${configuration.getWestonScreenHeight()}")
            }
        }

        // base dir
        params.add("-dir:${configuration.project.basePath}")
        // work dir
        if (configuration.workingDirectory.isEmpty()) {
            configuration.project.basePath?.let {
                val baseDir = Paths.get(it)
                val workDir = baseDir.resolve("JTWork").toString()
                configuration.workingDirectory = workDir
            }
        }
        params.add("-workDir:${configuration.workingDirectory}")
        // report dir
        if (configuration.getReportDir().isEmpty()) {
            Paths.get(configuration.workingDirectory).parent.resolve("JTReport").toString().let {
                configuration.setReportDir(it)
            }
        }
        params.add("-reportDir:${configuration.getReportDir()}")

        // additional test options
        // keywords
        if (configuration.getKeyword().isNotEmpty()) {
            params.add("-k:${configuration.getKeyword()}")
        }

        if (configuration.getExcludeList().isNotEmpty()) {
            params.add("-exclude:${configuration.getExcludeList()}")
        }

        // test mode
        if (runner is JTRegDebuggerRunner) { // debug mode can run only in agent vm
            params.add("-agentvm")
        } else {
            params.add("-${configuration.getTestMode().cmdOption}")
        }

        // test category
        if (configuration.getTestCategory() == TestData.ONLY_MANUAL) {
            params.add("-m")
        } else if (configuration.getTestCategory() == TestData.ONLY_AUTOMATIC) {
            params.add("-a")
        }

        // attach debug agent
        if (runner is JTRegDebuggerRunner) {
            val debugRunner: JTRegDebuggerRunner = runner
            params.add("-debug:-agentlib:jdwp=transport=dt_socket,server=n,suspend=y,address=127.0.0.1:" + debugRunner.getAddress())
        }

        if (configuration.getRunCmd().isNotEmpty()) {
            val cmd = configuration.getRunCmd().split(" ").map { it.trim() }.filter { it !== "" }.joinToString(" ")
            params.add("-singleCmd:\"${cmd}\"")
        }

        // set jdk
        val jdkString = configuration.getJDKString()
        params.add("-jdk:$jdkString")

        // set VM options
        if (configuration.getNativeLibraryPath().isNotEmpty()) {
            params.add("-nativepath:${configuration.getNativeLibraryPath()}")
        }

        if (configuration.getTestJavaOptions().trim().isNotEmpty()) {
            params.add("-javaoptions:${configuration.getTestJavaOptions()}")
        }

        if (configuration.getTestEnvVars().isNotEmpty()) {
            configuration.getTestEnvVars().map { "${it.key}=${it.value}" }.joinToString(",").let {
                params.add("-e:$it")
            }
        }

        if (classPath.pathList.isNotEmpty()) {
            params.add("-cpa:${buildClasspath(classPath.pathList)}")
        }

        // add test observer
        params.add("-o:com.oracle.plugin.jtreg.runtime.TestListener")
        params.add("-od:" + PathUtil.getJarPathForClass(JTRegConfiguration::class.java))
        if (configuration.getTestKind() == TestData.TEST_CLASS) {
            params.add(configuration.getRunClass())
        } else if (configuration.getTestKind() == TestData.TEST_DIRECTORY) {
            params.add(configuration.getPackage())
        } else if (configuration.getTestKind() == TestData.TEST_GROUP) {
            configuration.getTestGroup()?.let {
                val testSelection = "${it.relativeTestDirectory}:${it.groupName}"
                params.add(testSelection)
            }
        }

        if (configuration.programParameters.isNotEmpty()) {
            configuration.programParameters.split(" ").map { it.trim() }.filter { it.isNotEmpty() }
                .forEach { params.add(it) }
        }

        return params
    }

    private fun buildClasspath(paths: List<String>): String {
        val pathSeparator = File.pathSeparator
        return paths.joinToString(pathSeparator) { path -> path.replace("\"", "\\\"") }
    }

}