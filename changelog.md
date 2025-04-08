# Changelog
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
