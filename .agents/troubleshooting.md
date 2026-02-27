# Troubleshooting — Known Bugs & Fixes

## Critical Bugs

### 1. Material Theme Crash on Unity Activity

**Symptom**: `IllegalArgumentException: You need to use a Theme.AppCompat theme (or descendant)` when launching Custom Tabs.

**Root Cause**: Unity's `UnityPlayerGameActivity` doesn't use AppCompat theme. CustomTabsIntent.Builder requires it.

**Fix**: Use `CustomTabsIntent.Builder()` directly without theme wrapper. The Custom Tabs implementation in [`customtab/CustomTabBrowser.kt`](../src/android/app/src/main/java/com/tedliou/android/browser/customtab/CustomTabBrowser.kt) avoids Material dependencies entirely.

### 2. ClassNotFoundException for UnityPlayer

**Symptom**: `ClassNotFoundException: com.unity3d.player.UnityPlayer` during standalone Android testing.

**Root Cause**: UnityPlayer only exists at Unity runtime, not in test environment.

**Fix**: Reflection-based access with try-catch in [`bridge/BrowserBridge.kt`](../src/android/app/src/main/java/com/tedliou/android/browser/bridge/BrowserBridge.kt). Logs warning, doesn't crash.

### 3. Back Button Not Working (GameActivity)

**Symptom**: `OnBackPressedCallback` has no effect. Back button doesn't navigate WebView or close it.

**Root Cause**: Unity's `UnityPlayerGameActivity` extends `android.app.Activity`, not `androidx.activity.ComponentActivity`. `OnBackPressedDispatcher` requires ComponentActivity.

**Fix**: Safe cast `activity as? ComponentActivity` with graceful fallback. See [`webview/WebViewBrowser.kt`](../src/android/app/src/main/java/com/tedliou/android/browser/webview/WebViewBrowser.kt) lifecycle section. `BackPressInterceptLayout` provides alternative back button handling.

### 4. Custom Tabs `<queries>` Required on API 30+

**Symptom**: `CustomTabsClient.getPackageName()` returns null even when Chrome is installed.

**Root Cause**: Android 11+ (API 30) package visibility restrictions.

**Fix**: Added `<queries>` element in [`AndroidManifest.xml`](../src/android/app/src/main/AndroidManifest.xml) with `<action android:name="android.support.customtabs.action.CustomTabsService" />`.

### 5. ErrorMessage JSON Missing `type` Field

**Symptom**: C# `ErrorMessage` deserialization loses error type information.

**Root Cause**: Android sends `{"type":"PageLoadException", "message":"...", "url":"..."}` but C# class originally lacked `type` field.

**Fix**: Added `public string type;` to `ErrorMessage` class in [`NativeBrowserCallbackReceiver.cs`](../src/unity/Assets/Plugins/NativeBrowser/Runtime/NativeBrowserCallbackReceiver.cs).

**Technical Debt**: `requestId` field only present for `JavaScriptException` — C# only has `url` field. Low priority since most errors are `PageLoadException`.

### 6. BrowserException Sealed Class — Exhaustive `when`

**Symptom**: Compilation error when adding new exception subclass.

**Root Cause**: Kotlin sealed class requires exhaustive `when` expressions. Adding `InvalidUrlException` broke `UnityBridgeCallback.kt`.

**Fix**: Add matching branch in [`bridge/UnityBridgeCallback.kt`](../src/android/app/src/main/java/com/tedliou/android/browser/bridge/UnityBridgeCallback.kt) whenever a new `BrowserException` subclass is added.

## Build Issues

### 7. Gradle Cache Lock (WSL2)

**Symptom**: `Could not create service of type FileHasher` or file lock errors.

**Fix**: `./gradlew --stop && sleep 5 && ./gradlew assembleDebug --no-daemon`

### 8. AGP 9.0 Kotlin Plugin Conflict

**Symptom**: Duplicate plugin application error.

**Fix**: Do NOT add `kotlin-android` plugin to root `build.gradle.kts`. AGP 9.0+ handles Kotlin automatically. Use `kotlin { jvmToolchain(11) }` instead of `kotlinOptions`.

## Testing Issues

### 9. Robolectric Threading

**Symptom**: Callback assertions fail — callback never invoked.

**Root Cause**: `runOnUiThread` posts to main looper, but Robolectric doesn't auto-drain.

**Fix**: Call `shadowOf(Looper.getMainLooper()).idle()` after any operation using `runOnUiThread`.

### 10. JaCoCo + Robolectric Low Coverage

**Symptom**: Coverage reports show ~5% despite passing tests.

**Root Cause**: Robolectric tests run in JVM; JaCoCo's `testDebugUnitTest.exec` is 0 bytes.

**Status**: Known limitation. Coverage is validated via instrumented tests or manual review.

### 11. MockK ValueCallback Capture

**Pattern**: Use `slot<ValueCallback<String>>()` + `capture(callbackSlot)` + `callbackSlot.captured.onReceiveValue()` for WebView `evaluateJavascript` results.

### 12. Unity Test Framework — No JNI in Edit Mode

**Constraint**: Edit Mode tests cannot call Android JNI. Use reflection to validate API surface (methods, properties, events). Play Mode tests with `#if UNITY_ANDROID` guards for integration.
