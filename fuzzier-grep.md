# Pure Kotlin Grep Backend Plan (The "Fuzzier" Strategy)

## 1. Core APIs to Leverage

- **`CacheManager.getInstance(project).getFilesWithWord()`**: Find files indexed with specific words.
- **`ChangeListManager.getInstance(project)`**: Check if a file is ignored by VCS (e.g., via `.gitignore`).
- **`GlobalSearchScope`**: Restrict searches to the project scope, which naturally excludes library sources and excluded
  folders.
- **`ReadAction.nonBlocking`**: Ensures thread-safe index access that cancels automatically on user input.

## 2. The Hybrid Search Strategy

### Case A: Short String (Length < 3)

* **Action**: Use a linear iteration (like your `IntelliJIterationFileCollector`).
* **VCS Check**: For every file encountered, check `ChangeListManager.isIgnoredFile(vf)`. Skip if `true`.

### Case B: Long String (Length >= 3)

* **Action**: Use `CacheManager.getFilesWithWord()`.
* **VCS Check**:

1. The `GlobalSearchScope.projectScope(project)` already excludes many non-project files.
2. Filter the resulting `VirtualFile[]` against `ChangeListManager.isIgnoredFile(vf)`.

## 3. Passing the VCS Filter

To make this work cleanly, we should update `GrepConfig` or the `handleSearch` signature to include a file filter. This
allows `FuzzierVCS` (or any other action) to tell the backend exactly what to ignore.

1. **Define a Filter**: In `FuzzyGrep`, create a `fileFilter: (VirtualFile) -> Boolean`.
2. **VCS Logic**: Inside the filter, use `!ChangeListManager.getInstance(project).isIgnoredFile(vf)`.
3. **Backend Execution**: The `Fuzzier` backend should apply this filter before reading file contents.

## 4. Implementation Steps for `Fuzzier.handleSearch`

1. **Get Project instance**: Since `BackendStrategy` doesn't have it, we should add `project: Project` to the
   `handleSearch` parameters.
2. **Check Length**:

- If `searchString.length < 3`: Manually iterate project files using the VCS-aware filter.
- If `>= 3`: Query `CacheManager` using `GlobalSearchScope.projectScope(project)`, then filter results by VCS status and
  `grepConfig.targets`.

3. **Read & Match**: For valid files, load text, find matches, and batch-add to `listModel`.
4. **Batch & Cancel**: Use `withContext(Dispatchers.Main)` for batching and `ensureActive()` for cancellation.
5. **Limit**: Stop at 1000 results.

## 5. Performance Tips

- **Scope is Key**: `GlobalSearchScope` is very powerful; it can be restricted to specific modules or directories, which
  maps perfectly to your `grepConfig.targets`.
- **Read Action**: Always perform `CacheManager` and `ChangeListManager` checks inside a `ReadAction`.
