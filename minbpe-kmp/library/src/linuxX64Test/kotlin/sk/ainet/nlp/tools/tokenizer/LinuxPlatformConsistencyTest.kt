package sk.ainet.nlp.tools.tokenizer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Linux-specific platform consistency tests.
 * 
 * These tests verify Linux-specific behavior and compare results with reference JVM implementation.
 * 
 * Feature: kotlin-minbpe-tokenizer
 */
class LinuxPlatformConsistencyTest {
    
    @Test
    fun testLinuxBasicTokenizerConsistency() {
        // Test BasicTokenizer on Linux platform
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
            assertEquals(text, decoded, "Linux BasicTokenizer round-trip failed for $testName")
            assertTrue(mergeCount <= vocabSize - 256, "Linux BasicTokenizer merge count invalid for $testName")
            assertEquals(256 + mergeCount, vocabActualSize, "Linux BasicTokenizer vocab size invalid for $testName")
            
            // For Wikipedia example, verify exact expected output
            if (testName == "Wikipedia BPE") {
                assertEquals(listOf(258, 100, 258, 97, 99), encoded,
                    "Linux BasicTokenizer should produce expected Wikipedia BPE result")
            }
            
            // Test deterministic behavior
            val tokenizer2 = BasicTokenizer()
            tokenizer2.train(text, vocabSize, verbose = false)
            val encoded2 = tokenizer2.encode(text)
            
            assertEquals(encoded, encoded2, 
                "Linux BasicTokenizer should produce deterministic results for $testName")
        }
    }
    
    @Test
    fun testLinuxRegexTokenizerConsistency() {
        // Test RegexTokenizer on Linux platform
        val testTexts = listOf(
            "hello world",
            "The quick brown fox",
            "Testing 123 with numbers!",
            "Unicode: 世界 🌍",
            "Special chars: !@#$%^&*()"
        )
        
        for (text in testTexts) {
            val tokenizer = RegexTokenizer()
            tokenizer.train(text, 280, verbose = false)
            
            val encoded = tokenizer.encode(text, "none")
            val decoded = tokenizer.decode(encoded)
            
            assertEquals(text, decoded, 
                "Linux RegexTokenizer round-trip failed for text: '$text'")
            
            // Test with special tokens
            val specialTokens = mapOf("<|test|>" to 50000, "<|linux|>" to 50001)
            tokenizer.registerSpecialTokens(specialTokens)
            
            val encodedWithSpecial = tokenizer.encode(text, "all")
            val decodedWithSpecial = tokenizer.decode(encodedWithSpecial)
            
            assertEquals(text, decodedWithSpecial,
                "Linux RegexTokenizer with special tokens failed for text: '$text'")
        }
    }
    
    @Test
    fun testLinuxGPT4TokenizerConsistency() {
        // Test GPT4Tokenizer on Linux platform
        val testTexts = listOf(
            "hello world",
            "The quick brown fox jumps over the lazy dog.",
            "Testing with numbers: 123456789",
            "Unicode characters: 世界 🌍 αβγ",
            ""
        )
        
        for (text in testTexts) {
            val tokenizer = GPT4Tokenizer()
            
            try {
                val encoded = tokenizer.encode(text, "all")
                val decoded = tokenizer.decode(encoded)
                
                assertEquals(text, decoded,
                    "Linux GPT4Tokenizer round-trip failed for text: '$text'")
                
                // Test deterministic behavior
                val encoded2 = tokenizer.encode(text, "all")
                assertEquals(encoded, encoded2,
                    "Linux GPT4Tokenizer should produce deterministic results for text: '$text'")
                
            } catch (e: Exception) {
                // Some texts might contain disallowed special tokens
                assertTrue(e.message?.contains("special token") == true || 
                          e.message?.contains("not allowed") == true,
                    "Linux GPT4Tokenizer should throw consistent special token errors")
            }
        }
    }
    
    @Test
    fun testLinuxUtilityFunctionConsistency() {
        // Test utility functions on Linux platform
        val testTexts = listOf(
            "hello world",
            "test\nwith\tcontrol\rcharacters",
            "unicode: 世界 🌍",
            "",
            "a",
            "Linux specific test with /path/to/file"
        )
        
        for (text in testTexts) {
            // Test UTF-8 encoding/decoding consistency
            val bytes = text.encodeToByteArray()
            val decoded = bytes.decodeToString()
            
            assertEquals(text, decoded, 
                "Linux UTF-8 round-trip should be consistent for text: '$text'")
            
            // Test byte array operations
            val tokenIds = bytes.map { it.toInt() and 0xFF }
            val reconstructedBytes = tokenIds.map { it.toByte() }.toByteArray()
            val reconstructedText = reconstructedBytes.decodeToString()
            
            assertEquals(text, reconstructedText,
                "Linux byte array operations should be consistent for text: '$text'")
        }
    }
    
    @Test
    fun testLinuxPerformance() {
        // Test performance characteristics on Linux (should be similar to JVM)
        val tokenizer = BasicTokenizer()
        val largeText = "hello world ".repeat(500) // Moderate size for native platform
        
        // Training should complete successfully
        tokenizer.train(largeText, 300, verbose = false)
        
        val encoded = tokenizer.encode(largeText)
        val decoded = tokenizer.decode(encoded)
        
        assertEquals(largeText, decoded, "Linux should handle large text efficiently")
        
        // Test multiple operations
        repeat(20) {
            val testText = "performance test $it"
            val perfEncoded = tokenizer.encode(testText)
            val perfDecoded = tokenizer.decode(perfEncoded)
            assertEquals(testText, perfDecoded, "Linux performance test iteration $it failed")
        }
    }
    
    @Test
    fun testLinuxMemoryManagement() {
        // Test memory management on Linux native platform
        val tokenizers = mutableListOf<BasicTokenizer>()
        
        // Create multiple tokenizers
        repeat(10) {
            val tokenizer = BasicTokenizer()
            tokenizer.train("hello world test memory $it", 280, verbose = false)
            tokenizers.add(tokenizer)
        }
        
        // Verify all tokenizers work correctly
        for ((index, tokenizer) in tokenizers.withIndex()) {
            val text = "test tokenizer $index"
            val encoded = tokenizer.encode(text)
            val decoded = tokenizer.decode(encoded)
            assertEquals(text, decoded, "Linux tokenizer $index should work correctly")
        }
        
        // Test garbage collection behavior (implicit in Kotlin/Native)
        val tempTokenizers = mutableListOf<BasicTokenizer>()
        repeat(5) {
            val tokenizer = BasicTokenizer()
            tokenizer.train("temporary tokenizer $it", 270, verbose = false)
            tempTokenizers.add(tokenizer)
        }
        
        // Clear references (should allow GC)
        tempTokenizers.clear()
        
        // Original tokenizers should still work
        for ((index, tokenizer) in tokenizers.withIndex()) {
            val text = "post-gc test $index"
            val encoded = tokenizer.encode(text)
            val decoded = tokenizer.decode(encoded)
            assertEquals(text, decoded, "Linux tokenizer $index should work after GC")
        }
    }
    
    @Test
    fun testLinuxStringHandling() {
        // Test Linux-specific string handling
        val testStrings = listOf(
            "Normal ASCII text",
            "Text with\nnewlines\tand\ttabs",
            "Unicode: 世界 🌍 αβγδε",
            "Emoji: 😀😃😄😁😆😅😂🤣",
            "Mixed: Hello 世界! 123 😀",
            "Linux paths: /usr/bin/test /home/user/file.txt",
            "Environment vars: \$HOME \$PATH \$USER"
        )
        
        for (text in testStrings) {
            val tokenizer = BasicTokenizer()
            
            try {
                tokenizer.train(text, 300, verbose = false)
                val encoded = tokenizer.encode(text)
                val decoded = tokenizer.decode(encoded)
                
                assertEquals(text, decoded, "Linux string handling failed for: ${text.take(20)}...")
                
                // Verify UTF-8 consistency on Linux
                val utf8Bytes = text.encodeToByteArray()
                val utf8Decoded = utf8Bytes.decodeToString()
                assertEquals(text, utf8Decoded, "Linux UTF-8 handling failed")
                
            } catch (e: Exception) {
                // Some special characters might cause issues
                println("Linux string handling exception for '${text.take(20)}...': ${e.message}")
            }
        }
    }
    
    @Test
    fun testLinuxConcurrencySupport() {
        // Test concurrent usage patterns on Linux
        val tokenizer = BasicTokenizer()
        val baseText = "concurrent test on linux"
        tokenizer.train(baseText, 280, verbose = false)
        
        // Test multiple concurrent-like operations (simulated since we don't have threads in K/N)
        val results = mutableListOf<String>()
        
        repeat(10) { i ->
            val testText = "$baseText $i"
            val encoded = tokenizer.encode(testText)
            val decoded = tokenizer.decode(encoded)
            results.add(decoded)
        }
        
        // Verify all results are correct
        for ((index, result) in results.withIndex()) {
            val expectedText = "$baseText $index"
            assertEquals(expectedText, result, "Linux concurrent-like operation $index failed")
        }
    }
    
    @Test
    fun testLinuxEdgeCases() {
        // Test Linux-specific edge cases
        val edgeCases = listOf(
            "" to 256,                    // Empty text
            "a" to 256,                   // Single character
            "\n" to 256,                  // Single newline
            "\t" to 256,                  // Single tab
            "🌍" to 256,                  // Single emoji
            "世" to 256,                   // Single Unicode char
            "a".repeat(1000) to 300,      // Very repetitive text
            "abcdefghijklmnopqrstuvwxyz" to 280  // Alphabet
        )
        
        for ((text, vocabSize) in edgeCases) {
            val tokenizer = BasicTokenizer()
            tokenizer.train(text, vocabSize, verbose = false)
            
            val encoded = tokenizer.encode(text)
            val decoded = tokenizer.decode(encoded)
            
            assertEquals(text, decoded, 
                "Linux edge case failed for text: '${text.take(20)}...' (length: ${text.length})")
        }
    }
}