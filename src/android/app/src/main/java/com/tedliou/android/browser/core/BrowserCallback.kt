package com.tedliou.android.browser.core

/**
 * Callback interface for browser lifecycle and interaction events.
 *
 * Implementations receive notifications for page events, user interactions, and errors.
 */
interface BrowserCallback {

    /**
     * Called when a page starts loading.
     *
     * @param url The URL being loaded
     */
    fun onPageStarted(url: String)

    /**
     * Called when a page finishes loading.
     *
     * @param url The final URL after all redirects
     */
    fun onPageFinished(url: String)

    /**
     * Called when an error occurs during browser operation.
     *
     * @param exception [BrowserException] with error details
     */
    fun onError(exception: BrowserException)

    /**
     * Called when JavaScript posts a message from web content.
     *
     * Message format is JSON: `{type: string, data: any}`
     *
     * @param message JSON string containing type and data
     */
    fun onPostMessage(message: String)

    /**
     * Called when JavaScript execution completes with a result.
     *
     * Matches async calls via requestId for proper request-response correlation.
     *
     * @param requestId Unique identifier for the JavaScript execution request
     * @param result JSON string containing execution result or null if execution failed
     */
    fun onJsResult(requestId: String, result: String?)

    /**
     * Called when a deep link URL is intercepted.
     *
     * Deep links are detected by matching [BrowserConfig.deepLinkPatterns].
     *
     * @param url The deep link URL
     */
    fun onDeepLink(url: String)

    /**
     * Called when the browser is closed.
     *
     * This occurs after [IBrowser.close] is called or user-initiated close.
     */
    fun onClosed()
}
