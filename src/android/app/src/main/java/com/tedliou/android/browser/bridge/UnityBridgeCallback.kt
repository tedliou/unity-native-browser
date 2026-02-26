package com.tedliou.android.browser.bridge

import com.tedliou.android.browser.core.BrowserCallback
import com.tedliou.android.browser.core.BrowserException
import org.json.JSONObject

/**
 * Implementation of [BrowserCallback] that forwards all events to Unity C# via [BrowserBridge].
 *
 * Each callback method:
 * 1. Serializes event data to JSON
 * 2. Converts method name to PascalCase (Kotlin onPageStarted → C# OnPageStarted)
 * 3. Sends via [BrowserBridge.sendToUnity]
 *
 * This class bridges the Android callback lifecycle to Unity's MonoBehaviour message system.
 */
class UnityBridgeCallback : BrowserCallback {

    /**
     * Page load started.
     *
     * Sends to C# method: OnPageStarted
     */
    override fun onPageStarted(url: String) {
        val json = JSONObject().apply {
            put("url", url)
        }
        BrowserBridge.sendToUnity("OnPageStarted", json.toString())
    }

    /**
     * Page load completed.
     *
     * Sends to C# method: OnPageFinished
     */
    override fun onPageFinished(url: String) {
        val json = JSONObject().apply {
            put("url", url)
        }
        BrowserBridge.sendToUnity("OnPageFinished", json.toString())
    }

    /**
     * Browser operation error occurred.
     *
     * Serializes exception type and context (e.g., URL for PageLoadException, requestId for JavaScriptException).
     *
     * Sends to C# method: OnError
     */
    override fun onError(exception: BrowserException) {
        val json = JSONObject().apply {
            put("type", exception::class.simpleName)
            put("message", exception.message)

            when (exception) {
                is BrowserException.PageLoadException -> put("url", exception.url)
                is BrowserException.JavaScriptException -> put("requestId", exception.requestId)
                is BrowserException.InvalidUrlException -> put("url", exception.url)
                is BrowserException.InitializationException,
                is BrowserException.ConfigException,
                is BrowserException.NotAvailableException -> {
                }
            }
        }
        BrowserBridge.sendToUnity("OnError", json.toString())
    }

    /**
     * JavaScript posted a message from web content.
     *
     * Message is already JSON-formatted "{type, data}".
     *
     * Sends to C# method: OnPostMessage
     */
    override fun onPostMessage(message: String) {
        val json = JSONObject().apply {
            put("message", message)
        }
        BrowserBridge.sendToUnity("OnPostMessage", json.toString())
    }

    /**
     * JavaScript execution completed with a result.
     *
     * Correlates with async JS calls via requestId.
     *
     * Sends to C# method: OnJsResult
     */
    override fun onJsResult(requestId: String, result: String?) {
        val json = JSONObject().apply {
            put("requestId", requestId)
            put("result", result)
        }
        BrowserBridge.sendToUnity("OnJsResult", json.toString())
    }

    /**
     * Deep link intercepted by the browser.
     *
     * Sends to C# method: OnDeepLink
     */
    override fun onDeepLink(url: String) {
        val json = JSONObject().apply {
            put("url", url)
        }
        BrowserBridge.sendToUnity("OnDeepLink", json.toString())
    }

    /**
     * Browser was closed.
     *
     * Sends to C# method: OnClosed
     */
    override fun onClosed() {
        val json = JSONObject()
        BrowserBridge.sendToUnity("OnClosed", json.toString())
    }
}
