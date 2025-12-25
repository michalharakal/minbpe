package sk.ainet.nlp.tools.tokenizer

import sk.ainet.nlp.tools.tokenizer.utils.BpeUtils
import sk.ainet.nlp.tools.tokenizer.utils.TextUtils
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Test tokenizer that exposes protected validation methods for testing.
 */
class TestTokenizer : BasicTokenizer() {
    fun testValidateVocabSize(vocabSize: Int) = validateVocabSize(vocabSize)
    fun testValidateTokenIds(tokenIds: List<Int>) = validateTokenIds(tokenIds)
    fun testValidatePattern(pattern: String) = validatePattern(pattern)
    fun testValidateText(text: String?, operation: String) = validateText(text, operation)
    fun testValidateSpecialTokens(specialTokens: Map<String, Int>) = validateSpecialTokens(specialTokens)
    fun testValidateMerges(merges: Map<Pair<Int, Int>, Int>) = validateMerges(merges)
    fun testValidateAllowedSpecialParameter(allowedSpecial: String, availableSpecialTokens: Set<String>) = 
        validateAllowedSpecialParameter(allowedSpecial, availableSpecialTokens)
}

/**
 * Comprehensive tests for error handling across all tokenizer components.
 * 
 * This test class validates that all error conditions are properly handled
 * with appropriate exceptions and error messages.
 */
class ErrorHandlingTest {
    
    @Test
    fun testBpeUtilsErrorHandling() {
        // Test getStats with negative token IDs
        assertFailsWith<TokenizationException> {
            BpeUtils.getStats(listOf(1, -1, 2))
        }
        
        // Test getStats with too large token IDs
        assertFailsWith<TokenizationException> {
            BpeUtils.getStats(listOf(1, 2_000_001, 2))
        }
        
        // Test merge with negative token IDs in input
        assertFailsWith<TokenizationException> {
            BpeUtils.merge(listOf(1, -1, 2), Pair(1, 2), 256)
        }
        
        // Test merge with negative token IDs in pair
        assertFailsWith<TokenizationException> {
            BpeUtils.merge(listOf(1, 2, 3), Pair(-1, 2), 256)
        }
        
        // Test merge with negative new token ID
        assertFailsWith<TokenizationException> {
            BpeUtils.merge(listOf(1, 2, 3), Pair(1, 2), -1)
        }
        
        // Test merge with too large token IDs
        assertFailsWith<TokenizationException> {
            BpeUtils.merge(listOf(1, 2_000_001, 3), Pair(1, 2), 256)
        }
    }
    
    @Test
    fun testBpeUtilsValidInputs() {
        // Test that valid inputs work correctly
        val stats = BpeUtils.getStats(listOf(1, 2, 1, 2))
        assertEquals(2, stats[Pair(1, 2)]) // Should be 2 occurrences of (1,2)
        assertEquals(1, stats[Pair(2, 1)]) // Should be 1 occurrence of (2,1)
        assertEquals(2, stats.size) // Should have 2 different pairs
        
        val merged = BpeUtils.merge(listOf(1, 2, 1, 2), Pair(1, 2), 256)
        assertEquals(listOf(256, 256), merged)
        
        // Test empty inputs
        val emptyStats = BpeUtils.getStats(emptyList())
        assertTrue(emptyStats.isEmpty())
        
        val emptyMerge = BpeUtils.merge(emptyList(), Pair(1, 2), 256)
        assertTrue(emptyMerge.isEmpty())
        
        // Test single token
        val singleStats = BpeUtils.getStats(listOf(1))
        assertTrue(singleStats.isEmpty())
        
        val singleMerge = BpeUtils.merge(listOf(1), Pair(1, 2), 256)
        assertEquals(listOf(1), singleMerge)
    }
    
    @Test
    fun testTextUtilsErrorHandling() {
        // Test that normal UTF-8 strings work
        val text = "Hello, 世界! 🌍"
        val bytes = TextUtils.stringToBytes(text)
        val decoded = TextUtils.bytesToString(bytes)
        assertEquals(text, decoded)
        
        // Test UTF-8 validation
        TextUtils.validateUtf8("Valid UTF-8 text")
        
        // Test text sanitization - the replacement character is actually '�' not '?'
        val sanitized = TextUtils.sanitizeText("Hello\uFFFDWorld")
        assertTrue(sanitized.contains("Hello") && sanitized.contains("World"))
    }
    
    @Test
    fun testBasicTokenizerErrorHandling() {
        val tokenizer = BasicTokenizer()
        
        // Test encoding before training
        assertFailsWith<IllegalStateException> {
            tokenizer.encode("test")
        }
        
        // Test decoding before training
        assertFailsWith<IllegalStateException> {
            tokenizer.decode(listOf(97, 98, 99))
        }
        
        // Test invalid vocabulary size
        assertFailsWith<IllegalArgumentException> {
            tokenizer.train("test", 255)
        }
        
        // Test training with empty text (should work)
        tokenizer.train("", 256)
        assertEquals(emptyList(), tokenizer.encode(""))
        assertEquals("", tokenizer.decode(emptyList()))
    }
    
