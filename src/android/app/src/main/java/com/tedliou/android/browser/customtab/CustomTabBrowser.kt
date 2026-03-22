package com.tedliou.android.browser.customtab

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Looper
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsClient
import com.tedliou.android.browser.core.BrowserCallback
import com.tedliou.android.browser.core.BrowserConfig
import com.tedliou.android.browser.core.BrowserException
import com.tedliou.android.browser.core.IBrowser
import com.tedliou.android.browser.util.BrowserLogger
import java.lang.ref.WeakReference

/**
 * Android Custom Tabs 瀏覽器實作。
 *
 * 使用預設瀏覽器的 Custom Tabs 服務啟動 Custom Tab（可自訂工具列的應用內瀏覽器）。
 * 支援工具列顏色自訂、動畫效果，以及服務預熱以加快啟動速度。
 *
 * Custom Tabs 在瀏覽器程序中執行，無法以程式方式關閉或重新整理。
 * 僅支援啟動與設定操作。
 *
 * 若未安裝支援 Custom Tabs 的瀏覽器，則優雅地降級至系統瀏覽器（ACTION_VIEW）。
 */
class CustomTabBrowser(activity: Activity) : IBrowser {
    private val activityRef = WeakReference(activity)
    private val connectionManager = CustomTabConnectionManager(activity)
    private var callback: BrowserCallback? = null
    private var isCustomTabOpen = false
    private var currentUrl: String? = null

    /**
     * 以指定設定開啟 Custom Tab。
     *
     * 建立含工具列顏色、動畫、標題顯示及分享選單的 CustomTabsIntent。
     * 若有支援 Custom Tabs 的瀏覽器，則預熱服務並啟動分頁；否則降級至系統瀏覽器。
     *
     * Custom Tabs 設定不支援以下欄位：width、height、alignment、closeOnTapOutside、
     * deepLinkPatterns、enableJavaScript、userAgent（Custom Tabs API 不支援）。
     *
     * @param config 包含 URL 及可選工具列顏色的 BrowserConfig
     * @param callback 用於生命週期事件的 BrowserCallback（onPageStarted、onError）
     */
    override fun open(config: BrowserConfig, callback: BrowserCallback) {
        runOnUiThread {
            BrowserLogger.d(SUBTAG, "Opening Custom Tab with URL: ${config.url}")
            this.callback = callback
            this.currentUrl = config.url

            val activity = activityRef.get()
            if (activity == null) {
                BrowserLogger.w(SUBTAG, "Activity reference lost; cannot open Custom Tab")
                callback.onError(
                    BrowserException.NotAvailableException("Activity reference is null")
                )
                return@runOnUiThread
            }

            val packageName = CustomTabsClient.getPackageName(activity, null)
            if (packageName == null) {
                BrowserLogger.w(SUBTAG, "No Custom Tabs support detected; falling back to system browser")
                fallbackToSystemBrowser(activity, config.url, callback)
                return@runOnUiThread
            }

            connectionManager.bindService {
                connectionManager.warmup()
                connectionManager.mayLaunchUrl(config.url)
                launchCustomTab(activity, config, callback)
            }
        }
    }

    /**
     * 關閉 Custom Tab。
     *
     * 限制：Custom Tabs 無法以程式方式關閉。此方法記錄警告並將內部狀態標記為已關閉，
     * 但分頁仍保持開啟，直到使用者手動關閉為止。
     */
    override fun close() {
        runOnUiThread {
            BrowserLogger.w(SUBTAG, "Custom Tabs cannot be closed programmatically; user must dismiss manually")
            isCustomTabOpen = false
            currentUrl = null
        }
    }

    /**
     * 重新整理目前頁面。
     *
     * 限制：Custom Tabs 無法以程式方式重新整理。此方法記錄警告並不執行任何操作。
     */
    override fun refresh() {
        runOnUiThread {
            BrowserLogger.w(SUBTAG, "Custom Tabs cannot be refreshed programmatically")
        }
    }

    /**
     * 檢查 Custom Tab 是否目前開啟中。
     *
     * 注意：此方法僅追蹤內部狀態。Custom Tabs 在獨立程序中執行，
     * 不提供使用者關閉時的生命週期回調。
     *
     * @return 若 Custom Tab 已啟動且尚未透過 destroy() 關閉則回傳 true
     */
    override fun isOpen(): Boolean = isCustomTabOpen

    /**
     * 銷毀 Custom Tab 瀏覽器並釋放資源。
     *
     * 解除 Custom Tabs 服務連線並重置狀態。若使用者尚未關閉，Custom Tab 本身仍保持開啟。
     */
    override fun destroy() {
        runOnUiThread {
            BrowserLogger.d(SUBTAG, "Destroying Custom Tab browser")
            connectionManager.unbindService()
            isCustomTabOpen = false
            currentUrl = null
            callback = null
        }
    }

    private fun launchCustomTab(activity: Activity, config: BrowserConfig, callback: BrowserCallback) {
        try {
            val builder = CustomTabsIntent.Builder(connectionManager.getSession())

            val defaultColorScheme = CustomTabColorSchemeParams.Builder()
                .setToolbarColor(DEFAULT_TOOLBAR_COLOR)
                .build()
            builder.setDefaultColorSchemeParams(defaultColorScheme)

            builder.setShowTitle(true)
            builder.setShareState(CustomTabsIntent.SHARE_STATE_ON)

            builder.setStartAnimations(activity, android.R.anim.fade_in, android.R.anim.fade_out)
            builder.setExitAnimations(activity, android.R.anim.fade_in, android.R.anim.fade_out)

            val customTabsIntent = builder.build()
            val uri = Uri.parse(config.url)

            callback.onPageStarted(config.url)
            customTabsIntent.launchUrl(activity, uri)
            isCustomTabOpen = true

            BrowserLogger.d(SUBTAG, "Custom Tab launched successfully")
        } catch (e: ActivityNotFoundException) {
            BrowserLogger.e(SUBTAG, "No browser activity found; falling back to system browser", e)
            callback.onError(
                BrowserException.NotAvailableException("No browser available to handle URL")
            )
            fallbackToSystemBrowser(activity, config.url, callback)
        } catch (e: Exception) {
            BrowserLogger.e(SUBTAG, "Failed to launch Custom Tab", e)
            callback.onError(
                BrowserException.PageLoadException("Failed to launch Custom Tab: ${e.message}", config.url)
            )
        }
    }

    private fun fallbackToSystemBrowser(activity: Activity, url: String, callback: BrowserCallback) {
        try {
            BrowserLogger.i(SUBTAG, "Launching system browser with URL: $url")
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            callback.onPageStarted(url)
            activity.startActivity(intent)
            isCustomTabOpen = false

            BrowserLogger.d(SUBTAG, "System browser launched successfully")
        } catch (e: ActivityNotFoundException) {
            BrowserLogger.e(SUBTAG, "No activity found to handle ACTION_VIEW", e)
            callback.onError(
                BrowserException.NotAvailableException("No browser installed on device")
            )
        } catch (e: Exception) {
            BrowserLogger.e(SUBTAG, "Failed to launch system browser", e)
            callback.onError(
                BrowserException.PageLoadException("Failed to launch system browser: ${e.message}", url)
            )
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
        private const val SUBTAG = "CustomTab"
        private const val DEFAULT_TOOLBAR_COLOR = 0xFF6200EE.toInt()
    }
}
