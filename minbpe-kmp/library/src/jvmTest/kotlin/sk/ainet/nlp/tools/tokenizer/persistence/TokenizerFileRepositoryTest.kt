package sk.ainet.nlp.tools.tokenizer.persistence

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import sk.ainet.nlp.tools.tokenizer.BasicTokenizer
import sk.ainet.nlp.tools.tokenizer.RegexTokenizer
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import java.io.File

class TokenizerFileRepositoryTest {
    
    private val repository = TokenizerFileRepository()
    
    @Test
    fun testSaveAndLoadBasicTokenizer() = runBlocking {
        // Create and train a basic tokenizer
        val originalTokenizer = BasicTokenizer()
        originalTokenizer.train("hello world hello", 258, verbose = false)
        
        // Create temporary directory
        val tempDir = File.createTempFile("test", "").apply { 
            delete()
            mkdirs() 
        }
        
        try {
            val modelFile = File(tempDir, "basic_test.model").absolutePath
            
            // Test saving
            val saveResults = repository.save(originalTokenizer, modelFile).toList()
            val lastSaveResult = saveResults.lastOrNull()
            assertTrue(lastSaveResult is RepositoryResult.Completed)
            assertTrue(repository.exists(modelFile))
            
            // Test loading
            val loadResults = repository.load(modelFile).toList()
            val lastLoadResult = loadResults.lastOrNull()
            assertTrue(lastLoadResult is RepositoryResult.TokenizerLoaded)
            
            val loadedTokenizer = (lastLoadResult as RepositoryResult.TokenizerLoaded).tokenizer
            
            // Verify the loaded tokenizer works the same as the original
            val testText = "hello world"
            val originalEncoded = originalTokenizer.encode(testText)
            val loadedEncoded = loadedTokenizer.encode(testText)
            assertEquals(originalEncoded, loadedEncoded)
            
            val originalDecoded = originalTokenizer.decode(originalEncoded)
            val loadedDecoded = loadedTokenizer.decode(loadedEncoded)
            assertEquals(originalDecoded, loadedDecoded)
            assertEquals(testText, loadedDecoded)
            
        } finally {
            // Clean up
            tempDir.deleteRecursively()
        }
    }
    
    @Test
    fun testSaveAndLoadRegexTokenizer() = runBlocking {
        // Create and train a regex tokenizer
        val originalTokenizer = RegexTokenizer()
        originalTokenizer.train("hello world hello", 258, verbose = false)
        
        // Register some special tokens
        originalTokenizer.registerSpecialTokens(mapOf(
            "<|start|>" to 1000,
            "<|end|>" to 1001
        ))
        
        // Create temporary directory
        val tempDir = File.createTempFile("test", "").apply { 
            delete()
            mkdirs() 
        }
        
        try {
            val jsonFile = File(tempDir, "regex_test.json").absolutePath
            
            // Test saving in JSON format
            val saveResults = repository.saveJson(originalTokenizer, jsonFile).toList()
            val lastSaveResult = saveResults.lastOrNull()
            assertTrue(lastSaveResult is RepositoryResult.Completed)
            assertTrue(repository.exists(jsonFile))
            
            // Test loading
            val loadResults = repository.loadJson(jsonFile).toList()
            val lastLoadResult = loadResults.lastOrNull()
            assertTrue(lastLoadResult is RepositoryResult.TokenizerLoaded)
            
            val loadedTokenizer = (lastLoadResult as RepositoryResult.TokenizerLoaded).tokenizer
            
            // Verify the loaded tokenizer works the same as the original
            val testText = "hello world"
            val originalEncoded = originalTokenizer.encode(testText)
            val loadedEncoded = loadedTokenizer.encode(testText)
            assertEquals(originalEncoded, loadedEncoded)
            
            // Test special tokens
            assertEquals(originalTokenizer.specialTokens, loadedTokenizer.specialTokens)
            
        } finally {
            // Clean up
            tempDir.deleteRecursively()
        }
    }
    
