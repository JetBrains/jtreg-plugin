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
package com.jetbrains.plugin.jtreg.configuration.model

import com.intellij.openapi.util.Comparing
import com.jetbrains.plugin.jtreg.configuration.RepeatCount
import java.util.*

data class RepeatSettings(
    var mode: String = RepeatCount.ONCE,
    var count: Int = 1,
    var maxCount: Int = 100,
): Cloneable {

    override fun equals(other: Any?): Boolean {
        if (other !is RepeatSettings) return false
        return Objects.equals(mode, other.mode) &&
                Objects.equals(count, other.count) &&
                Objects.equals(maxCount, other.maxCount)
    }

    override fun hashCode(): Int {
        return Comparing.hashcode(mode) xor
                Comparing.hashcode(count) xor
                Comparing.hashcode(maxCount)
    }
}