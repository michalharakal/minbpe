package sk.ainet.nlp.tools.tokenizer.utils

import sk.ainet.nlp.tools.tokenizer.TokenizationException

/**
 * Core utilities for Byte Pair Encoding (BPE) operations.
 * 
 * This object provides the fundamental algorithms needed for BPE tokenization:
 * - Counting consecutive pair frequencies in token sequences
 * - Merging all occurrences of a token pair with a new token ID
 */
object BpeUtils {
    
    /**
     * Count consecutive pair frequencies in a token sequence.
     * 
     * This function analyzes a sequence of token IDs and counts how many times
     * each consecutive pair appears. This is the core operation used during
     * BPE training to identify which pairs should be merged next.
     * 
     * @param ids List of token IDs to analyze
     * @param counts Optional mutable map to accumulate counts into (for efficiency)
     * @return Map from token pairs to their frequency counts
     * @throws TokenizationException if input is invalid
     * 
     * Requirements: 6.1 - Provide function to count consecutive pair frequencies
     */
    fun getStats(ids: List<Int>, counts: MutableMap<Pair<Int, Int>, Int>? = null): Map<Pair<Int, Int>, Int> {
        try {
            // Validate input
            if (ids.any { it < 0 }) {
                throw TokenizationException("Token IDs cannot be negative: ${ids.filter { it < 0 }}")
            }
            if (ids.any { it > 2_000_000 }) {
                val largeIds = ids.filter { it > 2_000_000 }
                throw TokenizationException("Token IDs too large (max 2,000,000): $largeIds")
            }
            
            val result = counts ?: mutableMapOf()
            
            // Handle empty or single-token sequences
            if (ids.size < 2) {
                return result
            }
            
            // Iterate through consecutive pairs in the token sequence
            for (i in 0 until ids.size - 1) {
                val pair = Pair(ids[i], ids[i + 1])
                result[pair] = result.getOrElse(pair) { 0 } + 1
            }
            
            return result
            
        } catch (e: TokenizationException) {
            throw e
        } catch (e: Exception) {
            throw TokenizationException("Failed to compute pair statistics: ${e.message}", e)
        }
    }
    
    /**
     * Merge all occurrences of a token pair with a new token ID.
     * 
     * This function replaces every occurrence of a specific consecutive token pair
     * with a single new token ID. This is the core merge operation used during
     * both BPE training and encoding.
     * 
     * @param ids List of token IDs to process
     * @param pair The consecutive token pair to replace
     * @param idx The new token ID to replace the pair with
     * @return New list with all occurrences of the pair replaced
     * @throws TokenizationException if input is invalid
     * 
     * Requirements: 6.2 - Provide function to merge token pairs
     */
    fun merge(ids: List<Int>, pair: Pair<Int, Int>, idx: Int): List<Int> {
        try {
            // Validate inputs
            if (ids.any { it < 0 }) {
                throw TokenizationException("Token IDs cannot be negative: ${ids.filter { it < 0 }}")
            }
            if (ids.any { it > 2_000_000 }) {
                val largeIds = ids.filter { it > 2_000_000 }
                throw TokenizationException("Token IDs too large (max 2,000,000): $largeIds")
            }
            if (pair.first < 0 || pair.second < 0) {
                throw TokenizationException("Merge pair cannot contain negative token IDs: $pair")
            }
            if (pair.first > 2_000_000 || pair.second > 2_000_000) {
                throw TokenizationException("Merge pair token IDs too large (max 2,000,000): $pair")
            }
            if (idx < 0) {
                throw TokenizationException("New token ID cannot be negative: $idx")
            }
            if (idx > 2_000_000) {
                throw TokenizationException("New token ID too large (max 2,000,000): $idx")
            }
            
            // Handle empty or single-token sequences
            if (ids.size < 2) {
                return ids
            }
            
            val result = mutableListOf<Int>()
            var i = 0
            
            while (i < ids.size) {
                // Check if we have a matching pair at the current position
                if (i < ids.size - 1 && ids[i] == pair.first && ids[i + 1] == pair.second) {
                    // Replace the pair with the new token ID
                    result.add(idx)
                    i += 2  // Skip both tokens in the pair
                } else {
                    // Keep the current token as-is
                    result.add(ids[i])
                    i += 1
                }
            }
            
            return result
            
        } catch (e: TokenizationException) {
            throw e
        } catch (e: Exception) {
            throw TokenizationException("Failed to merge token pair $pair -> $idx: ${e.message}", e)
        }
    }
}