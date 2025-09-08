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

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.jetbrains.plugin.jtreg.configuration.model.TestData
import com.jetbrains.plugin.jtreg.util.JTRegLibUtils

class JTRegDirectoryConfigurationProducer: JTRegConfigurationProducer() {

    override fun setupConfigurationFromContext(configuration: JTRegConfiguration, context: ConfigurationContext, sourceElement: Ref<PsiElement>): Boolean {
        val contextLocation = context.location
        contextLocation ?: return false

        val element = contextLocation.psiElement
        if (element !is PsiDirectory) {
            return false
        }

        val dir: PsiDirectory = element
        val virtualFile = dir.virtualFile
        if (!JTRegLibUtils.isInJTRegRoot(dir)) {
            return false
        }

        setupConfigurationModule(context, configuration)

        val originalModule = configuration.configurationModule.module
        configuration.setTestKind(TestData.TEST_DIRECTORY)
        configuration.`package` = virtualFile.path
        configuration.restoreOriginalModule(originalModule)
        configuration.name = dir.virtualFile.name

        configuration.classpathModifications = emptyList()

        return true
    }

}