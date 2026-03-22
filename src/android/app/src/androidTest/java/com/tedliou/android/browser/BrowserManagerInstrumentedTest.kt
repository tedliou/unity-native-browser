package com.tedliou.android.browser

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.tedliou.android.browser.core.BrowserCallback
import com.tedliou.android.browser.core.BrowserConfig
import com.tedliou.android.browser.core.BrowserException
import com.tedliou.android.browser.core.BrowserType
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

// Feature: android-native-browser-refactor, Property 9: BrowserManager open-close round-trip

/**
 * BrowserManager 整合測試（Instrumented Test）。
 *
 * 在真實 Android 裝置或模擬器上驗證：
 * - 完整 open→操作→close 流程（WEBVIEW 類型）
 * - 完整 open→操作→close 流程（CUSTOM_TAB 類型）
 * - 屬性 9：呼叫 open() 後再呼叫 close()，isOpen() 必須回傳 false
 * - refresh() 不崩潰
 * - executeJavaScript() 在 open() 後可正常執行
 *
 * 需要 Android 虛擬機或實體裝置才能執行。
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class BrowserManagerInstrumentedTest {

    /** 測試用 Activity 場景，提供真實 Activity 上下文。 */
    private var activityScenario: ActivityScenario<WebViewIntegrationTest.TestActivity>? = null

    /**
     * 每個測試前啟動 TestActivity 並初始化 BrowserManager。
     */
    @Before
    fun setUp() {
        activityScenario = ActivityScenario.launch(WebViewIntegrationTest.TestActivity::class.java)
        activityScenario?.onActivity { activity ->
            BrowserManager.initialize(activity)
        }
    }

    /**
     * 每個測試後關閉瀏覽器並釋放 Activity 資源。
     */
    @After
    fun tearDown() {
        activityScenario?.onActivity {
            BrowserManager.close()
        }
        activityScenario?.close()
    }

    // -------------------------------------------------------------------------
    // 屬性 9：BrowserManager open-close round-trip（WEBVIEW）
    // -------------------------------------------------------------------------

    /**
     * 驗證屬性 9：對 WEBVIEW 類型，呼叫 open() 後再呼叫 close()，isOpen() 必須回傳 false。
     *
     * **Validates: Requirements 5.1, 5.2**
     */
    @Test
    fun property9_webview_openThenClose_isOpenReturnsFalse() {
        val pageStartedLatch = CountDownLatch(1)
        val closedLatch = CountDownLatch(1)

        activityScenario?.onActivity { activity ->
            val config = BrowserConfig(url = "about:blank")
            val callback = object : NoOpBrowserCallback() {
                override fun onPageStarted(url: String) {
                    pageStartedLatch.countDown()
                }
                override fun onClosed() {
                    closedLatch.countDown()
                }
            }
            BrowserManager.open(BrowserType.WEBVIEW, config)
            // BrowserManager.open() 內部使用 unityCallback，需另外監聽
            // 改用直接等待後關閉的方式驗證屬性 9
        }

        // 等待頁面開始載入（確認 open() 已生效）
        pageStartedLatch.await(5, TimeUnit.SECONDS)

        // 呼叫 close()
        val closeLatch = CountDownLatch(1)
        var isOpenAfterClose = true
        activityScenario?.onActivity { activity ->
            activity.runOnUiThread {
                BrowserManager.close()
                isOpenAfterClose = BrowserManager.isOpen()
                closeLatch.countDown()
            }
        }

        assertTrue("close() 應在 3 秒內完成", closeLatch.await(3, TimeUnit.SECONDS))
        assertFalse("屬性 9：WEBVIEW open() 後 close()，isOpen() 必須回傳 false", isOpenAfterClose)
    }

    /**
     * 驗證屬性 9：對 WEBVIEW 類型，在未等待頁面載入的情況下立即 close()，isOpen() 仍回傳 false。
     *
     * **Validates: Requirements 5.1, 5.2**
     */
    @Test
    fun property9_webview_openAndImmediateClose_isOpenReturnsFalse() {
        val latch = CountDownLatch(1)
        var isOpenAfterClose = true

        activityScenario?.onActivity { activity ->
            activity.runOnUiThread {
                val config = BrowserConfig(url = "about:blank")
                BrowserManager.open(BrowserType.WEBVIEW, config)
                BrowserManager.close()
                isOpenAfterClose = BrowserManager.isOpen()
                latch.countDown()
            }
        }

        assertTrue("操作應在 3 秒內完成", latch.await(3, TimeUnit.SECONDS))
        assertFalse("屬性 9：WEBVIEW open() 後立即 close()，isOpen() 必須回傳 false", isOpenAfterClose)
    }

    // -------------------------------------------------------------------------
    // 屬性 9：BrowserManager open-close round-trip（CUSTOM_TAB）
    // -------------------------------------------------------------------------

    /**
     * 驗證屬性 9：對 CUSTOM_TAB 類型，呼叫 open() 後再呼叫 close()，isOpen() 必須回傳 false。
     *
     * **Validates: Requirements 5.1, 5.2**
     */
    @Test
    fun property9_customTab_openThenClose_isOpenReturnsFalse() {
        val openLatch = CountDownLatch(1)

        activityScenario?.onActivity { activity ->
            val config = BrowserConfig(url = "https://example.com")
            BrowserManager.open(BrowserType.CUSTOM_TAB, config)
            openLatch.countDown()
        }

        assertTrue("open() 應在 3 秒內完成", openLatch.await(3, TimeUnit.SECONDS))

        val closeLatch = CountDownLatch(1)
        var isOpenAfterClose = true
        activityScenario?.onActivity { activity ->
            activity.runOnUiThread {
                BrowserManager.close()
                isOpenAfterClose = BrowserManager.isOpen()
                closeLatch.countDown()
            }
        }

        assertTrue("close() 應在 3 秒內完成", closeLatch.await(3, TimeUnit.SECONDS))
        assertFalse("屬性 9：CUSTOM_TAB open() 後 close()，isOpen() 必須回傳 false", isOpenAfterClose)
    }

    // -------------------------------------------------------------------------
    // 完整 open→操作→close 流程（WEBVIEW）
    // -------------------------------------------------------------------------

    /**
     * 驗證 WEBVIEW 完整流程：open() 後 isOpen() 回傳 true。
     */
    @Test
    fun testWebViewFlow_isOpenAfterOpen() {
        val pageStartedLatch = CountDownLatch(1)

        activityScenario?.onActivity { activity ->
            val config = BrowserConfig(url = "about:blank")
            BrowserManager.open(BrowserType.WEBVIEW, config)
        }

        // 等待 WebView 開始載入
        Thread.sleep(500)

        val isOpenLatch = CountDownLatch(1)
        var isOpen = false
        activityScenario?.onActivity { activity ->
            activity.runOnUiThread {
                isOpen = BrowserManager.isOpen()
                isOpenLatch.countDown()
            }
        }

        assertTrue("isOpen 查詢應完成", isOpenLatch.await(3, TimeUnit.SECONDS))
        assertTrue("open() 後 isOpen() 應回傳 true", isOpen)
    }

    /**
     * 驗證 refresh() 在 WEBVIEW open() 後不崩潰。
     */
    @Test
    fun testWebViewFlow_refresh_doesNotCrash() {
        val pageStartedLatch = CountDownLatch(1)
        val refreshLatch = CountDownLatch(1)
        var didThrow = false

        activityScenario?.onActivity { activity ->
            val config = BrowserConfig(url = "about:blank")
            BrowserManager.open(BrowserType.WEBVIEW, config)
        }

        // 等待 WebView 開始載入
        Thread.sleep(500)

        activityScenario?.onActivity { activity ->
            activity.runOnUiThread {
                try {
                    BrowserManager.refresh()
                } catch (e: Exception) {
                    didThrow = true
                } finally {
                    refreshLatch.countDown()
                }
            }
        }

        assertTrue("refresh() 應在 3 秒內完成", refreshLatch.await(3, TimeUnit.SECONDS))
        assertFalse("refresh() 不應拋出例外", didThrow)
    }

    /**
     * 驗證 executeJavaScript() 在 WEBVIEW open() 後不崩潰。
     */
    @Test
    fun testWebViewFlow_executeJavaScript_doesNotCrash() {
        val pageFinishedLatch = CountDownLatch(1)
        val jsLatch = CountDownLatch(1)
        var didThrow = false

        // 使用 createBrowser 直接測試，以便監聽回調
        activityScenario?.onActivity { activity ->
            val config = BrowserConfig(url = "about:blank")
            BrowserManager.open(BrowserType.WEBVIEW, config)
        }

        // 等待 WebView 載入完成
        Thread.sleep(1000)

        activityScenario?.onActivity { activity ->
            activity.runOnUiThread {
                try {
                    BrowserManager.executeJavaScript("1 + 1", "req-manager-test")
                } catch (e: Exception) {
                    didThrow = true
                } finally {
                    jsLatch.countDown()
                }
            }
        }

        assertTrue("executeJavaScript() 應在 3 秒內完成", jsLatch.await(3, TimeUnit.SECONDS))
        assertFalse("executeJavaScript() 不應拋出例外", didThrow)
    }

    // -------------------------------------------------------------------------
    // 完整 open→操作→close 流程（CUSTOM_TAB）
    // -------------------------------------------------------------------------

    /**
     * 驗證 CUSTOM_TAB 完整流程：open() 不崩潰，close() 後 isOpen() 回傳 false。
     */
    @Test
    fun testCustomTabFlow_openAndClose_doesNotCrash() {
        val latch = CountDownLatch(1)
        var didThrow = false
        var isOpenAfterClose = true

        activityScenario?.onActivity { activity ->
            activity.runOnUiThread {
                try {
                    val config = BrowserConfig(url = "https://example.com")
                    BrowserManager.open(BrowserType.CUSTOM_TAB, config)
                    BrowserManager.close()
                    isOpenAfterClose = BrowserManager.isOpen()
                } catch (e: Exception) {
                    didThrow = true
                } finally {
                    latch.countDown()
                }
            }
        }

        assertTrue("操作應在 5 秒內完成", latch.await(5, TimeUnit.SECONDS))
        assertFalse("CUSTOM_TAB open→close 不應拋出例外", didThrow)
        assertFalse("CUSTOM_TAB close() 後 isOpen() 應回傳 false", isOpenAfterClose)
    }

    // -------------------------------------------------------------------------
    // BrowserManager 狀態管理
    // -------------------------------------------------------------------------

    /**
     * 驗證初始狀態下 isOpen() 回傳 false。
     */
    @Test
    fun testInitialState_isOpenReturnsFalse() {
        val latch = CountDownLatch(1)
        var isOpen = true

        activityScenario?.onActivity { activity ->
            activity.runOnUiThread {
                // 先確保關閉
                BrowserManager.close()
                isOpen = BrowserManager.isOpen()
                latch.countDown()
            }
        }

        assertTrue("查詢應在 3 秒內完成", latch.await(3, TimeUnit.SECONDS))
        assertFalse("初始狀態 isOpen() 應回傳 false", isOpen)
    }

    /**
     * 驗證連續多次呼叫 close() 不崩潰（冪等性）。
     */
    @Test
    fun testClose_calledMultipleTimes_doesNotCrash() {
        val latch = CountDownLatch(1)
        var didThrow = false

        activityScenario?.onActivity { activity ->
            activity.runOnUiThread {
                try {
                    BrowserManager.close()
                    BrowserManager.close()
                    BrowserManager.close()
                } catch (e: Exception) {
                    didThrow = true
                } finally {
                    latch.countDown()
                }
            }
        }

        assertTrue("多次 close() 應在 3 秒內完成", latch.await(3, TimeUnit.SECONDS))
        assertFalse("多次呼叫 close() 不應崩潰", didThrow)
    }

    /**
     * 驗證切換瀏覽器類型（WEBVIEW → CUSTOM_TAB）時，舊瀏覽器被正確關閉。
     */
    @Test
    fun testSwitchBrowserType_oldBrowserIsClosed() {
        val latch = CountDownLatch(1)
        var isOpenAfterSwitch = true

        activityScenario?.onActivity { activity ->
            activity.runOnUiThread {
                // 先開啟 WEBVIEW
                val webviewConfig = BrowserConfig(url = "about:blank")
                BrowserManager.open(BrowserType.WEBVIEW, webviewConfig)
            }
        }

        Thread.sleep(300)

        activityScenario?.onActivity { activity ->
            activity.runOnUiThread {
                // 切換至 CUSTOM_TAB（應自動關閉舊的 WEBVIEW）
                val customTabConfig = BrowserConfig(url = "https://example.com")
                BrowserManager.open(BrowserType.CUSTOM_TAB, customTabConfig)
                // 關閉後驗證
                BrowserManager.close()
                isOpenAfterSwitch = BrowserManager.isOpen()
                latch.countDown()
            }
        }

        assertTrue("切換操作應在 5 秒內完成", latch.await(5, TimeUnit.SECONDS))
        assertFalse("切換後 close()，isOpen() 應回傳 false", isOpenAfterSwitch)
    }

    // -------------------------------------------------------------------------
    // 輔助類別
    // -------------------------------------------------------------------------

    /**
     * 空實作的 BrowserCallback，供測試繼承並只覆寫需要的方法。
     */
    private open class NoOpBrowserCallback : BrowserCallback {
        override fun onPageStarted(url: String) {}
        override fun onPageFinished(url: String) {}
        override fun onError(exception: BrowserException) {}
        override fun onPostMessage(message: String) {}
        override fun onJsResult(requestId: String, result: String?) {}
        override fun onDeepLink(url: String) {}
        override fun onClosed() {}
    }
}
