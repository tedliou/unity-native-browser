#!/usr/bin/env python3
"""
NativeBrowser WebGL Test Server

Serves Unity WebGL builds and provides test pages for verifying the
NativeBrowser WebGL plugin. Supports:

  - Serving a Unity WebGL build directory
  - Same-origin test page (for iframe JS execution tests)
  - Cross-origin test page (served on a second port)
  - Console log collection endpoint (POST /api/logs)
  - Health check endpoint (GET /api/health)

Usage:
    python tools/webgl-test-server.py [BUILD_DIR] [--port PORT] [--cross-origin-port PORT2]

    BUILD_DIR defaults to "webgl-build" in the working directory.

Examples:
    # Serve a WebGL build on port 8080 (cross-origin on 8081)
    python tools/webgl-test-server.py E:/test-project/WebGLBuild

    # Custom ports
    python tools/webgl-test-server.py E:/test-project/WebGLBuild --port 9000 --cross-origin-port 9001
"""

import argparse
import http.server
import json
import os
import socket
import sys
import threading
import time
from datetime import datetime
from pathlib import Path
from urllib.parse import parse_qs, urlparse

# ─── Collected Logs ────────────────────────────────────────────────────────────

_logs = []
_logs_lock = threading.Lock()


# ─── Test Pages ────────────────────────────────────────────────────────────────

SAME_ORIGIN_TEST_PAGE = """\
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>NativeBrowser Test — Same Origin</title>
  <style>
    body { font-family: system-ui, sans-serif; padding: 24px; background: #f5f5f5; }
    h1 { color: #333; }
    .card { background: #fff; border-radius: 8px; padding: 16px; margin: 12px 0;
            box-shadow: 0 1px 3px rgba(0,0,0,0.12); }
    button { padding: 8px 16px; margin: 4px; cursor: pointer; border: 1px solid #ccc;
             border-radius: 4px; background: #fff; }
    button:hover { background: #e8e8e8; }
    #log { background: #1e1e1e; color: #d4d4d4; padding: 12px; border-radius: 4px;
           font-family: monospace; font-size: 13px; max-height: 200px; overflow-y: auto;
           white-space: pre-wrap; }
  </style>
</head>
<body>
  <h1>NativeBrowser Test Page (Same Origin)</h1>

  <div class="card">
    <h3>PostMessage Test</h3>
    <button onclick="sendPostMessage()">Send PostMessage to Unity</button>
    <button onclick="listenPostMessage()">Start Listening</button>
  </div>

  <div class="card">
    <h3>JS Execution Test</h3>
    <p>This page exposes <code>window.testValue</code> and <code>window.getTestResult()</code>
    for verifying ExecuteJavaScript / InjectJavaScript from Unity.</p>
  </div>

  <div class="card">
    <h3>Console</h3>
    <div id="log"></div>
  </div>

  <script>
    window.testValue = "hello-from-test-page";

    window.getTestResult = function() {
      return JSON.stringify({ status: "ok", timestamp: Date.now() });
    };

    function log(msg) {
      var el = document.getElementById("log");
      el.textContent += "[" + new Date().toLocaleTimeString() + "] " + msg + "\\n";
      el.scrollTop = el.scrollHeight;
      // Also post to collection endpoint
      fetch("/api/logs", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ source: "test-page", message: msg, timestamp: Date.now() })
      }).catch(function() {});
    }

    function sendPostMessage() {
      window.parent.postMessage("test-message-from-iframe", "*");
      log("Sent postMessage: test-message-from-iframe");
    }

    function listenPostMessage() {
      window.addEventListener("message", function(e) {
        log("Received postMessage: " + (typeof e.data === "string" ? e.data : JSON.stringify(e.data)));
      });
      log("Listening for postMessages...");
    }

    log("Same-origin test page loaded. Origin: " + window.location.origin);
  </script>
</body>
</html>
"""

