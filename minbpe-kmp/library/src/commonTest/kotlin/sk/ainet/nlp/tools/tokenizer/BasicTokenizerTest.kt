package sk.ainet.nlp.tools.tokenizer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Unit tests for BasicTokenizer implementation.
 * 
 * Tests the core BPE functionality including:
 * - Training with various vocabulary sizes
 * - Encode/decode round trip consistency
 * - Wikipedia BPE example validation
 * - Error handling for edge cases
 */
class BasicTokenizerTest {
    
    @Test
    fun testBasicTraining() {
        val tokenizer = BasicTokenizer()
        val text = "aaabdaaabac"
        
        // Train with vocabulary size 259 (256 base + 3 merges)
        tokenizer.train(text, 259, verbose = false)
        
        // Should have learned 3 merges
        assertEquals(3, tokenizer.merges.size)
        assertEquals(259, tokenizer.vocab.size)
    }
    
    @Test
    fun testEncodeDecodeRoundTrip() {
        val tokenizer = BasicTokenizer()
        val text = "hello world! this is a test."
        
        // Train the tokenizer
        tokenizer.train(text, 280, verbose = false)
        
        // Encode and decode should preserve the original text
        val encoded = tokenizer.encode(text)
        val decoded = tokenizer.decode(encoded)
        
        assertEquals(text, decoded)
    }
    
    @Test
    fun testWikipediaBPEExample() {
        val tokenizer = BasicTokenizer()
        val text = "aaabdaaabac"
        
        // Train with vocabulary size 259 (should create 3 merges)
        tokenizer.train(text, 259, verbose = false)
        
        // Encode the text
        val encoded = tokenizer.encode(text)
        
        // The expected result should be [258, 100, 258, 97, 99]
        // where 258 is the merged "aa", 100 is 'd', 97 is 'a', 99 is 'c'
        assertEquals(listOf(258, 100, 258, 97, 99), encoded)
    }
    
    @Test
    fun testVocabSizeValidation() {
        val tokenizer = BasicTokenizer()
        
        // Should throw exception for vocab size < 256
        assertFailsWith<IllegalArgumentException> {
            tokenizer.train("test", 255)
        }
    }
    
    @Test
    fun testEncodeBeforeTraining() {
        val tokenizer = BasicTokenizer()
        
        // Should throw exception when encoding before training
        assertFailsWith<IllegalStateException> {
            tokenizer.encode("test")
        }
    }
    
    @Test
    fun testDecodeBeforeTraining() {
        val tokenizer = BasicTokenizer()
        
        // Should throw exception when decoding before training
        assertFailsWith<IllegalStateException> {
            tokenizer.decode(listOf(97, 98, 99))
        }
    }
    
    @Test
    fun testEmptyText() {
        val tokenizer = BasicTokenizer()
        
        // Training on empty text should work
        tokenizer.train("", 256, verbose = false)
        
        // Should have no merges
        assertEquals(0, tokenizer.merges.size)
        assertEquals(256, tokenizer.vocab.size)
        
        // Encoding empty text should return empty list
        assertEquals(emptyList(), tokenizer.encode(""))
        
        // Decoding empty list should return empty string
        assertEquals("", tokenizer.decode(emptyList()))
    }
    
    @Test
    fun testSingleCharacter() {
        val tokenizer = BasicTokenizer()
        val text = "a"
        
        // Train with minimal vocabulary
        tokenizer.train(text, 256, verbose = false)
        
        // Should have no merges (only one character)
        assertEquals(0, tokenizer.merges.size)
        
        // Encode/decode should work
        val encoded = tokenizer.encode(text)
        val decoded = tokenizer.decode(encoded)
        
        assertEquals(text, decoded)
        assertEquals(listOf(97), encoded) // 'a' = 97
    }
    
    @Test
    fun testUnicodeHandling() {
        val tokenizer = BasicTokenizer()
        val text = "Hello 世界! 🌍"
        
        // Train the tokenizer
        tokenizer.train(text, 300, verbose = false)
        
        // Encode/decode should preserve Unicode characters
        val encoded = tokenizer.encode(text)
        val decoded = tokenizer.decode(encoded)
        
        assertEquals(text, decoded)
    }
}