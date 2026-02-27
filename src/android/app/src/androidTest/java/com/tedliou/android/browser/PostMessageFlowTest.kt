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
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Instrumented test for PostMessage flow between web page and native code.
 * 
 * Tests the complete flow:
 * 1. Load page with button
 * 2. Page sends PostMessage when button clicked
 * 3. Native receives PostMessage
 * 4. Native executes JavaScript in response
 * 5. Web page responds to JavaScript execution
 * 
 * This matches the README verification scenario.
 */
@RunWith(AndroidJUnit4::class)
class PostMessageFlowTest {
    
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
    fun testFullPostMessageFlow() {
        val pageLoadedLatch = CountDownLatch(1)
        val postMessageLatch = CountDownLatch(1)
        var receivedMessage: String? = null
        
        activityScenario?.onActivity { activity ->
            val config = BrowserConfig(
                url = "file:///android_asset/mock_postmessage.html"
            )
            
            val callback = object : BrowserCallback {
                override fun onPageStarted(url: String) {}
                
                override fun onPageFinished(url: String) {
                    pageLoadedLatch.countDown()
                }
                
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
        
        // Wait for page to load
        assertTrue("Page should load", pageLoadedLatch.await(5, TimeUnit.SECONDS))
        
        // Simulate button click by injecting JavaScript
        activityScenario?.onActivity { activity ->
            activity.runOnUiThread {
                browser?.executeJavaScript(
                    "document.getElementById('sendButton').click();",
                    null
                )
            }
        }
        
        // Wait for PostMessage
        assertTrue("PostMessage should be received within 5 seconds",
            postMessageLatch.await(5, TimeUnit.SECONDS))
        
        assertNotNull("Received message should not be null", receivedMessage)
        
        // Verify message structure
        try {
            val json = JSONObject(receivedMessage!!)
            assertEquals("Message type should be 'buttonClicked'", 
                "buttonClicked", json.optString("type"))
            assertTrue("Message should have data object", 
                json.has("data"))
        } catch (e: Exception) {
            fail("Invalid JSON message: ${e.message}")
        }
    }
    
    @Test
    fun testPostMessageOnPageLoad() {
        val pageLoadedLatch = CountDownLatch(1)
        val postMessageLatch = CountDownLatch(1)
        var receivedMessage: String? = null
        
        activityScenario?.onActivity { activity ->
            val config = BrowserConfig(
                url = "file:///android_asset/mock_postmessage.html"
            )
            
            val callback = object : BrowserCallback {
                override fun onPageStarted(url: String) {}
                
                override fun onPageFinished(url: String) {
                    pageLoadedLatch.countDown()
                }
                
                override fun onJsResult(requestId: String, result: String?) {}
                
                override fun onPostMessage(message: String) {
                    // First message should be from DOMContentLoaded
                    if (receivedMessage == null) {
                        receivedMessage = message
                        postMessageLatch.countDown()
                    }
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
        
        // Wait for both page load and PostMessage
        assertTrue("Page should load", pageLoadedLatch.await(5, TimeUnit.SECONDS))
        assertTrue("PostMessage should be received on page load",
            postMessageLatch.await(5, TimeUnit.SECONDS))
        
        assertNotNull("Received message should not be null", receivedMessage)
        
        // Verify it's the 'loaded' message from DOMContentLoaded
        try {
            val json = JSONObject(receivedMessage!!)
            assertEquals("Message type should be 'loaded'", 
                "loaded", json.optString("type"))
        } catch (e: Exception) {
            fail("Invalid JSON message: ${e.message}")
        }
    }
    
    @Test
    fun testNativeExecutesJavaScriptInResponseToPostMessage() {
        val pageLoadedLatch = CountDownLatch(1)
        val postMessageLatch = CountDownLatch(1)
        val jsExecutedLatch = CountDownLatch(1)
        var postMessageReceived = false
        
        activityScenario?.onActivity { activity ->
            val config = BrowserConfig(
                url = "file:///android_asset/mock_postmessage.html"
            )
            
            val callback = object : BrowserCallback {
                override fun onPageStarted(url: String) {}
                
                override fun onPageFinished(url: String) {
                    pageLoadedLatch.countDown()
                }
                
                override fun onJsResult(requestId: String, result: String?) {}
                
                override fun onPostMessage(message: String) {
                    if (!postMessageReceived) {
                        postMessageReceived = true
                        postMessageLatch.countDown()
                        
                        // Native responds by executing JavaScript
                        activity.runOnUiThread {
                            browser?.executeJavaScript(
                                "document.getElementById('log').innerText += '\\nNative responded!';",
                                "response-1"
                            )
                            jsExecutedLatch.countDown()
                        }
                    }
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
        
        // Wait for page load
        assertTrue("Page should load", pageLoadedLatch.await(5, TimeUnit.SECONDS))
        
        // Wait for PostMessage (from DOMContentLoaded)
        assertTrue("PostMessage should be received",
            postMessageLatch.await(5, TimeUnit.SECONDS))
        
        // Wait for JavaScript execution in response
        assertTrue("JavaScript should be executed in response to PostMessage",
            jsExecutedLatch.await(5, TimeUnit.SECONDS))
        
        assertTrue("PostMessage should have been received", postMessageReceived)
    }
    
    @Test
    fun testMultiplePostMessages() {
        val pageLoadedLatch = CountDownLatch(1)
        val multipleMessagesLatch = CountDownLatch(3) // Expect at least 3 messages
        val receivedMessages = mutableListOf<String>()
        
        activityScenario?.onActivity { activity ->
            val config = BrowserConfig(
                url = "file:///android_asset/mock_postmessage.html"
            )
            
            val callback = object : BrowserCallback {
                override fun onPageStarted(url: String) {}
                
                override fun onPageFinished(url: String) {
                    pageLoadedLatch.countDown()
                }
                
                override fun onJsResult(requestId: String, result: String?) {}
                
                override fun onPostMessage(message: String) {
                    synchronized(receivedMessages) {
                        receivedMessages.add(message)
                        multipleMessagesLatch.countDown()
                    }
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
        
        // Wait for page load
        assertTrue("Page should load", pageLoadedLatch.await(5, TimeUnit.SECONDS))
        
        // Click button multiple times
        activityScenario?.onActivity { activity ->
            activity.runOnUiThread {
                // Click button 3 times
                browser?.executeJavaScript(
                    "document.getElementById('sendButton').click();",
                    null
                )
                browser?.executeJavaScript(
                    "document.getElementById('sendButton').click();",
                    null
                )
                browser?.executeJavaScript(
                    "document.getElementById('sendButton').click();",
                    null
                )
            }
        }
        
        // Wait for multiple messages (1 from page load + 3 from button clicks = 4 total)
        assertTrue("Should receive at least 3 PostMessages within 10 seconds",
            multipleMessagesLatch.await(10, TimeUnit.SECONDS))
        
        assertTrue("Should have received multiple messages",
            receivedMessages.size >= 3)
    }
}
