package com.tedliou.android.browser

import android.app.Activity
import com.tedliou.android.browser.bridge.BrowserBridge
import com.tedliou.android.browser.bridge.UnityBridgeCallback
import com.tedliou.android.browser.core.Alignment
import com.tedliou.android.browser.core.BrowserConfig
import com.tedliou.android.browser.core.BrowserType
import com.tedliou.android.browser.core.IBrowser
import com.tedliou.android.browser.customtab.CustomTabBrowser
import com.tedliou.android.browser.system.SystemBrowser
import com.tedliou.android.browser.util.BrowserLogger
import com.tedliou.android.browser.webview.WebViewBrowser
import org.json.JSONObject
import java.lang.ref.WeakReference

/**
 * Singleton entry point for the NativeBrowser plugin.
 *
 * All public methods are annotated with [@JvmStatic] so Unity C# can invoke them
 * via `AndroidJavaClass.CallStatic(...)` without needing to access the INSTANCE field.
 *
 * Unity-facing methods accept String parameters (type name, JSON config) because
 * JNI bridge cannot pass Kotlin enums or data classes directly.
 */
object BrowserManager {
    private const val SUBTAG = "Manager"

    private var currentBrowser: IBrowser? = null
    private var currentBrowserType: BrowserType? = null
    private var activityRef: WeakReference<Activity>? = null
    private val unityCallback = UnityBridgeCallback()

    /**
     * Initialize the browser manager with the current Activity.
     *
     * Must be called from Unity before any browser operations.
     * Also initializes the [BrowserBridge] for C# callback delivery.
     *
     * @param activity The Android Activity instance from Unity
     */
    @JvmStatic
    fun initialize(activity: Activity) {
        BrowserLogger.d(SUBTAG, "Initializing BrowserManager")
        activityRef = WeakReference(activity)
        BrowserBridge.initialize(activity)
    }

    /**
     * Open a browser with the given type and configuration.
     *
     * Called from Unity via JNI with string parameters.
     * Parses the type string to [BrowserType] and JSON string to [BrowserConfig].
     *
     * @param typeString Browser type name: "WEBVIEW", "CUSTOM_TAB", or "SYSTEM_BROWSER"
     * @param configJson JSON string with browser configuration fields
     */
    @JvmStatic
    fun open(typeString: String, configJson: String) {
        BrowserLogger.d(SUBTAG, "Opening browser type: $typeString")
        val type = parseBrowserType(typeString)
        val config = parseConfig(configJson)
        openInternal(type, config)
    }

    /**
     * Open a browser with typed parameters (for Kotlin/Java callers).
     */
    fun open(type: BrowserType, config: BrowserConfig) {
        BrowserLogger.d(SUBTAG, "Opening browser type: $type")
        openInternal(type, config)
    }

    /**
     * Close the currently open browser instance.
     */
    @JvmStatic
    fun close() {
        BrowserLogger.d(SUBTAG, "Closing current browser")
        currentBrowser?.close()
    }

    /**
     * Refresh the currently open browser page.
     */
    @JvmStatic
    fun refresh() {
        BrowserLogger.d(SUBTAG, "Refreshing current browser")
        currentBrowser?.refresh()
    }

    /**
     * Execute JavaScript in the currently open WebView.
     *
     * Only works when the current browser is a WebView; logs a warning otherwise.
     *
     * @param script JavaScript code to execute
     * @param requestId Optional identifier for correlating JS results
     */
    @JvmStatic
    fun executeJavaScript(script: String, requestId: String?) {
        val browser = currentBrowser
        if (browser is WebViewBrowser) {
            browser.executeJavaScript(script, requestId)
        } else {
            BrowserLogger.w(SUBTAG, "JS operations only available for WebView type")
        }
    }

    /**
     * Inject JavaScript to run on every page load in the currently open WebView.
     *
     * Only works when the current browser is a WebView; logs a warning otherwise.
     *
     * @param script JavaScript code to inject
     */
    @JvmStatic
    fun injectJavaScript(script: String) {
        val browser = currentBrowser
        if (browser is WebViewBrowser) {
            browser.injectJavaScript(script)
        } else {
            BrowserLogger.w(SUBTAG, "JS operations only available for WebView type")
        }
    }

    /**
     * Check if any browser instance is currently open.
     *
     * @return true if a browser is open, false otherwise
     */
    @JvmStatic
    fun isOpen(): Boolean {
        return currentBrowser?.isOpen() ?: false
    }

    /**
     * Get the type of the currently open browser, or null if none is open.
     */
    fun getCurrentBrowserType(): BrowserType? {
        return currentBrowserType
    }

    /**
     * Create a browser instance without opening it (for testing).
     */
    fun createBrowser(type: BrowserType): IBrowser {
        val activity = activityRef?.get()
        requireNotNull(activity) { "Activity not initialized" }
        return createBrowser(type, activity)
    }

    // --- Internal helpers ---

    private fun openInternal(type: BrowserType, config: BrowserConfig) {
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

    private fun createBrowser(type: BrowserType, activity: Activity): IBrowser {
        return when (type) {
            BrowserType.WEBVIEW -> WebViewBrowser(activity)
            BrowserType.CUSTOM_TAB -> CustomTabBrowser(activity)
            BrowserType.SYSTEM_BROWSER -> SystemBrowser(activity)
        }
    }

    /**
     * Parse a browser type string to [BrowserType] enum.
     * Falls back to WEBVIEW for unrecognized values.
     */
    private fun parseBrowserType(typeString: String): BrowserType {
        return try {
            BrowserType.valueOf(typeString.uppercase())
        } catch (e: IllegalArgumentException) {
            BrowserLogger.w(SUBTAG, "Unknown browser type '$typeString', falling back to WEBVIEW")
            BrowserType.WEBVIEW
        }
    }

    /**
     * Parse a JSON configuration string to [BrowserConfig].
     *
     * Expected JSON format matches the C# BrowserConfig.ToJson() output:
     * ```json
     * {
     *   "url": "https://example.com",
     *   "width": 0.9,
     *   "height": 0.8,
     *   "alignment": "CENTER",
     *   "closeOnTapOutside": true,
     *   "enableJavaScript": true,
     *   "deepLinkPatterns": ["pattern1", "pattern2"],
     *   "closeOnDeepLink": true,
     *   "userAgent": ""
     * }
     * ```
     */
    private fun parseConfig(configJson: String): BrowserConfig {
        val json = JSONObject(configJson)

        val deepLinkPatterns = mutableListOf<String>()
        if (json.has("deepLinkPatterns")) {
            val patternsArray = json.getJSONArray("deepLinkPatterns")
            for (i in 0 until patternsArray.length()) {
                deepLinkPatterns.add(patternsArray.getString(i))
            }
        }

        val alignmentStr = json.optString("alignment", "CENTER")
        val alignment = try {
            Alignment.valueOf(alignmentStr.uppercase())
        } catch (e: IllegalArgumentException) {
            BrowserLogger.w(SUBTAG, "Unknown alignment '$alignmentStr', falling back to CENTER")
            Alignment.CENTER
        }

        return BrowserConfig(
            url = json.getString("url"),
            width = json.optDouble("width", 1.0).toFloat(),
            height = json.optDouble("height", 1.0).toFloat(),
            alignment = alignment,
            closeOnTapOutside = json.optBoolean("closeOnTapOutside", false),
            deepLinkPatterns = deepLinkPatterns,
            closeOnDeepLink = json.optBoolean("closeOnDeepLink", true),
            enableJavaScript = json.optBoolean("enableJavaScript", true),
            userAgent = json.optString("userAgent", ""),
        )
    }
}
