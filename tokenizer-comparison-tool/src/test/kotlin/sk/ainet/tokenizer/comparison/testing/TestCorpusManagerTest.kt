package sk.ainet.tokenizer.comparison.testing

import sk.ainet.tokenizer.comparison.model.*
import kotlin.test.*
import java.io.File
import java.io.IOException

class TestCorpusManagerTest {
    
    private lateinit var corpusManager: TestCorpusManager
    
    @BeforeTest
    fun setUp() {
        corpusManager = TestCorpusManager()
    }
    
    @Test
    fun testGetBasicCorpus() {
        val basicCorpus = corpusManager.getBasicCorpus()
        assertNotNull(basicCorpus)
        assertTrue(basicCorpus is List<String>)
        // Basic corpus should be accessible even if empty during testing
    }
    
    @Test
    fun testGetUnicodeCorpus() {
        val unicodeCorpus = corpusManager.getUnicodeCorpus()
        assertNotNull(unicodeCorpus)
        assertTrue(unicodeCorpus is List<String>)
    }
    
    @Test
    fun testGetEdgeCases() {
        val edgeCases = corpusManager.getEdgeCases()
        assertNotNull(edgeCases)
        assertTrue(edgeCases.isNotEmpty(), "Edge cases should include synthetic cases even if no files are loaded")
        
        // Verify synthetic edge cases are included
        assertTrue(edgeCases.contains(""), "Should contain empty string")
        assertTrue(edgeCases.contains(" "), "Should contain single space")
        assertTrue(edgeCases.any { it.contains("🌍") }, "Should contain emoji")
    }
    
    @Test
    fun testGetReferenceTests() {
        val referenceTests = corpusManager.getReferenceTests()
        assertNotNull(referenceTests)
        assertTrue(referenceTests.isNotEmpty(), "Should have at least some built-in reference tests")
        
        // Verify structure of reference tests
        referenceTests.forEach { test ->
            assertNotNull(test.name)
            assertNotNull(test.input)
            assertNotNull(test.tokenizerType)
            assertTrue(test.vocabSize > 0)
        }
    }
    
    @Test
    fun testGetAllCorpora() {
        val allCorpora = corpusManager.getAllCorpora()
        assertNotNull(allCorpora)
        
        val basicCorpus = corpusManager.getBasicCorpus()
        val unicodeCorpus = corpusManager.getUnicodeCorpus()
        val edgeCases = corpusManager.getEdgeCases()
        
        val expectedSize = basicCorpus.size + unicodeCorpus.size + edgeCases.size
        assertEquals(expectedSize, allCorpora.size)
    }
    
    @Test
    fun testGetTestCasesByCategory() {
        // Test basic category
        val basicCases = corpusManager.getTestCasesByCategory(TestCategory.BASIC)
        assertEquals(corpusManager.getBasicCorpus(), basicCases)
        
        // Test unicode category
        val unicodeCases = corpusManager.getTestCasesByCategory(TestCategory.UNICODE)
        assertEquals(corpusManager.getUnicodeCorpus(), unicodeCases)
        
        // Test edge case category
        val edgeCases = corpusManager.getTestCasesByCategory(TestCategory.EDGE_CASE)
        assertEquals(corpusManager.getEdgeCases(), edgeCases)
        
        // Test reference category
        val referenceCases = corpusManager.getTestCasesByCategory(TestCategory.REFERENCE)
        assertEquals(corpusManager.getReferenceTests().map { it.input }, referenceCases)
        
        // Test performance category (should be large texts)
        val performanceCases = corpusManager.getTestCasesByCategory(TestCategory.PERFORMANCE)
        performanceCases.forEach { text ->
            assertTrue(text.length > 1000, "Performance test cases should be large")
        }
    }
    
    @Test
    fun testGetTestCasesByComplexity() {
        val allCorpora = corpusManager.getAllCorpora()
        
        // Test simple complexity
        val simpleCases = corpusManager.getTestCasesByComplexity(ComplexityLevel.SIMPLE)
        simpleCases.forEach { text ->
            assertTrue(text.length < 100, "Simple cases should be less than 100 characters")
        }
        
        // Test medium complexity
        val mediumCases = corpusManager.getTestCasesByComplexity(ComplexityLevel.MEDIUM)
        mediumCases.forEach { text ->
            assertTrue(text.length in 100..1000, "Medium cases should be 100-1000 characters")
        }
        
        // Test complex complexity
        val complexCases = corpusManager.getTestCasesByComplexity(ComplexityLevel.COMPLEX)
        complexCases.forEach { text ->
            assertTrue(text.length > 1000, "Complex cases should be over 1000 characters")
        }
    }
    
