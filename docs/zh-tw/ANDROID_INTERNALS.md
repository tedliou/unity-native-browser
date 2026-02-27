# Android 內部架構

NativeBrowser 原生 Android 架構概覽。

## 架構圖

下圖概述了 Unity 與 Android 原生端之間的橋接關係。

```text
Unity (C#)
  |
  +-- NativeBrowser (靜態類別)
        |
        +-- AndroidJavaClass (橋接呼叫)
              |
              v
Android (Kotlin)
  |
  +-- BrowserManager (入口點)
        |
        +-- IBrowser (介面)
              |
              +-- WebViewBrowser (實作)
              +-- CustomTabBrowser (實作)
              +-- SystemBrowser (實作)
        |
        +-- BrowserBridge (事件處理)
              |
              v
Unity (C#)
  |
  +-- NativeBrowserCallbackReceiver (UnitySendMessage 目標)
```

## 套件結構

原生 Android 函式庫由以下套件組成：

- `core/`: 核心介面與領域模型。
  - `IBrowser`: 瀏覽器實作介面。
  - `BrowserConfig`: 配置資料類別。
  - `BrowserType`: 支援的瀏覽器類型列舉。
  - `Alignment`: 視圖對齊位置列舉。
  - `BrowserCallback`: 通用回調介面。
  - `BrowserException`: 自定義錯誤處理。

- `bridge/`: Unity 與 Android 橋接邏輯。
  - `BrowserBridge`: 透過 JNI 呼叫的主要 Kotlin 類別。
  - `UnityBridgeCallback`: 觸發 `UnitySendMessage` 的 `BrowserCallback` 實作。

- `webview/`: `WebView` 實作專屬邏輯。
  - `WebViewBrowser`: 主要的 WebView 實體處理程式。
  - `WebViewLayoutManager`: 計算比例與尺寸位置。
  - `JsBridge`: 注入網頁的 JavaScript 介面。
  - `DeepLinkMatcher`: 網址 regex 匹配邏輯。
  - `BackPressInterceptLayout`: 用於處理返回鍵的特殊佈局容器。

- `customtab/`: Chrome Custom Tabs 的實作。

- `system/`: 開啟系統預設瀏覽器的邏輯。

- `util/`: 諸如 `BrowserLogger` 等用於內部日誌的工具程式。

## 線程模型

為了確保線程安全並避免 UI 凍結：

- 所有來自 Unity 的呼叫都發生在 Unity 主線程。
- 原生 UI 操作（例如建立 `WebView` 或修改佈局）必須在 Android UI 線程執行。
- `BrowserManager` 在所有 UI 關鍵程式碼塊中使用 `activity.runOnUiThread{}`。
- 傳回 Unity 的事件使用 `UnitySendMessage`，以確保呼叫能傳遞到 Unity 的主線程。

## 返回鍵處理

本插件提供自定義的 `BackPressInterceptLayout`，它繼承自 `FrameLayout`。此佈局被用作 `WebView` 的根容器。

- 它覆寫了 `dispatchKeyEvent()` 以偵測 `KEYCODE_BACK`。
- 當偵測到返回鍵按下時，它會檢查 `WebView` 是否可以返回上一頁。
- 如果可以，則 `WebView` 回到上一頁；否則，它會關閉瀏覽器或將事件向上傳遞給 Activity。
- 這確保了返回鍵在 Unity 的原生輸入層處理之前被攔截。

## ProGuard 規則

本函式庫在 `.aar` 檔案中包含 consumer ProGuard 規則，確保 JNI 橋接程式碼不會在建置過程中被移除。

```proguard
# 保留 JNI 使用的橋接類別
-keep class com.tedliou.android.browser.bridge.** { *; }

# 保留 JSON 序列化使用的模型類別
-keep class com.tedliou.android.browser.core.** { *; }
```
