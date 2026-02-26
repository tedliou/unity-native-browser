package com.tedliou.android.browser.core

/**
 * Sealed class for type-safe browser-related exceptions.
 *
 * Provides specific error types for different failure scenarios in browser operations.
 */
sealed class BrowserException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {

    /**
     * Thrown when browser initialization fails.
     *
     * @param message Error description
     * @param cause Underlying exception
     */
    class InitializationException(
        message: String,
        cause: Throwable? = null,
    ) : BrowserException(message, cause)

    /**
     * Thrown when a page fails to load.
     *
     * @param message Error description
     * @param url The URL that failed to load
     * @param cause Underlying exception
     */
    class PageLoadException(
        message: String,
        val url: String,
        cause: Throwable? = null,
    ) : BrowserException(message, cause)

    /**
     * Thrown when JavaScript execution fails.
     *
     * @param message Error description
     * @param requestId The JavaScript execution request ID
     * @param cause Underlying exception
     */
    class JavaScriptException(
        message: String,
        val requestId: String,
        cause: Throwable? = null,
    ) : BrowserException(message, cause)

    /**
     * Thrown when browser configuration is invalid.
     *
     * @param message Error description describing the configuration issue
     */
    class ConfigException(
        message: String,
    ) : BrowserException(message)

    /**
     * Thrown when browser is not available or not initialized.
     *
     * @param message Error description
     */
    class NotAvailableException(
        message: String,
    ) : BrowserException(message)

    /**
     * Thrown when a URL is invalid or has an unsupported scheme.
     *
     * @param url The invalid URL
     */
    class InvalidUrlException(
        val url: String,
    ) : BrowserException("Invalid URL: $url")
}