    @Test
    fun testRegexTokenizerErrorHandling() {
        val tokenizer = RegexTokenizer()
        
        // Test invalid special tokens
        assertFailsWith<TokenizationException> {
            tokenizer.registerSpecialTokens(mapOf("" to 256)) // Blank token
        }
        
        assertFailsWith<TokenizationException> {
            tokenizer.registerSpecialTokens(mapOf("token" to -1)) // Negative ID
        }
        
        assertFailsWith<TokenizationException> {
            tokenizer.registerSpecialTokens(mapOf("token" to 100)) // ID in base vocab range
        }
        
        assertFailsWith<TokenizationException> {
            tokenizer.registerSpecialTokens(mapOf(
                "token1" to 256,
                "token2" to 256  // Duplicate ID
            ))
        }
    }
    
    @Test
    fun testTokenizerValidationMethods() {
        val tokenizer = TestTokenizer()
        
        // Test vocabulary size validation
        assertFailsWith<IllegalArgumentException> {
            tokenizer.testValidateVocabSize(255)
        }
        
        assertFailsWith<IllegalArgumentException> {
            tokenizer.testValidateVocabSize(1_000_001)
        }
        
        // Test token ID validation
        assertFailsWith<TokenizationException> {
            tokenizer.testValidateTokenIds(listOf(1, -1, 2))
        }
        
        assertFailsWith<TokenizationException> {
            tokenizer.testValidateTokenIds(listOf(1, 2_000_001, 2))
        }
        
        // Test pattern validation
        assertFailsWith<TokenizationException> {
            tokenizer.testValidatePattern("[invalid regex")
        }
        
        // Test text validation
        assertFailsWith<TokenizationException> {
            tokenizer.testValidateText(null, "testing")
        }
        
        // Test special token validation
        assertFailsWith<TokenizationException> {
            tokenizer.testValidateSpecialTokens(mapOf("" to 256))
        }
        
        assertFailsWith<TokenizationException> {
            tokenizer.testValidateSpecialTokens(mapOf("token" to -1))
        }
        
        // Test merge validation
        assertFailsWith<TokenizationException> {
            tokenizer.testValidateMerges(mapOf(Pair(-1, 2) to 256))
        }
        
        assertFailsWith<TokenizationException> {
            tokenizer.testValidateMerges(mapOf(Pair(1, 2) to 255)) // ID < 256
        }
        
        // Test allowed_special parameter validation
        assertFailsWith<IllegalArgumentException> {
            tokenizer.testValidateAllowedSpecialParameter("", setOf("token1"))
        }
        
        assertFailsWith<IllegalArgumentException> {
            tokenizer.testValidateAllowedSpecialParameter("unknown_token", setOf("token1"))
        }
    }
    
    @Test
    fun testValidInputsPassValidation() {
        val tokenizer = TestTokenizer()
        
        // These should not throw exceptions
        tokenizer.testValidateVocabSize(256)
        tokenizer.testValidateVocabSize(1000)
        tokenizer.testValidateTokenIds(listOf(0, 255, 256, 1000))
        tokenizer.testValidatePattern("")
        tokenizer.testValidatePattern("\\w+")
        tokenizer.testValidateText("", "testing")
        tokenizer.testValidateText("hello world", "testing")
        tokenizer.testValidateSpecialTokens(mapOf("token1" to 256, "token2" to 257))
        tokenizer.testValidateMerges(mapOf(Pair(1, 2) to 256, Pair(3, 4) to 257))
        tokenizer.testValidateAllowedSpecialParameter("all", setOf("token1"))
        tokenizer.testValidateAllowedSpecialParameter("none", setOf("token1"))
        tokenizer.testValidateAllowedSpecialParameter("none_raise", setOf("token1"))
        tokenizer.testValidateAllowedSpecialParameter("token1", setOf("token1", "token2"))
    }
    
    @Test
    fun testErrorMessageQuality() {
        val tokenizer = TestTokenizer()
        
        // Test that error messages are informative
        val exception = assertFailsWith<IllegalArgumentException> {
            tokenizer.testValidateVocabSize(255)
        }
        assertTrue(exception.message!!.contains("256"))
        assertTrue(exception.message!!.contains("255"))
        
        val tokenException = assertFailsWith<TokenizationException> {
            tokenizer.testValidateTokenIds(listOf(1, -5, 2))
        }
        assertTrue(tokenException.message!!.contains("negative"))
        assertTrue(tokenException.message!!.contains("-5"))
    }
}