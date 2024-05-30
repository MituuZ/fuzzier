# Changelog
## Earlier Versions
- Prior to version 0.19.0, numerous features and improvements were made, laying the foundation for the current version. This includes initial development, various enhancements, and bug fixes. For detailed history, please refer to commit logs or release notes.

## Version 0.23.0
- Fix mover not working correctly

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