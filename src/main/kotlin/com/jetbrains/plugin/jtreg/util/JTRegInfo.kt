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

import com.intellij.openapi.util.SystemInfoRt

data class JTRegInfo(
    val isValid: Boolean,
    val error: String,
    val version: String,
    val additionalFeatures: Map<String, String> = emptyMap()
) {

    constructor(error: String) : this(false, error, "", emptyMap())

    constructor(version: String, additionalFeatures: Map<String, String>) : this(true, "", version, additionalFeatures)

    fun isWestonSupported() = isFeatureSupported("WestonLauncher")

    fun isScreenshotValidationSupported() = isFeatureSupported("ScreenshotsSaver")

    fun isSavingScratchSupported() = isFeatureSupported("SaveScratch")

    fun isTestRepeatSupported() = isFeatureSupported("TestRepeat")

    private fun isFeatureSupported(name: String): Boolean {
        val os = additionalFeatures[name] ?: return false
        if (os == "all") return true
        return SystemInfoRt.OS_NAME.startsWith(os, true)
    }

}