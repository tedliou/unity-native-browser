using System;
using UnityEngine;

namespace TedLiou.NativeBrowser.Internal
{
    /// <summary>
    /// Internal JSON parsing utilities for browser callbacks.
    /// This utility class provides helper methods for JSON deserialization using Unity's JsonUtility.
    /// </summary>
    internal static class JsonHelper
    {
        /// <summary>
        /// Parse a JSON string to a typed object using Unity's JsonUtility.
        /// </summary>
        /// <typeparam name="T">The target type to deserialize to.</typeparam>
        /// <param name="json">The JSON string to parse.</param>
        /// <returns>The deserialized object, or default(T) if parsing fails.</returns>
        public static T ParseJson<T>(string json)
        {
            if (string.IsNullOrEmpty(json))
            {
                Debug.LogWarning("JsonHelper: Attempting to parse null or empty JSON string");
                return default(T);
            }

            try
            {
                return JsonUtility.FromJson<T>(json);
            }
            catch (ArgumentException ex)
            {
                Debug.LogWarning($"JsonHelper: Failed to parse JSON - Invalid format: {ex.Message}");
                return default(T);
            }
            catch (Exception ex)
            {
                Debug.LogWarning($"JsonHelper: Failed to parse JSON: {ex.Message}");
                return default(T);
            }
        }

        /// <summary>
        /// Safely extract a string field from JSON using JsonUtility.
        /// Useful for single-field extraction without full deserialization.
        /// </summary>
        /// <param name="json">The JSON string to parse.</param>
        /// <param name="fieldName">The field name to extract.</param>
        /// <returns>The string value of the field, or null if not found or parsing fails.</returns>
        public static string GetStringField(string json, string fieldName)
        {
            if (string.IsNullOrEmpty(json))
            {
                return null;
            }

            try
            {
                // Create a simple wrapper to deserialize a single field
                string wrappedJson = $"{{\"value\": {ExtractJsonValue(json, fieldName)}}}";
                var wrapper = JsonUtility.FromJson<StringWrapper>(wrappedJson);
                return wrapper?.value;
            }
            catch
            {
                return null;
            }
        }

        /// <summary>
        /// Extract a raw JSON value from a JSON object by field name.
        /// </summary>
        /// <param name="json">The JSON string.</param>
        /// <param name="fieldName">The field name to extract.</param>
        /// <returns>The raw JSON value (including quotes for strings).</returns>
        private static string ExtractJsonValue(string json, string fieldName)
        {
            int startIndex = json.IndexOf($"\"{fieldName}\"");
            if (startIndex == -1)
            {
                return "null";
            }

            startIndex = json.IndexOf(':', startIndex);
            if (startIndex == -1)
            {
                return "null";
            }

            // Skip whitespace
            startIndex++;
            while (startIndex < json.Length && char.IsWhiteSpace(json[startIndex]))
            {
                startIndex++;
            }

            int endIndex = startIndex;
            if (json[endIndex] == '"')
            {
                // String value - find closing quote
                endIndex++;
                while (endIndex < json.Length && json[endIndex] != '"')
                {
                    if (json[endIndex] == '\\')
                    {
                        endIndex++; // Skip escaped character
                    }
                    endIndex++;
                }
                endIndex++; // Include closing quote
            }
            else if (json[endIndex] == '{' || json[endIndex] == '[')
            {
                // Object or array - find matching closing brace/bracket
                int depth = 1;
                char opening = json[endIndex];
                char closing = opening == '{' ? '}' : ']';
                endIndex++;

                while (endIndex < json.Length && depth > 0)
                {
                    if (json[endIndex] == opening)
                    {
                        depth++;
                    }
                    else if (json[endIndex] == closing)
                    {
                        depth--;
                    }
                    endIndex++;
                }
            }
            else
            {
                // Number or boolean - find next comma or closing brace
                while (endIndex < json.Length && json[endIndex] != ',' && json[endIndex] != '}')
                {
                    endIndex++;
                }
            }

            return json.Substring(startIndex, endIndex - startIndex);
        }

        /// <summary>
        /// Wrapper class for extracting single string fields from JSON.
        /// </summary>
        [Serializable]
        private class StringWrapper
        {
            public string value;
        }
    }
}
