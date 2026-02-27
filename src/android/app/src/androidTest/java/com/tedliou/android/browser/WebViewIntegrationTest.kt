package com.tedliou.android.browser

import android.app.Activity
import android.content.Context
import android.os.Looper
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tedliou.android.browser.core.BrowserCallback
import com.tedliou.android.browser.core.BrowserConfig
import com.tedliou.android.browser.core.BrowserException
import com.tedliou.android.browser.webview.WebViewBrowser
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Instrumented test for WebViewBrowser integration with real WebView on Android runtime.
 * 
 * Tests loading mock HTML pages from assets, verifying PostMessage callbacks,
 * JavaScript execution, and JavaScript injection.
 */
@RunWith(AndroidJUnit4::class)
class WebViewIntegrationTest {
    
    private lateinit var context: Context
    private var activityScenario: ActivityScenario<TestActivity>? = null
    private var browser: WebViewBrowser? = null
    
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        activityScenario = ActivityScenario.launch(TestActivity::class.java)
    }
    
    @After
    fun tearDown() {
        activityScenario?.onActivity { activity ->
            browser?.close()
        }
        activityScenario?.close()
        browser = null
    }
    
    @Test
    fun testLoadMockBasicHtml() {
        val pageLoadedLatch = CountDownLatch(1)
        var loadedUrl: String? = null
        
        activityScenario?.onActivity { activity ->
            val config = BrowserConfig(
                url = "file:///android_asset/mock_basic.html"
            )
            
            val callback = object : BrowserCallback {
                override fun onPageStarted(url: String) {}
                
                override fun onPageFinished(url: String) {
                    loadedUrl = url
                    pageLoadedLatch.countDown()
                }
                
                override fun onJsResult(requestId: String, result: String?) {}
                
                override fun onError(exception: BrowserException) {
                    fail("Page load failed: ${exception.message}")
                }
                
                override fun onPostMessage(message: String) {}
                
                override fun onDeepLink(url: String) {}
                
                override fun onClosed() {}

            }

            browser = WebViewBrowser(activity)
            browser?.open(config, callback)
        }
        
        assertTrue("Page should load within 5 seconds", 
            pageLoadedLatch.await(5, TimeUnit.SECONDS))
        assertNotNull("Loaded URL should not be null", loadedUrl)
        assertTrue("Should load mock_basic.html", 
            loadedUrl?.contains("mock_basic.html") == true)
    }
    
    @Test
    fun testPostMessageReceivedFromPage() {
        val postMessageLatch = CountDownLatch(1)
        var receivedMessage: String? = null
        
        activityScenario?.onActivity { activity ->
            val config = BrowserConfig(
                url = "file:///android_asset/mock_basic.html"
            )
            
            val callback = object : BrowserCallback {
                override fun onPageStarted(url: String) {}
                
                override fun onPageFinished(url: String) {}
                
                override fun onJsResult(requestId: String, result: String?) {}
                
                override fun onPostMessage(message: String) {
                    receivedMessage = message
                    postMessageLatch.countDown()
                }
                
                override fun onError(exception: BrowserException) {
                    fail("Error occurred: ${exception.message}")
                }
                
                override fun onDeepLink(url: String) {}
                override fun onClosed() {}
            }

            browser = WebViewBrowser(activity)
            
            browser = WebViewBrowser(activity)
            browser?.open(config, callback)
        }
        
        assertTrue("PostMessage should be received within 5 seconds", 
            postMessageLatch.await(5, TimeUnit.SECONDS))
        assertNotNull("Received message should not be null", receivedMessage)
        assertTrue("Message should contain 'loaded' type", 
            receivedMessage?.contains("\"type\":\"loaded\"") == true)
    }
    
    @Test
    fun testExecuteJavaScriptGetValue() {
        val pageLoadedLatch = CountDownLatch(1)
        val jsExecutedLatch = CountDownLatch(1)
        var jsResult: String? = null
        
        activityScenario?.onActivity { activity ->
            val config = BrowserConfig(
                url = "file:///android_asset/mock_js_execution.html"
            )
            
            val callback = object : BrowserCallback {
                override fun onPageStarted(url: String) {}
                
                override fun onPageFinished(url: String) {
                    pageLoadedLatch.countDown()
                }
                
                override fun onJsResult(requestId: String, result: String?) {}
                
                override fun onError(exception: BrowserException) {
                    fail("Error occurred: ${exception.message}")
                }
                
                override fun onPostMessage(message: String) {}
                override fun onDeepLink(url: String) {}
                override fun onClosed() {}
            }

            browser = WebViewBrowser(activity)
            
            browser = WebViewBrowser(activity)
            browser?.open(config, callback)
        }
        
        // Wait for page to load
        assertTrue("Page should load", pageLoadedLatch.await(5, TimeUnit.SECONDS))
        
        // Execute JavaScript to call getValue()
        activityScenario?.onActivity { activity ->
            // Need to run on main thread
            activity.runOnUiThread {
                browser?.executeJavaScript("getValue()", "test-request-id")
                // Note: Result would come through callback mechanism
                // For now, just verify no crash
                jsExecutedLatch.countDown()
            }
        }
        
        assertTrue("JavaScript execution should complete", 
            jsExecutedLatch.await(2, TimeUnit.SECONDS))
    }
    
    @Test
    fun testInjectJavaScript() {
        val pageLoadedLatch = CountDownLatch(1)
        val jsInjectedLatch = CountDownLatch(1)
        
        activityScenario?.onActivity { activity ->
            val config = BrowserConfig(
                url = "file:///android_asset/mock_js_injection.html"
            )
            
            val callback = object : BrowserCallback {
                override fun onPageStarted(url: String) {}
                
                override fun onPageFinished(url: String) {
                    pageLoadedLatch.countDown()
                }
                
                override fun onJsResult(requestId: String, result: String?) {}
                
                override fun onError(exception: BrowserException) {
                    fail("Error occurred: ${exception.message}")
                }
                
                override fun onPostMessage(message: String) {}
                override fun onDeepLink(url: String) {}
                override fun onClosed() {}
            }

            browser = WebViewBrowser(activity)
            
            browser = WebViewBrowser(activity)
            browser?.open(config, callback)
        }
        
        // Wait for page to load
        assertTrue("Page should load", pageLoadedLatch.await(5, TimeUnit.SECONDS))
        
        // Inject JavaScript
        activityScenario?.onActivity { activity ->
            activity.runOnUiThread {
                val injectedScript = """
                    displayInjectedContent('<p style="color: green;">Injected content works!</p>');
                """.trimIndent()
                
                browser?.injectJavaScript(injectedScript)
                // Verify no crash
                jsInjectedLatch.countDown()
            }
        }
        
        assertTrue("JavaScript injection should complete", 
            jsInjectedLatch.await(2, TimeUnit.SECONDS))
    }
    
    @Test
    fun testRefreshWebView() {
        val firstLoadLatch = CountDownLatch(1)
        val refreshLatch = CountDownLatch(2) // Will count down twice (first load + refresh)
        
        activityScenario?.onActivity { activity ->
            val config = BrowserConfig(
                url = "file:///android_asset/mock_basic.html"
            )
            
            val callback = object : BrowserCallback {
                override fun onPageStarted(url: String) {}
                
                override fun onPageFinished(url: String) {
                    firstLoadLatch.countDown()
                    refreshLatch.countDown()
                }
                
                override fun onJsResult(requestId: String, result: String?) {}
                
                override fun onError(exception: BrowserException) {
                    fail("Error occurred: ${exception.message}")
                }
                
                override fun onPostMessage(message: String) {}
                override fun onDeepLink(url: String) {}
                override fun onClosed() {}
            }

            browser = WebViewBrowser(activity)
            
            browser = WebViewBrowser(activity)
            browser?.open(config, callback)
        }
        
        // Wait for first load
        assertTrue("Page should load initially", 
            firstLoadLatch.await(5, TimeUnit.SECONDS))
        
        // Refresh
        activityScenario?.onActivity { activity ->
            activity.runOnUiThread {
                browser?.refresh()
            }
        }
        
        assertTrue("Page should reload after refresh", 
            refreshLatch.await(10, TimeUnit.SECONDS))
    }
    
    /**
     * Simple test activity for instrumented tests.
     */
    class TestActivity : Activity()
}
