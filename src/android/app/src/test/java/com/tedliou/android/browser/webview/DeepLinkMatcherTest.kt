package com.tedliou.android.browser.webview

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DeepLinkMatcherTest {
    @Test
    fun test_exact_match_pattern_matches() {
        val url = "https://example.com/callback"
        val patterns = listOf("https://example.com/callback")

        val result = DeepLinkMatcher.matches(url, patterns)

        assertTrue(result)
    }

    @Test
    fun test_exact_match_pattern_does_not_match_different_url() {
        val url = "https://example.com/other"
        val patterns = listOf("https://example.com/callback")

        val result = DeepLinkMatcher.matches(url, patterns)

        assertFalse(result)
    }

    @Test
    fun test_prefix_match_pattern_with_wildcard() {
        val url = "https://example.com/callback/success"
        val patterns = listOf("https://example.com/callback/*")

        val result = DeepLinkMatcher.matches(url, patterns)

        assertTrue(result)
    }

    @Test
    fun test_prefix_match_pattern_does_not_match_different_prefix() {
        val url = "https://example.com/other/path"
        val patterns = listOf("https://example.com/callback/*")

        val result = DeepLinkMatcher.matches(url, patterns)

        assertFalse(result)
    }

    @Test
    fun test_regex_pattern_matches() {
        val url = "https://example.com/callback123"
        val patterns = listOf("^https://example\\.com/callback\\d+$")

        val result = DeepLinkMatcher.matches(url, patterns)

        assertTrue(result)
    }

    @Test
    fun test_regex_pattern_does_not_match() {
        val url = "https://example.com/callback"
        val patterns = listOf("^https://example\\.com/callback\\d+$")

        val result = DeepLinkMatcher.matches(url, patterns)

        assertFalse(result)
    }

    @Test
    fun test_empty_patterns_list_returns_false() {
        val url = "https://example.com/callback"
        val patterns = emptyList<String>()

        val result = DeepLinkMatcher.matches(url, patterns)

        assertFalse(result)
    }

    @Test
    fun test_multiple_patterns_matches_first() {
        val url = "https://example.com/callback"
        val patterns = listOf(
            "https://example.com/callback",
            "https://example.com/other"
        )

        val result = DeepLinkMatcher.matches(url, patterns)

        assertTrue(result)
    }

    @Test
    fun test_multiple_patterns_matches_second() {
        val url = "https://example.com/other"
        val patterns = listOf(
            "https://example.com/callback",
            "https://example.com/other"
        )

        val result = DeepLinkMatcher.matches(url, patterns)

        assertTrue(result)
    }

    @Test
    fun test_multiple_patterns_no_match() {
        val url = "https://example.com/third"
        val patterns = listOf(
            "https://example.com/callback",
            "https://example.com/other"
        )

        val result = DeepLinkMatcher.matches(url, patterns)

        assertFalse(result)
    }

    @Test
    fun test_invalid_regex_pattern_returns_false() {
        val url = "https://example.com/callback"
        val patterns = listOf("^[invalid(regex")

        val result = DeepLinkMatcher.matches(url, patterns)

        assertFalse(result)
    }
}
