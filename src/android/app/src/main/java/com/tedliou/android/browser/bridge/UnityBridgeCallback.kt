package com.tedliou.android.browser.bridge

import com.tedliou.android.browser.core.BrowserCallback
import com.tedliou.android.browser.core.BrowserException
import org.json.JSONObject

/**
 * [BrowserCallback] 的實作，透過 [BrowserBridge] 將所有事件轉發至 Unity C#。
 *
 * 每個回調方法：
 * 1. 將事件資料序列化為 JSON
 * 2. 將方法名稱轉換為 PascalCase（Kotlin onPageStarted → C# OnPageStarted）
 * 3. 透過 [BrowserBridge.sendToUnity] 發送
 *
 * 此類別將 Android 回調生命週期橋接至 Unity 的 MonoBehaviour 訊息系統。
 */
class UnityBridgeCallback : BrowserCallback {

    /**
     * 頁面開始載入。
     *
     * 發送至 C# 方法：OnPageStarted
     */
    override fun onPageStarted(url: String) {
        val json = JSONObject().apply {
            put("url", url)
        }
        BrowserBridge.sendToUnity("OnPageStarted", json.toString())
    }

    /**
     * 頁面載入完成。
     *
     * 發送至 C# 方法：OnPageFinished
     */
    override fun onPageFinished(url: String) {
        val json = JSONObject().apply {
            put("url", url)
        }
        BrowserBridge.sendToUnity("OnPageFinished", json.toString())
    }

    /**
     * 瀏覽器操作發生錯誤。
     *
     * 序列化例外類型與上下文（例如 PageLoadException 的 URL、JavaScriptException 的 requestId）。
     *
     * 發送至 C# 方法：OnError
     */
    override fun onError(exception: BrowserException) {
        val json = JSONObject().apply {
            put("type", exception::class.simpleName)
            put("message", exception.message)

            when (exception) {
                is BrowserException.PageLoadException -> put("url", exception.url)
                is BrowserException.JavaScriptException -> put("requestId", exception.requestId)
                is BrowserException.InvalidUrlException -> put("url", exception.url)
                is BrowserException.InitializationException,
                is BrowserException.ConfigException,
                is BrowserException.NotAvailableException -> {
                }
            }
        }
        BrowserBridge.sendToUnity("OnError", json.toString())
    }

    /**
     * JavaScript 從網頁內容發送了訊息。
     *
     * message 為網頁內容發送的原始字串。
     *
     * 發送至 C# 方法：OnPostMessage
     */
    override fun onPostMessage(message: String) {
        val json = JSONObject().apply {
            put("message", message)
        }
        BrowserBridge.sendToUnity("OnPostMessage", json.toString())
    }

    /**
     * JavaScript 執行完成並回傳結果。
     *
     * 透過 requestId 與非同步 JS 呼叫進行關聯。
     *
     * 發送至 C# 方法：OnJsResult
     */
    override fun onJsResult(requestId: String, result: String?) {
        val json = JSONObject().apply {
            put("requestId", requestId)
            put("result", result)
        }
        BrowserBridge.sendToUnity("OnJsResult", json.toString())
    }

    /**
     * 瀏覽器攔截到深層連結。
     *
     * 發送至 C# 方法：OnDeepLink
     */
    override fun onDeepLink(url: String) {
        val json = JSONObject().apply {
            put("url", url)
        }
        BrowserBridge.sendToUnity("OnDeepLink", json.toString())
    }

    /**
     * 瀏覽器已關閉。
     *
     * 發送至 C# 方法：OnClosed
     */
    override fun onClosed() {
        val json = JSONObject()
        BrowserBridge.sendToUnity("OnClosed", json.toString())
    }
}