CROSS_ORIGIN_TEST_PAGE = """\
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>NativeBrowser Test — Cross Origin</title>
  <style>
    body { font-family: system-ui, sans-serif; padding: 24px; background: #fff3e0; }
    h1 { color: #e65100; }
    .card { background: #fff; border-radius: 8px; padding: 16px; margin: 12px 0;
            box-shadow: 0 1px 3px rgba(0,0,0,0.12); }
    button { padding: 8px 16px; margin: 4px; cursor: pointer; border: 1px solid #ccc;
             border-radius: 4px; background: #fff; }
    button:hover { background: #e8e8e8; }
  </style>
</head>
<body>
  <h1>NativeBrowser Test Page (Cross Origin)</h1>

  <div class="card">
    <h3>Cross-Origin PostMessage Test</h3>
    <p>This page is served on a different port to simulate cross-origin iframe behavior.</p>
    <button onclick="sendPostMessage()">Send PostMessage to Unity</button>
  </div>

  <div class="card">
    <h3>Notes</h3>
    <ul>
      <li>ExecuteJavaScript / InjectJavaScript should log cross-origin warnings</li>
      <li>PostMessage should still work cross-origin</li>
      <li>iframe load event should still fire</li>
    </ul>
  </div>

  <script>
    function sendPostMessage() {
      window.parent.postMessage("cross-origin-test-message", "*");
    }

    window.addEventListener("message", function(e) {
      console.log("[NativeBrowser CrossOrigin] Received:", e.data);
    });
  </script>
</body>
</html>
"""


# ─── Request Handler ───────────────────────────────────────────────────────────


class WebGLTestHandler(http.server.SimpleHTTPRequestHandler):
    """Extends SimpleHTTPRequestHandler with test page routes and API endpoints."""

    def __init__(self, *args, build_dir=None, cross_origin_port=None, **kwargs):
        self.build_dir = build_dir
        self.cross_origin_port = cross_origin_port
        super().__init__(*args, directory=build_dir, **kwargs)

    def do_GET(self):
        parsed = urlparse(self.path)
        path = parsed.path

        if path == "/api/health":
            self._json_response({"status": "ok", "time": datetime.now().isoformat()})
            return

        if path == "/api/logs":
            with _logs_lock:
                data = list(_logs)
            self._json_response({"logs": data, "count": len(data)})
            return

        if path == "/api/logs/clear":
            with _logs_lock:
                _logs.clear()
            self._json_response({"status": "cleared"})
            return

        if path == "/test/same-origin":
            self._html_response(SAME_ORIGIN_TEST_PAGE)
            return

        if path == "/test/cross-origin-info":
            self._json_response(
                {
                    "info": "Cross-origin test page is served on port "
                    + str(self.cross_origin_port),
                    "url": "http://localhost:"
                    + str(self.cross_origin_port)
                    + "/test/cross-origin",
                }
            )
            return

        # Default: serve static files from build directory
        super().do_GET()

    def do_POST(self):
        parsed = urlparse(self.path)
        path = parsed.path

        if path == "/api/logs":
            content_length = int(self.headers.get("Content-Length", 0))
            body = self.rfile.read(content_length)
            try:
                entry = json.loads(body.decode("utf-8"))
                entry["_received"] = datetime.now().isoformat()
                with _logs_lock:
                    _logs.append(entry)
                    # Cap at 1000 entries
                    if len(_logs) > 1000:
                        _logs.pop(0)
                self._json_response({"status": "ok"})
            except json.JSONDecodeError:
                self._json_response({"error": "invalid json"}, status=400)
            return

        self.send_error(404)

    def _json_response(self, data, status=200):
        body = json.dumps(data, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Access-Control-Allow-Origin", "*")
        self.end_headers()
        self.wfile.write(body)

    def _html_response(self, html, status=200):
        body = html.encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "text/html; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, format, *args):
        # Prefix with timestamp for readability
        sys.stderr.write(
            "[%s] %s\n" % (datetime.now().strftime("%H:%M:%S"), format % args)
        )

    def end_headers(self):
        # Add headers needed for Unity WebGL (SharedArrayBuffer requires these)
        self.send_header("Cross-Origin-Opener-Policy", "same-origin")
        self.send_header("Cross-Origin-Embedder-Policy", "require-corp")
        super().end_headers()


class CrossOriginHandler(http.server.BaseHTTPRequestHandler):
    """Minimal handler for the cross-origin test server."""

    def do_GET(self):
        parsed = urlparse(self.path)
        if parsed.path == "/test/cross-origin":
            body = CROSS_ORIGIN_TEST_PAGE.encode("utf-8")
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(body)))
            self.send_header("Access-Control-Allow-Origin", "*")
            self.end_headers()
            self.wfile.write(body)
        else:
            self.send_error(404)

    def log_message(self, format, *args):
        sys.stderr.write(
            "[%s] [cross-origin] %s\n"
            % (datetime.now().strftime("%H:%M:%S"), format % args)
        )


