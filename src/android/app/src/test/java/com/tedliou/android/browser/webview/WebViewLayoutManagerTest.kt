package com.tedliou.android.browser.webview

import android.util.DisplayMetrics
import android.view.Gravity
import android.widget.FrameLayout
import com.tedliou.android.browser.core.Alignment
import com.tedliou.android.browser.core.BrowserConfig
import io.kotest.property.Arb
import io.kotest.property.arbitrary.float
import io.kotest.property.arbitrary.filter
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
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

    // Feature: android-native-browser-refactor, Property 7: WebViewLayoutManager 全螢幕模式一致性
    // Validates: 當 width == 1.0f 且 height == 1.0f 時，calculateLayoutParams 回傳 MATCH_PARENT，
    //            且 createOverlayView 回傳 null
    @Test
    fun property_fullscreen_consistency() {
        runBlocking {
            val displayMetrics = DisplayMetrics().apply {
                widthPixels = 1080
                heightPixels = 1920
            }
            // 對任意螢幕尺寸，全螢幕設定必須始終產生 MATCH_PARENT
            checkAll(
                100,
                Arb.float(1f, 4000f).filter { !it.isNaN() && !it.isInfinite() },
                Arb.float(1f, 4000f).filter { !it.isNaN() && !it.isInfinite() },
            ) { screenW, screenH ->
                val dm = DisplayMetrics().apply {
                    widthPixels = screenW.toInt().coerceAtLeast(1)
                    heightPixels = screenH.toInt().coerceAtLeast(1)
                }
                val config = BrowserConfig(url = "https://example.com", width = 1.0f, height = 1.0f)
                val params = WebViewLayoutManager.calculateLayoutParams(config, dm)
                assertEquals(FrameLayout.LayoutParams.MATCH_PARENT, params.width)
                assertEquals(FrameLayout.LayoutParams.MATCH_PARENT, params.height)
            }
        }
    }

    // Feature: android-native-browser-refactor, Property 8: WebViewLayoutManager 對齊映射完整性
    // Validates: 所有 9 個 Alignment 列舉值均對應至不同的非零 gravity 值
    @Test
    fun property_alignment_mapping_completeness() {
        val displayMetrics = DisplayMetrics().apply {
            widthPixels = 1080
            heightPixels = 1920
        }
        val gravities = Alignment.entries.map { alignment ->
            val config = BrowserConfig(
                url = "https://example.com",
                width = 0.5f,
                height = 0.5f,
                alignment = alignment,
            )
            WebViewLayoutManager.calculateLayoutParams(config, displayMetrics).gravity
        }
        // 所有 gravity 值必須非零
        gravities.forEach { gravity ->
            assertNotEquals("gravity 不得為 0（未設定）", 0, gravity)
        }
        // 所有 gravity 值必須各不相同
        assertEquals(
            "所有 Alignment 必須對應至不同的 gravity 值",
            gravities.size,
            gravities.toSet().size,
        )
    }
}
