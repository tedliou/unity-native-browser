package com.tedliou.android.browser.system

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.annotation.MainThread
import com.tedliou.android.browser.core.BrowserCallback
import com.tedliou.android.browser.core.BrowserConfig
import com.tedliou.android.browser.core.BrowserException
import com.tedliou.android.browser.core.IBrowser
import com.tedliou.android.browser.util.BrowserLogger
import java.lang.ref.WeakReference

/**
 * System browser implementation that launches the device's default browser.
 *
 * Opens URLs in the system default browser via Intent.ACTION_VIEW. Cannot control
 * the browser after launch (no close, refresh, or page lifecycle callbacks).
 * All lifecycle methods beyond [open] are no-ops or log warnings.
 *
 * Useful for deep linking and analytics redirection where native browser control
 * is not required.
 */
class SystemBrowser(activity: Activity) : IBrowser {
    private val activityRef = WeakReference(activity)
    private var callback: BrowserCallback? = null

    /**
     * Opens the URL in the system default browser via Intent.ACTION_VIEW.
     *
     * Validates that the URL uses http:// or https:// scheme before launching.
     * Immediately calls [BrowserCallback.onClosed] after successful launch since
     * the system browser is not controllable.
     *
     * @param config [BrowserConfig] containing the URL to open (width, height, alignment ignored)
     * @param callback [BrowserCallback] for error/closed events
     */
    override fun open(config: BrowserConfig, callback: BrowserCallback) {
        BrowserLogger.d(SUBTAG, "Opening system browser with URL: ${config.url}")
        this.callback = callback

        // Validate URL scheme
        if (!config.url.startsWith("http://") && !config.url.startsWith("https://")) {
            BrowserLogger.w(SUBTAG, "Invalid URL scheme: ${config.url}")
            callback.onError(BrowserException.InvalidUrlException(config.url))
            return
        }

        val activity = activityRef.get()
        if (activity == null) {
            BrowserLogger.w(SUBTAG, "Activity reference lost; cannot open system browser")
            callback.onError(
                BrowserException.NotAvailableException("Activity reference is null")
            )
            return
        }

        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(config.url))
            activity.startActivity(intent)
            BrowserLogger.d(SUBTAG, "System browser launched successfully")
            // Immediately report closed since we have no control after launch
            callback.onClosed()
        } catch (e: Exception) {
            when (e) {
                is android.content.ActivityNotFoundException -> {
                    BrowserLogger.e(SUBTAG, "No browser installed on device", e)
                    callback.onError(
                        BrowserException.PageLoadException(
                            "No browser installed on device",
                            config.url,
                            e
                        )
                    )
                }
                else -> {
                    BrowserLogger.e(SUBTAG, "Failed to launch system browser", e)
                    callback.onError(
                        BrowserException.PageLoadException(
                            "Failed to launch system browser: ${e.message}",
                            config.url,
                            e
                        )
                    )
                }
            }
        }
    }

    /**
     * Closes the system browser.
     *
     * No-op since the system browser is not controllable after launch.
     * Logs a warning to document the limitation.
     */
    override fun close() {
        BrowserLogger.w(SUBTAG, "Cannot close system browser programmatically")
    }

    /**
     * Refreshes the current page in the system browser.
     *
     * No-op since the system browser is not controllable after launch.
     * Logs a warning to document the limitation.
     */
    override fun refresh() {
        BrowserLogger.w(SUBTAG, "Cannot refresh system browser programmatically")
    }

    /**
     * Checks if the system browser is currently open.
     *
     * Always returns false since we have no control or visibility into
     * the system browser lifecycle after launch.
     *
     * @return false always (system browser state is unknown)
     */
    override fun isOpen(): Boolean {
        return false
    }

    /**
     * Destroys the system browser instance.
     *
     * No-op since there are no resources to clean up. The system browser
     * is managed entirely by Android and the user.
     */
    override fun destroy() {
        BrowserLogger.d(SUBTAG, "SystemBrowser destroyed")
        callback = null
    }

    companion object {
        private const val SUBTAG = "SystemBrowser"
    }
}
