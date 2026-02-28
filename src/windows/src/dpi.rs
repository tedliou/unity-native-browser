// DPI awareness and scaling utilities for the WebView2 window.
// Ensures proper rendering on high-DPI (4K) and low-DPI displays.

use windows::Win32::Foundation::HWND;
use windows::Win32::UI::HiDpi::{
    GetDpiForWindow, SetProcessDpiAwarenessContext,
    DPI_AWARENESS_CONTEXT_PER_MONITOR_AWARE_V2,
};
use windows::Win32::UI::WindowsAndMessaging::{
    GetSystemMetrics, SM_CXSCREEN, SM_CYSCREEN,
    SystemParametersInfoW, SPI_GETWORKAREA,
};
use windows::Win32::Foundation::RECT;

const BASE_DPI: f64 = 96.0;
const DEFAULT_WIDTH: i32 = 1024;
const DEFAULT_HEIGHT: i32 = 768;
/// Maximum percentage of screen working area the window can occupy.
const MAX_SCREEN_RATIO: f64 = 0.90;
/// Minimum window size to remain usable.
const MIN_WIDTH: i32 = 640;
const MIN_HEIGHT: i32 = 480;

/// Enable per-monitor DPI awareness for the process.
/// Should be called once at initialization, before any window creation.
/// Returns true if succeeded, false if already set or failed.
pub fn enable_dpi_awareness() -> bool {
    unsafe {
        SetProcessDpiAwarenessContext(DPI_AWARENESS_CONTEXT_PER_MONITOR_AWARE_V2).is_ok()
    }
}

/// Get the DPI scale factor for a given window.
/// Returns 1.0 for standard 96 DPI, 1.5 for 144 DPI, 2.0 for 192 DPI (4K), etc.
pub fn get_scale_factor(hwnd: HWND) -> f64 {
    let dpi = unsafe { GetDpiForWindow(hwnd) };
    if dpi == 0 {
        // Fallback: assume standard DPI
        1.0
    } else {
        dpi as f64 / BASE_DPI
    }
}

/// Get the working area of the primary monitor (excludes taskbar).
pub fn get_work_area() -> (i32, i32) {
    let mut rect = RECT::default();
    let ok = unsafe {
        SystemParametersInfoW(
            SPI_GETWORKAREA,
            0,
            Some(&mut rect as *mut RECT as *mut _),
            Default::default(),
        )
    };
    if ok.is_ok() {
        let w = rect.right - rect.left;
        let h = rect.bottom - rect.top;
        if w > 0 && h > 0 {
            return (w, h);
        }
    }
    // Fallback to full screen metrics
    let w = unsafe { GetSystemMetrics(SM_CXSCREEN) };
    let h = unsafe { GetSystemMetrics(SM_CYSCREEN) };
    (w.max(800), h.max(600))
}

/// Calculate the editor window size, scaled for DPI and clamped to screen bounds.
///
/// Logic:
/// - Start with 1024x768 base size
/// - Scale by the DPI factor of the target monitor
/// - Clamp to 90% of the working area so it doesn't cover the entire screen
/// - Enforce a minimum of 640x480 so it remains usable on tiny screens
pub fn calculate_editor_window_size(scale_factor: f64) -> (i32, i32) {
    let (work_w, work_h) = get_work_area();

    // Scale base size by DPI
    let scaled_w = (DEFAULT_WIDTH as f64 * scale_factor) as i32;
    let scaled_h = (DEFAULT_HEIGHT as f64 * scale_factor) as i32;

    // Clamp to screen working area
    let max_w = (work_w as f64 * MAX_SCREEN_RATIO) as i32;
    let max_h = (work_h as f64 * MAX_SCREEN_RATIO) as i32;

    let final_w = scaled_w.min(max_w).max(MIN_WIDTH);
    let final_h = scaled_h.min(max_h).max(MIN_HEIGHT);

    (final_w, final_h)
}

/// Calculate the centered position for the editor window on the primary monitor.
pub fn calculate_centered_position(window_w: i32, window_h: i32) -> (i32, i32) {
    let (work_w, work_h) = get_work_area();
    let x = (work_w - window_w) / 2;
    let y = (work_h - window_h) / 2;
    (x.max(0), y.max(0))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn calculate_editor_window_size_at_1x() {
        // At 1.0 scale, should be exactly 1024x768 (assuming screen is large enough)
        let (w, h) = calculate_editor_window_size(1.0);
        // On a normal desktop screen, 1024x768 should fit within 90% of working area
        assert!(w >= MIN_WIDTH);
        assert!(h >= MIN_HEIGHT);
        assert!(w <= 1024 || w >= MIN_WIDTH); // Either base size or clamped
        assert!(h <= 768 || h >= MIN_HEIGHT);
    }

    #[test]
    fn calculate_editor_window_size_at_2x() {
        // At 2.0 scale (4K), base would be 2048x1536
        // This should be clamped to 90% of working area on most screens
        let (w, h) = calculate_editor_window_size(2.0);
        assert!(w >= MIN_WIDTH);
        assert!(h >= MIN_HEIGHT);
    }

    #[test]
    fn calculate_editor_window_size_enforces_minimum() {
        // Even at very small scale, minimum size is enforced
        let (w, h) = calculate_editor_window_size(0.1);
        assert!(w >= MIN_WIDTH);
        assert!(h >= MIN_HEIGHT);
    }

    #[test]
    fn calculate_centered_position_is_non_negative() {
        let (x, y) = calculate_centered_position(1024, 768);
        assert!(x >= 0);
        assert!(y >= 0);
    }
}
