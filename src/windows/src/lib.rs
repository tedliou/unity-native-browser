// FFI entry points for the NativeBrowser Windows native layer.
//
// This is the public API exposed to Unity C# via P/Invoke.
// All functions use C calling convention and operate on a single global browser instance.
//
// Thread safety model:
//   Unity main thread → calls nb_* functions → commands sent via mpsc channel → STA thread processes
//   STA thread → fires callback fn ptr → C# ConcurrentQueue → Unity Update() drains

mod callback;
mod config;
mod dpi;
mod error;
mod threading;
mod webview;
mod window;

use std::ffi::CStr;
use std::os::raw::c_char;
use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::{Arc, Mutex};

use once_cell::sync::Lazy;
use windows::core::PCWSTR;
use windows::Win32::UI::Shell::ShellExecuteW;

use crate::callback::UnityCallback;
use crate::config::BrowserConfig;
use crate::error::{BrowserError, ErrorKind};
use crate::threading::{Command, StaThread};
use crate::webview::WebViewInstance;
use crate::window::WindowMode;
// ─── Global State ───────────────────────────────────────────────────────────

/// Whether the browser is currently open. Accessed from both Unity thread and STA thread.
static IS_OPEN: Lazy<Arc<AtomicBool>> = Lazy::new(|| Arc::new(AtomicBool::new(false)));

/// Whether we're running in Editor mode (true) or Build/Standalone mode (false).
static IS_EDITOR: AtomicBool = AtomicBool::new(false);

/// The STA thread handle. Protected by Mutex for safe access from Unity's main thread.
static STA_THREAD: Lazy<Mutex<Option<StaThread>>> = Lazy::new(|| Mutex::new(None));

// ─── Helper Functions ───────────────────────────────────────────────────────

/// Convert a C string pointer to a Rust &str. Returns empty string for null/invalid.
fn cstr_to_str<'a>(ptr: *const c_char) -> &'a str {
    if ptr.is_null() {
        return "";
    }
    unsafe { CStr::from_ptr(ptr) }.to_str().unwrap_or("")
}

/// Send an error callback to Unity.
fn report_error(kind: ErrorKind, message: &str) {
    let err = BrowserError::new(kind, message);
    callback::send_error(
        &err.kind.to_string(),
        &err.message,
        &err.url,
        &err.request_id,
    );
}

// ─── FFI Entry Points ───────────────────────────────────────────────────────

/// Initialize the native browser system.
///
/// Must be called once before any other nb_* function.
/// - `callback_fn`: Function pointer for event delivery to Unity C#.
/// - `is_editor`: true if running in Unity Editor, false if standalone build.
///
/// # Safety
/// `callback_fn` must be a valid function pointer that remains valid for the plugin's lifetime.
#[no_mangle]
pub extern "C" fn nb_initialize(callback_fn: UnityCallback, is_editor: bool) {
    // Store the callback
    callback::set_callback(callback_fn);

    // Store editor mode flag
    IS_EDITOR.store(is_editor, Ordering::SeqCst);

    // Enable DPI awareness (best effort — may already be set)
    dpi::enable_dpi_awareness();

    // Spawn the STA thread with the command handler
    let is_open_flag = IS_OPEN.clone();
    let sta_result = StaThread::spawn(move |cmd| {
        handle_command(cmd, &is_open_flag);
    });

    match sta_result {
        Ok(sta) => {
            if let Ok(mut guard) = STA_THREAD.lock() {
                *guard = Some(sta);
            }
        }
        Err(e) => {
            report_error(ErrorKind::ThreadError, &format!("Failed to spawn STA thread: {}", e));
        }
    }
}

/// Open a browser with the given type and configuration.
///
/// # Safety
/// `type_str` and `config_json` must be valid null-terminated UTF-8 C strings.
/// `parent_hwnd` is the Unity game window HWND (0 for editor mode).
#[no_mangle]
pub extern "C" fn nb_open(
    type_str: *const c_char,
    config_json: *const c_char,
    parent_hwnd: isize,
) {
    let type_string = cstr_to_str(type_str).to_string();
    let config_string = cstr_to_str(config_json).to_string();

    send_command(Command::Open(type_string, config_string, parent_hwnd));
}

/// Close the browser.
#[no_mangle]
pub extern "C" fn nb_close() {
    send_command(Command::Close);
}

/// Reload the current page.
#[no_mangle]
pub extern "C" fn nb_refresh() {
    send_command(Command::Refresh);
}

/// Check if the browser is currently open.
/// Returns synchronously using the atomic flag (no STA thread round-trip needed).
#[no_mangle]
pub extern "C" fn nb_is_open() -> bool {
    IS_OPEN.load(Ordering::SeqCst)
}

