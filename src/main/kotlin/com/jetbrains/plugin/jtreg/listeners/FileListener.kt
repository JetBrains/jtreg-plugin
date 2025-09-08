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
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.Alarm
import com.jetbrains.plugin.jtreg.util.JTRegLibUtils

class FileListener(private val project: Project): FileEditorManagerListener {

    private val testInfos: MutableMap<VirtualFile, TestInfo> = mutableMapOf()
    private val rootManager: TestRootManager = RootManagers.createTestRootManager(project)
    private val listeners: MutableMap<VirtualFile, DocumentListener> = mutableMapOf()
    private val alarms: MutableMap<VirtualFile, Alarm> = mutableMapOf()

    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        ApplicationManager.getApplication().executeOnPooledThread {
            val isInJtregRoot = runReadAction { JTRegLibUtils.isInJTRegRoot(file) }
            if (!isInJtregRoot) {
                return@executeOnPooledThread
            }

            DumbService.getInstance(project).smartInvokeLater({
                val testInfo = TestInfo(file)
                testInfos[file] = testInfo
                alarms[file] = Alarm()
                listeners[file] = object : DocumentListener {
                    private val alarm = alarms[file] ?: Alarm()
                    override fun documentChanged(event: DocumentEvent) {
                        alarm.cancelAllRequests()
                        alarm.addRequest({ FileHandlers.processFileOpened(project, rootManager, testInfo) }, 1000)
                    }
                }
                FileHandlers.processFileOpened(project, rootManager, testInfo)
            }, ModalityState.nonModal())
        }
    }

    override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
        testInfos[file]?.let { testInfo ->
            logger.debug("Closing indexed file ${file.path}")

            DumbService.getInstance(project).smartInvokeLater({
                FileHandlers.processFileClosed(project, rootManager, testInfo)
                testInfo.dispose()
                testInfos.remove(file)
                alarms[file]?.cancelAllRequests()
                listeners.remove(file)
                alarms.remove(file)
            }, ModalityState.nonModal())
        }
    }

    companion object {

        private val logger: Logger = Logger.getInstance(FileListener::class.java)

    }

}