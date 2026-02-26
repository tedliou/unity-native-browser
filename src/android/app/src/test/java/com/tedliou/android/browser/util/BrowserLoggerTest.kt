package com.tedliou.android.browser.util

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BrowserLoggerTest {
    @Test
    fun test_debug_log_does_not_throw() {
        BrowserLogger.d("Test", "Debug message")
    }

    @Test
    fun test_info_log_does_not_throw() {
        BrowserLogger.i("Test", "Info message")
    }

    @Test
    fun test_warning_log_does_not_throw() {
        BrowserLogger.w("Test", "Warning message")
    }

    @Test
    fun test_error_log_does_not_throw() {
        BrowserLogger.e("Test", "Error message")
    }

    @Test
    fun test_error_log_with_exception_does_not_throw() {
        val exception = Exception("Test exception")
        BrowserLogger.e("Test", "Error message", exception)
    }

    @Test
    fun test_verbose_log_does_not_throw() {
        BrowserLogger.v("Test", "Verbose message")
    }

    @Test
    fun test_log_with_url_does_not_throw() {
        BrowserLogger.d("Test", "Opening URL: https://example.com?token=secret123")
    }
}
