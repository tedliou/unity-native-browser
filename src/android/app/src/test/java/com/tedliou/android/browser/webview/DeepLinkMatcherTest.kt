package com.tedliou.android.browser.webview

import io.kotest.property.Arb
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
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

    // Feature: android-native-browser-refactor, Property 4: DeepLinkMatcher 空模式列表永遠不匹配
    // Validates: Requirements 3.2
    @Test
    fun property_emptyPatternList_neverMatches() {
        runBlocking {
            checkAll(100, Arb.string()) { url ->
                assertFalse(DeepLinkMatcher.matches(url, emptyList()))
            }
        }
    }

    // 邊界情況：無效正規表達式不崩潰
    @Test
    fun test_invalid_regex_patterns_do_not_crash() {
        val invalidPatterns = listOf(
            "^[unclosed",
            "^(unclosed",
            "^{invalid",
            "^\\",
            "^(?invalid",
            "[a-"
        )
        val url = "https://example.com/test"
        // None of these should throw — they should all return false
        for (pattern in invalidPatterns) {
            assertFalse("Pattern '$pattern' should not match", DeepLinkMatcher.matches(url, listOf(pattern)))
        }
    }

    // Feature: android-native-browser-refactor, Property 3: DeepLinkMatcher 精確匹配對稱性
    // Validates: Requirements 3.1
    @Test
    fun property_exactMatch_symmetry() {
        // Characters that cause a pattern to be treated as regex or wildcard
        val metaChars = setOf('*', '^', '$', '[', '{', '\\', '(', '|', '+', '?')

        val safeArb = Arb.string(minSize = 1, maxSize = 100)
            .filter { s -> s.none { it in metaChars } }

        runBlocking {
            checkAll(100, safeArb, safeArb) { u, v ->
                // A URL should always match itself as an exact pattern
                assertTrue(DeepLinkMatcher.matches(u, listOf(u)))

                // A URL should NOT match a different exact pattern
                if (u != v) {
                    assertFalse(DeepLinkMatcher.matches(u, listOf(v)))
                }
            }
        }
    }
}
