package sk.ainet.nlp.tools.tokenizer.persistence

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import sk.ainet.nlp.tools.tokenizer.Tokenizer
import sk.ainet.nlp.tools.tokenizer.BasicTokenizer
import sk.ainet.nlp.tools.tokenizer.RegexTokenizer
import sk.ainet.nlp.tools.tokenizer.GPT4Tokenizer
import sk.ainet.nlp.tools.tokenizer.utils.FileUtils

/**
 * File-based repository for tokenizer persistence operations.
 * 
 * This repository provides a high-level API for saving and loading tokenizers
 * to/from files, handling format detection, error management, and progress monitoring.
 * It uses FileUtils for cross-platform file operations and supports both JSON
 * and Python-compatible formats.
 */
class TokenizerFileRepository {
    
    /**
     * Save tokenizer to file with automatic format detection based on extension.
     * Creates both model and vocabulary files for Python-compatible format.
     * 
     * @param tokenizer The tokenizer to save
     * @param filePath Path to the output file (extension determines format)
     * @return Flow of repository operation results
     */
    fun save(tokenizer: Tokenizer, filePath: String): Flow<RepositoryResult> = flow {
        emit(RepositoryResult.Started("Saving tokenizer to $filePath"))
        
        try {
            // Create parent directories if needed
            FileUtils.createParentDirectories(filePath)
            
            val saveResults = mutableListOf<SaveProgress>()
            FileUtils.save(tokenizer, filePath).toList(saveResults)
            
            val lastResult = saveResults.lastOrNull()
            when (lastResult) {
                is SaveProgress.Completed -> {
                    emit(RepositoryResult.Progress("Model file saved successfully"))
                    
                    // For Python-compatible format, also save vocabulary file
                    if (filePath.endsWith(".model")) {
                        val vocabPath = filePath.replace(".model", ".vocab")
                        val vocabResults = mutableListOf<SaveProgress>()
                        FileUtils.saveVocab(tokenizer, vocabPath).toList(vocabResults)
                        
                        val lastVocabResult = vocabResults.lastOrNull()
                        when (lastVocabResult) {
                            is SaveProgress.Completed -> {
                                emit(RepositoryResult.Progress("Vocabulary file saved successfully"))
                                emit(RepositoryResult.Completed("Tokenizer saved to $filePath and $vocabPath"))
                            }
                            is SaveProgress.Error -> {
                                emit(RepositoryResult.Error("Vocabulary save failed: ${lastVocabResult.message}"))
                            }
                            else -> {
                                emit(RepositoryResult.Error("Vocabulary save did not complete properly"))
                            }
                        }
                    } else {
                        emit(RepositoryResult.Completed("Tokenizer saved to $filePath"))
                    }
                }
                is SaveProgress.Error -> {
                    emit(RepositoryResult.Error("Save failed: ${lastResult.message}"))
                }
                else -> {
                    emit(RepositoryResult.Error("Save operation did not complete properly"))
                }
            }
            
        } catch (e: Exception) {
            emit(RepositoryResult.Error("Save operation failed: ${e.message}"))
        }
    }
    
    /**
     * Save tokenizer in Python-compatible format.
     * Creates both .model and .vocab files that can be read by the original Python minbpe.
     * 
     * @param tokenizer The tokenizer to save
     * @param filePrefix Prefix for output files (creates {prefix}.model and {prefix}.vocab)
     * @return Flow of repository operation results
     */
    fun savePythonCompatible(tokenizer: Tokenizer, filePrefix: String): Flow<RepositoryResult> {
        return save(tokenizer, "$filePrefix.model")
    }
    
    /**
     * Save tokenizer in JSON format.
     * Creates a single JSON file with all tokenizer data.
     * 
     * @param tokenizer The tokenizer to save
     * @param jsonPath Path to the JSON file
     * @return Flow of repository operation results
     */
    fun saveJson(tokenizer: Tokenizer, jsonPath: String): Flow<RepositoryResult> {
        return save(tokenizer, jsonPath)
    }
    
