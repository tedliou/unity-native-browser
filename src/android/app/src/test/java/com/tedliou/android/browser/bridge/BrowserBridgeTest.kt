package com.tedliou.android.browser.bridge

import android.app.Activity
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BrowserBridgeTest {
    @Test
    fun test_initialize_does_not_throw() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        BrowserBridge.initialize(activity)
    }

    @Test
    fun test_sendToUnity_does_not_throw() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        BrowserBridge.initialize(activity)
        
        BrowserBridge.sendToUnity("TestMethod", "TestMessage")
    }

    @Test
    fun test_sendToUnity_with_null_unity_player_does_not_throw() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        BrowserBridge.initialize(activity)
        
        // In test environment, UnityPlayer won't be available
        // This should log warning but not throw
        BrowserBridge.sendToUnity("TestMethod", "TestMessage")
    }
}
