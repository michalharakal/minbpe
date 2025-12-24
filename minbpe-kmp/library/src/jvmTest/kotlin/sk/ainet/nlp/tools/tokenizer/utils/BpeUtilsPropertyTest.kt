package sk.ainet.nlp.tools.tokenizer.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.random.Random

/**
 * Property-based tests for BPE utilities using manual property testing.
 * 
 * Feature: kotlin-minbpe-tokenizer
 */
class BpeUtilsPropertyTest {
    
    @Test
    fun testProperty13_PairFrequencyCountingProperty() {
        // **Feature: kotlin-minbpe-tokenizer, Property 13: Pair frequency counting**
        // **Validates: Requirements 6.1**
        
        // Test with multiple random inputs
        repeat(100) {
            val ids = generateRandomTokenSequence()
            val stats = BpeUtils.getStats(ids)
            
            // Manually count pairs to verify correctness
            val expectedStats = mutableMapOf<Pair<Int, Int>, Int>()
            for (i in 0 until ids.size - 1) {
                val pair = Pair(ids[i], ids[i + 1])
                expectedStats[pair] = expectedStats.getOrElse(pair) { 0 } + 1
            }
            
            // The stats should match our manual count
            assertEquals(expectedStats, stats)
            
            // Additional invariants
            if (ids.size <= 1) {
                assertTrue(stats.isEmpty())
            } else {
                // Total count should equal number of consecutive pairs
                assertEquals(ids.size - 1, stats.values.sum())
            }
        }
    }
    
    @Test
    fun testProperty14_MergeOperationCorrectnessProperty() {
        // **Feature: kotlin-minbpe-tokenizer, Property 14: Merge operation correctness**
        // **Validates: Requirements 6.2**
        
        // Test with multiple random inputs
        repeat(100) {
            val ids = generateRandomTokenSequence()
            val first = Random.nextInt(0, 100)
            val second = Random.nextInt(0, 100)
            val newId = Random.nextInt(200, 300)
            val pair = Pair(first, second)
            
            val result = BpeUtils.merge(ids, pair, newId)
            
            // Count occurrences of the pair in original sequence
            var pairCount = 0
            for (i in 0 until ids.size - 1) {
                if (ids[i] == first && ids[i + 1] == second) {
                    pairCount++
                }
            }
            
            // Count occurrences of newId in result
            val newIdCount = result.count { it == newId }
            
            // The number of newId tokens should equal the number of pairs that were merged
            assertEquals(pairCount, newIdCount)
            
            // If no pairs existed, result should be identical to input
            if (pairCount == 0) {
                assertEquals(ids, result)
            }
            
            // Result should never contain the original pair (if it was merged)
            if (pairCount > 0) {
                for (i in 0 until result.size - 1) {
                    val resultPair = Pair(result[i], result[i + 1])
                    // The original pair should not appear in the result
                    assertTrue(resultPair != pair)
                }
            }
            
            // The result should be shorter by exactly the number of merges performed
            assertEquals(ids.size - pairCount, result.size)
        }
    }
    
    private fun generateRandomTokenSequence(): List<Int> {
        val size = Random.nextInt(0, 21) // 0 to 20 tokens
        return (0 until size).map { Random.nextInt(0, 101) } // tokens 0-100
    }
}