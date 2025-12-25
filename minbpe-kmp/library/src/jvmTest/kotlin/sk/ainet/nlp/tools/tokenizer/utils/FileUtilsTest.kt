package sk.ainet.nlp.tools.tokenizer.utils

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import sk.ainet.nlp.tools.tokenizer.BasicTokenizer
import sk.ainet.nlp.tools.tokenizer.persistence.SaveProgress
import sk.ainet.nlp.tools.tokenizer.persistence.LoadResult
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import java.io.File

class FileUtilsTest {
    
    @Test
    fun testFileExists() {
        // Test with non-existent file
        assertFalse(FileUtils.exists("non_existent_file.txt"))
        
        // Create a temporary file
        val tempFile = File.createTempFile("test", ".txt")
        tempFile.writeText("test content")
        
        try {
            assertTrue(FileUtils.exists(tempFile.absolutePath))
        } finally {
            tempFile.delete()
        }
    }
    
    @Test
    fun testFileSize() {
        // Test with non-existent file
        assertEquals(-1L, FileUtils.getFileSize("non_existent_file.txt"))
        
        // Create a temporary file with known content
        val tempFile = File.createTempFile("test", ".txt")
        val content = "Hello, World!"
        tempFile.writeText(content)
        
        try {
            val size = FileUtils.getFileSize(tempFile.absolutePath)
            assertTrue(size > 0)
            assertEquals(content.length.toLong(), size)
        } finally {
            tempFile.delete()
        }
    }
    
    @Test
    fun testDeleteFile() {
        // Test with non-existent file
        assertFalse(FileUtils.delete("non_existent_file.txt"))
        
        // Create a temporary file
        val tempFile = File.createTempFile("test", ".txt")
        tempFile.writeText("test content")
        
        assertTrue(FileUtils.exists(tempFile.absolutePath))
        assertTrue(FileUtils.delete(tempFile.absolutePath))
        assertFalse(FileUtils.exists(tempFile.absolutePath))
    }
    
    @Test
    fun testNormalizePath() {
        val path = "test/path/file.txt"
        val normalized = FileUtils.normalizePath(path)
        assertNotNull(normalized)
        assertTrue(normalized.isNotEmpty())
    }
    
    @Test
    fun testSaveAndLoadBasicTokenizer() = runBlocking {
        // Create and train a basic tokenizer
        val tokenizer = BasicTokenizer()
        tokenizer.train("hello world hello", 258, verbose = false)
        
        // Create temporary files
        val tempDir = File.createTempFile("test", "").apply { 
            delete()
            mkdirs() 
        }
        
        try {
            val modelFile = File(tempDir, "test.model").absolutePath
            val jsonFile = File(tempDir, "test.json").absolutePath
            
            // Test saving in Python-compatible format
            val saveResults = FileUtils.save(tokenizer, modelFile).toList()
            val lastSaveResult = saveResults.lastOrNull()
            assertTrue(lastSaveResult is SaveProgress.Completed)
            assertTrue(FileUtils.exists(modelFile))
            
            // Test saving in JSON format
            val jsonSaveResults = FileUtils.save(tokenizer, jsonFile).toList()
            val lastJsonSaveResult = jsonSaveResults.lastOrNull()
            assertTrue(lastJsonSaveResult is SaveProgress.Completed)
            assertTrue(FileUtils.exists(jsonFile))
            
            // Test loading from Python-compatible format
            val loadResults = FileUtils.load(modelFile).toList()
            val lastLoadResult = loadResults.lastOrNull()
            assertTrue(lastLoadResult is LoadResult.Completed)
            
            // Test loading from JSON format
            val jsonLoadResults = FileUtils.load(jsonFile).toList()
            val lastJsonLoadResult = jsonLoadResults.lastOrNull()
            assertTrue(lastJsonLoadResult is LoadResult.Completed)
            
        } finally {
            // Clean up
            tempDir.deleteRecursively()
        }
    }
}