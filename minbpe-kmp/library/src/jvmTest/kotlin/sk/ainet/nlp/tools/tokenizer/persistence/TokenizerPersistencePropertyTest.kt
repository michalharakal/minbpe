package sk.ainet.nlp.tools.tokenizer.persistence

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.test.assertContains
import kotlin.random.Random
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.toList
import kotlinx.io.Buffer
import kotlinx.io.readString
import kotlinx.io.copyTo
import sk.ainet.nlp.tools.tokenizer.BasicTokenizer
import sk.ainet.nlp.tools.tokenizer.Tokenizer

/**
 * Property-based tests for TokenizerPersistence implementation using manual property testing.
 * 
 * Feature: kotlin-minbpe-tokenizer
 */
class TokenizerPersistencePropertyTest {
    
    @Test
    fun testProperty10_SaveLoadRoundTripProperty() = runBlocking {
        // **Feature: kotlin-minbpe-tokenizer, Property 10: Save/load round trip**
        // **Validates: Requirements 4.7**
        
        val persistence = JsonTokenizerPersistence()
        
        // Test with multiple random tokenizer configurations
        repeat(100) {
            val tokenizer = createTrainedTokenizer()
            
            // Save tokenizer to buffer
            val saveBuffer = Buffer()
            val saveResults = persistence.save(tokenizer, saveBuffer).toList()
            
            // Verify save completed successfully
            val lastSaveResult = saveResults.last()
            assertTrue(lastSaveResult is SaveProgress.Completed)
            
            // Load tokenizer from buffer - create new buffer from saved data
            val loadBuffer = Buffer()
            loadBuffer.write(saveBuffer, saveBuffer.size)
            val loadResults = persistence.load(loadBuffer).toList()
            
            // Verify load completed successfully
            val lastLoadResult = loadResults.last()
            assertTrue(lastLoadResult is LoadResult.Completed)
            
            val loadedData = (lastLoadResult as LoadResult.Completed).tokenizer
            
            // Create new tokenizer from loaded data
            val newTokenizer = BasicTokenizer()
            newTokenizer.loadFromData(loadedData)
            
            // Test that encoding produces identical results
            val testTexts = listOf(
                "hello world",
                "test tokenization",
                "aaabdaaabac", // Classic BPE example
                "",
                "a",
                "unicode: 世界 🌍"
            )
            
            for (testText in testTexts) {
                val originalEncoded = tokenizer.encode(testText)
                val loadedEncoded = newTokenizer.encode(testText)
                
                assertEquals(originalEncoded, loadedEncoded)
                
                // Also verify decoding works identically
                val originalDecoded = tokenizer.decode(originalEncoded)
                val loadedDecoded = newTokenizer.decode(loadedEncoded)
                
                assertEquals(originalDecoded, loadedDecoded)
                assertEquals(testText, originalDecoded)
            }
            
            // Verify core properties are preserved
            assertEquals(tokenizer.merges, newTokenizer.merges)
            assertEquals(tokenizer.pattern, newTokenizer.pattern)
            assertEquals(tokenizer.specialTokens, newTokenizer.specialTokens)
            assertEquals(tokenizer.vocab.size, newTokenizer.vocab.size)
        }
    }
    
    @Test
    fun testProperty11_FileFormatVersioningProperty() = runBlocking {
        // **Feature: kotlin-minbpe-tokenizer, Property 11: File format versioning**
        // **Validates: Requirements 4.6**
        
        val persistence = JsonTokenizerPersistence()
        
        // Test with multiple random tokenizer configurations
        repeat(50) {
            val tokenizer = createTrainedTokenizer()
            
            // Save tokenizer to buffer
            val buffer = Buffer()
            val saveResults = persistence.save(tokenizer, buffer).toList()
            
            // Verify save completed successfully
            val lastResult = saveResults.last()
            assertTrue(lastResult is SaveProgress.Completed)
            
            // Read the raw JSON content by copying buffer data
            val bufferCopy = Buffer()
            buffer.copyTo(bufferCopy)
            val jsonContent = bufferCopy.readString()
            
            // Verify the JSON contains version information
            assertTrue(jsonContent.startsWith("{"))
            assertContains(jsonContent, "\"version\"")
            assertContains(jsonContent, "\"minbpe v1\"")
            
            // Load and verify the version is correctly parsed
            val loadBuffer = Buffer()
            buffer.copyTo(loadBuffer)
            val loadResults = persistence.load(loadBuffer).toList()
            val loadResult = loadResults.last()
            assertTrue(loadResult is LoadResult.Completed)
            
            val loadedData = (loadResult as LoadResult.Completed).tokenizer
            assertTrue(loadedData.version.startsWith("minbpe v"))
            assertEquals("minbpe v1", loadedData.version)
        }
    }
    
