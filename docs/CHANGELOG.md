# Changelog

## [Unreleased]

### Added
- Created a `MainViewModelFactory` to handle `MainViewModel` instantiation.
- Implemented a new `getIpAddress()` method that correctly identifies the device's IP address when acting as a Wi-Fi hotspot.
- Added unit tests for `MainViewModel`, including tests for mode selection, client count updates, and the `onCleared` lifecycle event.
- Introduced a `TestCoroutineRule` to facilitate testing of coroutines.
- Created a professional `README.md` and updated this `CHANGELOG.md`.

### Fixed
- Resolved a startup crash caused by the `MainViewModel` not having a default constructor.
- Fixed an issue where the app would not display the correct IP address when in hotspot mode.
- Addressed several test failures in `MainViewModelTest` and improved the reliability of the test suite.

### Changed
- Refactored `MainViewModel` to allow for the injection of a `CoroutineDispatcher` for better testability.
- Moved all documentation files into the `docs/` directory to clean up the project root.
- Configured a `.gitignore` file to exclude build artifacts, Android Studio files, and test reports.
