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

package com.mituuz.fuzzier.actions.grep

import com.mituuz.fuzzier.entities.FuzzyContainer
import javax.swing.DefaultListModel

class FuzzyGrepCaseInsensitive : FuzzyGrep() {
    override var popupTitle: String = "Fuzzy Grep (Case Insensitive)"
    override var dimensionKey = "FuzzyGrepCaseInsensitivePopup"

    override suspend fun runCommand(
        commands: List<String>,
        listModel: DefaultListModel<FuzzyContainer>,
        projectBasePath: String
    ) {
        val modifiedCommands = commands.toMutableList()
        if (isWindows && !useRg) {
            // Customize findstr for case insensitivity
            modifiedCommands.add(1, "/I")
        } else if (!useRg) {
            // Customize grep for case insensitivity
            modifiedCommands.add(1, "-i")
        } else {
            // Customize ripgrep for case insensitivity
            modifiedCommands.add(1, "--smart-case")
            modifiedCommands.add(2, "-F")
        }
        super.runCommand(modifiedCommands, listModel, projectBasePath)
    }
}
