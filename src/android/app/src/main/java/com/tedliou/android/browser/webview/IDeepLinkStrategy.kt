package com.tedliou.android.browser.webview

/**
 * 深層連結匹配策略介面。
 */
interface IDeepLinkStrategy {
    /**
     * 判斷給定 URL 是否符合此策略的匹配規則。
     *
     * @param url 待匹配的 URL
     * @return 是否匹配
     */
    fun matches(url: String): Boolean
}
