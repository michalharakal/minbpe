package sk.ainet.tokenizer.comparison.testing

import sk.ainet.tokenizer.comparison.model.*
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.random.Random

/**
 * Manages test corpus data for tokenizer comparison testing.
 * Provides access to embedded test resources, synthetic edge cases, and external corpus loading.
 */
class TestCorpusManager {
    
    companion object {
        private const val TEST_CORPUS_BASE_PATH = "test-corpus"
        private const val BASIC_CORPUS_PATH = "$TEST_CORPUS_BASE_PATH/basic"
        private const val UNICODE_CORPUS_PATH = "$TEST_CORPUS_BASE_PATH/unicode"
        private const val EDGE_CASES_PATH = "$TEST_CORPUS_BASE_PATH/edge-cases"
        private const val REFERENCE_PATH = "$TEST_CORPUS_BASE_PATH/reference"
    }
    
    private val _basicCorpus: List<String> by lazy { loadCorpusFromDirectory(BASIC_CORPUS_PATH) }
    private val _unicodeCorpus: List<String> by lazy { loadCorpusFromDirectory(UNICODE_CORPUS_PATH) }
    private val _edgeCases: List<String> by lazy { loadCorpusFromDirectory(EDGE_CASES_PATH) + generateSyntheticEdgeCases() }
    private val _referenceTests: List<ReferenceTest> by lazy { loadReferenceTests() }
    
    /**
     * Get basic corpus for simple tokenization testing.
     */
    fun getBasicCorpus(): List<String> = _basicCorpus
    
    /**
     * Get Unicode corpus for testing various character encodings.
     */
    fun getUnicodeCorpus(): List<String> = _unicodeCorpus
    
    /**
     * Get edge cases including both loaded and synthetic cases.
     */
    fun getEdgeCases(): List<String> = _edgeCases
    
    /**
     * Get reference test cases with expected outputs.
     */
    fun getReferenceTests(): List<ReferenceTest> = _referenceTests
    
    /**
     * Get all available corpora combined.
     */
    fun getAllCorpora(): List<String> = _basicCorpus + _unicodeCorpus + _edgeCases
    
