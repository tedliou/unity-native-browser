# Android Internals

Overview of the native Android architecture for NativeBrowser.

## Architecture Diagram

The following text-based diagram outlines the bridge between Unity and the Android native side.

```text
Unity (C#)
  |
  +-- NativeBrowser (Static class)
        |
        +-- AndroidJavaClass (Bridge call)
              |
              v
Android (Kotlin)
  |
  +-- BrowserManager (Entry point)
        |
        +-- IBrowser (Interface)
              |
              +-- WebViewBrowser (Implementation)
              +-- CustomTabBrowser (Implementation)
              +-- SystemBrowser (Implementation)
        |
        +-- BrowserBridge (Event handling)
              |
              v
Unity (C#)
  |
  +-- NativeBrowserCallbackReceiver (UnitySendMessage target)
```

## Package Structure

The native Android library is organized into the following packages:

- `core/`: Core interfaces and domain models.
  - `IBrowser`: Interface for browser implementations.
  - `BrowserConfig`: Configuration data class.
  - `BrowserType`: Supported browser enumeration.
  - `Alignment`: View positioning enumeration.
  - `BrowserCallback`: Generic callback interface.
  - `BrowserException`: Custom error handling.

- `bridge/`: Logic for Unity-to-Android and Android-to-Unity bridging.
  - `BrowserBridge`: The main Kotlin class invoked via JNI.
  - `UnityBridgeCallback`: Implementation of `BrowserCallback` that triggers `UnitySendMessage`.

- `webview/`: Dedicated logic for the `WebView` implementation.
  - `WebViewBrowser`: The primary WebView instance handler.
  - `WebViewLayoutManager`: Calculates sizes and fractional positioning.
  - `JsBridge`: The JavaScript interface injected into the web page.
  - `DeepLinkMatcher`: Logic for regular expression pattern matching on URLs.
  - `BackPressInterceptLayout`: Specialized layout container for handling the back button.

- `customtab/`: Implementation for Chrome Custom Tabs.

- `system/`: Logic for launching the system-wide default browser.

- `util/`: Helper utilities like `BrowserLogger` for internal logging.

## Threading Model

To ensure thread safety and avoid UI freezes:

- All calls from Unity arrive on the Unity main thread.
- Native UI operations (such as creating the `WebView` or modifying the layout) must be executed on the Android UI thread.
- `BrowserManager` uses `activity.runOnUiThread{}` for all such UI-critical blocks.
- Events sent back to Unity use `UnitySendMessage`, which correctly delivers the call to Unity's main thread.

## Back Button Handling

The plugin provides a custom `BackPressInterceptLayout` that extends `FrameLayout`. This layout is used as the root container for the `WebView`.

- It overrides `dispatchKeyEvent()` to detect `KEYCODE_BACK`.
- When a back press is detected, it checks if the `WebView` can go back.
- If it can, the `WebView` navigates back; otherwise, it closes the browser or passes the event up to the activity.
- This ensures the back button is intercepted before Unity's native input layer processes it.

## ProGuard Rules

The library includes consumer ProGuard rules within the `.aar` file to ensure the JNI bridge is not stripped during the build process.

```proguard
# Keep the bridge class used by JNI
-keep class com.tedliou.android.browser.bridge.** { *; }

# Keep models used for JSON serialization
-keep class com.tedliou.android.browser.core.** { *; }
```
