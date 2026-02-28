// Window creation and management for the WebView2 browser.
//
// Two modes:
// - Editor mode: Standalone topmost window (no close/min/max buttons, always on top, resizable)
// - Build mode: Borderless child window embedded in Unity's HWND
//
// Width/height from BrowserConfig are 0.0-1.0 floats representing percentage of available space.
// Alignment determines positioning within that space.
//
// Close-on-tap-outside:
// When enabled, a low-level mouse hook detects clicks outside the browser window
// and posts a custom message (WM_CLOSE_ON_TAP_OUTSIDE) to trigger close.


use std::cell::Cell;

use windows::core::PCWSTR;
use windows::Win32::Foundation::{HWND, LPARAM, LRESULT, RECT, WPARAM, POINT};
use windows::Win32::Graphics::Gdi::{UpdateWindow, GetStockObject, WHITE_BRUSH};
use windows::Win32::System::LibraryLoader::GetModuleHandleW;
use windows::Win32::UI::WindowsAndMessaging::*;

use crate::config::BrowserConfig;
use crate::dpi;
const WINDOW_CLASS_NAME: &str = "NativeBrowserWebView\0";

/// Custom window message to trigger close-on-tap-outside.
/// When the mouse hook detects a click outside the browser window,
/// it posts this message to the window's message queue.
const WM_CLOSE_ON_TAP_OUTSIDE: u32 = WM_APP + 2;

/// Window mode determined by the is_editor flag.
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum WindowMode {
    /// Standalone topmost window for Unity Editor
    Editor,
    /// Borderless child window embedded in Unity game HWND
    Build,
}

/// Result of window creation.
pub struct WindowInfo {
    pub hwnd: HWND,
    pub mode: WindowMode,
}

/// Create a window for the WebView2 browser.
///
/// - In Editor mode: Creates a standalone topmost window with DPI-aware sizing.
///   Width/height from config are applied as ratios of the DPI-scaled editor window.
/// - In Build mode: Creates a child window parented to the given Unity HWND.
///   Width/height from config are applied as ratios of the parent client area.
pub fn create_browser_window(
    mode: WindowMode,
    parent_hwnd: isize,
    config: &BrowserConfig,
) -> Result<WindowInfo, String> {
    unsafe {
        register_window_class()?;
    }

    match mode {
        WindowMode::Editor => create_editor_window(config),
        WindowMode::Build => create_build_window(parent_hwnd, config),
    }
}

/// Register the window class (idempotent — safe to call multiple times).
unsafe fn register_window_class() -> Result<(), String> {
    let hinstance = GetModuleHandleW(None)
        .map_err(|e| format!("GetModuleHandleW failed: {}", e))?;

    let class_name = to_wide(WINDOW_CLASS_NAME);

    let wc = WNDCLASSEXW {
        cbSize: std::mem::size_of::<WNDCLASSEXW>() as u32,
        style: CS_HREDRAW | CS_VREDRAW,
        lpfnWndProc: Some(wnd_proc),
        hInstance: hinstance.into(),
        hCursor: LoadCursorW(None, IDC_ARROW).unwrap_or_default(),
        hbrBackground: windows::Win32::Graphics::Gdi::HBRUSH(GetStockObject(WHITE_BRUSH).0),
        lpszClassName: PCWSTR(class_name.as_ptr()),
        ..Default::default()
    };

    // RegisterClassExW returns 0 if the class already exists (ERROR_CLASS_ALREADY_EXISTS)
    // which is fine — we just need it registered once.
    let atom = RegisterClassExW(&wc);
    if atom == 0 {
        let err = windows::core::Error::from_win32();
        // ERROR_CLASS_ALREADY_EXISTS (1410) is not a real error
        if err.code().0 as u32 != 0x80070582 {
            // Check raw HRESULT for class already exists
            let last_err = windows::Win32::Foundation::GetLastError();
            if last_err.0 != 1410 {
                return Err(format!("RegisterClassExW failed: {}", err));
            }
        }
    }

    Ok(())
}

