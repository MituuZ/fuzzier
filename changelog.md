# Changelog

## Version 2.0.1

- Fix incorrect `grep` command
- Partially fix `findstr` command
- Re-add the backend name to the popup title

**Known issues**

- `findstr` does not work with currently open tabs
  - To reduce the maintenance burden, I may remove support later
  - Performance is poor enough that I thought that the command wasn't returning any results

## Version 2.0.0

This version contains larger refactors and multiple new actions enabled by them.

I'm updating the existing package structure to keep things nicer and not supporting the old actions to avoid possible
problems in the future.

### Breaking changes

**Rename existing actions**

- `com.mituuz.fuzzier.FuzzyGrepCaseInsensitive` to `com.mituuz.fuzzier.grep.FuzzyGrepCI`
- `com.mituuz.fuzzier.FuzzyGrep` to `com.mituuz.fuzzier.grep.FuzzyGrep`
- `com.mituuz.fuzzier.Fuzzier` to `com.mituuz.fuzzier.search.Fuzzier`
- `com.mituuz.fuzzier.FuzzierVCS` to `com.mituuz.fuzzier.search.FuzzierVCS`
- `com.mituuz.fuzzier.FuzzyMover` to `com.mituuz.fuzzier.operation.FuzzyMover`

**Update default list movement keys**

- From `CTRL + j` and `CTRL + k` to `CTRL + n` and `CTRL + p`

### New actions

Added some new grep variations

```
com.mituuz.fuzzier.grep.FuzzyGrepOpenTabsCI
com.mituuz.fuzzier.grep.FuzzyGrepOpenTabs
com.mituuz.fuzzier.grep.FuzzyGrepCurrentBufferCI
com.mituuz.fuzzier.grep.FuzzyGrepCurrentBuffer
```

### Example mappings

```
" File search
nmap <Leader>sf <action>(com.mituuz.fuzzier.search.Fuzzier)
nmap <Leader>sg <action>(com.mituuz.fuzzier.search.FuzzierVCS)

" Mover
nmap <Leader>fm <action>(com.mituuz.fuzzier.operation.FuzzyMover)

" Grepping
nmap <Leader>ss <action>(com.mituuz.fuzzier.grep.FuzzyGrepCI)
nmap <Leader>sS <action>(com.mituuz.fuzzier.grep.FuzzyGrep)
nmap <Leader>st <action>(com.mituuz.fuzzier.grep.FuzzyGrepOpenTabsCI)
nmap <Leader>sT <action>(com.mituuz.fuzzier.grep.FuzzyGrepOpenTabs)
nmap <Leader>sb <action>(com.mituuz.fuzzier.grep.FuzzyGrepCurrentBufferCI)
nmap <Leader>sB <action>(com.mituuz.fuzzier.grep.FuzzyGrepCurrentBuffer)
```

### New features

- Popup now defaults to auto-sized, which scales with the current window
- You can revert this from the settings

### Other changes

- Refactor file search to use coroutines
  - Handle list size limiting during processing instead of doing them separately
- Add debouncing for file preview using `SingleAlarm`
- Refactor everything
- Remove manual handling of the divider location (use JBSplitter instead) and unify styling

## Version 1.14.0

- Add a global exclusion list for convenience when working with multiple projects
  - Combined with the project exclusions at runtime
- Add an option to use the editor font on the file list
  - Defaults to true

## Version 1.13.0

