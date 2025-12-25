package sk.ainet.nlp.tools.tokenizer

import sk.ainet.nlp.tools.tokenizer.utils.BpeUtils
import sk.ainet.nlp.tools.tokenizer.persistence.TokenizerData

/**
 * Basic Byte Pair Encoding (BPE) tokenizer implementation.
 * 
 * This is the simplest BPE tokenizer that implements the core algorithm:
 * 1. Start with UTF-8 bytes as base vocabulary (tokens 0-255)
 * 2. Iteratively find the most frequent consecutive pair
 * 3. Merge that pair into a new token (256, 257, 258, ...)
 * 4. Repeat until desired vocabulary size is reached
 * 
 * The BasicTokenizer does not use regex splitting or special tokens,
 * making it suitable for understanding the fundamental BPE algorithm.
 * 
 * Requirements implemented:
 * - 1.1: Core BPE training algorithm on UTF-8 encoded text
 * - 1.2: Create exactly (vocab_size - 256) merge operations
 * - 1.3: Convert strings to token ID lists using trained merges
 * - 1.4: Reconstruct original text from token sequences
 * - 1.5: Maintain vocabulary mappings from token IDs to byte sequences
 */
open class BasicTokenizer : Tokenizer() {
    
    private var _merges: Map<Pair<Int, Int>, Int> = emptyMap()
    private var _vocab: Map<Int, ByteArray> = emptyMap()
    
    override val merges: Map<Pair<Int, Int>, Int> get() = _merges
    override val vocab: Map<Int, ByteArray> get() = _vocab
    override val pattern: String = ""  // BasicTokenizer doesn't use regex patterns
    override val specialTokens: Map<String, Int> = emptyMap()  // BasicTokenizer doesn't use special tokens
    
    /**
     * Train the BasicTokenizer on the given text with BPE algorithm.
     * 
     * The training process:
     * 1. Convert text to UTF-8 bytes (initial tokens 0-255)
     * 2. Iteratively find most frequent consecutive pair
     * 3. Create new merge rule and token ID (starting from 256)
     * 4. Apply merge to all text and repeat
     * 5. Continue until we have (vocabSize - 256) merges
     * 
     * @param text Training text to learn BPE merges from
     * @param vocabSize Target vocabulary size (must be >= 256)
     * @param verbose Whether to print training progress
     * @throws IllegalArgumentException if parameters are invalid
     * @throws TokenizationException if training fails
     * 
     * Requirements: 1.1, 1.2
     */
    override fun train(text: String, vocabSize: Int, verbose: Boolean) {
        // Validate inputs
        validateText(text, "training")
        validateVocabSize(vocabSize)
        
        try {
            // Handle empty text case
            if (text.isEmpty()) {
                if (verbose) {
                    println("Training BasicTokenizer with empty text")
                    println("No merges will be learned")
                }
                _merges = emptyMap()
                _vocab = buildVocab()
                return
            }
            
            // Convert text to UTF-8 bytes as initial token sequence
            val textBytes = safeEncodeUtf8(text)
            var ids = textBytes.map { it.toInt() and 0xFF }.toMutableList()  // Convert to unsigned bytes
            
            val merges = mutableMapOf<Pair<Int, Int>, Int>()
            val numMerges = vocabSize - 256
            
            if (verbose) {
                println("Training BasicTokenizer with vocab size $vocabSize")
                println("Text length: ${text.length} characters, ${textBytes.size} bytes")
                println("Target merges: $numMerges")
            }
            
            // Perform BPE training
            for (i in 0 until numMerges) {
                // Count all consecutive pairs
                val stats = BpeUtils.getStats(ids)
                
                if (stats.isEmpty()) {
                    if (verbose) {
                        println("No more pairs to merge at iteration $i")
                    }
                    break
                }
                
                // Find the most frequent pair
                val mostFrequentPair = stats.maxByOrNull { it.value }?.key
                    ?: break
                
                val newTokenId = 256 + i
                
                if (verbose) {
                    println("Merge $i: $mostFrequentPair -> $newTokenId (count: ${stats[mostFrequentPair]})")
                }
                
                // Record the merge rule
                merges[mostFrequentPair] = newTokenId
                
                // Apply the merge to the token sequence
                ids = BpeUtils.merge(ids, mostFrequentPair, newTokenId).toMutableList()
            }
            
            // Store the learned merges and build vocabulary
            _merges = merges.toMap()
            _vocab = buildVocab()
            
            if (verbose) {
                println("Training complete. Learned ${_merges.size} merges.")
                println("Final vocabulary size: ${_vocab.size}")
            }
            
        } catch (e: TokenizationException) {
            throw e // Re-throw tokenization exceptions
        } catch (e: Exception) {
            throw TokenizationException("Training failed: ${e.message}", e)
        }
    }
    
