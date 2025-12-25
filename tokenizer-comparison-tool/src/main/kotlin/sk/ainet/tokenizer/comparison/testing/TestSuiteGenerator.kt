package sk.ainet.tokenizer.comparison.testing

import sk.ainet.tokenizer.comparison.model.*

/**
 * Generates comprehensive test suites using the TestCorpusManager.
 */
class TestSuiteGenerator(private val corpusManager: TestCorpusManager) {
    
    /**
     * Generate a complete test suite for all tokenizer types.
     */
    fun generateCompleteTestSuite(): TestSuite {
        val testCases = mutableListOf<TestCase>()
        
        // Basic tokenizer tests
        testCases.addAll(generateBasicTokenizerTests())
        
        // Regex tokenizer tests
        testCases.addAll(generateRegexTokenizerTests())
        
        // GPT-4 tokenizer tests
        testCases.addAll(generateGPT4TokenizerTests())
        
        // Edge case tests
        testCases.addAll(generateEdgeCaseTests())
        
        // Round-trip tests
        testCases.addAll(generateRoundTripTests())
        
        // Reference tests
        testCases.addAll(generateReferenceTests())
        
        return TestSuite(
            name = "complete_tokenizer_test_suite",
            testCases = testCases,
            metadata = mapOf(
                "generated_at" to System.currentTimeMillis().toString(),
                "total_cases" to testCases.size.toString(),
                "categories" to TestCategory.values().joinToString(",") { it.name }
            )
        )
    }
    
    /**
     * Generate test suite for specific tokenizer type.
     */
    fun generateTestSuiteForTokenizer(tokenizerType: TokenizerType, vocabSize: Int = 512): TestSuite {
        val config = createTokenizerConfig(tokenizerType, vocabSize)
        val testCases = mutableListOf<TestCase>()
        
        // Add basic tests
        testCases.addAll(TestCorpusUtils.createTestCases(
            corpusManager.getBasicCorpus(),
            config,
            TestCategory.BASIC,
            tokenizerType.name.lowercase()
        ))
        
        // Add Unicode tests
        testCases.addAll(TestCorpusUtils.createTestCases(
            corpusManager.getUnicodeCorpus(),
            config,
            TestCategory.UNICODE,
            tokenizerType.name.lowercase()
        ))
        
        // Add edge case tests
        testCases.addAll(TestCorpusUtils.createTestCases(
            corpusManager.getEdgeCases(),
            config,
            TestCategory.EDGE_CASE,
            tokenizerType.name.lowercase()
        ))
        
        // Add round-trip tests
        testCases.addAll(TestCorpusUtils.createRoundTripTestCases(
            corpusManager.getAllCorpora(),
            config,
            TestCategory.BASIC
        ))
        
        return TestSuite(
            name = "${tokenizerType.name.lowercase()}_test_suite",
            testCases = testCases,
            metadata = mapOf(
                "tokenizer_type" to tokenizerType.name,
                "vocab_size" to vocabSize.toString(),
                "test_count" to testCases.size.toString()
            )
        )
    }
    
    /**
     * Generate performance test suite with large texts.
     */
    fun generatePerformanceTestSuite(): TestSuite {
        val testCases = mutableListOf<TestCase>()
        val performanceCorpus = corpusManager.getTestCasesByComplexity(ComplexityLevel.COMPLEX)
        
        TokenizerType.values().forEach { tokenizerType ->
            listOf(256, 512, 1024, 2048).forEach { vocabSize ->
                val config = createTokenizerConfig(tokenizerType, vocabSize)
                
                testCases.addAll(TestCorpusUtils.createTestCases(
                    performanceCorpus,
                    config,
                    TestCategory.PERFORMANCE,
                    "perf_${tokenizerType.name.lowercase()}_${vocabSize}"
                ))
            }
        }
        
        return TestSuite(
            name = "performance_test_suite",
            testCases = testCases,
            metadata = mapOf(
                "focus" to "performance",
                "vocab_sizes" to "256,512,1024,2048",
                "test_count" to testCases.size.toString()
            )
        )
    }
    
