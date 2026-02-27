# ANDROID PROJECT — NativeBrowser

## OVERVIEW

Kotlin Android library project. Fully implemented producing a production-ready .aar for Unity. Uses AGP 9.0.1 and Gradle 9.3.1. Package: `com.tedliou.android.browser`.

## STRUCTURE

```
src/android/
├── app/
│   ├── build.gradle.kts          # Module config — android.library
│   ├── consumer-proguard-rules.pro # Rules for the consuming Unity project
│   ├── proguard-rules.pro        # Rules for the library itself
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml   # INTERNET permission, <queries> for Custom Tabs
│       │   ├── java/com/tedliou/android/browser/
│       │   │   ├── BrowserManager.kt # Singleton entry point
│       │   │   ├── bridge/           # UnitySendMessage bridge
│       │   │   ├── core/             # Interfaces and configuration
│       │   │   ├── webview/          # WebView implementation
│       │   │   ├── customtab/        # Custom Tabs implementation
│       │   │   ├── system/           # System Browser launch
│       │   │   └── util/             # Helpers and Logger
│       │   └── res/                  # Layouts and assets
│       └── test/                     # Unit tests (Junit 4)
├── build.gradle.kts              # Root — applies AGP plugin
├── settings.gradle.kts           # Single module ":app"
├── gradle.properties             # AndroidX enabled, official Kotlin style
└── gradle/
    ├── wrapper/                  # Gradle 9.3.1
    └── libs.versions.toml        # Version catalog
```

## PACKAGE RESPONSIBILITIES

- `core/` — `IBrowser`, `BrowserConfig`, `BrowserType`, `Alignment`, `BrowserCallback`, `BrowserException`
- `bridge/` — `BrowserBridge` (UnitySendMessage), `UnityBridgeCallback`
- `webview/` — `WebViewBrowser`, `WebViewLayoutManager`, `JsBridge`, `DeepLinkMatcher`, `BackPressInterceptLayout`
- `customtab/` — `CustomTabBrowser`
- `system/` — `SystemBrowser`
- `util/` — `BrowserLogger`

## THREADING MODEL

**CRITICAL**: All WebView and UI-related operations MUST run on the Android UI thread. Use `activity.runOnUiThread { ... }` when calling these methods from Unity's worker threads.

## PROGUARD

The `consumer-proguard-rules.pro` file ensures that public API classes and methods used by Unity via JNI are not obfuscated or removed. `proguard-rules.pro` mirrors these settings for the library build.

## BUILD OUTPUT

The build process produces a release .aar at:
`app/build/outputs/aar/app-release.aar`

## DEPENDENCIES

| Library | Purpose |
|---------|---------|
| `androidx.core:core-ktx` | Kotlin extensions |
| `androidx.browser:browser` | Custom Tabs support |
| `androidx.webkit:webkit` | Advanced WebView features |
| `androidx.activity:activity-ktx` | Activity context helpers |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | Threading and async operations |
| `junit` | Unit tests |

## NOTES

- **AndroidManifest**: Contains `INTERNET` permission and `<queries>` element for package visibility (required for Custom Tabs on API 30+).
- **ProGuard**: Ensure `-keep` rules are updated if new JS interfaces are added.
- **Back Button**: Handled via `BackPressInterceptLayout` to allow closing WebView with system back button.
- **Logger**: Uses `BrowserLogger` for unified logging with "NativeBrowser" tag.
