package sk.ainet.tokenizer.comparison.testing

import sk.ainet.tokenizer.comparison.model.*
import java.io.File
import kotlin.random.Random

/**
 * Utility functions for test corpus management and generation.
 */
object TestCorpusUtils {
    
    /**
     * Create test cases from corpus text with specified configuration.
     */
    fun createTestCases(
        corpus: List<String>,
        config: TokenizerConfig,
        category: TestCategory,
        namePrefix: String = "test"
    ): List<TestCase> {
        return corpus.mapIndexed { index, text ->
            TestCase(
                name = "${namePrefix}_${category.name.lowercase()}_$index",
                input = text,
                config = config,
                expectedBehavior = TestBehavior.IDENTICAL_TOKENS,
                category = category
            )
        }
    }
    
    /**
     * Create round-trip test cases for encode/decode validation.
     */
    fun createRoundTripTestCases(
        corpus: List<String>,
        config: TokenizerConfig,
        category: TestCategory
    ): List<TestCase> {
        return corpus.mapIndexed { index, text ->
            TestCase(
                name = "roundtrip_${category.name.lowercase()}_$index",
                input = text,
                config = config,
                expectedBehavior = TestBehavior.IDENTICAL_ROUNDTRIP,
                category = category
            )
        }
    }
    
    /**
     * Create error handling test cases with invalid inputs.
     */
    fun createErrorHandlingTestCases(config: TokenizerConfig): List<TestCase> {
        val invalidInputs = listOf(
            "\u0000", // Null character
            "\uFFFE", // Invalid Unicode
            "\uFFFF", // Invalid Unicode
            String(ByteArray(1000000) { 0 }), // Very large input
        )
        
        return invalidInputs.mapIndexed { index, input ->
            TestCase(
                name = "error_handling_$index",
                input = input,
                config = config,
                expectedBehavior = TestBehavior.ERROR_HANDLING,
                category = TestCategory.EDGE_CASE
            )
        }
    }
    
    /**
     * Filter corpus by text length.
     */
    fun filterByLength(corpus: List<String>, minLength: Int = 0, maxLength: Int = Int.MAX_VALUE): List<String> {
        return corpus.filter { it.length in minLength..maxLength }
    }
    
    /**
     * Filter corpus by character set.
     */
    fun filterByCharacterSet(corpus: List<String>, characterSet: CharacterSet): List<String> {
        return when (characterSet) {
            CharacterSet.ASCII -> corpus.filter { it.all { char -> char.code < 128 } }
            CharacterSet.LATIN -> corpus.filter { it.all { char -> char.code < 256 } }
            CharacterSet.UNICODE -> corpus.filter { it.any { char -> char.code >= 128 } }
            CharacterSet.MIXED -> corpus
        }
    }
    
    /**
     * Sample random subset from corpus.
     */
    fun sampleCorpus(corpus: List<String>, sampleSize: Int, random: Random = Random.Default): List<String> {
        return if (corpus.size <= sampleSize) {
            corpus
        } else {
            corpus.shuffled(random).take(sampleSize)
        }
    }
    
    /**
     * Generate stress test cases with various patterns.
     */
    fun generateStressTestCases(): List<String> {
        return listOf(
            // Repetitive patterns
            "a".repeat(10000),
            "ab".repeat(5000),
            "abc".repeat(3333),
            
            // Alternating patterns
            generateAlternatingPattern("ab", 1000),
            generateAlternatingPattern("123", 1000),
            
            // Random patterns
            generateRandomText(1000, CharacterSet.ASCII),
            generateRandomText(1000, CharacterSet.UNICODE),
            
            // Nested structures
            generateNestedBrackets(100),
            generateNestedQuotes(100),
            
            // Pathological cases
            generateWorstCaseBPE(1000)
        )
    }
    
    /**
     * Validate corpus integrity.
     */
    fun validateCorpus(corpus: List<String>): CorpusValidationResult {
        val issues = mutableListOf<String>()
        var totalSize = 0L
        var emptyCount = 0
        var duplicateCount = 0
        
        val seen = mutableSetOf<String>()
        
        corpus.forEach { text ->
            totalSize += text.length
            
            if (text.isEmpty()) {
                emptyCount++
            }
            
            if (text in seen) {
                duplicateCount++
            } else {
                seen.add(text)
            }
            
            // Check for potential issues
            if (text.length > 100000) {
                issues.add("Very large text found: ${text.length} characters")
            }
            
            if (text.contains('\u0000')) {
                issues.add("Null character found in text")
            }
        }
        
        return CorpusValidationResult(
            totalTexts = corpus.size,
            totalSize = totalSize,
            emptyTexts = emptyCount,
            duplicateTexts = duplicateCount,
            issues = issues
        )
    }
    
    private fun generateAlternatingPattern(pattern: String, length: Int): String {
        return (0 until length).map { pattern[it % pattern.length] }.joinToString("")
    }
    
    private fun generateRandomText(length: Int, characterSet: CharacterSet): String {
        val chars = when (characterSet) {
            CharacterSet.ASCII -> (32..126).map { it.toChar() }
            CharacterSet.LATIN -> (32..255).map { it.toChar() }
            CharacterSet.UNICODE -> (32..126).map { it.toChar() } + 
                                   (0x00A0..0x00FF).map { it.toChar() } +
                                   (0x4E00..0x4E10).map { it.toChar() } // Sample CJK
            CharacterSet.MIXED -> (32..126).map { it.toChar() }
        }
        
        return (1..length).map { chars.random() }.joinToString("")
    }
    
    private fun generateNestedBrackets(depth: Int): String {
        return (1..depth).joinToString("") { "(" } + (1..depth).joinToString("") { ")" }
    }
    
    private fun generateNestedQuotes(depth: Int): String {
        return (1..depth).joinToString("") { "\"" } + "text" + (1..depth).joinToString("") { "\"" }
    }
    
    private fun generateWorstCaseBPE(length: Int): String {
        // Generate text that would be challenging for BPE compression
        // Alternating unique characters to minimize compression opportunities
        return (0 until length).map { ('a'.code + (it % 26)).toChar() }.joinToString("")
    }
}

/**
 * Character set categories for corpus filtering.
 */
enum class CharacterSet {
    ASCII,    // 0-127
    LATIN,    // 0-255
    UNICODE,  // Any Unicode
    MIXED     // Mixed character sets
}

/**
 * Result of corpus validation.
 */
data class CorpusValidationResult(
    val totalTexts: Int,
    val totalSize: Long,
    val emptyTexts: Int,
    val duplicateTexts: Int,
    val issues: List<String>
) {
    val isValid: Boolean get() = issues.isEmpty()
}