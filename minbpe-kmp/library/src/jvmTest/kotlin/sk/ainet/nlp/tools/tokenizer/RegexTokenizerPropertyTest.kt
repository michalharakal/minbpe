package sk.ainet.nlp.tools.tokenizer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith
import kotlin.random.Random

/**
 * Property-based tests for RegexTokenizer implementation.
 * 
 * These tests validate universal properties that should hold across all inputs:
 * - Property 4: Regex split boundary isolation
 * - Property 5: Special token round trip
 * - Property 6: Special token parameter handling  
 * - Property 7: Special token error handling
 * 
 * Each test runs with minimum 100 iterations to ensure comprehensive coverage.
 */
class RegexTokenizerPropertyTest {
    
    @Test
    fun testProperty4RegexSplitBoundaryIsolation() {
        // **Feature: kotlin-minbpe-tokenizer, Property 4: Regex split boundary isolation**
        // **Validates: Requirements 2.1, 2.6**
        
        repeat(100) {
            val text = generateRandomText()
            val tokenizer = RegexTokenizer()
            
            // Skip empty or very short texts
            if (text.length < 2) return@repeat
            
            // Train tokenizer
            tokenizer.train(text, vocabSize = 300, verbose = false)
            
            // Get the regex chunks
            val pattern = Regex(tokenizer.pattern)
            val chunks = pattern.findAll(text).map { it.value }.toList()
            
            if (chunks.size <= 1) return@repeat // Need multiple chunks to test boundaries
            
            // Encode the full text
            val fullEncoding = tokenizer.encodeOrdinary(text)
            
            // Encode each chunk separately and concatenate
            val separateEncodings = chunks.flatMap { chunk ->
                tokenizer.encodeOrdinary(chunk)
            }
            
            // The encodings should be identical - no merges across boundaries
            assertEquals(separateEncodings, fullEncoding,
                "Merges should not occur across regex split boundaries. " +
                "Text: '$text', Chunks: ${chunks.size}")
        }
    }
    
    @Test
    fun testProperty5SpecialTokenRoundTrip() {
        // **Feature: kotlin-minbpe-tokenizer, Property 5: Special token round trip**
        // **Validates: Requirements 2.3**
        
        repeat(100) {
            val specialTokenText = generateRandomAlphanumeric()
            
            // Create a special token from the input (ensure it's unique)
            val specialToken = "<|${specialTokenText.take(10).filter { it.isLetterOrDigit() }}|>"
            if (specialToken.length < 5) return@repeat // Skip if too short
            
            val tokenizer = RegexTokenizer()
            val specialTokens = mapOf(specialToken to 100257)
            tokenizer.registerSpecialTokens(specialTokens)
            
            // Train on some basic text
            tokenizer.train("hello world test", vocabSize = 280, verbose = false)
            
            // Test text containing the special token
            val testText = "hello $specialToken world"
            
            // Encode with special tokens allowed
            val encoded = tokenizer.encode(testText, "all")
            val decoded = tokenizer.decode(encoded)
            
            // Should preserve the special token exactly
            assertEquals(testText, decoded,
                "Special token round trip should preserve text exactly. " +
                "Special token: '$specialToken', Original: '$testText', Decoded: '$decoded'")
        }
    }
    
    @Test
    fun testProperty6SpecialTokenParameterHandling() {
        // **Feature: kotlin-minbpe-tokenizer, Property 6: Special token parameter handling**
        // **Validates: Requirements 2.4**
        
        repeat(100) {
            val tokenizer = RegexTokenizer()
            val specialToken = "<|test|>"
            val specialTokens = mapOf(specialToken to 100257)
            tokenizer.registerSpecialTokens(specialTokens)
            
            // Train tokenizer
            tokenizer.train("hello world", vocabSize = 280, verbose = false)
            
            val testText = "hello world"
            
            // Test "all" policy - should work without errors
            val encodedAll = tokenizer.encode(testText, "all")
            val decodedAll = tokenizer.decode(encodedAll)
            assertEquals(testText, decodedAll,
                "Policy 'all' should preserve text without special tokens")
            
            // Test "none" policy - should encode special tokens as ordinary text
            val encodedNone = tokenizer.encode(testText, "none")
            val decodedNone = tokenizer.decode(encodedNone)
            assertEquals(testText, decodedNone,
                "Policy 'none' should preserve text without special tokens")
        }
    }
    
    @Test
    fun testProperty7SpecialTokenErrorHandling() {
        // **Feature: kotlin-minbpe-tokenizer, Property 7: Special token error handling**
        // **Validates: Requirements 2.5**
        
        repeat(100) {
            val baseText = generateRandomText()
            val tokenizer = RegexTokenizer()
            val specialToken = "<|forbidden|>"
            val specialTokens = mapOf(specialToken to 100257)
            tokenizer.registerSpecialTokens(specialTokens)
            
            // Train tokenizer
            tokenizer.train("hello world", vocabSize = 280, verbose = false)
            
            // Create text that definitely contains the special token
            val textWithSpecial = "${baseText.take(10)} $specialToken end"
            
            // Test "none_raise" policy - should throw TokenizationException
            assertFailsWith<TokenizationException>(
                "Text containing disallowed special tokens should raise TokenizationException"
            ) {
                tokenizer.encode(textWithSpecial, "none_raise")
            }
        }
    }
    
    @Test
    fun testEncodeDecodeRoundTripConsistency() {
        // Additional property: round trip consistency for RegexTokenizer
        repeat(100) {
            val text = generateRandomText()
            val tokenizer = RegexTokenizer()
            
            if (text.isEmpty()) return@repeat
            
            // Train tokenizer
            tokenizer.train(text, vocabSize = 300, verbose = false)
            
            // Test round trip
            val encoded = tokenizer.encode(text)
            val decoded = tokenizer.decode(encoded)
            
            assertEquals(text, decoded,
                "Encode/decode round trip should preserve original text. " +
                "Original: '$text', Decoded: '$decoded'")
        }
    }
    
    @Test
    fun testPatternConsistency() {
        // Test that custom patterns are respected
        repeat(10) {
            val customPattern = "\\w+"
            val tokenizer = RegexTokenizer(customPattern)
            
            assertEquals(customPattern, tokenizer.pattern,
                "Custom pattern should be used")
            
            // Default pattern test
            val defaultTokenizer = RegexTokenizer()
            assertEquals(RegexTokenizer.GPT4_SPLIT_PATTERN, defaultTokenizer.pattern,
                "Default pattern should be GPT4_SPLIT_PATTERN")
        }
    }
    
    private fun generateRandomText(): String {
        val words = listOf(
            "hello", "world", "test", "tokenizer", "bpe", "algorithm", 
            "kotlin", "multiplatform", "encoding", "decoding", "merge",
            "vocabulary", "training", "text", "processing", "nlp",
            "aaabdaaabac", "repeated", "patterns", "common", "sequences"
        )
        val length = Random.nextInt(5, 15)
        return (0 until length).map { words.random() }.joinToString(" ")
    }
    
    private fun generateRandomAlphanumeric(): String {
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val length = Random.nextInt(5, 15)
        return (0 until length).map { chars.random() }.joinToString("")
    }
}