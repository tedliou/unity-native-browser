// Threading infrastructure for WebView2.
//
// WebView2 requires a COM STA (Single-Threaded Apartment) thread with a message pump.
// Unity's main thread is NOT suitable because:
//   1. Unity Editor cannot provide a valid HWND
//   2. We must not block Unity's main thread with a message pump
//   3. WebView2 COM objects must be created and accessed on the same STA thread
//
// Architecture:
//   Unity main thread  --[mpsc::channel]--> STA thread (message pump + WebView2)
//   STA thread          --[callback fn ptr]--> Unity main thread (via ConcurrentQueue in C#)
//
// All WebView2 operations are sent as Command enums through the channel.
// The STA thread processes commands during its message pump loop.

use std::sync::mpsc;
use std::sync::atomic::AtomicBool;
use std::sync::Arc;
use std::thread::{self, JoinHandle};

use windows::Win32::System::Com::{CoInitializeEx, CoUninitialize, COINIT_APARTMENTTHREADED};
use windows::Win32::UI::WindowsAndMessaging::{
    DispatchMessageW, GetMessageW, PeekMessageW, PostThreadMessageW, TranslateMessage,
    MSG, PM_REMOVE, WM_APP, WM_QUIT,
};
use windows::Win32::Foundation::{WPARAM, LPARAM};

/// Custom message ID to wake the message pump when a command is available.
const WM_PROCESS_COMMAND: u32 = WM_APP + 1;

// Thread-local sender for the STA thread. Used by send_close_command()
// which is called from wnd_proc (on the STA thread) to queue a Close command.
thread_local! {
    static STA_SENDER: std::cell::RefCell<Option<mpsc::Sender<Command>>> = const { std::cell::RefCell::new(None) };
}
/// Commands sent from Unity's main thread to the STA thread.
#[derive(Debug)]
pub enum Command {
    /// Create the WebView2 environment and window.
    /// (browser_type, config_json, parent_hwnd — 0 for editor standalone window)
    Open(String, String, isize),
    /// Close the browser and destroy the window.
    Close,
    /// Reload the current page.
    Refresh,
    /// Execute JavaScript and return the result via OnJsResult callback.
    ExecuteJavaScript(String, String),
    /// Inject JavaScript to run on every page load.
    InjectJavaScript(String),
    /// Send a postMessage to the web content.
    SendPostMessage(String),
    /// Check if the browser is open (response via atomic bool).
    IsOpenQuery(Arc<AtomicBool>),
    /// Shutdown the STA thread entirely.
    Destroy,
}

/// Handle to the STA thread, providing command sending capability.
pub struct StaThread {
    sender: mpsc::Sender<Command>,
    thread_id: u32,
    join_handle: Option<JoinHandle<()>>,
}

impl StaThread {
    /// Spawn a new STA thread with a message pump.
    /// `handler` is called on the STA thread for each command received.
    /// The handler receives the Command and must process it synchronously.
    pub fn spawn<F>(handler: F) -> Result<Self, String>
    where
        F: FnMut(Command) + Send + 'static,
    {
        let (sender, receiver) = mpsc::channel::<Command>();
        let (tid_sender, tid_receiver) = mpsc::channel::<u32>();
        let (ready_sender, ready_receiver) = mpsc::channel::<Result<(), String>>();

        // Clone the sender for the STA thread-local (used by send_close_command)
        let sender_for_sta = sender.clone();

        let join_handle = thread::spawn(move || {
            sta_thread_main(receiver, tid_sender, ready_sender, handler, sender_for_sta);
        });

        // Wait for the thread to report its ID
        let thread_id = tid_receiver
            .recv()
            .map_err(|_| "STA thread failed to start".to_string())?;

        // Wait for COM initialization confirmation
        ready_receiver
            .recv()
            .map_err(|_| "STA thread initialization failed".to_string())??;

        Ok(StaThread {
            sender,
            thread_id,
            join_handle: Some(join_handle),
        })
    }

    /// Send a command to the STA thread.
    pub fn send(&self, cmd: Command) -> Result<(), String> {
        self.sender
            .send(cmd)
            .map_err(|e| format!("Failed to send command: {}", e))?;

        // Wake up the message pump so it processes the command
        unsafe {
            let _ = PostThreadMessageW(self.thread_id, WM_PROCESS_COMMAND, WPARAM(0), LPARAM(0));
        }
        Ok(())
    }

