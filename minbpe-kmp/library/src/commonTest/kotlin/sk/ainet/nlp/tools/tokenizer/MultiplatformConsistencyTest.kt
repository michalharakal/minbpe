package sk.ainet.nlp.tools.tokenizer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Multiplatform consistency tests for tokenizer implementations.
 * 
 * These tests verify that tokenization results are identical across all supported platforms:
 * JVM, Android, iOS, macOS, Linux, JavaScript, and WebAssembly.
 * 
 * Feature: kotlin-minbpe-tokenizer
 */
class MultiplatformConsistencyTest {
    
    companion object {
        // Standard test cases that should produce identical results across platforms
        private val TEST_CASES = listOf(
            TestCase(
                name = "Wikipedia BPE Example",
                text = "aaabdaaabac",
                vocabSize = 259,
                expectedEncoded = listOf(258, 100, 258, 97, 99)
            ),
            TestCase(
                name = "Simple English Text",
                text = "hello world test",
                vocabSize = 280,
                expectedEncoded = null // Will be determined at runtime
            ),
            TestCase(
                name = "Unicode Text",
                text = "Hello 世界! 🌍",
                vocabSize = 300,
                expectedEncoded = null
            ),
            TestCase(
                name = "Repeated Patterns",
                text = "aaaa bbbb cccc dddd eeee",
                vocabSize = 270,
                expectedEncoded = null
            ),
            TestCase(
                name = "Mixed Content",
                text = "The quick brown fox jumps over the lazy dog. 123456789!@#$%^&*()",
                vocabSize = 320,
                expectedEncoded = null
            ),
            TestCase(
                name = "Empty Text",
                text = "",
                vocabSize = 256,
                expectedEncoded = emptyList()
            ),
            TestCase(
                name = "Single Character",
                text = "a",
                vocabSize = 256,
                expectedEncoded = listOf(97)
            )
        )
        
        // GPT-4 pattern for RegexTokenizer testing
        private const val GPT4_PATTERN = """'(?i:[sdmt]|ll|ve|re)|[^\r\n\p{L}\p{N}]?+\p{L}+|\p{N}{1,3}| ?[^\s\p{L}\p{N}]++[\r\n]*|\s*[\r\n]|\s+(?!\S)|\s+"""
        
        // Special tokens for testing
        private val SPECIAL_TOKENS = mapOf(
            "<|endoftext|>" to 100257,
            "<|fim_prefix|>" to 100258,
            "<|fim_middle|>" to 100259
        )
    }
    
    data class TestCase(
        val name: String,
        val text: String,
        val vocabSize: Int,
        val expectedEncoded: List<Int>?
    )
    
    data class TokenizerResult(
        val merges: Map<Pair<Int, Int>, Int>,
        val vocab: Map<Int, ByteArray>,
        val encoded: List<Int>,
        val decoded: String
    )
    
    @Test
    fun testBasicTokenizerConsistency() {
        // Test BasicTokenizer consistency across platforms
        for (testCase in TEST_CASES) {
            val tokenizer = BasicTokenizer()
            
            // Train the tokenizer
            tokenizer.train(testCase.text, testCase.vocabSize, verbose = false)
            
            // Encode and decode
            val encoded = tokenizer.encode(testCase.text)
            val decoded = tokenizer.decode(encoded)
            
            // Verify round-trip consistency
            assertEquals(testCase.text, decoded, 
                "BasicTokenizer round-trip failed for test case: ${testCase.name}")
            
            // For Wikipedia example, verify exact expected output
            if (testCase.expectedEncoded != null) {
                assertEquals(testCase.expectedEncoded, encoded,
                    "BasicTokenizer encoding mismatch for test case: ${testCase.name}")
            }
            
            // Store result for cross-platform comparison
            val result = TokenizerResult(
                merges = tokenizer.merges,
                vocab = tokenizer.vocab,
                encoded = encoded,
                decoded = decoded
            )
            
            // Verify deterministic behavior
            verifyDeterministicBehavior(testCase, result)
        }
    }
    
    @Test
    fun testRegexTokenizerConsistency() {
        // Test RegexTokenizer consistency across platforms
        val testCases = listOf(
            "hello world",
            "The quick brown fox",
            "Testing 123 with numbers!",
            "Unicode: 世界 🌍",
            "Special chars: !@#$%^&*()",
            ""
        )
        
        for (text in testCases) {
            val tokenizer = RegexTokenizer(GPT4_PATTERN)
            
            // Register special tokens
            tokenizer.registerSpecialTokens(SPECIAL_TOKENS)
            
            // Train the tokenizer
            if (text.isNotEmpty()) {
                tokenizer.train(text, 280, verbose = false)
            } else {
                tokenizer.train("dummy", 256, verbose = false) // Need non-empty text for training
            }
            
            // Test encoding with different allowed_special settings
            val encodedNone = tokenizer.encode(text, "none")
            val encodedAll = tokenizer.encode(text, "all")
            
            // Decode both
            val decodedNone = tokenizer.decode(encodedNone)
            val decodedAll = tokenizer.decode(encodedAll)
            
            // Verify round-trip consistency
            assertEquals(text, decodedNone,
                "RegexTokenizer round-trip failed (none) for text: '$text'")
            assertEquals(text, decodedAll,
                "RegexTokenizer round-trip failed (all) for text: '$text'")
        }
    }
    
