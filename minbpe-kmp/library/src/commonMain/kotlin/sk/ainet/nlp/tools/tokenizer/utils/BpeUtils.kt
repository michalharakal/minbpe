package sk.ainet.nlp.tools.tokenizer.utils

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
     * 
     * Requirements: 6.1 - Provide function to count consecutive pair frequencies
     */
    fun getStats(ids: List<Int>, counts: MutableMap<Pair<Int, Int>, Int>? = null): Map<Pair<Int, Int>, Int> {
        val result = counts ?: mutableMapOf()
        
        // Iterate through consecutive pairs in the token sequence
        for (i in 0 until ids.size - 1) {
            val pair = Pair(ids[i], ids[i + 1])
            result[pair] = result.getOrElse(pair) { 0 } + 1
        }
        
        return result
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
     * 
     * Requirements: 6.2 - Provide function to merge token pairs
     */
    fun merge(ids: List<Int>, pair: Pair<Int, Int>, idx: Int): List<Int> {
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
    }
}