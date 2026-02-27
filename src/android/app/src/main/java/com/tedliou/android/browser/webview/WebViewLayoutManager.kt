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

object WebViewLayoutManager {
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
