package com.tedliou.android.browser.customtab

import android.app.Activity
import android.os.Looper
import androidx.browser.customtabs.CustomTabsClient
import com.tedliou.android.browser.core.BrowserCallback
import com.tedliou.android.browser.core.BrowserConfig
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class CustomTabBrowserTest {
    @Test
    fun test_open_with_no_custom_tabs_support_falls_back_to_system_browser() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val browser = CustomTabBrowser(activity)
        val callback = mockk<BrowserCallback>(relaxed = true)
        val config = BrowserConfig(url = "https://example.com")

        mockkStatic(CustomTabsClient::class)
        every { CustomTabsClient.getPackageName(any(), any()) } returns null

        browser.open(config, callback)
        shadowOf(Looper.getMainLooper()).idle()

        // System browser fallback should trigger onPageStarted
        verify(timeout = 1000) { callback.onPageStarted("https://example.com") }
    }

    @Test
    fun test_close_logs_warning_and_does_not_throw() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val browser = CustomTabBrowser(activity)

        // Should not throw, just log warning
        browser.close()
        shadowOf(Looper.getMainLooper()).idle()

        assertFalse(browser.isOpen())
    }

    @Test
    fun test_refresh_logs_warning_and_does_not_throw() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val browser = CustomTabBrowser(activity)

        // Should not throw, just log warning
        browser.refresh()
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun test_isOpen_returns_false_initially() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val browser = CustomTabBrowser(activity)

        assertFalse(browser.isOpen())
    }

    @Test
    fun test_destroy_without_open_does_not_throw() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val browser = CustomTabBrowser(activity)

        // Destroy without opening should not throw
        browser.destroy()
        shadowOf(Looper.getMainLooper()).idle()

        // Should complete without error
        assertFalse(browser.isOpen())
    }
}
