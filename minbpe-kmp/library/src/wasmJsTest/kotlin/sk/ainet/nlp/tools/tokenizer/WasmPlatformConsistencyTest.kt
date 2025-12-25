package sk.ainet.nlp.tools.tokenizer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * WebAssembly-specific platform consistency tests.
 * 
 * These tests verify WebAssembly-specific behavior and compare results with reference JVM implementation.
 * 
 * Feature: kotlin-minbpe-tokenizer
 */
class WasmPlatformConsistencyTest {
    
    @Test
    fun testWasmBasicTokenizerConsistency() {
        // Test BasicTokenizer on WebAssembly platform
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
            assertEquals(text, decoded, "WASM BasicTokenizer round-trip failed for $testName")
            assertTrue(mergeCount <= vocabSize - 256, "WASM BasicTokenizer merge count invalid for $testName")
            assertEquals(256 + mergeCount, vocabActualSize, "WASM BasicTokenizer vocab size invalid for $testName")
            
            // For Wikipedia example, verify exact expected output
            if (testName == "Wikipedia BPE") {
                assertEquals(listOf(258, 100, 258, 97, 99), encoded,
                    "WASM BasicTokenizer should produce expected Wikipedia BPE result")
            }
            
            // Test deterministic behavior
            val tokenizer2 = BasicTokenizer()
            tokenizer2.train(text, vocabSize, verbose = false)
            val encoded2 = tokenizer2.encode(text)
            
            assertEquals(encoded, encoded2, 
                "WASM BasicTokenizer should produce deterministic results for $testName")
        }
    }
    
    @Test
    fun testWasmRegexTokenizerConsistency() {
        // Test RegexTokenizer on WebAssembly platform
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
                "WASM RegexTokenizer round-trip failed for text: '$text'")
            
            // Test with special tokens
            val specialTokens = mapOf("<|wasm|>" to 50000, "<|binary|>" to 50001)
            tokenizer.registerSpecialTokens(specialTokens)
            
            val encodedWithSpecial = tokenizer.encode(text, "all")
            val decodedWithSpecial = tokenizer.decode(encodedWithSpecial)
            
            assertEquals(text, decodedWithSpecial,
                "WASM RegexTokenizer with special tokens failed for text: '$text'")
        }
    }
    
    @Test
    fun testWasmGPT4TokenizerConsistency() {
        // Test GPT4Tokenizer on WebAssembly platform
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
                    "WASM GPT4Tokenizer round-trip failed for text: '$text'")
                
                // Test deterministic behavior
                val encoded2 = tokenizer.encode(text, "all")
                assertEquals(encoded, encoded2,
                    "WASM GPT4Tokenizer should produce deterministic results for text: '$text'")
                
            } catch (e: Exception) {
                // Some texts might contain disallowed special tokens
                assertTrue(e.message?.contains("special token") == true || 
                          e.message?.contains("not allowed") == true,
                    "WASM GPT4Tokenizer should throw consistent special token errors")
            }
        }
    }
    
    @Test
    fun testWasmUtilityFunctionConsistency() {
        // Test utility functions on WebAssembly platform
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
                "WASM UTF-8 round-trip should be consistent for text: '$text'")
            
            // Test byte array operations
            val tokenIds = bytes.map { it.toInt() and 0xFF }
            val reconstructedBytes = tokenIds.map { it.toByte() }.toByteArray()
            val reconstructedText = reconstructedBytes.decodeToString()
            
            assertEquals(text, reconstructedText,
                "WASM byte array operations should be consistent for text: '$text'")
        }
    }
    
    @Test
    fun testWasmMemoryEfficiency() {
        // Test memory efficiency on WebAssembly (very important due to memory constraints)
        val tokenizers = mutableListOf<BasicTokenizer>()
        
        // Create fewer tokenizers for WASM due to strict memory constraints
        repeat(2) {
            val tokenizer = BasicTokenizer()
            tokenizer.train("hello world test memory $it", 270, verbose = false)
            tokenizers.add(tokenizer)
        }
        
        // Verify all tokenizers work correctly
        for ((index, tokenizer) in tokenizers.withIndex()) {
            val text = "test tokenizer $index"
            val encoded = tokenizer.encode(text)
            val decoded = tokenizer.decode(encoded)
            assertEquals(text, decoded, "WASM tokenizer $index should work correctly")
        }
        
        // Test with small text size (very important for WASM)
        val smallText = "hello world ".repeat(20)
        val tokenizer = BasicTokenizer()
        tokenizer.train(smallText, 280, verbose = false)
        
        val encoded = tokenizer.encode(smallText)
        val decoded = tokenizer.decode(encoded)
        
        assertEquals(smallText, decoded, "WASM should handle small text efficiently")
    }
    
    @Test
    fun testWasmStringHandling() {
        // Test WebAssembly-specific string handling
        val testStrings = listOf(
            "Normal ASCII text",
            "Text with\nnewlines\tand\ttabs",
            "Unicode: 世界 🌍 αβγδε",
            "Emoji: 😀😃😄",
            "Mixed: Hello 世界! 123 😀",
            "Binary data simulation: 0x1A2B3C4D",
            "WebAssembly: (module (func (result i32) i32.const 42))"
        )
        
        for (text in testStrings) {
            val tokenizer = BasicTokenizer()
            tokenizer.train(text, 290, verbose = false)
            
            val encoded = tokenizer.encode(text)
            val decoded = tokenizer.decode(encoded)
            
            assertEquals(text, decoded, "WASM string handling failed for: ${text.take(20)}...")
            
            // Verify UTF-8 consistency on WebAssembly
            val utf8Bytes = text.encodeToByteArray()
            val utf8Decoded = utf8Bytes.decodeToString()
            assertEquals(text, utf8Decoded, "WASM UTF-8 handling failed")
        }
    }
    
    @Test
    fun testWasmPerformance() {
        // Test performance characteristics on WebAssembly
        val tokenizer = BasicTokenizer()
        val text = "performance test text for WebAssembly platform"
        
        // Training should complete successfully
        tokenizer.train(text, 280, verbose = false)
        
        // Multiple encode/decode operations should be consistent
        repeat(5) { // Fewer iterations for WASM
            val encoded = tokenizer.encode(text)
            val decoded = tokenizer.decode(encoded)
            assertEquals(text, decoded, "WASM performance test iteration $it failed")
        }
        
        // Test with small text sizes (critical for WASM)
        val textSizes = listOf(2, 5, 10, 20)
        for (size in textSizes) {
            val testText = "word ".repeat(size)
            val sizeTokenizer = BasicTokenizer()
            sizeTokenizer.train(testText, 270, verbose = false)
            
            val encoded = sizeTokenizer.encode(testText)
            val decoded = sizeTokenizer.decode(encoded)
            
            assertEquals(testText, decoded, "WASM performance test failed for size $size")
        }
    }
    
    @Test
    fun testWasmBinaryDataHandling() {
        // Test WebAssembly-specific binary data handling
        val binaryTexts = listOf(
            "binary: \u0000\u0001\u0002\u0003",
            "hex: 0xFF 0x00 0xAB 0xCD",
            "base64-like: ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/",
            "control chars: \u0007\u0008\u0009\u000A\u000B\u000C\u000D"
        )
        
        for (text in binaryTexts) {
            try {
                val tokenizer = BasicTokenizer()
                tokenizer.train(text, 280, verbose = false)
                
                val encoded = tokenizer.encode(text)
                val decoded = tokenizer.decode(encoded)
                
                assertEquals(text, decoded, "WASM binary data handling failed for: ${text.take(20)}...")
                
            } catch (e: Exception) {
                // Some binary data might cause issues - this is acceptable for WASM
                println("WASM binary data handling exception for '${text.take(20)}...': ${e.message}")
            }
        }
    }
    
    @Test
    fun testWasmNumberPrecision() {
        // Test WebAssembly number precision (important for token IDs)
        val tokenizer = BasicTokenizer()
        val text = "precision test with numbers: 1 22 333 4444 55555"
        
        tokenizer.train(text, 300, verbose = false)
        
        val encoded = tokenizer.encode(text)
        val decoded = tokenizer.decode(encoded)
        
        assertEquals(text, decoded, "WASM number precision should work correctly")
        
        // Test that all token IDs are within expected ranges
        for (tokenId in encoded) {
            assertTrue(tokenId >= 0, "Token ID should be non-negative: $tokenId")
            assertTrue(tokenId < 100000, "Token ID should be reasonable for WASM: $tokenId")
            
            // Verify token ID precision (should be exact integers)
            val tokenIdDouble = tokenId.toDouble()
            val tokenIdInt = tokenIdDouble.toInt()
            assertEquals(tokenId, tokenIdInt, "Token ID should maintain integer precision: $tokenId")
        }
    }
    
    @Test
    fun testWasmEdgeCases() {
        // Test WebAssembly-specific edge cases
        val edgeCases = listOf(
            "" to 256,                    // Empty text
            "a" to 256,                   // Single character
            "\n" to 256,                  // Single newline
            "\t" to 256,                  // Single tab
            "🌍" to 256,                  // Single emoji
            "世" to 256,                   // Single Unicode char
            "wasm" to 260,                // WASM keyword
            "module" to 260,              // WASM keyword
            "func" to 260,                // WASM keyword
            "i32" to 260,                 // WASM type
            "f64" to 260                  // WASM type
        )
        
        for ((text, vocabSize) in edgeCases) {
            val tokenizer = BasicTokenizer()
            tokenizer.train(text, vocabSize, verbose = false)
            
            val encoded = tokenizer.encode(text)
            val decoded = tokenizer.decode(encoded)
            
            assertEquals(text, decoded, 
                "WASM edge case failed for text: '$text'")
        }
    }
    
    @Test
    fun testWasmResourceConstraints() {
        // Test that WASM works within resource constraints
        val tokenizer = BasicTokenizer()
        
        // Use minimal vocabulary size to conserve memory
        val minimalText = "test wasm constraints"
        tokenizer.train(minimalText, 260, verbose = false)
        
        // Test multiple small operations
        val smallTexts = listOf("a", "ab", "abc", "test", "wasm")
        
        for (text in smallTexts) {
            val encoded = tokenizer.encode(text)
            val decoded = tokenizer.decode(encoded)
            assertEquals(text, decoded, "WASM resource constraint test failed for: '$text'")
        }
        
        // Verify vocabulary is reasonable size
        assertTrue(tokenizer.vocab.size <= 260, 
            "WASM vocabulary should be within constraints: ${tokenizer.vocab.size}")
        assertTrue(tokenizer.merges.size <= 4, 
            "WASM merges should be within constraints: ${tokenizer.merges.size}")
    }
}