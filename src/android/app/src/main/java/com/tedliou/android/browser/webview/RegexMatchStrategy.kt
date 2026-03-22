package com.tedliou.android.browser.webview

/**
 * 正規表達式匹配策略。
 *
 * 使用完整的正規表達式語法進行 URL 匹配。
 * 適用於以 `^` 開頭、包含 `[`、`{` 或 `\` 等正規表達式元字元的模式。
 *
 * @param pattern 正規表達式匹配模式
 */
class RegexMatchStrategy(private val pattern: String) : IDeepLinkStrategy {

    /**
     * 判斷給定 URL 是否符合正規表達式模式。
     *
     * 若模式為無效的正規表達式，則安全地回傳 `false` 而不拋出例外。
     *
     * @param url 待匹配的 URL
     * @return 若 URL 符合正規表達式模式則回傳 `true`，否則回傳 `false`
     */
    override fun matches(url: String): Boolean {
        return try {
            pattern.toRegex().matches(url)
        } catch (e: IllegalArgumentException) {
            false
        }
    }
}
