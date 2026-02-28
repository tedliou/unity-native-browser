// Error types for the NativeBrowser Windows native layer.
// Maps to the BrowserErrorEvent JSON shape expected by Unity C#.

use std::fmt;

/// Error categories matching Android-side error types.
#[derive(Debug, Clone)]
pub enum ErrorKind {
    /// WebView2 environment or controller creation failed.
    InitError,
    /// Page navigation or load error.
    LoadError,
    /// Network-level failure.
    NetworkError,
    /// JavaScript execution error.
    JsError,
    /// WebView2 runtime not installed.
    RuntimeMissing,
    /// Window creation or management error.
    WindowError,
    /// Threading or COM error.
    ThreadError,
    /// Generic / uncategorized.
    InternalError,
}

impl fmt::Display for ErrorKind {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            ErrorKind::InitError => write!(f, "INIT_ERROR"),
            ErrorKind::LoadError => write!(f, "LOAD_ERROR"),
            ErrorKind::NetworkError => write!(f, "NETWORK_ERROR"),
            ErrorKind::JsError => write!(f, "JS_ERROR"),
            ErrorKind::RuntimeMissing => write!(f, "RUNTIME_MISSING"),
            ErrorKind::WindowError => write!(f, "WINDOW_ERROR"),
            ErrorKind::ThreadError => write!(f, "THREAD_ERROR"),
            ErrorKind::InternalError => write!(f, "INTERNAL_ERROR"),
        }
    }
}

/// A browser error that can be serialized to JSON and sent to Unity.
#[derive(Debug, Clone)]
pub struct BrowserError {
    pub kind: ErrorKind,
    pub message: String,
    pub url: String,
    pub request_id: String,
}

impl BrowserError {
    pub fn new(kind: ErrorKind, message: impl Into<String>) -> Self {
        Self {
            kind,
            message: message.into(),
            url: String::new(),
            request_id: String::new(),
        }
    }

    pub fn with_url(mut self, url: impl Into<String>) -> Self {
        self.url = url.into();
        self
    }

    pub fn with_request_id(mut self, request_id: impl Into<String>) -> Self {
        self.request_id = request_id.into();
        self
    }

    /// Serialize to JSON matching Unity's BrowserErrorEvent shape.
    pub fn to_json(&self) -> String {
        // Manual JSON to avoid serde dependency in this module for error paths
        format!(
            "{{\"type\":\"{}\",\"message\":\"{}\",\"url\":\"{}\",\"requestId\":\"{}\"}}",
            self.kind,
            escape_json_string(&self.message),
            escape_json_string(&self.url),
            escape_json_string(&self.request_id),
        )
    }
}

impl fmt::Display for BrowserError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "[{}] {}", self.kind, self.message)
    }
}

impl std::error::Error for BrowserError {}

/// Escape special characters for JSON string values.
pub fn escape_json_string(s: &str) -> String {
    let mut result = String::with_capacity(s.len());
    for c in s.chars() {
        match c {
            '"' => result.push_str("\\\""),
            '\\' => result.push_str("\\\\"),
            '\n' => result.push_str("\\n"),
            '\r' => result.push_str("\\r"),
            '\t' => result.push_str("\\t"),
            c if (c as u32) < 0x20 => {
                result.push_str(&format!("\\u{:04x}", c as u32));
            }
            c => result.push(c),
        }
    }
    result
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn error_kind_display() {
        assert_eq!(ErrorKind::InitError.to_string(), "INIT_ERROR");
        assert_eq!(ErrorKind::LoadError.to_string(), "LOAD_ERROR");
        assert_eq!(ErrorKind::NetworkError.to_string(), "NETWORK_ERROR");
        assert_eq!(ErrorKind::JsError.to_string(), "JS_ERROR");
        assert_eq!(ErrorKind::RuntimeMissing.to_string(), "RUNTIME_MISSING");
        assert_eq!(ErrorKind::WindowError.to_string(), "WINDOW_ERROR");
        assert_eq!(ErrorKind::ThreadError.to_string(), "THREAD_ERROR");
        assert_eq!(ErrorKind::InternalError.to_string(), "INTERNAL_ERROR");
    }

    #[test]
    fn browser_error_to_json() {
        let err = BrowserError::new(ErrorKind::LoadError, "Page not found")
            .with_url("https://example.com/missing")
            .with_request_id("req-1");
        let json = err.to_json();
        assert!(json.contains("\"type\":\"LOAD_ERROR\""));
        assert!(json.contains("\"message\":\"Page not found\""));
        assert!(json.contains("\"url\":\"https://example.com/missing\""));
        assert!(json.contains("\"requestId\":\"req-1\""));
    }

    #[test]
    fn browser_error_to_json_empty_fields() {
        let err = BrowserError::new(ErrorKind::InitError, "Failed");
        let json = err.to_json();
        assert!(json.contains("\"url\":\"\""));
        assert!(json.contains("\"requestId\":\"\""));
    }

    #[test]
    fn escape_json_string_special_chars() {
        assert_eq!(escape_json_string("hello"), "hello");
        assert_eq!(escape_json_string("he\"llo"), "he\\\"llo");
        assert_eq!(escape_json_string("he\\llo"), "he\\\\llo");
        assert_eq!(escape_json_string("line\nnew"), "line\\nnew");
        assert_eq!(escape_json_string("tab\there"), "tab\\there");
    }
}
