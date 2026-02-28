# NativeBrowser for Unity

Cross-platform native browser plugin for Unity — WebView, Custom Tabs, and System Browser on Android, Windows, and WebGL via a simple C# API.

## Features
- **WebView**: Core feature available on Android and Windows. Full-featured in-app browser with configurable size, alignment, PostMessage, JS execution/injection, deep link interception, tap-outside-to-close, back button support
- **Windows WebView2**: WebView2-based in-app browser, standalone preview window in Editor, embedded child window in builds, DPI-aware
- **WebGL**: iframe overlay WebView with postMessage bridge, window.open fallback for Custom Tabs and System Browser
- **Custom Tabs**: Chrome Custom Tabs integration with customizable toolbar colors
- **System Browser**: Launch URLs in the default system browser
- Comprehensive event system (page lifecycle, errors, PostMessage, JS results, deep links)
- Configurable layout: width, height, 9-point alignment (CENTER, LEFT, RIGHT, TOP, BOTTOM, corners)

## Requirements
| Component | Version |
|-----------|---------|
| Unity | 6000.0.0f1+ (Unity 6) |
| Android minSdk | 28 (Android 9.0) |
| Android compileSdk | 36 |
| Windows | WebView2 Runtime (included in Windows 10/11 with Edge) |
| WebGL | Modern browser with iframe support |

## Installation

### UPM via Git URL (Recommended)

Add to your `Packages/manifest.json`:

```json
{
  "dependencies": {
    "com.tedliou.nativebrowser": "https://github.com/tedliou/unity-native-browser.git#upm"
  }
}
```

To install a specific version:

```json
{
  "dependencies": {
    "com.tedliou.nativebrowser": "https://github.com/tedliou/unity-native-browser.git#v1.1.0"
  }
}
```

Or via Unity Package Manager UI:
1. **Window > Package Manager > + > Add package from git URL...**
2. Enter: `https://github.com/tedliou/unity-native-browser.git#upm`

### UPM via Tarball

1. Download `com.tedliou.nativebrowser-<version>.tgz` from [Releases](https://github.com/tedliou/unity-native-browser/releases)
2. In Unity: **Window > Package Manager > + > Add package from tarball...**

### .unitypackage

1. Download `NativeBrowser-<version>.unitypackage` from [Releases](https://github.com/tedliou/unity-native-browser/releases)
2. In Unity: **Assets > Import Package > Custom Package...**

### Manual .aar

1. Download `NativeBrowser.aar` from [Releases](https://github.com/tedliou/unity-native-browser/releases)
2. Place in `Assets/Plugins/Android/`
3. Copy the C# scripts from `src/unity/Assets/Plugins/NativeBrowser/Runtime/` into your project
4. Add the required Gradle dependencies (see [docs/en/README.md](docs/en/README.md))

## Quick Start

```csharp
using TedLiou.NativeBrowser;

// Initialize once
NativeBrowser.Initialize();

// Open a WebView
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

### Create Release Artifacts
```bash
./tools/create-release.sh            # Build .aar + pack .tgz
./tools/create-release.sh --publish  # Also create GitHub Release
```

## Project Structure
```
.
├── src/
│   ├── android/          # Android Gradle project -> builds .aar
│   ├── windows/          # Windows Rust project -> builds .dll
│   └── unity/            # Unity 6 project (URP)
│       └── Assets/Plugins/NativeBrowser/  # UPM package root
├── tools/                # Automation scripts (build, test, deploy, release)
├── docs/                 # Developer documentation
└── README.md             # Project overview
```

## Testing
- Android unit tests: `./gradlew test` (from src/android/)
- Android instrumented tests: `./gradlew connectedAndroidTest`
- Windows Rust unit tests: `cd src\windows && cargo test`
- Unity EditMode/PlayMode tests: via Unity Test Runner

## License

[MIT](src/unity/Assets/Plugins/NativeBrowser/LICENSE)
