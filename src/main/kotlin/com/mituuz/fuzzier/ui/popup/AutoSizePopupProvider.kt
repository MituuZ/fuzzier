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
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.WindowManager
import java.awt.Component
import java.awt.Dimension
import javax.swing.JComponent

class AutoSizePopupProvider(
    private val widthFactor: Double,
    private val heightFactor: Double,
    windowManager: WindowManager = WindowManager.getInstance(),
    popupFactory: JBPopupFactory = JBPopupFactory.getInstance(),
) : PopupProviderBase(windowManager, popupFactory) {

    override fun show(
        project: Project,
        content: JComponent,
        focus: JComponent,
        config: PopupConfig,
        cleanupFunction: () -> Unit,
    ): JBPopup? {
        val mainWindow: Component = getMainWindow(project)
            ?: return null

        val popupSize = computePopupSize(mainWindow)

        val popup = baseBuilder(content, focus, config.title)
            .createPopup()

        popup.size = popupSize
        popup.showInCenterOf(mainWindow)

        popup.addListener(createCleanupListener(cleanupFunction))

        return popup
    }

    internal fun computePopupSize(mainWindow: Component): Dimension {
        val windowWidth = (mainWindow.width * widthFactor).toInt()
        val windowHeight = (mainWindow.height * heightFactor).toInt()
        return Dimension(windowWidth, windowHeight)
    }
}