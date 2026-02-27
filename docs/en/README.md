# NativeBrowser Developer Guide

Developer documentation for the NativeBrowser Unity plugin. This plugin provides a native Android browser experience including WebView, Custom Tabs, and System Browser integration.

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
1. Build the Android library project to generate the `.aar` file.
2. Copy the `.aar` file into your Unity project at `Assets/Plugins/Android/`.
3. Ensure the `NativeBrowser` C# scripts are in your `Assets/` directory.

### Callback Receiver
Create a script that inherits from `NativeBrowserCallbackReceiver` to handle browser events.

```csharp
using UnityEngine;
using TedLiou.NativeBrowser;

public class MyBrowserController : NativeBrowserCallbackReceiver
{
    protected override void OnPageFinished(string url)
    {
        Debug.Log("Page loaded: " + url);
    }

    protected override void OnError(string message, string url)
    {
        Debug.LogError($"Browser error: {message} at {url}");
    }
}
```

### Opening a URL
Initialize the browser and open a URL with default settings.

```csharp
using TedLiou.NativeBrowser;

public void OpenGoogle()
{
    NativeBrowser.Initialize();
    NativeBrowser.Open("https://www.google.com");
}
```

## WebView Features

### JavaScript Execution and Injection
You can execute JavaScript in the current page or inject code before the page loads.

```csharp
// Execute JavaScript and receive result
NativeBrowser.ExecuteJavaScript("document.title", (requestId, result) => {
    Debug.Log("Page title: " + result);
});

// Inject JavaScript
NativeBrowser.InjectJavaScript("window.MyApp = { version: '1.0' };");
```

### PostMessage Communication
The web page can communicate with Unity using `window.NativeBrowser.postMessage(jsonString)`.

```javascript
// Web side
window.NativeBrowser.postMessage(JSON.stringify({ type: "LOGIN_SUCCESS", token: "xyz123" }));
```

```csharp
// Unity side (inside your callback receiver)
protected override void OnPostMessage(string message)
{
    var data = JsonUtility.FromJson<MyMessageData>(message);
    Debug.Log("Received token: " + data.token);
}
```

### Sizing and Alignment
WebView supports fractional sizing (0.0 to 1.0) relative to the screen.

```csharp
var config = new BrowserConfig
{
    url = "https://example.com",
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
var config = new BrowserConfig
{
    url = "https://example.com",
    deepLinkPatterns = new List<string> { "myapp://process/.*" },
    closeOnDeepLink = true
};
NativeBrowser.Open(BrowserType.WebView, config);

// In your callback receiver
protected override void OnDeepLink(string url)
{
    Debug.Log("Intercepted deep link: " + url);
}
```

## Custom Tabs

Custom Tabs provide a Chrome-optimized browser experience that feels native to the app.

```csharp
var config = new BrowserConfig
{
    url = "https://example.com"
};
NativeBrowser.Open(BrowserType.CustomTab, config);
```

## System Browser

Launch the device's default system browser. This moves the user out of your application.

```csharp
NativeBrowser.Open(BrowserType.SystemBrowser, "https://example.com");
```

## Configuration

The `BrowserConfig` class allows detailed control over the browser instance.

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `url` | `string` | `""` | The URL to open. |
| `width` | `float` | `1.0f` | Width from 0.0 to 1.0. |
| `height` | `float` | `1.0f` | Height from 0.0 to 1.0. |
| `alignment` | `Alignment` | `CENTER` | Positioning of the WebView. |
| `closeOnTapOutside` | `bool` | `false` | Close WebView when clicking background. |
| `deepLinkPatterns` | `List<string>` | `null` | Regex patterns for deep link interception. |
| `closeOnDeepLink` | `bool` | `true` | Close browser when a deep link is matched. |
| `enableJavaScript` | `bool` | `true` | Enable or disable JavaScript. |
| `userAgent` | `string` | `""` | Custom User-Agent string. |

## Error Handling

Events like `OnError` provide details when things go wrong. Common errors include network failures, invalid URLs, or JavaScript execution timeouts. Use the provided `BrowserErrorEvent` class to parse error details in your callback receiver.

## Threading Model

NativeBrowser uses `AndroidJavaClass` to bridge between Unity and Android.
1. Calls from Unity C# occur on the Unity main thread.
2. The bridge passes these calls to the Android `BrowserManager`.
3. All UI-related operations (creating the WebView, adding it to the layout) are automatically moved to the Android UI thread using `activity.runOnUiThread`.
4. Callbacks from Android back to Unity use `UnitySendMessage`, which ensures the callback arrives on the Unity main thread.

## ProGuard and Stripping

The library includes consumer ProGuard rules within the `.aar` to protect the bridge classes. If you use IL2CPP with high stripping levels, ensure you apply the `[Preserve]` attribute to your `NativeBrowserCallbackReceiver` implementation or use a `link.xml` file to prevent the callback methods from being stripped.
