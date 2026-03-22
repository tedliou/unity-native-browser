package com.tedliou.android.browser.bridge

/**
 * 抽象 Unity 訊息發送介面，允許在測試中替換真實的 UnitySendMessage 呼叫。
 */
interface IUnitySender {
    /**
     * 向 Unity C# 發送訊息。
     *
     * @param gameObjectName 接收訊息的 GameObject 名稱
     * @param methodName C# 方法名稱
     * @param message JSON 格式的訊息字串
     */
    fun send(gameObjectName: String, methodName: String, message: String)
}
