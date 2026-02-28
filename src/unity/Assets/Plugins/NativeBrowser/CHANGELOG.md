# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.1] - 2026-02-28

### Fixed

- Change `NativeBrowserCallbackReceiver` class visibility from `internal` to `public` so consumers can subclass it
- Change callback methods (`OnPageStarted`, `OnPageFinished`, `OnError`, `OnPostMessage`, `OnJsResult`, `OnDeepLink`, `OnClosed`) from non-virtual to `public virtual` to allow overriding
- Fix `deepLinkPatterns` default documented as `null` — actual default is empty list
- Fix `BrowserConfig` documentation missing constructor requirement (`new BrowserConfig(string url)`)
- Fix `BrowserErrorEvent` documentation listing only 2 fields — actual class has 4 fields (`type`, `message`, `url`, `requestId`)
- Remove phantom `Open(string url)` method from documentation (only `Open(BrowserType, BrowserConfig)` exists)
- Remove phantom `ExecuteJavaScript` callback parameter from documentation (results come via `OnJsResult` event)
- Fix callback override examples using wrong `protected override void OnXxx(parsed_params)` signature — correct signature is `public override void OnXxx(string json)`
- Rewrite all documentation (EN + ZH-TW) to match actual API
- Rewrite `NativeBrowserCallbackReceiverTest` to test public API surface instead of using reflection


## [1.1.0] - 2026-02-28

### Added

- **Windows platform support** — WebView2-based browser via Rust native layer (NativeBrowserWebView.dll)
  - Standalone EXE: borderless child window embedded inside Unity game window
  - Unity Editor: independent top-level preview window (1024×768 default, DPI-aware, auto-scaled)
  - STA-threaded COM operations for WebView2 lifecycle safety
  - Click-outside-to-close support
  - Custom Tabs fallback to system default browser on Windows
- **WebGL platform support** — iframe overlay and `window.open` fallback
  - WebView via responsive iframe overlay with `postMessage` bridge
  - Custom Tabs / System Browser via `window.open`
  - Automatic cross-origin `postMessage` relay
  - WebGL build validator editor tool
- `NativeBrowser.SendPostMessage(string message)` — send arbitrary string messages from Unity to web content via `window.postMessage`
- Direct `window.NativeBrowserBridge.postMessage(message)` call path for web → Unity messaging

### Changed

- PostMessage now accepts any non-empty string (previously required JSON with a `type` field)
- Bridge script uses smart detection: strings pass through as-is, objects are JSON-serialized

### Fixed

- Relocate Windows native DLL into UPM package root for proper inclusion in consumer project builds
## [1.0.3] - 2026-02-28

### Fixed

- Restore `NativeBrowser.aar.meta` (PluginImporter) to UPM package path so the `upm` branch includes it after `git subtree split` — without this file Unity does not recognize the .aar as an Android plugin, causing `ClassNotFoundException` at runtime

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
