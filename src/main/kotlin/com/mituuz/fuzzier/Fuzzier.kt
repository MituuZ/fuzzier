package com.mituuz.fuzzier

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.WindowManager

class Fuzzier : AnAction() {
    private var component = FuzzyFinder()

    override fun actionPerformed(p0: AnActionEvent) {
        component.textPane1.text = "asdqwieqwjeqduwu ";
        component.setSize(500, 500)
        component.fuzzyPanel.setSize(500, 500)

        val mainWindow = WindowManager.getInstance().getIdeFrame(p0.project)?.component
        mainWindow?.let {
            JBPopupFactory
                    .getInstance()
                    .createComponentPopupBuilder(component, null)
                    .createPopup()
                    .showInCenterOf(it)
        }
    }
}