/// Create the standalone Editor window.
/// Config width/height (0.0-1.0) are applied as ratios of the DPI-scaled base size (1024x768).
fn create_editor_window(config: &BrowserConfig) -> Result<WindowInfo, String> {
    let scale_factor = {
        // Initial scale factor — will be adjusted after window creation using actual DPI
        1.0_f64
    };

    // Calculate the maximum editor window size (DPI-scaled, screen-clamped)
    let (max_w, max_h) = dpi::calculate_editor_window_size(scale_factor);

    // Apply config width/height as ratios of the max editor size
    let cfg_w = config.width.clamp(0.1, 1.0) as f64;
    let cfg_h = config.height.clamp(0.1, 1.0) as f64;
    let initial_w = ((max_w as f64) * cfg_w) as i32;
    let initial_h = ((max_h as f64) * cfg_h) as i32;

    // Position based on alignment
    let (work_w, work_h) = dpi::get_work_area();
    let (initial_x, initial_y) = calculate_aligned_position(
        initial_w, initial_h, work_w, work_h, &config.alignment,
    );

    let hinstance = unsafe {
        GetModuleHandleW(None)
            .map_err(|e| format!("GetModuleHandleW failed: {}", e))?
    };

    let class_name = to_wide(WINDOW_CLASS_NAME);
    let title = to_wide("NativeBrowser\0");

    // Editor window style:
    // - WS_OVERLAPPEDWINDOW gives title bar + resize borders
    // - Remove WS_MINIMIZEBOX and WS_MAXIMIZEBOX to prevent min/max
    // - We handle WM_CLOSE in wnd_proc to prevent closing
    let style = WS_OVERLAPPEDWINDOW & !WS_MINIMIZEBOX & !WS_MAXIMIZEBOX;
    let ex_style = WS_EX_TOPMOST; // Always on top

    let hwnd = unsafe {
        CreateWindowExW(
            ex_style,
            PCWSTR(class_name.as_ptr()),
            PCWSTR(title.as_ptr()),
            style,
            initial_x,
            initial_y,
            initial_w,
            initial_h,
            None,     // No parent
            None,     // No menu
            Some(hinstance.into()),
            None,
        )
        .map_err(|e| format!("CreateWindowExW (editor) failed: {}", e))?
    };

    // Now that we have a window, get the actual DPI and resize if needed
    let actual_scale = dpi::get_scale_factor(hwnd);
    if (actual_scale - 1.0).abs() > 0.01 {
        let (adj_max_w, adj_max_h) = dpi::calculate_editor_window_size(actual_scale);
        let adjusted_w = ((adj_max_w as f64) * cfg_w) as i32;
        let adjusted_h = ((adj_max_h as f64) * cfg_h) as i32;
        let (adj_work_w, adj_work_h) = dpi::get_work_area();
        let (adjusted_x, adjusted_y) = calculate_aligned_position(
            adjusted_w, adjusted_h, adj_work_w, adj_work_h, &config.alignment,
        );
        unsafe {
            let _ = SetWindowPos(
                hwnd,
                None,
                adjusted_x,
                adjusted_y,
                adjusted_w,
                adjusted_h,
                SWP_NOZORDER | SWP_NOACTIVATE,
            );
        }
    }

    // Remove close button from system menu
    unsafe {
        let hmenu = GetSystemMenu(hwnd, false);
        if !hmenu.is_invalid() {
            let _ = EnableMenuItem(hmenu, SC_CLOSE as u32, MF_BYCOMMAND | MF_GRAYED);
        }
    }

    // Show the window
    unsafe {
        let _ = ShowWindow(hwnd, SW_SHOW);
        let _ = UpdateWindow(hwnd);
    }

    Ok(WindowInfo {
        hwnd,
        mode: WindowMode::Editor,
    })
}

