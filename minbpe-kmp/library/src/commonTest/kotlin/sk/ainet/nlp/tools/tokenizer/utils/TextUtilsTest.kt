package sk.ainet.nlp.tools.tokenizer.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TextUtilsTest {
    
    @Test
    fun testReplaceControlCharacters() {
        val input = "Hello\nWorld\tTest\r\n"
        val result = TextUtils.replaceControlCharacters(input)
        
        assertEquals("Hello\\nWorld\\tTest\\r\\n", result)
    }
    
    @Test
    fun testRenderToken() {
        // Test normal UTF-8 text
        val token = "Hello".encodeToByteArray()
        val result = TextUtils.renderToken(token)
        
        assertEquals("Hello", result)
    }
    
    @Test
    fun testRenderTokenWithControlChars() {
        val token = "Hello\nWorld".encodeToByteArray()
        val result = TextUtils.renderToken(token)
        
        assertEquals("Hello\\nWorld", result)
    }
    
    @Test
    fun testStringToBytes() {
        val text = "Hello, 世界!"
        val bytes = TextUtils.stringToBytes(text)
        val decoded = TextUtils.bytesToString(bytes)
        
        assertEquals(text, decoded)
    }
    
    @Test
    fun testBytesToString() {
        val originalText = "Test with émojis 🚀"
        val bytes = originalText.encodeToByteArray()
        val result = TextUtils.bytesToString(bytes)
        
        assertEquals(originalText, result)
    }
}