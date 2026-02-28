# NativeBrowser Developer Guide

Developer documentation for the NativeBrowser Unity plugin. This plugin provides a cross-platform native browser experience for Android, Windows, and WebGL including WebView, Custom Tabs, and System Browser integration.

## Table of Contents

- [Quick Start](#quick-start)
- [WebView Features](#webview-features)
- [Custom Tabs](#custom-tabs)
- [System Browser](#system-browser)
- [Configuration](#configuration)
- [Error Handling](#error-handling)
- [Threading Model](#threading-model)
- [ProGuard and Stripping](#proguard-and-stripping)

## Quick Start

### Installation

#### UPM via Git URL (Recommended)

Add to your `Packages/manifest.json`:

```json
{
  "dependencies": {
    "com.tedliou.nativebrowser": "https://github.com/tedliou/unity-native-browser.git#upm"
  }
}
```

To install a specific version, replace `#upm` with `#v1.1.0`.

#### UPM via Tarball

1. Download `com.tedliou.nativebrowser-<version>.tgz` from [Releases](https://github.com/tedliou/unity-native-browser/releases)
2. In Unity: **Window > Package Manager > + > Add package from tarball...**

#### .unitypackage

1. Download `NativeBrowser-<version>.unitypackage` from [Releases](https://github.com/tedliou/unity-native-browser/releases)
2. In Unity: **Assets > Import Package > Custom Package...**

#### Manual .aar

1. Download `NativeBrowser.aar` from [Releases](https://github.com/tedliou/unity-native-browser/releases)
2. Place in `Assets/Plugins/Android/`
3. Add the following Gradle dependencies to your `mainTemplate.gradle` or Custom Gradle Template:

```gradle
dependencies {
    implementation 'org.jetbrains.kotlin:kotlin-stdlib:2.1.20'
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.10.1'
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1'
    implementation 'androidx.browser:browser:1.9.0'
    implementation 'androidx.webkit:webkit:1.13.0'
    implementation 'androidx.activity:activity-ktx:1.10.0'
}
```

> **Note**: UPM installation includes these dependencies automatically via `NativeBrowserDeps.androidlib`.

### Callback Receiver

Create a script that inherits from `NativeBrowserCallbackReceiver` to handle browser events.

```csharp
using UnityEngine;
using TedLiou.NativeBrowser;

public class MyBrowserController : NativeBrowserCallbackReceiver
{
    public override void OnPageFinished(string json)
    {
        base.OnPageFinished(json); // Preserve event pipeline
        var data = JsonUtility.FromJson<PageFinishedEvent>(json);
        Debug.Log("Page loaded: " + data.url);
    }

    public override void OnError(string json)
    {
        base.OnError(json); // Preserve event pipeline
        var data = JsonUtility.FromJson<BrowserErrorEvent>(json);
        Debug.LogError($"Browser error: {data.message} at {data.url}");
    }
}
```

### Opening a URL

Initialize the browser and open a URL using `BrowserConfig`.

```csharp
using TedLiou.NativeBrowser;

public void OpenGoogle()
{
    NativeBrowser.Initialize();
    NativeBrowser.Open(BrowserType.WebView, new BrowserConfig("https://www.google.com"));
}
```

## WebView Features

### JavaScript Execution and Injection

You can execute JavaScript in the current page or inject code before the page loads.

```csharp
// Execute JavaScript — result delivered via OnJsResult event
NativeBrowser.ExecuteJavaScript("document.title", "get-title");

// Listen for the result
NativeBrowser.OnJsResult += (requestId, result) => {
    if (requestId == "get-title")
        Debug.Log("Page title: " + result);
};

// Inject JavaScript
NativeBrowser.InjectJavaScript("window.MyApp = { version: '1.0' };");
```

### PostMessage Communication

Web pages and Unity can exchange messages bidirectionally using PostMessage.

**Web → Unity**

The web page sends a message using either `window.postMessage(message, '*')` (intercepted by the bridge script) or `window.NativeBrowserBridge.postMessage(message)` (direct call). Any non-empty string is accepted.

```javascript
// Web side - sending string or JSON
window.postMessage("hello from web", "*");
window.NativeBrowserBridge.postMessage(JSON.stringify({ type: "LOGIN_SUCCESS", token: "xyz123" }));
```

```csharp
// Unity side (inside your callback receiver)
public override void OnPostMessage(string json)
{
    base.OnPostMessage(json); // Preserve event pipeline
    var data = JsonUtility.FromJson<PostMessageEvent>(json);
    Debug.Log("Received from web: " + data.message);
}
```

**Unity → Web**

Use `NativeBrowser.SendPostMessage(message)` to send a string to the web page.

```csharp
// Unity side
NativeBrowser.SendPostMessage("hello from Unity");
```

```javascript
// Web side
window.addEventListener('message', function(e) {
    console.log(e.data); // "hello from Unity"
});
```

### Sizing and Alignment

WebView supports fractional sizing (0.0 to 1.0) relative to the screen.

```csharp
var config = new BrowserConfig("https://example.com")
{
    width = 0.8f,
    height = 0.6f,
    alignment = Alignment.CENTER,
    closeOnTapOutside = true
};
NativeBrowser.Open(BrowserType.WebView, config);
```

### Deep Link Interception

Intercept specific URL patterns and handle them in Unity.

```csharp
var config = new BrowserConfig("https://example.com")
{
    deepLinkPatterns = new List<string> { "myapp://process/.*" },
    closeOnDeepLink = true
};
NativeBrowser.Open(BrowserType.WebView, config);

// In your callback receiver
public override void OnDeepLink(string json)
{
    base.OnDeepLink(json);
    var data = JsonUtility.FromJson<DeepLinkEvent>(json);
    Debug.Log("Intercepted deep link: " + data.url);
}
```

## Custom Tabs

Custom Tabs provide a Chrome-optimized browser experience that feels native to the app. Available on Android only; on Windows and WebGL, `BrowserType.CustomTab` falls back to the system browser.

```csharp
var config = new BrowserConfig("https://example.com");
NativeBrowser.Open(BrowserType.CustomTab, config);
```

## System Browser

Launch the device's default system browser. This moves the user out of your application.

```csharp
NativeBrowser.Open(BrowserType.SystemBrowser, new BrowserConfig("https://example.com"));
```

## Configuration

The `BrowserConfig` class allows detailed control over the browser instance. Construct with `new BrowserConfig(string url)`.

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `url` | `string` | (required via constructor) | The URL to open. |
| `width` | `float` | `1.0f` | Width from 0.0 to 1.0. |
| `height` | `float` | `1.0f` | Height from 0.0 to 1.0. |
| `alignment` | `Alignment` | `CENTER` | Positioning of the WebView. |
| `closeOnTapOutside` | `bool` | `false` | Close WebView when clicking background. |
| `deepLinkPatterns` | `List<string>` | empty list | Regex patterns for deep link interception. |
| `closeOnDeepLink` | `bool` | `true` | Close browser when a deep link is matched. |
| `enableJavaScript` | `bool` | `true` | Enable or disable JavaScript. |
| `userAgent` | `string` | `""` | Custom User-Agent string. |

## Error Handling

Events like `OnError` provide details when things go wrong. Common errors include network failures, invalid URLs, or JavaScript execution timeouts. Use the provided `BrowserErrorEvent` class to parse error details in your callback receiver.

## Threading Model

NativeBrowser ensures cross-platform threading compatibility for Unity's main thread:
- **Android**: Calls use `AndroidJavaClass` to bridge to `BrowserManager`. UI operations run on the activity's UI thread via `runOnUiThread`. Callbacks return to Unity via `UnitySendMessage`.
- **Windows**: WebView2 COM operations run on the STA thread. A callback dispatcher ensures results reach the Unity main thread.
- **WebGL**: JavaScript interop uses `.jslib` and `postMessage` relay for iframe overlays.
- **All Platforms**: Callbacks always arrive on the Unity main thread.

## ProGuard and Stripping

The library includes consumer ProGuard rules within the `.aar` to protect the bridge classes. If you use IL2CPP with high stripping levels, ensure you apply the `[Preserve]` attribute to your `NativeBrowserCallbackReceiver` implementation or use a `link.xml` file to prevent the callback methods from being stripped.
