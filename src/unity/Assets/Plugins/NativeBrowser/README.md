# NativeBrowser

Cross-platform native browser plugin for Unity — WebView, Custom Tabs, and System Browser on Android, Windows, and WebGL via a simple C# API.

## Requirements

| Component | Version |
|-----------|---------|
| Unity | 6000.0.0f1+ (Unity 6) |
| Android minSdk | 28 (Android 9.0) |
| Android compileSdk | 36 |
| Windows | WebView2 Runtime (included with Edge) |
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

### UPM via Tarball

1. Download `com.tedliou.nativebrowser-<version>.tgz` from [Releases](https://github.com/tedliou/unity-native-browser/releases)
2. In Unity: **Window > Package Manager > + > Add package from tarball...**

### .unitypackage

1. Download `NativeBrowser-<version>.unitypackage` from [Releases](https://github.com/tedliou/unity-native-browser/releases)
2. In Unity: **Assets > Import Package > Custom Package...**

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

- [English Developer Guide](https://github.com/tedliou/unity-native-browser/blob/master/docs/en/README.md)
- [API Reference](https://github.com/tedliou/unity-native-browser/blob/master/docs/en/API.md)
- [Android Internals](https://github.com/tedliou/unity-native-browser/blob/master/docs/en/ANDROID_INTERNALS.md)

## License

[MIT](LICENSE)
