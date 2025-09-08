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

import com.intellij.psi.PsiComment
import com.intellij.psi.tree.java.IJavaElementType
import java.util.regex.Pattern

object JTRegTagParser {

    private val TAG_PATTERN = Pattern.compile("@([a-zA-Z]+)(\\s+|$)")

    data class Result(
        val nameToTags: Map<String, List<Tag>>
    )

    @JvmStatic
    fun parseTags(header: PsiComment): Result {
        var text = header.text
        if (text.length <= 2) return Result(emptyMap())
        text = text.dropLast(2)

        val tags: MutableList<Tag> = ArrayList()
        var start = -1
        var end = -1
        var tagStart = -1
        var tagEnd = -1

        var tagName: String? = null
        val tagText = StringBuilder()
        val prefix = if (header.tokenType is IJavaElementType) 2 else 3
        val lines = text.substring(prefix).split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        var pos = header.textOffset + prefix

        for (line in lines) {
            if (line.replace("[*\\s]+".toRegex(), "").isEmpty()) {
                pos += line.length + 1
                continue
            }
            val m = TAG_PATTERN.matcher(line)
            if (m.find()) {
                if (tagName != null) {
                    tags.add(Tag(start, pos, tagStart, tagEnd, tagName, tagText.toString()))
                    tagText.delete(0, tagText.length)
                }

                tagName = m.group(1)

                start = pos
                tagStart = pos + m.start()
                tagEnd = pos + m.end(1)
                tagText.append(line.substring(m.end()))
            } else if (tagName != null) {
                val asterisk = line.indexOf('*')
                tagText.append(line.substring(asterisk + 1))
            }

            pos += line.length + 1

            if (tagName != null) {
                end = pos
            }
        }

        if (tagName != null) {
            tags.add(Tag(start, end, tagStart, tagEnd, tagName, tagText.toString()))
        }

        val result: MutableMap<String, List<Tag>> = HashMap()

        for (tag in tags) {
            val innerTags = result.getOrDefault(tag.name, emptyList())
            result[tag.name] = innerTags + tag
        }

        return JTRegTagParser.Result(result)
    }

}