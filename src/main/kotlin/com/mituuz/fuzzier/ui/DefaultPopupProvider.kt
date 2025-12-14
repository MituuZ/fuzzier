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

package com.mituuz.fuzzier.ui

import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.DimensionService
import com.intellij.openapi.wm.WindowManager
import com.mituuz.fuzzier.util.FuzzierUtil.Companion.createDimensionKey
import java.awt.Component
import javax.swing.JComponent

class DefaultPopupProvider : PopupProvider {
    override fun show(
        project: com.intellij.openapi.project.Project,
        content: JComponent,
        focus: JComponent,
        config: PopupConfig
    ): JBPopup {
        // TODO: Check the error handling here
        val mainWindow: Component = WindowManager.getInstance().getIdeFrame(project)?.component
            ?: error("No IDE frame found for project")

        val screenBounds = mainWindow.graphicsConfiguration.bounds
        val screenDimensionKey = createDimensionKey(config.dimensionKey, screenBounds)

        if (config.resetWindow()) {
            DimensionService.getInstance().setSize(screenDimensionKey, config.preferredSizeProvider, null)
            DimensionService.getInstance().setLocation(screenDimensionKey, null, null)
            config.clearResetWindowFlag()
        }

        val popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(content, focus)
            .setFocusable(true)
            .setRequestFocus(true)
            .setResizable(true)
            .setDimensionServiceKey(null, screenDimensionKey, true)
            .setTitle(config.title)
            .setMovable(true)
            .setShowBorder(true)
            .createPopup()

        popup.showInCenterOf(mainWindow)
        return popup
    }
}