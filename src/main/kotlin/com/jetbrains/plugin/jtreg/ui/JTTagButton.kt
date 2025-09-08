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

import com.intellij.icons.AllIcons
import com.intellij.ide.HelpTooltip
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.options.OptionsBundle
import com.intellij.openapi.ui.popup.IconButton
import com.intellij.openapi.util.NlsContexts
import com.intellij.ui.Gray
import com.intellij.ui.InplaceButton
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLayeredPane
import com.intellij.util.ObjectUtils
import com.intellij.util.ui.JBInsets
import com.intellij.util.ui.JBUI
import java.awt.*
import java.awt.event.FocusListener
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseListener
import java.util.function.Consumer
import javax.swing.Icon
import javax.swing.JButton
import javax.swing.KeyStroke

open class JTTagButton(private val text: String, private val action: Consumer<in AnActionEvent>) : JBLayeredPane(),
    Disposable {

    val button: JButton = object : JButton(text) {
        private val setBorderColorFunc = Consumer { c: Color? -> putClientProperty("JButton.borderColor", c) }

        override fun paintComponent(g: Graphics) {
            val outline = ObjectUtils.tryCast(getClientProperty("JComponent.outline"), String::class.java)

            if (outline != null) {
                if (outline == "error") {
                    setBorderColorFunc.accept(JBUI.CurrentTheme.Focus.errorColor(hasFocus()))
                    return
                } else if (outline == "warning") {
                    setBorderColorFunc.accept(JBUI.CurrentTheme.Focus.warningColor(hasFocus()))
                    return
                }
            }
            setBorderColorFunc.accept(if (hasFocus()) null else getBackgroundColor())
            super.paintComponent(g)
        }
    }

    val closeButton = InplaceButton(
        IconButton(null, AllIcons.Actions.Close, AllIcons.Actions.CloseDarkGrey)
    ) { a -> remove(action, null) }

    init {
        button.putClientProperty("styleTag", true)
        button.putClientProperty("JButton.backgroundColor", getBackgroundColor())
        button.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (KeyEvent.VK_BACK_SPACE == e.getKeyCode() || KeyEvent.VK_DELETE == e.getKeyCode()) {
                    remove(action, AnActionEvent.createFromInputEvent(e, "", null, DataContext.EMPTY_CONTEXT))
                }
            }
        })
        setLayer(button, DEFAULT_LAYER)
        add(button)

        closeButton.isOpaque = false
        HelpTooltip()
            .setTitle(OptionsBundle.message("tag.button.tooltip"))
            .setShortcut(KeyboardShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), null))
            .installOn(closeButton)
        setLayer(closeButton, POPUP_LAYER)
        add(closeButton, POPUP_LAYER)

        layoutButtons()
    }

    override fun addFocusListener(l: FocusListener?) {
        button.addFocusListener(l)
    }

    override fun removeFocusListener(l: FocusListener?) {
        button.removeFocusListener(l)
    }

    override fun addMouseListener(l: MouseListener?) {
        button.addMouseListener(l)
    }

    override fun removeMouseListener(l: MouseListener?) {
        button.removeMouseListener(l)
    }

    override fun hasFocus(): Boolean {
        return button.hasFocus()
    }

    open fun updateButton(text: @NlsContexts.Button String?, icon: Icon?) {
        button.text = text
        button.icon = icon
        layoutButtons()
    }

    open fun layoutButtons() {
        button.margin = JBInsets(0, 0, 0, 0)
        val size: Dimension = button.preferredSize
        val iconSize: Dimension = closeButton.preferredSize
        val tagSize = Dimension(size.width + iconSize.width - ourInset * 2, size.height)
        preferredSize = tagSize
        button.bounds = Rectangle(tagSize)
        button.margin = JBUI.insetsRight(iconSize.width)
        val p = Point(
            tagSize.width - iconSize.width - ourInset * 3,
            (tagSize.height - iconSize.height) / 2 + JBUI.scale(1)
        )
        closeButton.bounds = Rectangle(p, iconSize)
    }

    private fun remove(action: Consumer<in AnActionEvent>, e: AnActionEvent?) {
        isVisible = false
        e?.let { action.accept(it) }
    }

    override fun dispose() {

    }

    companion object {

        val ourInset = JBUI.scale(3)

        private fun getBackgroundColor(): Color {
            return JBColor.namedColor("Tag.background", Gray.xDF);
        }


    }
}