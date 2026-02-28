using UnityEngine;
using UnityEngine.UI;
using TedLiou.NativeBrowser;
using NB = TedLiou.NativeBrowser.NativeBrowser;
using TMPro;

namespace TedLiou.Demo
{
    /// <summary>
    /// Demo controller that demonstrates NativeBrowser API usage.
    /// Creates UI programmatically at runtime to support headless builds
    /// where scene-based UI wiring is not available.
    /// </summary>
    public class DemoController : MonoBehaviour
    {
        [Header("UI References (auto-created if null)")]
        [SerializeField] private TMP_Text statusText;

        private ScrollRect scrollRect;
        private TMP_Text logText;

        private void Start()
        {
            // Create UI if not wired in scene
            if (statusText == null)
            {
                CreateUI();
            }

            NB.Initialize();

            // Subscribe to browser events
            NB.OnPageStarted  += OnPageStarted;
            NB.OnPageFinished += OnPageFinished;
            NB.OnError        += OnError;
            NB.OnPostMessage  += OnPostMessage;
            NB.OnJsResult     += OnJsResult;
            NB.OnDeepLink     += OnDeepLink;
            NB.OnClosed       += OnClosed;

            Log("NativeBrowser initialized. Tap a button to open a browser.");
        }

        private void OnDestroy()
        {
            NB.OnPageStarted  -= OnPageStarted;
            NB.OnPageFinished -= OnPageFinished;
            NB.OnError        -= OnError;
            NB.OnPostMessage  -= OnPostMessage;
            NB.OnJsResult     -= OnJsResult;
            NB.OnDeepLink     -= OnDeepLink;
            NB.OnClosed       -= OnClosed;
        }

        // ----- Button handlers -----

        public void OpenWebView()
        {
            var config = new BrowserConfig("https://example.com")
            {
                width             = 0.5f,
                height            = 0.8f,
                alignment         = Alignment.LEFT,
                closeOnTapOutside = true,
                enableJavaScript  = true
            };
            NB.Open(BrowserType.WebView, config);
            Log("Opening WebView: https://example.com");
        }

        public void OpenCustomTab()
        {
            var config = new BrowserConfig("https://example.com");
            NB.Open(BrowserType.CustomTab, config);
            Log("Opening Custom Tab: https://example.com");
        }

        public void OpenSystemBrowser()
        {
            var config = new BrowserConfig("https://example.com");
            NB.Open(BrowserType.SystemBrowser, config);
            Log("Opening System Browser: https://example.com");
        }

        public void CloseBrowser()
        {
            NB.Close();
            Log("Close requested.");
        }

        public void ExecuteJS()
        {
            NB.ExecuteJavaScript("document.title", "req-title");
            Log("Executing JS: document.title");
        }

        public void RefreshBrowser()
        {
            NB.Refresh();
            Log("Refresh requested.");
        }

        // ----- Event callbacks -----

        private void OnPageStarted(string url)  => Log($"Page started: {url}");
        private void OnPageFinished(string url) => Log($"Page finished: {url}");
        private void OnError(string msg, string url) => Log($"Error [{url}]: {msg}");
        private void OnPostMessage(string msg)  => Log($"PostMessage: {msg}");
        private void OnJsResult(string reqId, string result) => Log($"JS result [{reqId}]: {result}");
        private void OnDeepLink(string url)     => Log($"Deep link: {url}");
        private void OnClosed()                 => Log("Browser closed.");

        // ----- Logging -----

        private void Log(string msg)
        {
            Debug.Log($"[NativeBrowser Demo] {msg}");
            if (statusText != null)
                statusText.text = msg;
            if (logText != null)
            {
                logText.text += $"\n{msg}";
                // Auto-scroll to bottom
                if (scrollRect != null)
                    Canvas.ForceUpdateCanvases();
            }
        }

        // ----- Runtime UI Creation -----

