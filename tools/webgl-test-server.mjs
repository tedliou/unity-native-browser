#!/usr/bin/env node
/**
 * NativeBrowser WebGL Test Server (Node.js)
 *
 * Serves Unity WebGL builds with proper headers for:
 *   - Brotli-compressed files (.br) with correct Content-Encoding + Content-Type
 *   - COOP/COEP headers required for SharedArrayBuffer (Unity threading)
 *   - Same-origin and cross-origin test pages
 *   - Log collection API for automated testing
 *
 * Usage:
 *   node tools/webgl-test-server.mjs [BUILD_DIR] [--port PORT]
 *
 * Examples:
 *   node tools/webgl-test-server.mjs E:/webgl-build
 *   node tools/webgl-test-server.mjs E:/webgl-build --port 9000
 */

import http from "node:http";
import fs from "node:fs";
import path from "node:path";
import { URL } from "node:url";

// ─── Config ────────────────────────────────────────────────────────────────────

const args = process.argv.slice(2);
let buildDir = "webgl-build";
let port = 8080;

for (let i = 0; i < args.length; i++) {
  if (args[i] === "--port" || args[i] === "-p") {
    port = parseInt(args[++i], 10);
  } else if (!args[i].startsWith("-")) {
    buildDir = args[i];
  }
}

buildDir = path.resolve(buildDir);

// ─── MIME types ────────────────────────────────────────────────────────────────

const MIME_TYPES = {
  ".html": "text/html",
  ".htm": "text/html",
  ".js": "application/javascript",
  ".mjs": "application/javascript",
  ".css": "text/css",
  ".json": "application/json",
  ".png": "image/png",
  ".jpg": "image/jpeg",
  ".jpeg": "image/jpeg",
  ".gif": "image/gif",
  ".svg": "image/svg+xml",
  ".ico": "image/x-icon",
  ".wasm": "application/wasm",
  ".data": "application/octet-stream",
  ".br": "application/octet-stream", // fallback, overridden below
  ".txt": "text/plain",
  ".xml": "application/xml",
  ".woff": "font/woff",
  ".woff2": "font/woff2",
  ".ttf": "font/ttf",
  ".otf": "font/otf",
};

/**
 * For Brotli files, determine MIME from the pre-compressed extension.
 * e.g. "foo.wasm.br" → application/wasm, "foo.js.br" → application/javascript
 */
function getMimeType(filePath) {
  const ext = path.extname(filePath).toLowerCase();
  if (ext === ".br") {
    // Strip .br and check the real extension
    const innerExt = path.extname(filePath.slice(0, -3)).toLowerCase();
    return MIME_TYPES[innerExt] || "application/octet-stream";
  }
  return MIME_TYPES[ext] || "application/octet-stream";
}

// ─── Log Collection ────────────────────────────────────────────────────────────

const logs = [];

// ─── Test Pages ────────────────────────────────────────────────────────────────

const SAME_ORIGIN_TEST_PAGE = `<!DOCTYPE html>
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
</html>`;

// ─── Request Handler ───────────────────────────────────────────────────────────

function handleRequest(req, res) {
  const parsedUrl = new URL(req.url, `http://localhost:${port}`);
  const pathname = decodeURIComponent(parsedUrl.pathname);

  // Add COOP/COEP headers to ALL responses (required for Unity SharedArrayBuffer)
  res.setHeader("Cross-Origin-Opener-Policy", "same-origin");
  res.setHeader("Cross-Origin-Embedder-Policy", "require-corp");

  // ─── API Routes ──────────────────────────────────────────────────────────

  if (pathname === "/api/health") {
    jsonResponse(res, { status: "ok", time: new Date().toISOString() });
    return;
  }

  if (pathname === "/api/logs" && req.method === "GET") {
    jsonResponse(res, { logs, count: logs.length });
    return;
  }

  if (pathname === "/api/logs" && req.method === "POST") {
    let body = "";
    req.on("data", (chunk) => (body += chunk));
    req.on("end", () => {
      try {
        const entry = JSON.parse(body);
        entry._received = new Date().toISOString();
        logs.push(entry);
        if (logs.length > 1000) logs.shift();
        jsonResponse(res, { status: "ok" });
      } catch {
        jsonResponse(res, { error: "invalid json" }, 400);
      }
    });
    return;
  }

  if (pathname === "/api/logs/clear") {
    logs.length = 0;
    jsonResponse(res, { status: "cleared" });
    return;
  }

  if (pathname === "/test/same-origin") {
    htmlResponse(res, SAME_ORIGIN_TEST_PAGE);
    return;
  }

  // ─── Static File Serving ─────────────────────────────────────────────────

  let filePath = path.join(buildDir, pathname === "/" ? "index.html" : pathname);

  // Security: prevent directory traversal
  if (!path.resolve(filePath).startsWith(buildDir)) {
    res.writeHead(403);
    res.end("Forbidden");
    return;
  }

  // Try the exact path first
  if (!fs.existsSync(filePath)) {
    // If the file doesn't exist, check if there's a .br version
    // Unity WebGL loader will request e.g. "Build/foo.wasm" but the file is "Build/foo.wasm.br"
    if (fs.existsSync(filePath + ".br")) {
      filePath = filePath + ".br";
    } else {
      res.writeHead(404);
      res.end("Not Found");
      return;
    }
  }

  const stat = fs.statSync(filePath);
  if (stat.isDirectory()) {
    filePath = path.join(filePath, "index.html");
    if (!fs.existsSync(filePath)) {
      res.writeHead(404);
      res.end("Not Found");
      return;
    }
  }

  const mimeType = getMimeType(filePath);
  const headers = { "Content-Type": mimeType };

  // If serving a .br file, add Content-Encoding: br
  if (filePath.endsWith(".br")) {
    headers["Content-Encoding"] = "br";
  }

  headers["Content-Length"] = fs.statSync(filePath).size;

  res.writeHead(200, headers);
  fs.createReadStream(filePath).pipe(res);
}

function jsonResponse(res, data, status = 200) {
  const body = JSON.stringify(data);
  res.writeHead(status, {
    "Content-Type": "application/json",
    "Content-Length": Buffer.byteLength(body),
    "Access-Control-Allow-Origin": "*",
  });
  res.end(body);
}

function htmlResponse(res, html, status = 200) {
  res.writeHead(status, {
    "Content-Type": "text/html; charset=utf-8",
    "Content-Length": Buffer.byteLength(html),
  });
  res.end(html);
}

// ─── Start Server ──────────────────────────────────────────────────────────────

const server = http.createServer(handleRequest);

server.listen(port, "0.0.0.0", () => {
  console.log("=".repeat(60));
  console.log("  NativeBrowser WebGL Test Server (Node.js)");
  console.log("=".repeat(60));
  console.log(`  Build dir:       ${buildDir}`);
  console.log(`  Server:          http://localhost:${port}`);
  console.log();
  console.log("  Endpoints:");
  console.log(`    WebGL build:   http://localhost:${port}/`);
  console.log(`    Same-origin:   http://localhost:${port}/test/same-origin`);
  console.log(`    Health check:  http://localhost:${port}/api/health`);
  console.log(`    Logs (GET):    http://localhost:${port}/api/logs`);
  console.log(`    Logs (POST):   http://localhost:${port}/api/logs`);
  console.log(`    Clear logs:    http://localhost:${port}/api/logs/clear`);
  console.log("=".repeat(60));
  console.log("  Press Ctrl+C to stop");
  console.log();
});
