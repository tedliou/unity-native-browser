package com.tedliou.android.browser.core

/**
 * Configuration data class for browser instances.
 *
 * Specifies URL, layout, appearance, and behavior for browser operations.
 *
 * @property url The URL to load in the browser
 * @property width Screen width as percentage (0.0 to 1.0), where 1.0 = full width
 * @property height Screen height as percentage (0.0 to 1.0), where 1.0 = full height
 * @property alignment Position alignment on screen (e.g., CENTER, TOP_LEFT, BOTTOM_RIGHT)
 * @property closeOnTapOutside If true, browser closes when user taps outside the browser area
 * @property deepLinkPatterns List of URL regex patterns to intercept and handle as deep links
 * @property enableJavaScript If true, JavaScript execution is enabled in WebView
 * @property userAgent Custom User-Agent string; if empty, system default is used
 */
data class BrowserConfig(
    val url: String,
    val width: Float = 1.0f,
    val height: Float = 1.0f,
    val alignment: Alignment = Alignment.CENTER,
    val closeOnTapOutside: Boolean = false,
    val deepLinkPatterns: List<String> = emptyList(),
    val enableJavaScript: Boolean = true,
    val userAgent: String = "",
) {
    init {
        require(width in 0.0f..1.0f) { "width must be between 0.0 and 1.0" }
        require(height in 0.0f..1.0f) { "height must be between 0.0 and 1.0" }
        require(url.isNotBlank()) { "url cannot be blank" }
    }
}
