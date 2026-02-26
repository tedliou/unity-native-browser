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
 * Custom Tabs browser implementation for Android.
 *
 * Launches a Custom Tab (in-app browser with customizable toolbar) using the
 * default browser's Custom Tabs service. Provides toolbar color customization,
 * animations, and service pre-warming for faster launch times.
 *
 * Custom Tabs run in the browser process and cannot be programmatically closed
 * or refreshed. Only launch and configuration are supported.
 *
 * Gracefully falls back to system browser (ACTION_VIEW) if no Custom Tabs-capable
 * browser is installed.
 */
class CustomTabBrowser(activity: Activity) : IBrowser {
    private val activityRef = WeakReference(activity)
    private val connectionManager = CustomTabConnectionManager(activity)
    private var callback: BrowserCallback? = null
    private var isCustomTabOpen = false
    private var currentUrl: String? = null

    /**
     * Opens a Custom Tab with the provided configuration.
     *
     * Builds CustomTabsIntent with toolbar color, animations, title display,
     * and share menu. If a Custom Tabs-capable browser is available, pre-warms
     * the service and launches the tab. Otherwise, falls back to system browser.
     *
     * Custom Tabs configuration ignores: width, height, alignment, closeOnTapOutside,
     * deepLinkPatterns, enableJavaScript, userAgent (not supported by Custom Tabs API).
     *
     * @param config BrowserConfig containing URL and optional toolbar color
     * @param callback BrowserCallback for lifecycle events (onPageStarted, onError)
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
     * Closes the Custom Tab.
     *
     * LIMITATION: Custom Tabs cannot be closed programmatically. This method
     * logs a warning and marks the internal state as closed, but the tab
     * remains open until the user dismisses it manually.
     */
    override fun close() {
        runOnUiThread {
            BrowserLogger.w(SUBTAG, "Custom Tabs cannot be closed programmatically; user must dismiss manually")
            isCustomTabOpen = false
            currentUrl = null
        }
    }

    /**
     * Refreshes the current page.
     *
     * LIMITATION: Custom Tabs cannot be refreshed programmatically. This method
     * logs a warning and performs no action.
     */
    override fun refresh() {
        runOnUiThread {
            BrowserLogger.w(SUBTAG, "Custom Tabs cannot be refreshed programmatically")
        }
    }

    /**
     * Checks if a Custom Tab is currently open.
     *
     * NOTE: This tracks internal state only. Custom Tabs run in a separate
     * process and do not provide lifecycle callbacks for user dismissal.
     *
     * @return true if Custom Tab was launched and not yet closed via destroy()
     */
    override fun isOpen(): Boolean = isCustomTabOpen

    /**
     * Destroys the Custom Tab browser and releases resources.
     *
     * Unbinds the Custom Tabs service connection and resets state. The Custom Tab
     * itself remains open if the user has not dismissed it.
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
