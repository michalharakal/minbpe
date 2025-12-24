package sk.ainet.nlp.tools.tokenizer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.random.Random

/**
 * Property-based tests for BasicTokenizer implementation.
 * 
 * Feature: kotlin-minbpe-tokenizer
 */
class BasicTokenizerPropertyTest {
    
    @Test
    fun testProperty1BasicTokenizerMergeCountProperty() {
        // **Feature: kotlin-minbpe-tokenizer, Property 1: Basic tokenizer merge count**
        // **Validates: Requirements 1.2**
        
        // Test with multiple random configurations
        repeat(100) {
            val tokenizer = BasicTokenizer()
            val testText = generateRandomText()
            val vocabSize = Random.nextInt(256, 350)
            
            // Train the tokenizer
            tokenizer.train(testText, vocabSize, verbose = false)
            
            // The number of merges should be at most (vocabSize - 256)
            // It may be less if there aren't enough pairs to merge
            val maxExpectedMerges = vocabSize - 256
            assertTrue(tokenizer.merges.size <= maxExpectedMerges,
                "BasicTokenizer should create at most (vocab_size - 256) merges. " +
                "Max expected: $maxExpectedMerges, Actual: ${tokenizer.merges.size}")
            
            // Vocabulary size should be 256 + number of actual merges
            val expectedVocabSize = 256 + tokenizer.merges.size
            assertEquals(expectedVocabSize, tokenizer.vocab.size,
                "Vocabulary size should be 256 + number of merges. " +
                "Expected: $expectedVocabSize, Actual: ${tokenizer.vocab.size}")
        }
    }
    
    @Test
    fun testProperty2EncodeDecodeRoundTripProperty() {
        // **Feature: kotlin-minbpe-tokenizer, Property 2: Encode/decode round trip**
        // **Validates: Requirements 1.4**
        
        // Test with multiple random configurations
        repeat(100) {
            val tokenizer = BasicTokenizer()
            val testText = generateRandomText()
            val vocabSize = Random.nextInt(256, 350)
            
            // Train the tokenizer
            tokenizer.train(testText, vocabSize, verbose = false)
            
            // Generate multiple test strings
            repeat(10) {
                val inputText = generateRandomText()
                
                // Encode then decode should preserve the original text
                val encoded = tokenizer.encode(inputText)
                val decoded = tokenizer.decode(encoded)
                
                assertEquals(inputText, decoded,
                    "Encode/decode round trip should preserve original text. " +
                    "Original: '$inputText', Decoded: '$decoded'")
            }
        }
    }
    
    @Test
    fun testProperty3VocabularyConsistencyProperty() {
        // **Feature: kotlin-minbpe-tokenizer, Property 3: Vocabulary consistency**
        // **Validates: Requirements 1.5**
        
        // Test with multiple random configurations
        repeat(100) {
            val tokenizer = BasicTokenizer()
            val testText = generateRandomText()
            val vocabSize = Random.nextInt(256, 350)
            
            // Train the tokenizer
            tokenizer.train(testText, vocabSize, verbose = false)
            
            // All token IDs in vocabulary should be valid
            for ((tokenId, bytes) in tokenizer.vocab) {
                // Token ID should be non-negative
                assertTrue(tokenId >= 0,
                    "Token ID should be non-negative, got: $tokenId")
                
                // Bytes should not be empty
                assertTrue(bytes.isNotEmpty(),
                    "Byte sequence for token $tokenId should not be empty")
                
                // For base tokens (0-255), should be single byte
                if (tokenId <= 255) {
                    assertEquals(1, bytes.size,
                        "Base token $tokenId should have exactly one byte")
                    assertEquals(tokenId.toByte(), bytes[0],
                        "Base token $tokenId should contain the corresponding byte")
                }
            }
            
            // All merge token IDs should be in vocabulary
            for ((_, newTokenId) in tokenizer.merges) {
                assertTrue(tokenizer.vocab.containsKey(newTokenId),
                    "Merge token $newTokenId should be in vocabulary")
                
                // Merge tokens should have at least 2 bytes (result of merging)
                val bytes = tokenizer.vocab[newTokenId]!!
                assertTrue(bytes.size >= 2,
                    "Merge token $newTokenId should have at least 2 bytes, got: ${bytes.size}")
            }
            
            // Vocabulary should contain all base tokens (0-255)
            for (i in 0..255) {
                assertTrue(tokenizer.vocab.containsKey(i),
                    "Vocabulary should contain base token $i")
            }
        }
    }
    
    @Test
    fun testEmptyTextHandling() {
        // Test that empty text is handled correctly
        repeat(10) {
            val tokenizer = BasicTokenizer()
            val vocabSize = Random.nextInt(256, 300)
            
            // Training on empty text should work
            tokenizer.train("", vocabSize, verbose = false)
            
            // Should have no merges for empty text
            assertEquals(0, tokenizer.merges.size,
                "Empty text should result in no merges")
            
            // Should have base vocabulary only
            assertEquals(256, tokenizer.vocab.size,
                "Empty text should result in base vocabulary only")
            
            // Encoding empty text should return empty list
            assertEquals(emptyList(), tokenizer.encode(""),
                "Encoding empty text should return empty list")
            
            // Decoding empty list should return empty string
            assertEquals("", tokenizer.decode(emptyList()),
                "Decoding empty list should return empty string")
        }
    }
    
    @Test
    fun testSingleCharacterHandling() {
        // Test that single character text is handled correctly
        repeat(10) {
            val tokenizer = BasicTokenizer()
            val char = ('a'..'z').random()
            val text = char.toString()
            val vocabSize = Random.nextInt(256, 300)
            
            // Training on single character should work
            tokenizer.train(text, vocabSize, verbose = false)
            
            // Should have no merges for single character
            assertEquals(0, tokenizer.merges.size,
                "Single character should result in no merges")
            
            // Encode/decode should work correctly
            val encoded = tokenizer.encode(text)
            val decoded = tokenizer.decode(encoded)
            
            assertEquals(text, decoded,
                "Single character encode/decode should preserve text")
            assertEquals(listOf(char.code), encoded,
                "Single character should encode to its byte value")
        }
    }
    
    @Test
    fun testUnicodeHandling() {
        // Test that Unicode characters are handled correctly
        repeat(20) {
            val tokenizer = BasicTokenizer()
            val unicodeChars = listOf("世", "界", "🌍", "🚀", "α", "β", "γ")
            val text = unicodeChars.shuffled().take(Random.nextInt(1, 5)).joinToString("")
            val vocabSize = Random.nextInt(256, 350)
            
            // Training should work with Unicode
            tokenizer.train(text, vocabSize, verbose = false)
            
            // Encode/decode should preserve Unicode text
            val encoded = tokenizer.encode(text)
            val decoded = tokenizer.decode(encoded)
            
            assertEquals(text, decoded,
                "Unicode text should be preserved through encode/decode. " +
                "Original: '$text', Decoded: '$decoded'")
        }
    }
    
    private fun generateRandomText(): String {
        val words = listOf(
            "hello", "world", "test", "tokenizer", "bpe", "algorithm", 
            "kotlin", "multiplatform", "encoding", "decoding", "merge",
            "vocabulary", "training", "text", "processing", "nlp",
            "aaabdaaabac", "repeated", "patterns", "common", "sequences"
        )
        val length = Random.nextInt(10, 25)  // Longer text for more merging opportunities
        val text = (0 until length).map { words.random() }.joinToString(" ")
        
        // Add some repeated patterns to ensure mergeable pairs
        val repeatedPattern = "aa bb cc dd ee"
        return "$text $repeatedPattern $text"
    }
}