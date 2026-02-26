package com.tedliou.android.browser.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
}