# ─── Server Setup ──────────────────────────────────────────────────────────────


def find_free_port(start=8080, end=9000):
    """Find an available port in the given range."""
    for port in range(start, end):
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
            try:
                s.bind(("127.0.0.1", port))
                return port
            except OSError:
                continue
    raise RuntimeError(f"No free port found in range {start}-{end}")


def make_handler(build_dir, cross_origin_port):
    """Create a handler class with build_dir and cross_origin_port bound."""

    class BoundHandler(WebGLTestHandler):
        def __init__(self, *args, **kwargs):
            super().__init__(
                *args,
                build_dir=build_dir,
                cross_origin_port=cross_origin_port,
                **kwargs,
            )

    return BoundHandler


def run_server(build_dir, port, cross_origin_port):
    """Start both the main and cross-origin servers."""

    build_path = Path(build_dir).resolve()
    if not build_path.exists():
        print(f"Warning: Build directory does not exist yet: {build_path}")
        print(
            "Server will start but file serving will fail until the directory is created."
        )
        # Create it so SimpleHTTPRequestHandler doesn't crash
        build_path.mkdir(parents=True, exist_ok=True)

    # Main server
    handler_cls = make_handler(str(build_path), cross_origin_port)
    main_server = http.server.HTTPServer(("0.0.0.0", port), handler_cls)

    # Cross-origin server
    cross_server = http.server.HTTPServer(
        ("0.0.0.0", cross_origin_port), CrossOriginHandler
    )

    print("=" * 60)
    print("  NativeBrowser WebGL Test Server")
    print("=" * 60)
    print(f"  Build dir:       {build_path}")
    print(f"  Main server:     http://localhost:{port}")
    print(f"  Cross-origin:    http://localhost:{cross_origin_port}")
    print()
    print("  Endpoints:")
    print(f"    WebGL build:   http://localhost:{port}/")
    print(f"    Same-origin:   http://localhost:{port}/test/same-origin")
    print(f"    Cross-origin:  http://localhost:{cross_origin_port}/test/cross-origin")
    print(f"    Health check:  http://localhost:{port}/api/health")
    print(f"    Logs (GET):    http://localhost:{port}/api/logs")
    print(f"    Logs (POST):   http://localhost:{port}/api/logs")
    print(f"    Clear logs:    http://localhost:{port}/api/logs/clear")
    print("=" * 60)
    print("  Press Ctrl+C to stop")
    print()

    # Run cross-origin server in a daemon thread
    cross_thread = threading.Thread(target=cross_server.serve_forever, daemon=True)
    cross_thread.start()

    try:
        main_server.serve_forever()
    except KeyboardInterrupt:
        print("\nShutting down...")
        main_server.shutdown()
        cross_server.shutdown()


# ─── CLI ───────────────────────────────────────────────────────────────────────


def main():
    parser = argparse.ArgumentParser(
        description="NativeBrowser WebGL Test Server",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=__doc__,
    )
    parser.add_argument(
        "build_dir",
        nargs="?",
        default="webgl-build",
        help="Path to Unity WebGL build output directory (default: webgl-build)",
    )
    parser.add_argument(
        "--port",
        "-p",
        type=int,
        default=0,
        help="Main server port (default: auto-detect free port from 8080)",
    )
    parser.add_argument(
        "--cross-origin-port",
        "-c",
        type=int,
        default=0,
        help="Cross-origin test server port (default: main port + 1)",
    )

    args = parser.parse_args()

    # Auto-detect ports if not specified
    if args.port == 0:
        args.port = find_free_port(8080, 9000)

    if args.cross_origin_port == 0:
        args.cross_origin_port = find_free_port(args.port + 1, 9100)

    run_server(args.build_dir, args.port, args.cross_origin_port)


if __name__ == "__main__":
    main()