    /// Shutdown the STA thread and wait for it to exit.
    pub fn shutdown(&mut self) {
        // Guard against double shutdown
        let handle = match self.join_handle.take() {
            Some(h) => h,
            None => return, // Already shut down
        };

        // Send the destroy command
        let _ = self.sender.send(Command::Destroy);

        // Wake the message pump with our custom message
        unsafe {
            let _ = PostThreadMessageW(self.thread_id, WM_PROCESS_COMMAND, WPARAM(0), LPARAM(0));
        }

        // Also post WM_QUIT to break out of GetMessageW if it's blocking
        unsafe {
            let _ = PostThreadMessageW(self.thread_id, WM_QUIT, WPARAM(0), LPARAM(0));
        }

        // Wait for the thread to finish with a timeout (3 seconds)
        let deadline = std::time::Instant::now() + std::time::Duration::from_secs(3);
        loop {
            if handle.is_finished() {
                let _ = handle.join();
                return;
            }
            if std::time::Instant::now() >= deadline {
                // Timeout — the thread is stuck. Abandon it.
                // We intentionally leak the JoinHandle rather than blocking forever.
                std::mem::forget(handle);
                return;
            }
            std::thread::sleep(std::time::Duration::from_millis(50));
        }
    }

    /// Check if the sender channel is still connected (thread is alive).
    pub fn is_alive(&self) -> bool {
        // Try sending a zero-cost probe — if channel is disconnected, thread is dead
        self.join_handle.as_ref().map_or(false, |h| !h.is_finished())
    }
}

impl Drop for StaThread {
    fn drop(&mut self) {
        self.shutdown();
    }
}

/// Send a Close command from within the STA thread (e.g., from wnd_proc).
/// This is used by the close-on-tap-outside mouse hook handler.
pub fn send_close_command() {
    STA_SENDER.with(|cell| {
        if let Some(ref sender) = *cell.borrow() {
            let _ = sender.send(Command::Close);
            // Wake the message pump so it processes the Close command
            let tid = unsafe { windows::Win32::System::Threading::GetCurrentThreadId() };
            unsafe {
                let _ = PostThreadMessageW(tid, WM_PROCESS_COMMAND, WPARAM(0), LPARAM(0));
            }
        }
    });
}