    /**
     * Generate compatibility test suite for cross-platform validation.
     */
    fun generateCompatibilityTestSuite(): TestSuite {
        val testCases = mutableListOf<TestCase>()
        
        // Test all combinations of tokenizer types and vocab sizes
        TokenizerType.values().forEach { tokenizerType ->
            listOf(256, 512, 1024).forEach { vocabSize ->
                val config = createTokenizerConfig(tokenizerType, vocabSize)
                
                // Add representative samples from each category
                testCases.addAll(TestCorpusUtils.createTestCases(
                    TestCorpusUtils.sampleCorpus(corpusManager.getBasicCorpus(), 5),
                    config,
                    TestCategory.BASIC,
                    "compat_${tokenizerType.name.lowercase()}_${vocabSize}"
                ))
                
                testCases.addAll(TestCorpusUtils.createTestCases(
                    TestCorpusUtils.sampleCorpus(corpusManager.getUnicodeCorpus(), 3),
                    config,
                    TestCategory.UNICODE,
                    "compat_${tokenizerType.name.lowercase()}_${vocabSize}"
                ))
                
                testCases.addAll(TestCorpusUtils.createTestCases(
                    TestCorpusUtils.sampleCorpus(corpusManager.getEdgeCases(), 10),
                    config,
                    TestCategory.EDGE_CASE,
                    "compat_${tokenizerType.name.lowercase()}_${vocabSize}"
                ))
            }
        }
        
        return TestSuite(
            name = "compatibility_test_suite",
            testCases = testCases,
            metadata = mapOf(
                "focus" to "cross_platform_compatibility",
                "sampling" to "representative",
                "test_count" to testCases.size.toString()
            )
        )
    }
    
    private fun generateBasicTokenizerTests(): List<TestCase> {
        val config = createTokenizerConfig(TokenizerType.BASIC, 512)
        return TestCorpusUtils.createTestCases(
            corpusManager.getBasicCorpus(),
            config,
            TestCategory.BASIC,
            "basic"
        )
    }
    
    private fun generateRegexTokenizerTests(): List<TestCase> {
        val config = createTokenizerConfig(TokenizerType.REGEX, 512)
        return TestCorpusUtils.createTestCases(
            corpusManager.getBasicCorpus() + corpusManager.getUnicodeCorpus(),
            config,
            TestCategory.BASIC,
            "regex"
        )
    }
    
    private fun generateGPT4TokenizerTests(): List<TestCase> {
        val config = createTokenizerConfig(TokenizerType.GPT4, 1024)
        return TestCorpusUtils.createTestCases(
            corpusManager.getAllCorpora(),
            config,
            TestCategory.BASIC,
            "gpt4"
        )
    }
    
    private fun generateEdgeCaseTests(): List<TestCase> {
        val config = createTokenizerConfig(TokenizerType.BASIC, 256)
        return TestCorpusUtils.createTestCases(
            corpusManager.getEdgeCases(),
            config,
            TestCategory.EDGE_CASE,
            "edge"
        ) + TestCorpusUtils.createErrorHandlingTestCases(config)
    }
    
    private fun generateRoundTripTests(): List<TestCase> {
        val testCases = mutableListOf<TestCase>()
        
        TokenizerType.values().forEach { tokenizerType ->
            val config = createTokenizerConfig(tokenizerType, 512)
            testCases.addAll(TestCorpusUtils.createRoundTripTestCases(
                TestCorpusUtils.sampleCorpus(corpusManager.getAllCorpora(), 10),
                config,
                TestCategory.BASIC
            ))
        }
        
        return testCases
    }
    
    private fun generateReferenceTests(): List<TestCase> {
        return corpusManager.getReferenceTests().map { referenceTest ->
            TestCase(
                name = "reference_${referenceTest.name}",
                input = referenceTest.input,
                config = createTokenizerConfig(referenceTest.tokenizerType, referenceTest.vocabSize),
                expectedBehavior = TestBehavior.IDENTICAL_TOKENS,
                category = TestCategory.REFERENCE
            )
        }
    }
    
    private fun createTokenizerConfig(type: TokenizerType, vocabSize: Int): TokenizerConfig {
        return TokenizerConfig(
            type = type,
            vocabSize = vocabSize,
            merges = emptyList(), // Will be populated during training
            specialTokens = when (type) {
                TokenizerType.GPT4 -> mapOf(
                    "<|endoftext|>" to vocabSize,
                    "<|startoftext|>" to vocabSize + 1
                )
                else -> emptyMap()
            },
            pattern = when (type) {
                TokenizerType.REGEX -> "'s|'t|'re|'ve|'m|'ll|'d| ?\\p{L}+| ?\\p{N}+| ?[^\\s\\p{L}\\p{N}]+|\\s+(?!\\S)|\\s+"
                TokenizerType.GPT4 -> "'s|'t|'re|'ve|'m|'ll|'d| ?\\p{L}+| ?\\p{N}+| ?[^\\s\\p{L}\\p{N}]+|\\s+(?!\\S)|\\s+"
                else -> ""
            },
            metadata = mapOf(
                "created_at" to System.currentTimeMillis().toString(),
                "generator" to "TestSuiteGenerator"
            )
        )
    }
}

/**
 * Test suite containing multiple test cases.
 */
data class TestSuite(
    val name: String,
    val testCases: List<TestCase>,
    val metadata: Map<String, String> = emptyMap()
) {
    val size: Int get() = testCases.size
    
    fun getTestCasesByCategory(category: TestCategory): List<TestCase> {
        return testCases.filter { it.category == category }
    }
    
    fun getTestCasesByBehavior(behavior: TestBehavior): List<TestCase> {
        return testCases.filter { it.expectedBehavior == behavior }
    }
}