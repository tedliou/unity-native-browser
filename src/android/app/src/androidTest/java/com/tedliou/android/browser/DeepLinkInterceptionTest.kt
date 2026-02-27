package com.tedliou.android.browser

import android.app.Activity
import android.content.Context
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
 * Instrumented test for deep link interception in WebView.
 * 
 * Tests that when a WebView navigates to a deep link URL matching configured patterns,
 * the navigation is intercepted and the onDeepLink callback is triggered instead of
 * actually navigating to the deep link.
 */
@RunWith(AndroidJUnit4::class)
class DeepLinkInterceptionTest {
    
    private lateinit var context: Context
    private var activityScenario: ActivityScenario<WebViewIntegrationTest.TestActivity>? = null
    private var browser: WebViewBrowser? = null
    
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        activityScenario = ActivityScenario.launch(WebViewIntegrationTest.TestActivity::class.java)
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
    fun testDeepLinkInterception() {
        val pageLoadedLatch = CountDownLatch(1)
        val deepLinkLatch = CountDownLatch(1)
        var interceptedUrl: String? = null
        
        activityScenario?.onActivity { activity ->
            val config = BrowserConfig(
                url = "file:///android_asset/mock_deeplink.html",
                deepLinkPatterns = listOf("myapp://callback*")
            )
            
            val callback = object : BrowserCallback {
                override fun onPageStarted(url: String) {}
                
                override fun onPageFinished(url: String) {
                    pageLoadedLatch.countDown()
                }
                
                override fun onJsResult(requestId: String, result: String?) {}
                
                override fun onDeepLink(url: String) {
                    interceptedUrl = url
                    deepLinkLatch.countDown()
                }
                
                override fun onError(exception: BrowserException) {
                    fail("Error occurred: ${exception.message}")
                }
                
                override fun onPostMessage(message: String) {}
                
                override fun onClosed() {}
            }

            browser = WebViewBrowser(activity)
            browser = WebViewBrowser(activity)
            browser?.open(config, callback)
        }
        
        // Wait for page to load
        assertTrue("Page should load", pageLoadedLatch.await(5, TimeUnit.SECONDS))
        
        // Click deep link by injecting JavaScript
        activityScenario?.onActivity { activity ->
            activity.runOnUiThread {
                browser?.executeJavaScript(
                    """
                    var link = document.querySelector('a[href="myapp://callback?result=success"]');
                    if (link) link.click();
                    """.trimIndent(),
                    null
                )
            }
        }
        
        // Wait for deep link callback
        assertTrue("Deep link should be intercepted within 5 seconds",
            deepLinkLatch.await(5, TimeUnit.SECONDS))
        
        assertNotNull("Intercepted URL should not be null", interceptedUrl)
        assertTrue("Intercepted URL should start with myapp://callback",
            interceptedUrl?.startsWith("myapp://callback") == true)
        assertTrue("Intercepted URL should contain result=success parameter",
            interceptedUrl?.contains("result=success") == true)
    }
    
    @Test
    fun testDeepLinkWithErrorParameter() {
        val pageLoadedLatch = CountDownLatch(1)
        val deepLinkLatch = CountDownLatch(1)
        var interceptedUrl: String? = null
        
        activityScenario?.onActivity { activity ->
            val config = BrowserConfig(
                url = "file:///android_asset/mock_deeplink.html",
                deepLinkPatterns = listOf("myapp://callback*")
            )
            
            val callback = object : BrowserCallback {
                override fun onPageStarted(url: String) {}
                
                override fun onPageFinished(url: String) {
                    pageLoadedLatch.countDown()
                }
                
                override fun onJsResult(requestId: String, result: String?) {}
                
                override fun onDeepLink(url: String) {
                    interceptedUrl = url
                    deepLinkLatch.countDown()
                }
                
                override fun onError(exception: BrowserException) {
                    fail("Error occurred: ${exception.message}")
                }
                
                override fun onPostMessage(message: String) {}
                
                override fun onClosed() {}
            }

            browser = WebViewBrowser(activity)
            browser = WebViewBrowser(activity)
            browser?.open(config, callback)
        }
        
        // Wait for page to load
        assertTrue("Page should load", pageLoadedLatch.await(5, TimeUnit.SECONDS))
        
        // Click error deep link
        activityScenario?.onActivity { activity ->
            activity.runOnUiThread {
                browser?.executeJavaScript(
                    """
                    var link = document.querySelector('a[href*="result=error"]');
                    if (link) link.click();
                    """.trimIndent(),
                    null
                )
            }
        }
        
        // Wait for deep link callback
        assertTrue("Deep link should be intercepted",
            deepLinkLatch.await(5, TimeUnit.SECONDS))
        
        assertNotNull("Intercepted URL should not be null", interceptedUrl)
        assertTrue("Should intercept error deep link",
            interceptedUrl?.contains("result=error") == true)
        assertTrue("Should contain message parameter",
            interceptedUrl?.contains("message=TestError") == true)
    }
    