/// Create the child window for Build mode (embedded in Unity HWND).
/// Config width/height (0.0-1.0) are applied as ratios of the parent client area.
fn create_build_window(parent_hwnd: isize, config: &BrowserConfig) -> Result<WindowInfo, String> {
    let parent = HWND(parent_hwnd as *mut _);

    // Verify the parent HWND is valid
    if !unsafe { IsWindow(Some(parent)) }.as_bool() {
        return Err("Invalid parent HWND for build mode".to_string());
    }

    // Get parent client area size
    let mut client_rect = RECT::default();
    unsafe {
        let _ = GetClientRect(parent, &mut client_rect);
    }

    let parent_w = client_rect.right - client_rect.left;
    let parent_h = client_rect.bottom - client_rect.top;

    // Apply config width/height as ratios of the parent client area
    let cfg_w = config.width.clamp(0.1, 1.0) as f64;
    let cfg_h = config.height.clamp(0.1, 1.0) as f64;
    let child_w = ((parent_w as f64) * cfg_w) as i32;
    let child_h = ((parent_h as f64) * cfg_h) as i32;

    // Position based on alignment within the parent
    let (child_x, child_y) = calculate_aligned_position(
        child_w, child_h, parent_w, parent_h, &config.alignment,
    );

    let hinstance = unsafe {
        GetModuleHandleW(None)
            .map_err(|e| format!("GetModuleHandleW failed: {}", e))?
    };

    let class_name = to_wide(WINDOW_CLASS_NAME);

    // Child window style: borderless, clips children, visible
    let style = WS_CHILD | WS_CLIPCHILDREN | WS_VISIBLE;

    let hwnd = unsafe {
        CreateWindowExW(
            WINDOW_EX_STYLE::default(),
            PCWSTR(class_name.as_ptr()),
            PCWSTR::null(),
            style,
            child_x,
            child_y,
            child_w,
            child_h,
            Some(parent),
            None,
            Some(hinstance.into()),
            None,
        )
        .map_err(|e| format!("CreateWindowExW (build) failed: {}", e))?
    };

    Ok(WindowInfo {
        hwnd,
        mode: WindowMode::Build,
    })
}

/// Resize the browser window to match its parent (for Build mode WM_SIZE handling).
pub fn resize_to_parent(hwnd: HWND, parent_hwnd: HWND) {
    let mut rect = RECT::default();
    unsafe {
        let _ = GetClientRect(parent_hwnd, &mut rect);
        let _ = SetWindowPos(
            hwnd,
            None,
            0,
            0,
            rect.right - rect.left,
            rect.bottom - rect.top,
            SWP_NOZORDER | SWP_NOMOVE,
        );
    }
}

/// Calculate a position within a container based on alignment.
/// `item_w`/`item_h` = size of the item to position.
/// `container_w`/`container_h` = size of the available space.
/// `alignment` = one of CENTER, LEFT, RIGHT, TOP, BOTTOM, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT.
fn calculate_aligned_position(
    item_w: i32,
    item_h: i32,
    container_w: i32,
    container_h: i32,
    alignment: &str,
) -> (i32, i32) {
    let remaining_x = (container_w - item_w).max(0);
    let remaining_y = (container_h - item_h).max(0);

    match alignment.to_uppercase().as_str() {
        "LEFT" => (0, remaining_y / 2),
        "RIGHT" => (remaining_x, remaining_y / 2),
        "TOP" => (remaining_x / 2, 0),
        "BOTTOM" => (remaining_x / 2, remaining_y),
        "TOP_LEFT" => (0, 0),
        "TOP_RIGHT" => (remaining_x, 0),
        "BOTTOM_LEFT" => (0, remaining_y),
        "BOTTOM_RIGHT" => (remaining_x, remaining_y),
        _ => (remaining_x / 2, remaining_y / 2), // CENTER (default)
    }
}

/// Destroy the browser window.
pub fn destroy_window(hwnd: HWND) {
    if !hwnd.is_invalid() && unsafe { IsWindow(Some(hwnd)) }.as_bool() {
        unsafe {
            let _ = DestroyWindow(hwnd);
        }
    }
}

/// Get the client area size of a window.
pub fn get_client_size(hwnd: HWND) -> (i32, i32) {
    let mut rect = RECT::default();
    unsafe {
        let _ = GetClientRect(hwnd, &mut rect);
    }
    (rect.right - rect.left, rect.bottom - rect.top)
}

/// Window procedure for the browser window.
unsafe extern "system" fn wnd_proc(
    hwnd: HWND,
    msg: u32,
    wparam: WPARAM,
    lparam: LPARAM,
) -> LRESULT {
    match msg {
        WM_CLOSE => {
            // Block close in Editor mode — the window should only be closed via nb_close()
            LRESULT(0)
        }
        WM_SYSCOMMAND => {
            let cmd = wparam.0 & 0xFFF0;
            if cmd == SC_CLOSE as usize
                || cmd == SC_MINIMIZE as usize
                || cmd == SC_MAXIMIZE as usize
            {
                // Block close, minimize, maximize via system commands
                return LRESULT(0);
            }
            DefWindowProcW(hwnd, msg, wparam, lparam)
        }
        x if x == WM_CLOSE_ON_TAP_OUTSIDE => {
            // Close-on-tap-outside triggered by the mouse hook.
            // Send a Close command through the threading system.
            // This is safe because we're already on the STA thread.
            crate::threading::send_close_command();
            LRESULT(0)
        }
        _ => DefWindowProcW(hwnd, msg, wparam, lparam),
    }
}

