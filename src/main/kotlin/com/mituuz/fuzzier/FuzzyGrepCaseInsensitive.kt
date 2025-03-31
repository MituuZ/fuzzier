package com.mituuz.fuzzier

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
            modifiedCommands.add(1, "--smart-case")
            modifiedCommands.add(2, "-F")
        }
        return super.runCommand(modifiedCommands, projectBasePath)
    }
}
