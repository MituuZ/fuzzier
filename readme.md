# Fuzzier - IntelliJ IDEA plugin
A simple plugin to allow "fuzzy" file search with the UI inspired by [telescope.nvim](https://github.com/nvim-telescope/telescope.nvim)

If you are interested in plugin development you can check out my write-up about creating Fuzzier in [here.](https://mituuz.com/content/fuzzier_development.html)

Does **not** support true fuzzy finding. All search characters must be present and in correct order in the file path, but do not need to be sequential.

![The UI consist of three parts. A file list on the top left, search field on the bottom left and the preview pane on the right](assets/FuzzierUI.png "An image of the plugin UI")

## Usage
When focused on the search field, you can use:
- CTRL + j to move down
- CTRL + k to move up
- Enter to open the currently selected file (opens in current tab)

## Adding ideavim mapping for the plugin
Example of a .ideavimrc-row to add a vim keybinding for the plugin
```
map <Leader>pf <action>(com.mituuz.fuzzier.Fuzzier)
```

## Installation
Must be done manually by building the plugin and selecting the jar from the "Install Plugin From Disk"-option in the plugin menu
![A picture of the IntelliJ IDEA settings, showing where to install a plugin from disk](assets/Install.png "An image of the IntelliJ IDEA settings")

## Requirements (tested on)
IntelliJ IDEA 2023.3.2

## Potential improvements
- Smarter popup location handling
- Persistent split location
- Configurable exclusion list
- Configurable search weights with a test bench

## Contact
I can be reached from <mituuuuz@hotmail.com>