    @Test
    fun testGetTestCasesByLanguage() {
        // Test English language filtering
        val englishCases = corpusManager.getTestCasesByLanguage(LanguageType.ENGLISH)
        englishCases.forEach { text ->
            assertTrue(text.matches(Regex("[a-zA-Z0-9\\s\\p{Punct}]*")), 
                "English cases should only contain ASCII characters")
        }
        
        // Test Unicode language filtering
        val unicodeCases = corpusManager.getTestCasesByLanguage(LanguageType.UNICODE)
        assertEquals(corpusManager.getUnicodeCorpus(), unicodeCases)
        
        // Test mixed language
        val mixedCases = corpusManager.getTestCasesByLanguage(LanguageType.MIXED)
        assertEquals(corpusManager.getAllCorpora(), mixedCases)
    }
    
    @Test
    fun testGetWikipediaBPEExample() {
        val wikipediaExample = corpusManager.getWikipediaBPEExample()
        assertNotNull(wikipediaExample)
        assertTrue(wikipediaExample.isNotEmpty())
        assertTrue(wikipediaExample.contains("Byte pair encoding"))
        assertTrue(wikipediaExample.contains("BPE"))
    }
    
    @Test
    fun testLoadExternalCorpusWithNonExistentFile() {
        val result = corpusManager.loadExternalCorpus("non_existent_file.txt")
        assertTrue(result.isEmpty(), "Should return empty list for non-existent file")
    }
    
    @Test
    fun testLoadExternalCorpusWithValidFile() {
        // Create a temporary test file
        val tempFile = File.createTempFile("test_corpus", ".txt")
        tempFile.writeText("Test content for external corpus loading")
        
        try {
            val result = corpusManager.loadExternalCorpus(tempFile.absolutePath)
            assertEquals(1, result.size)
            assertEquals("Test content for external corpus loading", result[0])
        } finally {
            tempFile.delete()
        }
    }
    
    @Test
    fun testLoadExternalCorpusWithDirectory() {
        // Create a temporary directory with test files
        val tempDir = File.createTempFile("test_corpus", "").apply {
            delete()
            mkdir()
        }
        
        try {
            val file1 = File(tempDir, "test1.txt").apply { writeText("Content 1") }
            val file2 = File(tempDir, "test2.txt").apply { writeText("Content 2") }
            val file3 = File(tempDir, "test3.md").apply { writeText("Content 3") }
            val file4 = File(tempDir, "ignore.log").apply { writeText("Should be ignored") }
            
            val result = corpusManager.loadExternalCorpus(tempDir.absolutePath)
            assertEquals(3, result.size, "Should load .txt and .md files only")
            assertTrue(result.contains("Content 1"))
            assertTrue(result.contains("Content 2"))
            assertTrue(result.contains("Content 3"))
            assertFalse(result.contains("Should be ignored"))
        } finally {
            tempDir.deleteRecursively()
        }
    }
    
    @Test
    fun testReferenceTestStructure() {
        val referenceTests = corpusManager.getReferenceTests()
        
        // Verify we have the expected reference tests
        val testNames = referenceTests.map { it.name }
        assertTrue(testNames.contains("basic_hello_world"))
        assertTrue(testNames.contains("unicode_mixed"))
        assertTrue(testNames.contains("wikipedia_bpe_sample"))
        
        // Verify hello world test
        val helloWorldTest = referenceTests.find { it.name == "basic_hello_world" }
        assertNotNull(helloWorldTest)
        assertEquals("Hello, world!", helloWorldTest.input)
        assertEquals(TokenizerType.BASIC, helloWorldTest.tokenizerType)
        assertEquals(256, helloWorldTest.vocabSize)
        assertTrue(helloWorldTest.expectedTokens.isNotEmpty())
    }
}