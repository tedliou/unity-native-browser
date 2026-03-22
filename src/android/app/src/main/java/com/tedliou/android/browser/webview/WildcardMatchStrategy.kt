package com.tedliou.android.browser.webview

/**
 * 萬用字元匹配策略。
 *
 * 支援以 `*` 作為萬用字元的模式匹配，`*` 可對應任意數量的任意字元。
 * 例如：`https://example.com/&#42;` 可匹配所有以 `https://example.com/` 開頭的 URL。
 *
 * @param pattern 包含 `*` 萬用字元的匹配模式
 */
class WildcardMatchStrategy(private val pattern: String) : IDeepLinkStrategy {

    /**
     * 判斷給定 URL 是否符合萬用字元模式。
     *
     * 將模式中的 `*` 轉換為正規表達式的 `.*` 後進行匹配。
     * 若轉換後的正規表達式無效，則回傳 `false`。
     *
     * @param url 待匹配的 URL
     * @return 若 URL 符合萬用字元模式則回傳 `true`，否則回傳 `false`
     */
    override fun matches(url: String): Boolean {
        val regexPattern = pattern.replace("*", ".*")
        return try {
            regexPattern.toRegex().matches(url)
        } catch (e: IllegalArgumentException) {
            false
        }
    }
}
