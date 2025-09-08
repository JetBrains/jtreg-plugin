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

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.impl.JavaSdkImpl
import com.intellij.util.PathUtil
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.readLines
import kotlin.text.split
import kotlin.text.startsWith

object SdkUtils {

    fun getNativeSupportLibs(jdkPath: String?): String? {
        jdkPath?: return null
        val nativeLibPath = Paths.get(jdkPath).parent.resolve("support/test/jdk/jtreg/native")
        if (nativeLibPath.exists()) {
            return nativeLibPath.toString()
        }
        return null
    }

    fun detectLocalJdkBuilds(project: Project) {
        val buildDir = getBuildDir(project) ?: return
        if (!buildDir.exists()) return

        val topDirs = Files.walk(buildDir, 1)
            .filter { it.isDirectory() }
            .map { it.resolve("jdk") }
            .filter { it.exists() }
            .toList()

        topDirs.forEach {
            val releaseFilePath = it.resolve("release")
            if (!releaseFilePath.exists()) return@forEach
            if (!it.resolve("bin/java").exists()) return@forEach
            if (!it.resolve("bin/javac").exists()) return@forEach
            val name = "Local build ${it.parent.fileName}"
            val version = getVersion(releaseFilePath, it.parent.fileName.toString())

            val homePath = it.toString()
            val existingJdk = ProjectJdkTable.getInstance().allJdks.firstOrNull { sdk -> sdk.homePath == homePath }
            existingJdk?.let {
                if (existingJdk.versionString == version && existingJdk.name == name) {
                    return@forEach
                } else {
                    existingJdk.sdkModificator.name = name
                    existingJdk.sdkModificator.versionString = version
                    val newSdk = createLocalJdk(name, version, homePath)
                    WriteAction.runAndWait<RuntimeException> {
                        ProjectJdkTable.getInstance().removeJdk(existingJdk)
                        ProjectJdkTable.getInstance().addJdk(newSdk)
                        JavaSdk.getInstance().setupSdkPaths(newSdk)
                    }
                }
            } ?: run {
                WriteAction.runAndWait<RuntimeException> {
                    val similarNamesCount = ProjectJdkTable.getInstance().allJdks.count { sdk -> sdk.name.startsWith(name) }
                    val sdkName = if (similarNamesCount <= 1) name else "$name (${similarNamesCount})"
                    val sdk = createLocalJdk(sdkName, version, homePath)
                    ProjectJdkTable.getInstance().addJdk(sdk)
                    JavaSdk.getInstance().setupSdkPaths(sdk)
                }
            }
        }
    }

    private fun getBuildDir(project: Project): Path? {
        val projectDir = project.guessProjectDir() ?: return null
        val basePath = Paths.get(projectDir.path)
        return basePath.resolve("build")
    }

    private fun createLocalJdk(name: String, version: String, path: String): Sdk {
        val javaSdk = JavaSdk.getInstance()
        val sdk = ProjectJdkTable.getInstance().createSdk(version, javaSdk)
        val sdkModificator = sdk.sdkModificator

        val sdkPath = PathUtil.toSystemDependentName(path)
        sdkModificator.homePath = sdkPath
        sdkModificator.name = name
        sdkModificator.versionString = version

        JavaSdkImpl.attachJdkAnnotations(sdkModificator)
        val application = ApplicationManager.getApplication()
        if (application.isDispatchThread) {
            application.runWriteAction {
                sdkModificator.commitChanges()
            }
        } else {
            application.invokeAndWait {
                application.runWriteAction {
                    sdkModificator.commitChanges()
                }
            }
        }

        return sdk
    }

    private fun getVersion(releaseFilePath: Path, dirName: String): String {
        var runtimeVersion = ""
        releaseFilePath.readLines().forEach {
            if (it.startsWith("JAVA_RUNTIME_VERSION")) {
                val parts = it.split("=")
                if (parts.size == 2) {
                    runtimeVersion = parts[1].replace("\"", "")
                }
            }
        }

        return if (runtimeVersion.isEmpty()) {
            dirName
        } else {
            "$runtimeVersion $dirName"
        }
    }

}