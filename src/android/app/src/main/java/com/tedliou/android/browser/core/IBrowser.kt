package com.tedliou.android.browser.core

/**
 * Main interface for browser implementations.
 *
 * All browser types (WebView, Custom Tabs, System Browser) implement this interface
 * to provide a unified API for opening, managing, and closing browser instances.
 */
interface IBrowser {

    /**
     * Opens the browser with the provided configuration.
     *
     * @param config [BrowserConfig] containing URL, dimensions, alignment, and behavior settings
     * @param callback [BrowserCallback] for lifecycle and interaction events
     */
    fun open(config: BrowserConfig, callback: BrowserCallback)

    /**
     * Closes the currently open browser instance.
     *
     * Triggers [BrowserCallback.onClosed] after successful closure.
     */
    fun close()

    /**
     * Refreshes the currently loaded page.
     *
     * No-op if browser is not open. Applies only to WEBVIEW type.
     */
    fun refresh()

    /**
     * Checks if the browser instance is currently open.
     *
     * @return true if browser is open and displaying content, false otherwise
     */
    fun isOpen(): Boolean

    /**
     * Destroys the browser instance and releases all resources.
     *
     * After calling destroy(), the browser is no longer usable without calling open() again.
     */
    fun destroy()
}
