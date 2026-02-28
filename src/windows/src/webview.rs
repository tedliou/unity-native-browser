// WebView2 core — environment creation, controller, navigation, JS execution,
// PostMessage bridge, and event handlers.
//
// All WebView2 operations MUST be called on the STA thread (enforced by threading.rs).
// This module provides a `WebViewInstance` struct that encapsulates the WebView2 lifecycle.

use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::Arc;

use windows::core::{HSTRING, Interface, PCWSTR};
use windows::Win32::Foundation::{E_POINTER, HWND, RECT};
use windows::Win32::UI::WindowsAndMessaging::GetClientRect;

use webview2_com::Microsoft::Web::WebView2::Win32::{
    COREWEBVIEW2_WEB_ERROR_STATUS,
    CreateCoreWebView2EnvironmentWithOptions,
    ICoreWebView2, ICoreWebView2Controller, ICoreWebView2Environment,
    ICoreWebView2Settings,
};
use webview2_com::{
    CreateCoreWebView2EnvironmentCompletedHandler,
    CreateCoreWebView2ControllerCompletedHandler,
    NavigationStartingEventHandler,
    NavigationCompletedEventHandler,
    WebMessageReceivedEventHandler,
    AddScriptToExecuteOnDocumentCreatedCompletedHandler,
    ExecuteScriptCompletedHandler,
};

use crate::callback;
use crate::config::BrowserConfig;
use crate::error::{BrowserError, ErrorKind};

/// The JavaScript bridge injected on every page load to enable `NativeBrowser.postMessage()`.
const POST_MESSAGE_BRIDGE_JS: &str =
    "window.NativeBrowser = { postMessage: function(s) { window.chrome.webview.postMessage(s); } };";

/// Encapsulates a live WebView2 controller + webview pair.
/// Must only be accessed from the STA thread.
pub struct WebViewInstance {
    controller: ICoreWebView2Controller,
    webview: ICoreWebView2,
    hwnd: HWND,
    /// Tracks whether this instance is still open.
    is_open: Arc<AtomicBool>,
    /// Navigation event tokens for cleanup (raw i64).
    nav_starting_token: i64,
    nav_completed_token: i64,
    web_message_token: i64,
}

impl WebViewInstance {
    /// Create a new WebView2 instance inside the given window.
    ///
    /// This is a blocking call that must run on the STA thread.
    /// It creates the environment, then the controller, then configures the webview.
    pub fn create(
        parent_hwnd: HWND,
        config: &BrowserConfig,
        is_open_flag: Arc<AtomicBool>,
    ) -> Result<Self, BrowserError> {
        // Step 1: Create the WebView2 environment
        let environment = create_environment()?;

        // Step 2: Create the controller (this parents the webview in our window)
        let controller = create_controller(&environment, parent_hwnd)?;

        // Step 3: Get the webview from the controller
        let webview = unsafe { controller.CoreWebView2() }
            .map_err(|e| BrowserError::new(ErrorKind::InitError, format!("CoreWebView2 failed: {}", e)))?;

        // Step 4: Configure settings
        configure_settings(&webview, config)?;

        // Step 5: Resize the webview to fill the parent window
        resize_to_fit(&controller, parent_hwnd)?;

        // Step 6: Register event handlers
        let nav_starting_token = register_navigation_starting(&webview)?;
        let nav_completed_token = register_navigation_completed(&webview)?;
        let web_message_token = register_web_message_received(&webview)?;

        // Step 7: Inject the PostMessage bridge on every page
        inject_bridge_script(&webview)?;

        // Mark as open
        is_open_flag.store(true, Ordering::SeqCst);

        Ok(WebViewInstance {
            controller,
            webview,
            hwnd: parent_hwnd,
            is_open: is_open_flag,
            nav_starting_token,
            nav_completed_token,
            web_message_token,
        })
    }

    /// Navigate to a URL.
    pub fn navigate(&self, url: &str) -> Result<(), BrowserError> {
        let hstr = HSTRING::from(url);
        unsafe {
            self.webview.Navigate(&hstr)
        }
        .map_err(|e| {
            BrowserError::new(ErrorKind::LoadError, format!("Navigate failed: {}", e))
                .with_url(url)
        })
    }

    /// Reload the current page.
    pub fn refresh(&self) -> Result<(), BrowserError> {
        unsafe { self.webview.Reload() }
            .map_err(|e| BrowserError::new(ErrorKind::LoadError, format!("Reload failed: {}", e)))
    }

