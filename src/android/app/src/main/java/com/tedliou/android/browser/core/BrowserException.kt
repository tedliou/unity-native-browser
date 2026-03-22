package com.tedliou.android.browser.core

/**
 * 類型安全的瀏覽器相關例外的密封類別。
 *
 * 針對瀏覽器操作中不同的失敗情境提供具體的錯誤類型。
 */
sealed class BrowserException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {

    /**
     * 瀏覽器初始化失敗時拋出。
     *
     * @param message 錯誤描述
     * @param cause 底層例外
     */
    class InitializationException(
        message: String,
        cause: Throwable? = null,
    ) : BrowserException(message, cause)

    /**
     * 頁面載入失敗時拋出。
     *
     * @param message 錯誤描述
     * @param url 載入失敗的網址
     * @param cause 底層例外
     */
    class PageLoadException(
        message: String,
        val url: String,
        cause: Throwable? = null,
    ) : BrowserException(message, cause)

    /**
     * JavaScript 執行失敗時拋出。
     *
     * @param message 錯誤描述
     * @param requestId JavaScript 執行請求的識別碼
     * @param cause 底層例外
     */
    class JavaScriptException(
        message: String,
        val requestId: String,
        cause: Throwable? = null,
    ) : BrowserException(message, cause)

    /**
     * 瀏覽器設定無效時拋出。
     *
     * @param message 描述設定問題的錯誤說明
     */
    class ConfigException(
        message: String,
    ) : BrowserException(message)

    /**
     * 瀏覽器不可用或尚未初始化時拋出。
     *
     * @param message 錯誤描述
     */
    class NotAvailableException(
        message: String,
    ) : BrowserException(message)

    /**
     * 網址無效或使用不支援的協定時拋出。
     *
     * @param url 無效的網址
     */
    class InvalidUrlException(
        val url: String,
    ) : BrowserException("Invalid URL: $url")
}
