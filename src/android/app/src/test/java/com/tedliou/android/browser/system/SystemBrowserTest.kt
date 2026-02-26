package com.tedliou.android.browser.system

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Looper
import com.tedliou.android.browser.core.BrowserCallback
import com.tedliou.android.browser.core.BrowserConfig
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowActivity

@RunWith(RobolectricTestRunner::class)
class SystemBrowserTest {
    @Test
    fun test_open_creates_correct_intent_with_ACTION_VIEW() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val browser = SystemBrowser(activity)
        val callback = mockk<BrowserCallback>(relaxed = true)
        val config = BrowserConfig(url = "https://example.com")

        browser.open(config, callback)
        shadowOf(Looper.getMainLooper()).idle()

        val shadowActivity = shadowOf(activity)
        val intent = shadowActivity.nextStartedActivity

        assert(intent.action == Intent.ACTION_VIEW)
        assert(intent.data == Uri.parse("https://example.com"))
    }

    @Test
    fun test_open_with_http_url_succeeds() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val browser = SystemBrowser(activity)
        val callback = mockk<BrowserCallback>(relaxed = true)
        val config = BrowserConfig(url = "http://example.com")

        browser.open(config, callback)
        shadowOf(Looper.getMainLooper()).idle()

        verify { callback.onClosed() }
    }

    @Test
    fun test_open_with_https_url_succeeds() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val browser = SystemBrowser(activity)
        val callback = mockk<BrowserCallback>(relaxed = true)
        val config = BrowserConfig(url = "https://example.com")

        browser.open(config, callback)
        shadowOf(Looper.getMainLooper()).idle()

        verify { callback.onClosed() }
    }

    @Test
    fun test_open_with_invalid_url_scheme_invokes_onError() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val browser = SystemBrowser(activity)
        val callback = mockk<BrowserCallback>(relaxed = true)
        val config = BrowserConfig(url = "ftp://example.com")

        browser.open(config, callback)
        shadowOf(Looper.getMainLooper()).idle()

        verify { callback.onError(any()) }
        verify(exactly = 0) { callback.onClosed() }
    }

    @Test
    fun test_open_with_malformed_url_invokes_onError() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val browser = SystemBrowser(activity)
        val callback = mockk<BrowserCallback>(relaxed = true)
        val config = BrowserConfig(url = "not-a-url")

        browser.open(config, callback)
        shadowOf(Looper.getMainLooper()).idle()

        verify { callback.onError(any()) }
    }

    @Test
    fun test_close_logs_warning_and_does_not_throw() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val browser = SystemBrowser(activity)

        // Should not throw, just log warning
        browser.close()
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun test_refresh_logs_warning_and_does_not_throw() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val browser = SystemBrowser(activity)

        // Should not throw, just log warning
        browser.refresh()
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun test_isOpen_always_returns_false() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val browser = SystemBrowser(activity)

        assertFalse(browser.isOpen())

        val callback = mockk<BrowserCallback>(relaxed = true)
        val config = BrowserConfig(url = "https://example.com")
        browser.open(config, callback)
        shadowOf(Looper.getMainLooper()).idle()

        // Even after open, isOpen should return false (system browser is external)
        assertFalse(browser.isOpen())
    }

    @Test
    fun test_destroy_does_not_throw() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val browser = SystemBrowser(activity)

        // Should not throw
        browser.destroy()
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun test_open_invokes_onClosed_immediately_after_launch() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val browser = SystemBrowser(activity)
        val callback = mockk<BrowserCallback>(relaxed = true)
        val config = BrowserConfig(url = "https://example.com")

        browser.open(config, callback)
        shadowOf(Looper.getMainLooper()).idle()

        // System browser should immediately call onClosed since we can't control it
        verify { callback.onClosed() }
    }
}
