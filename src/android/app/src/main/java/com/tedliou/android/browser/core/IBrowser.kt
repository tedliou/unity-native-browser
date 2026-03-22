package com.tedliou.android.browser.core

/**
 * 瀏覽器實作的主要介面。
 *
 * 所有瀏覽器類型（WebView、Custom Tabs、系統瀏覽器）均實作此介面，
 * 提供統一的 API 以開啟、管理及關閉瀏覽器實例。
 */
interface IBrowser {

    /**
     * 使用指定的設定開啟瀏覽器。
     *
     * @param config 包含網址、尺寸、對齊方式及行為設定的 [BrowserConfig]
     * @param callback 用於接收生命週期與互動事件的 [BrowserCallback]
     */
    fun open(config: BrowserConfig, callback: BrowserCallback)

    /**
     * 關閉目前開啟的瀏覽器實例。
     *
     * 成功關閉後會觸發 [BrowserCallback.onClosed]。
     */
    fun close()

    /**
     * 重新整理目前載入的頁面。
     *
     * 若瀏覽器未開啟則不執行任何動作。僅適用於 WEBVIEW 類型。
     */
    fun refresh()

    /**
     * 檢查瀏覽器實例目前是否已開啟。
     *
     * @return 若瀏覽器已開啟並顯示內容則回傳 true，否則回傳 false
     */
    fun isOpen(): Boolean

    /**
     * 銷毀瀏覽器實例並釋放所有資源。
     *
     * 呼叫 destroy() 後，必須再次呼叫 open() 才能重新使用瀏覽器。
     */
    fun destroy()
}
