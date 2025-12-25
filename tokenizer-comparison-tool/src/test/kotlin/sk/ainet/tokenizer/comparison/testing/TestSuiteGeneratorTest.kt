package sk.ainet.tokenizer.comparison.testing

import sk.ainet.tokenizer.comparison.model.*
import kotlin.test.*

class TestSuiteGeneratorTest {
    
    private lateinit var corpusManager: TestCorpusManager
    private lateinit var generator: TestSuiteGenerator
    
    @BeforeTest
    fun setUp() {
        corpusManager = TestCorpusManager()
        generator = TestSuiteGenerator(corpusManager)
    }
    
    @Test
    fun testGenerateCompleteTestSuite() {
        val testSuite = generator.generateCompleteTestSuite()
        
        assertNotNull(testSuite)
        assertEquals("complete_tokenizer_test_suite", testSuite.name)
        assertTrue(testSuite.testCases.isNotEmpty())
        
        // Verify metadata
        assertTrue(testSuite.metadata.containsKey("generated_at"))
        assertTrue(testSuite.metadata.containsKey("total_cases"))
        assertTrue(testSuite.metadata.containsKey("categories"))
        
        // Verify all categories are represented
        val categories = testSuite.testCases.map { it.category }.toSet()
        assertTrue(categories.contains(TestCategory.BASIC))
        assertTrue(categories.contains(TestCategory.EDGE_CASE))
        
        // Verify all tokenizer types are tested
        val tokenizerTypes = testSuite.testCases.map { it.config.type }.toSet()
        assertTrue(tokenizerTypes.contains(TokenizerType.BASIC))
        assertTrue(tokenizerTypes.contains(TokenizerType.REGEX))
        assertTrue(tokenizerTypes.contains(TokenizerType.GPT4))
    }
    
    @Test
    fun testGenerateTestSuiteForTokenizer() {
        val testSuite = generator.generateTestSuiteForTokenizer(TokenizerType.BASIC, 512)
        
        assertNotNull(testSuite)
        assertEquals("basic_test_suite", testSuite.name)
        assertTrue(testSuite.testCases.isNotEmpty())
        
        // Verify all test cases use the specified tokenizer type
        assertTrue(testSuite.testCases.all { it.config.type == TokenizerType.BASIC })
        assertTrue(testSuite.testCases.all { it.config.vocabSize == 512 })
        
        // Verify metadata
        assertEquals("BASIC", testSuite.metadata["tokenizer_type"])
        assertEquals("512", testSuite.metadata["vocab_size"])
        assertEquals(testSuite.testCases.size.toString(), testSuite.metadata["test_count"])
        
        // Verify different categories are included
        val categories = testSuite.testCases.map { it.category }.toSet()
        assertTrue(categories.contains(TestCategory.BASIC))
        assertTrue(categories.contains(TestCategory.UNICODE))
        assertTrue(categories.contains(TestCategory.EDGE_CASE))
    }
    
    @Test
    fun testGeneratePerformanceTestSuite() {
        val testSuite = generator.generatePerformanceTestSuite()
        
        assertNotNull(testSuite)
        assertEquals("performance_test_suite", testSuite.name)
        
        // Verify metadata
        assertEquals("performance", testSuite.metadata["focus"])
        assertEquals("256,512,1024,2048", testSuite.metadata["vocab_sizes"])
        
        // Verify all test cases are performance category
        assertTrue(testSuite.testCases.all { it.category == TestCategory.PERFORMANCE })
        
        // Verify different vocab sizes are tested
        val vocabSizes = testSuite.testCases.map { it.config.vocabSize }.toSet()
        assertTrue(vocabSizes.contains(256))
        assertTrue(vocabSizes.contains(512))
        assertTrue(vocabSizes.contains(1024))
        assertTrue(vocabSizes.contains(2048))
        
        // Verify all tokenizer types are tested
        val tokenizerTypes = testSuite.testCases.map { it.config.type }.toSet()
        assertEquals(TokenizerType.values().toSet(), tokenizerTypes)
    }
    
    @Test
    fun testGenerateCompatibilityTestSuite() {
        val testSuite = generator.generateCompatibilityTestSuite()
        
        assertNotNull(testSuite)
        assertEquals("compatibility_test_suite", testSuite.name)
        assertTrue(testSuite.testCases.isNotEmpty())
        
        // Verify metadata
        assertEquals("cross_platform_compatibility", testSuite.metadata["focus"])
        assertEquals("representative", testSuite.metadata["sampling"])
        
        // Verify all tokenizer types are tested
        val tokenizerTypes = testSuite.testCases.map { it.config.type }.toSet()
        assertEquals(TokenizerType.values().toSet(), tokenizerTypes)
        
        // Verify different vocab sizes are tested
        val vocabSizes = testSuite.testCases.map { it.config.vocabSize }.toSet()
        assertTrue(vocabSizes.contains(256))
        assertTrue(vocabSizes.contains(512))
        assertTrue(vocabSizes.contains(1024))
        
        // Verify different categories are represented
        val categories = testSuite.testCases.map { it.category }.toSet()
        assertTrue(categories.contains(TestCategory.BASIC))
        assertTrue(categories.contains(TestCategory.UNICODE))
        assertTrue(categories.contains(TestCategory.EDGE_CASE))
    }
    
