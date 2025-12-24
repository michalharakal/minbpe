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
     */
    fun validate() {
        // Check version format
        if (!version.startsWith("minbpe v")) {
            throw IllegalStateException("Invalid version format: $version")
        }
        
        // Check that merge token IDs are sequential starting from 256
        val expectedIds = (256 until 256 + merges.size).toSet()
        val actualIds = merges.map { it.second }.toSet()
        
        if (expectedIds != actualIds) {
            throw IllegalStateException("Merge token IDs are not sequential starting from 256")
        }
        
        // Check that special token IDs don't conflict with merge IDs
        val mergeIds = merges.map { it.second }.toSet()
        val specialIds = specialTokens.values.toSet()
        val conflicts = mergeIds.intersect(specialIds)
        
        if (conflicts.isNotEmpty()) {
            throw IllegalStateException("Special token IDs conflict with merge IDs: $conflicts")
        }
        
        // Check that merge pairs reference valid token IDs
        val allValidIds = (0..255).toSet() + mergeIds
        for ((pair, _) in merges) {
            val (id1, id2) = pair
            if (id1 !in allValidIds || id2 !in allValidIds) {
                throw IllegalStateException("Merge pair ($id1, $id2) references invalid token IDs")
            }
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
 */
class TokenizationException(message: String, cause: Throwable? = null) : Exception(message, cause)