    @Test
    fun testProperty17_ReactiveProgressEventsProperty() = runBlocking {
        // **Feature: kotlin-minbpe-tokenizer, Property 17: Reactive progress events**
        // **Validates: Requirements 4.2**
        
        val persistence = JsonTokenizerPersistence()
        
        // Test with multiple random tokenizer configurations
        repeat(50) {
            val tokenizer = createTrainedTokenizer()
            
            // Test save progress events
            val saveBuffer = Buffer()
            val saveResults = persistence.save(tokenizer, saveBuffer).toList()
            
            // Verify save progress sequence
            assertNotEquals(0, saveResults.size)
            assertTrue(saveResults.first() is SaveProgress.Started)
            assertTrue(saveResults.last() is SaveProgress.Completed)
            
            // Should have WritingModel event
            val hasWritingModel = saveResults.any { it is SaveProgress.WritingModel }
            assertTrue(hasWritingModel)
            
            // Test load progress events
            val loadBuffer = Buffer()
            loadBuffer.write(saveBuffer, saveBuffer.size)
            val loadResults = persistence.load(loadBuffer).toList()
            
            // Verify load progress sequence
            assertNotEquals(0, loadResults.size)
            assertTrue(loadResults.first() is LoadResult.Started)
            assertTrue(loadResults.last() is LoadResult.Completed)
            
            // Should have ReadingModel and ParsingModel events
            val hasReadingModel = loadResults.any { it is LoadResult.ReadingModel }
            val hasParsingModel = loadResults.any { it is LoadResult.ParsingModel }
            
            assertTrue(hasReadingModel)
            assertTrue(hasParsingModel)
            
            // Test saveVocab progress events
            val vocabBuffer = Buffer()
            val vocabResults = persistence.saveVocab(tokenizer, vocabBuffer).toList()
            
            // Verify vocab save progress sequence
            assertNotEquals(0, vocabResults.size)
            assertTrue(vocabResults.first() is SaveProgress.Started)
            assertTrue(vocabResults.last() is SaveProgress.Completed)
            
            // Should have WritingVocab events for non-empty tokenizers
            if (tokenizer.vocab.size > 256) {
                val hasWritingVocab = vocabResults.any { it is SaveProgress.WritingVocab }
                assertTrue(hasWritingVocab)
            }
        }
    }
    
    /**
     * Create a trained BasicTokenizer instance for testing.
     */
    private fun createTrainedTokenizer(): Tokenizer {
        val tokenizer = BasicTokenizer()
        
        // Generate training text with patterns that will create merges
        val baseWords = listOf("hello", "world", "test", "tokenizer", "bpe", "algorithm")
        val repeatedPatterns = listOf("aa", "bb", "cc", "dd", "ee")
        
        val textParts = mutableListOf<String>()
        
        // Add base words
        repeat(Random.nextInt(5, 15)) {
            textParts.add(baseWords.random())
        }
        
        // Add repeated patterns to ensure merges
        repeat(Random.nextInt(3, 8)) {
            textParts.add(repeatedPatterns.random())
        }
        
        val trainingText = textParts.joinToString(" ")
        val vocabSize = Random.nextInt(256, 350)
        
        // Train the tokenizer
        tokenizer.train(trainingText, vocabSize, verbose = false)
        
        return tokenizer
    }
}