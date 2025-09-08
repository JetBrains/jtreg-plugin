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

import com.intellij.execution.JavaTestFrameworkRunnableState
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.ParametersList
import com.intellij.execution.configurations.RuntimeConfigurationException
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.target.TargetEnvironment
import com.intellij.execution.testframework.SearchForTestsTask
import com.intellij.execution.testframework.TestSearchScope
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.jetbrains.plugin.jtreg.JTRegBundle
import com.jetbrains.plugin.jtreg.service.JTRegService
import com.jetbrains.plugin.jtreg.ui.IncorrectJtregHomeException
import com.jetbrains.plugin.jtreg.ui.Watchdog
import com.jetbrains.plugin.jtreg.util.JTRegUtils
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists

class JTRegTestObject(private val configuration: JTRegConfiguration, executionEnvironment: ExecutionEnvironment): JavaTestFrameworkRunnableState<JTRegConfiguration>(executionEnvironment) {


    override fun getFrameworkName() = "JBR JTReg"

    override fun getFrameworkId() = "jtreg"

    override fun passTempFile(parametersList: ParametersList, tempFilePath: String) {
    }

    override fun getConfiguration(): JTRegConfiguration {
        return configuration
    }

    override fun getScope() = TestSearchScope.SINGLE_MODULE

    override fun getForkMode() = "none"

    override fun configureRTClasspath(javaParameters: JavaParameters, module: Module) {
        val settings: JTRegService = ApplicationManager.getApplication().getService(JTRegService::class.java)

        try {
            val jtregLibDir = Path.of(settings.jtregHomeDirectory, "lib")
            val libs = Files.newDirectoryStream(jtregLibDir, "*.jar")
            libs.forEach { lib -> javaParameters.classPath.add(lib.toString()) }
        } catch (ex: Exception) {
            Watchdog.processIncorrectJtregHome()
            throw IncorrectJtregHomeException(JTRegBundle.message("jtreg.configuration.error.not_found"))
        }
    }

    override fun passForkMode(forkMode: String, tempFile: File, parameters: JavaParameters) {
    }

    private fun validateJTReg() {
        val settings: JTRegService = ApplicationManager.getApplication().getService(JTRegService::class.java)
        val jtregInfo = JTRegUtils.getJTRegInfo(settings.jtregHomeDirectory)

        if (!jtregInfo.isValid) {
            throw RuntimeConfigurationException(jtregInfo.error)
        }

        if (configuration.isUseWeston() && !jtregInfo.isWestonSupported()) {
            throw RuntimeConfigurationException(JTRegBundle.message("jtreg.configuration.error.weston_not_supported"))
        }

        if (configuration.getRepeatMode() != RepeatCount.ONCE && !jtregInfo.isTestRepeatSupported()) {
            throw RuntimeConfigurationException(JTRegBundle.message("jtreg.configuration.error.test_repeat_not_supported"))
        }

        val workDir = Paths.get(configuration.workingDirectory)
        if (!workDir.exists()) {
            Files.createDirectories(workDir)
        }
    }

    public override fun createJavaParameters(): JavaParameters? {
        configuration.checkConfiguration()
        validateJTReg()

        val javaParameters = super.createJavaParameters()

        javaParameters.mainClass = "com.sun.javatest.regtest.Main"

        configuration.envs.forEach {
            javaParameters.addEnv(it.key, it.value)
        }

        javaParameters.programParametersList.clearAll()
        val jtregParams = TestObjectHelper.createJTRegProgramParameters(configuration, javaParameters.classPath, environment.runner)
        jtregParams.list.forEach {
            javaParameters.programParametersList.add(it)
        }

        return javaParameters
    }

    override fun createSearchingForTestsTask(targetEnvironment: TargetEnvironment): SearchForTestsTask? {
        return null
    }

}