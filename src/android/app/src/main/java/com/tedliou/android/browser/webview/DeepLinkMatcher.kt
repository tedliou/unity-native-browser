package com.tedliou.android.browser.webview

import com.tedliou.android.browser.util.BrowserLogger

/**
 * 深層連結匹配器。
 *
 * 根據給定的模式清單，自動選擇適合的匹配策略（精確、萬用字元或正規表達式），
 * 並判斷 URL 是否符合其中任一模式。
 */
object DeepLinkMatcher {
    /**
     * 判斷 URL 是否符合模式清單中的任一模式。
     *
     * 若模式清單為空，直接回傳 `false`。
     * 否則對每個模式建立對應策略，只要有一個匹配即回傳 `true`。
     *
     * @param url 待匹配的 URL
     * @param patterns 模式清單
     * @return 若 URL 符合任一模式則回傳 `true`，否則回傳 `false`
     */
    fun matches(url: String, patterns: List<String>): Boolean {
        if (patterns.isEmpty()) {
            return false
        }
        BrowserLogger.v("DeepLinkMatcher", "Checking URL: $url against ${patterns.size} patterns")
        return patterns.any { pattern -> buildStrategy(pattern).matches(url) }
    }

    /**
     * 根據模式內容建立對應的匹配策略。
     *
     * - 若模式包含正規表達式元字元（如 `^`、`$`、`[`、`{` 等），使用 [RegexMatchStrategy]
     * - 若模式包含 `*`，使用 [WildcardMatchStrategy]
     * - 否則使用 [ExactMatchStrategy]
     *
     * @param pattern 匹配模式字串
     * @return 對應的 [IDeepLinkStrategy] 實例
     */
    fun buildStrategy(pattern: String): IDeepLinkStrategy {
        return when {
            isRegexPattern(pattern) -> RegexMatchStrategy(pattern)
            pattern.contains("*") -> WildcardMatchStrategy(pattern)
            else -> ExactMatchStrategy(pattern)
        }
    }

    /**
     * 判斷模式是否為正規表達式格式。
     *
     * 檢查模式是否包含常見的正規表達式元字元，
     * 包括 `^`、`$`、`[`、`{`、`\`、`(`、`|`、`+`、`?`。
     *
     * @param pattern 待判斷的模式字串
     * @return 若模式包含正規表達式元字元則回傳 `true`，否則回傳 `false`
     */
    private fun isRegexPattern(pattern: String): Boolean {
        return pattern.startsWith("^") ||
            pattern.endsWith("$") ||
            pattern.contains("[") ||
            pattern.contains("{") ||
            pattern.contains("\\") ||
            pattern.contains("(") ||
            pattern.contains("|") ||
            pattern.contains("+") ||
            pattern.contains("?")
    }
}
