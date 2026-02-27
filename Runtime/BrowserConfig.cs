using System;
using System.Collections.Generic;
using UnityEngine;

namespace TedLiou.NativeBrowser
{
    [Serializable]
    public sealed class BrowserConfig
    {
        public string url;
        public float width = 1f;
        public float height = 1f;
        public Alignment alignment = Alignment.CENTER;
        public bool closeOnTapOutside = false;
        public List<string> deepLinkPatterns = new List<string>();
        public bool closeOnDeepLink = true;
        public bool enableJavaScript = true;
        public string userAgent = "";

        public BrowserConfig(string url)
        {
            this.url = url;
        }

        public string ToJson()
        {
            var json = new JsonSerializableWrapper
            {
                url = this.url,
                width = this.width,
                height = this.height,
                alignment = NativeBrowser.GetAlignmentString(this.alignment),
                closeOnTapOutside = this.closeOnTapOutside,
                closeOnDeepLink = this.closeOnDeepLink,
                enableJavaScript = this.enableJavaScript,
                userAgent = this.userAgent,
                deepLinkPatterns = this.deepLinkPatterns
            };
            return JsonUtility.ToJson(json);
        }

        [Serializable]
        private class JsonSerializableWrapper
        {
            public string url;
            public float width;
            public float height;
            public string alignment;
            public bool closeOnTapOutside;
            public bool closeOnDeepLink;
            public bool enableJavaScript;
            public string userAgent;
            public List<string> deepLinkPatterns;
        }
    }
}
