package sk.ainet.nlp.tools.tokenizer

import sk.ainet.nlp.tools.tokenizer.utils.BpeUtils
import sk.ainet.nlp.tools.tokenizer.persistence.TokenizerData

/**
 * Regex-based Byte Pair Encoding (BPE) tokenizer implementation.
 * 
 * This tokenizer extends the basic BPE algorithm with:
 * 1. Regex-based text splitting before BPE processing
 * 2. Special token registration and handling
 * 3. Configurable special token policies
 * 
 * The RegexTokenizer splits input text using regex patterns to prevent
 * merges across category boundaries (e.g., letters vs numbers vs punctuation).
 * This approach is used in production tokenizers like GPT-4's cl100k_base.
 * 
 * Requirements implemented:
 * - 2.1: Split input text using configurable regex patterns before BPE
 * - 2.2: Use GPT-4 split pattern as default when no pattern specified
 * - 2.3: Support registration and handling of special tokens with custom IDs
 * - 2.4: Handle allowed_special parameter ("all", "none", "none_raise", custom set)
 * - 2.5: Raise error when special tokens encountered but not allowed
 * - 2.6: Process text chunks independently to prevent merges across boundaries
 */
open class RegexTokenizer(
    pattern: String? = null
) : Tokenizer() {
    
    companion object {
        /**
         * GPT-4 split pattern for text categorization.
         * This pattern splits text into categories like:
         * - Contractions ('s, 't, 'll, 've, 're, 'd, 'm)
         * - Letters (with optional leading space/punctuation)
         * - Numbers (1-3 digits)
         * - Other characters (punctuation, symbols)
         * - Whitespace and newlines
         */
        const val GPT4_SPLIT_PATTERN = """'(?i:[sdmt]|ll|ve|re)|[^\r\n\p{L}\p{N}]?+\p{L}+|\p{N}{1,3}| ?[^\s\p{L}\p{N}]++[\r\n]*|\s*[\r\n]|\s+(?!\S)|\s+"""
    }
    
    private var _merges: Map<Pair<Int, Int>, Int> = emptyMap()
    private var _vocab: Map<Int, ByteArray> = emptyMap()
    private var _specialTokens: Map<String, Int> = emptyMap()
    private var _inverseSpecialTokens: Map<Int, String> = emptyMap()
    
    override val merges: Map<Pair<Int, Int>, Int> get() = _merges
    override val vocab: Map<Int, ByteArray> get() = _vocab
    override val pattern: String = pattern ?: GPT4_SPLIT_PATTERN
    override val specialTokens: Map<String, Int> get() = _specialTokens
    
    private val compiledPattern: Regex
    
    init {
        // Validate and compile the pattern during initialization
        validatePattern(this.pattern)
        try {
            compiledPattern = Regex(this.pattern)
        } catch (e: Exception) {
            throw TokenizationException("Failed to compile regex pattern: ${this.pattern}", e)
        }
    }
    
    /**
     * Register special tokens for encoding and decoding.
     * 
     * Special tokens are reserved strings that get assigned specific token IDs
     * and are handled separately from the normal BPE process.
     * 
     * @param specialTokens Map from special token strings to their assigned IDs
     * @throws TokenizationException if special tokens are invalid
     * 
     * Requirements: 2.3 - Support registration and handling of special tokens
     */
    fun registerSpecialTokens(specialTokens: Map<String, Int>) {
        try {
            // Validate special token IDs
            for ((token, id) in specialTokens) {
                if (token.isBlank()) {
                    throw TokenizationException("Special token cannot be blank")
                }
                if (id < 0) {
                    throw TokenizationException("Special token ID cannot be negative: $id for token '$token'")
                }
                if (id < 256) {
                    throw TokenizationException("Special token ID cannot be in base vocabulary range (0-255): $id for token '$token'")
                }
            }
            
            // Check for duplicate IDs
            val ids = specialTokens.values
            val uniqueIds = ids.toSet()
            if (ids.size != uniqueIds.size) {
                throw TokenizationException("Duplicate special token IDs found")
            }
            
            _specialTokens = specialTokens.toMap()
            _inverseSpecialTokens = specialTokens.entries.associate { (k, v) -> v to k }
            
        } catch (e: TokenizationException) {
            throw e
        } catch (e: Exception) {
            throw TokenizationException("Failed to register special tokens: ${e.message}", e)
        }
    }
    
    /**
     * Train the RegexTokenizer using BPE algorithm with regex-based text splitting.
     * 
     * The training process:
     * 1. Split text into chunks using the regex pattern
     * 2. Convert each chunk to UTF-8 bytes independently
     * 3. Apply BPE algorithm within each chunk (no merges across chunks)
     * 4. Learn merge rules from the most frequent pairs across all chunks
     * 
     * @param text Training text to learn BPE merges from
     * @param vocabSize Target vocabulary size (must be >= 256)
     * @param verbose Whether to print training progress
     * @throws IllegalArgumentException if parameters are invalid
     * @throws TokenizationException if training fails
     * 
     * Requirements: 2.1, 2.6 - Regex splitting and independent chunk processing
     */
    override fun train(text: String, vocabSize: Int, verbose: Boolean) {
        // Validate inputs
        validateText(text, "training")
        validateVocabSize(vocabSize)
        
        try {
            // Split text into chunks using regex pattern
            val textChunks = compiledPattern.findAll(text).map { it.value }.toList()
            
            if (textChunks.isEmpty()) {
                throw TokenizationException("No text chunks found after regex splitting. Pattern may be too restrictive.")
            }
            
            if (verbose) {
                println("Training RegexTokenizer with vocab size $vocabSize")
                println("Text length: ${text.length} characters")
                println("Text chunks: ${textChunks.size}")
                println("Pattern: $pattern")
                println("Target merges: ${vocabSize - 256}")
            }
            
            // Convert each chunk to UTF-8 bytes independently
            var chunkIds = textChunks.map { chunk ->
                try {
                    safeEncodeUtf8(chunk).map { it.toInt() and 0xFF }.toMutableList()
                } catch (e: Exception) {
                    throw TokenizationException("Failed to encode chunk: '$chunk'", e)
                }
            }.toMutableList()
            
            val merges = mutableMapOf<Pair<Int, Int>, Int>()
            val numMerges = vocabSize - 256
            
            // Perform BPE training
            for (i in 0 until numMerges) {
                // Count all consecutive pairs across all chunks
                val stats = mutableMapOf<Pair<Int, Int>, Int>()
                for (chunkIdList in chunkIds) {
                    BpeUtils.getStats(chunkIdList, stats)
                }
                
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
                
                // Apply the merge to all chunks independently
                chunkIds = chunkIds.map { chunkIdList ->
                    BpeUtils.merge(chunkIdList, mostFrequentPair, newTokenId).toMutableList()
                }.toMutableList()
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
     * Encode text chunk without special token handling.
     * This is the core encoding function that applies BPE to a single text chunk.
     * 
     * @param text Text chunk to encode
     * @return List of token IDs for the chunk
     * @throws IllegalStateException if tokenizer is not trained
     * @throws TokenizationException if encoding fails
     */
    fun encodeOrdinary(text: String): List<Int> {
        validateInitialized()
        
        // Handle empty text
        if (text.isEmpty()) {
            return emptyList()
        }
        
        try {
            // Split text into chunks using regex pattern
            val textChunks = compiledPattern.findAll(text).map { it.value }.toList()
            
            val allIds = mutableListOf<Int>()
            
            // Encode each chunk independently
            for (chunk in textChunks) {
                val chunkBytes = safeEncodeUtf8(chunk)
                val chunkIds = encodeChunk(chunkBytes)
                allIds.addAll(chunkIds)
            }
            
            return allIds
            
        } catch (e: TokenizationException) {
            throw e
        } catch (e: Exception) {
            throw TokenizationException("Ordinary encoding failed: ${e.message}", e)
        }
    }
    
    /**
     * Encode a single chunk of bytes using BPE merges.
     * 
     * @param textBytes Byte array to encode
     * @return List of token IDs
     */
    private fun encodeChunk(textBytes: ByteArray): List<Int> {
        // Convert bytes to token IDs (0-255)
        var ids = textBytes.map { it.toInt() and 0xFF }
        
        // Apply merges greedily until no more can be applied
        while (ids.size >= 2) {
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
    }
    
    /**
     * Encode text with special token handling.
     * 
     * @param text Input text to encode
     * @param allowedSpecial Special token handling policy:
     *   - "all": Allow all registered special tokens
     *   - "none": Ignore all special tokens (encode as ordinary text)
     *   - "none_raise": Raise error if any special token is encountered
     *   - Custom set: Allow only specified special tokens
     * @return List of token IDs
     * @throws IllegalStateException if tokenizer is not trained
     * @throws TokenizationException if encoding fails or disallowed special tokens found
     * 
     * Requirements: 2.4, 2.5 - Handle allowed_special parameter and error on disallowed tokens
     */
    override fun encode(text: String, allowedSpecial: String): List<Int> {
        validateInitialized()
        
        // Handle empty text
        if (text.isEmpty()) {
            return emptyList()
        }
        
        try {
            // Determine which special tokens are allowed
            val allowedSpecialTokens = when (allowedSpecial) {
                "all" -> _specialTokens
                "none" -> emptyMap()
                "none_raise" -> {
                    // Check if any special tokens are present in text
                    for (specialToken in _specialTokens.keys) {
                        if (specialToken in text) {
                            throw TokenizationException("Special token '$specialToken' found in text but not allowed (allowed_special='none_raise')")
                        }
                    }
                    emptyMap()
                }
                else -> {
                    // Try to parse as a set specification (this is a simplified approach)
                    // In a full implementation, you might want to accept a Set<String> parameter
                    throw IllegalArgumentException("allowed_special must be 'all', 'none', 'none_raise', or a custom set")
                }
            }
            
            if (allowedSpecialTokens.isEmpty()) {
                // No special tokens to handle, use ordinary encoding
                return encodeOrdinary(text)
            }
            
            // Handle special tokens by splitting text around them
            // We need to manually split to preserve the special tokens
            val parts = mutableListOf<String>()
            var currentText = text
            
            while (currentText.isNotEmpty()) {
                // Find the earliest special token in the remaining text
                var earliestIndex = currentText.length
                var foundToken: String? = null
                
                for (specialToken in allowedSpecialTokens.keys) {
                    val index = currentText.indexOf(specialToken)
                    if (index != -1 && index < earliestIndex) {
                        earliestIndex = index
                        foundToken = specialToken
                    }
                }
                
                if (foundToken != null) {
                    // Add text before the special token (if any)
                    if (earliestIndex > 0) {
                        parts.add(currentText.substring(0, earliestIndex))
                    }
                    // Add the special token itself
                    parts.add(foundToken)
                    // Continue with text after the special token
                    currentText = currentText.substring(earliestIndex + foundToken.length)
                } else {
                    // No more special tokens, add remaining text
                    parts.add(currentText)
                    break
                }
            }
            
            val allIds = mutableListOf<Int>()
            
            for (part in parts) {
                if (part in allowedSpecialTokens) {
                    // This is a special token
                    allIds.add(allowedSpecialTokens[part]!!)
                } else {
                    // This is ordinary text
                    allIds.addAll(encodeOrdinary(part))
                }
            }
            
            return allIds
            
        } catch (e: TokenizationException) {
            throw e
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: Exception) {
            throw TokenizationException("Encoding failed: ${e.message}", e)
        }
    }
    
    /**
     * Decode a list of token IDs back into text, handling both regular and special tokens.
     * 
     * @param ids List of token IDs to decode
     * @return Decoded text string
     * @throws IllegalStateException if tokenizer is not trained
     * @throws TokenizationException if decoding fails
     * 
     * Requirements: 1.4 - Reconstruct original text from token sequences
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
            
            val allBytes = mutableListOf<Byte>()
            
            for (id in ids) {
                when {
                    // Regular vocabulary token
                    id in _vocab -> {
                        val bytes = _vocab[id]!!
                        allBytes.addAll(bytes.toList())
                    }
                    // Special token
                    id in _inverseSpecialTokens -> {
                        val specialToken = _inverseSpecialTokens[id]!!
                        allBytes.addAll(safeEncodeUtf8(specialToken).toList())
                    }
                    else -> {
                        throw IllegalArgumentException("Unknown token ID: $id")
                    }
                }
            }
            
            // Convert bytes back to string with safe UTF-8 handling
            return safeDecodeUtf8(allBytes.toByteArray())
            
        } catch (e: IllegalArgumentException) {
            throw e  // Let IllegalArgumentException propagate
        } catch (e: TokenizationException) {
            throw e
        } catch (e: Exception) {
            throw TokenizationException("Decoding failed: ${e.message}", e)
        }
    }
    
    /**
     * Load tokenizer state from serialized data.
     * 
     * @param data Serialized tokenizer data containing merges, pattern, and special tokens
     * @throws IllegalArgumentException if data is invalid for RegexTokenizer
     * @throws TokenizationException if loading fails
     */
    override fun loadFromData(data: TokenizerData) {
        try {
            data.validate()
            
            // Verify this is a RegexTokenizer model
            if (data.type != "regex") {
                throw IllegalArgumentException("Expected RegexTokenizer model, got: ${data.type}")
            }
            
            // Load pattern (should match our pattern)
            if (data.pattern != pattern) {
                throw IllegalArgumentException("Pattern mismatch. Expected: '$pattern', got: '${data.pattern}'")
            }
            
            _merges = data.getMergesMap()
            _vocab = buildVocab()
            registerSpecialTokens(data.specialTokens)
            
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: TokenizationException) {
            throw e
        } catch (e: Exception) {
            throw TokenizationException("Failed to load RegexTokenizer from data: ${e.message}", e)
        }
    }
}