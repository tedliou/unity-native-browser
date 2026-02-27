# API Reference

Detailed API documentation for the `TedLiou.NativeBrowser` namespace.

## Table of Contents

- [NativeBrowser Class](#nativebrowser-class)
- [BrowserConfig Class](#browserconfig-class)
- [BrowserType Enum](#browsertype-enum)
- [Alignment Enum](#alignment-enum)
- [Event Classes](#event-classes)
- [PostMessage Protocol](#postmessage-protocol)

## NativeBrowser Class

Static class for controlling the browser instance.

### Properties

| Property | Type | Description |
|----------|------|-------------|
| `IsOpen` | `bool` | Returns true if any browser (WebView or Custom Tab) is currently open. |

### Methods

| Method | Description |
|--------|-------------|
| `Initialize()` | Initializes the native bridge. Call this before any other method. |
| `Open(string url)` | Opens the URL using the default browser (WebView) and settings. |
| `Open(BrowserType type, string url)` | Opens the URL using a specific browser type. |
| `Open(BrowserType type, BrowserConfig config)` | Opens the URL with specified configuration. |
| `Close()` | Closes the current browser instance (if any). |
| `Refresh()` | Refreshes the current WebView page. No-op for other browser types. |
| `ExecuteJavaScript(string script, Action<string, string> callback = null)` | Runs JavaScript in the WebView and returns the result through a callback. |
| `InjectJavaScript(string script)` | Injects JavaScript into the WebView's global scope. |

## BrowserConfig Class

Configuration settings for creating a browser instance.

### Fields

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `url` | `string` | `""` | The URL to navigate to. |
| `width` | `float` | `1.0f` | Fractional width of the WebView (0.0 to 1.0). |
| `height` | `float` | `1.0f` | Fractional height of the WebView (0.0 to 1.0). |
| `alignment` | `Alignment` | `CENTER` | Positioning of the browser on screen. |
| `closeOnTapOutside` | `bool` | `false` | Whether to close the browser when clicking the surrounding background. |
| `deepLinkPatterns` | `List<string>` | `null` | A list of regex patterns to intercept URLs before navigation. |
| `closeOnDeepLink` | `bool` | `true` | Whether to automatically close the browser when a deep link is intercepted. |
| `enableJavaScript` | `bool` | `true` | Enables or disables JavaScript in the WebView. |
| `userAgent` | `string` | `""` | Overrides the default browser User-Agent. |

## BrowserType Enum

Supported browser types.

| Member | Value | Description |
|--------|-------|-------------|
| `WebView` | `0` | Integrated browser view inside the application. |
| `CustomTab` | `1` | Android Custom Tab (Chrome) powered browser. |
| `SystemBrowser` | `2` | External system browser. |

## Alignment Enum

Positioning options for the WebView within the screen area.

| Member | Description |
|--------|-------------|
| `CENTER` | Centered horizontally and vertically. |
| `LEFT` | Aligned to the left, centered vertically. |
| `RIGHT` | Aligned to the right, centered vertically. |
| `TOP` | Aligned to the top, centered horizontally. |
| `BOTTOM` | Aligned to the bottom, centered horizontally. |
| `TOP_LEFT` | Aligned to the top-left corner. |
| `TOP_RIGHT` | Aligned to the top-right corner. |
| `BOTTOM_LEFT` | Aligned to the bottom-left corner. |
| `BOTTOM_RIGHT` | Aligned to the bottom-right corner. |

## Event Classes

Events triggered by the browser and passed to `NativeBrowserCallbackReceiver`.

### PageStartedEvent
- `string url`: The URL being loaded.

### PageFinishedEvent
- `string url`: The URL that finished loading.

### BrowserErrorEvent
- `string message`: Description of the error.
- `string url`: The URL where the error occurred.

### PostMessageEvent
- `string message`: The message string received from JavaScript.

### JsResultEvent
- `string requestId`: The unique identifier for the JavaScript execution call.
- `string result`: The raw result from the script execution.

### DeepLinkEvent
- `string url`: The intercepted URL.

## PostMessage Protocol

To communicate from a web page to Unity:

1. The page calls the native interface:
   ```javascript
   window.NativeBrowser.postMessage(jsonString);
   ```

2. Unity receives this via the `OnPostMessage` callback in `NativeBrowserCallbackReceiver`.
3. The `jsonString` should be a valid JSON to be parsed using `JsonUtility.FromJson`.
