package com.tedliou.android.browser

import android.app.Activity
import com.tedliou.android.browser.bridge.BrowserBridge
import com.tedliou.android.browser.bridge.UnityBridgeCallback
import com.tedliou.android.browser.core.BrowserConfig
import com.tedliou.android.browser.core.BrowserType
import com.tedliou.android.browser.core.IBrowser
import com.tedliou.android.browser.customtab.CustomTabBrowser
import com.tedliou.android.browser.system.SystemBrowser
import com.tedliou.android.browser.util.BrowserLogger
import com.tedliou.android.browser.webview.WebViewBrowser
import java.lang.ref.WeakReference

object BrowserManager {
    private const val SUBTAG = "Manager"

    private var currentBrowser: IBrowser? = null
    private var currentBrowserType: BrowserType? = null
    private var activityRef: WeakReference<Activity>? = null
    private val unityCallback = UnityBridgeCallback()

    fun initialize(activity: Activity) {
        BrowserLogger.d(SUBTAG, "Initializing BrowserManager")
        activityRef = WeakReference(activity)
        BrowserBridge.initialize(activity)
    }

    fun open(type: BrowserType, config: BrowserConfig) {
        BrowserLogger.d(SUBTAG, "Opening browser type: $type")
        val activity = activityRef?.get()
        if (activity == null) {
            BrowserLogger.w(SUBTAG, "Activity reference lost; cannot open browser")
            return
        }
        val existing = currentBrowser
        if (existing?.isOpen() == true) {
            BrowserLogger.d(SUBTAG, "Existing browser open; closing before opening new one")
            existing.close()
        }
        val browser = createBrowser(type, activity)
        currentBrowser = browser
        currentBrowserType = type
        browser.open(config, unityCallback)
    }

    fun close() {
        BrowserLogger.d(SUBTAG, "Closing current browser")
        currentBrowser?.close()
    }

    fun refresh() {
        BrowserLogger.d(SUBTAG, "Refreshing current browser")
        currentBrowser?.refresh()
    }

    fun executeJavaScript(script: String, requestId: String?) {
        val browser = currentBrowser
        if (browser is WebViewBrowser) {
            browser.executeJavaScript(script, requestId)
        } else {
            BrowserLogger.w(SUBTAG, "JS operations only available for WebView type")
        }
    }

    fun injectJavaScript(script: String) {
        val browser = currentBrowser
        if (browser is WebViewBrowser) {
            browser.injectJavaScript(script)
        } else {
            BrowserLogger.w(SUBTAG, "JS operations only available for WebView type")
        }
    }

    fun isOpen(): Boolean {
        return currentBrowser?.isOpen() ?: false
    }

    fun getCurrentBrowserType(): BrowserType? {
        return currentBrowserType
    }

    fun createBrowser(type: BrowserType): IBrowser {
        val activity = activityRef?.get()
        requireNotNull(activity) { "Activity not initialized" }
        return createBrowser(type, activity)
    }

    private fun createBrowser(type: BrowserType, activity: Activity): IBrowser {
        return when (type) {
            BrowserType.WEBVIEW -> WebViewBrowser(activity)
            BrowserType.CUSTOM_TAB -> CustomTabBrowser(activity)
            BrowserType.SYSTEM_BROWSER -> SystemBrowser(activity)
        }
    }
}
