package sk.ainet.nlp.tools.tokenizer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Unit tests for RegexTokenizer implementation.
 * 
 * Tests specific examples and edge cases including:
 * - Default GPT-4 pattern usage (Requirement 2.2)
 * - Various allowed_special configurations (Requirement 2.4)
 * - Error handling for disallowed special tokens (Requirement 2.5)
 */
class RegexTokenizerTest {
    
    @Test
    fun testDefaultGPT4PatternUsage() {
        // Test default GPT-4 pattern usage (Requirement 2.2)
        val tokenizer = RegexTokenizer()
        assertEquals(RegexTokenizer.GPT4_SPLIT_PATTERN, tokenizer.pattern)
        
        // Test that the pattern is used correctly during training
        val text = "Hello, world! This is a test."
        tokenizer.train(text, vocabSize = 300, verbose = false)
        
        // Should be able to encode and decode
        val encoded = tokenizer.encode(text)
        val decoded = tokenizer.decode(encoded)
        assertEquals(text, decoded)
    }
    
    @Test
    fun testCustomPattern() {
        // Test custom pattern override
        val customPattern = "\\w+"
        val tokenizer = RegexTokenizer(customPattern)
        assertEquals(customPattern, tokenizer.pattern)
    }
    
    @Test
    fun testAllowedSpecialAll() {
        // Test allowed_special = "all" (Requirement 2.4)
        val tokenizer = RegexTokenizer()
        val specialTokens = mapOf(
            "<|endoftext|>" to 100257,
            "<|startoftext|>" to 100258
        )
        tokenizer.registerSpecialTokens(specialTokens)
        
        val text = "hello world"
        tokenizer.train(text, vocabSize = 280, verbose = false)
        
        val textWithSpecial = "hello <|endoftext|> world <|startoftext|> test"
        val encoded = tokenizer.encode(textWithSpecial, "all")
        val decoded = tokenizer.decode(encoded)
        
        assertEquals(textWithSpecial, decoded)
        assertTrue(encoded.contains(100257)) // Should contain special token ID
        assertTrue(encoded.contains(100258)) // Should contain special token ID
    }
    
    @Test
    fun testAllowedSpecialNone() {
        // Test allowed_special = "none" (Requirement 2.4)
        val tokenizer = RegexTokenizer()
        val specialTokens = mapOf("<|test|>" to 100257)
        tokenizer.registerSpecialTokens(specialTokens)
        
        val text = "hello world"
        tokenizer.train(text, vocabSize = 280, verbose = false)
        
        val textWithSpecial = "hello <|test|> world"
        val encoded = tokenizer.encode(textWithSpecial, "none")
        val decoded = tokenizer.decode(encoded)
        
        assertEquals(textWithSpecial, decoded)
        assertFalse(encoded.contains(100257)) // Should NOT contain special token ID
    }
    
    @Test
    fun testAllowedSpecialNoneRaise() {
        // Test allowed_special = "none_raise" (Requirement 2.5)
        val tokenizer = RegexTokenizer()
        val specialTokens = mapOf("<|forbidden|>" to 100257)
        tokenizer.registerSpecialTokens(specialTokens)
        
        val text = "hello world"
        tokenizer.train(text, vocabSize = 280, verbose = false)
        
        val textWithSpecial = "hello <|forbidden|> world"
        
        // Should throw TokenizationException (Requirement 2.5)
        assertFailsWith<TokenizationException> {
            tokenizer.encode(textWithSpecial, "none_raise")
        }
        
        // Should work fine without special tokens
        val textWithoutSpecial = "hello world"
        val encoded = tokenizer.encode(textWithoutSpecial, "none_raise")
        val decoded = tokenizer.decode(encoded)
        assertEquals(textWithoutSpecial, decoded)
    }
    
    @Test
    fun testInvalidAllowedSpecialParameter() {
        // Test invalid allowed_special parameter (Requirement 2.4)
        val tokenizer = RegexTokenizer()
        tokenizer.train("hello world", vocabSize = 280, verbose = false)
        
        assertFailsWith<IllegalArgumentException> {
            tokenizer.encode("hello world", "invalid_parameter")
        }
    }
    
    @Test
    fun testMultipleSpecialTokensInText() {
        // Test multiple special tokens in same text (Requirement 2.4)
        val tokenizer = RegexTokenizer()
        val specialTokens = mapOf(
            "<|start|>" to 100257,
            "<|middle|>" to 100258,
            "<|end|>" to 100259
        )
        tokenizer.registerSpecialTokens(specialTokens)
        
        val text = "hello world"
        tokenizer.train(text, vocabSize = 280, verbose = false)
        
        val textWithMultipleSpecial = "<|start|> hello <|middle|> world <|end|>"
        val encoded = tokenizer.encode(textWithMultipleSpecial, "all")
        val decoded = tokenizer.decode(encoded)
        
        assertEquals(textWithMultipleSpecial, decoded)
        assertTrue(encoded.contains(100257))
        assertTrue(encoded.contains(100258))
        assertTrue(encoded.contains(100259))
    }
    
