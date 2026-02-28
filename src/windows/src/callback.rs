// Callback infrastructure for sending events from the Rust native layer to Unity C#.
// The C# side registers a single function pointer: fn(event_name: *const c_char, json: *const c_char).
// All callbacks are serialized as JSON matching the Android-side event shapes.

use std::ffi::CString;
use std::os::raw::c_char;
use std::sync::Mutex;

use once_cell::sync::Lazy;

use crate::error::escape_json_string;

/// The callback function type registered by Unity C#.
/// Parameters: (event_name, json_payload) — both are null-terminated UTF-8 C strings.
pub type UnityCallback = extern "C" fn(*const c_char, *const c_char);

/// Global storage for the registered Unity callback.
static CALLBACK: Lazy<Mutex<Option<UnityCallback>>> = Lazy::new(|| Mutex::new(None));

/// Register the Unity callback function. Called once from nb_initialize.
pub fn set_callback(cb: UnityCallback) {
    if let Ok(mut guard) = CALLBACK.lock() {
        *guard = Some(cb);
    }
}

/// Clear the registered callback (called on destroy).
pub fn clear_callback() {
    if let Ok(mut guard) = CALLBACK.lock() {
        *guard = None;
    }
}

/// Send an event to Unity. Safe to call from any thread.
/// If no callback is registered or string conversion fails, the call is silently dropped.
fn send_event(event_name: &str, json: &str) {
    let cb = {
        let guard = match CALLBACK.lock() {
            Ok(g) => g,
            Err(_) => return,
        };
        match *guard {
            Some(cb) => cb,
            None => return,
        }
    };

    let c_event = match CString::new(event_name) {
        Ok(s) => s,
        Err(_) => return,
    };
    let c_json = match CString::new(json) {
        Ok(s) => s,
        Err(_) => return,
    };

    cb(c_event.as_ptr(), c_json.as_ptr());
}

// --- Event sender functions matching Android-side callback names ---

/// Fire OnPageStarted with the URL.
pub fn send_page_started(url: &str) {
    let json = format!("{{\"url\":\"{}\"}}", escape_json_string(url));
    send_event("OnPageStarted", &json);
}

/// Fire OnPageFinished with the URL.
pub fn send_page_finished(url: &str) {
    let json = format!("{{\"url\":\"{}\"}}", escape_json_string(url));
    send_event("OnPageFinished", &json);
}

/// Fire OnError with error details.
pub fn send_error(error_type: &str, message: &str, url: &str, request_id: &str) {
    let json = format!(
        "{{\"type\":\"{}\",\"message\":\"{}\",\"url\":\"{}\",\"requestId\":\"{}\"}}",
        escape_json_string(error_type),
        escape_json_string(message),
        escape_json_string(url),
        escape_json_string(request_id),
    );
    send_event("OnError", &json);
}

/// Fire OnPostMessage with the message content.
pub fn send_post_message(message: &str) {
    let json = format!("{{\"message\":\"{}\"}}", escape_json_string(message));
    send_event("OnPostMessage", &json);
}

/// Fire OnJsResult with request ID and result.
pub fn send_js_result(request_id: &str, result: &str) {
    let json = format!(
        "{{\"requestId\":\"{}\",\"result\":\"{}\"}}",
        escape_json_string(request_id),
        escape_json_string(result),
    );
    send_event("OnJsResult", &json);
}

/// Fire OnDeepLink with the deep link URL.
pub fn send_deep_link(url: &str) {
    let json = format!("{{\"url\":\"{}\"}}", escape_json_string(url));
    send_event("OnDeepLink", &json);
}

/// Fire OnClosed.
pub fn send_closed() {
    send_event("OnClosed", "{}");
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::sync::atomic::{AtomicBool, Ordering};

    // We can't easily test the actual callback invocation in unit tests
    // because it requires a function pointer from Unity. But we can test
    // the JSON generation and the safety of send_event when no callback is set.

    #[test]
    fn send_event_without_callback_does_not_panic() {
        // No callback registered — should be a silent no-op
        send_page_started("https://example.com");
        send_page_finished("https://example.com");
        send_error("LOAD_ERROR", "Not found", "https://example.com", "");
        send_post_message("hello");
        send_js_result("req-1", "42");
        send_deep_link("myapp://path");
        send_closed();
    }

    #[test]
    fn set_and_clear_callback() {
        static CALLED: AtomicBool = AtomicBool::new(false);

        extern "C" fn test_cb(_event: *const c_char, _json: *const c_char) {
            CALLED.store(true, Ordering::SeqCst);
        }

        set_callback(test_cb);
        send_closed();
        assert!(CALLED.load(Ordering::SeqCst));

        CALLED.store(false, Ordering::SeqCst);
        clear_callback();
        send_closed();
        assert!(!CALLED.load(Ordering::SeqCst));
    }
}
