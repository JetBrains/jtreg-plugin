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
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import com.intellij.openapi.vfs.InvalidVirtualFileAccessException
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtil
import java.io.File
import java.util.*

object JTRegLibUtils {

    private val logger: Logger = Logger.getInstance(JTRegLibUtils::class.java)

    private val _cachedTestConfigs: MutableMap<Project, MutableMap<VirtualFile, Properties>> = mutableMapOf()

    fun getTestRoots(project: Project, file: VirtualFile): List<VirtualFile> {
        return if (isJTRegTestData(project, file)) getJTRegRoots(project, PsiUtil.getPsiFile(project, file))
            else getTestNGRoots(PsiUtil.getPsiFile(project, file))
    }

    fun isTestData(project: Project, file: VirtualFile): Boolean {
        try {
            val psiFile = PsiUtil.getPsiFile(project, file)
            return isJTRegTestData(psiFile) || isTestNGTestData(psiFile) || isJUnitTestData(psiFile)
        } catch (_: AssertionError) { // no psi file
            return false
        } catch (e: Exception) {
            logger.error("Unable to verify test data for ${project.name}, ${file.path}", e)
            return false
        }
    }

    fun isJTRegTestData(project: Project, file: VirtualFile): Boolean {
        try {
            val psiFile = PsiUtil.getPsiFile(project, file)
            return isJTRegTestData(psiFile)
        } catch (e: Exception) {
            logger.error("Unable to verify test data for ${project.name}, ${file.path}", e)
            return false
        }
    }

    fun isJTRegTestData(file: PsiFile): Boolean {
        try {
            if (file is PsiJavaFile) {
                return PsiTreeUtil.findChildrenOfType(file, PsiComment::class.java).any { hasTestTag(it) }
            }
        } catch (_: InvalidVirtualFileAccessException) {
            return false
        }
        return false
    }

    fun isInJTRegRoot(dir: PsiDirectory): Boolean {
        return isInJTRegRoot(dir.virtualFile)
    }

    fun isInJTRegRoot(file: VirtualFile) = findJTRegRoot(file) != null

