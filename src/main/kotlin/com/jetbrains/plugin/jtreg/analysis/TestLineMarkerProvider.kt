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
package com.jetbrains.plugin.jtreg.analysis

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.execution.lineMarker.RunLineMarkerProvider
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiJavaFile
import com.jetbrains.plugin.jtreg.util.JTRegTagParser
import java.awt.Point

class TestLineMarkerProvider: RunLineMarkerProvider(), LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        return null
    }

    override fun collectSlowLineMarkers(elements: List<PsiElement>, result: MutableCollection<in LineMarkerInfo<*>>) {
        elements.forEach { element ->
            if (element.containingFile !is PsiJavaFile) return@forEach
            if (element !is PsiComment) return@forEach
            if (!element.text.contains("@test")) return@forEach

            val tags = JTRegTagParser.parseTags(element)
            val runs = tags.nameToTags.getOrDefault("run", emptyList())

            val runClass = element.containingFile.virtualFile.path

            runs.forEach { tag ->
                val range = TextRange(tag.tagStart, tag.tagEnd)
                val runCmd = tag.value.substringAfter("@run ")
                result.add(LineMarkerInfo(
                    element,
                    range,
                    AllIcons.RunConfigurations.TestState.Run,
                    { "Click to perform custom action" },
                    { e, elt ->

                        val project = elt.project

                        val context = SimpleDataContext.getProjectContext(project)

                        val modifiedContext = SimpleDataContext.builder()
                            .setParent(context)
                            .add(JTREG_RUN_CLASS, runClass)
                            .add(JTREG_RUN_CMD, runCmd)
                            .add(CommonDataKeys.PSI_ELEMENT, element)
                            .build()

                        showActionGroupPopup("jtreg.TestContextActions", modifiedContext,
                            e.component, Point(e.xOnScreen, e.yOnScreen))
                    },
                    GutterIconRenderer.Alignment.LEFT,
                    { "Custom action" }
                ))

            }
        }

    }


}