    /**
     * Load external corpus from file path.
     */
    fun loadExternalCorpus(filePath: String): List<String> {
        return try {
            val file = File(filePath)
            if (file.exists() && file.isFile) {
                listOf(file.readText())
            } else if (file.exists() && file.isDirectory) {
                file.listFiles()?.filter { it.isFile && it.extension in listOf("txt", "md") }
                    ?.map { it.readText() } ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            println("Warning: Failed to load external corpus from $filePath: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Get test cases categorized by complexity and language.
     */
    fun getTestCasesByCategory(category: TestCategory): List<String> {
        return when (category) {
            TestCategory.BASIC -> _basicCorpus
            TestCategory.UNICODE -> _unicodeCorpus
            TestCategory.EDGE_CASE -> _edgeCases
            TestCategory.REFERENCE -> _referenceTests.map { it.input }
            TestCategory.PERFORMANCE -> getAllCorpora().filter { it.length > 1000 }
        }
    }
    
    /**
     * Get test cases by complexity level.
     */
    fun getTestCasesByComplexity(complexity: ComplexityLevel): List<String> {
        return when (complexity) {
            ComplexityLevel.SIMPLE -> _basicCorpus.filter { it.length < 100 }
            ComplexityLevel.MEDIUM -> getAllCorpora().filter { it.length in 100..1000 }
            ComplexityLevel.COMPLEX -> getAllCorpora().filter { it.length > 1000 }
        }
    }
    
    /**
     * Get test cases by language type.
     */
    fun getTestCasesByLanguage(language: LanguageType): List<String> {
        return when (language) {
            LanguageType.ENGLISH -> _basicCorpus + _edgeCases.filter { it.matches(Regex("[a-zA-Z0-9\\s\\p{Punct}]*")) }
            LanguageType.UNICODE -> _unicodeCorpus
            LanguageType.MIXED -> getAllCorpora()
        }
    }
    
    /**
     * Create Wikipedia BPE example test case.
     */
    fun getWikipediaBPEExample(): String {
        return """
            Byte pair encoding (BPE) is a simple form of data compression in which the most common pair of consecutive bytes of data is replaced with a byte that does not occur within that data. A table of the replacements is required to rebuild the original data. The algorithm was first described publicly by Philip Gage in a February 1994 article "A New Algorithm for Data Compression" in the C Users Journal.
            
            BPE is particularly effective for natural language processing tasks where it helps to handle out-of-vocabulary words by breaking them down into subword units. This approach has been widely adopted in modern language models and tokenization systems.
        """.trimIndent()
    }
    
    private fun loadCorpusFromDirectory(directoryPath: String): List<String> {
        return try {
            val resourcePath = this::class.java.classLoader.getResource(directoryPath)
            if (resourcePath != null) {
                // Load from resources (when running from JAR)
                loadFromResources(directoryPath)
            } else {
                // Load from file system (during development)
                loadFromFileSystem(directoryPath)
            }
        } catch (e: Exception) {
            println("Warning: Failed to load corpus from $directoryPath: ${e.message}")
            emptyList()
        }
    }
    
    private fun loadFromResources(directoryPath: String): List<String> {
        // For now, return empty list as resource loading requires more complex setup
        // This would be implemented with proper resource handling in a production system
        return emptyList()
    }
    
    private fun loadFromFileSystem(directoryPath: String): List<String> {
        val directory = File(directoryPath)
        if (!directory.exists() || !directory.isDirectory) {
            return emptyList()
        }
        
        return directory.listFiles()?.filter { 
            it.isFile && it.extension in listOf("txt", "md") 
        }?.mapNotNull { file ->
            try {
                file.readText().takeIf { it.isNotBlank() }
            } catch (e: Exception) {
                println("Warning: Failed to read file ${file.name}: ${e.message}")
                null
            }
        } ?: emptyList()
    }
    
    private fun generateSyntheticEdgeCases(): List<String> {
        return listOf(
            // Empty and whitespace cases
            "",
            " ",
            "\n",
            "\t",
            "\r",
            "\n\t\r",
            "   \t\n   ",
            
            // Single character cases
            "a",
            "1",
            "!",
            "🌍",
            "你",
            
            // Repeated character cases
            "a".repeat(100),
            "aa".repeat(50),
            "abc".repeat(33),
            " ".repeat(50),
            
            // Special character combinations
            "!@#$%^&*()",
            "[]{}()<>",
            "\"'`~",
            "\\n\\t\\r",
            
            // Unicode edge cases
            generateRandomUnicode(100),
            generateControlCharacters(),
            generateSpecialTokenPatterns(),
            
            // Mixed content
            "Hello 世界 🌍 123 !@#",
            "a\nb\tc\rd",
            "Normal text with\nnewlines\tand\ttabs",
            
            // Large text case
            "This is a longer text that should test the tokenizer's ability to handle substantial input. ".repeat(20),
            
            // Number sequences
            "0123456789",
            "1 2 3 4 5 6 7 8 9 0",
            "1.23 4.56 7.89",
            
            // URL and email patterns
            "https://example.com/path?param=value",
            "user@example.com",
            "file:///path/to/file.txt"
        )
    }
    
    private fun generateRandomUnicode(length: Int): String {
        val unicodeRanges = listOf(
            0x0080..0x00FF, // Latin-1 Supplement
            0x0100..0x017F, // Latin Extended-A
            0x0370..0x03FF, // Greek and Coptic
            0x0400..0x04FF, // Cyrillic
            0x4E00..0x9FFF, // CJK Unified Ideographs (sample range)
            0x1F600..0x1F64F // Emoticons
        )
        
        return (1..length).map {
            val range = unicodeRanges.random()
            range.random().toChar()
        }.joinToString("")
    }
    
    private fun generateControlCharacters(): String {
        return (0..31).map { it.toChar() }.joinToString("")
    }
    
    private fun generateSpecialTokenPatterns(): String {
        return listOf(
            "<|endoftext|>",
            "<|startoftext|>",
            "<pad>",
            "<unk>",
            "<mask>",
            "[CLS]",
            "[SEP]",
            "[MASK]",
            "[PAD]"
        ).joinToString(" ")
    }
    
    private fun loadReferenceTests(): List<ReferenceTest> {
        // For now, create some basic reference tests
        // In a full implementation, these would be loaded from reference files
        return listOf(
            ReferenceTest(
                name = "basic_hello_world",
                input = "Hello, world!",
                expectedTokens = listOf(72, 101, 108, 108, 111, 44, 32, 119, 111, 114, 108, 100, 33),
                tokenizerType = TokenizerType.BASIC,
                vocabSize = 256
            ),
            ReferenceTest(
                name = "unicode_mixed",
                input = "Hello 世界",
                expectedTokens = listOf(72, 101, 108, 108, 111, 32, 228, 184, 150, 231, 149, 140),
                tokenizerType = TokenizerType.BASIC,
                vocabSize = 256
            ),
            ReferenceTest(
                name = "wikipedia_bpe_sample",
                input = getWikipediaBPEExample(),
                expectedTokens = emptyList(), // Would be populated with actual expected tokens
                tokenizerType = TokenizerType.REGEX,
                vocabSize = 512
            )
        )
    }
}

/**
 * Reference test case with expected outputs.
 */
data class ReferenceTest(
    val name: String,
    val input: String,
    val expectedTokens: List<Int>,
    val tokenizerType: TokenizerType,
    val vocabSize: Int,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Complexity levels for test categorization.
 */
enum class ComplexityLevel {
    SIMPLE,   // < 100 characters
    MEDIUM,   // 100-1000 characters
    COMPLEX   // > 1000 characters
}

/**
 * Language types for test categorization.
 */
enum class LanguageType {
    ENGLISH,  // ASCII/Latin characters only
    UNICODE,  // Non-Latin characters
    MIXED     // Mixed character sets
}