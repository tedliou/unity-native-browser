package com.tedliou.android.browser.core

/**
 * Enum representing the different browser implementations available.
 *
 * - [WEBVIEW]: Android WebView embedded in the application
 * - [CUSTOM_TAB]: Chrome Custom Tabs for enhanced browsing
 * - [SYSTEM_BROWSER]: System default browser launch
 */
enum class BrowserType {
    /**
     * Android WebView: embedded browser component with full control over UI and behavior
     */
    WEBVIEW,

    /**
     * Chrome Custom Tabs: enhanced browser experience with app integration
     */
    CUSTOM_TAB,

    /**
     * System Browser: delegates to device default browser
     */
    SYSTEM_BROWSER,
}
