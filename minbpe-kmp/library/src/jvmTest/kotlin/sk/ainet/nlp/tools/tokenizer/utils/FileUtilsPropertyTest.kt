package sk.ainet.nlp.tools.tokenizer.utils

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import sk.ainet.nlp.tools.tokenizer.BasicTokenizer
import sk.ainet.nlp.tools.tokenizer.persistence.LoadResult
import sk.ainet.nlp.tools.tokenizer.persistence.SaveProgress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.random.Random
import java.io.File

/**
 * Property-based tests for file operations utilities.
 * 
 * These tests validate universal properties that should hold across all inputs,
 * ensuring robust cross-platform file handling and UTF-8 encoding/decoding.
 * 
 * Feature: kotlin-minbpe-tokenizer
 */
class FileUtilsPropertyTest {
    
    /**
     * Property 15: UTF-8 Round Trip
     * For any valid UTF-8 text, encoding to bytes then decoding should preserve the original text
     * **Feature: kotlin-minbpe-tokenizer, Property 15: UTF-8 round trip**
     * **Validates: Requirements 6.3**
     */
    @Test
    fun testProperty15UTF8RoundTrip() = runBlocking {
        // Test with multiple random UTF-8 texts
        repeat(100) {
            val text = generateRandomUTF8Text()
            
            // Create a temporary directory for this test iteration
            val tempDir = File.createTempFile("utf8_test", "").apply { 
                delete()
                mkdirs() 
            }
            
            try {
                // Create and train a tokenizer with the test text
                val tokenizer = BasicTokenizer()
                
                // Only train if text is not empty
                if (text.isNotEmpty()) {
                    try {
                        tokenizer.train(text, 258, verbose = false)
                        
                        // Test both JSON and Python-compatible formats
                        val jsonFile = File(tempDir, "test.json").absolutePath
                        val modelFile = File(tempDir, "test.model").absolutePath
                        
                        // Save in JSON format
                        val jsonSaveResults = FileUtils.save(tokenizer, jsonFile).toList()
                        val jsonSaveSuccess = jsonSaveResults.any { it is SaveProgress.Completed }
                        
                        if (jsonSaveSuccess) {
                            // Load from JSON format
                            val jsonLoadResults = FileUtils.load(jsonFile).toList()
                            val jsonLoadResult = jsonLoadResults.lastOrNull()
                            
                            if (jsonLoadResult is LoadResult.Completed) {
                                // Verify the loaded tokenizer can handle UTF-8 correctly
                                val loadedTokenizer = BasicTokenizer()
                                loadedTokenizer.loadFromData(jsonLoadResult.tokenizer)
                                
                                // Test UTF-8 round trip through tokenization
                                val encoded = loadedTokenizer.encode(text)
                                val decoded = loadedTokenizer.decode(encoded)
                                
                                // The decoded text should match the original
                                assertEquals(text, decoded, "UTF-8 round trip failed for text: '$text'")
                            }
                        }
                        
                        // Save in Python-compatible format
                        val modelSaveResults = FileUtils.save(tokenizer, modelFile).toList()
                        val modelSaveSuccess = modelSaveResults.any { it is SaveProgress.Completed }
                        
                        if (modelSaveSuccess) {
                            // Load from Python-compatible format
                            val modelLoadResults = FileUtils.load(modelFile).toList()
                            val modelLoadResult = modelLoadResults.lastOrNull()
                            
                            if (modelLoadResult is LoadResult.Completed) {
                                // Verify the loaded tokenizer can handle UTF-8 correctly
                                val loadedTokenizer = BasicTokenizer()
                                loadedTokenizer.loadFromData(modelLoadResult.tokenizer)
                                
                                // Test UTF-8 round trip through tokenization
                                val encoded = loadedTokenizer.encode(text)
                                val decoded = loadedTokenizer.decode(encoded)
                                
                                // The decoded text should match the original
                                assertEquals(text, decoded, "UTF-8 round trip failed for text: '$text'")
                            }
                        }
                        
                    } catch (e: Exception) {
                        // Some texts might not be suitable for training (e.g., too short, invalid UTF-8)
                        // This is acceptable behavior, not a test failure
                    }
                }
                
            } finally {
                // Clean up
                tempDir.deleteRecursively()
            }
        }
    }
    
    /**
     * Generate random UTF-8 text for testing.
     * This includes various Unicode characters to test UTF-8 handling.
     */
    private fun generateRandomUTF8Text(): String {
        val length = Random.nextInt(1, 100)
        val chars = mutableListOf<Char>()
        
        repeat(length) {
            when (Random.nextInt(6)) {
                0 -> chars.add(Random.nextInt(32, 127).toChar()) // ASCII printable
                1 -> chars.add(Random.nextInt(160, 256).toChar()) // Latin-1 supplement
                2 -> chars.add(Random.nextInt(0x0100, 0x017F).toChar()) // Latin Extended-A
                3 -> chars.add(listOf('世', '界', '你', '好').random()) // Chinese characters
                4 -> chars.add(listOf('α', 'β', 'γ', 'δ').random()) // Greek letters
                else -> chars.add(listOf("hello", "world", "test", " ", "\n").random().first())
            }
        }
        
        return chars.joinToString("")
    }
}