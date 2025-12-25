package sk.ainet.nlp.tools.tokenizer

import kotlinx.serialization.Serializable

/**
 * Serializable data class representing a tokenizer model.
 * Used for saving and loading tokenizer state to/from files.
 * 
 * The model contains all necessary information to reconstruct a tokenizer:
 * - Version information for compatibility checking
 * - Regex pattern for text splitting (if applicable)
 * - Special tokens with their assigned IDs
 * - Merge rules learned during training
 */
@Serializable
data class TokenizerModel(
    /**
     * Model format version (e.g., "minbpe v1")
     */
    val version: String = "minbpe v1",
    
    /**
     * Regex pattern used for text splitting.
     * Empty string for BasicTokenizer, GPT-4 pattern for RegexTokenizer/GPT4Tokenizer.
     */
    val pattern: String,
    
    /**
     * Special tokens mapping: token_string -> token_id
     */
    val specialTokens: Map<String, Int>,
    
    /**
     * Merge rules as a list of pairs: ((token1, token2), new_token_id)
     * Stored as list to preserve merge order which may be important for some implementations.
     */
    val merges: List<Pair<Pair<Int, Int>, Int>>
) {
    /**
     * Convert merges list back to map for efficient lookup during encoding/decoding.
     * 
     * @return Map of merge rules
     */
    fun getMergesMap(): Map<Pair<Int, Int>, Int> {
        return merges.toMap()
    }
    
    /**
     * Get the number of merge operations in this model.
     * 
     * @return Number of merges
     */
    fun getMergeCount(): Int = merges.size
    
    /**
     * Get the effective vocabulary size (256 base tokens + merges).
     * 
     * @return Total vocabulary size
     */
    fun getVocabSize(): Int = 256 + merges.size
    
    /**
     * Validate that the model is well-formed.
     * 
     * @throws IllegalStateException if model is invalid
     * @throws TokenizationException if model data is corrupted
     */
    fun validate() {
        try {
            // Check version format
            if (!version.startsWith("minbpe v")) {
                throw IllegalStateException("Invalid version format: $version")
            }
            
            // Validate pattern (empty is allowed for BasicTokenizer)
            if (pattern.isNotBlank()) {
                try {
                    Regex(pattern)
                } catch (e: Exception) {
                    throw TokenizationException("Invalid regex pattern in model: $pattern", e)
                }
            }
            
            // Check that merge token IDs are sequential starting from 256
            if (merges.isNotEmpty()) {
                val expectedIds = (256 until 256 + merges.size).toSet()
                val actualIds = merges.map { it.second }.toSet()
                
                if (expectedIds != actualIds) {
                    throw IllegalStateException("Merge token IDs are not sequential starting from 256. Expected: $expectedIds, Got: $actualIds")
                }
            }
            
            // Check that special token IDs don't conflict with merge IDs or base vocabulary
            val mergeIds = merges.map { it.second }.toSet()
            val specialIds = specialTokens.values.toSet()
            val baseVocabIds = (0..255).toSet()
            
            // Check conflicts with merge IDs
            val mergeConflicts = mergeIds.intersect(specialIds)
            if (mergeConflicts.isNotEmpty()) {
                throw IllegalStateException("Special token IDs conflict with merge IDs: $mergeConflicts")
            }
            
            // Check conflicts with base vocabulary
            val baseConflicts = baseVocabIds.intersect(specialIds)
            if (baseConflicts.isNotEmpty()) {
                throw IllegalStateException("Special token IDs conflict with base vocabulary (0-255): $baseConflicts")
            }
            
            // Validate special token strings
            for ((token, id) in specialTokens) {
                if (token.isBlank()) {
                    throw TokenizationException("Special token string cannot be blank")
                }
                if (id < 0) {
                    throw TokenizationException("Special token ID cannot be negative: $id for token '$token'")
                }
            }
            
            // Check that merge pairs reference valid token IDs
            val allValidIds = (0..255).toSet() + mergeIds
            for ((pair, newId) in merges) {
                val (id1, id2) = pair
                if (id1 !in allValidIds || id2 !in allValidIds) {
                    throw IllegalStateException("Merge pair ($id1, $id2) -> $newId references invalid token IDs")
                }
                if (id1 < 0 || id2 < 0) {
                    throw TokenizationException("Merge pair contains negative token IDs: ($id1, $id2)")
                }
                if (newId < 256) {
                    throw TokenizationException("Merge result token ID must be >= 256, got: $newId")
                }
            }
            
        } catch (e: IllegalStateException) {
            throw e
        } catch (e: TokenizationException) {
            throw e
        } catch (e: Exception) {
            throw TokenizationException("Model validation failed: ${e.message}", e)
        }
    }
}

/**
 * Configuration for special tokens.
 * Used for registering special tokens with their IDs and metadata.
 */
@Serializable
data class SpecialTokenConfig(
    /**
     * The special token string (e.g., "<|endoftext|>")
     */
    val token: String,
    
    /**
     * The assigned token ID
     */
    val id: Int,
    
    /**
     * Optional description of the token's purpose
     */
    val description: String? = null
)

/**
 * Exception thrown when tokenization operations fail.
 * 
 * This exception is used for all tokenizer-specific errors including:
 * - Invalid input validation (text, token IDs, patterns)
 * - UTF-8 encoding/decoding errors
 * - Special token handling errors
 * - Training and inference failures
 */
class TokenizationException(message: String, cause: Throwable? = null) : Exception(message, cause)