    /**
     * Load tokenizer from file with automatic format detection.
     * 
     * @param filePath Path to the tokenizer file
     * @return Flow of repository operation results, ending with the loaded tokenizer
     */
    fun load(filePath: String): Flow<RepositoryResult> = flow {
        emit(RepositoryResult.Started("Loading tokenizer from $filePath"))
        
        try {
            if (!FileUtils.exists(filePath)) {
                emit(RepositoryResult.Error("File not found: $filePath"))
                return@flow
            }
            
            val loadResults = mutableListOf<LoadResult>()
            FileUtils.load(filePath).toList(loadResults)
            
            val lastResult = loadResults.lastOrNull()
            when (lastResult) {
                is LoadResult.Completed -> {
                    val tokenizerData = lastResult.tokenizer
                    val tokenizer = createTokenizerFromData(tokenizerData)
                    emit(RepositoryResult.TokenizerLoaded(tokenizer, "Tokenizer loaded successfully from $filePath"))
                }
                is LoadResult.Error -> {
                    emit(RepositoryResult.Error("Load failed: ${lastResult.message}"))
                }
                else -> {
                    emit(RepositoryResult.Error("Load operation did not complete properly"))
                }
            }
            
        } catch (e: Exception) {
            emit(RepositoryResult.Error("Load operation failed: ${e.message}"))
        }
    }
    
    /**
     * Load tokenizer from Python-compatible .model file.
     * 
     * @param modelFile Path to the .model file
     * @return Flow of repository operation results
     */
    fun loadPythonCompatible(modelFile: String): Flow<RepositoryResult> {
        return load(modelFile)
    }
    
    /**
     * Load tokenizer from JSON file.
     * 
     * @param jsonFile Path to the JSON file
     * @return Flow of repository operation results
     */
    fun loadJson(jsonFile: String): Flow<RepositoryResult> {
        return load(jsonFile)
    }
    
    /**
     * Auto-detect file format and load tokenizer.
     * Supports both .model and .json files, with content-based detection as fallback.
     * 
     * @param filePath Path to the tokenizer file
     * @return Flow of repository operation results
     */
    fun loadAuto(filePath: String): Flow<RepositoryResult> {
        return load(filePath) // FileUtils.load already does auto-detection
    }
    
    /**
     * Check if a tokenizer file exists.
     * 
     * @param filePath Path to check
     * @return true if file exists, false otherwise
     */
    fun exists(filePath: String): Boolean {
        return FileUtils.exists(filePath)
    }
    
    /**
     * Delete a tokenizer file.
     * For Python-compatible format, deletes both .model and .vocab files.
     * 
     * @param filePath Path to the file to delete
     * @return true if deletion was successful, false otherwise
     */
    fun delete(filePath: String): Boolean {
        return try {
            var success = FileUtils.delete(filePath)
            
            // For Python-compatible format, also delete vocabulary file
            if (filePath.endsWith(".model")) {
                val vocabPath = filePath.replace(".model", ".vocab")
                if (FileUtils.exists(vocabPath)) {
                    success = success && FileUtils.delete(vocabPath)
                }
            }
            
            success
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get information about a tokenizer file.
     * 
     * @param filePath Path to the tokenizer file
     * @return File information or null if file doesn't exist
     */
    fun getFileInfo(filePath: String): FileInfo? {
        return try {
            if (!FileUtils.exists(filePath)) {
                return null
            }
            
            val size = FileUtils.getFileSize(filePath)
            val format = when {
                filePath.endsWith(".json") -> "JSON"
                filePath.endsWith(".model") -> "Python-compatible"
                else -> "Unknown"
            }
            
            FileInfo(
                path = FileUtils.normalizePath(filePath),
                size = size,
                format = format,
                exists = true
            )
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Create a tokenizer instance from loaded data.
     * This factory method determines the appropriate tokenizer type
     * based on the data and reconstructs the tokenizer.
     */
    private fun createTokenizerFromData(data: TokenizerData): Tokenizer {
        return when (data.type.lowercase()) {
            "basic" -> {
                val tokenizer = BasicTokenizer()
                tokenizer.loadFromData(data)
                tokenizer
            }
            "regex" -> {
                val tokenizer = RegexTokenizer(data.pattern)
                tokenizer.loadFromData(data)
                tokenizer
            }
            "gpt4" -> {
                // GPT4Tokenizer is pre-trained, so we validate compatibility
                val tokenizer = GPT4Tokenizer()
                // Validate that loaded data is compatible with GPT-4
                if (data.specialTokens != tokenizer.specialTokens) {
                    throw IllegalArgumentException("Loaded data is not compatible with GPT-4 tokenizer")
                }
                tokenizer
            }
            else -> {
                throw IllegalArgumentException("Unknown tokenizer type: ${data.type}")
            }
        }
    }
}