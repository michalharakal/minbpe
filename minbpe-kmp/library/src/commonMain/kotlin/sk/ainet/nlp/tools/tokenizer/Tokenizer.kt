package sk.ainet.nlp.tools.tokenizer

import sk.ainet.nlp.tools.tokenizer.persistence.TokenizerData

/**
 * Abstract base class for all tokenizer implementations.
 * 
 * Provides common functionality for BPE tokenization including:
 * - Abstract interface for training, encoding, and decoding
 * - Vocabulary building utilities
 * 
 * Note: Save/load functionality is handled by the persistence layer
 * (TokenizerPersistence and TokenizerRepository) to separate I/O concerns
 * from tokenizer logic.
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
     * Load tokenizer state from serialized data.
     * This method is used by the persistence layer to reconstruct tokenizers.
     * 
     * @param data Serialized tokenizer data
     */
    abstract fun loadFromData(data: TokenizerData)
    
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