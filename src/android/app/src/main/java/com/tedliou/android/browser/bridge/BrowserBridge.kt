package com.tedliou.android.browser.bridge

import android.app.Activity
import android.util.Log
import java.lang.ref.WeakReference

/**
 * Bridge singleton for Unity C# ↔ Android Kotlin communication.
 *
 * Handles:
 * - Activity reference management (weak reference to prevent memory leaks)
 * - Thread marshalling (GL thread → UI thread via Activity.runOnUiThread)
 * - UnityPlayer discovery via reflection (no compile-time Unity dependency)
 * - UnitySendMessage invocation for C# callback delivery
 *
 * Usage:
 * ```kotlin
 * BrowserBridge.initialize(activity)
 * BrowserBridge.sendToUnity("OnPageStarted", "{\"url\": \"...\"}")
 * ```
 */
object BrowserBridge {
    private const val TAG = "BrowserBridge"
    private const val UNITY_PLAYER_CLASS = "com.unity3d.player.UnityPlayer"
    private const val UNITY_SEND_MESSAGE_METHOD = "UnitySendMessage"

    private var activityRef: WeakReference<Activity>? = null

    /**
     * Default game object name to receive Unity callbacks.
     *
     * Can be overridden before calling [sendToUnity].
     */
    var gameObjectName: String = "NativeBrowserCallback"

    /**
     * Initialize the bridge with the current Activity.
     *
     * Must be called from Unity before any browser operations.
     *
     * @param activity The Android Activity instance from Unity
     */
    fun initialize(activity: Activity) {
        activityRef = WeakReference(activity)
        Log.d(TAG, "Bridge initialized with activity: ${activity.javaClass.simpleName}")
    }

    /**
     * Send a message to Unity C# via UnitySendMessage reflection.
     *
     * Thread-safe: automatically marshals calls from any thread to the UI thread.
     * If Activity is not available or UnityPlayer is not found, logs a warning instead of crashing.
     *
     * @param methodName The C# method name to invoke (e.g., "OnPageStarted")
     * @param message JSON-encoded message string to pass to C# method
     */
    fun sendToUnity(methodName: String, message: String) {
        val activity = activityRef?.get()
        if (activity == null) {
            Log.w(TAG, "Activity reference not available (GC collected or not initialized)")
            return
        }

        activity.runOnUiThread {
            try {
                val unityPlayerClass = Class.forName(UNITY_PLAYER_CLASS)
                val method = unityPlayerClass.getMethod(
                    UNITY_SEND_MESSAGE_METHOD,
                    String::class.java,
                    String::class.java,
                    String::class.java
                )
                method.invoke(null, gameObjectName, methodName, message)
                Log.v(TAG, "Sent to Unity: $gameObjectName.$methodName with message: $message")
            } catch (e: ClassNotFoundException) {
                Log.w(TAG, "UnityPlayer not found (standalone Android test mode?). Message not sent.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to invoke UnitySendMessage: ${e.message}", e)
            }
        }
    }
}
