package com.tedliou.android.browser.webview

import android.app.Activity
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.tedliou.android.browser.core.BrowserCallback
import com.tedliou.android.browser.util.BrowserLogger
import java.lang.ref.WeakReference

/**
 * JavaScript 橋接器，負責 WebView 與原生層之間的雙向通訊。
 *
 * 透過 [JavascriptInterface] 接收來自網頁的訊息與日誌，
 * 並提供執行、注入 JavaScript 及傳送 postMessage 的功能。
 */
class JsBridge(
    activity: Activity,
    private val callback: BrowserCallback?,
) {
    private val activityRef = WeakReference(activity)

    /**
     * 接收來自網頁的 postMessage 訊息。
     *
     * 若訊息內容為空白則忽略，否則在主執行緒上通知 [BrowserCallback.onPostMessage]。
     *
     * @param message 網頁傳送的訊息字串
     */
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

    /**
     * 接收來自網頁的日誌訊息。
     *
     * 根據 [level] 將訊息路由至對應的 [BrowserLogger] 方法，
     * 並在輸出前對敏感資訊進行遮罩處理。
     *
     * @param level 日誌等級字串（如 "debug"、"info"、"warn"、"error"、"verbose"）
     * @param message 日誌訊息內容
     */
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

    /**
     * 將此橋接器作為 JavaScript 介面加入 WebView。
     *
     * 介面名稱為 [INTERFACE_NAME]，網頁可透過 `window.NativeBrowserBridge` 存取。
     *
     * @param webView 要加入介面的 WebView
     */
    fun addJavaScriptInterface(webView: WebView) {
        runOnUiThread {
            BrowserLogger.d(SUBTAG, "Adding JavaScript interface: $INTERFACE_NAME")
            webView.addJavascriptInterface(this, INTERFACE_NAME)
        }
    }

    /**
     * 在 WebView 中執行 JavaScript 並透過回呼回傳結果。
     *
     * 若腳本為空白，直接以 `null` 呼叫 [BrowserCallback.onJsResult]。
     *
     * @param webView 要執行腳本的 WebView
     * @param script 要執行的 JavaScript 字串
     * @param requestId 用於識別此次請求的 ID，結果將透過 [BrowserCallback.onJsResult] 回傳
     */
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

    /**
     * 將 JavaScript 腳本注入 WebView，不回傳執行結果。
     *
     * 若腳本為空白則直接返回。
     *
     * @param webView 要注入腳本的 WebView
     * @param script 要注入的 JavaScript 字串
     */
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

    /**
     * 注入 postMessage 橋接腳本，攔截網頁的 `window.postMessage` 呼叫，
     * 並將訊息轉發至原生層。
     *
     * @param webView 要注入橋接腳本的 WebView
     */
    fun injectPostMessageBridge(webView: WebView) {
        injectJavaScript(webView, POST_MESSAGE_BRIDGE_SCRIPT)
    }

    /**
     * 透過 JavaScript postMessage 從原生層傳送訊息至網頁內容。
     *
     * 訊息透過執行 `window.postMessage` 腳本傳遞至 WebView，
     * 傳送前會對特殊字元進行跳脫處理。
     *
     * @param webView 要傳送訊息的 WebView
     * @param message 要傳送的訊息字串
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
