package sk.ainet.nlp.tools.tokenizer.persistence

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.buffered
import kotlinx.io.readString
import sk.ainet.nlp.tools.tokenizer.Tokenizer
import sk.ainet.nlp.tools.tokenizer.BasicTokenizer
import sk.ainet.nlp.tools.tokenizer.RegexTokenizer

/**
 * High-level repository for tokenizer persistence operations.
 * 
 * This class provides a convenient API for saving and loading tokenizers
 * to/from the file system, handling the details of file operations and
 * progress monitoring. Supports both JSON and Python-compatible formats.
 */
class TokenizerRepository(
    private val persistence: TokenizerPersistence = JsonTokenizerPersistence()
) {
    
    /**
     * Save tokenizer in Python-compatible format.
     * Creates files that can be read by the original Python minbpe implementation.
     */
    fun savePythonCompatible(tokenizer: Tokenizer, filePrefix: String): Flow<RepositoryResult> {
        val pythonPersistence = PythonCompatiblePersistence()
        return saveWithPersistence(tokenizer, filePrefix, pythonPersistence)
    }
    
    /**
     * Load tokenizer from Python-compatible .model file.
     * Can read files created by the original Python minbpe implementation.
     */
    fun loadPythonCompatible(modelFile: String): Flow<RepositoryResult> {
        val pythonPersistence = PythonCompatiblePersistence()
        return loadWithPersistence(modelFile, pythonPersistence)
    }
    
    /**
     * Auto-detect file format and load tokenizer.
     */
    fun loadAuto(filePath: String): Flow<RepositoryResult> = flow {
        emit(RepositoryResult.Started("Auto-detecting format for $filePath"))
        
        try {
            val path = Path(filePath)
            if (!SystemFileSystem.exists(path)) {
                emit(RepositoryResult.Error("File not found: $filePath"))
                return@flow
            }
            
            // Detect format
            val detectedPersistence = when {
                filePath.endsWith(".model") -> {
                    emit(RepositoryResult.Progress("Detected Python-compatible format"))
                    PythonCompatiblePersistence()
                }
                filePath.endsWith(".json") -> {
                    emit(RepositoryResult.Progress("Detected JSON format"))
                    JsonTokenizerPersistence()
                }
                else -> {
                    // Try to detect by content
                    val source = SystemFileSystem.source(path).buffered()
                    val firstLine = source.readString().lines().firstOrNull()?.trim()
                    source.close()
                    
                    when {
                        firstLine == "minbpe v1" -> {
                            emit(RepositoryResult.Progress("Detected Python-compatible format by content"))
                            PythonCompatiblePersistence()
                        }
                        firstLine?.startsWith("{") == true -> {
                            emit(RepositoryResult.Progress("Detected JSON format by content"))
                            JsonTokenizerPersistence()
                        }
                        else -> {
                            emit(RepositoryResult.Error("Cannot detect file format for: $filePath"))
                            return@flow
                        }
                    }
                }
            }
            
            // Load with detected persistence
            loadWithPersistence(filePath, detectedPersistence).collect { result ->
                emit(result)
            }
            
        } catch (e: Exception) {
            emit(RepositoryResult.Error("Auto-detection failed: ${e.message}"))
        }
    }
    
    /**
     * Save a tokenizer to files with the given prefix using the default persistence.
     * Creates two files:
     * - {filePrefix}.model: Serialized tokenizer model
     * - {filePrefix}.vocab: Human-readable vocabulary file
     * 
     * @param tokenizer The tokenizer to save
     * @param filePrefix Prefix for output files
     * @return Flow of save operations with progress
     */
    fun save(tokenizer: Tokenizer, filePrefix: String): Flow<RepositoryResult> {
        return saveWithPersistence(tokenizer, filePrefix, persistence)
    }
    
    /**
     * Load a tokenizer from a model file using the default persistence.
     * 
     * @param modelFile Path to the model file
     * @return Flow of load operations with the final tokenizer
     */
    fun load(modelFile: String): Flow<RepositoryResult> {
        return loadWithPersistence(modelFile, persistence)
    }
    
    /**
     * Internal method to save with a specific persistence implementation.
     */
    private fun saveWithPersistence(
        tokenizer: Tokenizer, 
        filePrefix: String, 
        persistenceImpl: TokenizerPersistence
    ): Flow<RepositoryResult> = flow {
        emit(RepositoryResult.Started("Saving tokenizer to $filePrefix"))
        
        try {
            // Save model file
            val modelPath = Path("$filePrefix.model")
            val modelSink = SystemFileSystem.sink(modelPath).buffered()
            
            val modelProgress = mutableListOf<SaveProgress>()
            persistenceImpl.save(tokenizer, modelSink).toList(modelProgress)
            
            modelSink.close()
            
            val lastModelProgress = modelProgress.lastOrNull()
            if (lastModelProgress is SaveProgress.Error) {
                emit(RepositoryResult.Error("Model save failed: ${lastModelProgress.message}"))
                return@flow
            }
            
            emit(RepositoryResult.Progress("Model file saved successfully"))
            
            // Save vocabulary file
            val vocabPath = Path("$filePrefix.vocab")
            val vocabSink = SystemFileSystem.sink(vocabPath).buffered()
            
            val vocabProgress = mutableListOf<SaveProgress>()
            persistenceImpl.saveVocab(tokenizer, vocabSink).toList(vocabProgress)
            
            vocabSink.close()
            
            val lastVocabProgress = vocabProgress.lastOrNull()
            if (lastVocabProgress is SaveProgress.Error) {
                emit(RepositoryResult.Error("Vocabulary save failed: ${lastVocabProgress.message}"))
                return@flow
            }
            
            emit(RepositoryResult.Progress("Vocabulary file saved successfully"))
            emit(RepositoryResult.Completed("Tokenizer saved to $filePrefix.model and $filePrefix.vocab"))
            
        } catch (e: Exception) {
            emit(RepositoryResult.Error("Save operation failed: ${e.message}"))
        }
    }
    
    /**
     * Internal method to load with a specific persistence implementation.
     */
    private fun loadWithPersistence(modelFile: String, persistenceImpl: TokenizerPersistence): Flow<RepositoryResult> = flow {
        emit(RepositoryResult.Started("Loading tokenizer from $modelFile"))
        
        try {
            val modelPath = Path(modelFile)
            if (!SystemFileSystem.exists(modelPath)) {
                emit(RepositoryResult.Error("Model file not found: $modelFile"))
                return@flow
            }
            
            val source = SystemFileSystem.source(modelPath).buffered()
            
            val loadResults = mutableListOf<LoadResult>()
            persistenceImpl.load(source).toList(loadResults)
            
            source.close()
            
            val lastResult = loadResults.lastOrNull()
            when (lastResult) {
                is LoadResult.Completed -> {
                    val tokenizerData = lastResult.tokenizer
                    val tokenizer = createTokenizerFromData(tokenizerData)
                    emit(RepositoryResult.TokenizerLoaded(tokenizer, "Tokenizer loaded successfully"))
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
     * Create a tokenizer instance from loaded data.
     * This factory method determines the appropriate tokenizer type
     * based on the data and reconstructs the tokenizer.
     */
    private fun createTokenizerFromData(data: TokenizerData): Tokenizer {
        return when (data.type.lowercase()) {
            "basic" -> {
                val tokenizer = BasicTokenizer()
                // Use reflection or a factory pattern to set internal state
                // For now, we'll need to add a factory method to tokenizers
                tokenizer.loadFromData(data)
                tokenizer
            }
            "regex" -> {
                val tokenizer = RegexTokenizer(data.pattern)
                tokenizer.loadFromData(data)
                tokenizer
            }
            "gpt4" -> {
                // TODO: Implement GPT4Tokenizer factory
                throw UnsupportedOperationException("GPT4Tokenizer loading not yet implemented")
            }
            else -> {
                throw IllegalArgumentException("Unknown tokenizer type: ${data.type}")
            }
        }
    }
}