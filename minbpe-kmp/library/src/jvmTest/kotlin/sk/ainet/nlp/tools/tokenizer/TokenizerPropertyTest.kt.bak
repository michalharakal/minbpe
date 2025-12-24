package sk.ainet.nlp.tools.tokenizer

import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.buffered
import kotlinx.io.readString
import kotlinx.io.writeString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith
import kotlin.random.Random

/**
 * Property-based tests for the base Tokenizer class.
 * 
 * Feature: kotlin-minbpe-tokenizer
 */
class TokenizerPropertyTest {
    
    @Test
    fun testProperty10SaveLoadRoundTripProperty() {
        // **Feature: kotlin-minbpe-tokenizer, Property 10: Save/load round trip**
        // **Validates: Requirements 4.5**
        
        // Test with multiple random tokenizer configurations
        repeat(100) {
            val tokenizer = createTestTokenizer()
            val testText = generateRandomText()
            
            // Train the tokenizer
            val vocabSize = Random.nextInt(256, 300)
            tokenizer.train(testText, vocabSize)
            
            // Encode some text with the original tokenizer
            val originalEncoding = tokenizer.encode(testText)
            
            // Save and load the tokenizer
            val tempPrefix = "test_tokenizer_${Random.nextInt()}"
            try {
                tokenizer.save(tempPrefix)
                val loadedTokenizer = tokenizer.load("$tempPrefix.model")
                
                // The loaded tokenizer should produce identical encodings
                val loadedEncoding = loadedTokenizer.encode(testText)
                assertEquals(originalEncoding, loadedEncoding, 
                    "Loaded tokenizer should produce identical encodings")
                
                // Decoding should also be identical
                val originalDecoding = tokenizer.decode(originalEncoding)
                val loadedDecoding = loadedTokenizer.decode(loadedEncoding)
                assertEquals(originalDecoding, loadedDecoding,
                    "Loaded tokenizer should produce identical decodings")
                
                // Core properties should be preserved
                assertEquals(tokenizer.merges, loadedTokenizer.merges,
                    "Merges should be preserved after save/load")
                assertEquals(tokenizer.pattern, loadedTokenizer.pattern,
                    "Pattern should be preserved after save/load")
                assertEquals(tokenizer.specialTokens, loadedTokenizer.specialTokens,
                    "Special tokens should be preserved after save/load")
                
            } finally {
                // Clean up temporary files
                try {
                    SystemFileSystem.delete(Path("$tempPrefix.model"))
                    SystemFileSystem.delete(Path("$tempPrefix.vocab"))
                } catch (e: Exception) {
                    // Ignore cleanup errors
                }
            }
        }
    }
    
    @Test
    fun testProperty11FileFormatVersioningProperty() {
        // **Feature: kotlin-minbpe-tokenizer, Property 11: File format versioning**
        // **Validates: Requirements 4.4**
        
        // Test with multiple random tokenizer configurations
        repeat(100) {
            val tokenizer = createTestTokenizer()
            val testText = generateRandomText()
            
            // Train the tokenizer
            val vocabSize = Random.nextInt(256, 300)
            tokenizer.train(testText, vocabSize)
            
            // Save the tokenizer
            val tempPrefix = "test_version_${Random.nextInt()}"
            try {
                tokenizer.save(tempPrefix)
                
                // Read the model file directly and check version
                val modelPath = Path("$tempPrefix.model")
                val modelJson = SystemFileSystem.source(modelPath).buffered().use { source ->
                    source.readString()
                }
                
                val model = Json.decodeFromString<TokenizerModel>(modelJson)
                
                // The version should start with "minbpe v"
                assertTrue(model.version.startsWith("minbpe v"),
                    "Model version should start with 'minbpe v', got: ${model.version}")
                
                // The vocab file should also start with version
                val vocabPath = Path("$tempPrefix.vocab")
                val vocabContent = SystemFileSystem.source(vocabPath).buffered().use { source ->
                    source.readString()
                }
                
                val firstLine = vocabContent.lines().first()
                assertTrue(firstLine.startsWith("minbpe v"),
                    "Vocab file should start with 'minbpe v', got: $firstLine")
                
                // Loading should work with valid version
                val loadedTokenizer = tokenizer.load("$tempPrefix.model")
                assertEquals(tokenizer.merges.size, loadedTokenizer.merges.size,
                    "Loaded tokenizer should have same number of merges")
                
            } finally {
                // Clean up temporary files
                try {
                    SystemFileSystem.delete(Path("$tempPrefix.model"))
                    SystemFileSystem.delete(Path("$tempPrefix.vocab"))
                } catch (e: Exception) {
                    // Ignore cleanup errors
                }
            }
        }
    }
    