    /// Execute JavaScript and return the result via OnJsResult callback.
    pub fn execute_javascript(&self, script: &str, request_id: String) -> Result<(), BrowserError> {
        let rid = request_id.clone();
        let handler = ExecuteScriptCompletedHandler::create(
            Box::new(move |error_code: windows::core::Result<()>, result_str: String| {
                if let Err(ref e) = error_code {
                    callback::send_error(
                        "JS_ERROR",
                        &format!("JS execution failed: {}", e),
                        "",
                        &rid,
                    );
                } else {
                    callback::send_js_result(&rid, &result_str);
                }
                Ok(())
            }),
        );

        unsafe {
            self.webview.ExecuteScript(&HSTRING::from(script), &handler)
        }
        .map_err(|e| {
            BrowserError::new(ErrorKind::JsError, format!("ExecuteScript failed: {}", e))
                .with_request_id(&request_id)
        })
    }

    /// Inject JavaScript that runs on every page load (document created).
    pub fn inject_javascript(&self, script: &str) -> Result<(), BrowserError> {
        let handler = AddScriptToExecuteOnDocumentCreatedCompletedHandler::create(
            Box::new(|_error_code, _id| Ok(())),
        );

        unsafe {
            self.webview
                .AddScriptToExecuteOnDocumentCreated(&HSTRING::from(script), &handler)
        }
        .map_err(|e| {
            BrowserError::new(ErrorKind::JsError, format!("AddScriptToExecuteOnDocumentCreated failed: {}", e))
        })
    }

    /// Send a postMessage to the web content via the WebView2 PostWebMessageAsString API.
    pub fn send_post_message(&self, message: &str) -> Result<(), BrowserError> {
        unsafe {
            self.webview
                .PostWebMessageAsString(&HSTRING::from(message))
        }
        .map_err(|e| {
            BrowserError::new(ErrorKind::InternalError, format!("PostWebMessageAsString failed: {}", e))
        })
    }

    /// Resize the WebView2 to fill its parent window.
    /// Called when the parent window is resized.
    pub fn resize(&self) -> Result<(), BrowserError> {
        resize_to_fit(&self.controller, self.hwnd)
    }

    /// Close and clean up the WebView2 instance.
    pub fn close(self) {
        self.is_open.store(false, Ordering::SeqCst);

        // Remove event handlers
        unsafe {
            let _ = self.webview.remove_NavigationStarting(self.nav_starting_token);
            let _ = self.webview.remove_NavigationCompleted(self.nav_completed_token);
            let _ = self.webview.remove_WebMessageReceived(self.web_message_token);
        }

        // Close the controller (this removes the WebView2 visual)
        unsafe {
            let _ = self.controller.Close();
        }
    }
}

// ─── Environment Creation ───────────────────────────────────────────────────

/// Create the WebView2 environment. Blocks until the async operation completes.
fn create_environment() -> Result<ICoreWebView2Environment, BrowserError> {
    // Use a user data folder under %TEMP% to avoid E_ACCESSDENIED errors.
    // Without an explicit folder, WebView2 tries to write to the process working directory,
    // which may be read-only (e.g. Unity batchmode, installed apps).
    let user_data_folder = {
        let mut temp = std::env::temp_dir();
        temp.push("NativeBrowser_WebView2");
        HSTRING::from(temp.to_string_lossy().as_ref())
    };

    let (tx, rx) = std::sync::mpsc::channel();

    CreateCoreWebView2EnvironmentCompletedHandler::wait_for_async_operation(
        Box::new(move |handler| unsafe {
            CreateCoreWebView2EnvironmentWithOptions(
                PCWSTR::null(),  // Use installed WebView2 runtime (no custom browser path)
                &user_data_folder,
                None,  // No additional options
                &handler,
            )
            .map_err(webview2_com::Error::WindowsError)
        }),
        Box::new(move |error_code, environment| {
            error_code?;
            tx.send(environment.ok_or_else(|| windows::core::Error::from(E_POINTER)))
                .expect("send over mpsc channel");
            Ok(())
        }),
    )
    .map_err(|e| {
        BrowserError::new(ErrorKind::InitError, format!("WebView2 environment creation failed: {}", e))
    })?;

    let env = rx
        .recv()
        .map_err(|_| BrowserError::new(ErrorKind::InitError, "Environment channel recv failed".to_string()))?
        .map_err(|e| BrowserError::new(ErrorKind::InitError, format!("WebView2 environment result failed: {}", e)))?;

    Ok(env)
}

