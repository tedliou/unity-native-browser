package com.tedliou.android.browser.webview

import android.app.Activity
import android.os.Looper
import android.webkit.WebView
import com.tedliou.android.browser.core.BrowserCallback
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class JsBridgeTest {
    @Test
    fun test_postMessage_valid_json_invokes_callback() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val callback = mockk<BrowserCallback>(relaxed = true)
        val jsBridge = JsBridge(activity, callback)

        val validJson = """{"type":"test","data":"value"}"""
        jsBridge.postMessage(validJson)
        shadowOf(Looper.getMainLooper()).idle()

        verify { callback.onPostMessage(validJson) }
    }

    @Test
    fun test_postMessage_non_json_string_invokes_callback() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val callback = mockk<BrowserCallback>(relaxed = true)
        val jsBridge = JsBridge(activity, callback)

        val plainString = "hello world"
        jsBridge.postMessage(plainString)
        shadowOf(Looper.getMainLooper()).idle()

        verify { callback.onPostMessage(plainString) }
    }

    @Test
    fun test_postMessage_json_without_type_field_invokes_callback() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val callback = mockk<BrowserCallback>(relaxed = true)
        val jsBridge = JsBridge(activity, callback)

        val jsonWithoutType = """{"data":"value"}"""
        jsBridge.postMessage(jsonWithoutType)
        shadowOf(Looper.getMainLooper()).idle()

        verify { callback.onPostMessage(jsonWithoutType) }
    }

    @Test
    fun test_executeJavaScript_delegates_to_webview_evaluateJavascript() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val callback = mockk<BrowserCallback>(relaxed = true)
        val jsBridge = JsBridge(activity, callback)
        val webView = mockk<WebView>(relaxed = true)

        val script = "console.log('test');"
        val requestId = "req_123"

        jsBridge.executeJavaScript(webView, script, requestId)
        shadowOf(Looper.getMainLooper()).idle()

        verify { webView.evaluateJavascript(script, any()) }
    }

    @Test
    fun test_executeJavaScript_returns_result_via_callback() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val callback = mockk<BrowserCallback>(relaxed = true)
        val jsBridge = JsBridge(activity, callback)
        val webView = mockk<WebView>(relaxed = true)

        val callbackSlot = slot<android.webkit.ValueCallback<String>>()
        every { webView.evaluateJavascript(any(), capture(callbackSlot)) } answers {
            callbackSlot.captured.onReceiveValue("\"result_value\"")
        }

        val script = "document.title;"
        val requestId = "req_456"

        jsBridge.executeJavaScript(webView, script, requestId)
        shadowOf(Looper.getMainLooper()).idle()

        verify { callback.onJsResult(requestId, "\"result_value\"") }
    }

    @Test
    fun test_executeJavaScript_empty_script_returns_null() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val callback = mockk<BrowserCallback>(relaxed = true)
        val jsBridge = JsBridge(activity, callback)
        val webView = mockk<WebView>(relaxed = true)

        jsBridge.executeJavaScript(webView, "", "req_empty")
        shadowOf(Looper.getMainLooper()).idle()

        verify { callback.onJsResult("req_empty", null) }
        verify(exactly = 0) { webView.evaluateJavascript(any(), any()) }
    }

    @Test
    fun test_injectJavaScript_delegates_to_webview_evaluateJavascript() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val callback = mockk<BrowserCallback>(relaxed = true)
        val jsBridge = JsBridge(activity, callback)
        val webView = mockk<WebView>(relaxed = true)

        val script = "window.testFlag = true;"

        jsBridge.injectJavaScript(webView, script)
        shadowOf(Looper.getMainLooper()).idle()

        verify { webView.evaluateJavascript(script, null) }
    }

    @Test
    fun test_injectJavaScript_empty_script_does_nothing() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val callback = mockk<BrowserCallback>(relaxed = true)
        val jsBridge = JsBridge(activity, callback)
        val webView = mockk<WebView>(relaxed = true)

        jsBridge.injectJavaScript(webView, "")
        shadowOf(Looper.getMainLooper()).idle()

        verify(exactly = 0) { webView.evaluateJavascript(any(), any()) }
    }

    @Test
    fun test_addJavaScriptInterface_adds_bridge_to_webview() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val callback = mockk<BrowserCallback>(relaxed = true)
        val jsBridge = JsBridge(activity, callback)
        val webView = mockk<WebView>(relaxed = true)

        jsBridge.addJavaScriptInterface(webView)
        shadowOf(Looper.getMainLooper()).idle()

        verify { webView.addJavascriptInterface(jsBridge, "NativeBrowserBridge") }
    }

    @Test
    fun test_log_method_does_not_throw() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val callback = mockk<BrowserCallback>(relaxed = true)
        val jsBridge = JsBridge(activity, callback)

        // Just verify it doesn't throw
        jsBridge.log("info", "test message")
        jsBridge.log("error", "error message")
        jsBridge.log("debug", "debug message")
    }

    @Test
    fun test_sendPostMessage_delegates_to_injectJavaScript() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val callback = mockk<BrowserCallback>(relaxed = true)
        val jsBridge = JsBridge(activity, callback)
        val webView = mockk<WebView>(relaxed = true)

        jsBridge.sendPostMessage(webView, "hello from unity")
        shadowOf(Looper.getMainLooper()).idle()

        verify { webView.evaluateJavascript(match { it.contains("window.postMessage") && it.contains("hello from unity") }, null) }
    }

    @Test
    fun test_sendPostMessage_empty_does_nothing() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val callback = mockk<BrowserCallback>(relaxed = true)
        val jsBridge = JsBridge(activity, callback)
        val webView = mockk<WebView>(relaxed = true)

        jsBridge.sendPostMessage(webView, "")
        shadowOf(Looper.getMainLooper()).idle()

        verify(exactly = 0) { webView.evaluateJavascript(any(), any()) }
    }

    @Test
    fun test_sendPostMessage_escapes_special_characters() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val callback = mockk<BrowserCallback>(relaxed = true)
        val jsBridge = JsBridge(activity, callback)
        val webView = mockk<WebView>(relaxed = true)

        jsBridge.sendPostMessage(webView, "it's a \"test\"")
        shadowOf(Looper.getMainLooper()).idle()

        verify { webView.evaluateJavascript(match { it.contains("window.postMessage") && it.contains("it\\'s a") }, null) }
    }
}