/// Execute JavaScript in the browser and receive the result via OnJsResult callback.
///
/// # Safety
/// `script` and `request_id` must be valid null-terminated UTF-8 C strings.
#[no_mangle]
pub extern "C" fn nb_execute_js(script: *const c_char, request_id: *const c_char) {
    let script_string = cstr_to_str(script).to_string();
    let request_id_string = cstr_to_str(request_id).to_string();

    send_command(Command::ExecuteJavaScript(script_string, request_id_string));
}

/// Inject JavaScript to run on every page load.
///
/// # Safety
/// `script` must be a valid null-terminated UTF-8 C string.
#[no_mangle]
pub extern "C" fn nb_inject_js(script: *const c_char) {
    let script_string = cstr_to_str(script).to_string();
    send_command(Command::InjectJavaScript(script_string));
}

/// Send a postMessage to the web content.
///
/// # Safety
/// `message` must be a valid null-terminated UTF-8 C string.
#[no_mangle]
pub extern "C" fn nb_send_post_message(message: *const c_char) {
    let message_string = cstr_to_str(message).to_string();
    send_command(Command::SendPostMessage(message_string));
}

/// Destroy the native browser system and release all resources.
/// After calling this, nb_initialize must be called again before any other operations.
#[no_mangle]
pub extern "C" fn nb_destroy() {
    // Send destroy command and shutdown the STA thread
    if let Ok(mut guard) = STA_THREAD.lock() {
        if let Some(mut sta) = guard.take() {
            sta.shutdown();
        }
    }

    // Clear the callback
    callback::clear_callback();

    // Reset open flag
    IS_OPEN.store(false, Ordering::SeqCst);
}

// ─── Command Sending ────────────────────────────────────────────────────────

/// Send a command to the STA thread. Reports error via callback if the thread is unavailable.
fn send_command(cmd: Command) {
    let guard = match STA_THREAD.lock() {
        Ok(g) => g,
        Err(_) => {
            report_error(ErrorKind::ThreadError, "STA thread mutex poisoned");
            return;
        }
    };

    match guard.as_ref() {
        Some(sta) => {
            if let Err(e) = sta.send(cmd) {
                report_error(ErrorKind::ThreadError, &e);
            }
        }
        None => {
            report_error(
                ErrorKind::ThreadError,
                "NativeBrowser not initialized — call nb_initialize first",
            );
        }
    }
}

// ─── STA Thread Command Handler ─────────────────────────────────────────────

/// State held on the STA thread for the active browser session.
struct BrowserState {
    webview: WebViewInstance,
    window_hwnd: windows::Win32::Foundation::HWND,
    close_on_tap_outside: bool,
    mouse_hook: Option<windows::Win32::UI::WindowsAndMessaging::HHOOK>,
}

/// Thread-local browser state. Only accessed on the STA thread.
/// We use a function-scoped static via a closure capture instead of thread_local!
/// because StaThread::spawn takes a FnMut closure.
///
/// The state is captured in the closure passed to StaThread::spawn.

