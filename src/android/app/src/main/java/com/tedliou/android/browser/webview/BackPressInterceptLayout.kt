package com.tedliou.android.browser.webview

import android.content.Context
import android.view.KeyEvent
import android.widget.FrameLayout
import com.tedliou.android.browser.util.BrowserLogger

/**
 * A FrameLayout container that intercepts back button key events via [dispatchKeyEvent].
 *
 * Unity 6's [GameActivity] processes key events at the native C++ layer (UGAInput.cpp)
 * before they reach Java's [OnBackPressedDispatcher] or [Activity.onKeyDown]. This means
 * standard back button handling approaches (OnBackPressedCallback, View.setOnKeyListener)
 * never fire when a WebView is overlaid on top of Unity's GameActivity.
 *
 * This layout intercepts [KeyEvent.KEYCODE_BACK] in [dispatchKeyEvent], which is called
 * during the view hierarchy's key event dispatch phase — before GameActivity's native
 * input layer can consume the event. The container must be focusable and hold focus.
 *
 * @param context The context to use for view creation.
 * @param onBackPress Callback invoked when the back button is pressed (ACTION_UP).
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
