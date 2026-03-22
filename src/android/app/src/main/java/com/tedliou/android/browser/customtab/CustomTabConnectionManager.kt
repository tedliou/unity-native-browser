package com.tedliou.android.browser.customtab

import android.app.Activity
import android.content.ComponentName
import android.net.Uri
import androidx.browser.customtabs.CustomTabsCallback
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession
import com.tedliou.android.browser.util.BrowserLogger
import java.lang.ref.WeakReference

/**
 * 管理 Custom Tabs 服務連線生命週期，以優化效能。
 *
 * 提供預熱（warmup）與 URL 預取（mayLaunchUrl）功能，降低 Custom Tab 啟動延遲。
 * 透過服務回調追蹤連線狀態。
 *
 * 使用範例：
 * ```kotlin
 * val manager = CustomTabConnectionManager(activity)
 * manager.bindService(onConnected = {
 *     manager.warmup()
 *     manager.mayLaunchUrl("https://example.com")
 * })
 * manager.unbindService()
 * ```
 */
class CustomTabConnectionManager(activity: Activity) {
    private val activityRef = WeakReference(activity)
    private var client: CustomTabsClient? = null
    private var session: CustomTabsSession? = null
    private var connection: CustomTabsServiceConnection? = null
    private var isServiceBound = false

    /**
     * 綁定至預設瀏覽器的 Custom Tabs 服務。
     *
     * 非同步連線至服務；連線成功後呼叫 onConnected 回調。
     * 若找不到支援 Custom Tabs 的瀏覽器，則記錄警告並不呼叫回調。
     *
     * @param onConnected 服務連線成功後呼叫的回調
     */
    fun bindService(onConnected: () -> Unit) {
        val activity = activityRef.get()
        if (activity == null) {
            BrowserLogger.w(SUBTAG, "Activity reference lost; cannot bind Custom Tabs service")
            return
        }

        val packageName = CustomTabsClient.getPackageName(activity, null)
        if (packageName == null) {
            BrowserLogger.w(SUBTAG, "No Custom Tabs-capable browser found; skipping service bind")
            return
        }

        BrowserLogger.d(SUBTAG, "Binding to Custom Tabs service: $packageName")
        val serviceConnection = object : CustomTabsServiceConnection() {
            override fun onCustomTabsServiceConnected(
                name: ComponentName,
                customTabsClient: CustomTabsClient
            ) {
                BrowserLogger.d(SUBTAG, "Custom Tabs service connected: ${name.flattenToShortString()}")
                client = customTabsClient
                session = customTabsClient.newSession(object : CustomTabsCallback() {
                    override fun onNavigationEvent(navigationEvent: Int, extras: android.os.Bundle?) {
                        super.onNavigationEvent(navigationEvent, extras)
                        BrowserLogger.v(SUBTAG, "Navigation event: $navigationEvent")
                    }
                })
                isServiceBound = true
                onConnected()
            }

            override fun onServiceDisconnected(name: ComponentName) {
                BrowserLogger.w(SUBTAG, "Custom Tabs service disconnected: ${name.flattenToShortString()}")
                client = null
                session = null
                isServiceBound = false
            }
        }

        connection = serviceConnection
        CustomTabsClient.bindCustomTabsService(activity, packageName, serviceConnection)
    }

    /**
     * 解除 Custom Tabs 服務綁定並釋放資源。
     *
     * 應在不再需要 Custom Tabs 時呼叫（例如 Activity 的 onDestroy）。
     * 若服務從未綁定，則忽略此呼叫。
     */
    fun unbindService() {
        val activity = activityRef.get()
        if (activity == null) {
            BrowserLogger.w(SUBTAG, "Activity reference lost; cannot unbind service")
            return
        }
        connection?.let { conn ->
            if (isServiceBound) {
                BrowserLogger.d(SUBTAG, "Unbinding Custom Tabs service")
                try {
                    activity.unbindService(conn)
                } catch (e: IllegalArgumentException) {
                    // Service was not registered or already unbound
                    BrowserLogger.w(SUBTAG, "Service was not bound or already unbound")
                }
                isServiceBound = false
            }
        }
        connection = null
        client = null
        session = null
    }

    /**
     * 預熱瀏覽器程序以降低 Custom Tab 啟動延遲。
     *
     * 應在服務連線成功後呼叫。若預熱成功則回傳 true，
     * 若客戶端未連線或預熱失敗則回傳 false。
     *
     * @return 預熱成功回傳 true，否則回傳 false
     */
    fun warmup(): Boolean {
        val customTabsClient = client
        if (customTabsClient == null) {
            BrowserLogger.w(SUBTAG, "Client not connected; cannot warmup")
            return false
        }
        val success = customTabsClient.warmup(0L)
        if (success) {
            BrowserLogger.d(SUBTAG, "Browser warmup succeeded")
        } else {
            BrowserLogger.w(SUBTAG, "Browser warmup failed")
        }
        return success
    }

    /**
     * 提示瀏覽器預取指定 URL 以加快載入速度。
     *
     * 應在服務連線成功並完成預熱後呼叫。瀏覽器可能忽略此提示。
     * 若提示被接受則回傳 true。
     *
     * @param url 要預取的 URL
     * @return 瀏覽器接受提示回傳 true，若 session 未就緒或提示被拒絕則回傳 false
     */
    fun mayLaunchUrl(url: String): Boolean {
        val customTabsSession = session
        if (customTabsSession == null) {
            BrowserLogger.w(SUBTAG, "Session not created; cannot pre-fetch URL")
            return false
        }
        val uri = Uri.parse(url)
        val success = customTabsSession.mayLaunchUrl(uri, null, null)
        if (success) {
            BrowserLogger.d(SUBTAG, "Pre-fetch hint sent for URL: $url")
        } else {
            BrowserLogger.w(SUBTAG, "Pre-fetch hint rejected for URL: $url")
        }
        return success
    }

    /**
     * 檢查 Custom Tabs 服務是否已綁定並就緒。
     *
     * @return 服務已綁定且客戶端可用時回傳 true，否則回傳 false
     */
    fun isServiceConnected(): Boolean = isServiceBound && client != null

    /**
     * 取得目前的 Custom Tabs session。
     *
     * 若服務未連線或 session 尚未建立則回傳 null。
     * Session 在啟動 Custom Tab 時使用，以關聯至預熱的瀏覽器實例。
     *
     * @return 目前的 session，若不可用則回傳 null
     */
    fun getSession(): CustomTabsSession? = session

    private companion object {
        private const val SUBTAG = "CustomTab"
    }
}
