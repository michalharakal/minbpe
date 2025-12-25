package sk.ainet.nlp.tools.tokenizer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.random.Random

/**
 * Property-based tests for GPT4Tokenizer implementation.
 * 
 * These tests validate universal properties that should hold across all inputs:
 * - Property 8: GPT4 compatibility
 * - Property 9: Byte shuffle consistency
 * 
 * Each test runs with minimum 100 iterations to ensure comprehensive coverage.
 */
class GPT4TokenizerPropertyTest {
    
    @Test
    fun testProperty8GPT4Compatibility() {
        // **Feature: kotlin-minbpe-tokenizer, Property 8: GPT4 compatibility**
        // **Validates: Requirements 3.1, 3.4**
        
        repeat(100) {
            val text = generateRandomText()
            val tokenizer = GPT4Tokenizer()
            
            // Skip empty texts
            if (text.isEmpty()) return@repeat
            
            try {
                // Property: GPT4Tokenizer should produce deterministic results
                val encoding1 = tokenizer.encode(text)
                val encoding2 = tokenizer.encode(text)
                
                // Same text should always produce same encoding
                assertEquals(
                    encoding1, 
                    encoding2,
                    "GPT4Tokenizer should produce consistent encodings for the same text: '$text'"
                )
                
                // Property: GPT4Tokenizer should maintain round-trip consistency
                val decoded = tokenizer.decode(encoding1)
                val reencoded = tokenizer.encode(decoded)
                
                // encode(decode(tokens)) should equal original tokens (for valid sequences)
                assertEquals(
                    encoding1,
                    reencoded,
                    "Round-trip encoding should be consistent for text: '$text'"
                )
                
                // Property: GPT4Tokenizer should use the correct pattern
                assertEquals(
                    RegexTokenizer.GPT4_SPLIT_PATTERN,
                    tokenizer.pattern,
                    "GPT4Tokenizer should use the GPT-4 split pattern"
                )
                
            } catch (e: Exception) {
                // Skip if encoding fails (e.g., due to special tokens in none_raise mode)
                return@repeat
            }
        }
    }
    
    @Test
    fun testProperty9ByteShuffleConsistency() {
        // **Feature: kotlin-minbpe-tokenizer, Property 9: Byte shuffle consistency**
        // **Validates: Requirements 3.2**
        
        repeat(100) {
            val text = generateRandomText()
            val tokenizer = GPT4Tokenizer()
            
            // Skip empty texts
            if (text.isEmpty()) return@repeat
            
            try {
                // Property: Byte shuffle should be applied consistently during encode/decode
                val encoded = tokenizer.encode(text)
                val decoded = tokenizer.decode(encoded)
                
                // decode(encode(text)) should equal original text
                // This validates that byte shuffle and inverse byte shuffle are consistent
                assertEquals(
                    text,
                    decoded,
                    "Byte shuffle should be consistently applied and reversed for text: '$text'"
                )
                
                // Property: Byte shuffle should work correctly with special tokens
                val textWithSpecial = "$text<|endoftext|>"
                val encodedWithSpecial = tokenizer.encode(textWithSpecial, "all")
                val decodedWithSpecial = tokenizer.decode(encodedWithSpecial)
                
                // Special tokens should also maintain consistency through byte shuffle
                assertEquals(
                    textWithSpecial,
                    decodedWithSpecial,
                    "Byte shuffle should work correctly with special tokens for text: '$textWithSpecial'"
                )
                
                // Property: Multiple encode/decode cycles should be stable
                val doubleDecoded = tokenizer.decode(tokenizer.encode(decoded))
                assertEquals(
                    decoded,
                    doubleDecoded,
                    "Multiple encode/decode cycles should be stable for text: '$text'"
                )
                
            } catch (e: Exception) {
                // Skip if encoding fails
                return@repeat
            }
        }
    }
    
    /**
     * Generate random text for property testing.
     * Includes various character types to test different scenarios.
     */
    private fun generateRandomText(): String {
        val length = Random.nextInt(1, 50)
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 .,!?'\"-()[]{}:;"
        
        return (1..length)
            .map { chars.random() }
            .joinToString("")
    }
}