    /**
     * Encode text into a list of token IDs using the trained BPE merges.
     * 
     * The encoding process:
     * 1. Convert text to UTF-8 bytes (tokens 0-255)
     * 2. Iteratively apply learned merges in the order they were learned
     * 3. Continue until no more merges can be applied
     * 
     * @param text Input text to encode
     * @param allowedSpecial Ignored for BasicTokenizer (no special tokens)
     * @return List of token IDs representing the encoded text
     * @throws IllegalStateException if tokenizer is not trained
     * @throws TokenizationException if encoding fails
     * 
     * Requirements: 1.3
     */
    override fun encode(text: String, allowedSpecial: String): List<Int> {
        validateInitialized()
        
        // Handle empty text
        if (text.isEmpty()) {
            return emptyList()
        }
        
        try {
            // Convert text to UTF-8 bytes as initial token sequence
            val textBytes = safeEncodeUtf8(text)
            var ids = textBytes.map { it.toInt() and 0xFF }  // Convert to unsigned bytes
            
            // Apply merges in the order they were learned (greedy approach)
            // We need to keep applying merges until no more can be applied
            while (true) {
                val stats = BpeUtils.getStats(ids)
                
                // Find the merge with the lowest index (earliest learned) that can be applied
                val applicableMerge = _merges.entries
                    .filter { (pair, _) -> stats.containsKey(pair) }
                    .minByOrNull { (_, tokenId) -> tokenId }
                
                if (applicableMerge == null) {
                    break  // No more merges can be applied
                }
                
                val (pair, newTokenId) = applicableMerge
                ids = BpeUtils.merge(ids, pair, newTokenId)
            }
            
            return ids
            
        } catch (e: TokenizationException) {
            throw e // Re-throw tokenization exceptions
        } catch (e: Exception) {
            throw TokenizationException("Encoding failed: ${e.message}", e)
        }
    }
    
    /**
     * Decode a list of token IDs back into the original text.
     * 
     * The decoding process:
     * 1. Look up each token ID in the vocabulary to get byte sequences
     * 2. Concatenate all byte sequences
     * 3. Decode the resulting bytes as UTF-8 text
     * 
     * @param ids List of token IDs to decode
     * @return Decoded text string
     * @throws IllegalStateException if tokenizer is not trained
     * @throws TokenizationException if decoding fails
     * 
     * Requirements: 1.4
     */
    override fun decode(ids: List<Int>): String {
        validateInitialized()
        
        // Handle empty input
        if (ids.isEmpty()) {
            return ""
        }
        
        try {
            // Validate token IDs
            validateTokenIds(ids)
            
            // Concatenate byte sequences for all token IDs
            val allBytes = mutableListOf<Byte>()
            
            for (id in ids) {
                val bytes = _vocab[id] 
                    ?: throw IllegalArgumentException("Unknown token ID: $id")
                allBytes.addAll(bytes.toList())
            }
            
            // Convert bytes back to string with safe UTF-8 handling
            return safeDecodeUtf8(allBytes.toByteArray())
            
        } catch (e: TokenizationException) {
            throw e // Re-throw tokenization exceptions
        } catch (e: Exception) {
            throw TokenizationException("Decoding failed: ${e.message}", e)
        }
    }
    
    /**
     * Load tokenizer state from serialized data.
     * 
     * @param data Serialized tokenizer data containing merges and metadata
     * @throws IllegalArgumentException if data is invalid for BasicTokenizer
     * @throws TokenizationException if loading fails
     */
    override fun loadFromData(data: TokenizerData) {
        try {
            data.validate()
            
            // Verify this is a BasicTokenizer model (no pattern, no special tokens)
            if (data.pattern.isNotEmpty()) {
                throw IllegalArgumentException("BasicTokenizer model should have empty pattern, got: '${data.pattern}'")
            }
            
            if (data.specialTokens.isNotEmpty()) {
                throw IllegalArgumentException("BasicTokenizer model should have no special tokens, got: ${data.specialTokens}")
            }
            
            _merges = data.getMergesMap()
            _vocab = buildVocab()
            
        } catch (e: IllegalArgumentException) {
            throw e // Re-throw validation errors
        } catch (e: Exception) {
            throw TokenizationException("Failed to load BasicTokenizer from data: ${e.message}", e)
        }
    }
    
    /**
     * Load tokenizer state from serialized data.
     * 
     * @param data Serialized tokenizer data containing merges and metadata
     */
    @Deprecated("Use loadFromData instead", ReplaceWith("loadFromData(model)"))
    fun createFromModel(model: TokenizerModel): Tokenizer {
        // This method is deprecated and will be removed
        // Use the persistence layer instead
        throw UnsupportedOperationException("Use TokenizerRepository.load() instead")
    }
}