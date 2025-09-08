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

import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readLines

object JTRegUtils {

    fun getJTRegInfo(jtregHome: String): JTRegInfo {
        val jtregHomePath = Path.of(jtregHome)
        if (!jtregHomePath.resolve("bin").resolve("jtreg").exists()) return JTRegInfo("Invalid JTReg home path")

        val releaseFile = jtregHomePath.resolve("release")
        if (!releaseFile.exists()) return JTRegInfo("Release file not found")

        val releaseDescription = releaseFile.readLines()
        val additionalFeatures = getAdditionalFeatures(releaseDescription)
        val version = getVersion(releaseDescription)

        return JTRegInfo(version, additionalFeatures)
    }

    private fun getAdditionalFeatures(releaseDescription: List<String>): Map<String, String> {
        val additionalFeatures = releaseDescription.firstOrNull { it.startsWith("ADDITIONAL_FEATURES") } ?: return emptyMap()

        val parts = additionalFeatures.split("=")
        if (parts.size != 2) return emptyMap()

        val features = parts[1].split(",")
            .filter { it.isNotBlank() && it.contains('(') && it.contains(')') }
            .map { it.trim() }.toList()

        val ret = mutableMapOf<String, String>()
        features.forEach {
            val name = it.substringBefore('(')
            val os = it.substringAfter('(').substringBefore(')')
            ret[name] = os
        }
        return ret
    }

    private fun getVersion(releaseDescription: List<String>): String {
        val versionString = releaseDescription.firstOrNull { it.startsWith("JTREG_VERSION") } ?: return ""

        val parts = versionString.split("=")
        if (parts.size != 2) return ""
        return parts[1].trim()
    }

}