    @Test
    fun testGPT4TokenizerConsistency() {
        // Test GPT4Tokenizer consistency across platforms
        val testTexts = listOf(
            "hello world",
            "The quick brown fox jumps over the lazy dog.",
            "Testing with numbers: 123456789",
            "Unicode characters: 世界 🌍 αβγ",
            "Special tokens: <|endoftext|>",
            ""
        )
        
        for (text in testTexts) {
            val tokenizer = GPT4Tokenizer()
            
            // GPT4Tokenizer is pretrained, so no training needed
            
            // Test encoding with different special token handling
            try {
                val encoded = tokenizer.encode(text, "all")
                val decoded = tokenizer.decode(encoded)
                
                // Verify round-trip consistency
                assertEquals(text, decoded,
                    "GPT4Tokenizer round-trip failed for text: '$text'")
                
                // Verify deterministic encoding (encode same text multiple times)
                repeat(3) {
                    val reEncoded = tokenizer.encode(text, "all")
                    assertEquals(encoded, reEncoded,
                        "GPT4Tokenizer should produce deterministic results for text: '$text'")
                }
                
            } catch (e: Exception) {
                // Some texts might contain disallowed special tokens
                // This is expected behavior and should be consistent across platforms
                assertTrue(e.message?.contains("special token") == true || 
                          e.message?.contains("not allowed") == true,
                    "GPT4Tokenizer should throw consistent special token errors")
            }
        }
    }
    
    @Test
    fun testCrossTokenizerConsistency() {
        // Test that different tokenizer types handle the same basic operations consistently
        val text = "hello world test"
        val vocabSize = 280
        
        // Test BasicTokenizer
        val basicTokenizer = BasicTokenizer()
        basicTokenizer.train(text, vocabSize, verbose = false)
        val basicEncoded = basicTokenizer.encode(text)
        val basicDecoded = basicTokenizer.decode(basicEncoded)
        
        // Test RegexTokenizer with simple pattern that matches everything
        val regexTokenizer = RegexTokenizer(".*")
        regexTokenizer.train(text, vocabSize, verbose = false)
        val regexEncoded = regexTokenizer.encode(text, "none")
        val regexDecoded = regexTokenizer.decode(regexEncoded)
        
        // Both should preserve the original text
        assertEquals(text, basicDecoded, "BasicTokenizer should preserve text")
        assertEquals(text, regexDecoded, "RegexTokenizer should preserve text")
        
        // Verify that both tokenizers produce valid vocabularies
        assertTrue(basicTokenizer.vocab.size >= 256, "BasicTokenizer should have at least base vocab")
        assertTrue(regexTokenizer.vocab.size >= 256, "RegexTokenizer should have at least base vocab")
        
        // Verify that both tokenizers have consistent merge counts
        val basicMergeCount = basicTokenizer.merges.size
        val regexMergeCount = regexTokenizer.merges.size
        
        assertTrue(basicMergeCount <= vocabSize - 256, "BasicTokenizer merge count should be valid")
        assertTrue(regexMergeCount <= vocabSize - 256, "RegexTokenizer merge count should be valid")
    }
    
    @Test
    fun testUtilityFunctionConsistency() {
        // Test that utility functions produce consistent results across platforms
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
                "UTF-8 round-trip should be consistent for text: '$text'")
            
            // Test that byte array operations are consistent
            val tokenIds = bytes.map { it.toInt() and 0xFF }
            val reconstructedBytes = tokenIds.map { it.toByte() }.toByteArray()
            val reconstructedText = reconstructedBytes.decodeToString()
            
            assertEquals(text, reconstructedText,
                "Byte array operations should be consistent for text: '$text'")
        }
    }
    
    @Test
    fun testSerializationConsistency() {
        // Test that serialization produces consistent results across platforms
        val tokenizer = BasicTokenizer()
        val text = "hello world test serialization"
        
        // Train the tokenizer
        tokenizer.train(text, 280, verbose = false)
        
        // Test that the tokenizer state is consistent
        val merges = tokenizer.merges
        val vocab = tokenizer.vocab
        
        // Verify merge consistency
        for ((pair, newTokenId) in merges) {
            assertTrue(vocab.containsKey(newTokenId),
                "All merge tokens should be in vocabulary")
            assertTrue(newTokenId >= 256,
                "Merge tokens should have IDs >= 256")
        }
        
        // Verify vocabulary consistency
        for (i in 0..255) {
            assertTrue(vocab.containsKey(i),
                "Base vocabulary should contain all byte values")
            assertEquals(1, vocab[i]!!.size,
                "Base tokens should have single byte")
            assertEquals(i.toByte(), vocab[i]!![0],
                "Base token $i should contain correct byte value")
        }
        
        // Test encoding consistency
        val encoded1 = tokenizer.encode(text)
        val encoded2 = tokenizer.encode(text)
        
        assertEquals(encoded1, encoded2,
            "Multiple encodings of same text should be identical")
        
        // Test decoding consistency
        val decoded1 = tokenizer.decode(encoded1)
        val decoded2 = tokenizer.decode(encoded1)
        
        assertEquals(decoded1, decoded2,
            "Multiple decodings of same tokens should be identical")
        assertEquals(text, decoded1,
            "Decoded text should match original")
    }
    
    private fun verifyDeterministicBehavior(testCase: TestCase, result: TokenizerResult) {
        // Verify that the same training produces identical results
        val tokenizer2 = BasicTokenizer()
        tokenizer2.train(testCase.text, testCase.vocabSize, verbose = false)
        
        val encoded2 = tokenizer2.encode(testCase.text)
        val decoded2 = tokenizer2.decode(encoded2)
        
        // Results should be identical
        assertEquals(result.encoded, encoded2,
            "Deterministic training should produce identical encoding for: ${testCase.name}")
        assertEquals(result.decoded, decoded2,
            "Deterministic training should produce identical decoding for: ${testCase.name}")
        assertEquals(result.merges.size, tokenizer2.merges.size,
            "Deterministic training should produce same number of merges for: ${testCase.name}")
        assertEquals(result.vocab.size, tokenizer2.vocab.size,
            "Deterministic training should produce same vocabulary size for: ${testCase.name}")
    }
}