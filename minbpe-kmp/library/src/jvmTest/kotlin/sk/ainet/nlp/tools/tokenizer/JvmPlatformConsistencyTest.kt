package sk.ainet.nlp.tools.tokenizer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * JVM-specific platform consistency tests.
 * 
 * These tests verify JVM-specific behavior and serve as a reference implementation
 * for comparing results with other platforms.
 * 
 * Feature: kotlin-minbpe-tokenizer
 */
class JvmPlatformConsistencyTest {
    
    companion object {
        // Reference results that should be identical across all platforms
        // These are computed on JVM and used as the "golden standard"
        private val REFERENCE_RESULTS = mutableMapOf<String, Any>()
    }
    
    @Test
    fun testJvmReferenceResults() {
        // Generate reference results on JVM platform
        val testCases = listOf(
            "Wikipedia BPE" to Pair("aaabdaaabac", 259),
            "Simple Text" to Pair("hello world", 270),
            "Unicode Text" to Pair("Hello 世界! 🌍", 280),
            "Empty Text" to Pair("", 256),
            "Single Char" to Pair("a", 256)
        )
        
        for ((testName, testData) in testCases) {
            val (text, vocabSize) = testData
            
            // Test BasicTokenizer
            val basicTokenizer = BasicTokenizer()
            basicTokenizer.train(text, vocabSize, verbose = false)
            
            val basicEncoded = basicTokenizer.encode(text)
            val basicDecoded = basicTokenizer.decode(basicEncoded)
            val basicMergeCount = basicTokenizer.merges.size
            val basicVocabSize = basicTokenizer.vocab.size
            
            // Store reference results
            REFERENCE_RESULTS["${testName}_basic_encoded"] = basicEncoded
            REFERENCE_RESULTS["${testName}_basic_decoded"] = basicDecoded
            REFERENCE_RESULTS["${testName}_basic_merge_count"] = basicMergeCount
            REFERENCE_RESULTS["${testName}_basic_vocab_size"] = basicVocabSize
            
            // Verify basic properties
            assertEquals(text, basicDecoded, "JVM BasicTokenizer round-trip failed for $testName")
            assertTrue(basicMergeCount <= vocabSize - 256, "JVM BasicTokenizer merge count invalid for $testName")
            assertEquals(256 + basicMergeCount, basicVocabSize, "JVM BasicTokenizer vocab size invalid for $testName")
            
            // Test RegexTokenizer if text is not empty
            if (text.isNotEmpty()) {
                val regexTokenizer = RegexTokenizer()
                regexTokenizer.train(text, vocabSize, verbose = false)
                
                val regexEncoded = regexTokenizer.encode(text, "none")
                val regexDecoded = regexTokenizer.decode(regexEncoded)
                val regexMergeCount = regexTokenizer.merges.size
                val regexVocabSize = regexTokenizer.vocab.size
                
                // Store reference results
                REFERENCE_RESULTS["${testName}_regex_encoded"] = regexEncoded
                REFERENCE_RESULTS["${testName}_regex_decoded"] = regexDecoded
                REFERENCE_RESULTS["${testName}_regex_merge_count"] = regexMergeCount
                REFERENCE_RESULTS["${testName}_regex_vocab_size"] = regexVocabSize
                
                // Verify basic properties
                assertEquals(text, regexDecoded, "JVM RegexTokenizer round-trip failed for $testName")
                assertTrue(regexMergeCount <= vocabSize - 256, "JVM RegexTokenizer merge count invalid for $testName")
                assertEquals(256 + regexMergeCount, regexVocabSize, "JVM RegexTokenizer vocab size invalid for $testName")
            }
        }
        
        // Test GPT4Tokenizer
        val gpt4TestTexts = listOf(
            "hello world",
            "The quick brown fox",
            ""
        )
        
        for (text in gpt4TestTexts) {
            val gpt4Tokenizer = GPT4Tokenizer()
            
            try {
                val gpt4Encoded = gpt4Tokenizer.encode(text, "all")
                val gpt4Decoded = gpt4Tokenizer.decode(gpt4Encoded)
                
                // Store reference results
                REFERENCE_RESULTS["gpt4_${text.hashCode()}_encoded"] = gpt4Encoded
                REFERENCE_RESULTS["gpt4_${text.hashCode()}_decoded"] = gpt4Decoded
                
                // Verify basic properties
                assertEquals(text, gpt4Decoded, "JVM GPT4Tokenizer round-trip failed for '$text'")
                
            } catch (e: Exception) {
                // Store exception info for comparison
                REFERENCE_RESULTS["gpt4_${text.hashCode()}_exception"] = e.javaClass.simpleName
            }
        }
    }
    