/// Convert a Rust string to a null-terminated wide string (UTF-16).
fn to_wide(s: &str) -> Vec<u16> {
    s.encode_utf16().chain(std::iter::once(0)).collect()
}

/// Find the Unity game window by class name "UnityWndClass".
/// Used in Build mode to find the parent HWND when one isn't explicitly provided.
pub fn find_unity_window() -> Option<HWND> {
    let class_name = to_wide("UnityWndClass\0");
    match unsafe { FindWindowW(PCWSTR(class_name.as_ptr()), None) } {
        Ok(hwnd) if !hwnd.is_invalid() => Some(hwnd),
        _ => None,
    }
}

// ─── Close-on-tap-outside mouse hook ─────────────────────────────────────────

// Thread-local storage for the browser window HWND that the mouse hook monitors.
// Only used on the STA thread.
thread_local! {
    static HOOK_TARGET_HWND: Cell<isize> = const { Cell::new(0) };
}

/// Install a low-level mouse hook that detects clicks outside the given window.
/// The hook posts WM_CLOSE_ON_TAP_OUTSIDE to the window when a click occurs outside.
/// Must be called on the STA thread (the thread with the message pump).
pub fn install_mouse_hook(hwnd: HWND) -> Result<HHOOK, String> {
    HOOK_TARGET_HWND.with(|cell| cell.set(hwnd.0 as isize));

    let hook = unsafe {
        SetWindowsHookExW(WH_MOUSE_LL, Some(mouse_hook_proc), None, 0)
    };

    match hook {
        Ok(h) => Ok(h),
        Err(e) => Err(format!("SetWindowsHookExW failed: {}", e)),
    }
}

/// Remove a previously installed mouse hook.
pub fn remove_mouse_hook(hook: HHOOK) {
    unsafe {
        let _ = UnhookWindowsHookEx(hook);
    }
    HOOK_TARGET_HWND.with(|cell| cell.set(0));
}

/// Low-level mouse hook procedure.
/// Detects WM_LBUTTONDOWN/WM_NCLBUTTONDOWN outside the browser window
/// and posts WM_CLOSE_ON_TAP_OUTSIDE to the window.
unsafe extern "system" fn mouse_hook_proc(
    n_code: i32,
    wparam: WPARAM,
    lparam: LPARAM,
) -> LRESULT {
    if n_code >= 0 {
        let msg_id = wparam.0 as u32;
        // Only trigger on left mouse button down (normal or non-client area)
        if msg_id == WM_LBUTTONDOWN || msg_id == WM_NCLBUTTONDOWN {
            let target_hwnd_raw = HOOK_TARGET_HWND.with(|cell| cell.get());
            if target_hwnd_raw != 0 {
                let target_hwnd = HWND(target_hwnd_raw as *mut _);

                // Get the click point from MSLLHOOKSTRUCT
                let mouse_struct = &*(lparam.0 as *const MSLLHOOKSTRUCT);
                let pt = mouse_struct.pt;

                // Check if the click is outside the browser window
                let mut window_rect = RECT::default();
                if GetWindowRect(target_hwnd, &mut window_rect).is_ok() {
                    if !point_in_rect(&pt, &window_rect) {
                        // Click is outside — post close message to the window
                        let _ = PostMessageW(
                            Some(target_hwnd),
                            WM_CLOSE_ON_TAP_OUTSIDE,
                            WPARAM(0),
                            LPARAM(0),
                        );
                    }
                }
            }
        }
    }
    CallNextHookEx(None, n_code, wparam, lparam)
}

