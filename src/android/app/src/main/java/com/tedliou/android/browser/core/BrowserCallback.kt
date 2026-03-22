package com.tedliou.android.browser.core

/**
 * 瀏覽器生命週期與互動事件的回呼介面。
 *
 * 實作此介面可接收頁面事件、使用者互動及錯誤的通知。
 */
interface BrowserCallback {

    /**
     * 頁面開始載入時呼叫。
     *
     * @param url 正在載入的網址
     */
    fun onPageStarted(url: String)

    /**
     * 頁面載入完成時呼叫。
     *
     * @param url 所有重新導向後的最終網址
     */
    fun onPageFinished(url: String)

    /**
     * 瀏覽器操作發生錯誤時呼叫。
     *
     * @param exception 包含錯誤詳細資訊的 [BrowserException]
     */
    fun onError(exception: BrowserException)

    /**
     * JavaScript 從網頁內容發送訊息時呼叫。
     *
     * 為網頁內容透過 postMessage 發送的原始字串。
     *
     * @param message 來自網頁內容的原始字串
     */
    fun onPostMessage(message: String)

    /**
     * JavaScript 執行完成並回傳結果時呼叫。
     *
     * 透過 requestId 與非同步呼叫進行對應，以正確關聯請求與回應。
     *
     * @param requestId JavaScript 執行請求的唯一識別碼
     * @param result 包含執行結果的 JSON 字串，若執行失敗則為 null
     */
    fun onJsResult(requestId: String, result: String?)

    /**
     * 攔截到深層連結網址時呼叫。
     *
     * 深層連結透過比對 [BrowserConfig.deepLinkPatterns] 來偵測。
     *
     * @param url 深層連結網址
     */
    fun onDeepLink(url: String)

    /**
     * 瀏覽器關閉時呼叫。
     *
     * 在呼叫 [IBrowser.close] 或使用者主動關閉後觸發。
     */
    fun onClosed()
}
