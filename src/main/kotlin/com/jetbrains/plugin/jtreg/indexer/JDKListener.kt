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
package com.jetbrains.plugin.jtreg.indexer

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.jetbrains.plugin.jtreg.configuration.SdkUtils
import com.jetbrains.plugin.jtreg.util.TestGroupUtils
import kotlinx.coroutines.runBlocking

class JDKListener(private val project: Project) : DumbService.DumbModeListener {

    override fun exitDumbMode() {
        TestGroupUtils.updateTestGroups(project)
        val modalTask = object : Task.Modal(project, "Detecting Local JDK Builds", true) {
            override fun run(indicator: ProgressIndicator) {
                runBlocking {
                    SdkUtils.detectLocalJdkBuilds(project)
                }
            }
        }
        ProgressManager.getInstance().run(modalTask)
    }
}