package sk.ainet.nlp.tools.tokenizer

import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.buffered
import kotlinx.io.readString
import kotlinx.io.writeString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Abstract base class for all tokenizer implementations.
 * 
 * Provides common functionality for BPE tokenization including:
 * - Abstract interface for training, encoding, and decoding
 * - Save/load functionality for tokenizer persistence
 * - Vocabulary building utilities
 * 
 * Concrete implementations include:
 * - [BasicTokenizer]: Simple BPE implementation
 * - [RegexTokenizer]: Regex-based BPE with special tokens
 * - [GPT4Tokenizer]: GPT-4 compatible tokenizer
 */
abstract class Tokenizer {
    
    /**
     * Map of merge rules: (token1, token2) -> new_token_id
     */
    abstract val merges: Map<Pair<Int, Int>, Int>
    
    /**
     * Vocabulary mapping: token_id -> byte_sequence
     */
    abstract val vocab: Map<Int, ByteArray>
    
    /**
     * Regex pattern used for text splitting (empty for BasicTokenizer)
     */
    abstract val pattern: String
    
    /**
     * Special tokens mapping: token_string -> token_id
     */
    abstract val specialTokens: Map<String, Int>
    
    /**
     * Train the tokenizer on the given text with specified vocabulary size.
     * 
     * @param text Training text
     * @param vocabSize Target vocabulary size (must be >= 256)
     * @param verbose Whether to print training progress
     */
    abstract fun train(text: String, vocabSize: Int, verbose: Boolean = false)
    
    /**
     * Encode text into a list of token IDs.
     * 
     * @param text Input text to encode
     * @param allowedSpecial Special token handling policy
     * @return List of token IDs
     */
    abstract fun encode(text: String, allowedSpecial: String = "none_raise"): List<Int>
    
    /**
     * Decode a list of token IDs back into text.
     * 
     * @param ids List of token IDs to decode
     * @return Decoded text
     */
    abstract fun decode(ids: List<Int>): String
    
    /**
     * Save the tokenizer to files with the given prefix.
     * Creates two files:
     * - {filePrefix}.model: Serialized tokenizer model
     * - {filePrefix}.vocab: Human-readable vocabulary file
     * 
     * @param filePrefix Prefix for output files
     */
    fun save(filePrefix: String) {
        // Save model file
        val model = TokenizerModel(
            version = "minbpe v1",
            pattern = pattern,
            specialTokens = specialTokens,
            merges = merges.map { (pair, id) -> pair to id }
        )
        
        val modelJson = Json.encodeToString(model)
        val modelPath = Path("$filePrefix.model")
        SystemFileSystem.sink(modelPath).buffered().use { sink ->
            sink.writeString(modelJson)
        }
        
        // Save vocab file for human inspection
        val vocabContent = buildString {
            appendLine("minbpe v1")
            appendLine("Pattern: $pattern")
            appendLine("Special tokens: ${specialTokens.size}")
            
            specialTokens.forEach { (token, id) ->
                appendLine("$token -> $id")
            }
            
            appendLine("Vocabulary: ${vocab.size}")
            vocab.entries.sortedBy { it.key }.forEach { (id, bytes) ->
                val tokenStr = try {
                    bytes.decodeToString()
                } catch (e: Exception) {
                    bytes.joinToString(" ") { byte -> 
                        val unsigned = byte.toInt() and 0xFF
                        if (unsigned < 16) "0${unsigned.toString(16)}" else unsigned.toString(16)
                    }
                }
                appendLine("$id -> $tokenStr")
            }
        }
        
        val vocabPath = Path("$filePrefix.vocab")
        SystemFileSystem.sink(vocabPath).buffered().use { sink ->
            sink.writeString(vocabContent)
        }
    }
    
    /**
     * Load a tokenizer from a model file.
     * 
     * @param modelFile Path to the model file
     * @return Loaded tokenizer instance
     */
    fun load(modelFile: String): Tokenizer {
        val modelPath = Path(modelFile)
        val modelJson = SystemFileSystem.source(modelPath).buffered().use { source ->
            source.readString()
        }
        val model = Json.decodeFromString<TokenizerModel>(modelJson)
        
        // Validate version
        if (!model.version.startsWith("minbpe v")) {
            throw IllegalArgumentException("Unsupported model version: ${model.version}")
        }
        
        return createFromModel(model)
    }
    
    /**
     * Create a tokenizer instance from a loaded model.
     * Subclasses must implement this to reconstruct their specific type.
     * 
     * @param model Loaded tokenizer model
     * @return Tokenizer instance
     */
    protected abstract fun createFromModel(model: TokenizerModel): Tokenizer
    
    /**
     * Build vocabulary mapping from token IDs to byte sequences.
     * Combines base vocabulary (0-255) with merge-generated tokens.
     * 
     * @return Complete vocabulary mapping
     */
    protected fun buildVocab(): Map<Int, ByteArray> {
        val vocab = mutableMapOf<Int, ByteArray>()
        
        // Base vocabulary: single bytes
        for (i in 0..255) {
            vocab[i] = byteArrayOf(i.toByte())
        }
        
        // Add merged tokens
        for ((pair, newId) in merges) {
            val (id1, id2) = pair
            val bytes1 = vocab[id1] ?: throw IllegalStateException("Token $id1 not found in vocab")
            val bytes2 = vocab[id2] ?: throw IllegalStateException("Token $id2 not found in vocab")
            vocab[newId] = bytes1 + bytes2
        }
        
        return vocab
    }
    
    /**
     * Validate that vocabulary size is acceptable.
     * 
     * @param vocabSize Requested vocabulary size
     */
    protected fun validateVocabSize(vocabSize: Int) {
        if (vocabSize < 256) {
            throw IllegalArgumentException("Vocabulary size must be at least 256, got $vocabSize")
        }
    }
}