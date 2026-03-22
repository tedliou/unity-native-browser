package com.tedliou.android.browser.webview

/**
 * 精確匹配策略。
 *
 * 僅當 URL 與模式字串完全相等時才視為匹配。
 *
 * @param pattern 用於比對的精確字串模式
 */
class ExactMatchStrategy(private val pattern: String) : IDeepLinkStrategy {

    /**
     * 判斷給定 URL 是否與模式完全相等。
     *
     * @param url 待匹配的 URL
     * @return 若 URL 與模式完全相同則回傳 `true`，否則回傳 `false`
     */
    override fun matches(url: String): Boolean {
        return url == pattern
    }
}
