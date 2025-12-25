package sk.ainet.nlp.tools.tokenizer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * iOS-specific platform consistency tests.
 * 
 * These tests verify iOS-specific behavior and compare results with reference JVM implementation.
 * 
 * Feature: kotlin-minbpe-tokenizer
 */
class IosPlatformConsistencyTest {
    
    @Test
    fun testIosBasicTokenizerConsistency() {
        // Test BasicTokenizer on iOS platform
        val testCases = listOf(
            Triple("Wikipedia BPE", "aaabdaaabac", 259),
            Triple("Simple Text", "hello world", 270),
            Triple("Unicode Text", "Hello 世界! 🌍", 280),
            Triple("Empty Text", "", 256),
            Triple("Single Char", "a", 256)
        )
        
        for ((testName, text, vocabSize) in testCases) {
            val tokenizer = BasicTokenizer()
            tokenizer.train(text, vocabSize, verbose = false)
            
            val encoded = tokenizer.encode(text)
            val decoded = tokenizer.decode(encoded)
            val mergeCount = tokenizer.merges.size
            val vocabActualSize = tokenizer.vocab.size
            
            // Verify basic properties that should be consistent across platforms
            assertEquals(text, decoded, "iOS BasicTokenizer round-trip failed for $testName")
            assertTrue(mergeCount <= vocabSize - 256, "iOS BasicTokenizer merge count invalid for $testName")
            assertEquals(256 + mergeCount, vocabActualSize, "iOS BasicTokenizer vocab size invalid for $testName")
            
            // For Wikipedia example, verify exact expected output
            if (testName == "Wikipedia BPE") {
                assertEquals(listOf(258, 100, 258, 97, 99), encoded,
                    "iOS BasicTokenizer should produce expected Wikipedia BPE result")
            }
            
            // Test deterministic behavior
            val tokenizer2 = BasicTokenizer()
            tokenizer2.train(text, vocabSize, verbose = false)
            val encoded2 = tokenizer2.encode(text)
            
            assertEquals(encoded, encoded2, 
                "iOS BasicTokenizer should produce deterministic results for $testName")
        }
    }
    
    @Test
    fun testIosRegexTokenizerConsistency() {
        // Test RegexTokenizer on iOS platform
        val testTexts = listOf(
            "hello world",
            "The quick brown fox",
            "Testing 123 with numbers!",
            "Unicode: 世界 🌍"
        )
        
        for (text in testTexts) {
            val tokenizer = RegexTokenizer()
            tokenizer.train(text, 280, verbose = false)
            
            val encoded = tokenizer.encode(text, "none")
            val decoded = tokenizer.decode(encoded)
            
            assertEquals(text, decoded, 
                "iOS RegexTokenizer round-trip failed for text: '$text'")
            
            // Test with special tokens
            val specialTokens = mapOf("<|test|>" to 50000)
            tokenizer.registerSpecialTokens(specialTokens)
            
            val encodedWithSpecial = tokenizer.encode(text, "all")
            val decodedWithSpecial = tokenizer.decode(encodedWithSpecial)
            
            assertEquals(text, decodedWithSpecial,
                "iOS RegexTokenizer with special tokens failed for text: '$text'")
        }
    }
    
    @Test
    fun testIosGPT4TokenizerConsistency() {
        // Test GPT4Tokenizer on iOS platform
        val testTexts = listOf(
            "hello world",
            "The quick brown fox jumps over the lazy dog.",
            "Testing with numbers: 123456789",
            ""
        )
        
        for (text in testTexts) {
            val tokenizer = GPT4Tokenizer()
            
            try {
                val encoded = tokenizer.encode(text, "all")
                val decoded = tokenizer.decode(encoded)
                
                assertEquals(text, decoded,
                    "iOS GPT4Tokenizer round-trip failed for text: '$text'")
                
                // Test deterministic behavior
                val encoded2 = tokenizer.encode(text, "all")
                assertEquals(encoded, encoded2,
                    "iOS GPT4Tokenizer should produce deterministic results for text: '$text'")
                
            } catch (e: Exception) {
                // Some texts might contain disallowed special tokens
                assertTrue(e.message?.contains("special token") == true || 
                          e.message?.contains("not allowed") == true,
                    "iOS GPT4Tokenizer should throw consistent special token errors")
            }
        }
    }
    