- Update dependencies
- Add file icons to the list renderer
  - Huge thanks to [jspmarc](https://github.com/jspmarc) for this!

## Version 1.12.0

- Add keybindings for CTRL + D/U for half page down/up scroll for the preview

## Version 1.11.0

- Support file type globs in FuzzyGrep when using ripgrep
- Update deprecated method calls
- Update dependencies
- Remove JMH deps
- Migrate from mockito to mockk

## Version 1.10.0

- Add an option to load full file contents in the FuzzyGrep preview window
  - Uses full highlighting
  - On by default
  - Thanks to [givemeurhats](https://github.com/givemeurhats) for highlighting this issue
- Update IntelliJ platform plugin to 2.7.2
- Update Gradle to 9.0.0

## Version 1.9.0

- Refactor FuzzyGrep to use coroutines
- Limit results size to avoid possible oom and increase speed
- Slightly increase the default dimensions of the popup
- Update some dependencies
- Increase the minimum version to 2025.1

## Version 1.8.2

- Fix command modification so FuzzyGrepCaseInsensitive now works correctly
- Improve FuzzyGrep string handling and validation for better performance and stability
- Show trimmed row text content in the popup
- **Known issue**: command output is handled line by line, but sometimes a single result can span more than one line,
  resulting in incomplete results

## Version 1.8.1

- Remove result limit, fix line and column positions for FuzzyGrep
- Update IntelliJ platform plugin to 2.6.0

## Version 1.8.0

- Improve FuzzyGrep process handling for consistent behavior
  - Huge thanks to [jspmarc](https://github.com/jspmarc) for this
- Update IntelliJ platform plugin to 2.5.0

## Version 1.7.1

- Add module info to project settings, unify debug format

## Version 1.7.0

- Add new action FuzzyGrepCaseInsensitive with smart case and non-regex search

## Version 1.6.1

- Update gradle and plugins
- Improve FuzzyGrep PATH handling by using GeneralCommandline
  - Thanks to [gymynnym](https://github.com/gymynnym) for finding the issue and the solution
- Indicate which command FuzzyGrep is running

## Version 1.6.0

- Introduce the first, experimental version of Fuzzy Grep
  - Call [ripgrep](https://github.com/BurntSushi/ripgrep) in the background
  - Attempt to fall back to `grep` or `findstr` if rg is not found

## Version 1.5.0

- Allow configuring the default finder popup size
- Fix a bug where the correct dimension key wasn't used
- Enable changing the search field location on the popup
  - Top, bottom, left and right
- Improve mover popup's default size

## Version 1.4.2

- Make highlighting speed faster when highlighting sequential matches
- Init JMH benchmarking to be used in the future

## Version 1.4.1

- Refactor match containers, add row support
- Properly separate project and application settings
- Fix duplicate files in search results

## Version 1.4.0

- Add option to ignore characters from search
- Update dependencies

## Version 1.3.0

- Add option to list recently searched files on popup open
- Update some dependencies

## Version 1.2.0

- Make popup dimensions persistent across projects
- Improve popup location consistency (fixes right screen, left half issue)
  - Popup dimensions are saved per screen bounds (location and size)
- Update kotlin-jvm and intellij-platform plugins to 2.1.0

## Version 1.1.1

- Use open version support from 2024.2 onwards<br><br>

## Version 1.1.0

- Improve task cancelling to avoid InterruptedException and reduce cancelling delay<br>
- Run searches with concurrency<br><br>

## Version 1.0.0

- Re-implement project file handling as a backup if no modules are present
- Migrate IntelliJ Platform Gradle Plugin to 2.x

## Version 0.26.2

- Re-implement project file handling as a backup if no modules are present

## Version 0.26.1

- Update version support to include 242

## Version 0.26.0

- Default to initial view when clearing the search string

## Version 0.25.1

- Fix task cancellation, making lower debounce time feel much better
- Lower default debounce time

## Version 0.25.0

- Clear up settings grouping
- Add option to highlight filename matches in the file list

## Version 0.24.0

- Add an option to change the font size for the preview window
- Some dependency updates
- Separate settings sections

## Version 0.23.0

- Add an option to show recently used files when opening the popup
- Fix mover not working correctly
- Refactor module handling

## Version 0.22.2

- Fix test bench not working correctly

## Version 0.22.1

- Add setting to prioritize shorter file paths for file mover
- Handle root path "/" separately for file mover
- Upgrade to gradle 8.7
- Multi module support for finder and mover
- Enable remapping list movement

## Version 0.21.0

- Add tolerance setting for string matching
- Set default preview text for files that cannot be previewed
- Use application manager instead of swing utilities for invoke later
- Fix settings modification comparison using set

## Version 0.20.0

- Score calculation rewrite
  - Upgraded and made more transparent
  - Each individual score for each file can be seen in the test bench
- Multi match correction
  - Correctly calculates each occurrence of unique search string characters in the filepath
- Introduced filename match weight
- Set up automated publishing to marketplace from GitHub on new tags

## Version 0.19.1

- Corrected version support from 241 to 241.*

## Version 0.19.0

- Create changelog.md
- Add Fuzzy search only for VCS tracked files (FuzzierVCS)
  - Huge thanks to [ramonvermeulen](https://github.com/ramonvermeulen) for implementing this feature
- Change versioning scheme to more clearly indicate development status
  - Version 0.19.0 is the first release under the new scheme

## Earlier Versions

- Prior to version 0.19.0, numerous features and improvements were made, laying the foundation for the current version.
  This includes initial development, various enhancements, and bug fixes. For detailed history,
  please refer to commit logs or release notes.
