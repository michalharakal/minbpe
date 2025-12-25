package sk.ainet.nlp.tools.tokenizer.integration

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import sk.ainet.nlp.tools.tokenizer.BasicTokenizer
import sk.ainet.nlp.tools.tokenizer.RegexTokenizer
import sk.ainet.nlp.tools.tokenizer.persistence.TokenizerFileRepository
import sk.ainet.nlp.tools.tokenizer.persistence.RepositoryResult
import sk.ainet.nlp.tools.tokenizer.utils.FileUtils
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import java.io.File

/**
 * Integration test demonstrating the complete file operations workflow
 * for cross-platform tokenizer persistence.
 */
class FileOperationsIntegrationTest {
    
    @Test
    fun testCompleteWorkflow() = runBlocking {
        // Create temporary directory for test files
        val tempDir = File.createTempFile("integration_test", "").apply { 
            delete()
            mkdirs() 
        }
        
        try {
            // Step 1: Create and train tokenizers
            val basicTokenizer = BasicTokenizer()
            basicTokenizer.train("hello world hello world test", 260, verbose = false)
            
            val regexTokenizer = RegexTokenizer()
            regexTokenizer.train("hello world hello world test", 260, verbose = false)
            regexTokenizer.registerSpecialTokens(mapOf(
                "<|start|>" to 2000,
                "<|end|>" to 2001
            ))
            
            // Step 2: Test FileUtils direct operations
            val basicModelFile = File(tempDir, "basic.model").absolutePath
            val basicJsonFile = File(tempDir, "basic.json").absolutePath
            val regexJsonFile = File(tempDir, "regex.json").absolutePath
            
            // Save using FileUtils
            val basicSaveResults = FileUtils.save(basicTokenizer, basicModelFile).toList()
            assertTrue(basicSaveResults.any { it.javaClass.simpleName.contains("Completed") })
            assertTrue(FileUtils.exists(basicModelFile))
            
            val regexSaveResults = FileUtils.save(regexTokenizer, regexJsonFile).toList()
            assertTrue(regexSaveResults.any { it.javaClass.simpleName.contains("Completed") })
            assertTrue(FileUtils.exists(regexJsonFile))
            
            // Step 3: Test TokenizerFileRepository operations
            val repository = TokenizerFileRepository()
            
            // Save basic tokenizer using repository
            val repoSaveResults = repository.save(basicTokenizer, basicJsonFile).toList()
            val lastSaveResult = repoSaveResults.lastOrNull()
            assertTrue(lastSaveResult is RepositoryResult.Completed)
            
            // Step 4: Test loading and round-trip consistency
            val loadResults = repository.load(basicJsonFile).toList()
            val lastLoadResult = loadResults.lastOrNull()
            assertTrue(lastLoadResult is RepositoryResult.TokenizerLoaded)
            
            val loadedTokenizer = (lastLoadResult as RepositoryResult.TokenizerLoaded).tokenizer
            
            // Step 5: Verify round-trip consistency
            val testTexts = listOf(
                "hello world",
                "test tokenization",
                "hello world test hello"
            )
            
            for (testText in testTexts) {
                val originalEncoded = basicTokenizer.encode(testText)
                val loadedEncoded = loadedTokenizer.encode(testText)
                assertEquals(originalEncoded, loadedEncoded, "Encoding mismatch for: $testText")
                
                val originalDecoded = basicTokenizer.decode(originalEncoded)
                val loadedDecoded = loadedTokenizer.decode(loadedEncoded)
                assertEquals(originalDecoded, loadedDecoded, "Decoding mismatch for: $testText")
                assertEquals(testText, loadedDecoded, "Round-trip failed for: $testText")
            }
            
            // Step 6: Test file information and management
            val fileInfo = repository.getFileInfo(basicJsonFile)
            assertTrue(fileInfo != null)
            assertTrue(fileInfo.exists)
            assertTrue(fileInfo.size > 0)
            assertEquals("JSON", fileInfo.format)
            
            // Step 7: Test auto-detection
            val autoLoadResults = repository.loadAuto(basicModelFile).toList()
            val autoLastResult = autoLoadResults.lastOrNull()
            assertTrue(autoLastResult is RepositoryResult.TokenizerLoaded)
            
            // Step 8: Test Python-compatible format
            val pythonPrefix = File(tempDir, "python_test").absolutePath
            val pythonSaveResults = repository.savePythonCompatible(basicTokenizer, pythonPrefix).toList()
            val pythonLastSave = pythonSaveResults.lastOrNull()
            assertTrue(pythonLastSave is RepositoryResult.Completed)
            
            assertTrue(repository.exists("$pythonPrefix.model"))
            assertTrue(repository.exists("$pythonPrefix.vocab"))
            
            // Step 9: Test cleanup
            assertTrue(repository.delete("$pythonPrefix.model"))
            assertTrue(FileUtils.delete(basicJsonFile))
            assertTrue(FileUtils.delete(regexJsonFile))
            
            println("✅ All file operations integration tests passed!")
            
        } finally {
            // Clean up
            tempDir.deleteRecursively()
        }
    }
    
    @Test
    fun testErrorHandling() = runBlocking {
        val repository = TokenizerFileRepository()
        
        // Test loading non-existent file
        val loadResults = repository.load("non_existent_file.model").toList()
        val lastResult = loadResults.lastOrNull()
        assertTrue(lastResult is RepositoryResult.Error)
        
        // Test file info for non-existent file
        val fileInfo = repository.getFileInfo("non_existent_file.model")
        assertEquals(null, fileInfo)
        
        // Test delete non-existent file
        val deleteResult = repository.delete("non_existent_file.model")
        assertEquals(false, deleteResult)
        
        println("✅ Error handling tests passed!")
    }
    
    @Test
    fun testCrossPlatformPathHandling() {
        // Test path normalization
        val paths = listOf(
            "test/path/file.model",
            "test\\path\\file.model",
            "./test/file.model",
            "../test/file.model"
        )
        
        for (path in paths) {
            val normalized = FileUtils.normalizePath(path)
            assertTrue(normalized.isNotEmpty(), "Path normalization failed for: $path")
        }
        
        println("✅ Cross-platform path handling tests passed!")
    }
}