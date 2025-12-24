package sk.ainet.nlp.tools.tokenizer.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BpeUtilsTest {
    
    @Test
    fun testGetStats() {
        // Test basic pair counting
        val ids = listOf(1, 2, 3, 2, 1)
        val stats = BpeUtils.getStats(ids)
        
        assertEquals(1, stats[Pair(1, 2)])
        assertEquals(1, stats[Pair(2, 3)])
        assertEquals(1, stats[Pair(3, 2)])
        assertEquals(1, stats[Pair(2, 1)])
        assertEquals(4, stats.size)
    }
    
    @Test
    fun testGetStatsEmpty() {
        val ids = emptyList<Int>()
        val stats = BpeUtils.getStats(ids)
        assertTrue(stats.isEmpty())
    }
    
    @Test
    fun testGetStatsSingle() {
        val ids = listOf(42)
        val stats = BpeUtils.getStats(ids)
        assertTrue(stats.isEmpty())
    }
    
    @Test
    fun testMerge() {
        // Test basic merge operation
        val ids = listOf(1, 2, 3, 2, 1, 2)
        val result = BpeUtils.merge(ids, Pair(1, 2), 100)
        
        assertEquals(listOf(100, 3, 2, 100), result)
    }
    
    @Test
    fun testMergeNoMatch() {
        val ids = listOf(1, 3, 5, 7)
        val result = BpeUtils.merge(ids, Pair(2, 4), 100)
        
        assertEquals(ids, result)
    }
    
    @Test
    fun testMergeEmpty() {
        val ids = emptyList<Int>()
        val result = BpeUtils.merge(ids, Pair(1, 2), 100)
        
        assertTrue(result.isEmpty())
    }
}