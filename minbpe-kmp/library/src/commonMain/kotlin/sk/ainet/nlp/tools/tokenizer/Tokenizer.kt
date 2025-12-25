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
     * @throws IllegalArgumentException if vocabulary size is invalid
     */
    protected fun validateVocabSize(vocabSize: Int) {
        if (vocabSize < 256) {
            throw IllegalArgumentException("Vocabulary size must be at least 256, got $vocabSize")
        }
        if (vocabSize > 1_000_000) {
            throw IllegalArgumentException("Vocabulary size too large (max 1,000,000), got $vocabSize")
        }
    }
    
    /**
     * Validate that token IDs are within acceptable ranges.
     * 
     * @param tokenIds List of token IDs to validate
     * @throws TokenizationException if any token ID is invalid
     */
    protected fun validateTokenIds(tokenIds: List<Int>) {
        for (tokenId in tokenIds) {
            if (tokenId < 0) {
                throw TokenizationException("Token ID cannot be negative: $tokenId")
            }
            if (tokenId > 2_000_000) {
                throw TokenizationException("Token ID too large (max 2,000,000): $tokenId")
            }
        }
    }
    
    /**
     * Validate that a regex pattern is well-formed.
     * 
     * @param pattern Regex pattern to validate
     * @throws TokenizationException if pattern is invalid
     */
    protected fun validatePattern(pattern: String) {
        if (pattern.isBlank()) {
            return // Empty pattern is valid for BasicTokenizer
        }
        
        try {
            Regex(pattern)
        } catch (e: Exception) {
            throw TokenizationException("Invalid regex pattern: $pattern", e)
        }
    }
    
    /**
     * Safely decode UTF-8 bytes to string, handling invalid sequences gracefully.
     * 
     * @param bytes Byte array to decode
     * @return Decoded string with replacement characters for invalid UTF-8
     */
    protected fun safeDecodeUtf8(bytes: ByteArray): String {
        return try {
            bytes.decodeToString()
        } catch (e: Exception) {
            // Handle invalid UTF-8 sequences by using replacement characters
            val result = StringBuilder()
            var i = 0
            while (i < bytes.size) {
                try {
                    // Try to decode a single character
                    val char = bytes.sliceArray(i..minOf(i + 3, bytes.size - 1)).decodeToString()
                    if (char.isNotEmpty()) {
                        result.append(char)
                        i += char.encodeToByteArray().size
                    } else {
                        result.append('\uFFFD') // Unicode replacement character
                        i++
                    }
                } catch (e: Exception) {
                    result.append('\uFFFD') // Unicode replacement character
                    i++
                }
            }
            result.toString()
        }
    }
    
    /**
     * Safely encode string to UTF-8 bytes, handling encoding errors gracefully.
     * 
     * @param text String to encode
     * @return UTF-8 byte array
     * @throws TokenizationException if text cannot be encoded
     */
    protected fun safeEncodeUtf8(text: String): ByteArray {
        return try {
            text.encodeToByteArray()
        } catch (e: Exception) {
            throw TokenizationException("Failed to encode text as UTF-8: ${e.message}", e)
        }
    }
    
    /**
     * Validate that text is not null for operations that require content.
     * Empty text is allowed and will be handled gracefully.
     * 
     * @param text Text to validate
     * @param operation Name of the operation for error messages
     * @throws TokenizationException if text is invalid
     */
    protected fun validateText(text: String?, operation: String) {
        if (text == null) {
            throw TokenizationException("Text cannot be null for $operation")
        }
        // Note: Empty text is allowed and will be handled gracefully by each operation
    }
    
    /**
     * Validate that the tokenizer is properly initialized before use.
     * 
     * @throws IllegalStateException if tokenizer is not initialized
     */
    protected fun validateInitialized() {
        if (vocab.isEmpty()) {
            throw IllegalStateException("Tokenizer must be trained or loaded before use")
        }
    }
    
    /**
     * Validate special token configuration.
     * 
     * @param specialTokens Map of special tokens to validate
     * @throws TokenizationException if special tokens are invalid
     */
    protected fun validateSpecialTokens(specialTokens: Map<String, Int>) {
        try {
            for ((token, id) in specialTokens) {
                if (token.isBlank()) {
                    throw TokenizationException("Special token string cannot be blank")
                }
                if (id < 0) {
                    throw TokenizationException("Special token ID cannot be negative: $id for token '$token'")
                }
                if (id < 256) {
                    throw TokenizationException("Special token ID cannot be in base vocabulary range (0-255): $id for token '$token'")
                }
                if (id > 2_000_000) {
                    throw TokenizationException("Special token ID too large (max 2,000,000): $id for token '$token'")
                }
            }
            
            // Check for duplicate IDs
            val ids = specialTokens.values.toList()
            val uniqueIds = ids.toSet()
            if (ids.size != uniqueIds.size) {
                val duplicates = ids.groupBy { it }.filter { it.value.size > 1 }.keys
                throw TokenizationException("Duplicate special token IDs found: $duplicates")
            }
            
            // Check for duplicate tokens
            val tokens = specialTokens.keys.toList()
            val uniqueTokens = tokens.toSet()
            if (tokens.size != uniqueTokens.size) {
                val duplicates = tokens.groupBy { it }.filter { it.value.size > 1 }.keys
                throw TokenizationException("Duplicate special token strings found: $duplicates")
            }
            
        } catch (e: TokenizationException) {
            throw e
        } catch (e: Exception) {
            throw TokenizationException("Special token validation failed: ${e.message}", e)
        }
    }
    
    /**
     * Validate merge operations for consistency.
     * 
     * @param merges Map of merge operations to validate
     * @throws TokenizationException if merges are invalid
     */
    protected fun validateMerges(merges: Map<Pair<Int, Int>, Int>) {
        try {
            for ((pair, newId) in merges) {
                val (id1, id2) = pair
                
                // Validate individual token IDs
                if (id1 < 0 || id2 < 0) {
                    throw TokenizationException("Merge pair contains negative token IDs: ($id1, $id2)")
                }
                if (id1 > 2_000_000 || id2 > 2_000_000) {
                    throw TokenizationException("Merge pair token IDs too large (max 2,000,000): ($id1, $id2)")
                }
                
                // Validate new token ID
                if (newId < 256) {
                    throw TokenizationException("Merge result token ID must be >= 256, got: $newId for pair ($id1, $id2)")
                }
                if (newId > 2_000_000) {
                    throw TokenizationException("Merge result token ID too large (max 2,000,000): $newId")
                }
            }
            
            // Check that merge IDs are sequential starting from 256
            if (merges.isNotEmpty()) {
                val mergeIds = merges.values.sorted()
                val expectedIds = (256 until 256 + merges.size).toList()
                if (mergeIds != expectedIds) {
                    throw TokenizationException("Merge token IDs are not sequential starting from 256. Expected: $expectedIds, Got: $mergeIds")
                }
            }
            
        } catch (e: TokenizationException) {
            throw e
        } catch (e: Exception) {
            throw TokenizationException("Merge validation failed: ${e.message}", e)
        }
    }
    
    /**
     * Validate that a vocabulary size is reasonable for the current system.
     * 
     * @param vocabSize Requested vocabulary size
     * @param maxRecommended Maximum recommended size for performance
     * @throws IllegalArgumentException if vocabulary size is invalid
     */
    protected fun validateVocabSizeWithWarning(vocabSize: Int, maxRecommended: Int = 100_000) {
        validateVocabSize(vocabSize)
        
        if (vocabSize > maxRecommended) {
            // This is a warning, not an error - large vocabularies are allowed but may be slow
            println("Warning: Large vocabulary size ($vocabSize) may impact performance. Consider using a smaller size for better performance.")
        }
    }
    
    /**
     * Validate input parameters for allowed_special parameter.
     * 
     * @param allowedSpecial The allowed_special parameter value
     * @param availableSpecialTokens Set of available special tokens
     * @throws IllegalArgumentException if parameter is invalid
     */
    protected fun validateAllowedSpecialParameter(allowedSpecial: String, availableSpecialTokens: Set<String>) {
        when (allowedSpecial) {
            "all", "none", "none_raise" -> {
                // Valid standard values
            }
            else -> {
                // Should be a comma-separated list of special tokens
                if (allowedSpecial.isBlank()) {
                    throw IllegalArgumentException("allowed_special parameter cannot be blank")
                }
                
                val requestedTokens = allowedSpecial.split(",").map { it.trim() }.toSet()
                val unknownTokens = requestedTokens - availableSpecialTokens
                
                if (unknownTokens.isNotEmpty()) {
                    throw IllegalArgumentException("Unknown special tokens in allowed_special: $unknownTokens. Available: $availableSpecialTokens")
                }
            }
        }
    }
}