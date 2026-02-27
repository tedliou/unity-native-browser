# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.2] - 2026-02-28

### Fixed

- Fix malformed `IPlatformBridge.cs.meta` preventing Unity from recognizing the script (invalid GUID, missing MonoImporter section)
- Fix `NativeBrowserDeps.androidlib/build.gradle` missing Android library plugin and `android {}` block, causing Gradle dependency resolution to fail during Unity APK build
- Fix malformed `NativeBrowser.aar.meta` missing PluginImporter section, preventing Unity from loading the .aar as an Android plugin
- Remove orphaned `NativeBrowser.aar.meta` from wrong path (`Runtime/Plugins/Android/`)

## [1.0.1] - 2025-06-08

### Added

- UPM package support (Git URL, .tgz tarball)
- `.androidlib` Gradle dependency resolution for consumers
- Editor tools: GradleTemplateValidator, PackageExporter
- Release automation: CI workflow for GitHub Releases + `upm` branch
- `tools/create-release.sh` local release script

### Changed

- .aar is now built by CI and injected into `upm` branch (not tracked in master)

## [1.0.0] - 2025-06-01

### Added

- WebView browser with configurable size, alignment, and tap-outside-to-close
- Chrome Custom Tabs integration with customizable toolbar colors
- System Browser launch via default browser app
- JavaScript execution and injection in WebView
- PostMessage communication between web content and Unity
- Deep link interception with regex pattern matching
- 9-point alignment system (CENTER, LEFT, RIGHT, TOP, BOTTOM, corners)
- Comprehensive event system (page lifecycle, errors, PostMessage, JS results, deep links)
- Back button support for WebView navigation
- Consumer ProGuard rules included in .aar
