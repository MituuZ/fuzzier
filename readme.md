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
No empty strings or comments are supported.

All files in the project root have `/` at the start of the file path.

#### Options for exclusions are as follows:
- Ends with
  - e.g. `*.log`
  - excludes all files that end with `.log`
- Starts with
  - e.g. `/build*`
  - excludes all files from the project root that start with `build`
  - excludes all files under folders that start with `build`
  - to exclude only folders do append `/` at the end. e.g. `/build/*`
- Contains
  - e.g. `ee`
  - excludes all files and folders that have the string `ee` in them

![A picture of the IntelliJ IDEA settings, showing the exclusion list](assets/FileExclusion.png "An image of the IntelliJ IDEA settings")

### New tab
Decides whether Fuzzier opens a file to a new tab or not.

#### Tip
Can be combined with the editors tab amount limit. e.g. set the limit to 1 and open files in a new tab to allow splitting and changing files with vim commands (:sp and :vs)

### Debouncing
You can manually set a time after which the search processes the current search string, 
allowing inserting multiple characters before starting the process.

Defaults to 150 and can be set from 0 to 2000.

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
