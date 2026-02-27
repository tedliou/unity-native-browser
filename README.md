# NativeBrowser for Unity

Android native browser plugin for Unity — WebView, Custom Tabs, and System Browser via a simple C# API.

## Features
- **WebView**: Full-featured in-app browser with configurable size, alignment, PostMessage, JS execution/injection, deep link interception, tap-outside-to-close, back button support
- **Custom Tabs**: Chrome Custom Tabs integration with customizable toolbar colors
- **System Browser**: Launch URLs in the default system browser
- Comprehensive event system (page lifecycle, errors, PostMessage, JS results, deep links)
- Configurable layout: width, height, 9-point alignment (CENTER, LEFT, RIGHT, TOP, BOTTOM, corners)

## Requirements
| Component | Version |
|-----------|---------|
| Unity | 6000.3.10f1+ (Unity 6) |
| Android minSdk | 28 (Android 9.0) |
| Android compileSdk | 36 |
| Gradle | 8.7+ |

## Quick Start
1. Import `NativeBrowser.aar` into `Assets/Plugins/Android/`
2. Initialize in your script: `NativeBrowser.Initialize();`
3. Open a URL:
```csharp
var config = new BrowserConfig("https://example.com")
{
    width = 0.9f,
    height = 0.8f,
    alignment = Alignment.CENTER,
    closeOnTapOutside = true
};
NativeBrowser.Open(BrowserType.WebView, config);
```

## Documentation
- [English Developer Guide](docs/en/README.md)
- [繁體中文開發者指南](docs/zh-tw/README.md)
- [API Reference](docs/en/API.md)
- [Android Internals](docs/en/ANDROID_INTERNALS.md)

## Building from Source

### Build .aar
```bash
cd src/android
./gradlew assembleRelease
```
Output: `app/build/outputs/aar/app-release.aar`

### Build APK (Unity headless)
(Reference tools/ directory for automation scripts)

## Project Structure
```
.
├── src/
│   ├── android/          # Android Gradle project -> builds .aar
│   └── unity/            # Unity 6000.3.10f1 project (URP)
├── tools/                # Automation scripts (build, test, deploy, CI)
├── docs/                 # Developer documentation
└── README.md             # Project overview
```

## Testing
- Android unit tests: `./gradlew test` (from src/android/)
- Android instrumented tests: `./gradlew connectedAndroidTest`
