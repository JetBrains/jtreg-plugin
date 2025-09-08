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
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.roots.SourceFolder
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.vfs.VirtualFile

import java.util.*

class TestRootManager {

    private val refCount: MutableMap<VirtualFile, Int> = mutableMapOf()
    val moduleLibRefCount: MutableMap<Module, Int> = mutableMapOf()

    inner class TestRootModel(private val contentEntry: ContentEntry, private val modifiableRootModel: ModifiableRootModel): AutoCloseable {

        fun addSourceFolders(sourceRoots: List<VirtualFile>) {
            for (file in sourceRoots) {
                try {
                    val count = refCount.getOrDefault(file, 0)
                    if (count == 0) {
                        logger.debug("Adding source folder $file")
                        val isSourceFolderExistInIndex = contentEntry.sourceFolders.any { it.file == file }
                        if (!isSourceFolderExistInIndex) {
                            contentEntry.addSourceFolder(file, true)
                        }
                    } else {
                        logger.debug("Source folder $file is already added")
                    }
                    refCount[file] = count + 1
                } catch (e: IllegalStateException) {
                    //logger.error("Failed to add source folder $file", e)
                }
            }
        }

        fun removeSourceFolders(sourceRoots: List<VirtualFile>) {
            for (folder in contentEntry.sourceFolders) {
                if (!folder.isTestSource) continue
                for (file in sourceRoots) {
                    if (file.url == folder.url) {
                        removeSourceFolder(file, folder)
                    }
                }
            }
        }

        fun removeSourceFolder(file: VirtualFile, folder: SourceFolder) {
            val count = refCount.getOrDefault(file, 0)
            if (count == 1) {
                contentEntry.removeSourceFolder(folder)
                refCount.remove(file)
                logger.debug("Removed source folder $file")
            } else if (count > 1) {
                refCount[file] = count - 1
                logger.debug("Remove reference to source folder $file, ref count: ${refCount[file]}")
            }
        }

        fun addLibrary(library: Library) {
            val count = moduleLibRefCount.getOrDefault(modifiableRootModel.module, 0)
            if (count == 0) {
                ApplicationManager.getApplication().runWriteAction {
                    modifiableRootModel.addLibraryEntry(library)
                }
                logger.debug("Added library $library")
            } else {
                logger.debug("Library $library is already added. Increased ref count: ${count + 1}")
            }
            moduleLibRefCount[modifiableRootModel.module] = count + 1
        }

        fun removeLibrary(library: Library) {
            val count = moduleLibRefCount.getOrDefault(modifiableRootModel.module, 0)
            if (count == 1) {
                val entry =
                    modifiableRootModel.orderEntries.firstOrNull { orderEntry -> orderEntry.presentableName == library.name }
                entry?.let {
                    modifiableRootModel.removeOrderEntry(it)
                }
                moduleLibRefCount.remove(modifiableRootModel.module)
                logger.debug("Removed library $library")
            } else if (count > 1) {
                moduleLibRefCount[modifiableRootModel.module] = count - 1
                logger.debug("Remove reference to library $library, ref count: ${count - 1}")
            }
        }

        override fun close() {
            ApplicationManager.getApplication().invokeLater {
                if (modifiableRootModel.isChanged) {
                    ApplicationManager.getApplication().invokeLater {
                        ApplicationManager.getApplication().runWriteAction { modifiableRootModel.commit() }
                    }
                } else {
                    ApplicationManager.getApplication().invokeLater {
                        modifiableRootModel.dispose()
                    }
                }
            }
        }

    }

    fun rootModel(testInfo: TestInfo, project: Project): TestRootModel? {
        val file = testInfo.file
        val module = ModuleUtilCore.findModuleForFile(file, project)
        module?: return null
        val modifiableRootModel = ModuleRootManager.getInstance(module).modifiableModel

        val contentEntry = modifiableRootModel.contentEntries
            .filter {
                val contentRoot = ProjectRootManager.getInstance(project).fileIndex.getContentRootForFile(file)
                return@filter Objects.equals(it.file, contentRoot);
            }.firstOrNull()

        contentEntry?: return null

        return TestRootModel(contentEntry, modifiableRootModel)
    }

    fun dispose() {
        refCount.clear()
        moduleLibRefCount.clear()
    }

    companion object {

        private val logger: Logger = Logger.getInstance(TestRootManager::class.java)

    }

}
