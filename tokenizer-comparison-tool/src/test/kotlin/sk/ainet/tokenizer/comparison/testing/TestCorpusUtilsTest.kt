package sk.ainet.tokenizer.comparison.testing

import sk.ainet.tokenizer.comparison.model.*
import kotlin.test.*
import kotlin.random.Random

class TestCorpusUtilsTest {
    
    private val sampleCorpus = listOf(
        "Hello world",
        "This is a test",
        "Unicode: 你好",
        "Special chars: !@#$%",
        "Long text: " + "a".repeat(1000)
    )
    
    private val sampleConfig = TokenizerConfig(
        type = TokenizerType.BASIC,
        vocabSize = 256,
        merges = emptyList()
    )
    
    @Test
    fun testCreateTestCases() {
        val testCases = TestCorpusUtils.createTestCases(
            sampleCorpus,
            sampleConfig,
            TestCategory.BASIC,
            "sample"
        )
        
        assertEquals(sampleCorpus.size, testCases.size)
        
        testCases.forEachIndexed { index, testCase ->
            assertEquals("sample_basic_$index", testCase.name)
            assertEquals(sampleCorpus[index], testCase.input)
            assertEquals(sampleConfig, testCase.config)
            assertEquals(TestBehavior.IDENTICAL_TOKENS, testCase.expectedBehavior)
            assertEquals(TestCategory.BASIC, testCase.category)
        }
    }
    
    @Test
    fun testCreateRoundTripTestCases() {
        val testCases = TestCorpusUtils.createRoundTripTestCases(
            sampleCorpus,
            sampleConfig,
            TestCategory.UNICODE
        )
        
        assertEquals(sampleCorpus.size, testCases.size)
        
        testCases.forEachIndexed { index, testCase ->
            assertEquals("roundtrip_unicode_$index", testCase.name)
            assertEquals(sampleCorpus[index], testCase.input)
            assertEquals(TestBehavior.IDENTICAL_ROUNDTRIP, testCase.expectedBehavior)
            assertEquals(TestCategory.UNICODE, testCase.category)
        }
    }
    
    @Test
    fun testCreateErrorHandlingTestCases() {
        val testCases = TestCorpusUtils.createErrorHandlingTestCases(sampleConfig)
        
        assertTrue(testCases.isNotEmpty())
        
        testCases.forEach { testCase ->
            assertTrue(testCase.name.startsWith("error_handling_"))
            assertEquals(TestBehavior.ERROR_HANDLING, testCase.expectedBehavior)
            assertEquals(TestCategory.EDGE_CASE, testCase.category)
            assertEquals(sampleConfig, testCase.config)
        }
    }
    
    @Test
    fun testFilterByLength() {
        val shortTexts = TestCorpusUtils.filterByLength(sampleCorpus, maxLength = 20)
        assertTrue(shortTexts.all { it.length <= 20 })
        
        val longTexts = TestCorpusUtils.filterByLength(sampleCorpus, minLength = 100)
        assertTrue(longTexts.all { it.length >= 100 })
        
        val mediumTexts = TestCorpusUtils.filterByLength(sampleCorpus, minLength = 10, maxLength = 50)
        assertTrue(mediumTexts.all { it.length in 10..50 })
    }
    
    @Test
    fun testFilterByCharacterSet() {
        val testCorpus = listOf(
            "ASCII only text",
            "Latin with ñáéíóú",
            "Unicode with 你好世界",
            "Mixed ASCII and 中文"
        )
        
        val asciiTexts = TestCorpusUtils.filterByCharacterSet(testCorpus, CharacterSet.ASCII)
        assertEquals(1, asciiTexts.size)
        assertEquals("ASCII only text", asciiTexts[0])
        
        val latinTexts = TestCorpusUtils.filterByCharacterSet(testCorpus, CharacterSet.LATIN)
        assertTrue(latinTexts.size >= 2) // ASCII and Latin texts
        
        val unicodeTexts = TestCorpusUtils.filterByCharacterSet(testCorpus, CharacterSet.UNICODE)
        assertTrue(unicodeTexts.any { it.contains("你好") })
        
        val mixedTexts = TestCorpusUtils.filterByCharacterSet(testCorpus, CharacterSet.MIXED)
        assertEquals(testCorpus.size, mixedTexts.size)
    }
    
    @Test
    fun testSampleCorpus() {
        val sampleSize = 3
        val sampled = TestCorpusUtils.sampleCorpus(sampleCorpus, sampleSize, Random(42))
        
        assertEquals(sampleSize, sampled.size)
        assertTrue(sampled.all { it in sampleCorpus })
        
        // Test with sample size larger than corpus
        val largeSample = TestCorpusUtils.sampleCorpus(sampleCorpus, 100)
        assertEquals(sampleCorpus.size, largeSample.size)
    }
    
    @Test
    fun testGenerateStressTestCases() {
        val stressTests = TestCorpusUtils.generateStressTestCases()
        
        assertTrue(stressTests.isNotEmpty())
        
        // Verify we have repetitive patterns
        assertTrue(stressTests.any { it.startsWith("aaaa") })
        
        // Verify we have long texts
        assertTrue(stressTests.any { it.length > 500 })
        
        // Verify we have nested structures
        assertTrue(stressTests.any { it.contains("((((") })
    }
    
    @Test
    fun testValidateCorpus() {
        val testCorpus = listOf(
            "Normal text",
            "", // Empty text
            "Normal text", // Duplicate
            "Text with null\u0000char",
            "Very long text: " + "x".repeat(200000) // Very large text
        )
        
        val result = TestCorpusUtils.validateCorpus(testCorpus)
        
        assertEquals(5, result.totalTexts)
        assertEquals(1, result.emptyTexts)
        assertEquals(1, result.duplicateTexts)
        assertFalse(result.isValid) // Should have issues
        assertTrue(result.issues.any { it.contains("Very large text") })
        assertTrue(result.issues.any { it.contains("Null character") })
    }
    
    @Test
    fun testValidateCorpusWithValidData() {
        val validCorpus = listOf(
            "Text 1",
            "Text 2",
            "Text 3"
        )
        
        val result = TestCorpusUtils.validateCorpus(validCorpus)
        
        assertEquals(3, result.totalTexts)
        assertEquals(0, result.emptyTexts)
        assertEquals(0, result.duplicateTexts)
        assertTrue(result.isValid)
        assertTrue(result.issues.isEmpty())
    }
    
    @Test
    fun testCharacterSetEnum() {
        // Test that all character set values are defined
        val values = CharacterSet.values()
        assertTrue(values.contains(CharacterSet.ASCII))
        assertTrue(values.contains(CharacterSet.LATIN))
        assertTrue(values.contains(CharacterSet.UNICODE))
        assertTrue(values.contains(CharacterSet.MIXED))
    }
    
    @Test
    fun testCorpusValidationResultProperties() {
        val result = CorpusValidationResult(
            totalTexts = 10,
            totalSize = 1000L,
            emptyTexts = 1,
            duplicateTexts = 2,
            issues = listOf("Issue 1", "Issue 2")
        )
        
        assertEquals(10, result.totalTexts)
        assertEquals(1000L, result.totalSize)
        assertEquals(1, result.emptyTexts)
        assertEquals(2, result.duplicateTexts)
        assertEquals(2, result.issues.size)
        assertFalse(result.isValid) // Should be false when there are issues
        
        val validResult = result.copy(issues = emptyList())
        assertTrue(validResult.isValid) // Should be true when no issues
    }
}