        /// <summary>
        /// Creates the entire demo UI programmatically.
        /// This ensures the demo works even without scene-based UI setup,
        /// which is required for headless Unity builds.
        /// </summary>
        private void CreateUI()
        {
            // Canvas
            GameObject canvasGo = new GameObject("DemoCanvas");
            Canvas canvas = canvasGo.AddComponent<Canvas>();
            canvas.renderMode = RenderMode.ScreenSpaceOverlay;
            canvas.sortingOrder = 100;
            CanvasScaler scaler = canvasGo.AddComponent<CanvasScaler>();
            scaler.uiScaleMode = CanvasScaler.ScaleMode.ScaleWithScreenSize;
            scaler.referenceResolution = new Vector2(1080, 1920);
            scaler.matchWidthOrHeight = 0.5f;
            canvasGo.AddComponent<GraphicRaycaster>();

            // EventSystem (only if none exists)
            if (FindAnyObjectByType<UnityEngine.EventSystems.EventSystem>() == null)
            {
                GameObject eventSystemGo = new GameObject("EventSystem");
                eventSystemGo.AddComponent<UnityEngine.EventSystems.EventSystem>();
                eventSystemGo.AddComponent<UnityEngine.InputSystem.UI.InputSystemUIInputModule>();
            }

            // Root layout - vertical
            GameObject rootPanel = CreatePanel(canvasGo.transform, "RootPanel");
            RectTransform rootRect = rootPanel.GetComponent<RectTransform>();
            rootRect.anchorMin = Vector2.zero;
            rootRect.anchorMax = Vector2.one;
            rootRect.offsetMin = new Vector2(20, 20);
            rootRect.offsetMax = new Vector2(-20, -20);
            VerticalLayoutGroup rootLayout = rootPanel.AddComponent<VerticalLayoutGroup>();
            rootLayout.spacing = 12;
            rootLayout.padding = new RectOffset(16, 16, 16, 16);
            rootLayout.childForceExpandWidth = true;
            rootLayout.childForceExpandHeight = false;
            rootLayout.childControlWidth = true;
            rootLayout.childControlHeight = false;

            // Title
            CreateLabel(rootPanel.transform, "NativeBrowser Demo", 40, TextAlignmentOptions.Center, 60);

            // Status text
            GameObject statusGo = CreateLabel(rootPanel.transform, "Ready", 24, TextAlignmentOptions.Left, 40);
            statusText = statusGo.GetComponent<TMP_Text>();

            // Buttons
            CreateButton(rootPanel.transform, "Open WebView", new Color(0.2f, 0.6f, 1f), OpenWebView);
            CreateButton(rootPanel.transform, "Open Custom Tab", new Color(0.3f, 0.7f, 0.3f), OpenCustomTab);
            CreateButton(rootPanel.transform, "Open System Browser", new Color(0.8f, 0.5f, 0.2f), OpenSystemBrowser);
            CreateButton(rootPanel.transform, "Close Browser", new Color(0.8f, 0.3f, 0.3f), CloseBrowser);
            CreateButton(rootPanel.transform, "Execute JS", new Color(0.6f, 0.4f, 0.8f), ExecuteJS);
            CreateButton(rootPanel.transform, "Refresh", new Color(0.5f, 0.5f, 0.5f), RefreshBrowser);

            // Log area (scrollable)
            CreateLogArea(rootPanel.transform);
        }

        private GameObject CreatePanel(Transform parent, string name)
        {
            GameObject panel = new GameObject(name, typeof(RectTransform));
            panel.transform.SetParent(parent, false);
            Image bg = panel.AddComponent<Image>();
            bg.color = new Color(0.1f, 0.1f, 0.12f, 0.95f);
            return panel;
        }

        private GameObject CreateLabel(Transform parent, string text, int fontSize, TextAlignmentOptions align, float height)
        {
            GameObject go = new GameObject("Label", typeof(RectTransform));
            go.transform.SetParent(parent, false);
            LayoutElement le = go.AddComponent<LayoutElement>();
            le.preferredHeight = height;

            TMP_Text tmp = go.AddComponent<TextMeshProUGUI>();
            tmp.text = text;
            tmp.fontSize = fontSize;
            tmp.color = Color.white;
            tmp.alignment = align;
            return go;
        }

