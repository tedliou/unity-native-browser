package com.tedliou.android.browser.util

import android.util.Log
import java.net.URL

/**
 * NativeBrowser 插件的集中式日誌工具。
 *
 * 提供統一格式的結構化日誌輸出：
 * `[NativeBrowser][<SubTag>] <操作描述> | <關鍵參數>`
 *
 * 範例：
 * ```
 * [NativeBrowser][Manager] open() 開始 | type=WEBVIEW url=https://example.com
 * [NativeBrowser][WebView] WebView 建立完成 | url=https://example.com width=0.9 height=0.8
 * [NativeBrowser][Bridge] sendToUnity() | method=OnPageStarted gameObject=NativeBrowserCallback
 * [NativeBrowser][JsBridge] executeJavaScript() | requestId=req_001 scriptLen=42
 * ```
 *
 * 所有日誌層級（verbose、debug、info、warning、error）均透過 `android.util.Log` 輸出。
 * 可透過 [isDebugEnabled] 旗標控制日誌輸出。
 * 訊息中的 URL 會自動移除查詢參數，避免記錄敏感資料。
 *
 * @see android.util.Log
 */
object BrowserLogger {
    /**
     * 日誌主標籤，固定為 `[NativeBrowser]`。
     */
    const val TAG = "[NativeBrowser]"

    /**
     * 全域除錯旗標。設為 `false` 時，所有日誌輸出將被抑制。
     * 預設值：`true`（啟用日誌）
     *
     * 可在執行期動態設定：
     * ```kotlin
     * BrowserLogger.isDebugEnabled = BuildConfig.DEBUG
     * ```
     */
    var isDebugEnabled: Boolean = true

    /**
     * 輸出 verbose 層級日誌。
     *
     * 日誌標籤格式：`[NativeBrowser][<subTag>]`
     *
     * @param subTag 子元件識別碼（例如：Manager、WebView、Bridge）
     * @param message 訊息內容，建議格式：`<操作描述> | <關鍵參數>`。包含 URL 的字串會自動移除查詢參數。
     */
    fun v(subTag: String, message: String) {
        if (isDebugEnabled) {
            Log.v(formatTag(subTag), sanitizeMessage(message))
        }
    }

    /**
     * 輸出 debug 層級日誌。
     *
     * 日誌標籤格式：`[NativeBrowser][<subTag>]`
     *
     * @param subTag 子元件識別碼（例如：Manager、WebView、Bridge）
     * @param message 訊息內容，建議格式：`<操作描述> | <關鍵參數>`。包含 URL 的字串會自動移除查詢參數。
     */
    fun d(subTag: String, message: String) {
        if (isDebugEnabled) {
            Log.d(formatTag(subTag), sanitizeMessage(message))
        }
    }

    /**
     * 輸出 info 層級日誌。
     *
     * 日誌標籤格式：`[NativeBrowser][<subTag>]`
     *
     * @param subTag 子元件識別碼（例如：Manager、WebView、Bridge）
     * @param message 訊息內容，建議格式：`<操作描述> | <關鍵參數>`。包含 URL 的字串會自動移除查詢參數。
     */
    fun i(subTag: String, message: String) {
        if (isDebugEnabled) {
            Log.i(formatTag(subTag), sanitizeMessage(message))
        }
    }

    /**
     * 輸出 warning 層級日誌。
     *
     * 日誌標籤格式：`[NativeBrowser][<subTag>]`
     *
     * @param subTag 子元件識別碼（例如：Manager、WebView、Bridge）
     * @param message 訊息內容，建議格式：`<操作描述> | <關鍵參數>`。包含 URL 的字串會自動移除查詢參數。
     */
    fun w(subTag: String, message: String) {
        if (isDebugEnabled) {
            Log.w(formatTag(subTag), sanitizeMessage(message))
        }
    }

    /**
     * 輸出 error 層級日誌，可附帶例外堆疊追蹤。
     *
     * 日誌標籤格式：`[NativeBrowser][<subTag>]`
     *
     * @param subTag 子元件識別碼（例如：Manager、WebView、Bridge）
     * @param message 訊息內容，建議格式：`<操作描述> | <關鍵參數>`。包含 URL 的字串會自動移除查詢參數。
     * @param throwable 可選的例外物件，附帶完整堆疊追蹤。預設為 `null`。
     */
    fun e(subTag: String, message: String, throwable: Throwable? = null) {
        if (isDebugEnabled) {
            if (throwable != null) {
                Log.e(formatTag(subTag), sanitizeMessage(message), throwable)
            } else {
                Log.e(formatTag(subTag), sanitizeMessage(message))
            }
        }
    }

    /**
     * 測量程式碼區塊的執行時間並以 debug 層級記錄耗時。
     *
     * 適用於關鍵路徑的效能監控，日誌格式：`<operation> took <duration>ms`
     *
     * 範例：
     * ```kotlin
     * val result = BrowserLogger.measureTime("WebView", "PageLoad") {
     *     loadUrl("https://example.com")
     * }
     * ```
     *
     * @param subTag 子元件識別碼（例如：Manager、WebView、Bridge）
     * @param operation 操作名稱，用於日誌訊息
     * @param block 要測量的程式碼區塊
     * @return 程式碼區塊的執行結果
     */
    inline fun <T> measureTime(subTag: String, operation: String, block: () -> T): T {
        val start = System.currentTimeMillis()
        return block().also {
            val duration = System.currentTimeMillis() - start
            d(subTag, "$operation took ${duration}ms")
        }
    }

    /**
     * 將子標籤格式化為完整的 Android log tag。
     *
     * 格式：`[NativeBrowser][<subTag>]`
     *
     * @param subTag 子元件識別碼
     * @return 格式化後的標籤字串
     */
    private fun formatTag(subTag: String): String {
        return "$TAG[$subTag]"
    }

    /**
     * 對訊息中的 URL 進行脫敏處理，移除查詢參數以避免記錄敏感資料。
     *
     * 範例：`https://example.com/path?token=secret&other=data`
     *      → 記錄為 `https://example.com/path`
     *
     * @param message 可能包含 URL 的訊息字串
     * @return 已移除 URL 查詢參數的訊息字串
     */
    private fun sanitizeMessage(message: String): String {
        return message.replace(Regex("""https?://[^\s?]+\?[^\s]*""")) { matchResult ->
            val url = matchResult.value
            try {
                val parsed = URL(url.substringBefore(" "))
                "${parsed.protocol}://${parsed.host}${parsed.path}"
            } catch (e: Exception) {
                url.substringBefore("?")
            }
        }
    }
}
