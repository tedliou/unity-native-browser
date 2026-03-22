package com.tedliou.android.browser.bridge

import android.app.Activity
import com.tedliou.android.browser.util.BrowserLogger
import java.lang.ref.WeakReference

/**
 * Unity C# ↔ Android Kotlin 通訊橋接單例。
 *
 * 負責：
 * - Activity 引用管理（弱引用，防止記憶體洩漏）
 * - 執行緒調度（GL 執行緒 → UI 執行緒，透過 Activity.runOnUiThread）
 * - 透過反射發現 UnityPlayer（無需編譯期 Unity 依賴）
 * - 呼叫 UnitySendMessage 以傳遞 C# 回調
 *
 * 使用範例：
 * ```kotlin
 * BrowserBridge.initialize(activity)
 * BrowserBridge.sendToUnity("OnPageStarted", "{\"url\": \"...\"}")
 * ```
 */
object BrowserBridge {

    private var activityRef: WeakReference<Activity>? = null

    /**
     * Unity 訊息發送器，預設為 [ReflectionUnitySender]。
     *
     * 可替換為測試替身以進行單元測試。
     */
    var sender: IUnitySender = ReflectionUnitySender()

    /**
     * 接收 Unity 回調的預設遊戲物件名稱。
     *
     * 可在呼叫 [sendToUnity] 前覆寫此值。
     */
    var gameObjectName: String = "NativeBrowserCallback"

    /**
     * 以當前 Activity 初始化橋接器。
     *
     * 必須在任何瀏覽器操作前從 Unity 呼叫。
     *
     * @param activity 來自 Unity 的 Android Activity 實例
     */
    fun initialize(activity: Activity) {
        activityRef = WeakReference(activity)
        BrowserLogger.d("Bridge", "Bridge initialized with activity: ${activity.javaClass.simpleName}")
    }

    /**
     * 透過 UnitySendMessage 反射向 Unity C# 發送訊息。
     *
     * 執行緒安全：自動將任意執行緒的呼叫調度至 UI 執行緒。
     * 若 Activity 不可用或找不到 UnityPlayer，則記錄警告而非拋出例外。
     *
     * @param methodName 要呼叫的 C# 方法名稱（例如 "OnPageStarted"）
     * @param message 傳遞給 C# 方法的 JSON 編碼訊息字串
     */
    fun sendToUnity(methodName: String, message: String) {
        val activity = activityRef?.get()
        if (activity == null) {
            BrowserLogger.w("Bridge", "Activity reference not available (GC collected or not initialized)")
            return
        }

        BrowserLogger.d("Bridge", "sendToUnity() | method=$methodName gameObject=$gameObjectName msgLen=${message.length}")
        activity.runOnUiThread {
            sender.send(gameObjectName, methodName, message)
        }
    }
}