    @Test
    fun testDeepLinkWithMultipleParameters() {
        val pageLoadedLatch = CountDownLatch(1)
        val deepLinkLatch = CountDownLatch(1)
        var interceptedUrl: String? = null
        
        activityScenario?.onActivity { activity ->
            val config = BrowserConfig(
                url = "file:///android_asset/mock_deeplink.html",
                deepLinkPatterns = listOf("myapp://callback*")
            )
            
            val callback = object : BrowserCallback {
                override fun onPageStarted(url: String) {}
                
                override fun onPageFinished(url: String) {
                    pageLoadedLatch.countDown()
                }
                
                override fun onJsResult(requestId: String, result: String?) {}
                
                override fun onDeepLink(url: String) {
                    interceptedUrl = url
                    deepLinkLatch.countDown()
                }
                
                override fun onError(exception: BrowserException) {
                    fail("Error occurred: ${exception.message}")
                }
                
                override fun onPostMessage(message: String) {}
                
                override fun onClosed() {}
            }

            browser = WebViewBrowser(activity)
            browser = WebViewBrowser(activity)
            browser?.open(config, callback)
        }
        
        // Wait for page to load
        assertTrue("Page should load", pageLoadedLatch.await(5, TimeUnit.SECONDS))
        
        // Click deep link with multiple parameters
        activityScenario?.onActivity { activity ->
            activity.runOnUiThread {
                browser?.executeJavaScript(
                    """
                    var link = document.querySelector('a[href*="token=abc123"]');
                    if (link) link.click();
                    """.trimIndent(),
                    null
                )
            }
        }
        
        // Wait for deep link callback
        assertTrue("Deep link should be intercepted",
            deepLinkLatch.await(5, TimeUnit.SECONDS))
        
        assertNotNull("Intercepted URL should not be null", interceptedUrl)
        assertTrue("Should contain token parameter",
            interceptedUrl?.contains("token=abc123") == true)
        assertTrue("Should contain userId parameter",
            interceptedUrl?.contains("userId=456") == true)
        assertTrue("Should contain result parameter",
            interceptedUrl?.contains("result=success") == true)
    }
    
    @Test
    fun testNonDeepLinkNavigationNotIntercepted() {
        val pageLoadedLatch = CountDownLatch(1)
        val deepLinkLatch = CountDownLatch(1)
        var deepLinkCalled = false
        
        activityScenario?.onActivity { activity ->
            val config = BrowserConfig(
                url = "file:///android_asset/mock_deeplink.html",
                deepLinkPatterns = listOf("myapp://callback*")
            )
            
            val callback = object : BrowserCallback {
                override fun onPageStarted(url: String) {}
                
                override fun onPageFinished(url: String) {
                    pageLoadedLatch.countDown()
                }
                
                override fun onJsResult(requestId: String, result: String?) {}
                
                override fun onDeepLink(url: String) {
                    deepLinkCalled = true
                    deepLinkLatch.countDown()
                }
                
                override fun onError(exception: BrowserException) {
                    // Ignore errors from fragment navigation
                }
                
                override fun onPostMessage(message: String) {}
                
                override fun onClosed() {}
            }

            browser = WebViewBrowser(activity)
            browser = WebViewBrowser(activity)
            browser?.open(config, callback)
        }
        
        // Wait for page to load
        assertTrue("Page should load", pageLoadedLatch.await(5, TimeUnit.SECONDS))
        
        // Click normal fragment link (should NOT trigger deep link callback)
        activityScenario?.onActivity { activity ->
            activity.runOnUiThread {
                browser?.executeJavaScript(
                    """
                    var link = document.querySelector('a[href="#normal"]');
                    if (link) link.click();
                    """.trimIndent(),
                    null
                )
            }
        }
        
        // Wait a bit to ensure deep link callback is NOT called
        val called = deepLinkLatch.await(2, TimeUnit.SECONDS)
        
        assertFalse("Deep link callback should NOT be called for normal links", called)
        assertFalse("Deep link callback should NOT be called for fragment navigation", 
            deepLinkCalled)
    }
    
    @Test
    fun testCustomActionDeepLink() {
        val pageLoadedLatch = CountDownLatch(1)
        val deepLinkLatch = CountDownLatch(1)
        var interceptedUrl: String? = null
        
        activityScenario?.onActivity { activity ->
            val config = BrowserConfig(
                url = "file:///android_asset/mock_deeplink.html",
                deepLinkPatterns = listOf("myapp://action*", "myapp://callback*")
            )
            
            val callback = object : BrowserCallback {
                override fun onPageStarted(url: String) {}
                
                override fun onPageFinished(url: String) {
                    pageLoadedLatch.countDown()
                }
                
                override fun onJsResult(requestId: String, result: String?) {}
                
                override fun onDeepLink(url: String) {
                    interceptedUrl = url
                    deepLinkLatch.countDown()
                }
                
                override fun onError(exception: BrowserException) {
                    fail("Error occurred: ${exception.message}")
                }
                
                override fun onPostMessage(message: String) {}
                
                override fun onClosed() {}
            }

            browser = WebViewBrowser(activity)
            browser = WebViewBrowser(activity)
            browser?.open(config, callback)
        }
        
        // Wait for page to load
        assertTrue("Page should load", pageLoadedLatch.await(5, TimeUnit.SECONDS))
        
        // Click action deep link
        activityScenario?.onActivity { activity ->
            activity.runOnUiThread {
                browser?.executeJavaScript(
                    """
                    var link = document.querySelector('a[href="myapp://action?type=close"]');
                    if (link) link.click();
                    """.trimIndent(),
                    null
                )
            }
        }
        
        // Wait for deep link callback
        assertTrue("Deep link should be intercepted",
            deepLinkLatch.await(5, TimeUnit.SECONDS))
        
        assertNotNull("Intercepted URL should not be null", interceptedUrl)
        assertTrue("Should intercept action deep link",
            interceptedUrl?.startsWith("myapp://action") == true)
        assertTrue("Should contain type parameter",
            interceptedUrl?.contains("type=close") == true)
    }
}