    @Test
    fun testPythonCompatibleFormat() = runBlocking {
        // Create and train a basic tokenizer
        val tokenizer = BasicTokenizer()
        tokenizer.train("hello world hello", 258, verbose = false)
        
        // Create temporary directory
        val tempDir = File.createTempFile("test", "").apply { 
            delete()
            mkdirs() 
        }
        
        try {
            val filePrefix = File(tempDir, "python_test").absolutePath
            
            // Test saving in Python-compatible format
            val saveResults = repository.savePythonCompatible(tokenizer, filePrefix).toList()
            val lastSaveResult = saveResults.lastOrNull()
            assertTrue(lastSaveResult is RepositoryResult.Completed)
            
            // Check that both .model and .vocab files were created
            assertTrue(repository.exists("$filePrefix.model"))
            assertTrue(repository.exists("$filePrefix.vocab"))
            
            // Test loading
            val loadResults = repository.loadPythonCompatible("$filePrefix.model").toList()
            val lastLoadResult = loadResults.lastOrNull()
            assertTrue(lastLoadResult is RepositoryResult.TokenizerLoaded)
            
        } finally {
            // Clean up
            tempDir.deleteRecursively()
        }
    }
    
    @Test
    fun testAutoDetection() = runBlocking {
        // Create and train a basic tokenizer
        val tokenizer = BasicTokenizer()
        tokenizer.train("hello world hello", 258, verbose = false)
        
        // Create temporary directory
        val tempDir = File.createTempFile("test", "").apply { 
            delete()
            mkdirs() 
        }
        
        try {
            val modelFile = File(tempDir, "auto_test.model").absolutePath
            val jsonFile = File(tempDir, "auto_test.json").absolutePath
            
            // Save in both formats
            repository.save(tokenizer, modelFile).toList()
            repository.save(tokenizer, jsonFile).toList()
            
            // Test auto-detection for both formats
            val modelLoadResults = repository.loadAuto(modelFile).toList()
            val modelLastResult = modelLoadResults.lastOrNull()
            assertTrue(modelLastResult is RepositoryResult.TokenizerLoaded)
            
            val jsonLoadResults = repository.loadAuto(jsonFile).toList()
            val jsonLastResult = jsonLoadResults.lastOrNull()
            assertTrue(jsonLastResult is RepositoryResult.TokenizerLoaded)
            
        } finally {
            // Clean up
            tempDir.deleteRecursively()
        }
    }
    
    @Test
    fun testFileInfo() {
        // Test with non-existent file
        val nonExistentInfo = repository.getFileInfo("non_existent_file.model")
        assertEquals(null, nonExistentInfo)
        
        // Create a temporary file
        val tempFile = File.createTempFile("test", ".model")
        tempFile.writeText("test content")
        
        try {
            val fileInfo = repository.getFileInfo(tempFile.absolutePath)
            assertNotNull(fileInfo)
            assertTrue(fileInfo.exists)
            assertTrue(fileInfo.size > 0)
            assertEquals("Python-compatible", fileInfo.format)
        } finally {
            tempFile.delete()
        }
    }
    
    @Test
    fun testDeleteFile() {
        // Create temporary files
        val tempDir = File.createTempFile("test", "").apply { 
            delete()
            mkdirs() 
        }
        
        try {
            val modelFile = File(tempDir, "delete_test.model")
            val vocabFile = File(tempDir, "delete_test.vocab")
            
            modelFile.writeText("model content")
            vocabFile.writeText("vocab content")
            
            assertTrue(repository.exists(modelFile.absolutePath))
            assertTrue(repository.exists(vocabFile.absolutePath))
            
            // Delete should remove both files for Python-compatible format
            assertTrue(repository.delete(modelFile.absolutePath))
            
            assertFalse(repository.exists(modelFile.absolutePath))
            assertFalse(repository.exists(vocabFile.absolutePath))
            
        } finally {
            // Clean up
            tempDir.deleteRecursively()
        }
    }
}