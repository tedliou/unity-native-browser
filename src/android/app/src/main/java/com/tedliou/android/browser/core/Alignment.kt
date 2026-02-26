package com.tedliou.android.browser.core

/**
 * Enum representing screen alignment positions for browser layout.
 *
 * Used by [BrowserConfig.alignment] to position the browser on screen.
 */
enum class Alignment {
    /**
     * Center of the screen
     */
    CENTER,

    /**
     * Left edge, vertically centered
     */
    LEFT,

    /**
     * Right edge, vertically centered
     */
    RIGHT,

    /**
     * Top edge, horizontally centered
     */
    TOP,

    /**
     * Bottom edge, horizontally centered
     */
    BOTTOM,

    /**
     * Top-left corner
     */
    TOP_LEFT,

    /**
     * Top-right corner
     */
    TOP_RIGHT,

    /**
     * Bottom-left corner
     */
    BOTTOM_LEFT,

    /**
     * Bottom-right corner
     */
    BOTTOM_RIGHT,
}