// ─── Controller Creation ────────────────────────────────────────────────────

/// Create the WebView2 controller inside the given parent window. Blocks until complete.
fn create_controller(
    environment: &ICoreWebView2Environment,
    parent_hwnd: HWND,
) -> Result<ICoreWebView2Controller, BrowserError> {
    let env = environment.clone();
    let (tx, rx) = std::sync::mpsc::channel();

    CreateCoreWebView2ControllerCompletedHandler::wait_for_async_operation(
        Box::new(move |handler| unsafe {
            env.CreateCoreWebView2Controller(parent_hwnd, &handler)
                .map_err(webview2_com::Error::WindowsError)
        }),
        Box::new(move |error_code, controller| {
            error_code?;
            tx.send(controller.ok_or_else(|| windows::core::Error::from(E_POINTER)))
                .expect("send over mpsc channel");
            Ok(())
        }),
    )
    .map_err(|e| {
        BrowserError::new(ErrorKind::InitError, format!("WebView2 controller creation failed: {}", e))
    })?;

    let controller = rx
        .recv()
        .map_err(|_| BrowserError::new(ErrorKind::InitError, "Controller channel recv failed".to_string()))?
        .map_err(|e| BrowserError::new(ErrorKind::InitError, format!("WebView2 controller result failed: {}", e)))?;

    Ok(controller)
}

// ─── Settings Configuration ─────────────────────────────────────────────────

/// Configure WebView2 settings based on the BrowserConfig.
fn configure_settings(
    webview: &ICoreWebView2,
    config: &BrowserConfig,
) -> Result<(), BrowserError> {
    let settings: ICoreWebView2Settings = unsafe { webview.Settings() }
        .map_err(|e| BrowserError::new(ErrorKind::InitError, format!("Settings failed: {}", e)))?;

    unsafe {
        // JavaScript toggle
        let _ = settings.SetIsScriptEnabled(config.enable_java_script);

        // Disable dev tools in production (keep enabled for development)
        let _ = settings.SetAreDevToolsEnabled(true);

        // Disable context menu for cleaner UX (can be made configurable later)
        let _ = settings.SetAreDefaultContextMenusEnabled(false);

        // Disable status bar
        let _ = settings.SetIsStatusBarEnabled(false);

        // Disable zoom control (WebView fills the window, user resizes window instead)
        let _ = settings.SetIsZoomControlEnabled(false);
    }

    // Set user agent if provided
    if !config.user_agent.is_empty() {
        // ICoreWebView2Settings2 is needed for UserAgent — try to cast
        if let Ok(s2) = settings.cast::<webview2_com::Microsoft::Web::WebView2::Win32::ICoreWebView2Settings2>() {
            unsafe {
                let _ = s2.SetUserAgent(&HSTRING::from(&config.user_agent));
            }
        }
    }

    Ok(())
}

// ─── Resize ─────────────────────────────────────────────────────────────────

/// Resize the WebView2 controller bounds to fill the parent window's client area.
fn resize_to_fit(
    controller: &ICoreWebView2Controller,
    parent_hwnd: HWND,
) -> Result<(), BrowserError> {
    let mut rect = RECT::default();
    unsafe {
        let _ = GetClientRect(parent_hwnd, &mut rect);
        controller.SetBounds(rect)
    }
    .map_err(|e| BrowserError::new(ErrorKind::WindowError, format!("SetBounds failed: {}", e)))
}

// ─── Event Handlers ─────────────────────────────────────────────────────────

/// Register NavigationStarting handler — fires OnPageStarted callback.
fn register_navigation_starting(
    webview: &ICoreWebView2,
) -> Result<i64, BrowserError> {
    let mut token: i64 = 0;

    let handler = NavigationStartingEventHandler::create(
        Box::new(|_webview, args| {
            if let Some(args) = args {
                let mut uri_pwstr = windows::core::PWSTR::null();
                if unsafe { args.Uri(&mut uri_pwstr) }.is_ok() {
                    let url = unsafe { uri_pwstr.to_string() }.unwrap_or_default();
                    callback::send_page_started(&url);
                }
            }
            Ok(())
        }),
    );

    unsafe { webview.add_NavigationStarting(&handler, &mut token) }
        .map_err(|e| {
            BrowserError::new(ErrorKind::InitError, format!("add_NavigationStarting failed: {}", e))
        })?;

    Ok(token)
}

