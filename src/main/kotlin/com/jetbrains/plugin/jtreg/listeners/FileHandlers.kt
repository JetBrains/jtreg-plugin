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
package com.jetbrains.plugin.jtreg.listeners

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.jetbrains.plugin.jtreg.service.JTRegService
import com.jetbrains.plugin.jtreg.util.JTRegLibUtils

object FileHandlers {

    fun processFileOpened(project: Project, rootManager: TestRootManager, testInfo: TestInfo) {
        ApplicationManager.getApplication().executeOnPooledThread {
            val file = testInfo.file
            if (!file.exists()) return@executeOnPooledThread
            val isJtreg = runReadAction { JTRegLibUtils.isJTRegTestData(project, file) }
            val isTestNg = runReadAction { JTRegLibUtils.isTestNGTestData(project, file) }
            val isJUnit = runReadAction { JTRegLibUtils.isJUnitTestData(project, file) }
            if (isJtreg || isTestNg || isJUnit) {
                val testRoots = runReadAction { JTRegLibUtils.getTestRoots(project, file) }
                val rootModel = runReadAction { rootManager.rootModel(testInfo, project) }
                rootModel?.use {
                    if (testInfo.roots != testRoots) {
                        rootModel.removeSourceFolders(testInfo.roots)
                        testInfo.roots = testRoots.toMutableList()
                        if (testRoots.isNotEmpty()) {
                            rootModel.addSourceFolders(testRoots)
                        }
                    }
                    if (isTestNg || isJUnit) {
                        val settings = ApplicationManager.getApplication().getService(JTRegService::class.java)
                        val libDir = settings.jtregHomeDirectory
                        val library = JTRegLibUtils.createJTRegLibrary(project, libDir)
                        testInfo.jtregLib = library
                        rootModel.addLibrary(library)
                    } else if (testInfo.jtregLib != null) {
                        rootModel.removeLibrary(testInfo.jtregLib!!)
                        testInfo.jtregLib = null
                    }
                }
            } else {
                val rootModel = runReadAction { rootManager.rootModel(testInfo, project) }
                rootModel?.use {
                    rootModel.removeSourceFolders(testInfo.roots)
                    testInfo.roots = mutableListOf()
                    testInfo.jtregLib?.let {
                        rootModel.removeLibrary(it)
                        testInfo.jtregLib = null
                    }
                }
            }
        }
    }

    fun processFileClosed(project: Project, rootManager: TestRootManager, testInfo: TestInfo) {
        ApplicationManager.getApplication().executeOnPooledThread {
            val file = testInfo.file
            val isJtregTestData = runReadAction { JTRegLibUtils.isTestData(project, file) }
            if (!file.exists() || isJtregTestData) {
                if (project.isOpen) {
                    val rootModel = runReadAction { rootManager.rootModel(testInfo, project) }
                    rootModel?.use {
                        val rootsToRemove = if (file.exists()) runReadAction {
                            return@runReadAction JTRegLibUtils.getTestRoots(project, file)
                        } else testInfo.roots
                        rootModel.removeSourceFolders(rootsToRemove)
                        testInfo.jtregLib?.let {
                            rootModel.removeLibrary(it)
                            testInfo.jtregLib = null
                        }
                    }
                }
            }
        }
    }

}