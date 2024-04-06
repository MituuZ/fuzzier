# Changelog
## Earlier Versions
- Prior to version 0.19.0, numerous features and improvements were made, laying the foundation for the current version. This includes initial development, various enhancements, and bug fixes. For detailed history, please refer to commit logs or release notes.

## Version 0.20.0
- Score calculation rewrite
  - Upgraded and made more transparent
  - Each individual score for each file can be seen in the test bench
- Multi match correction
  - Correctly calculates each occurrence of unique search string characters in the filepath
- Introduced filename match weight

## Version 0.19.1
- Corrected version support from 241 to 241.*

## Version 0.19.0
- Create changelog.md
- Add Fuzzy search only for VCS tracked files (FuzzierVCS)
  - Huge thanks to [ramonvermeulen](https://github.com/ramonvermeulen) for implementing this feature
- Change versioning scheme to more clearly indicate development status
  - Version 0.19.0 is the first release under the new scheme