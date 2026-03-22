package com.tedliou.android.browser.webview

import android.app.Activity
import android.os.Looper
import android.webkit.WebView
import com.tedliou.android.browser.core.BrowserCallback
import io.kotest.property.Arb
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.runBlocking
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

    /**
     * 屬性測試：對任意包含特殊字元（單引號、反斜線、換行符 `\n`、`\r`、`\u2028`、`\u2029`）的字串，
     * `sendPostMessage()` 生成的 JavaScript 腳本字串必須不包含未轉義的這些字元。
     *
     * **Validates: Requirements 6.2**
     */
    // Feature: android-native-browser-refactor, Property 11: JsBridge sendPostMessage 特殊字元轉義
    @Test
    fun property_sendPostMessage_special_chars_are_escaped() {
        val specialChars = listOf("'", "\\", "\n", "\r", "\u2028", "\u2029")
        runBlocking {
            checkAll(100, Arb.string()) { base ->
                // 確保訊息包含至少一個特殊字元
                val specialChar = specialChars[base.length % specialChars.size]
                val message = base + specialChar

                val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
                val callback = mockk<BrowserCallback>(relaxed = true)
                val jsBridge = JsBridge(activity, callback)
                val webView = mockk<WebView>(relaxed = true)

                val scriptSlot = slot<String>()
                every { webView.evaluateJavascript(capture(scriptSlot), null) } returns Unit

                jsBridge.sendPostMessage(webView, message)
                shadowOf(Looper.getMainLooper()).idle()

                // 若訊息非空白，腳本應已被呼叫
                if (message.isNotBlank()) {
                    val script = scriptSlot.captured
                    // 驗證腳本中不含任何原始（未轉義）的特殊字元
                    // 換行符、\r、\u2028、\u2029 轉義後以反斜線序列表示，原始字元不應出現
                    assertTrue("腳本不應含未轉義的換行符 \\n", !script.contains("\n"))
                    assertTrue("腳本不應含未轉義的換行符 \\r", !script.contains("\r"))
                    assertTrue("腳本不應含未轉義的行分隔符 \\u2028", !script.contains("\u2028"))
                    assertTrue("腳本不應含未轉義的段落分隔符 \\u2029", !script.contains("\u2029"))
                    // 對於單引號：腳本格式為 window.postMessage('...', '*');
                    // 擷取內層字串字面值（去除外層包裹結構）
                    val innerContent = script
                        .removePrefix("window.postMessage('")
                        .removeSuffix("', '*');")
                    // 內層中每個 ' 都必須緊接在 \ 之後（即為 \' 轉義序列）
                    // 使用 Regex 找出未被 \ 轉義的裸露單引號
                    val unescapedQuote = Regex("(?<!\\\\)'")
                    assertTrue(
                        "腳本字串字面值中不應含未轉義的單引號，內容：$innerContent",
                        !unescapedQuote.containsMatchIn(innerContent),
                    )
                }
            }
        }
    }

    /**
     * 屬性測試：對任意純空白字串，`postMessage()` 不得呼叫 `BrowserCallback.onPostMessage()`，
     * 且不得拋出例外。
     *
     * **Validates: Requirements 6.1**
     */
    // Feature: android-native-browser-refactor, Property 10: JsBridge 空白訊息被拒絕
    @Test
    fun property_postMessage_blank_message_never_invokes_callback() {
        runBlocking {
            checkAll(100, Arb.string().filter { it.isBlank() }) { blankMessage ->
                val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
                val callback = mockk<BrowserCallback>(relaxed = true)
                val jsBridge = JsBridge(activity, callback)

                // 不得拋出例外
                jsBridge.postMessage(blankMessage)
                shadowOf(Looper.getMainLooper()).idle()

                // 不得呼叫 onPostMessage
                verify(exactly = 0) { callback.onPostMessage(any()) }
            }
        }
    }
}
