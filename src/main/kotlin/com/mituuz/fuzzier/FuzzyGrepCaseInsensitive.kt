package com.mituuz.fuzzier

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.ui.components.JBList
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.util.concurrent.Future
import javax.swing.DefaultListModel

class FuzzyGrepCaseInsensitive : FuzzyGrep() {
    override var popupTitle: String = "Fuzzy Grep (Case Insensitive)"
    override var dimensionKey = "FuzzyGrepCaseInsensitivePopup"

    override fun runCommand(commands: List<String>, projectBasePath: String): String? {
        val modifiedCommands = commands.toMutableList()
        if (isWindows) {
            // Customize findstr for case insensitivity
            modifiedCommands.add("/I")
        } else {
            // Customize grep and ripgrep for case insensitivity
            modifiedCommands.add(1, "-i")
        }
        return super.runCommand(modifiedCommands, projectBasePath)
    }
}
