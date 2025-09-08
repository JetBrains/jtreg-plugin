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
package com.jetbrains.plugin.jtreg.util

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.openapi.vfs.isFile

object TestGroupUtils {

    private val testGroupsMap: MutableMap<String, TestGroup> = mutableMapOf()

    fun getTestGroups(): Array<TestGroup> = testGroupsMap.values.toTypedArray()

    fun findGroup(name: String) = testGroupsMap[name]

    private fun parseTestGroupsFile(file: VirtualFile): List<String> {
        var testGroupNames: List<String> = emptyList()
        ApplicationManager.getApplication().runReadAction {
            val content = String(file.contentsToByteArray(), Charsets.UTF_8)
            testGroupNames = parseTestGroupNames(content)
        }
        return testGroupNames
    }

    fun parseTestGroupNames(content: String): List<String> {
        val pattern = """(\w+)\s*=\s*\\""".toRegex()

        return pattern.findAll(content)
            .map { it.groupValues[1].trim() } // groupValues[1] gets just the name part
            .filter { it.isNotEmpty() }
            .toList()
    }

    fun updateTestGroups(project: Project) {
        val projectDir = project.guessProjectDir() ?: return

        ApplicationManager.getApplication().executeOnPooledThread {
            val testRootPaths = mutableSetOf<String>()
            val testGroupPaths = mutableSetOf<VirtualFile>()
            VfsUtilCore.visitChildrenRecursively(projectDir, object : VirtualFileVisitor<Any>() {
                override fun visitFile(file: VirtualFile): Boolean {
                    if (file.isFile) {
                        if (file.name == "TEST.ROOT") {
                            testRootPaths.add(file.parent.path)
                        } else if (file.name == "TEST.groups") {
                            testGroupPaths.add(file)
                        }
                    }
                    return true
                }
            })

            val localTestGroups = mutableListOf<TestGroup>()
            testGroupPaths.forEach { group ->
                val parentPath = group.parent.path
                if (testRootPaths.contains(parentPath)) {
                    val relativePath = VfsUtilCore.getRelativePath(group.parent, projectDir) ?: parentPath
                    val groupNames = parseTestGroupsFile(group)
                    groupNames.forEach { name ->
                        localTestGroups.add(TestGroup(parentPath, relativePath, name))
                    }
                }
            }
            localTestGroups.forEach { testGroup ->
                val name = "${testGroup.relativeTestDirectory}:${testGroup.groupName}"
                testGroupsMap[name] = testGroup
            }
        }
    }

}