# API Reference

Detailed API documentation for the `TedLiou.NativeBrowser` namespace.

## Platform Support

- **Android**: Full support (WebView, Custom Tabs, System Browser).
- **Windows**: WebView2 (requires Edge/WebView2 Runtime). Custom Tabs falls back to system browser.
- **WebGL**: WebView via iframe overlay. Custom Tabs and System Browser via `window.open`.

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
| `Open(BrowserType type, BrowserConfig config)` | Opens a browser with the specified type and configuration. |
| `Close()` | Closes the current browser instance (if any). |
| `Refresh()` | Refreshes the current WebView page. No-op for other browser types. |
| `ExecuteJavaScript(string script, string requestId = null)` | Runs JavaScript in the WebView. Results are delivered via the `OnJsResult` event, correlated by `requestId`. |
| `InjectJavaScript(string script)` | Injects JavaScript into the WebView's global scope. |
| `SendPostMessage(string message)` | Sends a message to web content via JavaScript postMessage. WebView only. |

## BrowserConfig Class

Configuration settings for creating a browser instance. Construct with `new BrowserConfig(string url)`.

### Fields

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `url` | `string` | (required via constructor) | The URL to navigate to. |
| `width` | `float` | `1.0f` | Fractional width of the WebView (0.0 to 1.0). |
| `height` | `float` | `1.0f` | Fractional height of the WebView (0.0 to 1.0). |
| `alignment` | `Alignment` | `CENTER` | Positioning of the browser on screen. |
| `closeOnTapOutside` | `bool` | `false` | Whether to close the browser when clicking the surrounding background. |
| `deepLinkPatterns` | `List<string>` | empty list | A list of regex patterns to intercept URLs before navigation. |
| `closeOnDeepLink` | `bool` | `true` | Whether to automatically close the browser when a deep link is intercepted. |
| `enableJavaScript` | `bool` | `true` | Enables or disables JavaScript in the WebView. |
| `userAgent` | `string` | `""` | Overrides the default browser User-Agent. |

## BrowserType Enum

Supported browser types.

| Member | Value | Description |
|--------|-------|-------------|
| `WebView` | `0` | Integrated browser view inside the application. |
| `CustomTab` | `1` | Chrome Custom Tab on Android. Falls back to system browser on Windows and WebGL. |
| `SystemBrowser` | `2` | External system browser (all platforms). |

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

Events triggered by the browser and passed to `NativeBrowserCallbackReceiver`. These are public, serializable classes that can be used with `JsonUtility.FromJson<T>()` to parse the JSON string received by callback methods.

### PageStartedEvent
- `string url`: The URL being loaded.

### PageFinishedEvent
- `string url`: The URL that finished loading.

### BrowserErrorEvent
- `string type`: The error type (e.g., "LOAD_ERROR", "NETWORK_ERROR").
- `string message`: Description of the error.
- `string url`: The URL where the error occurred.
- `string requestId`: The request identifier, if applicable.

### PostMessageEvent
- `string message`: The message string received from JavaScript.

### JsResultEvent
- `string requestId`: The unique identifier for the JavaScript execution call.
- `string result`: The raw result from the script execution.

### DeepLinkEvent
- `string url`: The intercepted URL.

## PostMessage Protocol

### Web → Unity (Receiving Messages)

Web content can send messages back to Unity as plain strings or JSON strings. Any non-empty string is supported.

There are two ways to send messages from JavaScript:

1. **Intercepted postMessage**: Use the standard web `window.postMessage` API. Our bridge script intercepts these calls and forwards them to Unity.
   ```javascript
   // Send a plain string
   window.postMessage("Hello from Web", "*");

   // Send a JSON string
   window.postMessage(JSON.stringify({ type: "LOGIN", user: "Alice" }), "*");
   ```

2. **Direct Bridge call**: Call the bridge interface directly for slightly better performance.
   ```javascript
   window.NativeBrowserBridge.postMessage("Direct message");
   ```

Unity receives these messages through the `OnPostMessage` callback in your `NativeBrowserCallbackReceiver` subclass:

```csharp
public override void OnPostMessage(string json)
{
    base.OnPostMessage(json); // Preserve event pipeline
    var data = JsonUtility.FromJson<PostMessageEvent>(json);
    Debug.Log("Received from web: " + data.message);
}
```

### Unity → Web (Sending Messages)

Unity can send messages to the web page using the `SendPostMessage` method. This triggers a `message` event on the web page's `window` object.

```csharp
// From Unity
NativeBrowser.SendPostMessage("Hello from Unity!");
// Or send JSON
NativeBrowser.SendPostMessage(JsonUtility.ToJson(new MyData { status = "ok" }));
```

The web page listens for these messages using a standard event listener:

```javascript
window.addEventListener('message', (event) => {
    console.log("Received from Unity:", event.data);
});
```
