package com.tedliou.android.browser.webview

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.MainThread
import com.tedliou.android.browser.core.BrowserCallback
import com.tedliou.android.browser.core.BrowserConfig
import com.tedliou.android.browser.core.BrowserException
import com.tedliou.android.browser.core.IBrowser
import com.tedliou.android.browser.util.BrowserLogger
import java.lang.ref.WeakReference
import java.util.concurrent.CountDownLatch

/**
 * 基於 WebView 的瀏覽器實作，用於 Unity 覆蓋層渲染。
 *
 * 使用 applicationContext 建立 WebView 以防止 Activity 記憶體洩漏，
 * 將其作為覆蓋層附加至 Activity 的 decorView，並分派生命週期回呼。
 */
class WebViewBrowser(activity: Activity) : IBrowser {
    private val activityRef = WeakReference(activity)
    private var webView: WebView? = null
    private var callback: BrowserCallback? = null
    private var config: BrowserConfig? = null
    private var jsBridge: JsBridge? = null
    private var overlayView: View? = null
    private var backPressContainer: BackPressInterceptLayout? = null
    private var lifecycleCallbacks: Application.ActivityLifecycleCallbacks? = null

    /**
     * 使用指定設定開啟 WebView。
     *
     * 建立新的 WebView、套用設定、將其加入 decorView 覆蓋層，
     * 並載入指定的 URL。
     */
    override fun open(config: BrowserConfig, callback: BrowserCallback) {
        runOnUiThread {
            BrowserLogger.d(SUBTAG, "Opening WebView with URL: ${config.url}")
            this.callback = callback
            this.config = config
            if (webView != null) {
                BrowserLogger.w(SUBTAG, "WebView already open; closing before reopen")
                closeInternal(notifyClosed = false)
            }
            val activity = activityRef.get()
            if (activity == null) {
                BrowserLogger.w(SUBTAG, "Activity reference lost; cannot open WebView")
                callback.onError(
                    BrowserException.NotAvailableException("Activity reference is null")
                )
                return@runOnUiThread
            }
            jsBridge = JsBridge(activity, callback)
            val createdWebView = WebView(activity.applicationContext)
            configureWebView(createdWebView, config)
            jsBridge?.addJavaScriptInterface(createdWebView)
            val layoutParams = WebViewLayoutManager.calculateLayoutParams(
                config,
                activity.resources.displayMetrics,
            )
            val overlay = WebViewLayoutManager.createOverlayView(activity, config) {
                close()
            }
            attachToDecorView(activity, createdWebView, layoutParams, overlay)
            overlayView = overlay
            createdWebView.loadUrl(config.url)
            webView = createdWebView
            // Request focus on the back press container so it receives key events
            backPressContainer?.requestFocus()
            setupLifecycleHandling(activity)
        }
    }

    fun updateLayout(config: BrowserConfig) {
        runOnUiThread {
            val activity = activityRef.get()
            if (activity == null) {
                BrowserLogger.w(SUBTAG, "Activity reference lost; cannot update layout")
                return@runOnUiThread
            }
            val currentWebView = webView ?: return@runOnUiThread
            val layoutParams = WebViewLayoutManager.calculateLayoutParams(
                config,
                activity.resources.displayMetrics,
            )
            currentWebView.layoutParams = layoutParams
            val container = backPressContainer ?: return@runOnUiThread
            val overlay = WebViewLayoutManager.createOverlayView(activity, config) {
                close()
            }
            if (overlay == null) {
                overlayView?.let { existing ->
                    container.removeView(existing)
                }
                overlayView = null
            } else if (overlayView == null) {
                overlayView = overlay
                container.addView(overlay, 0)
            }
        }
    }

    /**
     * 關閉目前的 WebView 實例並通知 [BrowserCallback.onClosed]。
     */
    override fun close() {
        runOnUiThread {
            BrowserLogger.d(SUBTAG, "Closing WebView")
            closeInternal(notifyClosed = true)
        }
    }

    /**
     * 若 WebView 已開啟，重新載入目前頁面。
     */
    override fun refresh() {
        runOnUiThread {
            BrowserLogger.d(SUBTAG, "Refreshing WebView")
            webView?.reload()
        }
    }

    fun executeJavaScript(script: String, requestId: String?) {
        val current = webView
        val bridge = jsBridge
        if (current == null || bridge == null) {
            BrowserLogger.w(SUBTAG, "WebView not initialized; cannot execute JavaScript")
            requestId?.let { callback?.onJsResult(it, null) }
            return
        }
        val safeRequestId = requestId ?: run {
            BrowserLogger.w(SUBTAG, "requestId is null; using default id")
            DEFAULT_REQUEST_ID
        }
        bridge.executeJavaScript(current, script, safeRequestId)
    }

