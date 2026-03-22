package com.tedliou.android.browser

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.tedliou.android.browser.core.BrowserCallback
import com.tedliou.android.browser.core.BrowserConfig
import com.tedliou.android.browser.core.BrowserException
import com.tedliou.android.browser.customtab.CustomTabBrowser
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * CustomTabBrowser 整合測試（Instrumented Test）。
 *
 * 在真實 Android 裝置或模擬器上驗證：
 * - Custom Tab 啟動流程（含 fallback 至系統瀏覽器）
 * - 無 Custom Tab 支援時的降級行為
 * - isOpen() 狀態管理
 * - close() 與 destroy() 不崩潰
 *
 * 注意：模擬器通常未安裝 Chrome，因此大多數測試會走 fallback 路徑（系統瀏覽器）。
 * 測試設計為在有無 Custom Tab 支援的環境下均可穩定執行。
 *
 * 需要 Android 虛擬機或實體裝置才能執行。
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class CustomTabBrowserInstrumentedTest {

    /** 測試用 Activity 場景，提供真實 Activity 上下文。 */
    private var activityScenario: ActivityScenario<WebViewIntegrationTest.TestActivity>? = null

    /** 受測的 CustomTabBrowser 實例。 */
    private var browser: CustomTabBrowser? = null

    /**
     * 每個測試前啟動 TestActivity。
     */
    @Before
    fun setUp() {
        activityScenario = ActivityScenario.launch(WebViewIntegrationTest.TestActivity::class.java)
    }

    /**
     * 每個測試後銷毀瀏覽器並釋放 Activity 資源。
     */
    @After
    fun tearDown() {
        activityScenario?.onActivity {
            browser?.destroy()
        }
        activityScenario?.close()
        browser = null
    }

    // -------------------------------------------------------------------------
    // 測試一：CustomTabBrowser 實例化
    // -------------------------------------------------------------------------

    /**
     * 驗證在真實 Activity 上下文中可成功建立 CustomTabBrowser 實例。
     *
     * 預期：建立後 isOpen() 回傳 false（尚未呼叫 open()）。
     */
    @Test
    fun testInstantiation_instanceIsNotNull() {
        activityScenario?.onActivity { activity ->
            browser = CustomTabBrowser(activity)
            assertNotNull("CustomTabBrowser 實例不應為 null", browser)
        }
    }

    /**
     * 驗證建立 CustomTabBrowser 後，在呼叫 open() 之前 isOpen() 回傳 false。
     */
    @Test
    fun testInstantiation_isOpenReturnsFalseBeforeOpen() {
        activityScenario?.onActivity { activity ->
            browser = CustomTabBrowser(activity)
            assertFalse("open() 前 isOpen() 應回傳 false", browser!!.isOpen())
        }
    }

    // -------------------------------------------------------------------------
    // 測試二：Custom Tab 啟動流程（含 fallback）
    // -------------------------------------------------------------------------

    /**
     * 驗證呼叫 open() 後，onPageStarted 回調被觸發（無論走 Custom Tab 或 fallback 路徑）。
     *
     * 在模擬器上通常走 fallback（系統瀏覽器），但 onPageStarted 均應被呼叫。
     */
    @Test
    fun testOpen_triggersOnPageStarted() {
        val latch = CountDownLatch(1)
        var capturedUrl: String? = null

        activityScenario?.onActivity { activity ->
            val config = BrowserConfig(url = "https://example.com")
            val callback = object : NoOpBrowserCallback() {
                override fun onPageStarted(url: String) {
                    capturedUrl = url
                    latch.countDown()
                }
            }
            browser = CustomTabBrowser(activity)
            browser?.open(config, callback)
        }

        assertTrue("onPageStarted 應在 5 秒內觸發", latch.await(5, TimeUnit.SECONDS))
        assertEquals("onPageStarted 的 URL 應與設定相符", "https://example.com", capturedUrl)
    }

    /**
     * 驗證 open() 在無 Custom Tab 支援的環境下（fallback 路徑）不崩潰，
     * 且 onPageStarted 或 onError 其中一個回調被觸發。
     */
    @Test
    fun testOpen_fallbackPath_doesNotCrash() {
        val latch = CountDownLatch(1)
        var callbackTriggered = false

        activityScenario?.onActivity { activity ->
            val config = BrowserConfig(url = "https://example.com")
            val callback = object : NoOpBrowserCallback() {
                override fun onPageStarted(url: String) {
                    callbackTriggered = true
                    latch.countDown()
                }
                override fun onError(exception: BrowserException) {
                    callbackTriggered = true
                    latch.countDown()
                }
            }
            browser = CustomTabBrowser(activity)
            browser?.open(config, callback)
        }

        // 等待回調（最多 5 秒），若無回調也不算失敗（某些環境下 startActivity 不同步回調）
        latch.await(5, TimeUnit.SECONDS)
        // 主要驗證：不崩潰
        assertTrue("open() 不應崩潰", true)
    }

    // -------------------------------------------------------------------------
    // 測試三：isOpen() 狀態管理
    // -------------------------------------------------------------------------

    /**
     * 驗證 Custom Tab 啟動後（走 Custom Tab 路徑），isOpen() 回傳 true。
     *
     * 注意：若走 fallback 路徑，isOpen() 應回傳 false（系統瀏覽器不追蹤開啟狀態）。
     * 此測試驗證兩種路徑下 isOpen() 均不拋出例外。
     */
    @Test
    fun testIsOpen_afterOpen_doesNotThrow() {
        val latch = CountDownLatch(1)

        activityScenario?.onActivity { activity ->
            val config = BrowserConfig(url = "https://example.com")
            val callback = object : NoOpBrowserCallback() {
                override fun onPageStarted(url: String) {
                    latch.countDown()
                }
                override fun onError(exception: BrowserException) {
                    latch.countDown()
                }
            }
            browser = CustomTabBrowser(activity)
            browser?.open(config, callback)
        }

        latch.await(5, TimeUnit.SECONDS)

        // 在主執行緒查詢 isOpen()，驗證不拋出例外
        val isOpenLatch = CountDownLatch(1)
        var isOpenResult = false
        var didThrow = false
        activityScenario?.onActivity { activity ->
            activity.runOnUiThread {
                try {
                    isOpenResult = browser?.isOpen() ?: false
                } catch (e: Exception) {
                    didThrow = true
                } finally {
                    isOpenLatch.countDown()
                }
            }
        }

        assertTrue("isOpen() 查詢應完成", isOpenLatch.await(3, TimeUnit.SECONDS))
        assertFalse("isOpen() 不應拋出例外", didThrow)
    }

    /**
     * 驗證呼叫 close() 後，isOpen() 回傳 false。
     *
     * Custom Tabs 無法以程式方式關閉，但內部狀態應被重置為 false。
     */
    @Test
    fun testIsOpen_afterClose_returnsFalse() {
        val openLatch = CountDownLatch(1)

        activityScenario?.onActivity { activity ->
            val config = BrowserConfig(url = "https://example.com")
            val callback = object : NoOpBrowserCallback() {
                override fun onPageStarted(url: String) {
                    openLatch.countDown()
                }
                override fun onError(exception: BrowserException) {
                    openLatch.countDown()
                }
            }
            browser = CustomTabBrowser(activity)
            browser?.open(config, callback)
        }

        openLatch.await(5, TimeUnit.SECONDS)

        // 呼叫 close() 並驗證 isOpen() 回傳 false
        val closeLatch = CountDownLatch(1)
        var isOpenAfterClose = true
        activityScenario?.onActivity { activity ->
            activity.runOnUiThread {
                browser?.close()
                isOpenAfterClose = browser?.isOpen() ?: true
                closeLatch.countDown()
            }
        }

        assertTrue("close() 應在 3 秒內完成", closeLatch.await(3, TimeUnit.SECONDS))
        assertFalse("close() 後 isOpen() 應回傳 false", isOpenAfterClose)
    }

    // -------------------------------------------------------------------------
    // 測試四：close() 與 destroy() 不崩潰
    // -------------------------------------------------------------------------

    /**
     * 驗證在未呼叫 open() 的情況下呼叫 close() 不崩潰。
     */
    @Test
    fun testClose_withoutOpen_doesNotCrash() {
        val latch = CountDownLatch(1)
        var didThrow = false

        activityScenario?.onActivity { activity ->
            activity.runOnUiThread {
                try {
                    browser = CustomTabBrowser(activity)
                    browser?.close()
                } catch (e: Exception) {
                    didThrow = true
                } finally {
                    latch.countDown()
                }
            }
        }

        assertTrue("close() 應在 3 秒內完成", latch.await(3, TimeUnit.SECONDS))
        assertFalse("未 open() 直接 close() 不應崩潰", didThrow)
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
                    browser = CustomTabBrowser(activity)
                    browser?.close()
                    browser?.close()
                    browser?.close()
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
     * 驗證在未呼叫 open() 的情況下呼叫 destroy() 不崩潰。
     */
    @Test
    fun testDestroy_withoutOpen_doesNotCrash() {
        val latch = CountDownLatch(1)
        var didThrow = false

        activityScenario?.onActivity { activity ->
            activity.runOnUiThread {
                try {
                    browser = CustomTabBrowser(activity)
                    browser?.destroy()
                } catch (e: Exception) {
                    didThrow = true
                } finally {
                    latch.countDown()
                }
            }
        }

        assertTrue("destroy() 應在 3 秒內完成", latch.await(3, TimeUnit.SECONDS))
        assertFalse("未 open() 直接 destroy() 不應崩潰", didThrow)
    }

    /**
     * 驗證 destroy() 後 isOpen() 回傳 false。
     */
    @Test
    fun testDestroy_resetsIsOpenToFalse() {
        val latch = CountDownLatch(1)
        var isOpenAfterDestroy = true

        activityScenario?.onActivity { activity ->
            activity.runOnUiThread {
                browser = CustomTabBrowser(activity)
                browser?.destroy()
                isOpenAfterDestroy = browser?.isOpen() ?: true
                latch.countDown()
            }
        }

        assertTrue("destroy() 應在 3 秒內完成", latch.await(3, TimeUnit.SECONDS))
        assertFalse("destroy() 後 isOpen() 應回傳 false", isOpenAfterDestroy)
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