    @Test
    fun testInvalidVersionHandling() {
        // Test that invalid versions are rejected
        val invalidModel = TokenizerModel(
            version = "invalid version",
            pattern = "",
            specialTokens = emptyMap(),
            merges = emptyList()
        )
        
        val tempFile = "test_invalid_${Random.nextInt()}.model"
        try {
            // Save invalid model
            val modelJson = Json.encodeToString(invalidModel)
            val modelPath = Path(tempFile)
            SystemFileSystem.sink(modelPath).buffered().use { sink ->
                sink.writeString(modelJson)
            }
            
            // Loading should fail
            val tokenizer = createTestTokenizer()
            assertFailsWith<IllegalArgumentException> {
                tokenizer.load(tempFile)
            }
            
        } finally {
            try {
                SystemFileSystem.delete(Path(tempFile))
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
    }
    
    private fun createTestTokenizer(): TestTokenizer {
        return TestTokenizer()
    }
    
    private fun generateRandomText(): String {
        val words = listOf("hello", "world", "test", "tokenizer", "bpe", "algorithm", "kotlin", "multiplatform")
        val length = Random.nextInt(5, 20)
        return (0 until length).map { words.random() }.joinToString(" ")
    }
}

/**
 * Simple concrete implementation of Tokenizer for testing purposes.
 */
private class TestTokenizer : Tokenizer() {
    private var _merges: Map<Pair<Int, Int>, Int> = emptyMap()
    private var _vocab: Map<Int, ByteArray> = emptyMap()
    private var _pattern: String = ""
    private var _specialTokens: Map<String, Int> = emptyMap()
    
    override val merges: Map<Pair<Int, Int>, Int> get() = _merges
    override val vocab: Map<Int, ByteArray> get() = _vocab
    override val pattern: String get() = _pattern
    override val specialTokens: Map<String, Int> get() = _specialTokens
    
    override fun train(text: String, vocabSize: Int, verbose: Boolean) {
        validateVocabSize(vocabSize)
        
        // Simple training: create some dummy merges
        val numMerges = vocabSize - 256
        val mergeMap = mutableMapOf<Pair<Int, Int>, Int>()
        
        for (i in 0 until numMerges) {
            val pair = Pair(Random.nextInt(0, 256), Random.nextInt(0, 256))
            mergeMap[pair] = 256 + i
        }
        
        _merges = mergeMap
        _vocab = buildVocab()
    }
    
    override fun encode(text: String, allowedSpecial: String): List<Int> {
        // Simple encoding: convert to bytes
        return text.encodeToByteArray().map { it.toInt() and 0xFF }
    }
    
    override fun decode(ids: List<Int>): String {
        // Simple decoding: convert back to bytes
        return ids.map { it.toByte() }.toByteArray().decodeToString()
    }
    
    override fun createFromModel(model: TokenizerModel): Tokenizer {
        val tokenizer = TestTokenizer()
        tokenizer._merges = model.getMergesMap()
        tokenizer._vocab = tokenizer.buildVocab()
        tokenizer._pattern = model.pattern
        tokenizer._specialTokens = model.specialTokens
        return tokenizer
    }
}