package sk.ainet.nlp.tools.tokenizer

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll

/**
 * Property-based tests for BasicTokenizer implementation.
 * 
 * Feature: kotlin-minbpe-tokenizer
 */
class BasicTokenizerPropertyTest : FunSpec({
    
    test("Property 1: Basic tokenizer merge count") {
        // **Feature: kotlin-minbpe-tokenizer, Property 1: Basic tokenizer merge count**
        // **Validates: Requirements 1.2**
        
        checkAll(100, Arb.string(1, 50), Arb.int(256, 350)) { testText, vocabSize ->
            val tokenizer = BasicTokenizer()
            
            // Train the tokenizer
            tokenizer.train(testText, vocabSize, verbose = false)
            
            // The number of merges should be at most (vocabSize - 256)
            // It may be less if there aren't enough pairs to merge
            val maxExpectedMerges = vocabSize - 256
            tokenizer.merges.size shouldBeLessThanOrEqual maxExpectedMerges
            
            // Vocabulary size should be 256 + number of actual merges
            val expectedVocabSize = 256 + tokenizer.merges.size
            tokenizer.vocab.size shouldBe expectedVocabSize
        }
    }
    
    test("Property 2: Encode/decode round trip") {
        // **Feature: kotlin-minbpe-tokenizer, Property 2: Encode/decode round trip**
        // **Validates: Requirements 1.4**
        
        checkAll(100, Arb.string(1, 30), Arb.int(256, 320)) { trainingText, vocabSize ->
            val tokenizer = BasicTokenizer()
            
            // Train the tokenizer
            tokenizer.train(trainingText, vocabSize, verbose = false)
            
            // Test round trip on various strings
            checkAll(10, Arb.string(0, 20)) { inputText ->
                val encoded = tokenizer.encode(inputText)
                val decoded = tokenizer.decode(encoded)
                
                decoded shouldBe inputText
            }
        }
    }
    
    test("Property 3: Vocabulary consistency") {
        // **Feature: kotlin-minbpe-tokenizer, Property 3: Vocabulary consistency**
        // **Validates: Requirements 1.5**
        
        checkAll(100, Arb.string(1, 30), Arb.int(256, 320)) { testText, vocabSize ->
            val tokenizer = BasicTokenizer()
            
            // Train the tokenizer
            tokenizer.train(testText, vocabSize, verbose = false)
            
            // All token IDs in vocabulary should be valid
            for ((tokenId, bytes) in tokenizer.vocab) {
                // Token ID should be non-negative
                tokenId shouldBeGreaterThanOrEqual 0
                
                // Bytes should not be empty
                bytes.size shouldBeGreaterThanOrEqual 1
                
                // For base tokens (0-255), should be single byte
                if (tokenId <= 255) {
                    bytes.size shouldBe 1
                    bytes[0] shouldBe tokenId.toByte()
                }
            }
            
            // All merge token IDs should be in vocabulary
            for ((_, newTokenId) in tokenizer.merges) {
                tokenizer.vocab shouldContainKey newTokenId
                
                // Merge tokens should have at least 2 bytes (result of merging)
                val bytes = tokenizer.vocab[newTokenId]!!
                bytes.size shouldBeGreaterThanOrEqual 2
            }
            
            // Vocabulary should contain all base tokens (0-255)
            for (i in 0..255) {
                tokenizer.vocab shouldContainKey i
            }
        }
    }
    
    test("Empty text handling") {
        // Test that empty text is handled correctly
        checkAll(10, Arb.int(256, 300)) { vocabSize ->
            val tokenizer = BasicTokenizer()
            
            // Training on empty text should work
            tokenizer.train("", vocabSize, verbose = false)
            
            // Should have no merges for empty text
            tokenizer.merges.size shouldBe 0
            
            // Should have base vocabulary only
            tokenizer.vocab.size shouldBe 256
            
            // Encoding empty text should return empty list
            tokenizer.encode("") shouldBe emptyList()
            
            // Decoding empty list should return empty string
            tokenizer.decode(emptyList()) shouldBe ""
        }
    }
    
    test("Single character handling") {
        // Test that single character text is handled correctly
        checkAll(10, Arb.char('a'..'z'), Arb.int(256, 300)) { char, vocabSize ->
            val tokenizer = BasicTokenizer()
            val text = char.toString()
            
            // Training on single character should work
            tokenizer.train(text, vocabSize, verbose = false)
            
            // Should have no merges for single character
            tokenizer.merges.size shouldBe 0
            
            // Encode/decode should work correctly
            val encoded = tokenizer.encode(text)
            val decoded = tokenizer.decode(encoded)
            
            decoded shouldBe text
            encoded shouldBe listOf(char.code)
        }
    }
    
    test("Unicode handling") {
        // Test that Unicode characters are handled correctly
        val unicodeChars = listOf("世", "界", "🌍", "🚀", "α", "β", "γ")
        
        checkAll(20, Arb.list(Arb.element(unicodeChars), 1..4), Arb.int(256, 350)) { chars, vocabSize ->
            val tokenizer = BasicTokenizer()
            val text = chars.joinToString("")
            
            // Training should work with Unicode
            tokenizer.train(text, vocabSize, verbose = false)
            
            // Encode/decode should preserve Unicode text
            val encoded = tokenizer.encode(text)
            val decoded = tokenizer.decode(encoded)
            
            decoded shouldBe text
        }
    }
})