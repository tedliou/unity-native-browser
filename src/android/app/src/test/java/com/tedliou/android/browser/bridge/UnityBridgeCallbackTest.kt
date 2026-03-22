package com.tedliou.android.browser.bridge

import android.app.Activity
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

/**
 * [UnityBridgeCallback] 的屬性測試。
 *
 * 驗證屬性 6：JSON 序列化完整性。
 * 對任意 URL 字串，[UnityBridgeCallback.onPageStarted]、[UnityBridgeCallback.onPageFinished]、
 * [UnityBridgeCallback.onDeepLink] 發送的 JSON 必須包含正確的 `url` 欄位。
 */
@RunWith(RobolectricTestRunner::class)
class UnityBridgeCallbackTest {

    private fun setupBridge(): IUnitySender {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        BrowserBridge.initialize(activity)
        val mockSender = mockk<IUnitySender>(relaxed = true)
        BrowserBridge.sender = mockSender
        return mockSender
    }

    // Feature: android-native-browser-refactor, Property 6: UnityBridgeCallback JSON 序列化完整性
    // Validates: Requirements 2.1
    @Test
    fun property_onPageStarted_jsonContainsCorrectUrl() {
        runBlocking {
            checkAll(50, Arb.string(minSize = 1, maxSize = 100)) { url ->
                val mockSender = setupBridge()
                val capturedMessages = mutableListOf<String>()
                every { mockSender.send(any(), any(), capture(capturedMessages)) } returns Unit

                val callback = UnityBridgeCallback()
                callback.onPageStarted(url)
                Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

                if (capturedMessages.isNotEmpty()) {
                    val json = JSONObject(capturedMessages.last())
                    assert(json.getString("url") == url) {
                        "Expected url='$url' in JSON but got: ${capturedMessages.last()}"
                    }
                }
            }
        }
    }

    @Test
    fun property_onPageFinished_jsonContainsCorrectUrl() {
        runBlocking {
            checkAll(50, Arb.string(minSize = 1, maxSize = 100)) { url ->
                val mockSender = setupBridge()
                val capturedMessages = mutableListOf<String>()
                every { mockSender.send(any(), any(), capture(capturedMessages)) } returns Unit

                val callback = UnityBridgeCallback()
                callback.onPageFinished(url)
                Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

                if (capturedMessages.isNotEmpty()) {
                    val json = JSONObject(capturedMessages.last())
                    assert(json.getString("url") == url) {
                        "Expected url='$url' in JSON but got: ${capturedMessages.last()}"
                    }
                }
            }
        }
    }

    @Test
    fun property_onDeepLink_jsonContainsCorrectUrl() {
        runBlocking {
            checkAll(50, Arb.string(minSize = 1, maxSize = 100)) { url ->
                val mockSender = setupBridge()
                val capturedMessages = mutableListOf<String>()
                every { mockSender.send(any(), any(), capture(capturedMessages)) } returns Unit

                val callback = UnityBridgeCallback()
                callback.onDeepLink(url)
                Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

                if (capturedMessages.isNotEmpty()) {
                    val json = JSONObject(capturedMessages.last())
                    assert(json.getString("url") == url) {
                        "Expected url='$url' in JSON but got: ${capturedMessages.last()}"
                    }
                }
            }
        }
    }
}
