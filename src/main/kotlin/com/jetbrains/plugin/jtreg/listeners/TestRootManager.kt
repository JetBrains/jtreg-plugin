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
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.roots.SourceFolder
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import java.nio.file.Paths

import java.util.*

class TestRootManager {

    private val refCount: MutableMap<VirtualFile, Int> = mutableMapOf()

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

        fun addLibrary(project: Project, jtregDir: String) {
            ApplicationManager.getApplication().invokeLater {
                WriteAction.run<RuntimeException> {
                    val jtregHome = jtregDir.takeIf { it.isNotBlank() } ?: return@run
                    val libPath = Paths.get(jtregHome, "lib").toString()
                    val libUrl = VfsUtilCore.pathToUrl(libPath)

                    val libraryTable = LibraryTablesRegistrar.getInstance().getLibraryTable(project)
                    val tableModel = libraryTable.modifiableModel
                    val library = tableModel.getLibraryByName(LIB_NAME) ?: tableModel.createLibrary(LIB_NAME)

                    val libraryModel = library.modifiableModel
                    try {
                        if (!libraryModel.isJarDirectory(libUrl, OrderRootType.CLASSES)) {
                            libraryModel.addJarDirectory(libUrl, /* recursive = */ true, OrderRootType.CLASSES)
                        }
                    } finally {
                        libraryModel.commit()
                    }
                    tableModel.commit()

                    try {
                        val alreadyThere = modifiableRootModel.orderEntries.any {
                            it is LibraryOrderEntry && it.libraryName == "jtreg-libs"
                        }
                        if (!alreadyThere) modifiableRootModel.addLibraryEntry(library)
                    } finally {
                        modifiableRootModel.commit()
                    }
                }
            }
        }

        override fun close() {
            ApplicationManager.getApplication().invokeLater {
                ApplicationManager.getApplication().runWriteAction {
                    if (!modifiableRootModel.isDisposed && modifiableRootModel.isChanged) {
                        modifiableRootModel.commit()
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

    companion object {

        private const val LIB_NAME = "jtreg-libs"

        private val logger: Logger = Logger.getInstance(TestRootManager::class.java)

    }

}
