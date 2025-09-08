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
package com.jetbrains.plugin.jtreg.ui

import com.intellij.util.xmlb.annotations.OptionTag
import com.intellij.util.xmlb.annotations.XMap
import com.jetbrains.plugin.jtreg.configuration.IgnoreMode
import com.jetbrains.plugin.jtreg.configuration.TestMode

data class JTRegConfig(

    // JTREG LOCATION
    @XMap
    @OptionTag
    @JvmField
    val jtregDir: String = "",

    // VERBOSE OPTIONS

    @XMap
    @OptionTag
    @JvmField
    val verboseOutput: Boolean = true,

    // GENERAL OPTIONS

    @XMap
    @OptionTag
    @JvmField
    // -allowSetSecurityManager | -allowSetSecurityManager:<boolean-value>
    val allowSecurityManager: Boolean = false, // true, false

    @XMap
    @OptionTag
    @JvmField
    // -conc:<factor> | -concurrency:<factor>
    val concurrency: Int = 1, // positive value

    @XMap
    @OptionTag
    @JvmField
    // -ignore:<value>
    val ignore: IgnoreMode = IgnoreMode.ERROR, //

    @XMap
    @OptionTag
    @JvmField
    // -timeout:<number> | -timeoutFactor:<number>
    val timeoutFactor: Float = 1.0f,

    @XMap
    @OptionTag
    @JvmField
    // -tl:<#seconds> | -timelimit:<#seconds>
    val timeLimit: Int = 3600,

    @XMap
    @OptionTag
    @JvmField
    //-xml | -xml:verify
    val xmlReport: Boolean = false,

    @XMap
    @OptionTag
    @JvmField
    val vmTestMode: TestMode = TestMode.OTHER_VM,

    @XMap
    @OptionTag
    @JvmField
    val wakeFieldPath: String = "",

    @XMap
    @OptionTag
    @JvmField
    val westonDefaultScreenWidth: Int = 1024,

    @XMap
    @OptionTag
    @JvmField
    val westonDefaultScreenHeight: Int = 768,

    @XMap
    @OptionTag
    @JvmField
    val westonScreensCount: Int = 1,
)