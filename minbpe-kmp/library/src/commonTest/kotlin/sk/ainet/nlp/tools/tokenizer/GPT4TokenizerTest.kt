package sk.ainet.nlp.tools.tokenizer

import sk.ainet.nlp.tools.tokenizer.persistence.TokenizerData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Unit tests for GPT4Tokenizer implementation.
 * 
 * These tests verify the basic functionality of the GPT4Tokenizer,
 * including initialization, special token handling, and error conditions.
 */
class GPT4TokenizerTest {
    
    @Test
    fun testGPT4TokenizerInitialization() {
        // Test that GPT4Tokenizer can be created and initialized
        val tokenizer = GPT4Tokenizer()
        
        // Verify special tokens are registered
        assertEquals(GPT4Tokenizer.GPT4_SPECIAL_TOKENS, tokenizer.specialTokens)
        
        // Verify pattern is set correctly
        assertEquals(RegexTokenizer.GPT4_SPLIT_PATTERN, tokenizer.pattern)
        
        // Verify it has some merges (from our demo data)
        assertTrue(tokenizer.merges.isNotEmpty(), "GPT4Tokenizer should have merge rules")
        
        // Verify vocabulary is built
        assertTrue(tokenizer.vocab.isNotEmpty(), "GPT4Tokenizer should have vocabulary")
        assertTrue(tokenizer.vocab.size >= 256, "Vocabulary should include at least base bytes")
    }
    
    @Test
    fun testGPT4SpecialTokens() {
        val tokenizer = GPT4Tokenizer()
        
        // Test that all GPT-4 special tokens are present
        val expectedTokens = mapOf(
            "<|endoftext|>" to 100257,
            "<|fim_prefix|>" to 100258,
            "<|fim_middle|>" to 100259,
            "<|fim_suffix|>" to 100260,
            "<|endofprompt|>" to 100276
        )
        
        assertEquals(expectedTokens, tokenizer.specialTokens)
    }
    
    @Test
    fun testBasicEncodeDecodeRoundTrip() {
        val tokenizer = GPT4Tokenizer()
        
        // Test basic encode/decode round trip
        val testText = "Hello, world!"
        val encoded = tokenizer.encode(testText)
        val decoded = tokenizer.decode(encoded)
        
        assertEquals(testText, decoded, "Encode/decode should be round-trip consistent")
    }
    
    @Test
    fun testSpecialTokenEncoding() {
        val tokenizer = GPT4Tokenizer()
        
        // Test encoding with special tokens allowed
        val textWithSpecial = "Hello <|endoftext|> world"
        val encoded = tokenizer.encode(textWithSpecial, "all")
        
        // Should contain the special token ID
        assertTrue(encoded.contains(100257), "Encoded result should contain special token ID")
        
        // Test round trip
        val decoded = tokenizer.decode(encoded)
        assertEquals(textWithSpecial, decoded)
    }
    
    @Test
    fun testSpecialTokenErrorHandling() {
        val tokenizer = GPT4Tokenizer()
        
        // Test that disallowed special tokens raise an error
        val textWithSpecial = "Hello <|endoftext|> world"
        
        assertFailsWith<TokenizationException> {
            tokenizer.encode(textWithSpecial, "none_raise")
        }
    }
    
    @Test
    fun testTrainingNotAllowed() {
        val tokenizer = GPT4Tokenizer()
        
        // GPT4Tokenizer should not allow training
        assertFailsWith<UnsupportedOperationException> {
            tokenizer.train("some text", 1000)
        }
    }
    
    @Test
    fun testLoadFromDataWithGPT4Type() {
        val tokenizer = GPT4Tokenizer()
        
        // Create test data with gpt4 type
        val testData = TokenizerData(
            version = "minbpe v1",
            type = "gpt4",
            pattern = RegexTokenizer.GPT4_SPLIT_PATTERN,
            specialTokens = GPT4Tokenizer.GPT4_SPECIAL_TOKENS,
            merges = listOf(
                Pair(Pair(32, 116), 256),  // " t" -> 256
                Pair(Pair(32, 97), 257)    // " a" -> 257
            )
        )
        
        // Should load successfully
        tokenizer.loadFromData(testData)
        
        // Verify it's properly initialized
        assertTrue(tokenizer.merges.isNotEmpty())
        assertEquals(GPT4Tokenizer.GPT4_SPECIAL_TOKENS, tokenizer.specialTokens)
    }
    
    @Test
    fun testLoadFromDataWithWrongType() {
        val tokenizer = GPT4Tokenizer()
        
        // Create test data with wrong type
        val testData = TokenizerData(
            version = "minbpe v1",
            type = "basic",  // Wrong type
            pattern = RegexTokenizer.GPT4_SPLIT_PATTERN,
            specialTokens = GPT4Tokenizer.GPT4_SPECIAL_TOKENS,
            merges = listOf()
        )
        
        // Should fail with wrong type
        assertFailsWith<IllegalArgumentException> {
            tokenizer.loadFromData(testData)
        }
    }
    
    @Test
    fun testLoadFromDataWithWrongPattern() {
        val tokenizer = GPT4Tokenizer()
        
        // Create test data with wrong pattern
        val testData = TokenizerData(
            version = "minbpe v1",
            type = "gpt4",
            pattern = "wrong pattern",  // Wrong pattern
            specialTokens = GPT4Tokenizer.GPT4_SPECIAL_TOKENS,
            merges = listOf()
        )
        
        // Should fail with wrong pattern
        assertFailsWith<IllegalArgumentException> {
            tokenizer.loadFromData(testData)
        }
    }
}