    fun injectJavaScript(script: String) {
        val current = webView
        val bridge = jsBridge
        if (current == null || bridge == null) {
            BrowserLogger.w(SUBTAG, "WebView not initialized; cannot inject JavaScript")
            return
        }
        bridge.injectJavaScript(current, script)
    }

    /**
     * 透過 JavaScript postMessage 從 Unity 傳送訊息至網頁內容。
     *
     * @param message 要傳送至網頁內容的訊息字串
     */
    fun sendPostMessage(message: String) {
        val current = webView
        val bridge = jsBridge
        if (current == null || bridge == null) {
            BrowserLogger.w(SUBTAG, "WebView not initialized; cannot send postMessage")
            return
        }
        bridge.sendPostMessage(current, message)
    }

    /**
     * 若 WebView 已附加至視窗，回傳 `true`。
     */
    override fun isOpen(): Boolean {
        val activity = activityRef.get()
        if (activity == null) {
            BrowserLogger.w(SUBTAG, "Activity reference lost; isOpen returns false")
            return false
        }
        if (Looper.getMainLooper() == Looper.myLooper()) {
            return isWebViewOpen()
        }
        val latch = CountDownLatch(1)
        var result = false
        activity.runOnUiThread {
            result = isWebViewOpen()
            latch.countDown()
        }
        latch.await()
        return result
    }

    /**
     * 銷毀 WebView 實例並釋放所有資源。
     */
    override fun destroy() {
        runOnUiThread {
            BrowserLogger.d(SUBTAG, "Destroying WebView")
            destroyInternal()
        }
    }

    @MainThread
    private fun isWebViewOpen(): Boolean {
        val current = webView
        return current != null && current.isAttachedToWindow
    }

    @MainThread
    private fun closeInternal(notifyClosed: Boolean) {
        val current = webView ?: run {
            if (notifyClosed) {
                callback?.onClosed()
            }
            return
        }
        // Remove the entire back press container (which holds overlay + WebView)
        backPressContainer?.let { container ->
            (container.parent as? ViewGroup)?.removeView(container)
        }
        backPressContainer = null
        overlayView = null
        current.stopLoading()
        current.clearHistory()
        webView = null
        jsBridge = null
        if (notifyClosed) {
            callback?.onClosed()
        }
    }

    @MainThread
    private fun destroyInternal() {
        val current = webView ?: return
        // Remove the entire back press container (which holds overlay + WebView)
        backPressContainer?.let { container ->
            (container.parent as? ViewGroup)?.removeView(container)
        }
        backPressContainer = null
        overlayView = null
        current.stopLoading()
        current.clearHistory()
        current.removeAllViews()
        current.destroy()
        webView = null
        jsBridge = null
        lifecycleCallbacks?.let { callbacks ->
            activityRef.get()?.application?.unregisterActivityLifecycleCallbacks(callbacks)
        }
        lifecycleCallbacks = null
    }

