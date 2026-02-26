package com.tedliou.android.browser.webview

import com.tedliou.android.browser.util.BrowserLogger

object DeepLinkMatcher {
    fun matches(url: String, patterns: List<String>): Boolean {
        if (patterns.isEmpty()) {
            return false
        }
        BrowserLogger.v("DeepLinkMatcher", "Checking URL: $url against ${patterns.size} patterns")
        return patterns.any { pattern ->
            when {
                pattern.contains("*") -> matchPrefix(url, pattern)
                isRegexPattern(pattern) -> matchRegex(url, pattern)
                else -> url == pattern
            }
        }
    }

    private fun matchPrefix(url: String, pattern: String): Boolean {
        val regexPattern = pattern.replace("*", ".*")
        return matchRegex(url, regexPattern)
    }

    private fun matchRegex(url: String, pattern: String): Boolean {
        return try {
            pattern.toRegex().matches(url)
        } catch (exception: IllegalArgumentException) {
            BrowserLogger.e("DeepLinkMatcher", "Invalid regex pattern: $pattern", exception)
            false
        }
    }

    private fun isRegexPattern(pattern: String): Boolean {
        return pattern.startsWith("^") || pattern.contains("[") || pattern.contains("{") || pattern.contains("\\")
    }
}
