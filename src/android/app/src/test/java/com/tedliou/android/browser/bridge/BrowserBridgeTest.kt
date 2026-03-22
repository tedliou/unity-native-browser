package com.tedliou.android.browser.bridge

import android.app.Activity
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BrowserBridgeTest {

    private lateinit var activity: Activity
    private lateinit var mockSender: IUnitySender

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        mockSender = mockk(relaxed = true)
        BrowserBridge.sender = mockSender
        BrowserBridge.initialize(activity)
    }

    @Test
    fun test_initialize_does_not_throw() {
        BrowserBridge.initialize(activity)
    }

    @Test
    fun test_sendToUnity_does_not_throw() {
        BrowserBridge.sendToUnity("TestMethod", "TestMessage")
    }

    @Test
    fun test_sendToUnity_invokes_sender_exactly_once_with_correct_args() {
        val methodName = "OnPageStarted"
        val message = """{"url":"https://example.com"}"""

        BrowserBridge.sendToUnity(methodName, message)

        verify(exactly = 1) {
            mockSender.send(BrowserBridge.gameObjectName, methodName, message)
        }
    }

    // Feature: android-native-browser-refactor, Property 5: BrowserBridge 訊息發送完整性
    // Validates: Requirements 2.1, 2.2
    @Test
    fun property_sendToUnity_sender_receives_exactly_one_call_with_exact_values() {
        runBlocking {
            checkAll(100, Arb.string(), Arb.string()) { methodName, message ->
                val localSender = mockk<IUnitySender>(relaxed = true)
                BrowserBridge.sender = localSender

                BrowserBridge.sendToUnity(methodName, message)

                verify(exactly = 1) {
                    localSender.send(BrowserBridge.gameObjectName, methodName, message)
                }
            }
        }
    }
}