    @Test
    fun testSpecialTokenAtTextBoundaries() {
        // Test special tokens at start and end of text (Requirement 2.4)
        val tokenizer = RegexTokenizer()
        val specialTokens = mapOf("<|boundary|>" to 100257)
        tokenizer.registerSpecialTokens(specialTokens)
        
        val text = "hello world"
        tokenizer.train(text, vocabSize = 280, verbose = false)
        
        // Test at start
        val textStartSpecial = "<|boundary|> hello world"
        val encodedStart = tokenizer.encode(textStartSpecial, "all")
        val decodedStart = tokenizer.decode(encodedStart)
        assertEquals(textStartSpecial, decodedStart)
        
        // Test at end
        val textEndSpecial = "hello world <|boundary|>"
        val encodedEnd = tokenizer.encode(textEndSpecial, "all")
        val decodedEnd = tokenizer.decode(encodedEnd)
        assertEquals(textEndSpecial, decodedEnd)
        
        // Test both
        val textBothSpecial = "<|boundary|> hello world <|boundary|>"
        val encodedBoth = tokenizer.encode(textBothSpecial, "all")
        val decodedBoth = tokenizer.decode(encodedBoth)
        assertEquals(textBothSpecial, decodedBoth)
    }
    
    @Test
    fun testErrorHandlingForDisallowedSpecialTokens() {
        // Test comprehensive error handling for disallowed special tokens (Requirement 2.5)
        val tokenizer = RegexTokenizer()
        val specialTokens = mapOf(
            "<|allowed|>" to 100257,
            "<|forbidden1|>" to 100258,
            "<|forbidden2|>" to 100259
        )
        tokenizer.registerSpecialTokens(specialTokens)
        
        val text = "hello world"
        tokenizer.train(text, vocabSize = 280, verbose = false)
        
        // Test single forbidden token
        assertFailsWith<TokenizationException> {
            tokenizer.encode("hello <|forbidden1|> world", "none_raise")
        }
        
        // Test multiple forbidden tokens
        assertFailsWith<TokenizationException> {
            tokenizer.encode("hello <|forbidden1|> world <|forbidden2|>", "none_raise")
        }
        
        // Test mixed allowed and forbidden (should still fail)
        assertFailsWith<TokenizationException> {
            tokenizer.encode("hello <|allowed|> world <|forbidden1|>", "none_raise")
        }
    }
    
    @Test
    fun testSpecialTokenRegistration() {
        val tokenizer = RegexTokenizer()
        assertTrue(tokenizer.specialTokens.isEmpty())
        
        val specialTokens = mapOf(
            "<|endoftext|>" to 100257,
            "<|startoftext|>" to 100258
        )
        tokenizer.registerSpecialTokens(specialTokens)
        
        assertEquals(specialTokens, tokenizer.specialTokens)
    }
    
    @Test
    fun testEncodeOrdinary() {
        val tokenizer = RegexTokenizer()
        val text = "hello world"
        tokenizer.train(text, vocabSize = 280, verbose = false)
        
        // encodeOrdinary should ignore special tokens even if registered
        val specialTokens = mapOf("<|test|>" to 100257)
        tokenizer.registerSpecialTokens(specialTokens)
        
        val textWithSpecial = "hello <|test|> world"
        val encoded = tokenizer.encodeOrdinary(textWithSpecial)
        val decoded = tokenizer.decode(encoded)
        
        assertEquals(textWithSpecial, decoded)
        assertFalse(encoded.contains(100257)) // Should NOT contain special token ID
    }
    
    @Test
    fun testMustTrainBeforeEncoding() {
        val tokenizer = RegexTokenizer()
        
        assertFailsWith<IllegalStateException> {
            tokenizer.encode("hello")
        }
        
        assertFailsWith<IllegalStateException> {
            tokenizer.encodeOrdinary("hello")
        }
        
        assertFailsWith<IllegalStateException> {
            tokenizer.decode(listOf(104, 101, 108, 108, 111))
        }
    }
    
    @Test
    fun testInvalidVocabSize() {
        val tokenizer = RegexTokenizer()
        
        assertFailsWith<IllegalArgumentException> {
            tokenizer.train("hello", vocabSize = 255)
        }
    }
    
    @Test
    fun testUnknownTokenIdInDecode() {
        val tokenizer = RegexTokenizer()
        tokenizer.train("hello", vocabSize = 280, verbose = false)
        
        assertFailsWith<IllegalArgumentException> {
            tokenizer.decode(listOf(999999)) // Unknown token ID
        }
    }
    
    @Test
    fun testEmptyTextHandling() {
        val tokenizer = RegexTokenizer()
        tokenizer.train("hello world", vocabSize = 280, verbose = false)
        
        // Empty text should encode to empty list
        val encoded = tokenizer.encode("")
        assertTrue(encoded.isEmpty())
        
        val decoded = tokenizer.decode(emptyList())
        assertEquals("", decoded)
    }
    
    @Test
    fun testRegexBoundaryIsolation() {
        // Test that merges don't cross regex boundaries
        val tokenizer = RegexTokenizer("\\w+|\\s+|\\p{Punct}+") // Simple word/space/punct pattern
        val text = "hello, world!"
        
        tokenizer.train(text, vocabSize = 300, verbose = false)
        
        // The comma and exclamation should be in separate chunks from words
        val encoded = tokenizer.encode(text)
        val decoded = tokenizer.decode(encoded)
        assertEquals(text, decoded)
    }
}