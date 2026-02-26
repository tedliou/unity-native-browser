package com.tedliou.android.browser.webview

import android.app.Activity
import android.os.Looper
import android.webkit.WebView
import com.tedliou.android.browser.core.BrowserCallback
import com.tedliou.android.browser.core.BrowserConfig
import io.mockk.mockk
import io.mockk.verify
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowWebView
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

@RunWith(RobolectricTestRunner::class)
class WebViewBrowserTest {
    @Test
    fun test_open_creates_webview_and_attaches() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val browser = WebViewBrowser(activity)
        val callback = mockk<BrowserCallback>(relaxed = true)
        val config = BrowserConfig(url = "https://example.com")

        browser.open(config, callback)
        shadowOf(Looper.getMainLooper()).idle()

        assertIsOpenOnUiThread(activity, browser, expected = true)
    }

    @Test
    fun test_close_removes_webview() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val browser = WebViewBrowser(activity)
        val callback = mockk<BrowserCallback>(relaxed = true)
        val config = BrowserConfig(url = "https://example.com")

        browser.open(config, callback)
        shadowOf(Looper.getMainLooper()).idle()

        browser.close()
        shadowOf(Looper.getMainLooper()).idle()

        assertIsOpenOnUiThread(activity, browser, expected = false)
    }

    @Test
    fun test_refresh_calls_webview_reload() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val browser = WebViewBrowser(activity)
        val callback = mockk<BrowserCallback>(relaxed = true)
        val config = BrowserConfig(url = "https://example.com")

        browser.open(config, callback)
        shadowOf(Looper.getMainLooper()).idle()

        val mockWebView = mockk<WebView>(relaxed = true)
        setWebView(browser, mockWebView)

        browser.refresh()
        shadowOf(Looper.getMainLooper()).idle()

        verify { mockWebView.reload() }
    }

    @Test
    fun test_open_invokes_callback_onPageStarted() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val browser = WebViewBrowser(activity)
        val callback = mockk<BrowserCallback>(relaxed = true)
        val config = BrowserConfig(url = "https://example.com")

        browser.open(config, callback)
        shadowOf(Looper.getMainLooper()).idle()

        val webView = requireNotNull(getWebView(browser))
        val shadowWebView = shadowOf(webView) as ShadowWebView
        val client = requireNotNull(shadowWebView.webViewClient)

        client.onPageStarted(webView, "https://example.com", null)

        verify { callback.onPageStarted("https://example.com") }
    }

    @Test
    fun test_destroy_cleans_up_resources() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val browser = WebViewBrowser(activity)
        val callback = mockk<BrowserCallback>(relaxed = true)
        val config = BrowserConfig(url = "https://example.com")

        browser.open(config, callback)
        shadowOf(Looper.getMainLooper()).idle()

        browser.destroy()
        shadowOf(Looper.getMainLooper()).idle()

        assertIsOpenOnUiThread(activity, browser, expected = false)
    }

    private fun getWebView(browser: WebViewBrowser): WebView? {
        val field = WebViewBrowser::class.java.getDeclaredField("webView")
        field.isAccessible = true
        return field.get(browser) as? WebView
    }

    private fun setWebView(browser: WebViewBrowser, webView: WebView?) {
        val field = WebViewBrowser::class.java.getDeclaredField("webView")
        field.isAccessible = true
        field.set(browser, webView)
    }

    private fun assertIsOpenOnUiThread(activity: Activity, browser: WebViewBrowser, expected: Boolean) {
        val result = AtomicBoolean(false)
        activity.runOnUiThread { result.set(browser.isOpen()) }
        shadowOf(Looper.getMainLooper()).idle()
        if (expected) {
            assertTrue(result.get())
        } else {
            assertFalse(result.get())
        }
    }
}
