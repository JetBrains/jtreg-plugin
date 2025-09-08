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

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project
import icons.JTRegPluginIcons
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import javax.swing.Icon

class JTRegConfigurationType: ConfigurationType {

    private val factory = object: ConfigurationFactory(this) {
        override fun createTemplateConfiguration(project: Project): RunConfiguration {
            return JTRegConfiguration("", project, this)
        }

        override fun getId(): @NonNls String {
            return "jtreg"
        }

        override fun isEditableInDumbMode(): Boolean {
            return true
        }
    }

    override fun getDisplayName(): @Nls(capitalization = Nls.Capitalization.Title) String {
        return "JBR JTReg"
    }

    override fun getConfigurationTypeDescription(): @Nls(capitalization = Nls.Capitalization.Sentence) String? {
        return "JBR JTReg configuration type"
    }

    override fun getIcon(): Icon? {
        return JTRegPluginIcons.JTREG_ICON_16
    }

    override fun getId(): @NonNls String {
        return "jtreg"
    }

    override fun getConfigurationFactories(): Array<out ConfigurationFactory> {
        return arrayOf(factory)
    }

    override fun isDumbAware(): Boolean {
        return true
    }

    object Util {
        @JvmStatic
        fun getInstance(): JTRegConfigurationType {
            return ConfigurationTypeUtil.findConfigurationType(JTRegConfigurationType::class.java)
        }
    }


}