/*
 *  MIT License
 *
 *  Copyright (c) 2025 Mitja Leino
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package com.mituuz.fuzzier.ui.popup

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.ComponentPopupBuilder
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.wm.WindowManager
import java.awt.Component
import javax.swing.JComponent

abstract class PopupProviderBase(
    protected val windowManager: WindowManager = WindowManager.getInstance(),
    protected val popupFactory: JBPopupFactory = JBPopupFactory.getInstance(),
) : PopupProvider {

    internal fun getMainWindow(project: Project): Component? = windowManager.getIdeFrame(project)?.component

    internal fun baseBuilder(content: JComponent, focus: JComponent, title: String): ComponentPopupBuilder =
        popupFactory
            .createComponentPopupBuilder(content, focus)
            .setFocusable(true)
            .setRequestFocus(true)
            .setResizable(true)
            .setTitle(title)
            .setMovable(true)
            .setShowBorder(true)
            .setCancelOnClickOutside(true)
            .setCancelOnWindowDeactivation(true)

    internal fun createCleanupListener(cleanupFunction: () -> Unit): JBPopupListener = object : JBPopupListener {
        override fun onClosed(event: LightweightWindowEvent) {
            cleanupFunction()
        }
    }
}
