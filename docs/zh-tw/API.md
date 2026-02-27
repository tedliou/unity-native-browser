# API 參考

`TedLiou.NativeBrowser` 命名空間的詳細 API 說明。

## 目錄

- [NativeBrowser 類別](#nativebrowser-類別)
- [BrowserConfig 類別](#browserconfig-類別)
- [BrowserType 列舉](#browsertype-列舉)
- [Alignment 列舉](#alignment-列舉)
- [事件類別](#事件類別)
- [PostMessage 協議](#postmessage-協議)

## NativeBrowser 類別

用於控制瀏覽器實例的靜態類別。

### 屬性

| 屬性 | 類型 | 說明 |
|----------|------|-------------|
| `IsOpen` | `bool` | 如果當前有任何瀏覽器 (WebView 或 Custom Tab) 開啟中，回傳 true。 |

### 方法

| 方法 | 說明 |
|--------|-------------|
| `Initialize()` | 初始化原生橋接器。請在呼叫其他方法前呼叫此方法。 |
| `Open(string url)` | 使用預設瀏覽器 (WebView) 與設定開啟網址。 |
| `Open(BrowserType type, string url)` | 使用特定的瀏覽器類型開啟網址。 |
| `Open(BrowserType type, BrowserConfig config)` | 使用指定的設定開啟網址。 |
| `Close()` | 關閉當前的瀏覽器實例。 |
| `Refresh()` | 重新整理當前的 WebView 頁面。對其他類型的瀏覽器無效。 |
| `ExecuteJavaScript(string script, Action<string, string> callback = null)` | 在 WebView 中執行 JavaScript 並透過回調函數回傳結果。 |
| `InjectJavaScript(string script)` | 將 JavaScript 注入到 WebView 的全域作用域。 |
| `SendPostMessage(string message)` | 透過 JavaScript postMessage 向網頁內容發送訊息。僅限 WebView。 |

## BrowserConfig 類別

用於建立瀏覽器實例的組態設定。

### 欄位

| 欄位 | 類型 | 預設值 | 說明 |
|-------|------|---------|-------------|
| `url` | `string` | `""` | 要導覽的網址。 |
| `width` | `float` | `1.0f` | WebView 的寬度比例 (0.0 到 1.0)。 |
| `height` | `float` | `1.0f` | WebView 的高度比例 (0.0 到 1.0)。 |
| `alignment` | `Alignment` | `CENTER` | 瀏覽器在螢幕上的對齊位置。 |
| `closeOnTapOutside` | `bool` | `false` | 點擊外部背景區域時是否關閉瀏覽器。 |
| `deepLinkPatterns` | `List<string>` | `null` | 網址導覽前用於攔截的 regex 模式列表。 |
| `closeOnDeepLink` | `bool` | `true` | 當攔截到深層連結時，是否自動關閉瀏覽器。 |
| `enableJavaScript` | `bool` | `true` | 啟用或停用 WebView 中的 JavaScript。 |
| `userAgent` | `string` | `""` | 覆寫預設的瀏覽器 User-Agent。 |

## BrowserType 列舉

支援的瀏覽器類型。

| 成員 | 數值 | 說明 |
|--------|-------|-------------|
| `WebView` | `0` | 整合在應用程式內部的瀏覽器視圖。 |
| `CustomTab` | `1` | Android Custom Tab (Chrome) 驅動的瀏覽器。 |
| `SystemBrowser` | `2` | 外部系統瀏覽器。 |

## Alignment 列舉

WebView 在螢幕區域內的對齊選項。

| 成員 | 說明 |
|--------|-------------|
| `CENTER` | 水平與垂直居中。 |
| `LEFT` | 靠左對齊，垂直居中。 |
| `RIGHT` | 靠右對齊，垂直居中。 |
| `TOP` | 靠頂對齊，水平居中。 |
| `BOTTOM` | 靠底對齊，水平居中。 |
| `TOP_LEFT` | 對齊左上角。 |
| `TOP_RIGHT` | 對齊右上角。 |
| `BOTTOM_LEFT` | 對齊左下角。 |
| `BOTTOM_RIGHT` | 對齊右下角。 |

## 事件類別

由瀏覽器觸發並傳遞給 `NativeBrowserCallbackReceiver` 的事件。

### PageStartedEvent
- `string url`: 正在加載的網址。

### PageFinishedEvent
- `string url`: 已完成加載的網址。

### BrowserErrorEvent
- `string message`: 錯誤說明。
- `string url`: 發生錯誤的網址。

### PostMessageEvent
- `string message`: 從 JavaScript 接收到的訊息字串。

### JsResultEvent
- `string requestId`: JavaScript 執行呼叫的唯一識別碼。
- `string result`: 腳本執行的原始結果。

### DeepLinkEvent
- `string url`: 被攔截的網址。

## PostMessage 協議

### 網頁 → Unity（接收訊息）

網頁端可以向 Unity 發送任何非空字串訊息。支援以下兩種呼叫路徑：

1. **透過橋接腳本攔截**：使用標準的 `window.postMessage`。橋接器會自動攔截發送至視窗的訊息。
   ```javascript
   window.postMessage("Hello from Web", "*");
   // 也可以發送 JSON 字串
   window.postMessage(JSON.stringify({ type: "LOGIN", token: "abc" }), "*");
   ```
2. **直接呼叫橋接介面**：使用 `window.NativeBrowserBridge.postMessage` 直接與原生層通訊。
   ```javascript
   window.NativeBrowserBridge.postMessage("Direct message from Web");
   ```
Unity 端透過 `NativeBrowserCallbackReceiver` 中的 `OnPostMessage` 事件接收原始字串：

```csharp
protected override void OnPostMessage(string message)
{
    Debug.Log("收到來自網頁的訊息: " + message);
    // 如果是 JSON，可使用 JsonUtility 解析
    // var data = JsonUtility.FromJson<MyData>(message);
}
```

### Unity → 網頁（發送訊息）

Unity 可以使用 `NativeBrowser.SendPostMessage(message)` 向網頁發送訊息。這會在網頁端的 `window` 物件上觸發 `message` 事件。

Unity 端發送：
```csharp
NativeBrowser.SendPostMessage("Hello from Unity");
```
網頁端監聽：
```javascript
window.addEventListener('message', (event) => {
    console.log("收到來自 Unity 的訊息:", event.data);
});
```
