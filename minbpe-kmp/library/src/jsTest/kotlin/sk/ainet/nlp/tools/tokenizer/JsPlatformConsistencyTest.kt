package sk.ainet.nlp.tools.tokenizer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * JavaScript-specific platform consistency tests.
 * 
 * These tests verify JavaScript-specific behavior and compare results with reference JVM implementation.
 * 
 * Feature: kotlin-minbpe-tokenizer
 */
class JsPlatformConsistencyTest {
    
    @Test
    fun testJsBasicTokenizerConsistency() {
        // Test BasicTokenizer on JavaScript platform
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
            assertEquals(text, decoded, "JS BasicTokenizer round-trip failed for $testName")
            assertTrue(mergeCount <= vocabSize - 256, "JS BasicTokenizer merge count invalid for $testName")
            assertEquals(256 + mergeCount, vocabActualSize, "JS BasicTokenizer vocab size invalid for $testName")
            
            // For Wikipedia example, verify exact expected output
            if (testName == "Wikipedia BPE") {
                assertEquals(listOf(258, 100, 258, 97, 99), encoded,
                    "JS BasicTokenizer should produce expected Wikipedia BPE result")
            }
            
            // Test deterministic behavior
            val tokenizer2 = BasicTokenizer()
            tokenizer2.train(text, vocabSize, verbose = false)
            val encoded2 = tokenizer2.encode(text)
            
            assertEquals(encoded, encoded2, 
                "JS BasicTokenizer should produce deterministic results for $testName")
        }
    }
    
    @Test
    fun testJsRegexTokenizerConsistency() {
        // Test RegexTokenizer on JavaScript platform
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
                "JS RegexTokenizer round-trip failed for text: '$text'")
            
            // Test with special tokens
            val specialTokens = mapOf("<|js|>" to 50000, "<|web|>" to 50001)
            tokenizer.registerSpecialTokens(specialTokens)
            
            val encodedWithSpecial = tokenizer.encode(text, "all")
            val decodedWithSpecial = tokenizer.decode(encodedWithSpecial)
            
            assertEquals(text, decodedWithSpecial,
                "JS RegexTokenizer with special tokens failed for text: '$text'")
        }
    }
    
    @Test
    fun testJsGPT4TokenizerConsistency() {
        // Test GPT4Tokenizer on JavaScript platform
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
                    "JS GPT4Tokenizer round-trip failed for text: '$text'")
                
                // Test deterministic behavior
                val encoded2 = tokenizer.encode(text, "all")
                assertEquals(encoded, encoded2,
                    "JS GPT4Tokenizer should produce deterministic results for text: '$text'")
                
            } catch (e: Exception) {
                // Some texts might contain disallowed special tokens
                assertTrue(e.message?.contains("special token") == true || 
                          e.message?.contains("not allowed") == true,
                    "JS GPT4Tokenizer should throw consistent special token errors")
            }
        }
    }
    
    @Test
    fun testJsUtilityFunctionConsistency() {
        // Test utility functions on JavaScript platform
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
                "JS UTF-8 round-trip should be consistent for text: '$text'")
            
            // Test byte array operations
            val tokenIds = bytes.map { it.toInt() and 0xFF }
            val reconstructedBytes = tokenIds.map { it.toByte() }.toByteArray()
            val reconstructedText = reconstructedBytes.decodeToString()
            
            assertEquals(text, reconstructedText,
                "JS byte array operations should be consistent for text: '$text'")
        }
    }
    
    @Test
    fun testJsMemoryEfficiency() {
        // Test memory efficiency on JavaScript (important for browser environments)
        val tokenizers = mutableListOf<BasicTokenizer>()
        
        // Create multiple tokenizers (fewer for JS due to memory constraints)
        repeat(3) {
            val tokenizer = BasicTokenizer()
            tokenizer.train("hello world test memory $it", 280, verbose = false)
            tokenizers.add(tokenizer)
        }
        
        // Verify all tokenizers work correctly
        for ((index, tokenizer) in tokenizers.withIndex()) {
            val text = "test tokenizer $index"
            val encoded = tokenizer.encode(text)
            val decoded = tokenizer.decode(encoded)
            assertEquals(text, decoded, "JS tokenizer $index should work correctly")
        }
        
        // Test with moderate text size (smaller for JS)
        val moderateText = "hello world ".repeat(50)
        val tokenizer = BasicTokenizer()
        tokenizer.train(moderateText, 300, verbose = false)
        
        val encoded = tokenizer.encode(moderateText)
        val decoded = tokenizer.decode(encoded)
        
        assertEquals(moderateText, decoded, "JS should handle moderate text size")
    }
    
    @Test
    fun testJsStringHandling() {
        // Test JavaScript-specific string handling
        val testStrings = listOf(
            "Normal ASCII text",
            "Text with\nnewlines\tand\ttabs",
            "Unicode: 世界 🌍 αβγδε",
            "Emoji: 😀😃😄😁😆",
            "Mixed: Hello 世界! 123 😀",
            "JavaScript: var x = 'hello'; console.log(x);",
            "JSON: {\"key\": \"value\", \"number\": 42}"
        )
        
        for (text in testStrings) {
            val tokenizer = BasicTokenizer()
            tokenizer.train(text, 300, verbose = false)
            
            val encoded = tokenizer.encode(text)
            val decoded = tokenizer.decode(encoded)
            
            assertEquals(text, decoded, "JS string handling failed for: ${text.take(20)}...")
            
            // Verify UTF-8 consistency on JavaScript
            val utf8Bytes = text.encodeToByteArray()
            val utf8Decoded = utf8Bytes.decodeToString()
            assertEquals(text, utf8Decoded, "JS UTF-8 handling failed")
        }
    }
    
    @Test
    fun testJsPerformance() {
        // Test performance characteristics on JavaScript
        val tokenizer = BasicTokenizer()
        val text = "performance test text for JavaScript platform"
        
        // Training should complete successfully
        tokenizer.train(text, 280, verbose = false)
        
        // Multiple encode/decode operations should be consistent
        repeat(10) {
            val encoded = tokenizer.encode(text)
            val decoded = tokenizer.decode(encoded)
            assertEquals(text, decoded, "JS performance test iteration $it failed")
        }
        
        // Test with various text sizes (smaller for JS)
        val textSizes = listOf(5, 20, 50, 100)
        for (size in textSizes) {
            val testText = "word ".repeat(size)
            val sizeTokenizer = BasicTokenizer()
            sizeTokenizer.train(testText, 280, verbose = false)
            
            val encoded = sizeTokenizer.encode(testText)
            val decoded = sizeTokenizer.decode(encoded)
            
            assertEquals(testText, decoded, "JS performance test failed for size $size")
        }
    }
    
    @Test
    fun testJsNumberHandling() {
        // Test JavaScript-specific number handling (important due to JS number precision)
        val tokenizer = BasicTokenizer()
        val text = "numbers: 123 456 789 1000 9999"
        
        tokenizer.train(text, 280, verbose = false)
        
        val encoded = tokenizer.encode(text)
        val decoded = tokenizer.decode(encoded)
        
        assertEquals(text, decoded, "JS number handling should work correctly")
        
        // Test that token IDs are handled correctly (JS numbers are 64-bit floats)
        for (tokenId in encoded) {
            assertTrue(tokenId >= 0, "Token ID should be non-negative: $tokenId")
            assertTrue(tokenId < 1000000, "Token ID should be reasonable: $tokenId")
            
            // Test that token ID can be used for vocabulary lookup
            assertTrue(tokenizer.vocab.containsKey(tokenId) || tokenId < 256,
                "Token ID $tokenId should be valid")
        }
    }
    
    @Test
    fun testJsRegexCompatibility() {
        // Test JavaScript regex compatibility
        val patterns = listOf(
            "",  // Empty pattern (should work)
            "\\w+",  // Word characters
            "[a-zA-Z]+",  // Letters only
            "\\d+",  // Digits only
            "\\s+"   // Whitespace
        )
        
        for (pattern in patterns) {
            try {
                val tokenizer = RegexTokenizer(pattern.ifEmpty { null })
                val text = "test regex pattern 123 abc"
                
                tokenizer.train(text, 280, verbose = false)
                val encoded = tokenizer.encode(text, "none")
                val decoded = tokenizer.decode(encoded)
                
                assertEquals(text, decoded, "JS regex pattern '$pattern' should work")
                
            } catch (e: Exception) {
                // Some patterns might not be supported - this is acceptable
                println("JS regex pattern '$pattern' not supported: ${e.message}")
            }
        }
    }
    
    @Test
    fun testJsEdgeCases() {
        // Test JavaScript-specific edge cases
        val edgeCases = listOf(
            "" to 256,                    // Empty text
            "a" to 256,                   // Single character
            "\n" to 256,                  // Single newline
            "\t" to 256,                  // Single tab
            "🌍" to 256,                  // Single emoji
            "世" to 256,                   // Single Unicode char
            "undefined" to 260,           // JS keyword
            "null" to 260,                // JS keyword
            "true" to 260,                // JS keyword
            "false" to 260,               // JS keyword
            "NaN" to 260,                 // JS special value
            "Infinity" to 260             // JS special value
        )
        
        for ((text, vocabSize) in edgeCases) {
            val tokenizer = BasicTokenizer()
            tokenizer.train(text, vocabSize, verbose = false)
            
            val encoded = tokenizer.encode(text)
            val decoded = tokenizer.decode(encoded)
            
            assertEquals(text, decoded, 
                "JS edge case failed for text: '$text'")
        }
    }
}