# NativeBrowser 開發者指南

NativeBrowser Unity 插件的開發者文檔。此插件提供原生的 Android 瀏覽器體驗，包括 WebView、Custom Tabs 以及系統瀏覽器整合。

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
1. 建置 Android 函式庫專案以生成 `.aar` 檔案。
2. 將 `.aar` 檔案複製到 Unity 專案中的 `Assets/Plugins/Android/` 目錄。
3. 確保 `NativeBrowser` C# 腳本已放置在 `Assets/` 目錄下。

### 回調接收器
建立一個繼承自 `NativeBrowserCallbackReceiver` 的腳本來處理瀏覽器事件。

```csharp
using UnityEngine;
using TedLiou.NativeBrowser;

public class MyBrowserController : NativeBrowserCallbackReceiver
{
    // 當頁面加載完成時觸發
    protected override void OnPageFinished(string url)
    {
        Debug.Log("頁面加載完成: " + url);
    }

    // 當發生錯誤時觸發
    protected override void OnError(string message, string url)
    {
        Debug.LogError($"瀏覽器錯誤: {message} 於 {url}");
    }
}
```

### 開啟網址
初始化瀏覽器並使用預設設定開啟網址。

```csharp
using TedLiou.NativeBrowser;

public void OpenGoogle()
{
    // 初始化原生橋接器
    NativeBrowser.Initialize();
    // 開啟網址
    NativeBrowser.Open("https://www.google.com");
}
```

## WebView 功能

### JavaScript 執行與注入
您可以在當前頁面執行 JavaScript 或在頁面加載前注入代碼。

```csharp
// 執行 JavaScript 並接收結果
NativeBrowser.ExecuteJavaScript("document.title", (requestId, result) => {
    Debug.Log("頁面標題: " + result);
});

// 注入 JavaScript
NativeBrowser.InjectJavaScript("window.MyApp = { version: '1.0' };");
```

### PostMessage 通訊
網頁端可以使用 `window.NativeBrowser.postMessage(jsonString)` 與 Unity 通訊。

```javascript
// 網頁端
window.NativeBrowser.postMessage(JSON.stringify({ type: "LOGIN_SUCCESS", token: "xyz123" }));
```

```csharp
// Unity 端 (在您的回調接收器中)
protected override void OnPostMessage(string message)
{
    var data = JsonUtility.FromJson<MyMessageData>(message);
    Debug.Log("收到 Token: " + data.token);
}
```

### 尺寸與對齊方式
WebView 支持相對於螢幕的比例尺寸（0.0 到 1.0）。

```csharp
var config = new BrowserConfig
{
    url = "https://example.com",
    width = 0.8f,
    height = 0.6f,
    alignment = Alignment.CENTER,
    closeOnTapOutside = true // 點擊背景時關閉
};
NativeBrowser.Open(BrowserType.WebView, config);
```

### 深層連結攔截
攔截特定的網址格式並在 Unity 中處理。

```csharp
var config = new BrowserConfig
{
    url = "https://example.com",
    deepLinkPatterns = new List<string> { "myapp://process/.*" },
    closeOnDeepLink = true // 匹配到深層連結後關閉瀏覽器
};
NativeBrowser.Open(BrowserType.WebView, config);

// 在您的回調接收器中
protected override void OnDeepLink(string url)
{
    Debug.Log("攔截到深層連結: " + url);
}
```

## Custom Tabs

Custom Tabs 提供由 Chrome 驅動的優化瀏覽器體驗，外觀與應用程式更一致。

```csharp
var config = new BrowserConfig
{
    url = "https://example.com"
};
NativeBrowser.Open(BrowserType.CustomTab, config);
```

## 系統瀏覽器

開啟裝置預設的系統瀏覽器。這會讓使用者離開您的應用程式。

```csharp
NativeBrowser.Open(BrowserType.SystemBrowser, "https://example.com");
```

## 配置參數

`BrowserConfig` 類別允許對瀏覽器實例進行詳細控制。

| 選項 | 類型 | 預設值 | 說明 |
|--------|------|---------|-------------|
| `url` | `string` | `""` | 要開啟的網址。 |
| `width` | `float` | `1.0f` | 寬度比例 (0.0 到 1.0)。 |
| `height` | `float` | `1.0f` | 高度比例 (0.0 到 1.0)。 |
| `alignment` | `Alignment` | `CENTER` | WebView 的對齊位置。 |
| `closeOnTapOutside` | `bool` | `false` | 點擊外部背景時是否關閉 WebView。 |
| `deepLinkPatterns` | `List<string>` | `null` | 用於攔截深層連結的正則表達式列表。 |
| `closeOnDeepLink` | `bool` | `true` | 匹配到深層連結時是否自動關閉瀏覽器。 |
| `enableJavaScript` | `bool` | `true` | 是否啟用 JavaScript。 |
| `userAgent` | `string` | `""` | 自定義 User-Agent 字串。 |

## 錯誤處理

諸如 `OnError` 之類的事件在發生問題時提供詳細資訊。常見錯誤包括網絡失敗、無效網址或 JavaScript 執行逾時。請在回調接收器中使用提供的 `BrowserErrorEvent` 類別來解析錯誤詳情。

## 線程模型

NativeBrowser 使用 `AndroidJavaClass` 作為 Unity 與 Android 之間的橋接。
1. 從 Unity C# 端發出的呼叫發生在 Unity 主線程。
2. 橋接器將這些呼叫傳遞給 Android 的 `BrowserManager`。
3. 所有與 UI 相關的操作（建立 WebView、將其加入佈局）都會自動透過 `activity.runOnUiThread` 移動到 Android UI 線程執行。
4. 從 Android 回傳給 Unity 的回調使用 `UnitySendMessage`，這能確保回調在 Unity 主線程觸發。

## ProGuard 與代碼裁減

本函式庫在 `.aar` 中包含了 consumer ProGuard 規則，以保護橋接類別不被混淆或刪除。如果您使用 IL2CPP 並設定了高等級的代碼裁減，請確保在 `NativeBrowserCallbackReceiver` 的實作中套用 `[Preserve]` 屬性，或使用 `link.xml` 檔案來防止回調方法被裁減。
