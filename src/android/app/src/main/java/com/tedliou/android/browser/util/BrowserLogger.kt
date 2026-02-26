package com.tedliou.android.browser.util

import android.util.Log
import java.net.URL

/**
 * Centralized logging utility for NativeBrowser plugin.
 *
 * Provides structured logging with consistent tag formatting ([NativeBrowser:{subtag}])
 * and includes URL sanitization to prevent logging sensitive query parameters.
 *
 * All logging levels (debug, info, warning, error, verbose) are routed through
 * `android.util.Log` with a unified base tag. The companion object provides a
 * configurable [isDebugEnabled] flag to control logging output.
 *
 * Usage:
 * ```kotlin
 * BrowserLogger.d("WebView", "Loading URL")
 * BrowserLogger.i("CustomTab", "User tapped tab")
 * BrowserLogger.e("Bridge", "Failed to execute JS", throwable)
 * ```
 *
 * @see android.util.Log
 */
object BrowserLogger {
    private const val BASE_TAG = "NativeBrowser"

    /**
     * Global debug flag. When false, all logging is suppressed.
     * Default: true (logging enabled)
     *
     * Can be configured at runtime to control verbosity:
     * ```kotlin
     * BrowserLogger.isDebugEnabled = BuildConfig.DEBUG
     * ```
     */
    var isDebugEnabled: Boolean = true

    /**
     * Logs a debug-level message with structured tag.
     *
     * Tag format: `NativeBrowser:{subtag}` (e.g., `NativeBrowser:WebView`)
     *
     * @param subtag Component identifier (WebView, CustomTab, Bridge, etc.)
     * @param msg Message text. Strings containing URLs are sanitized automatically.
     */
    fun d(subtag: String, msg: String) {
        if (isDebugEnabled) {
            Log.d(formatTag(subtag), sanitizeMessage(msg))
        }
    }

    /**
     * Logs an info-level message with structured tag.
     *
     * Tag format: `NativeBrowser:{subtag}` (e.g., `NativeBrowser:CustomTab`)
     *
     * @param subtag Component identifier (WebView, CustomTab, Bridge, etc.)
     * @param msg Message text. Strings containing URLs are sanitized automatically.
     */
    fun i(subtag: String, msg: String) {
        if (isDebugEnabled) {
            Log.i(formatTag(subtag), sanitizeMessage(msg))
        }
    }

    /**
     * Logs a warning-level message with structured tag.
     *
     * Tag format: `NativeBrowser:{subtag}` (e.g., `NativeBrowser:System`)
     *
     * @param subtag Component identifier (WebView, CustomTab, Bridge, etc.)
     * @param msg Message text. Strings containing URLs are sanitized automatically.
     */
    fun w(subtag: String, msg: String) {
        if (isDebugEnabled) {
            Log.w(formatTag(subtag), sanitizeMessage(msg))
        }
    }

    /**
     * Logs an error-level message with optional exception stack trace.
     *
     * Tag format: `NativeBrowser:{subtag}` (e.g., `NativeBrowser:Bridge`)
     *
     * @param subtag Component identifier (WebView, CustomTab, Bridge, etc.)
     * @param msg Message text. Strings containing URLs are sanitized automatically.
     * @param throwable Optional exception to log with full stack trace. Default: null
     */
    fun e(subtag: String, msg: String, throwable: Throwable? = null) {
        if (isDebugEnabled) {
            if (throwable != null) {
                Log.e(formatTag(subtag), sanitizeMessage(msg), throwable)
            } else {
                Log.e(formatTag(subtag), sanitizeMessage(msg))
            }
        }
    }

    /**
     * Logs a verbose-level message with structured tag.
     *
     * Tag format: `NativeBrowser:{subtag}` (e.g., `NativeBrowser:Manager`)
     *
     * @param subtag Component identifier (WebView, CustomTab, Bridge, etc.)
     * @param msg Message text. Strings containing URLs are sanitized automatically.
     */
    fun v(subtag: String, msg: String) {
        if (isDebugEnabled) {
            Log.v(formatTag(subtag), sanitizeMessage(msg))
        }
    }

    /**
     * Measures execution time of a code block and logs duration.
     *
     * Useful for performance monitoring of critical paths. Logs at debug level
     * with format: `{operation} took {duration}ms`
     *
     * Example:
     * ```kotlin
     * val result = BrowserLogger.measureTime("WebView", "PageLoad") {
     *     loadUrl("https://example.com")
     * }
     * ```
     *
     * @param subtag Component identifier (WebView, CustomTab, Bridge, etc.)
     * @param operation Operation name for log message
     * @param block Lambda containing code to measure
     * @return Result of the block execution
     */
    inline fun <T> measureTime(subtag: String, operation: String, block: () -> T): T {
        val start = System.currentTimeMillis()
        return block().also { result ->
            val duration = System.currentTimeMillis() - start
            d(subtag, "$operation took ${duration}ms")
        }
    }

    /**
     * Formats a tag string to follow the NativeBrowser pattern.
     *
     * @param subtag Component identifier
     * @return Formatted tag: `NativeBrowser:{subtag}`
     */
    private fun formatTag(subtag: String): String {
        return "$BASE_TAG:$subtag"
    }

    /**
     * Sanitizes messages to prevent logging sensitive data.
     *
     * Currently sanitizes URLs by stripping query parameters to prevent
     * logging of sensitive tokens, API keys, or authentication data.
     *
     * Example: `https://example.com/path?token=secret&other=data`
     *          → logs as `https://example.com/path`
     *
     * @param msg Message containing potential URLs
     * @return Sanitized message with query parameters removed
     */
    private fun sanitizeMessage(msg: String): String {
        // Simple URL detection and sanitization: strip query params
        return msg.replace(Regex("""https?://[^\s?]+\?[^\s]*""")) { matchResult ->
            val url = matchResult.value
            try {
                val parsed = URL(url.substringBefore(" "))
                "${parsed.protocol}://${parsed.host}${parsed.path}"
            } catch (e: Exception) {
                // If URL parsing fails, return sanitized version by stripping after ?
                url.substringBefore("?")
            }
        }
    }
}
