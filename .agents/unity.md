# Unity Architecture

## Project Info

- **Unity**: 6000.3.10f1 (Unity 6) with URP 17.3.0
- **Bundle ID**: `com.tedliou.android.browser`
- **minSdk**: 28, **compileSdk**: 36

## Namespace Structure

| Namespace | Visibility | Contents |
|-----------|-----------|----------|
| `TedLiou.NativeBrowser` | Public | `NativeBrowser`, `BrowserType`, `BrowserConfig`, `Alignment` |
| `TedLiou.NativeBrowser.Internal` | Internal | `NativeBrowserCallbackReceiver`, JSON models, JNI bridge |

**Source**: [`src/unity/Assets/Plugins/NativeBrowser/Runtime/`](../src/unity/Assets/Plugins/NativeBrowser/Runtime/)

## Assembly Definitions

| Asmdef | Platform | References |
|--------|----------|------------|
| `TedLiou.NativeBrowser` | All | (none) |
| `TedLiou.NativeBrowser.Editor` | Editor only | `TedLiou.NativeBrowser` |
| `TedLiou.NativeBrowser.Tests.Editor` | Editor only | `TedLiou.NativeBrowser` |
| `TedLiou.NativeBrowser.Tests.Runtime` | All | `TedLiou.NativeBrowser` |

## Public C# API

```csharp
// Initialize (call once)
NativeBrowser.Initialize();

// Open browser
NativeBrowser.Open(BrowserType.WebView, new BrowserConfig("https://example.com") {
    width = 0.9f, height = 0.8f,
    alignment = Alignment.CENTER,
    closeOnTapOutside = true
});

// Events
NativeBrowser.OnPageStarted += (url) => { };
NativeBrowser.OnPageFinished += (url) => { };
NativeBrowser.OnError += (message, url) => { };
NativeBrowser.OnPostMessage += (message) => { };
NativeBrowser.OnJsResult += (requestId, result) => { };
NativeBrowser.OnDeepLink += (url) => { };
NativeBrowser.OnClosed += () => { };

// JS operations (WebView only)
NativeBrowser.ExecuteJavaScript("document.title");
NativeBrowser.InjectJavaScript("console.log('hello')");

// Lifecycle
NativeBrowser.Close();
NativeBrowser.Refresh();
bool isOpen = NativeBrowser.IsOpen;
```

## Callback Pattern

`NativeBrowserCallbackReceiver` — internal singleton MonoBehaviour:
- Auto-creates GameObject named **`NativeBrowserCallback`** (hardcoded, must match Android bridge)
- `DontDestroyOnLoad` for scene persistence
- `[Preserve]` attribute on all callback methods (prevents IL2CPP stripping)
- Receives `UnitySendMessage(gameObjectName, methodName, json)` from Android
- Deserializes JSON via `JsonUtility.FromJson<T>()`, fires C# events

## Android Integration

### .aar Plugin

[`Assets/Plugins/Android/NativeBrowser.aar`](../src/unity/Assets/Plugins/Android/) — the native library.

### mainTemplate.gradle

[`Assets/Plugins/Android/mainTemplate.gradle`](../src/unity/Assets/Plugins/Android/mainTemplate.gradle) — Unity custom Gradle template includes Maven dependencies NOT bundled in .aar:

- `kotlin-stdlib`
- `kotlinx-coroutines-android`
- `androidx.browser:browser`
- `androidx.webkit:webkit`
- `androidx.activity:activity-ktx`

> Uses Unity placeholder tokens like `**APPLICATIONID**` — do NOT replace these.

### JNI Bridge

```csharp
#if UNITY_ANDROID && !UNITY_EDITOR
using (var javaClass = new AndroidJavaClass("com.tedliou.android.browser.BrowserManager"))
{
    javaClass.CallStatic("open", configJson);
}
#endif
```

Platform guarded: editor shows warnings, Android runs native.

## Testing

| Type | Location | Framework |
|------|----------|-----------|
| Edit Mode | [`Assets/Tests/Editor/`](../src/unity/Assets/Tests/Editor/) | Unity Test Framework, reflection-based |
| Play Mode | [`Assets/Tests/Runtime/`](../src/unity/Assets/Tests/Runtime/) | Unity Test Framework |

## Key Warnings

- **NEVER** search `src/unity/Library/` — 20k+ cached files, not project code
- **NativeBrowserCallback** GameObject name is hardcoded — do NOT rename
- If adding new JNI classes, update ProGuard rules in Android project