/// Check if a point is inside a rectangle.
fn point_in_rect(pt: &POINT, rect: &RECT) -> bool {
    pt.x >= rect.left && pt.x < rect.right && pt.y >= rect.top && pt.y < rect.bottom
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn to_wide_conversion() {
        let wide = to_wide("Hello\0");
        assert_eq!(wide, vec![b'H' as u16, b'e' as u16, b'l' as u16, b'l' as u16, b'o' as u16, 0, 0]);
    }

    #[test]
    fn window_mode_equality() {
        assert_eq!(WindowMode::Editor, WindowMode::Editor);
        assert_eq!(WindowMode::Build, WindowMode::Build);
        assert_ne!(WindowMode::Editor, WindowMode::Build);
    }

    #[test]
    fn aligned_position_center() {
        let (x, y) = calculate_aligned_position(400, 300, 1000, 800, "CENTER");
        assert_eq!(x, 300);
        assert_eq!(y, 250);
    }

    #[test]
    fn aligned_position_top_left() {
        let (x, y) = calculate_aligned_position(400, 300, 1000, 800, "TOP_LEFT");
        assert_eq!(x, 0);
        assert_eq!(y, 0);
    }

    #[test]
    fn aligned_position_bottom_right() {
        let (x, y) = calculate_aligned_position(400, 300, 1000, 800, "BOTTOM_RIGHT");
        assert_eq!(x, 600);
        assert_eq!(y, 500);
    }

    #[test]
    fn aligned_position_left() {
        let (x, y) = calculate_aligned_position(400, 300, 1000, 800, "LEFT");
        assert_eq!(x, 0);
        assert_eq!(y, 250);
    }

    #[test]
    fn aligned_position_right() {
        let (x, y) = calculate_aligned_position(400, 300, 1000, 800, "RIGHT");
        assert_eq!(x, 600);
        assert_eq!(y, 250);
    }

    #[test]
    fn aligned_position_top() {
        let (x, y) = calculate_aligned_position(400, 300, 1000, 800, "TOP");
        assert_eq!(x, 300);
        assert_eq!(y, 0);
    }

    #[test]
    fn aligned_position_bottom() {
        let (x, y) = calculate_aligned_position(400, 300, 1000, 800, "BOTTOM");
        assert_eq!(x, 300);
        assert_eq!(y, 500);
    }

    #[test]
    fn aligned_position_unknown_defaults_to_center() {
        let (x, y) = calculate_aligned_position(400, 300, 1000, 800, "UNKNOWN");
        assert_eq!(x, 300);
        assert_eq!(y, 250);
    }

    #[test]
    fn aligned_position_item_larger_than_container() {
        // When item is larger, position should be 0 (clamped)
        let (x, y) = calculate_aligned_position(1200, 1000, 1000, 800, "CENTER");
        assert_eq!(x, 0);
        assert_eq!(y, 0);
    }

    #[test]
    fn aligned_position_case_insensitive() {
        let (x1, y1) = calculate_aligned_position(400, 300, 1000, 800, "center");
        let (x2, y2) = calculate_aligned_position(400, 300, 1000, 800, "CENTER");
        assert_eq!(x1, x2);
        assert_eq!(y1, y2);
    }

    #[test]
    fn point_in_rect_inside() {
        let pt = POINT { x: 50, y: 50 };
        let rect = RECT { left: 0, top: 0, right: 100, bottom: 100 };
        assert!(point_in_rect(&pt, &rect));
    }

    #[test]
    fn point_in_rect_on_left_edge() {
        let pt = POINT { x: 0, y: 50 };
        let rect = RECT { left: 0, top: 0, right: 100, bottom: 100 };
        assert!(point_in_rect(&pt, &rect));
    }

    #[test]
    fn point_in_rect_on_right_edge_exclusive() {
        // right edge is exclusive (x < rect.right)
        let pt = POINT { x: 100, y: 50 };
        let rect = RECT { left: 0, top: 0, right: 100, bottom: 100 };
        assert!(!point_in_rect(&pt, &rect));
    }

    #[test]
    fn point_in_rect_on_bottom_edge_exclusive() {
        let pt = POINT { x: 50, y: 100 };
        let rect = RECT { left: 0, top: 0, right: 100, bottom: 100 };
        assert!(!point_in_rect(&pt, &rect));
    }

    #[test]
    fn point_in_rect_outside_left() {
        let pt = POINT { x: -1, y: 50 };
        let rect = RECT { left: 0, top: 0, right: 100, bottom: 100 };
        assert!(!point_in_rect(&pt, &rect));
    }

    #[test]
    fn point_in_rect_outside_above() {
        let pt = POINT { x: 50, y: -1 };
        let rect = RECT { left: 0, top: 0, right: 100, bottom: 100 };
        assert!(!point_in_rect(&pt, &rect));
    }

    #[test]
    fn point_in_rect_outside_far() {
        let pt = POINT { x: 500, y: 500 };
        let rect = RECT { left: 10, top: 20, right: 100, bottom: 200 };
        assert!(!point_in_rect(&pt, &rect));
    }
}
