package sk.ainet.nlp.tools.tokenizer.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.random.Random

/**
 * Property-based tests for Text utilities using manual property testing.
 * 
 * Feature: kotlin-minbpe-tokenizer
 */
class TextUtilsPropertyTest {
    
    @Test
    fun testProperty16_ControlCharacterEscapingProperty() {
        // **Feature: kotlin-minbpe-tokenizer, Property 16: Control character escaping**
        // **Validates: Requirements 6.4**
        
        // Test with multiple random strings
        repeat(100) {
            val text = generateRandomString()
            val result = TextUtils.replaceControlCharacters(text)
            
            // Control characters should be escaped in the result
            assertFalse(result.contains("\n"))
            assertFalse(result.contains("\r"))
            assertFalse(result.contains("\t"))
            assertFalse(result.contains("\b"))
            
            // If original contained control characters, result should contain escape sequences
            if (text.contains("\n")) {
                assertTrue(result.contains("\\n"))
            }
            if (text.contains("\r")) {
                assertTrue(result.contains("\\r"))
            }
            if (text.contains("\t")) {
                assertTrue(result.contains("\\t"))
            }
            if (text.contains("\b")) {
                assertTrue(result.contains("\\b"))
            }
            
            // Backslashes should be properly escaped
            if (text.contains("\\")) {
                assertTrue(result.contains("\\\\"))
            }
        }
    }
    
    @Test
    fun testTokenRenderingProperty() {
        // Additional property for token rendering
        repeat(100) {
            val text = generateRandomString()
            val token = text.encodeToByteArray()
            val rendered = TextUtils.renderToken(token)
            
            // For valid UTF-8 text without control characters or backslashes, rendering should preserve the text
            if (!text.any { it.isISOControl() || it == '\\' || it == '"' || it == '\'' }) {
                assertEquals(text, rendered)
            }
            
            // Rendered result should never contain raw control characters
            assertFalse(rendered.contains("\n"))
            assertFalse(rendered.contains("\r"))
            assertFalse(rendered.contains("\t"))
        }
    }
    
    @Test
    fun testUtf8EncodingRoundTripProperty() {
        // Test UTF-8 encoding/decoding round trip
        repeat(100) {
            val text = generateRandomString()
            val bytes = TextUtils.stringToBytes(text)
            val decoded = TextUtils.bytesToString(bytes)
            
            // Round trip should preserve the original text
            assertEquals(text, decoded)
        }
    }
    
    private fun generateRandomString(): String {
        val length = Random.nextInt(0, 51) // 0 to 50 characters
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 \n\r\t\b\\\"'"
        return (0 until length).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    }
}