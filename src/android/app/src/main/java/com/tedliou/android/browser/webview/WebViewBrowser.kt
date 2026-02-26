package com.tedliou.android.browser.webview

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Looper
import android.view.ViewGroup
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
 * WebView-based browser implementation for Unity overlay rendering.
 *
 * Creates a WebView using applicationContext to prevent Activity leaks, attaches it
 * to the Activity decorView as an overlay, and dispatches lifecycle callbacks.
 */
class WebViewBrowser(activity: Activity) : IBrowser {
    private val activityRef = WeakReference(activity)
    private var webView: WebView? = null
    private var callback: BrowserCallback? = null

    /**
     * Opens the WebView with the provided configuration.
     *
     * Creates a new WebView, applies settings, adds it to the decorView overlay,
     * and loads the specified URL.
     */
    override fun open(config: BrowserConfig, callback: BrowserCallback) {
        runOnUiThread {
            BrowserLogger.d(SUBTAG, "Opening WebView with URL: ${config.url}")
            this.callback = callback
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
            val createdWebView = WebView(activity.applicationContext)
            configureWebView(createdWebView, config)
            attachToDecorView(activity, createdWebView)
            createdWebView.loadUrl(config.url)
            webView = createdWebView
        }
    }

    /**
     * Closes the current WebView instance and notifies [BrowserCallback.onClosed].
     */
    override fun close() {
        runOnUiThread {
            BrowserLogger.d(SUBTAG, "Closing WebView")
            closeInternal(notifyClosed = true)
        }
    }

    /**
     * Reloads the currently loaded page if WebView is open.
     */
    override fun refresh() {
        runOnUiThread {
            BrowserLogger.d(SUBTAG, "Refreshing WebView")
            webView?.reload()
        }
    }

    /**
     * Returns true if the WebView is attached to the window.
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
     * Destroys the WebView instance and releases all resources.
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
        (current.parent as? ViewGroup)?.removeView(current)
        current.stopLoading()
        current.clearHistory()
        webView = null
        if (notifyClosed) {
            callback?.onClosed()
        }
    }

    @MainThread
    private fun destroyInternal() {
        val current = webView ?: return
        (current.parent as? ViewGroup)?.removeView(current)
        current.stopLoading()
        current.clearHistory()
        current.removeAllViews()
        current.destroy()
        webView = null
    }

    @MainThread
    private fun attachToDecorView(activity: Activity, view: WebView) {
        val root = activity.window?.decorView as? ViewGroup
        if (root == null) {
            BrowserLogger.e(SUBTAG, "DecorView is not a ViewGroup; cannot attach WebView")
            callback?.onError(
                BrowserException.InitializationException("DecorView is not a ViewGroup")
            )
            return
        }
        val layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        root.addView(view, layoutParams)
    }

    @MainThread
    private fun configureWebView(view: WebView, config: BrowserConfig) {
        configureSettings(view.settings, config)
        view.webViewClient = createWebViewClient()
        view.webChromeClient = createWebChromeClient()
    }

    /**
     * Enables JavaScript because the WebView loads a Unity-specified URL and
     * WebView-based features (postMessage, JS execution) require it.
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

    private companion object {
        private const val SUBTAG = "WebView"
        private const val UNKNOWN_ERROR_CODE = -1
    }
}
