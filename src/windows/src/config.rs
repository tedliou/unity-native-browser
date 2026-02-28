// Configuration parsing for browser open requests.
// Deserializes JSON from Unity's BrowserConfig.ToJson() output.

use serde::Deserialize;

/// Browser configuration received from Unity C#.
/// Matches the JSON shape produced by BrowserConfig.ToJson().
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct BrowserConfig {
    pub url: String,
    #[serde(default = "default_one")]
    pub width: f32,
    #[serde(default = "default_one")]
    pub height: f32,
    #[serde(default = "default_alignment")]
    pub alignment: String,
    #[serde(default)]
    pub close_on_tap_outside: bool,
    #[serde(default)]
    pub close_on_deep_link: bool,
    #[serde(default = "default_true")]
    pub enable_java_script: bool,
    #[serde(default)]
    pub user_agent: String,
    #[serde(default)]
    pub deep_link_patterns: Vec<String>,
}

fn default_one() -> f32 {
    1.0
}

fn default_alignment() -> String {
    "CENTER".to_string()
}

fn default_true() -> bool {
    true
}

impl BrowserConfig {
    /// Parse a JSON string into a BrowserConfig.
    pub fn from_json(json: &str) -> Result<Self, String> {
        serde_json::from_str(json).map_err(|e| format!("Failed to parse BrowserConfig: {}", e))
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn parse_full_config() {
        let json = r#"{
            "url": "https://example.com",
            "width": 0.9,
            "height": 0.8,
            "alignment": "CENTER",
            "closeOnTapOutside": true,
            "closeOnDeepLink": false,
            "enableJavaScript": true,
            "userAgent": "TestAgent",
            "deepLinkPatterns": ["myapp://"]
        }"#;
        let config = BrowserConfig::from_json(json).unwrap();
        assert_eq!(config.url, "https://example.com");
        assert!((config.width - 0.9).abs() < f32::EPSILON);
        assert!((config.height - 0.8).abs() < f32::EPSILON);
        assert_eq!(config.alignment, "CENTER");
        assert!(config.close_on_tap_outside);
        assert!(!config.close_on_deep_link);
        assert!(config.enable_java_script);
        assert_eq!(config.user_agent, "TestAgent");
        assert_eq!(config.deep_link_patterns, vec!["myapp://"]);
    }

    #[test]
    fn parse_minimal_config() {
        let json = r#"{"url": "https://example.com"}"#;
        let config = BrowserConfig::from_json(json).unwrap();
        assert_eq!(config.url, "https://example.com");
        assert!((config.width - 1.0).abs() < f32::EPSILON);
        assert!((config.height - 1.0).abs() < f32::EPSILON);
        assert_eq!(config.alignment, "CENTER");
        assert!(!config.close_on_tap_outside);
        assert!(config.enable_java_script);
    }

    #[test]
    fn parse_invalid_json() {
        let result = BrowserConfig::from_json("not json");
        assert!(result.is_err());
    }

    #[test]
    fn parse_empty_json() {
        // Missing required field "url"
        let result = BrowserConfig::from_json("{}");
        assert!(result.is_err());
    }
}
