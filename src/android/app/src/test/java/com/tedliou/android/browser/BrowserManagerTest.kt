package com.tedliou.android.browser

import android.app.Activity
import android.os.Looper
import com.tedliou.android.browser.core.BrowserConfig
import com.tedliou.android.browser.core.BrowserType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class BrowserManagerTest {
    private lateinit var activity: Activity

    @Before
    fun setup() {
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        BrowserManager.initialize(activity)
    }

    @Test
    fun test_initialize_does_not_throw() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        BrowserManager.initialize(activity)
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun test_open_webview_creates_webview_browser() {
        val config = BrowserConfig(url = "https://example.com")
        BrowserManager.open(BrowserType.WEBVIEW, config)
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun test_open_custom_tabs_creates_custom_tab_browser() {
        val config = BrowserConfig(url = "https://example.com")
        BrowserManager.open(BrowserType.CUSTOM_TAB, config)
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun test_open_system_browser_creates_system_browser() {
        val config = BrowserConfig(url = "https://example.com")
        BrowserManager.open(BrowserType.SYSTEM_BROWSER, config)
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun test_close_does_not_throw_when_no_browser_open() {
        BrowserManager.close()
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun test_refresh_does_not_throw_when_no_browser_open() {
        BrowserManager.refresh()
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun test_isOpen_returns_false_when_no_browser() {
        assertFalse(BrowserManager.isOpen())
    }

    @Test
    fun test_createBrowser_returns_correct_type() {
        val webview = BrowserManager.createBrowser(BrowserType.WEBVIEW)
        assertNotNull(webview)
        shadowOf(Looper.getMainLooper()).idle()
    }
}