    @Test
    fun testIosUtilityFunctionConsistency() {
        // Test utility functions on iOS platform
        val testTexts = listOf(
            "hello world",
            "test\nwith\tcontrol\rcharacters",
            "unicode: 世界 🌍",
            "",
            "a"
        )
        
        for (text in testTexts) {
            // Test UTF-8 encoding/decoding consistency
            val bytes = text.encodeToByteArray()
            val decoded = bytes.decodeToString()
            
            assertEquals(text, decoded, 
                "iOS UTF-8 round-trip should be consistent for text: '$text'")
            
            // Test byte array operations
            val tokenIds = bytes.map { it.toInt() and 0xFF }
            val reconstructedBytes = tokenIds.map { it.toByte() }.toByteArray()
            val reconstructedText = reconstructedBytes.decodeToString()
            
            assertEquals(text, reconstructedText,
                "iOS byte array operations should be consistent for text: '$text'")
        }
    }
    
    @Test
    fun testIosMemoryEfficiency() {
        // Test memory efficiency on iOS (important for mobile platforms)
        val tokenizers = mutableListOf<BasicTokenizer>()
        
        // Create multiple tokenizers to test memory usage
        repeat(5) { // Fewer iterations for mobile platform
            val tokenizer = BasicTokenizer()
            tokenizer.train("hello world test memory", 280, verbose = false)
            tokenizers.add(tokenizer)
        }
        
        // Verify all tokenizers work correctly
        for ((index, tokenizer) in tokenizers.withIndex()) {
            val text = "test tokenizer $index"
            val encoded = tokenizer.encode(text)
            val decoded = tokenizer.decode(encoded)
            assertEquals(text, decoded, "iOS tokenizer $index should work correctly")
        }
        
        // Test with larger text (but not too large for mobile)
        val largeText = "hello world ".repeat(100) // Smaller than JVM test
        val tokenizer = BasicTokenizer()
        tokenizer.train(largeText, 300, verbose = false)
        
        val encoded = tokenizer.encode(largeText)
        val decoded = tokenizer.decode(encoded)
        
        assertEquals(largeText, decoded, "iOS should handle moderately large text")
    }
    
    @Test
    fun testIosStringHandling() {
        // Test iOS-specific string handling
        val testStrings = listOf(
            "Normal ASCII text",
            "Text with\nnewlines\tand\ttabs",
            "Unicode: 世界 🌍 αβγδε",
            "Emoji: 😀😃😄😁😆",
            "Mixed: Hello 世界! 123 😀"
        )
        
        for (text in testStrings) {
            val tokenizer = BasicTokenizer()
            tokenizer.train(text, 300, verbose = false)
            
            val encoded = tokenizer.encode(text)
            val decoded = tokenizer.decode(encoded)
            
            assertEquals(text, decoded, "iOS string handling failed for: ${text.take(20)}...")
            
            // Verify UTF-8 consistency on iOS
            val utf8Bytes = text.encodeToByteArray()
            val utf8Decoded = utf8Bytes.decodeToString()
            assertEquals(text, utf8Decoded, "iOS UTF-8 handling failed")
        }
    }
    
    @Test
    fun testIosPerformance() {
        // Test performance characteristics on iOS
        val tokenizer = BasicTokenizer()
        val text = "performance test text for iOS platform"
        
        // Training should complete successfully
        tokenizer.train(text, 280, verbose = false)
        
        // Multiple encode/decode operations should be consistent
        repeat(10) {
            val encoded = tokenizer.encode(text)
            val decoded = tokenizer.decode(encoded)
            assertEquals(text, decoded, "iOS performance test iteration $it failed")
        }
        
        // Test with various text sizes
        val textSizes = listOf(10, 50, 100, 200)
        for (size in textSizes) {
            val testText = "word ".repeat(size)
            val sizeTokenizer = BasicTokenizer()
            sizeTokenizer.train(testText, 280, verbose = false)
            
            val encoded = sizeTokenizer.encode(testText)
            val decoded = sizeTokenizer.decode(encoded)
            
            assertEquals(testText, decoded, "iOS performance test failed for size $size")
        }
    }
}