/// Handle a command on the STA thread.
/// `state` is the mutable browser state, `is_open_flag` is the shared atomic bool.
fn handle_command(cmd: Command, is_open_flag: &Arc<AtomicBool>) {
    // We need persistent state across calls. Use a thread-local cell.
    thread_local! {
        static STATE: std::cell::RefCell<Option<BrowserState>> = const { std::cell::RefCell::new(None) };
    }

    match cmd {
        Command::Open(_type_str, config_json, parent_hwnd) => {
            // Parse config first (needed for all open types)
            let config = match BrowserConfig::from_json(&config_json) {
                Ok(c) => c,
                Err(e) => {
                    callback::send_error("INIT_ERROR", &e, "", "");
                    return;
                }
            };

            // Check browser type — CustomTab and SystemBrowser fall back to system default browser
            if is_system_browser_type(&_type_str) {
                open_system_browser(&config.url);
                return;
            }

            STATE.with(|cell| {
                // Close existing browser if any
                if let Some(old_state) = cell.borrow_mut().take() {
                    cleanup_browser_state(old_state);
                    callback::send_closed();
                }

                // Determine window mode
                let is_editor = IS_EDITOR.load(Ordering::SeqCst);
                let mode = if is_editor {
                    WindowMode::Editor
                } else {
                    WindowMode::Build
                };

                // Determine parent HWND for build mode
                let effective_parent_hwnd = if mode == WindowMode::Build {
                    if parent_hwnd != 0 {
                        parent_hwnd
                    } else {
                        // Try to find Unity's window automatically
                        match window::find_unity_window() {
                            Some(hwnd) => hwnd.0 as isize,
                            None => {
                                callback::send_error(
                                    "WINDOW_ERROR",
                                    "Cannot find Unity window (UnityWndClass) for embedding",
                                    "",
                                    "",
                                );
                                return;
                            }
                        }
                    }
                } else {
                    0 // Editor mode doesn't need a parent
                };

                // Create the browser window
                let window_info = match window::create_browser_window(mode, effective_parent_hwnd, &config) {
                    Ok(info) => info,
                    Err(e) => {
                        callback::send_error("WINDOW_ERROR", &e, "", "");
                        return;
                    }
                };

                // Install mouse hook for close-on-tap-outside if requested
                let mouse_hook = if config.close_on_tap_outside {
                    match window::install_mouse_hook(window_info.hwnd) {
                        Ok(hook) => Some(hook),
                        Err(e) => {
                            // Non-fatal: log but continue without the hook
                            callback::send_error("WINDOW_ERROR", &format!("Mouse hook failed: {}", e), "", "");
                            None
                        }
                    }
                } else {
                    None
                };

                // Create the WebView2 instance inside the window
                match WebViewInstance::create(window_info.hwnd, &config, is_open_flag.clone()) {
                    Ok(wv) => {
                        // Navigate to the configured URL
                        if let Err(e) = wv.navigate(&config.url) {
                            callback::send_error(
                                &e.kind.to_string(),
                                &e.message,
                                &e.url,
                                &e.request_id,
                            );
                        }

                        *cell.borrow_mut() = Some(BrowserState {
                            webview: wv,
                            window_hwnd: window_info.hwnd,
                            close_on_tap_outside: config.close_on_tap_outside,
                            mouse_hook,
                        });
                    }
                    Err(e) => {
                        // Clean up hook if WebView creation failed
                        if let Some(hook) = mouse_hook {
                            window::remove_mouse_hook(hook);
                        }
                        window::destroy_window(window_info.hwnd);
                        callback::send_error(&e.kind.to_string(), &e.message, &e.url, &e.request_id);
                    }
                }
            });
        }

        Command::Close => {
            STATE.with(|cell| {
                if let Some(state) = cell.borrow_mut().take() {
                    cleanup_browser_state(state);
                    callback::send_closed();
                }
            });
        }

        Command::Refresh => {
            STATE.with(|cell| {
                if let Some(ref state) = *cell.borrow() {
                    if let Err(e) = state.webview.refresh() {
                        callback::send_error(&e.kind.to_string(), &e.message, "", "");
                    }
                }
            });
        }

        Command::ExecuteJavaScript(script, request_id) => {
            STATE.with(|cell| {
                if let Some(ref state) = *cell.borrow() {
                    if let Err(e) = state.webview.execute_javascript(&script, request_id.clone()) {
                        callback::send_error(
                            &e.kind.to_string(),
                            &e.message,
                            &e.url,
                            &e.request_id,
                        );
                    }
                } else {
                    callback::send_error(
                        "INTERNAL_ERROR",
                        "Browser not open",
                        "",
                        &request_id,
                    );
                }
            });
        }

        Command::InjectJavaScript(script) => {
            STATE.with(|cell| {
                if let Some(ref state) = *cell.borrow() {
                    if let Err(e) = state.webview.inject_javascript(&script) {
                        callback::send_error(&e.kind.to_string(), &e.message, "", "");
                    }
                }
            });
        }

        Command::SendPostMessage(message) => {
            STATE.with(|cell| {
                if let Some(ref state) = *cell.borrow() {
                    if let Err(e) = state.webview.send_post_message(&message) {
                        callback::send_error(&e.kind.to_string(), &e.message, "", "");
                    }
                }
            });
        }

        Command::IsOpenQuery(result_flag) => {
            STATE.with(|cell| {
                let is_open = cell.borrow().is_some();
                result_flag.store(is_open, Ordering::SeqCst);
            });
        }

        Command::Destroy => {
            STATE.with(|cell| {
                if let Some(state) = cell.borrow_mut().take() {
                    cleanup_browser_state(state);
                    // Don't send OnClosed for destroy — this is a full teardown
                }
            });
        }
    }
}

/// Clean up a browser state: remove mouse hook, close webview, destroy window.
fn cleanup_browser_state(state: BrowserState) {
    if let Some(hook) = state.mouse_hook {
        window::remove_mouse_hook(hook);
    }
    state.webview.close();
    window::destroy_window(state.window_hwnd);
}