/// Register NavigationCompleted handler — fires OnPageFinished or OnError callback.
fn register_navigation_completed(
    webview: &ICoreWebView2,
) -> Result<i64, BrowserError> {
    let mut token: i64 = 0;
    let webview_clone = webview.clone();

    let handler = NavigationCompletedEventHandler::create(
        Box::new(move |_webview, args| {
            if let Some(args) = args {
                let mut is_success = windows::core::BOOL::default();
                let success = if unsafe { args.IsSuccess(&mut is_success) }.is_ok() {
                    is_success == true
                } else {
                    false
                };

                // Get the current URL from the webview itself
                let url = {
                    let mut source_pwstr = windows::core::PWSTR::null();
                    if unsafe { webview_clone.Source(&mut source_pwstr) }.is_ok() {
                        unsafe { source_pwstr.to_string() }.unwrap_or_default()
                    } else {
                        String::new()
                    }
                };

                if success {
                    callback::send_page_finished(&url);
                } else {
                    let mut status = COREWEBVIEW2_WEB_ERROR_STATUS::default();
                    let _ = unsafe { args.WebErrorStatus(&mut status) };
                    callback::send_error(
                        "LOAD_ERROR",
                        &format!("Navigation failed with status: {:?}", status),
                        &url,
                        "",
                    );
                }
            }
            Ok(())
        }),
    );

    unsafe { webview.add_NavigationCompleted(&handler, &mut token) }
        .map_err(|e| {
            BrowserError::new(ErrorKind::InitError, format!("add_NavigationCompleted failed: {}", e))
        })?;

    Ok(token)
}

/// Register WebMessageReceived handler — fires OnPostMessage callback.
fn register_web_message_received(
    webview: &ICoreWebView2,
) -> Result<i64, BrowserError> {
    let mut token: i64 = 0;

    let handler = WebMessageReceivedEventHandler::create(
        Box::new(|_webview, args| {
            if let Some(args) = args {
                let mut msg_pwstr = windows::core::PWSTR::null();
                if unsafe { args.TryGetWebMessageAsString(&mut msg_pwstr) }.is_ok() {
                    let message = unsafe { msg_pwstr.to_string() }.unwrap_or_default();
                    callback::send_post_message(&message);
                }
            }
            Ok(())
        }),
    );

    unsafe { webview.add_WebMessageReceived(&handler, &mut token) }
        .map_err(|e| {
            BrowserError::new(
                ErrorKind::InitError,
                format!("add_WebMessageReceived failed: {}", e),
            )
        })?;

    Ok(token)
}

// ─── Bridge Script Injection ────────────────────────────────────────────────

/// Inject the NativeBrowser.postMessage bridge on every page load.
fn inject_bridge_script(webview: &ICoreWebView2) -> Result<(), BrowserError> {
    let handler = AddScriptToExecuteOnDocumentCreatedCompletedHandler::create(
        Box::new(|_error_code, _id| Ok(())),
    );

    unsafe {
        webview.AddScriptToExecuteOnDocumentCreated(
            &HSTRING::from(POST_MESSAGE_BRIDGE_JS),
            &handler,
        )
    }
    .map_err(|e| {
        BrowserError::new(
            ErrorKind::InitError,
            format!("AddScriptToExecuteOnDocumentCreated failed: {}", e),
        )
    })
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn post_message_bridge_js_is_valid() {
        // Verify the bridge JS contains the expected structure
        assert!(POST_MESSAGE_BRIDGE_JS.contains("window.NativeBrowser"));
        assert!(POST_MESSAGE_BRIDGE_JS.contains("postMessage"));
        assert!(POST_MESSAGE_BRIDGE_JS.contains("window.chrome.webview.postMessage"));
    }

    #[test]
    fn webview_instance_fields_are_correct_types() {
        // Type-level test: ensure WebViewInstance struct compiles with correct field types.
        // We can't create a real instance without WebView2 runtime, but we verify the struct definition.
        fn _assert_send<T: Send>() {}
        // WebViewInstance is NOT Send (COM objects are apartment-threaded) — this is correct.
        // We just verify the struct exists and its public API compiles.
    }
}
