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

import com.intellij.execution.ui.SettingsEditorFragment
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.components.DropDownLink
import com.jetbrains.plugin.jtreg.configuration.JTRegConfiguration
import org.jetbrains.annotations.Nls
import java.util.function.BiConsumer
import java.util.function.Consumer
import java.util.function.Supplier
import java.util.function.Function
import java.util.function.Predicate
import com.intellij.util.containers.ContainerUtil
import java.awt.Rectangle
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.Icon

class VariableSelectionFragment(
    id: String,
    @Nls(capitalization = Nls.Capitalization.Sentence) name: String,
    @Nls(capitalization = Nls.Capitalization.Title) group: String,
    component: JTTagButton,
    private val variantsProvider: Supplier<Array<String>>,
    private val getter: Function<JTRegConfiguration, String>,
    private val setter: BiConsumer<JTRegConfiguration, String>,
    initialSelection: Predicate<JTRegConfiguration>
    ): SettingsEditorFragment<JTRegConfiguration, JTTagButton>(id, name, group, component, null, null, initialSelection) {

    var variantNameProvider: Function<String, String>? = null
    var variantHintProvider: Function<String, String>? = null
    var variantDescriptionProvider: Function<String, String>? = null

    var selectedVariant: String = ""
        set(value) {
            field = value
            isSelected = true
            component().updateButton(name + ": " + getVariantName(value), null)
        }

    fun getVariants(): Array<String> {
        return variantsProvider.get()
    }

    var toggleListener: Consumer<String>? = null

    override fun toggle(selected: Boolean, e: AnActionEvent?) {
        super.toggle(selected, e)
        if (!selected && !getVariants().isEmpty()) {
            selectedVariant = getVariants()[0]
        }
    }

    override fun applyEditorTo(s: JTRegConfiguration) {
        setter.accept(s, selectedVariant)
    }

    override fun resetEditorFrom(s: JTRegConfiguration) {
        selectedVariant = getter.apply(s)
    }

    fun getVariantName(variant: String): @Nls String {
        return variantNameProvider?.apply(variant) ?: return StringUtil.capitalize(variant)
    }

    fun getVariantHint(variant: String): @Nls String? {
        return variantHintProvider?.apply(variant)
    }

    fun getVariantDescription(variant: String): @Nls String? {
        return variantDescriptionProvider?.apply(variant)
    }

    override fun isTag() = true

    override fun getCustomActionGroup(): ActionGroup? {
        val mapping = ContainerUtil.map(getVariants(), { s -> object: ToggleAction(getVariantName(s), getVariantHint(s), null) {
            override fun update(e: AnActionEvent) {
                super.update(e)
                getVariantDescription(s)?.let { description ->
                    e.presentation.putClientProperty(ActionUtil.SECONDARY_TEXT, description)
                }
            }

            override fun isSelected(e: AnActionEvent): Boolean {
                return s == selectedVariant
            }

            override fun setSelected(e: AnActionEvent, state: Boolean) {
                selectedVariant = s
                fireEditorStateChanged()
                toggleListener?.accept(selectedVariant)
                logChange(state, e)
            }

            override fun isDumbAware(): Boolean {
                return true
            }
        }})
        val group = object: DefaultActionGroup(name, mapping) {
            override fun update(e: AnActionEvent) {
                super.update(e)
                e.presentation.putClientProperty(ActionUtil.SECONDARY_TEXT, selectedVariant)
                e.presentation.isVisible = isRemovable
            }

            override fun getActionUpdateThread() = ActionUpdateThread.EDT

            override fun isDumbAware() = true
        }
        group.isPopup = true
        return group
    }

    class VariantTagButton: JTTagButton {

        private val dropDown: DropDownLink<String?> = DropDownLink(null) { link -> showPopup() }
        var fragment: VariableSelectionFragment? = null

        constructor(text: String, action: Consumer<in AnActionEvent>): super(text, action) {
            dropDown.autoHideOnDisable = false
            setLayer(dropDown, POPUP_LAYER)
            add(dropDown)
            button.addKeyListener(object : KeyAdapter() {
                override fun keyPressed(e: KeyEvent) {
                    if (e.keyCode == KeyEvent.VK_DOWN) {
                        dropDown.dispatchEvent(e)
                    }
                }

                override fun keyReleased(e: KeyEvent) {
                    if (e.keyCode == KeyEvent.VK_DOWN) {
                        dropDown.dispatchEvent(e)
                    }
                }
            })
        }

        fun showPopup(): JBPopup {
            val context = DataManager.getInstance().getDataContext(dropDown)
            val group = DefaultActionGroup(ContainerUtil.map(fragment!!.getVariants()) { v ->
                object : DumbAwareAction(fragment?.getVariantName(v)) {
                    override fun actionPerformed(e: AnActionEvent) {
                        fragment?.selectedVariant = v
                        IdeFocusManager.findInstanceByComponent(button).requestFocus(button, true)
                    }
                }
            })
            return JBPopupFactory.getInstance().createActionGroupPopup(null, group, context, JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, true)
        }

        override fun updateButton(text: @NlsContexts.Button String?, icon: Icon?) {
            println("updateButton: $text")
            val split = text?.split(": ")
            button.text = split?.getOrNull(0) + ": "
            dropDown.text = split?.getOrNull(1)
            layoutButtons()
        }

        override fun layoutButtons() {
            super.layoutButtons()
            var dropDownWidth = 0
            dropDown?.let {
                val preferredSze = dropDown.preferredSize
                dropDownWidth = preferredSze.width - ourInset * 2
                dropDown.bounds = Rectangle(closeButton.x - ourInset * 2, 0, preferredSze.width, button.height)
            }

            val insets = button.margin
            insets.right += dropDownWidth
            button.margin = insets

            val closeButtonBounds = closeButton.bounds
            closeButtonBounds.x += dropDownWidth
            closeButton.bounds = closeButtonBounds

            val bounds = button.bounds
            bounds.width += dropDownWidth
            button.bounds = bounds

            preferredSize = bounds.size
        }
    }

    companion object {

        fun createFragment(id: String,
                           @Nls(capitalization = Nls.Capitalization.Sentence) name: String,
                           @Nls(capitalization = Nls.Capitalization.Title) group: String,
                           variantsProvider: Supplier<Array<String>>,
                           getter: Function<JTRegConfiguration, String>,
                           setter: BiConsumer<JTRegConfiguration, String>,
                           initialSelection: Predicate<JTRegConfiguration>): VariableSelectionFragment {
            val ref: Ref<VariableSelectionFragment> = Ref<VariableSelectionFragment>()
            val tagButton = VariantTagButton(name) { e -> ref.get().toggle(false, null) }
            val fragment = VariableSelectionFragment(id, name, group, tagButton, variantsProvider, getter, setter, initialSelection)
            tagButton.fragment = fragment
            Disposer.register(fragment, tagButton)
            ref.set(fragment)
            return fragment
        }

    }


}