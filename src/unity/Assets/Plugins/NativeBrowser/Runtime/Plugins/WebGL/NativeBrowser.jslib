// NativeBrowser WebGL Plugin
// Provides iframe overlay (WebView mode) and window.open (CustomTab/SystemBrowser)
// for Unity WebGL builds. Communicates back to C# via SendMessage.

var NativeBrowserWebGLPlugin = {

    // ─── Shared State ──────────────────────────────────────────────────────────

    $NB_State: {
        initialized: false,
        gameObjectName: "NativeBrowserCallback",
        isOpen: false,
        currentType: null,
        // DOM references
        backdrop: null,
        container: null,
        iframe: null,
        closeButton: null,
        // Config cache
        config: null,
        // Cross-origin tracking
        isCrossOrigin: false,
        iframeOrigin: null
    },

    // ─── Internal Helpers (not exported to C#) ─────────────────────────────────

    $NB_FindCanvas: function() {
        // Multi-fallback strategy to find the Unity canvas in any template
        // Priority: #unity-canvas → Module.canvas → first canvas element
        var canvas = document.getElementById("unity-canvas");
        if (canvas) return canvas;

        // Try Emscripten Module.canvas (available inside .jslib scope)
        if (typeof Module !== "undefined" && Module.canvas) {
            return Module.canvas;
        }

        // Fallback: first canvas element in document
        var canvases = document.getElementsByTagName("canvas");
        if (canvases.length > 0) return canvases[0];

        console.warn("[NativeBrowser] Could not find Unity canvas element");
        return null;
    },

    $NB_SendCallback: function(eventName, jsonData) {
        if (!NB_State.gameObjectName) return;
        try {
            SendMessage(NB_State.gameObjectName, eventName, jsonData);
        } catch (e) {
            console.error("[NativeBrowser] SendMessage failed:", e);
        }
    },

    $NB_ParseConfig: function(configJsonPtr) {
        var configJson = UTF8ToString(configJsonPtr);
        try {
            return JSON.parse(configJson);
        } catch (e) {
            console.error("[NativeBrowser] Failed to parse config JSON:", e);
            return null;
        }
    },

    $NB_InjectStyles: function() {
        if (document.getElementById("nb-webgl-styles")) return;

        var style = document.createElement("style");
        style.id = "nb-webgl-styles";
        style.textContent =
            "#nb-backdrop {" +
                "position: fixed;" +
                "top: 0; left: 0; right: 0; bottom: 0;" +
                "background: rgba(0, 0, 0, 0.5);" +
                "z-index: 10000;" +
                "display: none;" +
                "align-items: center;" +
                "justify-content: center;" +
            "}" +
            "#nb-backdrop.nb-visible {" +
                "display: flex;" +
            "}" +
            "#nb-container {" +
                "position: relative;" +
                "background: #fff;" +
                "border-radius: 8px;" +
                "overflow: hidden;" +
                "box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);" +
                "display: flex;" +
                "flex-direction: column;" +
            "}" +
            "#nb-iframe {" +
                "border: none;" +
                "flex: 1;" +
                "width: 100%;" +
                "background: #fff;" +
            "}" +
            "#nb-close-btn {" +
                "position: absolute;" +
                "top: 4px; right: 4px;" +
                "width: 28px; height: 28px;" +
                "border: none;" +
                "background: rgba(0,0,0,0.5);" +
                "color: #fff;" +
                "border-radius: 50%;" +
                "cursor: pointer;" +
                "font-size: 16px;" +
                "line-height: 28px;" +
                "text-align: center;" +
                "z-index: 10001;" +
                "padding: 0;" +
                "font-family: Arial, sans-serif;" +
            "}" +
            "#nb-close-btn:hover {" +
                "background: rgba(0,0,0,0.7);" +
            "}" +
            // Alignment utilities — applied to #nb-backdrop as flex alignment
            ".nb-align-center { align-items: center; justify-content: center; }" +
            ".nb-align-left { align-items: center; justify-content: flex-start; padding-left: 16px; }" +
            ".nb-align-right { align-items: center; justify-content: flex-end; padding-right: 16px; }" +
            ".nb-align-top { align-items: flex-start; justify-content: center; padding-top: 16px; }" +
            ".nb-align-bottom { align-items: flex-end; justify-content: center; padding-bottom: 16px; }" +
            ".nb-align-top-left { align-items: flex-start; justify-content: flex-start; padding: 16px; }" +
            ".nb-align-top-right { align-items: flex-start; justify-content: flex-end; padding: 16px; }" +
            ".nb-align-bottom-left { align-items: flex-end; justify-content: flex-start; padding: 16px; }" +
            ".nb-align-bottom-right { align-items: flex-end; justify-content: flex-end; padding: 16px; }";

        document.head.appendChild(style);
    },

    $NB_GetAlignmentClass: function(alignment) {
        switch (alignment) {
            case "LEFT":         return "nb-align-left";
            case "RIGHT":        return "nb-align-right";
            case "TOP":          return "nb-align-top";
            case "BOTTOM":       return "nb-align-bottom";
            case "TOP_LEFT":     return "nb-align-top-left";
            case "TOP_RIGHT":    return "nb-align-top-right";
            case "BOTTOM_LEFT":  return "nb-align-bottom-left";
            case "BOTTOM_RIGHT": return "nb-align-bottom-right";
            default:             return "nb-align-center";
        }
    },

    $NB_CheckCrossOrigin: function(url) {
        try {
            var parsed = new URL(url, window.location.href);
            return parsed.origin !== window.location.origin;
        } catch (e) {
            return true; // Assume cross-origin if URL parsing fails
        }
    },

    $NB_CreateDOM: function() {
        // Clean up existing DOM if any
        NB_DestroyDOM();

        NB_InjectStyles();

        // Create backdrop (the semi-transparent overlay)
        var backdrop = document.createElement("div");
        backdrop.id = "nb-backdrop";

        // Create container (holds iframe + close button)
        var container = document.createElement("div");
        container.id = "nb-container";

        // Create close button
        var closeBtn = document.createElement("button");
        closeBtn.id = "nb-close-btn";
        closeBtn.innerHTML = "&#215;";
        closeBtn.setAttribute("aria-label", "Close");

        // Create iframe
        var iframe = document.createElement("iframe");
        iframe.id = "nb-iframe";
        iframe.setAttribute("allow", "autoplay; fullscreen; clipboard-write");
        iframe.setAttribute("sandbox", "allow-scripts allow-same-origin allow-forms allow-popups allow-popups-to-escape-sandbox");

        container.appendChild(closeBtn);
        container.appendChild(iframe);
        backdrop.appendChild(container);
        document.body.appendChild(backdrop);

        NB_State.backdrop = backdrop;
        NB_State.container = container;
        NB_State.iframe = iframe;
        NB_State.closeButton = closeBtn;

        // Close button always closes
        closeBtn.addEventListener("click", function(e) {
            e.stopPropagation();
            NB_Close();
        });
    },

    $NB_DestroyDOM: function() {
        if (NB_State.backdrop) {
            NB_State.backdrop.remove();
            NB_State.backdrop = null;
            NB_State.container = null;
            NB_State.iframe = null;
            NB_State.closeButton = null;
        }
    },

    $NB_SetupIframeListeners: function() {
        var iframe = NB_State.iframe;
        if (!iframe) return;

        // iframe load event fires for both same-origin and cross-origin
        iframe.addEventListener("load", function() {
            if (!NB_State.isOpen) return;

            var url = "";
            try {
                // Same-origin: can read the URL
                url = iframe.contentWindow.location.href;
            } catch (e) {
                // Cross-origin: use the src attribute as fallback
                url = iframe.src || "";
            }

            NB_SendCallback("OnPageFinished", JSON.stringify({ url: url }));
        });

        // Listen for postMessage from iframe content (works cross-origin)
        window.addEventListener("message", NB_State._messageHandler = function(event) {
            if (!NB_State.isOpen || !NB_State.iframe) return;

            // Verify the message comes from our iframe
            try {
                if (NB_State.iframe.contentWindow !== event.source) return;
            } catch (e) {
                // Can't verify source in some cross-origin cases; accept it
            }

            var message = typeof event.data === "string" ? event.data : JSON.stringify(event.data);
            NB_SendCallback("OnPostMessage", JSON.stringify({ message: message }));
        });
    },

    $NB_RemoveIframeListeners: function() {
        if (NB_State._messageHandler) {
            window.removeEventListener("message", NB_State._messageHandler);
            NB_State._messageHandler = null;
        }
    },

    $NB_Close: function() {
        if (!NB_State.isOpen) return;

        NB_State.isOpen = false;
        NB_State.currentType = null;
        NB_State.isCrossOrigin = false;
        NB_State.iframeOrigin = null;

        // Hide backdrop
        if (NB_State.backdrop) {
            NB_State.backdrop.className = "";
            NB_State.backdrop.onclick = null;
        }

        // Clear iframe src to stop loading
        if (NB_State.iframe) {
            NB_State.iframe.src = "about:blank";
        }

        NB_SendCallback("OnClosed", "{}");
    },

    // ─── Exported Functions (called from C#) ───────────────────────────────────

    NB_WebGL_Initialize__deps: [
        "$NB_State", "$NB_FindCanvas", "$NB_SendCallback", "$NB_ParseConfig",
        "$NB_InjectStyles", "$NB_GetAlignmentClass", "$NB_CheckCrossOrigin",
        "$NB_CreateDOM", "$NB_DestroyDOM", "$NB_SetupIframeListeners",
        "$NB_RemoveIframeListeners", "$NB_Close"
    ],
    NB_WebGL_Initialize: function(gameObjectNamePtr) {
        NB_State.gameObjectName = UTF8ToString(gameObjectNamePtr);
        NB_State.initialized = true;

        // Pre-create DOM elements
        NB_CreateDOM();
        NB_SetupIframeListeners();

        console.log("[NativeBrowser] WebGL bridge initialized, callback target: " + NB_State.gameObjectName);
    },

    NB_WebGL_Open__deps: ["$NB_State", "$NB_SendCallback", "$NB_ParseConfig",
        "$NB_GetAlignmentClass", "$NB_CheckCrossOrigin", "$NB_CreateDOM",
        "$NB_SetupIframeListeners", "$NB_FindCanvas", "$NB_Close"],
    NB_WebGL_Open: function(typePtr, configJsonPtr) {
        if (!NB_State.initialized) {
            console.warn("[NativeBrowser] Not initialized — call Initialize() first");
            return;
        }

        var type = UTF8ToString(typePtr);
        var config = NB_ParseConfig(configJsonPtr);
        if (!config || !config.url) {
            console.warn("[NativeBrowser] Invalid config or missing URL");
            return;
        }

        NB_State.config = config;
        NB_State.currentType = type;

        // CustomTab or SystemBrowser → window.open
        if (type === "CustomTab" || type === "SystemBrowser") {
            try {
                var win = window.open(config.url, "_blank");
                if (!win) {
                    console.warn("[NativeBrowser] window.open was blocked by the browser. User interaction may be required.");
                    NB_SendCallback("OnError", JSON.stringify({
                        type: "POPUP_BLOCKED",
                        message: "Browser blocked the popup. Ensure the call is triggered by user interaction.",
                        url: config.url,
                        requestId: ""
                    }));
                }
            } catch (e) {
                NB_SendCallback("OnError", JSON.stringify({
                    type: "OPEN_ERROR",
                    message: e.toString(),
                    url: config.url,
                    requestId: ""
                }));
            }
            // CustomTab/SystemBrowser fire OnClosed immediately since we can't track the new window
            NB_SendCallback("OnClosed", "{}");
            return;
        }

        // WebView mode → iframe overlay
        if (!NB_State.backdrop) {
            NB_CreateDOM();
            NB_SetupIframeListeners();
        }

        // Determine sizes relative to the viewport (matching Android percentage-based approach)
        var canvas = NB_FindCanvas();
        var refWidth = canvas ? canvas.clientWidth : window.innerWidth;
        var refHeight = canvas ? canvas.clientHeight : window.innerHeight;

        var w = Math.round(refWidth * (config.width || 1));
        var h = Math.round(refHeight * (config.height || 1));

        NB_State.container.style.width = w + "px";
        NB_State.container.style.height = h + "px";

        // Apply alignment
        var alignment = config.alignment || "CENTER";
        // Remove previous alignment classes
        NB_State.backdrop.className = "nb-visible " + NB_GetAlignmentClass(alignment);

        // Setup closeOnTapOutside
        if (config.closeOnTapOutside) {
            NB_State.backdrop.onclick = function(e) {
                if (e.target === NB_State.backdrop) {
                    NB_Close();
                }
            };
        } else {
            NB_State.backdrop.onclick = null;
        }

        // Check cross-origin
        NB_State.isCrossOrigin = NB_CheckCrossOrigin(config.url);
        if (NB_State.isCrossOrigin) {
            try {
                NB_State.iframeOrigin = new URL(config.url, window.location.href).origin;
            } catch (e) {
                NB_State.iframeOrigin = "*";
            }
        }

        // Handle enableJavaScript via sandbox attribute
        if (config.enableJavaScript === false) {
            NB_State.iframe.setAttribute("sandbox", "allow-same-origin allow-forms allow-popups");
        } else {
            NB_State.iframe.setAttribute("sandbox", "allow-scripts allow-same-origin allow-forms allow-popups allow-popups-to-escape-sandbox");
        }

        // Navigate iframe
        NB_State.iframe.src = config.url;
        NB_State.isOpen = true;

        // Fire OnPageStarted
        NB_SendCallback("OnPageStarted", JSON.stringify({ url: config.url }));
    },

    NB_WebGL_Close__deps: ["$NB_State", "$NB_SendCallback", "$NB_Close"],
    NB_WebGL_Close: function() {
        NB_Close();
    },

    NB_WebGL_Refresh__deps: ["$NB_State"],
    NB_WebGL_Refresh: function() {
        if (!NB_State.isOpen || !NB_State.iframe) return;

        try {
            // Same-origin: use contentWindow.location.reload()
            NB_State.iframe.contentWindow.location.reload();
        } catch (e) {
            // Cross-origin: re-set the src attribute
            var currentSrc = NB_State.iframe.src;
            NB_State.iframe.src = "about:blank";
            // Small delay to force re-navigation
            setTimeout(function() {
                NB_State.iframe.src = currentSrc;
            }, 50);
        }
    },

    NB_WebGL_IsOpen__deps: ["$NB_State"],
    NB_WebGL_IsOpen: function() {
        return NB_State.isOpen;
    },

    NB_WebGL_ExecuteJavaScript__deps: ["$NB_State", "$NB_SendCallback"],
    NB_WebGL_ExecuteJavaScript: function(scriptPtr, requestIdPtr) {
        if (!NB_State.isOpen || !NB_State.iframe) return;

        var script = UTF8ToString(scriptPtr);
        var requestId = UTF8ToString(requestIdPtr);

        try {
            // Same-origin only — will throw for cross-origin
            var result = NB_State.iframe.contentWindow.eval(script);
            var resultStr = (result === undefined || result === null) ? "" : String(result);

            if (requestId) {
                NB_SendCallback("OnJsResult", JSON.stringify({
                    requestId: requestId,
                    result: resultStr
                }));
            }
        } catch (e) {
            if (NB_State.isCrossOrigin) {
                console.warn("[NativeBrowser] ExecuteJavaScript is not available for cross-origin iframes. URL origin: " +
                    (NB_State.iframeOrigin || "unknown") + ", page origin: " + window.location.origin);
            }

            NB_SendCallback("OnError", JSON.stringify({
                type: "JS_EXEC_ERROR",
                message: e.toString(),
                url: NB_State.iframe.src || "",
                requestId: requestId || ""
            }));
        }
    },

    NB_WebGL_InjectJavaScript__deps: ["$NB_State", "$NB_SendCallback"],
    NB_WebGL_InjectJavaScript: function(scriptPtr) {
        if (!NB_State.isOpen || !NB_State.iframe) return;

        var script = UTF8ToString(scriptPtr);

        try {
            // Same-origin only — will throw for cross-origin
            var doc = NB_State.iframe.contentDocument || NB_State.iframe.contentWindow.document;
            var scriptEl = doc.createElement("script");
            scriptEl.textContent = script;
            doc.head.appendChild(scriptEl);
        } catch (e) {
            if (NB_State.isCrossOrigin) {
                console.warn("[NativeBrowser] InjectJavaScript is not available for cross-origin iframes. URL origin: " +
                    (NB_State.iframeOrigin || "unknown") + ", page origin: " + window.location.origin);
            }

            NB_SendCallback("OnError", JSON.stringify({
                type: "JS_INJECT_ERROR",
                message: e.toString(),
                url: NB_State.iframe.src || "",
                requestId: ""
            }));
        }
    },

    NB_WebGL_SendPostMessage__deps: ["$NB_State"],
    NB_WebGL_SendPostMessage: function(messagePtr) {
        if (!NB_State.isOpen || !NB_State.iframe) return;

        var message = UTF8ToString(messagePtr);

        try {
            // postMessage works cross-origin — use target origin for security
            var targetOrigin = NB_State.isCrossOrigin ? (NB_State.iframeOrigin || "*") : "*";
            NB_State.iframe.contentWindow.postMessage(message, targetOrigin);
        } catch (e) {
            console.error("[NativeBrowser] SendPostMessage failed:", e);
        }
    }
};

autoAddDeps(NativeBrowserWebGLPlugin, "$NB_State");
mergeInto(LibraryManager.library, NativeBrowserWebGLPlugin);
