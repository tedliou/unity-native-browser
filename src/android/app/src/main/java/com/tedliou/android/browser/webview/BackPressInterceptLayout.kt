package com.tedliou.android.browser.webview

import android.content.Context
import android.view.KeyEvent
import android.widget.FrameLayout
import com.tedliou.android.browser.util.BrowserLogger

/**
 * 透過 [dispatchKeyEvent] 攔截返回鍵事件的 FrameLayout 容器。
 *
 * Unity 6 的 [GameActivity] 在原生 C++ 層（UGAInput.cpp）處理按鍵事件，
 * 早於 Java 層的 [OnBackPressedDispatcher] 或 [Activity.onKeyDown]。
 * 這意味著當 WebView 覆蓋在 Unity GameActivity 上時，
 * 標準的返回鍵處理方式（OnBackPressedCallback、View.setOnKeyListener）不會觸發。
 *
 * 此容器在視圖層級的按鍵事件分派階段攔截 [KeyEvent.KEYCODE_BACK]，
 * 早於 GameActivity 的原生輸入層消費事件。容器必須可聚焦並持有焦點。
 *
 * @param context 用於建立視圖的 Context。
 * @param onBackPress 按下返回鍵時（ACTION_UP）觸發的回呼。
 */
internal class BackPressInterceptLayout(
    context: Context,
    private val onBackPress: () -> Unit,
) : FrameLayout(context) {

    init {
        // Must be focusable to receive key events in the view dispatch chain
        isFocusable = true
        isFocusableInTouchMode = true
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK) {
            if (event.action == KeyEvent.ACTION_UP) {
                BrowserLogger.d(SUBTAG, "Back key intercepted via dispatchKeyEvent")
                onBackPress()
            }
            // Consume both ACTION_DOWN and ACTION_UP to prevent double-fire
            // and to prevent the event from reaching GameActivity's native layer
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    private companion object {
        private const val SUBTAG = "BackPressIntercept"
    }
}
