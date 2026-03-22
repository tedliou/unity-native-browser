package com.tedliou.android.browser.core

/**
 * 代表瀏覽器版面配置螢幕對齊位置的列舉。
 *
 * 由 [BrowserConfig.alignment] 使用，用於設定瀏覽器在螢幕上的位置。
 */
enum class Alignment {
    /**
     * 螢幕中央
     */
    CENTER,

    /**
     * 左側邊緣，垂直置中
     */
    LEFT,

    /**
     * 右側邊緣，垂直置中
     */
    RIGHT,

    /**
     * 上方邊緣，水平置中
     */
    TOP,

    /**
     * 下方邊緣，水平置中
     */
    BOTTOM,

    /**
     * 左上角
     */
    TOP_LEFT,

    /**
     * 右上角
     */
    TOP_RIGHT,

    /**
     * 左下角
     */
    BOTTOM_LEFT,

    /**
     * 右下角
     */
    BOTTOM_RIGHT,
}
