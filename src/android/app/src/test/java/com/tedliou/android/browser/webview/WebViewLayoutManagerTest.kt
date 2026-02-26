package com.tedliou.android.browser.webview

import android.util.DisplayMetrics
import android.view.Gravity
import android.widget.FrameLayout
import com.tedliou.android.browser.core.Alignment
import com.tedliou.android.browser.core.BrowserConfig
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WebViewLayoutManagerTest {
    @Test
    fun test_fullscreen_mode_produces_MATCH_PARENT() {
        val config = BrowserConfig(
            url = "https://example.com",
            width = 1.0f,
            height = 1.0f
        )
        val displayMetrics = DisplayMetrics().apply {
            widthPixels = 1080
            heightPixels = 1920
        }

        val layoutParams = WebViewLayoutManager.calculateLayoutParams(config, displayMetrics)

        assertEquals(FrameLayout.LayoutParams.MATCH_PARENT, layoutParams.width)
        assertEquals(FrameLayout.LayoutParams.MATCH_PARENT, layoutParams.height)
    }

    @Test
    fun test_width_height_percentage_calculations() {
        val config = BrowserConfig(
            url = "https://example.com",
            width = 0.75f,
            height = 0.5f
        )
        val displayMetrics = DisplayMetrics().apply {
            widthPixels = 1080
            heightPixels = 1920
        }

        val layoutParams = WebViewLayoutManager.calculateLayoutParams(config, displayMetrics)

        assertEquals(810, layoutParams.width) // 1080 * 0.75
        assertEquals(960, layoutParams.height) // 1920 * 0.5
    }

    @Test
    fun test_alignment_CENTER_maps_to_Gravity_CENTER() {
        val config = BrowserConfig(
            url = "https://example.com",
            width = 0.5f,
            height = 0.5f,
            alignment = Alignment.CENTER
        )
        val displayMetrics = DisplayMetrics().apply {
            widthPixels = 1080
            heightPixels = 1920
        }

        val layoutParams = WebViewLayoutManager.calculateLayoutParams(config, displayMetrics)

        assertEquals(Gravity.CENTER, layoutParams.gravity)
    }

    @Test
    fun test_alignment_LEFT_maps_to_START_CENTER_VERTICAL() {
        val config = BrowserConfig(
            url = "https://example.com",
            width = 0.5f,
            height = 0.5f,
            alignment = Alignment.LEFT
        )
        val displayMetrics = DisplayMetrics().apply {
            widthPixels = 1080
            heightPixels = 1920
        }

        val layoutParams = WebViewLayoutManager.calculateLayoutParams(config, displayMetrics)

        assertEquals(Gravity.START or Gravity.CENTER_VERTICAL, layoutParams.gravity)
    }

    @Test
    fun test_alignment_RIGHT_maps_to_END_CENTER_VERTICAL() {
        val config = BrowserConfig(
            url = "https://example.com",
            width = 0.5f,
            height = 0.5f,
            alignment = Alignment.RIGHT
        )
        val displayMetrics = DisplayMetrics().apply {
            widthPixels = 1080
            heightPixels = 1920
        }

        val layoutParams = WebViewLayoutManager.calculateLayoutParams(config, displayMetrics)

        assertEquals(Gravity.END or Gravity.CENTER_VERTICAL, layoutParams.gravity)
    }

    @Test
    fun test_alignment_TOP_maps_to_TOP_CENTER_HORIZONTAL() {
        val config = BrowserConfig(
            url = "https://example.com",
            width = 0.5f,
            height = 0.5f,
            alignment = Alignment.TOP
        )
        val displayMetrics = DisplayMetrics().apply {
            widthPixels = 1080
            heightPixels = 1920
        }

        val layoutParams = WebViewLayoutManager.calculateLayoutParams(config, displayMetrics)

        assertEquals(Gravity.TOP or Gravity.CENTER_HORIZONTAL, layoutParams.gravity)
    }

    @Test
    fun test_alignment_BOTTOM_maps_to_BOTTOM_CENTER_HORIZONTAL() {
        val config = BrowserConfig(
            url = "https://example.com",
            width = 0.5f,
            height = 0.5f,
            alignment = Alignment.BOTTOM
        )
        val displayMetrics = DisplayMetrics().apply {
            widthPixels = 1080
            heightPixels = 1920
        }

        val layoutParams = WebViewLayoutManager.calculateLayoutParams(config, displayMetrics)

        assertEquals(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, layoutParams.gravity)
    }

    @Test
    fun test_alignment_TOP_LEFT_maps_to_TOP_START() {
        val config = BrowserConfig(
            url = "https://example.com",
            width = 0.5f,
            height = 0.5f,
            alignment = Alignment.TOP_LEFT
        )
        val displayMetrics = DisplayMetrics().apply {
            widthPixels = 1080
            heightPixels = 1920
        }

        val layoutParams = WebViewLayoutManager.calculateLayoutParams(config, displayMetrics)

        assertEquals(Gravity.TOP or Gravity.START, layoutParams.gravity)
    }

    @Test
    fun test_alignment_TOP_RIGHT_maps_to_TOP_END() {
        val config = BrowserConfig(
            url = "https://example.com",
            width = 0.5f,
            height = 0.5f,
            alignment = Alignment.TOP_RIGHT
        )
        val displayMetrics = DisplayMetrics().apply {
            widthPixels = 1080
            heightPixels = 1920
        }

        val layoutParams = WebViewLayoutManager.calculateLayoutParams(config, displayMetrics)

        assertEquals(Gravity.TOP or Gravity.END, layoutParams.gravity)
    }

    @Test
    fun test_alignment_BOTTOM_LEFT_maps_to_BOTTOM_START() {
        val config = BrowserConfig(
            url = "https://example.com",
            width = 0.5f,
            height = 0.5f,
            alignment = Alignment.BOTTOM_LEFT
        )
        val displayMetrics = DisplayMetrics().apply {
            widthPixels = 1080
            heightPixels = 1920
        }

        val layoutParams = WebViewLayoutManager.calculateLayoutParams(config, displayMetrics)

        assertEquals(Gravity.BOTTOM or Gravity.START, layoutParams.gravity)
    }

    @Test
    fun test_alignment_BOTTOM_RIGHT_maps_to_BOTTOM_END() {
        val config = BrowserConfig(
            url = "https://example.com",
            width = 0.5f,
            height = 0.5f,
            alignment = Alignment.BOTTOM_RIGHT
        )
        val displayMetrics = DisplayMetrics().apply {
            widthPixels = 1080
            heightPixels = 1920
        }

        val layoutParams = WebViewLayoutManager.calculateLayoutParams(config, displayMetrics)

        assertEquals(Gravity.BOTTOM or Gravity.END, layoutParams.gravity)
    }
}
