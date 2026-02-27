package com.tedliou.android.browser.webview

import android.app.Activity
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.tedliou.android.browser.core.BrowserCallback
import com.tedliou.android.browser.util.BrowserLogger
import java.lang.ref.WeakReference

class JsBridge(
    activity: Activity,
    private val callback: BrowserCallback?,
) {
    private val activityRef = WeakReference(activity)

    @JavascriptInterface
    fun postMessage(message: String) {
        val trimmed = message.trim()
        if (trimmed.isEmpty()) {
            BrowserLogger.w(SUBTAG, "postMessage received empty payload")
            return
        }
        runOnUiThread {
            BrowserLogger.d(SUBTAG, "postMessage len=${trimmed.length}")
            callback?.onPostMessage(trimmed)
        }
    }

    @JavascriptInterface
    fun log(level: String, message: String) {
        val sanitized = sanitizeLogMessage(message)
        val preview = sanitized.take(LOG_PREVIEW_LENGTH)
        val normalized = level.trim().lowercase()
        val payload = "WebLog[$normalized] len=${sanitized.length}: $preview"
        when (normalized) {
            "debug", "d" -> BrowserLogger.d(SUBTAG, payload)
            "info", "i" -> BrowserLogger.i(SUBTAG, payload)
            "warn", "warning", "w" -> BrowserLogger.w(SUBTAG, payload)
            "error", "e" -> BrowserLogger.e(SUBTAG, payload)
            "verbose", "v" -> BrowserLogger.v(SUBTAG, payload)
            else -> BrowserLogger.d(SUBTAG, payload)
        }
    }

    fun addJavaScriptInterface(webView: WebView) {
        runOnUiThread {
            BrowserLogger.d(SUBTAG, "Adding JavaScript interface: $INTERFACE_NAME")
            webView.addJavascriptInterface(this, INTERFACE_NAME)
        }
    }

    fun executeJavaScript(webView: WebView, script: String, requestId: String) {
        val trimmed = script.trim()
        if (trimmed.isEmpty()) {
            BrowserLogger.w(SUBTAG, "executeJavaScript called with empty script")
            callback?.onJsResult(requestId, null)
            return
        }
        runOnUiThread {
            val preview = trimmed.replace(Regex("\\s+"), " ").take(SCRIPT_PREVIEW_LENGTH)
            BrowserLogger.d(
                SUBTAG,
                "Executing JavaScript (len=${trimmed.length}): $preview...",
            )
            try {
                webView.evaluateJavascript(trimmed) { result ->
                    callback?.onJsResult(requestId, result)
                }
            } catch (e: Exception) {
                BrowserLogger.e(SUBTAG, "JavaScript execution failed", e)
                callback?.onJsResult(requestId, null)
            }
        }
    }

    fun injectJavaScript(webView: WebView, script: String) {
        val trimmed = script.trim()
        if (trimmed.isEmpty()) {
            BrowserLogger.w(SUBTAG, "injectJavaScript called with empty script")
            return
        }
        runOnUiThread {
            val preview = trimmed.replace(Regex("\\s+"), " ").take(SCRIPT_PREVIEW_LENGTH)
            BrowserLogger.d(
                SUBTAG,
                "Injecting JavaScript (len=${trimmed.length}): $preview...",
            )
            try {
                webView.evaluateJavascript(trimmed, null)
            } catch (e: Exception) {
                BrowserLogger.e(SUBTAG, "JavaScript injection failed", e)
            }
        }
    }

    fun injectPostMessageBridge(webView: WebView) {
        injectJavaScript(webView, POST_MESSAGE_BRIDGE_SCRIPT)
    }

    /**
     * Send a message from native to web content via JavaScript postMessage.
     *
     * The message is delivered to the WebView by evaluating a script that calls
     * window.postMessage with the provided string.
     *
     * @param webView The WebView to send the message to
     * @param message The message string to send
     */
    fun sendPostMessage(webView: WebView, message: String) {
        val trimmed = message.trim()
        if (trimmed.isEmpty()) {
            BrowserLogger.w(SUBTAG, "sendPostMessage called with empty message")
            return
        }
        val escaped = trimmed
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\u2028", "\\u2028")
            .replace("\u2029", "\\u2029")
        injectJavaScript(webView, "window.postMessage('$escaped', '*');")
    }

    private fun runOnUiThread(block: () -> Unit) {
        val activity = activityRef.get()
        if (activity == null) {
            BrowserLogger.w(SUBTAG, "Activity reference lost; cannot execute UI operation")
            return
        }
        if (Looper.getMainLooper() == Looper.myLooper()) {
            block()
        } else {
            activity.runOnUiThread(block)
        }
    }

    private fun sanitizeLogMessage(message: String): String {
        val secretPattern = Regex(
            "(?i)(\\\"?(token|authorization|password|secret|api[_-]?key)\\\"?\\s*[:=]\\s*\\\")([^\\\"]+)(\\\")",
        )
        return message.replace(secretPattern) { match ->
            val prefix = match.groupValues[1]
            val suffix = match.groupValues[4]
            "$prefix***$suffix"
        }
    }

    private companion object {
        private const val SUBTAG = "JsBridge"
        private const val INTERFACE_NAME = "NativeBrowserBridge"
        private const val SCRIPT_PREVIEW_LENGTH = 50
        private const val LOG_PREVIEW_LENGTH = 200

        private val POST_MESSAGE_BRIDGE_SCRIPT = """
            (function() {
              var originalPostMessage = window.postMessage;
              window.postMessage = function(message, targetOrigin) {
                if (window.${INTERFACE_NAME}) {
                  window.${INTERFACE_NAME}.postMessage(typeof message === 'string' ? message : JSON.stringify(message));
                }
                return originalPostMessage.apply(this, arguments);
              };
            })();
        """.trimIndent()
    }
}
