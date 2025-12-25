package sk.ainet.nlp.tools.tokenizer.persistence

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.last
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.buffered
import kotlinx.io.readString
import sk.ainet.nlp.tools.tokenizer.Tokenizer

/**
 * Utility functions for saving and loading tokenizers to/from files
 * with support for both JSON and Python-compatible formats.
 */
object TokenizerFileUtils {
    
    /**
     * Save tokenizer in Python-compatible format (.model and .vocab files).
     * This creates files that can be read by the original Python minbpe implementation.
     * 
     * @param tokenizer The tokenizer to save
     * @param filePrefix Path prefix (e.g., "my_tokenizer" creates "my_tokenizer.model" and "my_tokenizer.vocab")
     */
    suspend fun savePythonCompatible(tokenizer: Tokenizer, filePrefix: String) {
        val persistence = PythonCompatiblePersistence()
        
        // Save model file
        val modelPath = Path("$filePrefix.model")
        val modelSink = SystemFileSystem.sink(modelPath).buffered()
        
        try {
            persistence.save(tokenizer, modelSink).last()
        } finally {
            modelSink.close()
        }
        
        // Save vocab file
        val vocabPath = Path("$filePrefix.vocab")
        val vocabSink = SystemFileSystem.sink(vocabPath).buffered()
        
        try {
            persistence.saveVocab(tokenizer, vocabSink).last()
        } finally {
            vocabSink.close()
        }
    }
    
    /**
     * Load tokenizer from Python-compatible .model file.
     * This can read files created by the original Python minbpe implementation.
     * 
     * @param modelFile Path to the .model file
     * @return Flow of load results, ending with the loaded tokenizer data
     */
    fun loadPythonCompatible(modelFile: String): Flow<LoadResult> {
        val persistence = PythonCompatiblePersistence()
        val modelPath = Path(modelFile)
        
        val source = SystemFileSystem.source(modelPath).buffered()
        return persistence.load(source)
    }
    
    /**
     * Save tokenizer in JSON format.
     * This creates a single JSON file with all tokenizer data.
     * 
     * @param tokenizer The tokenizer to save
     * @param jsonFile Path to the JSON file to create
     */
    suspend fun saveJson(tokenizer: Tokenizer, jsonFile: String) {
        val persistence = JsonTokenizerPersistence()
        val jsonPath = Path(jsonFile)
        
        val sink = SystemFileSystem.sink(jsonPath).buffered()
        try {
            persistence.save(tokenizer, sink).last()
        } finally {
            sink.close()
        }
    }
    
    /**
     * Load tokenizer from JSON file.
     * 
     * @param jsonFile Path to the JSON file
     * @return Flow of load results, ending with the loaded tokenizer data
     */
    fun loadJson(jsonFile: String): Flow<LoadResult> {
        val persistence = JsonTokenizerPersistence()
        val jsonPath = Path(jsonFile)
        
        val source = SystemFileSystem.source(jsonPath).buffered()
        return persistence.load(source)
    }
    
    /**
     * Auto-detect file format and load tokenizer.
     * Detects format based on file extension and content.
     * 
     * @param filePath Path to the tokenizer file
     * @return Flow of load results, ending with the loaded tokenizer data
     */
    fun loadAuto(filePath: String): Flow<LoadResult> {
        return when {
            filePath.endsWith(".model") -> loadPythonCompatible(filePath)
            filePath.endsWith(".json") -> loadJson(filePath)
            else -> {
                // Try to detect by content
                val path = Path(filePath)
                val source = SystemFileSystem.source(path).buffered()
                
                try {
                    val firstLine = source.readString().lines().firstOrNull()?.trim()
                    when {
                        firstLine == "minbpe v1" -> {
                            source.close()
                            loadPythonCompatible(filePath)
                        }
                        firstLine?.startsWith("{") == true -> {
                            source.close()
                            loadJson(filePath)
                        }
                        else -> {
                            source.close()
                            throw IllegalArgumentException("Cannot detect file format for: $filePath")
                        }
                    }
                } catch (e: Exception) {
                    source.close()
                    throw e
                }
            }
        }
    }
}