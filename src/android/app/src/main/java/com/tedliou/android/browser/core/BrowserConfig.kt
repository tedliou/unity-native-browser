package com.tedliou.android.browser.core

/**
 * 瀏覽器實例的設定資料類別。
 *
 * 指定瀏覽器操作所需的網址、版面配置、外觀及行為。
 *
 * @property url 要在瀏覽器中載入的網址
 * @property width 螢幕寬度百分比（0.0 至 1.0），1.0 代表全寬
 * @property height 螢幕高度百分比（0.0 至 1.0），1.0 代表全高
 * @property alignment 螢幕上的位置對齊方式（例如 CENTER、TOP_LEFT、BOTTOM_RIGHT）
 * @property closeOnTapOutside 若為 true，使用者點擊瀏覽器區域外部時將關閉瀏覽器
 * @property deepLinkPatterns 用於攔截並作為深層連結處理的網址正規表示式清單
 * @property closeOnDeepLink 若為 true，攔截到深層連結後將關閉瀏覽器
 * @property enableJavaScript 若為 true，WebView 中將啟用 JavaScript 執行
 * @property userAgent 自訂 User-Agent 字串；若為空字串則使用系統預設值
 */
data class BrowserConfig(
    val url: String,
    val width: Float = 1.0f,
    val height: Float = 1.0f,
    val alignment: Alignment = Alignment.CENTER,
    val closeOnTapOutside: Boolean = false,
    val deepLinkPatterns: List<String> = emptyList(),
    val closeOnDeepLink: Boolean = true,
    val enableJavaScript: Boolean = true,
    val userAgent: String = "",
) {
    init {
        require(width in 0.0f..1.0f) { "width must be between 0.0 and 1.0" }
        require(height in 0.0f..1.0f) { "height must be between 0.0 and 1.0" }
        require(url.isNotBlank()) { "url cannot be blank" }
    }
}