/// Check if a browser type string should be handled as a system browser fallback.
/// On Windows, CustomTab and SystemBrowser types fall back to the OS default browser.
fn is_system_browser_type(type_str: &str) -> bool {
    let upper = type_str.to_uppercase();
    upper == "CUSTOMTAB" || upper == "CUSTOM_TAB" || upper == "SYSTEMBROWSER" || upper == "SYSTEM_BROWSER"
}

/// Open a URL in the system's default browser (fallback for CustomTab/SystemBrowser on Windows).
fn open_system_browser(url: &str) {
    let wide_open: Vec<u16> = "open\0".encode_utf16().collect();
    let wide_url: Vec<u16> = url.encode_utf16().chain(std::iter::once(0)).collect();

    let result = unsafe {
        ShellExecuteW(
            None,
            PCWSTR(wide_open.as_ptr()),
            PCWSTR(wide_url.as_ptr()),
            PCWSTR::null(),
            PCWSTR::null(),
            windows::Win32::UI::WindowsAndMessaging::SW_SHOWNORMAL,
        )
    };

    // ShellExecuteW returns HINSTANCE > 32 on success
    if result.0 as usize > 32 {
        callback::send_page_started(url);
        callback::send_closed();
    } else {
        callback::send_error(
            "LOAD_ERROR",
            &format!("Failed to open system browser (ShellExecuteW returned {})", result.0 as usize),
            url,
            "",
        );
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn cstr_to_str_null_returns_empty() {
        let result = cstr_to_str(std::ptr::null());
        assert_eq!(result, "");
    }

    #[test]
    fn cstr_to_str_valid_string() {
        let c_string = std::ffi::CString::new("hello").unwrap();
        let result = cstr_to_str(c_string.as_ptr());
        assert_eq!(result, "hello");
    }

    #[test]
    fn is_open_default_false() {
        // In test context, IS_OPEN should default to false
        assert!(!IS_OPEN.load(Ordering::SeqCst));
    }

    #[test]
    fn is_editor_default_false() {
        assert!(!IS_EDITOR.load(Ordering::SeqCst));
    }

    // ─── CustomTab / SystemBrowser fallback tests ─────────────────────────

    #[test]
    fn is_system_browser_type_custom_tab() {
        assert!(is_system_browser_type("CustomTab"));
    }

    #[test]
    fn is_system_browser_type_custom_tab_case_insensitive() {
        assert!(is_system_browser_type("customtab"));
        assert!(is_system_browser_type("CUSTOMTAB"));
        assert!(is_system_browser_type("Customtab"));
    }

    #[test]
    fn is_system_browser_type_custom_tab_underscore() {
        assert!(is_system_browser_type("Custom_Tab"));
        assert!(is_system_browser_type("CUSTOM_TAB"));
        assert!(is_system_browser_type("custom_tab"));
    }

    #[test]
    fn is_system_browser_type_system_browser() {
        assert!(is_system_browser_type("SystemBrowser"));
        assert!(is_system_browser_type("SYSTEMBROWSER"));
        assert!(is_system_browser_type("systembrowser"));
    }

    #[test]
    fn is_system_browser_type_system_browser_underscore() {
        assert!(is_system_browser_type("System_Browser"));
        assert!(is_system_browser_type("SYSTEM_BROWSER"));
        assert!(is_system_browser_type("system_browser"));
    }

    #[test]
    fn is_system_browser_type_webview_is_not() {
        assert!(!is_system_browser_type("WebView"));
        assert!(!is_system_browser_type("WEBVIEW"));
        assert!(!is_system_browser_type("webview"));
    }

    #[test]
    fn is_system_browser_type_empty_is_not() {
        assert!(!is_system_browser_type(""));
    }

    #[test]
    fn is_system_browser_type_unknown_is_not() {
        assert!(!is_system_browser_type("unknown"));
        assert!(!is_system_browser_type("InAppBrowser"));
    }

    // ─── BrowserConfig close_on_tap_outside parsing ──────────────────────

    #[test]
    fn config_close_on_tap_outside_default_false() {
        let config = BrowserConfig::from_json(r#"{"url":"https://example.com"}"#).unwrap();
        assert!(!config.close_on_tap_outside);
    }

    #[test]
    fn config_close_on_tap_outside_true() {
        let config = BrowserConfig::from_json(
            r#"{"url":"https://example.com","closeOnTapOutside":true}"#,
        ).unwrap();
        assert!(config.close_on_tap_outside);
    }

    #[test]
    fn config_close_on_tap_outside_false_explicit() {
        let config = BrowserConfig::from_json(
            r#"{"url":"https://example.com","closeOnTapOutside":false}"#,
        ).unwrap();
        assert!(!config.close_on_tap_outside);
    }
}
