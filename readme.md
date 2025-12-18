# Fuzzier - IntelliJ IDEA plugin

<p align="left">
    <a href="https://plugins.jetbrains.com/plugin/23451-fuzzier" alt="Downloads">
        <img src="https://img.shields.io/jetbrains/plugin/d/23451-fuzzier" />
    </a>
    <a href="https://plugins.jetbrains.com/plugin/23451-fuzzier/versions" alt="Latest Version">
        <img src="https://img.shields.io/jetbrains/plugin/v/23451-fuzzier" />
    </a>
    <a href="https://plugins.jetbrains.com/plugin/23451-fuzzier/reviews" alt="Plugin Reviews">
        <img src="https://img.shields.io/jetbrains/plugin/r/stars/23451-fuzzier" />
    </a>
</p>

A simple plugin to allow "fuzzy" file search with the UI inspired
by [telescope.nvim](https://github.com/nvim-telescope/telescope.nvim)

If you are interested in plugin development you can check out my write-up about creating Fuzzier in
[here](https://mituuz.com/content/fuzzier_development.html).

Supports spaces in the search string, splitting the string and searching for both parts separately.
e.g. parts do not need to be in the correct order, as long as both succeed on their own.

When having a project open with multiple modules, the module folder is prepended to the file paths.
This might be somewhat unstable as modules do not have a definite root folder.
Current implementation uses the first content root.

![The UI consist of three parts. A file list on the top left, search field on the bottom left and the preview pane on the right](assets/FuzzierUI.png "An image of the plugin UI")

## Usage

Double-clicking a list item opens the file

When focused on the search field, you can use:

- Arrow keys to move the list selection up and down
- CTRL + p/n to move the list selection up/down (modifiable through keymap)
- CTRL + u/d to move the file preview half page up/down
- Enter to open the currently selected file

List movement can be remapped from settings -> keymaps, but do not support chorded shortcuts.

## Features

- Fuzzy file finder
  - Search all except excluded files
  - Search only from VCS-tracked files
- Text search leveraging [ripgrep](https://github.com/BurntSushi/ripgrep "Link to GitHub - ripgrep"), grep or findstr
  - With file extension support for ripgrep in the secondary search field
- File mover

## Documentation

For a more thorough documentation of the plugin, please refer to
the [Fuzzier Wiki](https://github.com/MituuZ/fuzzier/wiki).

The goal is to have a central place for all the documentation and to keep the README as a quick reference.

## Shortcuts

### Adding ideavim mapping for the plugin

Example `.ideavimrc` rows to add a vim keybindings for the plugin

```
" File search
map <Leader>fs <action>(com.mituuz.fuzzier.search.Fuzzier)
map <Leader>fg <action>(com.mituuz.fuzzier.search.FuzzierVCS)

" Mover
map <Leader>fm <action>(com.mituuz.fuzzier.operations.FuzzyMover)

" Grepping
map <Leader>fF <action>(com.mituuz.fuzzier.grep.FuzzyGrep)
map <Leader>ff <action>(com.mituuz.fuzzier.grep.FuzzyGrepCI)
map <Leader>fT <action>(com.mituuz.fuzzier.grep.FuzzyGrepOpenTabsCI)
map <Leader>ft <action>(com.mituuz.fuzzier.grep.FuzzyGrepOpenTabs)
map <Leader>fB <action>(com.mituuz.fuzzier.grep.FuzzyGrepCurrentBufferCI)
map <Leader>fb <action>(com.mituuz.fuzzier.grep.FuzzyGrepCurrentBuffer)
```

### Adding an editor shortcut

![A picture of the IntelliJ IDEA settings, showing where to set the shortcut](assets/Shortcut.png "An image of the IntelliJ IDEA settings")

## Installation

The plugin can be installed from
the [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/23451-fuzzier "Link to the JetBrains Marketplace - Fuzzier")

## Contact

I can be reached at <mitja@mituuz.com>.

## Contributing

I have a tendency to make some larger refactors from time to time,
so I would appreciate it if you would open an issue before starting to work on a feature or a bug fix.

I'll help as I can and can give guidance on how to implement the feature or fix the bug,
but cannot guarantee that I will accept the PR.
