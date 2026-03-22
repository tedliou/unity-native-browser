# ProGuard Rules Guide

This document explains when and how to update the two ProGuard rule files in this project.

## Files

| File | Purpose |
|------|---------|
| `src/android/app/proguard-rules.pro` | Applied during local `.aar` builds |
| `src/android/app/consumer-proguard-rules.pro` | Bundled into the `.aar` and applied by the consuming app (Unity) |

Both files **must always be kept in sync**. Any rule added to one must be added to the other.

## Current Coverage

| Class / Package | Rule | Reason |
|----------------|------|--------|
| `BrowserManager` | `-keep class … { *; }` | Unity calls it via `AndroidJavaClass` (JNI reflection) |
| `bridge/**` | `-keep class … { *; }` | `BrowserBridge`, `UnityBridgeCallback`, `IUnitySender`, `ReflectionUnitySender` — all in JNI call path or internal dependencies |
| `core/**` | `-keep class … { *; }` | `BrowserConfig`, `BrowserCallback`, etc. serialized/deserialized as JSON between Unity and Android |
| `JsBridge` methods | `-keepclassmembers … @JavascriptInterface` | WebView JS interface — methods called by JavaScript engine, not visible to R8 |
| `WebViewBrowser` | `-keep class … { *; }` | Instantiated via reflection by `BrowserManager` |
| `CustomTabBrowser` | `-keep class … { *; }` | Instantiated via reflection by `BrowserManager` |
| `SystemBrowser` | `-keep class … { *; }` | Instantiated via reflection by `BrowserManager` |
| `util/**`, `webview/**`, `customtab/**` | `-keep class … { *; }` | Supporting classes used by the above |

## When to Update Both Files

### 1. Adding a new `@JavascriptInterface` method or class

If you add a new class with `@android.webkit.JavascriptInterface` methods:

```proguard
-keepclassmembers class com.tedliou.android.browser.webview.YourNewBridge {
    @android.webkit.JavascriptInterface <methods>;
}
```

Currently only `JsBridge` uses `@JavascriptInterface`. If a second bridge class is ever added, it must be explicitly listed.

### 2. Adding a new class called via Unity JNI reflection

Unity calls Android classes via `AndroidJavaClass` / `AndroidJavaObject`. R8 cannot see these references and will strip the class unless kept. Add:

```proguard
-keep class com.tedliou.android.browser.your.NewClass { *; }
```

### 3. Adding a new package outside the existing wildcards

The current wildcards cover `bridge/`, `core/`, `webview/`, `customtab/`, `util/`. If you add a new top-level package (e.g., `notification/`), add a wildcard rule:

```proguard
-keep class com.tedliou.android.browser.notification.** { *; }
```

### 4. Adding a new browser implementation

Any class instantiated via reflection by `BrowserManager.createBrowser()` must be explicitly kept:

```proguard
-keep class com.tedliou.android.browser.your.NewBrowserImpl { *; }
```

## Classes That Do NOT Need Additional Rules

| Class | Reason |
|-------|--------|
| `IUnitySender` | Interface in `bridge/` — already covered by `bridge/**` wildcard; not called via JNI |
| `ReflectionUnitySender` | Concrete class in `bridge/` — already covered by `bridge/**` wildcard; instantiated internally by `BrowserBridge`, not by Unity reflection |
| Any class only called from Kotlin/Java code | R8 can trace these references and will not strip them |

## Verification Checklist

Before each release, confirm:

- [ ] All classes with `@JavascriptInterface` are listed in both ProGuard files
- [ ] All classes called via `AndroidJavaClass`/`AndroidJavaObject` from Unity are kept
- [ ] Both files are identical in content (diff them)
- [ ] Build with `minifyEnabled = true` and run instrumented tests to catch any missing rules
