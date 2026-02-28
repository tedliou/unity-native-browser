# NativeBrowser 開發者指南

NativeBrowser Unity 插件的開發者文檔。此插件提供跨平台的原生瀏覽器體驗，支援 Android、Windows 和 WebGL 平台，包括 WebView、Custom Tabs 以及系統瀏覽器整合。

## 目錄

- [快速入門](#快速入門)
- [WebView 功能](#webview-功能)
- [Custom Tabs](#custom-tabs)
- [系統瀏覽器](#系統瀏覽器)
- [配置參數](#配置參數)
- [錯誤處理](#錯誤處理)
- [線程模型](#線程模型)
- [ProGuard 與代碼裁減](#proguard-與代碼裁減)

## 快速入門

### 安裝

#### UPM Git URL 安裝（推薦）

將以下內容加入您的 `Packages/manifest.json`：

```json
{
  "dependencies": {
    "com.tedliou.nativebrowser": "https://github.com/tedliou/unity-native-browser.git#upm"
  }
}
```

安裝特定版本，將 `#upm` 替換為 `#v1.1.0`。

#### UPM Tarball 安裝

1. 從 [Releases](https://github.com/tedliou/unity-native-browser/releases) 下載 `com.tedliou.nativebrowser-<version>.tgz`
2. 在 Unity 中：**Window > Package Manager > + > Add package from tarball...**

#### .unitypackage 安裝

1. 從 [Releases](https://github.com/tedliou/unity-native-browser/releases) 下載 `NativeBrowser-<version>.unitypackage`
2. 在 Unity 中：**Assets > Import Package > Custom Package...**

#### 手動 .aar 安裝

1. 從 [Releases](https://github.com/tedliou/unity-native-browser/releases) 下載 `NativeBrowser.aar`
2. 放入 `Assets/Plugins/Android/`
3. 將以下 Gradle 依賴加入您的 `mainTemplate.gradle` 或自訂 Gradle 模板：

```gradle
dependencies {
    implementation 'org.jetbrains.kotlin:kotlin-stdlib:2.1.20'
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.10.1'
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1'
    implementation 'androidx.browser:browser:1.9.0'
    implementation 'androidx.webkit:webkit:1.13.0'
    implementation 'androidx.activity:activity-ktx:1.10.0'
}
```

> **注意**：透過 UPM 安裝時，這些依賴會由 `NativeBrowserDeps.androidlib` 自動處理。

### 回調接收器

建立一個繼承自 `NativeBrowserCallbackReceiver` 的腳本來處理瀏覽器事件。

```csharp
using UnityEngine;
using TedLiou.NativeBrowser;

public class MyBrowserController : NativeBrowserCallbackReceiver
{
    public override void OnPageFinished(string json)
    {
        base.OnPageFinished(json); // 保留事件管線
        var data = JsonUtility.FromJson<PageFinishedEvent>(json);
        Debug.Log("頁面加載完成: " + data.url);
    }

    public override void OnError(string json)
    {
        base.OnError(json); // 保留事件管線
        var data = JsonUtility.FromJson<BrowserErrorEvent>(json);
        Debug.LogError($"瀏覽器錯誤: {data.message} 於 {data.url}");
    }
}
```

### 開啟網址

初始化瀏覽器並使用 `BrowserConfig` 開啟網址。

```csharp
using TedLiou.NativeBrowser;

public void OpenGoogle()
{
    NativeBrowser.Initialize();
    NativeBrowser.Open(BrowserType.WebView, new BrowserConfig("https://www.google.com"));
}
```

## WebView 功能

### JavaScript 執行與注入

您可以在當前頁面執行 JavaScript 或在頁面加載前注入代碼。

```csharp
// 執行 JavaScript — 結果透過 OnJsResult 事件回傳
NativeBrowser.ExecuteJavaScript("document.title", "get-title");

// 監聽結果
NativeBrowser.OnJsResult += (requestId, result) => {
    if (requestId == "get-title")
        Debug.Log("頁面標題: " + result);
};

// 注入 JavaScript
NativeBrowser.InjectJavaScript("window.MyApp = { version: '1.0' };");
```

### PostMessage 通訊

網頁與 Unity 可以透過 PostMessage 進行雙向訊息交換。

**網頁 → Unity**

網頁端可以使用 `window.postMessage(message, '*')`（由橋接腳本攔截）或 `window.NativeBrowserBridge.postMessage(message)`（直接呼叫）與 Unity 通訊。接受任何非空字串。

```javascript
// 傳送純字串
window.NativeBrowserBridge.postMessage("hello from web");

// 傳送 JSON 字串
window.NativeBrowserBridge.postMessage(JSON.stringify({ type: "LOGIN_SUCCESS", token: "xyz123" }));
```

```csharp
// Unity 端接收原始字串
public override void OnPostMessage(string json)
{
    base.OnPostMessage(json); // 保留事件管線
    var data = JsonUtility.FromJson<PostMessageEvent>(json);
    Debug.Log("收到訊息: " + data.message);
}
```

**Unity → 網頁**

使用 `NativeBrowser.SendPostMessage(message)` 將字串傳送至網頁。

```csharp
NativeBrowser.SendPostMessage("hello from Unity");
```

```javascript
window.addEventListener('message', function(e) {
    console.log("收到 Unity 訊息:", e.data);
});
```

### 尺寸與對齊方式

WebView 支持相對於螢幕的比例尺寸（0.0 到 1.0）。

```csharp
var config = new BrowserConfig("https://example.com")
{
    width = 0.8f,
    height = 0.6f,
    alignment = Alignment.CENTER,
    closeOnTapOutside = true
};
NativeBrowser.Open(BrowserType.WebView, config);
```

### 深層連結攔截

攔截特定的網址格式並在 Unity 中處理。

```csharp
var config = new BrowserConfig("https://example.com")
{
    deepLinkPatterns = new List<string> { "myapp://process/.*" },
    closeOnDeepLink = true
};
NativeBrowser.Open(BrowserType.WebView, config);

// 在您的回調接收器中
public override void OnDeepLink(string json)
{
    base.OnDeepLink(json);
    var data = JsonUtility.FromJson<DeepLinkEvent>(json);
    Debug.Log("攔截到深層連結: " + data.url);
}
```

## Custom Tabs

Custom Tabs 提供由 Chrome 驅動的優化瀏覽器體驗，外觀與應用程式更一致。僅限 Android 平台；在 Windows 和 WebGL 上，`BrowserType.CustomTab` 會回退為系統瀏覽器。

```csharp
var config = new BrowserConfig("https://example.com");
NativeBrowser.Open(BrowserType.CustomTab, config);
```

## 系統瀏覽器

開啟裝置預設的系統瀏覽器。這會讓使用者離開您的應用程式。

```csharp
NativeBrowser.Open(BrowserType.SystemBrowser, new BrowserConfig("https://example.com"));
```

## 配置參數

`BrowserConfig` 類別允許對瀏覽器實例進行詳細控制。透過 `new BrowserConfig(string url)` 建構。

| 選項 | 類型 | 預設值 | 說明 |
|--------|------|---------|-------------|
| `url` | `string` | （建構函式必填） | 要開啟的網址。 |
| `width` | `float` | `1.0f` | 寬度比例 (0.0 到 1.0)。 |
| `height` | `float` | `1.0f` | 高度比例 (0.0 到 1.0)。 |
| `alignment` | `Alignment` | `CENTER` | WebView 的對齊位置。 |
| `closeOnTapOutside` | `bool` | `false` | 點擊外部背景時是否關閉 WebView。 |
| `deepLinkPatterns` | `List<string>` | 空列表 | 用於攔截深層連結的正則表達式列表。 |
| `closeOnDeepLink` | `bool` | `true` | 匹配到深層連結時是否自動關閉瀏覽器。 |
| `enableJavaScript` | `bool` | `true` | 是否啟用 JavaScript。 |
| `userAgent` | `string` | `""` | 自定義 User-Agent 字串。 |

## 錯誤處理

諸如 `OnError` 之類的事件在發生問題時提供詳細資訊。常見錯誤包括網絡失敗、無效網址或 JavaScript 執行逾時。請在回調接收器中使用提供的 `BrowserErrorEvent` 類別來解析錯誤詳情。

## 線程模型

NativeBrowser 根據不同平台採用對應的線程策略：
- **Android**: 透過 `AndroidJavaClass` 橋接，所有 UI 操作自動在 `runOnUiThread` 執行，並使用 `UnitySendMessage` 將回調傳回 Unity 主線程。
- **Windows**: WebView2 的生命週期由 STA (Single-Threaded Apartment) 執行緒守護以確保 COM 操作安全。內部回調分派器會將事件同步回 Unity 主線程。
- **WebGL**: 透過 `.jslib` 進行 JavaScript 互操作，並利用 iframe `postMessage` 機制進行通訊。
- **所有平台**: 所有 C# 回調事件一律在 Unity 主線程上觸發，確保 API 使用安全。

## ProGuard 與代碼裁減

本函式庫在 `.aar` 中包含了 consumer ProGuard 規則，以保護橋接類別不被混淆或刪除。如果您使用 IL2CPP 並設定了高等級的代碼裁減，請確保在 `NativeBrowserCallbackReceiver` 的實作中套用 `[Preserve]` 屬性，或使用 `link.xml` 檔案來防止回調方法被裁減。