        private void CreateButton(Transform parent, string label, Color color, UnityEngine.Events.UnityAction onClick)
        {
            GameObject btnGo = new GameObject(label, typeof(RectTransform));
            btnGo.transform.SetParent(parent, false);
            LayoutElement le = btnGo.AddComponent<LayoutElement>();
            le.preferredHeight = 80;

            Image bg = btnGo.AddComponent<Image>();
            bg.color = color;

            // Make corners slightly rounded via sprite (not needed — plain color is fine)
            Button btn = btnGo.AddComponent<Button>();
            btn.targetGraphic = bg;
            ColorBlock colors = btn.colors;
            colors.highlightedColor = color * 1.2f;
            colors.pressedColor = color * 0.7f;
            btn.colors = colors;

            // Button text
            GameObject textGo = new GameObject("Text", typeof(RectTransform));
            textGo.transform.SetParent(btnGo.transform, false);
            RectTransform textRect = textGo.GetComponent<RectTransform>();
            textRect.anchorMin = Vector2.zero;
            textRect.anchorMax = Vector2.one;
            textRect.offsetMin = Vector2.zero;
            textRect.offsetMax = Vector2.zero;

            TMP_Text tmp = textGo.AddComponent<TextMeshProUGUI>();
            tmp.text = label;
            tmp.fontSize = 28;
            tmp.color = Color.white;
            tmp.alignment = TextAlignmentOptions.Center;

            btn.onClick.AddListener(onClick);
        }

        private void CreateLogArea(Transform parent)
        {
            // Scroll view container
            GameObject scrollGo = new GameObject("LogScroll", typeof(RectTransform));
            scrollGo.transform.SetParent(parent, false);
            LayoutElement scrollLe = scrollGo.AddComponent<LayoutElement>();
            scrollLe.flexibleHeight = 1;
            scrollLe.preferredHeight = 300;

            Image scrollBg = scrollGo.AddComponent<Image>();
            scrollBg.color = new Color(0.05f, 0.05f, 0.07f, 1f);

            scrollRect = scrollGo.AddComponent<ScrollRect>();
            scrollRect.horizontal = false;
            scrollRect.vertical = true;
            scrollRect.movementType = ScrollRect.MovementType.Clamped;

            // Viewport
            GameObject viewportGo = new GameObject("Viewport", typeof(RectTransform));
            viewportGo.transform.SetParent(scrollGo.transform, false);
            RectTransform vpRect = viewportGo.GetComponent<RectTransform>();
            vpRect.anchorMin = Vector2.zero;
            vpRect.anchorMax = Vector2.one;
            vpRect.offsetMin = new Vector2(8, 4);
            vpRect.offsetMax = new Vector2(-8, -4);
            viewportGo.AddComponent<Image>().color = Color.clear;
            viewportGo.AddComponent<Mask>().showMaskGraphic = false;

            scrollRect.viewport = vpRect;

            // Content
            GameObject contentGo = new GameObject("Content", typeof(RectTransform));
            contentGo.transform.SetParent(viewportGo.transform, false);
            RectTransform contentRect = contentGo.GetComponent<RectTransform>();
            contentRect.anchorMin = new Vector2(0, 1);
            contentRect.anchorMax = new Vector2(1, 1);
            contentRect.pivot = new Vector2(0.5f, 1);
            contentRect.offsetMin = Vector2.zero;
            contentRect.offsetMax = Vector2.zero;
            ContentSizeFitter csf = contentGo.AddComponent<ContentSizeFitter>();
            csf.verticalFit = ContentSizeFitter.FitMode.PreferredSize;

            scrollRect.content = contentRect;

            // Log text
            logText = contentGo.AddComponent<TextMeshProUGUI>();
            logText.text = "[Log]";
            logText.fontSize = 20;
            logText.color = new Color(0.7f, 0.9f, 0.7f);
            logText.alignment = TextAlignmentOptions.TopLeft;
        }
    }
}