    @MainThread
    private fun attachToDecorView(
        activity: Activity,
        view: WebView,
        layoutParams: FrameLayout.LayoutParams,
        overlay: View?,
    ) {
        val root = activity.window?.decorView as? FrameLayout
        if (root == null) {
            BrowserLogger.e(SUBTAG, "DecorView is not a FrameLayout; cannot attach WebView")
            callback?.onError(
                BrowserException.InitializationException("DecorView is not a FrameLayout")
            )
            return
        }
        // Wrap overlay + WebView in a BackPressInterceptLayout that captures
        // KEYCODE_BACK via dispatchKeyEvent — the only reliable interception point
        // when running inside Unity's GameActivity (native C++ layer eats key events
        // before OnBackPressedDispatcher or Activity.onKeyDown can see them).
        val container = BackPressInterceptLayout(activity) {
            val wv = webView
            if (wv != null && wv.canGoBack()) {
                BrowserLogger.d(SUBTAG, "Back pressed: navigating back in WebView")
                wv.goBack()
            } else {
                BrowserLogger.d(SUBTAG, "Back pressed: closing WebView")
                close()
            }
        }
        container.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        )
        if (overlay != null) {
            container.addView(overlay)
        }
        container.addView(view, layoutParams)
        root.addView(container)
        backPressContainer = container
    }

    @MainThread
    private fun configureWebView(view: WebView, config: BrowserConfig) {
        configureSettings(view.settings, config)
        view.webViewClient = createWebViewClient()
        view.webChromeClient = createWebChromeClient()
    }

    /**
     * 啟用 JavaScript，因為 WebView 載入 Unity 指定的 URL，
     * 且 WebView 功能（postMessage、JS 執行）需要 JavaScript 支援。
     */
    @SuppressLint("SetJavaScriptEnabled")
    @MainThread
    private fun configureSettings(settings: WebSettings, config: BrowserConfig) {
        settings.javaScriptEnabled = config.enableJavaScript
        settings.domStorageEnabled = true
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        if (config.userAgent.isNotBlank()) {
            settings.userAgentString = config.userAgent
        }
    }

    private fun createWebViewClient(): WebViewClient {
        return object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                val safeUrl = url.orEmpty()
                BrowserLogger.d(SUBTAG, "Page started: $safeUrl")
                callback?.onPageStarted(safeUrl)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                val safeUrl = url.orEmpty()
                BrowserLogger.d(SUBTAG, "Page finished: $safeUrl")
                callback?.onPageFinished(safeUrl)
                val currentView = view
                if (currentView != null) {
                    jsBridge?.injectPostMessageBridge(currentView)
                }
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?,
            ) {
                val url = request?.url?.toString().orEmpty()
                val errorCode = error?.errorCode ?: UNKNOWN_ERROR_CODE
                val description = error?.description?.toString().orEmpty()
                BrowserLogger.e(SUBTAG, "Page error [$errorCode]: $description ($url)")
                callback?.onError(
                    BrowserException.PageLoadException(
                        message = "Error $errorCode: $description",
                        url = url,
                    )
                )
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?,
            ): Boolean {
                val url = request?.url?.toString().orEmpty()
                BrowserLogger.v(SUBTAG, "URL loading: $url")
                val currentConfig = config ?: return false
                if (DeepLinkMatcher.matches(url, currentConfig.deepLinkPatterns)) {
                    BrowserLogger.d(SUBTAG, "Deep link intercepted: $url")
                    callback?.onDeepLink(url)
                    if (currentConfig.closeOnDeepLink) {
                        close()
                    }
                    return true
                }
                return false
            }
        }
    }

    private fun createWebChromeClient(): WebChromeClient {
        return object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                BrowserLogger.v(SUBTAG, "Progress: $newProgress%")
            }

            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                val message = consoleMessage?.message().orEmpty()
                BrowserLogger.v(SUBTAG, "Console: $message")
                return super.onConsoleMessage(consoleMessage)
            }
        }
    }

    private fun runOnUiThread(block: () -> Unit) {
        val activity = activityRef.get()
        if (activity == null) {
            BrowserLogger.w(SUBTAG, "Activity reference lost; cannot execute UI operation")
            return
        }
        if (Looper.getMainLooper() == Looper.myLooper()) {
            block()
        } else {
            activity.runOnUiThread(block)
        }
    }


    /**
     * 設定 WebView 的 Activity 生命週期處理。
     *
     * 回應 Activity 生命週期事件：
     * - onPause：暫停 WebView 渲染與計時器
     * - onResume：恢復 WebView 渲染與計時器
     * - onDestroy：銷毀 WebView 並釋放資源
     */
    @MainThread
    private fun setupLifecycleHandling(activity: Activity) {
        val callbacks = object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

            override fun onActivityStarted(activity: Activity) {}

            override fun onActivityResumed(activity: Activity) {
                if (activity === activityRef.get()) {
                    runOnUiThread {
                        BrowserLogger.d(SUBTAG, "Activity resumed, resuming WebView")
                        webView?.onResume()
                        webView?.resumeTimers()
                    }
                }
            }

            override fun onActivityPaused(activity: Activity) {
                if (activity === activityRef.get()) {
                    runOnUiThread {
                        BrowserLogger.d(SUBTAG, "Activity paused, pausing WebView")
                        webView?.onPause()
                        webView?.pauseTimers()
                    }
                }
            }

            override fun onActivityStopped(activity: Activity) {}

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {
                if (activity === activityRef.get()) {
                    BrowserLogger.d(SUBTAG, "Activity destroyed, destroying WebView")
                    destroy()
                }
            }
        }
        activity.application.registerActivityLifecycleCallbacks(callbacks)
        lifecycleCallbacks = callbacks
    }

    private companion object {
        private const val SUBTAG = "WebView"
        private const val UNKNOWN_ERROR_CODE = -1
        private const val DEFAULT_REQUEST_ID = "no_request_id"
    }
}
