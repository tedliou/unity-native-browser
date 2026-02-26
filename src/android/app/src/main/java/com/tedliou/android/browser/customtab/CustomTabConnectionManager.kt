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
 * Manages Custom Tabs service connection lifecycle for performance optimization.
 *
 * Provides warmup and URL pre-fetching (mayLaunchUrl) capabilities to reduce
 * Custom Tab launch latency. Tracks connection state via service callbacks.
 *
 * Usage:
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
     * Binds to the Custom Tabs service of the default browser.
     *
     * Asynchronously connects to the service; onConnected callback is invoked
     * when connection succeeds. If no Custom Tabs-capable browser is found,
     * logs a warning and does not invoke the callback.
     *
     * @param onConnected Callback invoked after successful service connection
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
     * Unbinds from the Custom Tabs service and releases resources.
     *
     * Should be called when Custom Tabs are no longer needed (e.g., Activity onDestroy).
     * Ignores call if service was never bound.
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
     * Warms up the browser process to reduce Custom Tab launch latency.
     *
     * Should be called after successful service connection. Returns true if warmup
     * succeeded, false if client is not connected or warmup failed.
     *
     * @return true if warmup succeeded, false otherwise
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
     * Hints the browser to pre-fetch the specified URL for faster loading.
     *
     * Should be called after successful service connection and warmup. The browser
     * may choose to ignore this hint. Returns true if hint was accepted.
     *
     * @param url The URL to pre-fetch
     * @return true if browser accepted the hint, false if session not ready or hint rejected
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
     * Checks if the Custom Tabs service is currently bound and ready.
     *
     * @return true if service is bound and client is available, false otherwise
     */
    fun isServiceConnected(): Boolean = isServiceBound && client != null

    /**
     * Gets the current Custom Tabs session.
     *
     * Returns null if service is not connected or session was not created.
     * The session is used when launching Custom Tabs to associate with the
     * pre-warmed browser instance.
     *
     * @return The current session, or null if unavailable
     */
    fun getSession(): CustomTabsSession? = session

    private companion object {
        private const val SUBTAG = "CustomTab"
    }
}
