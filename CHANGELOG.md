# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed
- Improved resource management in `Audio.kt` to prevent potential leaks.
- Replaced generic exception handling with specific `IOException` and `SocketException` catches in `Client.kt`.
- Replaced generic exception handling with specific `IOException` and `SocketException` catches in `Server.kt`.

### Added
- `CHANGELOG.md` to track project changes.

### Removed
- Unnecessary `ACCESS_WIFI_STATE` and `CHANGE_WIFI_STATE` permissions from `AndroidManifest.xml`.
