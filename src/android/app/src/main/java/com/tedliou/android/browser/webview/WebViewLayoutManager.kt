package com.tedliou.android.browser.webview

import android.content.Context
import android.graphics.Color
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import com.tedliou.android.browser.core.Alignment
import com.tedliou.android.browser.core.BrowserConfig
import com.tedliou.android.browser.util.BrowserLogger

/**
 * WebView 版面配置管理器。
 *
 * 負責根據 [BrowserConfig] 計算 WebView 的版面參數，
 * 以及建立非全螢幕模式下的半透明覆蓋層視圖。
 */
object WebViewLayoutManager {
    /**
     * 根據設定與螢幕尺寸計算 WebView 的版面參數。
     *
     * 若寬高均為 1.0f，視為全螢幕模式，使用 MATCH_PARENT；
     * 否則依比例換算為像素值，並依對齊方式設定 gravity。
     *
     * @param config 瀏覽器設定，包含寬高比例與對齊方式
     * @param displayMetrics 螢幕顯示指標，用於換算像素
     * @return 計算完成的 [FrameLayout.LayoutParams]
     */
    fun calculateLayoutParams(
        config: BrowserConfig,
        displayMetrics: DisplayMetrics,
    ): FrameLayout.LayoutParams {
        val isFullScreen = config.width == 1.0f && config.height == 1.0f
        if (isFullScreen) {
            BrowserLogger.d(SUBTAG, "Full-screen mode detected")
        }
        val width = if (isFullScreen) {
            FrameLayout.LayoutParams.MATCH_PARENT
        } else {
            (config.width * displayMetrics.widthPixels).toInt()
        }
        val height = if (isFullScreen) {
            FrameLayout.LayoutParams.MATCH_PARENT
        } else {
            (config.height * displayMetrics.heightPixels).toInt()
        }
        val gravity = mapAlignmentToGravity(config.alignment)
        BrowserLogger.d(
            SUBTAG,
            "Calculated layout: width=${width}px, height=${height}px, gravity=$gravity",
        )
        return FrameLayout.LayoutParams(width, height).apply {
            this.gravity = gravity
        }
    }

    /**
     * 建立非全螢幕模式下的半透明覆蓋層視圖。
     *
     * 若為全螢幕模式（寬高均為 1.0f），回傳 `null`。
     * 否則建立黑色半透明背景視圖，並在 [BrowserConfig.closeOnTapOutside] 為 `true` 時
     * 設定點擊外部關閉的監聽器。
     *
     * @param context 用於建立視圖的 Context
     * @param config 瀏覽器設定
     * @param onTapOutside 點擊覆蓋層時的回呼，可為 `null`
     * @return 覆蓋層 [View]，全螢幕模式時回傳 `null`
     */
    fun createOverlayView(context: Context, config: BrowserConfig, onTapOutside: (() -> Unit)? = null): View? {
        val isFullScreen = config.width == 1.0f && config.height == 1.0f
        if (isFullScreen) {
            return null
        }
        BrowserLogger.d(SUBTAG, "Creating semi-transparent overlay for non-fullscreen mode")
        return View(context).apply {
            setBackgroundColor(Color.parseColor("#80000000"))
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            )
            if (config.closeOnTapOutside && onTapOutside != null) {
                setOnClickListener {
                    BrowserLogger.d(SUBTAG, "Tap outside detected; closing WebView")
                    onTapOutside()
                }
            }
        }
    }

    /**
     * 將 [Alignment] 列舉值對應至 Android Gravity 常數。
     *
     * @param alignment 對齊方式列舉值
     * @return 對應的 Gravity 整數值
     */
    private fun mapAlignmentToGravity(alignment: Alignment): Int {
        return when (alignment) {
            Alignment.CENTER -> Gravity.CENTER
            Alignment.LEFT -> Gravity.START or Gravity.CENTER_VERTICAL
            Alignment.RIGHT -> Gravity.END or Gravity.CENTER_VERTICAL
            Alignment.TOP -> Gravity.TOP or Gravity.CENTER_HORIZONTAL
            Alignment.BOTTOM -> Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            Alignment.TOP_LEFT -> Gravity.TOP or Gravity.START
            Alignment.TOP_RIGHT -> Gravity.TOP or Gravity.END
            Alignment.BOTTOM_LEFT -> Gravity.BOTTOM or Gravity.START
            Alignment.BOTTOM_RIGHT -> Gravity.BOTTOM or Gravity.END
        }
    }

    private const val SUBTAG = "LayoutManager"
}
