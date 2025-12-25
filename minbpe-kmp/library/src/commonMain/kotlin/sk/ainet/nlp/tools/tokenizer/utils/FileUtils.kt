package sk.ainet.nlp.tools.tokenizer.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.buffered
import kotlinx.io.readString
import sk.ainet.nlp.tools.tokenizer.Tokenizer
import sk.ainet.nlp.tools.tokenizer.persistence.*

/**
 * Cross-platform file utilities for tokenizer operations using kotlinx-io.
 * 
 * This utility class provides consistent file handling across all Kotlin Multiplatform
 * targets, including proper error handling and path management. It uses kotlinx-io
 * for cross-platform compatibility and reactive Flow APIs for progress monitoring.
 */
object FileUtils {
    
    /**
     * Save tokenizer to files with automatic format detection based on extension.
     * 
     * @param tokenizer The tokenizer to save
     * @param filePath Path to the output file (extension determines format)
     * @return Flow of save progress events
     */
    fun save(tokenizer: Tokenizer, filePath: String): Flow<SaveProgress> = flow {
        emit(SaveProgress.Started)
        
        try {
            val path = Path(filePath)
            val persistence = when {
                filePath.endsWith(".json") -> JsonTokenizerPersistence()
                filePath.endsWith(".model") -> PythonCompatiblePersistence()
                else -> {
                    emit(SaveProgress.Error("Unsupported file extension. Use .json or .model"))
                    return@flow
                }
            }
            
            val sink = SystemFileSystem.sink(path).buffered()
            
            try {
                persistence.save(tokenizer, sink).collect { progress ->
                    emit(progress)
                }
            } finally {
                sink.close()
            }
            
        } catch (e: Exception) {
            emit(SaveProgress.Error(
                message = "Failed to save tokenizer: ${e.message}",
                cause = e.cause?.message
            ))
        }
    }
    
    /**
     * Save tokenizer vocabulary to a human-readable file.
     * 
     * @param tokenizer The tokenizer whose vocabulary to save
     * @param vocabPath Path to the vocabulary file
     * @return Flow of save progress events
     */
    fun saveVocab(tokenizer: Tokenizer, vocabPath: String): Flow<SaveProgress> = flow {
        emit(SaveProgress.Started)
        
        try {
            val path = Path(vocabPath)
            val persistence = when {
                vocabPath.endsWith(".json") -> JsonTokenizerPersistence()
                else -> PythonCompatiblePersistence() // Default to Python format for vocab
            }
            
            val sink = SystemFileSystem.sink(path).buffered()
            
            try {
                persistence.saveVocab(tokenizer, sink).collect { progress ->
                    emit(progress)
                }
            } finally {
                sink.close()
            }
            
        } catch (e: Exception) {
            emit(SaveProgress.Error(
                message = "Failed to save vocabulary: ${e.message}",
                cause = e.cause?.message
            ))
        }
    }
    
    /**
     * Load tokenizer from file with automatic format detection.
     * 
     * @param filePath Path to the tokenizer file
     * @return Flow of load results, ending with the loaded tokenizer data
     */
    fun load(filePath: String): Flow<LoadResult> = flow {
        emit(LoadResult.Started)
        
        try {
            val path = Path(filePath)
            
            if (!SystemFileSystem.exists(path)) {
                emit(LoadResult.Error("File not found: $filePath"))
                return@flow
            }
            
            // Auto-detect format
            val persistence = detectFormat(filePath)
            if (persistence == null) {
                emit(LoadResult.Error("Cannot detect file format for: $filePath"))
                return@flow
            }
            
            val source = SystemFileSystem.source(path).buffered()
            
            try {
                persistence.load(source).collect { result ->
                    emit(result)
                }
            } finally {
                source.close()
            }
            
        } catch (e: Exception) {
            emit(LoadResult.Error(
                message = "Failed to load tokenizer: ${e.message}",
                cause = e.cause?.message
            ))
        }
    }
    
    /**
     * Check if a file exists at the given path.
     * 
     * @param filePath Path to check
     * @return true if file exists, false otherwise
     */
    fun exists(filePath: String): Boolean {
        return try {
            SystemFileSystem.exists(Path(filePath))
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Delete a file at the given path.
     * 
     * @param filePath Path to the file to delete
     * @return true if deletion was successful, false otherwise
     */
    fun delete(filePath: String): Boolean {
        return try {
            val path = Path(filePath)
            if (SystemFileSystem.exists(path)) {
                SystemFileSystem.delete(path)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get file size in bytes.
     * 
     * @param filePath Path to the file
     * @return File size in bytes, or -1 if file doesn't exist or error occurs
     */
    fun getFileSize(filePath: String): Long {
        return try {
            val path = Path(filePath)
            if (SystemFileSystem.exists(path)) {
                SystemFileSystem.metadataOrNull(path)?.size ?: -1L
            } else {
                -1L
            }
        } catch (e: Exception) {
            -1L
        }
    }
    
    /**
     * Create parent directories for the given file path if they don't exist.
     * 
     * @param filePath Path to the file
     */
    fun createParentDirectories(filePath: String) {
        try {
            val path = Path(filePath)
            val parent = path.parent
            if (parent != null && !SystemFileSystem.exists(parent)) {
                SystemFileSystem.createDirectories(parent)
            }
        } catch (e: Exception) {
            // Ignore errors - let the actual file operation fail if needed
        }
    }
    
    /**
     * Normalize file path for the current platform.
     * 
     * @param filePath Input file path
     * @return Normalized path
     */
    fun normalizePath(filePath: String): String {
        return try {
            Path(filePath).toString()
        } catch (e: Exception) {
            filePath
        }
    }
    
    /**
     * Detect file format based on extension and content.
     * 
     * @param filePath Path to the file
     * @return Appropriate persistence implementation or null if format cannot be detected
     */
    private fun detectFormat(filePath: String): TokenizerPersistence? {
        return try {
            when {
                filePath.endsWith(".json") -> JsonTokenizerPersistence()
                filePath.endsWith(".model") -> PythonCompatiblePersistence()
                else -> {
                    // Try to detect by content
                    val path = Path(filePath)
                    val source = SystemFileSystem.source(path).buffered()
                    
                    try {
                        val firstLine = source.readString().lines().firstOrNull()?.trim()
                        when {
                            firstLine == "minbpe v1" -> PythonCompatiblePersistence()
                            firstLine?.startsWith("{") == true -> JsonTokenizerPersistence()
                            else -> null
                        }
                    } finally {
                        source.close()
                    }
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}