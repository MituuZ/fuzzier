# Fuzzier - IntelliJ IDEA plugin
A simple plugin to allow "fuzzy" file search with the UI inspired by [telescope.nvim](https://github.com/nvim-telescope/telescope.nvim)

If you are interested in plugin development you can check out my write-up about creating Fuzzier in [here.](https://mituuz.com/content/fuzzier_development.html)

Does **not** support true fuzzy finding. All search characters must be present and in correct order in the file path, but do not need to be sequential.

Since 0.8 supports spaces in search string, splitting the string and searching for both parts separately. e.g. parts do not need to be in the correct order, as long as both succeed on their own.

![The UI consist of three parts. A file list on the top left, search field on the bottom left and the preview pane on the right](assets/FuzzierUI.png "An image of the plugin UI")

## Usage
You can open Fuzzier with a shortcut or from the Tools menu

Double-clicking a list item opens the file

When focused on the search field, you can use:
- Arrow keys to move up and down
- CTRL + j to move down
- CTRL + k to move up
- Enter to open the currently selected file (opens in current tab)

## Settings
### Excluding files from the search
You exclude files and file paths by adding them to the file exclusion list in Settings → Tools → Fuzzier Settings.

Only supports contains for now. Identifiers are separated by newlines and empty strings are ignored.
![A picture of the IntelliJ IDEA settings, showing the exclusion list](assets/FileExclusion.png "An image of the IntelliJ IDEA settings")

## Shortcuts
### Adding ideavim mapping for the plugin
Example of a .ideavimrc-row to add a vim keybinding for the plugin
```
map <Leader>pf <action>(com.mituuz.fuzzier.Fuzzier)
```

### Adding a shortcut for the plugin
![A picture of the IntelliJ IDEA settings, showing where to set the shortcut](assets/Shortcut.png "An image of the IntelliJ IDEA settings")

## Installation
The plugin can be installed from the [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/23451-fuzzier)

## Requirements (tested on)
IntelliJ IDEA version 2023.1.5 or later

## Contact
I can be reached from <mituuuuz@hotmail.com>
