package com.tedliou.android.browser.core

import io.kotest.property.Arb
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.float
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class BrowserConfigTest {
    @Test
    fun test_default_values() {
        val config = BrowserConfig(url = "https://example.com")

        assertEquals("https://example.com", config.url)
        assertEquals(1.0f, config.width)
        assertEquals(1.0f, config.height)
        assertEquals(Alignment.CENTER, config.alignment)
        assertTrue(config.deepLinkPatterns.isEmpty())
        assertTrue(config.enableJavaScript)
        assertEquals("", config.userAgent)
    }

    @Test
    fun test_custom_width_and_height() {
        val config = BrowserConfig(
            url = "https://example.com",
            width = 0.75f,
            height = 0.5f
        )

        assertEquals(0.75f, config.width)
        assertEquals(0.5f, config.height)
    }

    @Test
    fun test_custom_alignment() {
        val config = BrowserConfig(
            url = "https://example.com",
            alignment = Alignment.TOP_LEFT
        )

        assertEquals(Alignment.TOP_LEFT, config.alignment)
    }

    @Test
    fun test_deep_link_patterns() {
        val patterns = listOf("https://example.com/callback", "https://example.com/success/*")
        val config = BrowserConfig(
            url = "https://example.com",
            deepLinkPatterns = patterns
        )

        assertEquals(2, config.deepLinkPatterns.size)
        assertEquals(patterns, config.deepLinkPatterns)
    }

    @Test
    fun test_user_agent_custom() {
        val config = BrowserConfig(
            url = "https://example.com",
            userAgent = "CustomAgent/1.0"
        )

        assertEquals("CustomAgent/1.0", config.userAgent)
    }

    // Feature: android-native-browser-refactor, Property 1: BrowserConfig 寬高範圍不變式
    // Validates: Requirements 1.1, 1.2
    @Test
    fun property_widthHeight_rangeInvariant() {
        runBlocking {
            // Valid range: values in [0.0f, 1.0f] should succeed
            checkAll(100, Arb.float(0.0f, 1.0f), Arb.float(0.0f, 1.0f)) { w, h ->
                if (!w.isNaN() && !h.isNaN()) {
                    val config = BrowserConfig(url = "https://example.com", width = w, height = h)
                    assertEquals(w, config.width)
                    assertEquals(h, config.height)
                }
            }
            // Out-of-range width should throw
            checkAll(100, Arb.float().filter { it < 0.0f || it > 1.0f }.filter { !it.isNaN() && !it.isInfinite() }) { w ->
                assertThrows(IllegalArgumentException::class.java) {
                    BrowserConfig(url = "https://example.com", width = w, height = 0.5f)
                }
            }
            // Out-of-range height should throw
            checkAll(100, Arb.float().filter { it < 0.0f || it > 1.0f }.filter { !it.isNaN() && !it.isInfinite() }) { h ->
                assertThrows(IllegalArgumentException::class.java) {
                    BrowserConfig(url = "https://example.com", width = 0.5f, height = h)
                }
            }
        }
    }

    // Feature: android-native-browser-refactor, Property 2: BrowserConfig url 空白字串不變式
    // Validates: Requirements 1.3
    @Test
    fun property_url_blankStringInvariant() {
        runBlocking {
            // Fixed blank strings should throw
            val blankStrings = listOf("", " ", "  ", "\t", "\n", "\r\n", "   \t\n  ")
            for (blank in blankStrings) {
                assertThrows(IllegalArgumentException::class.java) {
                    BrowserConfig(url = blank)
                }
            }
            // Property-based: arbitrary whitespace-only strings should throw
            checkAll(100, Arb.string().filter { it.isBlank() }) { blank ->
                assertThrows(IllegalArgumentException::class.java) {
                    BrowserConfig(url = blank)
                }
            }
        }
    }
}
