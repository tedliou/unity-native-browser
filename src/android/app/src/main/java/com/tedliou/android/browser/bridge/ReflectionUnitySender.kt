package com.tedliou.android.browser.bridge

import android.util.Log

/**
 * 使用 Java 反射呼叫 Unity 的 UnitySendMessage 的預設實作。
 * 在無法取得 UnityPlayer 類別時（例如單元測試環境），會靜默忽略呼叫。
 */
class ReflectionUnitySender : IUnitySender {

    private companion object {
        private const val TAG = "ReflectionUnitySender"
        private const val UNITY_PLAYER_CLASS = "com.unity3d.player.UnityPlayer"
        private const val UNITY_SEND_MESSAGE_METHOD = "UnitySendMessage"
    }

    /**
     * 透過反射呼叫 `UnityPlayer.UnitySendMessage`，向 Unity C# 發送訊息。
     * 若找不到 UnityPlayer 類別（例如在單元測試環境中），則靜默忽略此次呼叫。
     *
     * @param gameObjectName 接收訊息的 GameObject 名稱
     * @param methodName C# 方法名稱
     * @param message JSON 格式的訊息字串
     */
    override fun send(gameObjectName: String, methodName: String, message: String) {
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