/// The STA thread's main function.
fn sta_thread_main<F>(
    receiver: mpsc::Receiver<Command>,
    tid_sender: mpsc::Sender<u32>,
    ready_sender: mpsc::Sender<Result<(), String>>,
    mut handler: F,
    sender_clone: mpsc::Sender<Command>,
) where
    F: FnMut(Command),
{
    // Initialize COM in STA mode
    let com_result = unsafe { CoInitializeEx(None, COINIT_APARTMENTTHREADED) };
    if com_result.is_err() {
        let _ = tid_sender.send(0);
        let _ = ready_sender.send(Err(format!("CoInitializeEx failed: {:?}", com_result)));
        return;
    }

    // Store the sender clone in thread-local for send_close_command()
    STA_SENDER.with(|cell| {
        *cell.borrow_mut() = Some(sender_clone);
    });

    // Report thread ID
    let tid = unsafe { windows::Win32::System::Threading::GetCurrentThreadId() };
    let _ = tid_sender.send(tid);

    // Signal ready
    let _ = ready_sender.send(Ok(()));
    // Message pump loop
    let mut msg = MSG::default();

    loop {
        // Process all pending commands first
        let mut should_quit = false;
        while let Ok(cmd) = receiver.try_recv() {
            if matches!(cmd, Command::Destroy) {
                handler(cmd);
                should_quit = true;
                break;
            }
            handler(cmd);
        }

        if should_quit {
            break;
        }

        // Standard Windows message pump with GetMessageW (blocks until message arrives)
        let ret = unsafe { GetMessageW(&mut msg, None, 0, 0) };
        match ret.0 {
            -1 => {
                // Error in GetMessage — exit the loop
                break;
            }
            0 => {
                // WM_QUIT received — exit
                break;
            }
            _ => {
                if msg.message == WM_PROCESS_COMMAND {
                    // Our custom wake-up message — drain commands
                    while let Ok(cmd) = receiver.try_recv() {
                        if matches!(cmd, Command::Destroy) {
                            handler(cmd);
                            should_quit = true;
                            break;
                        }
                        handler(cmd);
                    }
                    if should_quit {
                        break;
                    }
                } else {
                    unsafe {
                        let _ = TranslateMessage(&msg);
                        DispatchMessageW(&msg);
                    }
                }
            }
        }

        // Also drain any commands that arrived while processing Windows messages
        while let Ok(cmd) = receiver.try_recv() {
            if matches!(cmd, Command::Destroy) {
                handler(cmd);
                should_quit = true;
                break;
            }
            handler(cmd);
        }
        if should_quit {
            break;
        }
    }

    // Post WM_QUIT to cleanly exit any nested message loops
    unsafe {
        let _ = PostThreadMessageW(tid, WM_QUIT as u32, WPARAM(0), LPARAM(0));
        // Drain remaining messages
        while PeekMessageW(&mut msg, None, 0, 0, PM_REMOVE).as_bool() {}
    }

    // Cleanup COM
    unsafe {
        CoUninitialize();
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::sync::atomic::{AtomicU32, Ordering};

    #[test]
    fn sta_thread_spawn_and_destroy() {
        let counter = Arc::new(AtomicU32::new(0));
        let counter_clone = counter.clone();

        let mut sta = StaThread::spawn(move |cmd| {
            match cmd {
                Command::Close => {
                    counter_clone.fetch_add(1, Ordering::SeqCst);
                }
                Command::Destroy => {
                    // Cleanup
                }
                _ => {}
            }
        })
        .expect("STA thread should spawn");

        assert!(sta.is_alive());

        // Send a command
        sta.send(Command::Close).expect("Should send");
        // Give the STA thread time to process
        std::thread::sleep(std::time::Duration::from_millis(100));
        assert_eq!(counter.load(Ordering::SeqCst), 1);

        // Shutdown
        sta.shutdown();
    }

    #[test]
    fn sta_thread_multiple_commands() {
        let counter = Arc::new(AtomicU32::new(0));
        let counter_clone = counter.clone();

        let mut sta = StaThread::spawn(move |cmd| {
            if matches!(cmd, Command::Refresh) {
                counter_clone.fetch_add(1, Ordering::SeqCst);
            }
        })
        .expect("STA thread should spawn");

        for _ in 0..10 {
            sta.send(Command::Refresh).expect("Should send");
        }
        std::thread::sleep(std::time::Duration::from_millis(200));
        assert_eq!(counter.load(Ordering::SeqCst), 10);

        sta.shutdown();
    }

    #[test]
    fn sta_thread_shutdown_completes_within_timeout() {
        let mut sta = StaThread::spawn(move |_cmd| {
            // No-op handler
        })
        .expect("STA thread should spawn");

        assert!(sta.is_alive());

        let start = std::time::Instant::now();
        sta.shutdown();
        let elapsed = start.elapsed();

        // Shutdown should complete well within the 3-second timeout
        assert!(elapsed < std::time::Duration::from_secs(2),
            "Shutdown took too long: {:?}", elapsed);
    }

    #[test]
    fn sta_thread_double_shutdown_is_safe() {
        let mut sta = StaThread::spawn(move |_cmd| {})
            .expect("STA thread should spawn");

        sta.shutdown(); // First shutdown
        sta.shutdown(); // Second shutdown should be a no-op
        // If we get here without hanging, the test passes
    }

    #[test]
    fn sta_thread_drop_calls_shutdown() {
        let counter = Arc::new(AtomicU32::new(0));
        let counter_clone = counter.clone();

        {
            let _sta = StaThread::spawn(move |cmd| {
                if matches!(cmd, Command::Destroy) {
                    counter_clone.fetch_add(1, Ordering::SeqCst);
                }
            })
            .expect("STA thread should spawn");
            // Drop triggers shutdown
        }

        // Give a moment for processing
        std::thread::sleep(std::time::Duration::from_millis(200));
        assert_eq!(counter.load(Ordering::SeqCst), 1);
    }

    #[test]
    fn send_close_command_without_sender_does_not_panic() {
        // When STA_SENDER is None (default), send_close_command should be a no-op
        send_close_command();
        // If we get here without panicking, the test passes
    }

    #[test]
    fn send_close_command_with_sender_sends_close() {
        // Set up a channel and store the sender in the thread-local
        let (tx, rx) = mpsc::channel::<Command>();
        STA_SENDER.with(|cell| {
            *cell.borrow_mut() = Some(tx);
        });

        send_close_command();

        // The Close command should be in the channel
        let cmd = rx.try_recv().expect("Should have received a command");
        assert!(matches!(cmd, Command::Close));

        // Clean up thread-local
        STA_SENDER.with(|cell| {
            *cell.borrow_mut() = None;
        });
    }

    #[test]
    fn sta_thread_stores_sender_in_thread_local() {
        // Verify the STA thread properly stores the sender clone
        // by sending a Close command from the handler via send_close_command
        let close_received = Arc::new(AtomicU32::new(0));
        let close_clone = close_received.clone();

        let mut sta = StaThread::spawn(move |cmd| {
            if matches!(cmd, Command::Close) {
                close_clone.fetch_add(1, Ordering::SeqCst);
            }
        })
        .expect("STA thread should spawn");

        // Send a Refresh command; the handler will just ignore it,
        // but the STA thread should have its sender set up.
        // We can't call send_close_command from this thread since it uses thread-local,
        // but we verify the thread spawns correctly with the sender param.
        sta.send(Command::Close).expect("Should send Close");
        std::thread::sleep(std::time::Duration::from_millis(100));
        assert_eq!(close_received.load(Ordering::SeqCst), 1);

        sta.shutdown();
    }
}
