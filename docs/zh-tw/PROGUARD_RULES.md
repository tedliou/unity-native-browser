# ProGuard 規則指南

本文件說明何時以及如何更新本專案的兩個 ProGuard 規則檔案。

## 檔案說明

| 檔案 | 用途 |
|------|------|
| `src/android/app/proguard-rules.pro` | 本地 `.aar` 建置時套用 |
| `src/android/app/consumer-proguard-rules.pro` | 打包進 `.aar`，由消費端應用程式（Unity）的 R8/ProGuard 套用 |

**兩個檔案必須始終保持同步**。任何新增至其中一個的規則，都必須同步新增至另一個。

## 目前涵蓋範圍

| 類別 / 套件 | 規則 | 原因 |
|------------|------|------|
| `BrowserManager` | `-keep class … { *; }` | Unity 透過 `AndroidJavaClass`（JNI 反射）呼叫 |
| `bridge/**` | `-keep class … { *; }` | `BrowserBridge`、`UnityBridgeCallback`、`IUnitySender`、`ReflectionUnitySender` — 均在 JNI 呼叫路徑上或為其內部依賴 |
| `core/**` | `-keep class … { *; }` | `BrowserConfig`、`BrowserCallback` 等透過 JSON 在 Unity 與 Android 之間序列化/反序列化 |
| `JsBridge` 方法 | `-keepclassmembers … @JavascriptInterface` | WebView JS 介面 — 方法由 JavaScript 引擎呼叫，R8 無法追蹤 |
| `WebViewBrowser` | `-keep class … { *; }` | 由 `BrowserManager` 透過反射實例化 |
| `CustomTabBrowser` | `-keep class … { *; }` | 由 `BrowserManager` 透過反射實例化 |
| `SystemBrowser` | `-keep class … { *; }` | 由 `BrowserManager` 透過反射實例化 |
| `util/**`、`webview/**`、`customtab/**` | `-keep class … { *; }` | 上述類別使用的輔助類別 |

## 何時需要更新兩個檔案

### 1. 新增含 `@JavascriptInterface` 方法的類別

若新增含有 `@android.webkit.JavascriptInterface` 方法的類別：

```proguard
-keepclassmembers class com.tedliou.android.browser.webview.YourNewBridge {
    @android.webkit.JavascriptInterface <methods>;
}
```

目前只有 `JsBridge` 使用 `@JavascriptInterface`。若日後新增第二個橋接類別，必須明確列出。

### 2. 新增透過 Unity JNI 反射呼叫的類別

Unity 透過 `AndroidJavaClass` / `AndroidJavaObject` 呼叫 Android 類別。R8 無法追蹤這些參考，會將類別移除，除非明確保留：

```proguard
-keep class com.tedliou.android.browser.your.NewClass { *; }
```

### 3. 新增現有萬用字元未涵蓋的套件

目前萬用字元涵蓋 `bridge/`、`core/`、`webview/`、`customtab/`、`util/`。若新增頂層套件（例如 `notification/`），需新增萬用字元規則：

```proguard
-keep class com.tedliou.android.browser.notification.** { *; }
```

### 4. 新增瀏覽器實作類別

任何由 `BrowserManager.createBrowser()` 透過反射實例化的類別，都必須明確保留：

```proguard
-keep class com.tedliou.android.browser.your.NewBrowserImpl { *; }
```

## 不需要額外規則的類別

| 類別 | 原因 |
|------|------|
| `IUnitySender` | 介面位於 `bridge/` — 已由 `bridge/**` 萬用字元涵蓋；非 JNI 呼叫路徑 |
| `ReflectionUnitySender` | 具體類別位於 `bridge/` — 已由 `bridge/**` 萬用字元涵蓋；由 `BrowserBridge` 內部實例化，非 Unity 反射呼叫 |
| 僅由 Kotlin/Java 程式碼呼叫的類別 | R8 可追蹤這些參考，不會移除 |

## 發布前驗證清單

每次發布前確認：

- [ ] 所有含 `@JavascriptInterface` 的類別均已列於兩個 ProGuard 檔案中
- [ ] 所有從 Unity 透過 `AndroidJavaClass`/`AndroidJavaObject` 呼叫的類別均已保留
- [ ] 兩個檔案內容完全一致（執行 diff 比對）
- [ ] 以 `minifyEnabled = true` 建置並執行整合測試，確認無遺漏規則