    private fun getJTRegRoots(project: Project, file: PsiFile): List<VirtualFile> {
        if (file !is PsiJavaFile) return emptyList()
        val childrenHeader = PsiTreeUtil.findChildrenOfType(file, PsiComment::class.java).firstOrNull { hasTestTag(it) } ?: return emptyList()
        val header = getTestHeader(childrenHeader) ?: return emptyList()

        val roots = mutableListOf<VirtualFile>()
        val pkgRoot = getPackageRoot(file)
        pkgRoot?.let {
            roots.add(pkgRoot)
        } ?: run {
            roots.add(file.virtualFile.parent)
        }

        val result = JTRegTagParser.parseTags(header)
        val libTags = result.nameToTags["library"]
        if (libTags != null) {
            for (libTag in libTags) {
                val libVal = libTag.value
                for (lib in libVal.split(" ")) {
                    var libFile: VirtualFile? = null
                    if (lib.startsWith("/")) {
                        val testRootFile = findRootFile(file.virtualFile)
                        testRootFile?.let {
                            val jtRegRoot = testRootFile.parent
                            libFile = jtRegRoot.findFileByRelativePath(lib.substring(1))
                            if (libFile == null) {
                                val testSuiteConfig = testSuiteConfigForRootFile(project, testRootFile)
                                testSuiteConfig?.let {
                                    val s = testSuiteConfig.getProperty("external.lib.roots")
                                    s?.let {
                                        val searchPath = jtRegRoot.findFileByRelativePath(s)
                                        searchPath?.let {
                                            libFile = searchPath.findFileByRelativePath(lib)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        file.parent?.let {
                            libFile = it.virtualFile.findFileByRelativePath(lib)
                        }
                    }
                    libFile?.let {
                        if (it.exists()) {
                            roots.add(it)
                        }
                    }
                }
            }
        }

        return roots
    }

    private fun testSuiteConfigForRootFile(project: Project, rootFile: VirtualFile): Properties? {
        val projectConfigs: MutableMap<VirtualFile, Properties> = _cachedTestConfigs.computeIfAbsent(project) { mutableMapOf() }

        var props = projectConfigs[rootFile]
        if (props == null) {
            props = parseTestSuiteConfig(rootFile)
            props?.let {
                projectConfigs[rootFile] = it
            }
            _cachedTestConfigs[project] = projectConfigs
        }
        return props
    }

    private fun parseTestSuiteConfig(rootFile: VirtualFile): Properties? {
        try {
            val input = rootFile.inputStream
            val props = Properties()
            props.load(input)
            return props
        } catch (e: Exception) {
            logger.error(e)
            return null
        }
    }

    /**
     * Given a file, searches up the vfs hierarchy for the closest parent directory containing the
     * associated test suite config (TEST.ROOT).
     * @param file the file
     * @return file referring to the test root directory or null if not found
     */
    private fun findJTRegRoot(file: VirtualFile): VirtualFile? {
        val testRootFile = findRootFile(file)
        testRootFile?.let {
            return file.parent
        }
        return null
    }

    private fun findRootFile(file: VirtualFile): VirtualFile? {
        var pointer: VirtualFile? = file
        while (pointer != null) {
            val rootFile = pointer.findChild("TEST.ROOT")
            if (rootFile != null) {
                return rootFile
            }
            pointer = pointer.parent
        }
        return null
    }

    fun getTestNGRoots(file: PsiFile): List<VirtualFile> {
        getPackageRoot(file)?.let { pkgRoot ->
            return listOf(pkgRoot)
        } ?: run {
            return listOf(file.virtualFile.parent)
        }
    }

    private fun getPackageRoot(file: PsiFile): VirtualFile? {
        if (file is PsiJavaFile) {
            val packageDeclaration = PsiTreeUtil.findChildrenOfType(file, PsiPackageStatement::class.java).firstOrNull() ?: return null
            val packages = packageDeclaration.packageName.split("\\.")
            var root = file.virtualFile
            for (i in packages.size - 1 downTo 0) {
                root = root.parent
                if (root.name != packages[i]) {
                    return null
                }
            }
            return root.parent
        }
        return null
    }

    /**
     * Is the given file a testng test?
     */
    fun isTestNGTestData(project: Project, file: VirtualFile): Boolean {
        try {
            val psiFile = PsiUtil.getPsiFile(project, file)
            return isTestNGTestData(psiFile)
        } catch (e: AssertionError) {
            return false
        }
    }

    /**
     * Is the given file a testng test?
     */
    fun isTestNGTestData(file: PsiFile): Boolean {
        try {
            if (file is PsiJavaFile) {
                val javaFile: PsiJavaFile = file
                val statement = javaFile.importList?.importStatements?.firstOrNull { it != null && isTestNGImport(it) }
                return statement != null
            }
        } catch (_: InvalidVirtualFileAccessException) {
            return false
        }
        return false
    }

    /**
     * Is the given file a testng test?
     */
    fun isTestNGImport(importStatement: PsiImportStatement): Boolean {
        val qualifiedName = importStatement.qualifiedName
        return qualifiedName != null && qualifiedName.startsWith("org.testng")
    }

    /**
     * Judge whether the given file is a junit test.
     */
    fun isJUnitTestData(project: Project, file: VirtualFile): Boolean {
        try {
            val psiFile = PsiUtil.getPsiFile(project, file)
            return isJUnitTestData(psiFile)
        } catch (e: AssertionError) {
            return false
        }
    }

    /**
     * Judge whether the given file has the junit import statement.
     */
    fun isJUnitTestData(file: PsiFile): Boolean {
        try {
            if (file is PsiJavaFile) {
                val javaFile: PsiJavaFile = file
                val statement = javaFile.importList?.importStatements?.firstOrNull { it != null && isJUnitImport(it) }
                return statement != null
            }
        } catch (_: InvalidVirtualFileAccessException) {
            return false
        }
        return false
    }

    /**
     * Judge whether the statement is a junit import statement.
     */
    private fun isJUnitImport(importStatement: PsiImportStatement): Boolean {
        val qualifiedName = importStatement.qualifiedName
        return qualifiedName != null && qualifiedName.startsWith("org.junit")
    }

    /**
     * Create (if not existing) and get the jtreg project library.
     */
    fun createJTRegLibrary(project: Project, jtregDir: String): Library {
        return updateJTRegLibrary(project, null, jtregDir)
    }

    /**
     * Update the jtreg project library. The library would be created if it doesn't exist.
     */
    fun updateJTRegLibrary(project: Project, oldJTRegDir: String?, newJTRegDir: String?): Library {
        val libraryTable = LibraryTablesRegistrar.getInstance().getLibraryTable(project)
        val tableModel = libraryTable.modifiableModel
        val library = tableModel.getLibraryByName("jtreg-libs") ?: tableModel.createLibrary("jtreg-libs")
        val libraryModel = library.modifiableModel

        val oldDir = "file://${oldJTRegDir}${File.separator}lib"
        if (!oldJTRegDir.isNullOrBlank() && libraryModel.isJarDirectory(oldDir, OrderRootType.CLASSES)) {
            libraryModel.removeRoot(oldDir, OrderRootType.CLASSES)
        }
        val newDir = "file://${newJTRegDir}${File.separator}lib"
        if (!newJTRegDir.isNullOrBlank() && !libraryModel.isJarDirectory(newDir, OrderRootType.CLASSES)) {
            libraryModel.addJarDirectory(newDir, true)
        }
        ApplicationManager.getApplication().runWriteAction {
            libraryModel.commit()
            tableModel.commit()
        }
        return library
    }

    /**
     * Does the given file contain a jtreg header?
     */
    private fun hasTestTag(element: PsiElement) = getTestHeader(element) != null

    /**
     * Does the given file contain a jtreg header?
     */
    private fun getTestHeader(element: PsiElement): PsiComment? {
        var pointer: PsiElement? = element
        while (pointer is PsiComment) {
            val comment: PsiComment = pointer
            if (comment.text.contains("@test")) {
                return comment
            }
            pointer = PsiTreeUtil.skipSiblingsForward(pointer, PsiWhiteSpace::class.java)
        }
        return null
    }


}