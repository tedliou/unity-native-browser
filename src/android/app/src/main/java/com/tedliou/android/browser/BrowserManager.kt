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
 * NativeBrowser 插件的單例入口點。
 *
 * 所有公開方法均標註 [@JvmStatic]，讓 Unity C# 可透過
 * `AndroidJavaClass.CallStatic(...)` 直接呼叫，無需存取 INSTANCE 欄位。
 *
 * 面向 Unity 的方法接受 String 參數（類型名稱、JSON 設定），
 * 因為 JNI 橋接無法直接傳遞 Kotlin 列舉或資料類別。
 */
object BrowserManager {
    private const val SUBTAG = "Manager"

    private var currentBrowser: IBrowser? = null
    private var currentBrowserType: BrowserType? = null
    private var activityRef: WeakReference<Activity>? = null
    private val unityCallback = UnityBridgeCallback()

    /**
     * 以目前的 Activity 初始化瀏覽器管理器。
     *
     * 必須在任何瀏覽器操作前從 Unity 呼叫。
     * 同時初始化 [BrowserBridge] 以傳遞 C# 回調。
     *
     * @param activity 來自 Unity 的 Android Activity 實例
     */
    @JvmStatic
    fun initialize(activity: Activity) {
        BrowserLogger.d(SUBTAG, "initialize() 開始 | activity=${activity.javaClass.simpleName}")
        activityRef = WeakReference(activity)
        BrowserBridge.initialize(activity)
        BrowserLogger.d(SUBTAG, "initialize() 完成")
    }

    /**
     * 以指定類型與設定開啟瀏覽器。
     *
     * 由 Unity 透過 JNI 以字串參數呼叫。
     * 將類型字串解析為 [BrowserType]，將 JSON 字串解析為 [BrowserConfig]。
     *
     * @param typeString 瀏覽器類型名稱："WEBVIEW"、"CUSTOM_TAB" 或 "SYSTEM_BROWSER"
     * @param configJson 包含瀏覽器設定欄位的 JSON 字串
     */
    @JvmStatic
    fun open(typeString: String, configJson: String) {
        BrowserLogger.d(SUBTAG, "open() 開始 | typeString=$typeString")
        val type = parseBrowserType(typeString)
        val config = parseConfig(configJson)
        openInternal(type, config)
        BrowserLogger.d(SUBTAG, "open() 完成 | type=$type url=${config.url}")
    }

    /**
     * 以具型別參數開啟瀏覽器（供 Kotlin/Java 呼叫端使用）。
     */
    fun open(type: BrowserType, config: BrowserConfig) {
        BrowserLogger.d(SUBTAG, "open() 開始 | type=$type url=${config.url}")
        openInternal(type, config)
        BrowserLogger.d(SUBTAG, "open() 完成")
    }

    /**
     * 關閉目前開啟的瀏覽器實例。
     */
    @JvmStatic
    fun close() {
        BrowserLogger.d(SUBTAG, "close() 開始")
        currentBrowser?.close()
        BrowserLogger.d(SUBTAG, "close() 完成")
    }

    /**
     * 重新整理目前開啟的瀏覽器頁面。
     */
    @JvmStatic
    fun refresh() {
        BrowserLogger.d(SUBTAG, "refresh() 開始")
        currentBrowser?.refresh()
        BrowserLogger.d(SUBTAG, "refresh() 完成")
    }

    /**
     * 在目前開啟的 WebView 中執行 JavaScript。
     *
     * 僅在目前瀏覽器為 WebView 時有效；否則記錄警告。
     *
     * @param script 要執行的 JavaScript 程式碼
     * @param requestId 用於關聯 JS 結果的可選識別碼
     */
    @JvmStatic
    fun executeJavaScript(script: String, requestId: String?) {
        BrowserLogger.d(SUBTAG, "executeJavaScript() 開始 | requestId=$requestId scriptLen=${script.length}")
        val browser = currentBrowser
        if (browser is WebViewBrowser) {
            browser.executeJavaScript(script, requestId)
            BrowserLogger.d(SUBTAG, "executeJavaScript() 完成 | requestId=$requestId")
        } else {
            BrowserLogger.w(SUBTAG, "JS operations only available for WebView type")
        }
    }

    /**
     * 注入 JavaScript，使其在目前開啟的 WebView 每次頁面載入時執行。
     *
     * 僅在目前瀏覽器為 WebView 時有效；否則記錄警告。
     *
     * @param script 要注入的 JavaScript 程式碼
     */
    @JvmStatic
    fun injectJavaScript(script: String) {
        BrowserLogger.d(SUBTAG, "injectJavaScript() 開始 | scriptLen=${script.length}")
        val browser = currentBrowser
        if (browser is WebViewBrowser) {
            browser.injectJavaScript(script)
            BrowserLogger.d(SUBTAG, "injectJavaScript() 完成")
        } else {
            BrowserLogger.w(SUBTAG, "JS operations only available for WebView type")
        }
    }

    /**
     * 從 Unity 向目前開啟的 WebView 中的網頁內容發送訊息。
     *
     * 訊息透過 JavaScript postMessage 傳遞。僅在目前瀏覽器為 WebView 時有效；
     * 否則記錄警告。
     *
     * @param message 要發送至網頁內容的訊息字串
     */
    @JvmStatic
    fun sendPostMessage(message: String) {
        BrowserLogger.d(SUBTAG, "sendPostMessage() 開始 | messageLen=${message.length}")
        val browser = currentBrowser
        if (browser is WebViewBrowser) {
            browser.sendPostMessage(message)
            BrowserLogger.d(SUBTAG, "sendPostMessage() 完成")
        } else {
            BrowserLogger.w(SUBTAG, "sendPostMessage only available for WebView type")
        }
    }

    /**
     * 檢查是否有任何瀏覽器實例目前開啟中。
     *
     * @return 若瀏覽器開啟中則回傳 true，否則回傳 false
     */
    @JvmStatic
    fun isOpen(): Boolean {
        BrowserLogger.d(SUBTAG, "isOpen() 開始")
        val result = currentBrowser?.isOpen() ?: false
        BrowserLogger.d(SUBTAG, "isOpen() 完成 | result=$result")
        return result
    }

    /**
     * 取得目前開啟的瀏覽器類型，若無則回傳 null。
     */
    fun getCurrentBrowserType(): BrowserType? {
        BrowserLogger.d(SUBTAG, "getCurrentBrowserType() 開始")
        val result = currentBrowserType
        BrowserLogger.d(SUBTAG, "getCurrentBrowserType() 完成 | type=$result")
        return result
    }

    /**
     * 建立瀏覽器實例但不開啟（供測試使用）。
     */
    fun createBrowser(type: BrowserType): IBrowser {
        BrowserLogger.d(SUBTAG, "createBrowser() 開始 | type=$type")
        val activity = activityRef?.get()
        requireNotNull(activity) { "Activity not initialized" }
        val browser = createBrowser(type, activity)
        BrowserLogger.d(SUBTAG, "createBrowser() 完成 | type=$type")
        return browser
    }

    // --- 內部輔助方法 ---

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
     * 將瀏覽器類型字串解析為 [BrowserType] 列舉。
     * 無法識別的值則降級為 WEBVIEW。
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
     * 將 JSON 設定字串解析為 [BrowserConfig]。
     *
     * 預期的 JSON 格式對應 C# BrowserConfig.ToJson() 的輸出：
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