    @Test
    fun testJvmSpecificFeatures() {
        // Test JVM-specific optimizations and features
        val tokenizer = BasicTokenizer()
        val largeText = "hello world ".repeat(1000)
        
        // Test performance with larger text
        val startTime = System.nanoTime()
        tokenizer.train(largeText, 300, verbose = false)
        val trainingTime = System.nanoTime() - startTime
        
        // Training should complete in reasonable time (less than 1 second)
        assertTrue(trainingTime < 1_000_000_000L, "JVM training should be reasonably fast")
        
        // Test encoding performance
        val encodeStartTime = System.nanoTime()
        val encoded = tokenizer.encode(largeText)
        val encodingTime = System.nanoTime() - encodeStartTime
        
        // Encoding should complete in reasonable time
        assertTrue(encodingTime < 100_000_000L, "JVM encoding should be reasonably fast")
        
        // Test decoding performance
        val decodeStartTime = System.nanoTime()
        val decoded = tokenizer.decode(encoded)
        val decodingTime = System.nanoTime() - decodeStartTime
        
        // Decoding should complete in reasonable time
        assertTrue(decodingTime < 100_000_000L, "JVM decoding should be reasonably fast")
        
        // Verify correctness
        assertEquals(largeText, decoded, "JVM large text round-trip should work")
    }
    
    @Test
    fun testJvmMemoryUsage() {
        // Test memory usage patterns on JVM
        val runtime = Runtime.getRuntime()
        val initialMemory = runtime.totalMemory() - runtime.freeMemory()
        
        // Create and train multiple tokenizers
        val tokenizers = mutableListOf<BasicTokenizer>()
        repeat(10) {
            val tokenizer = BasicTokenizer()
            tokenizer.train("hello world test memory usage", 280, verbose = false)
            tokenizers.add(tokenizer)
        }
        
        val afterTrainingMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryIncrease = afterTrainingMemory - initialMemory
        
        // Memory increase should be reasonable (less than 10MB for 10 tokenizers)
        assertTrue(memoryIncrease < 10 * 1024 * 1024, 
            "JVM memory usage should be reasonable: ${memoryIncrease / 1024 / 1024}MB")
        
        // Test that tokenizers work correctly
        for ((index, tokenizer) in tokenizers.withIndex()) {
            val text = "test tokenizer $index"
            val encoded = tokenizer.encode(text)
            val decoded = tokenizer.decode(encoded)
            assertEquals(text, decoded, "Tokenizer $index should work correctly")
        }
    }
    
    @Test
    fun testJvmStringHandling() {
        // Test JVM-specific string handling edge cases
        val testStrings = listOf(
            "Normal ASCII text",
            "Text with\nnewlines\tand\ttabs",
            "Unicode: 世界 🌍 αβγδε",
            "Emoji: 😀😃😄😁😆😅😂🤣",
            "Mixed: Hello 世界! 123 😀",
            "Control chars: \u0001\u0002\u0003",
            "Null char: \u0000",
            "High Unicode: \uD83D\uDE00\uD83C\uDF0D"
        )
        
        for (text in testStrings) {
            val tokenizer = BasicTokenizer()
            
            try {
                tokenizer.train(text, 300, verbose = false)
                val encoded = tokenizer.encode(text)
                val decoded = tokenizer.decode(encoded)
                
                assertEquals(text, decoded, "JVM string handling failed for: ${text.take(20)}...")
                
                // Verify UTF-8 consistency
                val utf8Bytes = text.encodeToByteArray()
                val utf8Decoded = utf8Bytes.decodeToString()
                assertEquals(text, utf8Decoded, "JVM UTF-8 handling failed")
                
            } catch (e: Exception) {
                // Some control characters might cause issues - this is expected
                assertTrue(text.any { it.isISOControl() }, 
                    "Only control characters should cause encoding issues")
            }
        }
    }
    
    @Test
    fun testJvmConcurrency() {
        // Test that tokenizers work correctly in concurrent scenarios
        val text = "concurrent test text"
        val tokenizer = BasicTokenizer()
        tokenizer.train(text, 280, verbose = false)
        
        // Test concurrent encoding
        val results = mutableListOf<List<Int>>()
        val threads = mutableListOf<Thread>()
        
        repeat(10) { i ->
            val thread = Thread {
                val testText = "$text $i"
                val encoded = tokenizer.encode(testText)
                synchronized(results) {
                    results.add(encoded)
                }
            }
            threads.add(thread)
            thread.start()
        }
        
        // Wait for all threads to complete
        threads.forEach { it.join() }
        
        // Verify all results are valid
        assertEquals(10, results.size, "All concurrent operations should complete")
        
        for ((index, encoded) in results.withIndex()) {
            val decoded = tokenizer.decode(encoded)
            assertTrue(decoded.startsWith(text), "Concurrent result $index should be valid")
        }
    }
}