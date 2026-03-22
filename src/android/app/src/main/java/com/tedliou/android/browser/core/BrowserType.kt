package com.tedliou.android.browser.core

/**
 * 代表可用瀏覽器實作的列舉。
 *
 * - [WEBVIEW]：嵌入於應用程式中的 Android WebView
 * - [CUSTOM_TAB]：提供增強瀏覽體驗的 Chrome Custom Tabs
 * - [SYSTEM_BROWSER]：啟動系統預設瀏覽器
 */
enum class BrowserType {
    /**
     * Android WebView：嵌入式瀏覽器元件，可完整控制 UI 與行為
     */
    WEBVIEW,

    /**
     * Chrome Custom Tabs：與應用程式整合的增強瀏覽體驗
     */
    CUSTOM_TAB,

    /**
     * 系統瀏覽器：委派給裝置預設瀏覽器處理
     */
    SYSTEM_BROWSER,
}
