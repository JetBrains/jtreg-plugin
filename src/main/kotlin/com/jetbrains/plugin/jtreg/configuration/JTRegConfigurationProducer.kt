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

import com.intellij.execution.JavaExecutionUtil
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.ConfigurationFromContext
import com.intellij.execution.application.ApplicationConfiguration
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.junit.JavaRunConfigurationProducerBase
import com.theoryinpractice.testng.configuration.TestNGConfiguration


abstract class JTRegConfigurationProducer: JavaRunConfigurationProducerBase<JTRegConfiguration>(), Cloneable {

    override fun getConfigurationFactory(): ConfigurationFactory {
        return JTRegConfigurationObject.configurationType.configurationFactories[0]
    }

    override fun isConfigurationFromContext(configuration: JTRegConfiguration, context: ConfigurationContext): Boolean {
        val contextLocation = context.location ?: return false
        val location = JavaExecutionUtil.stepIntoSingleClass(contextLocation) ?: return false

        val testClass = location.psiElement.containingFile
        val runClass = configuration.runClass
        return testClass != null && testClass.virtualFile != null && runClass == testClass.virtualFile.path
    }

    override fun shouldReplace(self: ConfigurationFromContext, other: ConfigurationFromContext): Boolean {
        if (other.configuration is TestNGConfiguration) {
            return true
        } else if (other.configuration is ApplicationConfiguration) {
            return true
        }
        return super.shouldReplace(self, other)
    }

}