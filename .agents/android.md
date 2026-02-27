# Android Architecture

## Package Structure

**Base package**: `com.tedliou.android.browser`

| Package | Key Classes | Responsibility |
|---------|-------------|----------------|
| `core/` | `IBrowser`, `BrowserConfig`, `BrowserType`, `Alignment`, `BrowserCallback`, `BrowserException` | Interfaces, config, types |
| `bridge/` | `BrowserBridge`, `UnityBridgeCallback` | UnitySendMessage relay via reflection |
| `webview/` | `WebViewBrowser`, `WebViewLayoutManager`, `JsBridge`, `DeepLinkMatcher`, `BackPressInterceptLayout` | WebView implementation |
| `customtab/` | `CustomTabBrowser`, `CustomTabConnectionManager` | Chrome Custom Tabs |
| `system/` | `SystemBrowser` | Intent.ACTION_VIEW fallback |
| `util/` | `BrowserLogger` | Centralized logging ("NativeBrowser" tag) |
| (root) | `BrowserManager` | Singleton entry point, factory for IBrowser |

**Source**: [`src/android/app/src/main/java/com/tedliou/android/browser/`](../src/android/app/src/main/java/com/tedliou/android/browser/)

## Design Patterns

1. **Strategy** — `IBrowser` interface; swap WebView / CustomTab / System at runtime
2. **Singleton** — `BrowserManager` (entry point), `BrowserBridge` (Unity relay), `BrowserLogger`
3. **Observer** — `BrowserCallback` (7 event methods)
4. **Sealed Class** — `BrowserException` (5 typed subclasses: `PageLoadException`, `JavaScriptException`, `ConfigException`, `InitializationException`, `NotAvailableException`, `InvalidUrlException`)
5. **WeakReference** — All browser classes hold `WeakReference<Activity>` to prevent leaks

## Threading Model

**CRITICAL**: All WebView/UI operations MUST run on Android UI thread.

- Unity calls arrive on GL thread
- `activity.runOnUiThread { ... }` wraps all browser operations
- `BrowserBridge.sendToUnity()` also marshals via runOnUiThread before UnityPlayer reflection
- After `runOnUiThread`, use `shadowOf(Looper.getMainLooper()).idle()` in tests

## Unity Bridge (Reflection)

```
Unity C# (GL thread)
  → AndroidJavaClass.Call("BrowserManager")
    → BrowserBridge.sendToUnity() [runOnUiThread]
      → Class.forName("com.unity3d.player.UnityPlayer")
        → UnitySendMessage("NativeBrowserCallback", methodName, json)
          → C# MonoBehaviour callback
```

- No compile-time dependency on `UnityPlayer` — uses `Class.forName` reflection
- Graceful degradation: missing UnityPlayer logs warning, no crash
- JSON format for all callbacks: `{"url":"...", "type":"...", ...}`

## ProGuard

Two rule files — **MUST** be kept in sync:

| File | Purpose |
|------|---------|
| [`proguard-rules.pro`](../src/android/app/proguard-rules.pro) | Library build |
| [`consumer-proguard-rules.pro`](../src/android/app/consumer-proguard-rules.pro) | Consuming apps (.aar users) |

**Rules cover**: `@JavascriptInterface` methods, Bridge classes, Config/Callback interfaces, all IBrowser implementations.

> When adding new JS interfaces or JNI-accessed classes, update BOTH ProGuard files.

## Dependencies

See [`gradle/libs.versions.toml`](../src/android/gradle/libs.versions.toml) for version catalog.

| Library | Purpose |
|---------|---------|
| `androidx.browser:browser` | Custom Tabs |
| `androidx.webkit:webkit` | Advanced WebView features |
| `androidx.activity:activity-ktx` | ComponentActivity (back button) |
| `kotlinx-coroutines-android` | Async operations |
| `mockk` + `robolectric` | Unit testing |

## Build Output

Release .aar at: `src/android/app/build/outputs/aar/app-release.aar`

See [build.md](build.md) for full build commands.