    @Test
    fun testTestSuiteGetTestCasesByCategory() {
        val testSuite = generator.generateCompleteTestSuite()
        
        val basicCases = testSuite.getTestCasesByCategory(TestCategory.BASIC)
        assertTrue(basicCases.all { it.category == TestCategory.BASIC })
        
        val edgeCases = testSuite.getTestCasesByCategory(TestCategory.EDGE_CASE)
        assertTrue(edgeCases.all { it.category == TestCategory.EDGE_CASE })
        
        val referenceCases = testSuite.getTestCasesByCategory(TestCategory.REFERENCE)
        assertTrue(referenceCases.all { it.category == TestCategory.REFERENCE })
    }
    
    @Test
    fun testTestSuiteGetTestCasesByBehavior() {
        val testSuite = generator.generateCompleteTestSuite()
        
        val identicalTokensCases = testSuite.getTestCasesByBehavior(TestBehavior.IDENTICAL_TOKENS)
        assertTrue(identicalTokensCases.all { it.expectedBehavior == TestBehavior.IDENTICAL_TOKENS })
        
        val roundTripCases = testSuite.getTestCasesByBehavior(TestBehavior.IDENTICAL_ROUNDTRIP)
        assertTrue(roundTripCases.all { it.expectedBehavior == TestBehavior.IDENTICAL_ROUNDTRIP })
        
        val errorHandlingCases = testSuite.getTestCasesByBehavior(TestBehavior.ERROR_HANDLING)
        assertTrue(errorHandlingCases.all { it.expectedBehavior == TestBehavior.ERROR_HANDLING })
    }
    
    @Test
    fun testTokenizerConfigGeneration() {
        // Test basic tokenizer config
        val basicSuite = generator.generateTestSuiteForTokenizer(TokenizerType.BASIC, 256)
        val basicConfig = basicSuite.testCases.first().config
        
        assertEquals(TokenizerType.BASIC, basicConfig.type)
        assertEquals(256, basicConfig.vocabSize)
        assertTrue(basicConfig.specialTokens.isEmpty())
        assertEquals("", basicConfig.pattern)
        
        // Test GPT-4 tokenizer config
        val gpt4Suite = generator.generateTestSuiteForTokenizer(TokenizerType.GPT4, 1024)
        val gpt4Config = gpt4Suite.testCases.first().config
        
        assertEquals(TokenizerType.GPT4, gpt4Config.type)
        assertEquals(1024, gpt4Config.vocabSize)
        assertTrue(gpt4Config.specialTokens.isNotEmpty())
        assertTrue(gpt4Config.specialTokens.containsKey("<|endoftext|>"))
        assertTrue(gpt4Config.specialTokens.containsKey("<|startoftext|>"))
        assertTrue(gpt4Config.pattern.isNotEmpty())
        
        // Test regex tokenizer config
        val regexSuite = generator.generateTestSuiteForTokenizer(TokenizerType.REGEX, 512)
        val regexConfig = regexSuite.testCases.first().config
        
        assertEquals(TokenizerType.REGEX, regexConfig.type)
        assertEquals(512, regexConfig.vocabSize)
        assertTrue(regexConfig.pattern.isNotEmpty())
    }
    
    @Test
    fun testTestSuiteSize() {
        val testSuite = generator.generateCompleteTestSuite()
        assertEquals(testSuite.testCases.size, testSuite.size)
    }
    
    @Test
    fun testTestSuiteMetadata() {
        val testSuite = generator.generateTestSuiteForTokenizer(TokenizerType.BASIC, 256)
        
        // Verify all configs have metadata
        testSuite.testCases.forEach { testCase ->
            assertTrue(testCase.config.metadata.containsKey("created_at"))
            assertEquals("TestSuiteGenerator", testCase.config.metadata["generator"])
        }
    }
    
    @Test
    fun testEmptyCorpusHandling() {
        // Create a corpus manager that might have empty corpora
        val emptyCorpusManager = TestCorpusManager()
        val emptyGenerator = TestSuiteGenerator(emptyCorpusManager)
        
        // Should still generate test suite even with limited corpus
        val testSuite = emptyGenerator.generateCompleteTestSuite()
        assertNotNull(testSuite)
        
        // Should at least have edge cases and reference tests
        assertTrue(testSuite.testCases.isNotEmpty())
    }
}