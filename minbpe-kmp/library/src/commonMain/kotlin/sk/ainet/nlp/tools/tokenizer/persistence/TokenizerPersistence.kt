package sk.ainet.nlp.tools.tokenizer.persistence

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.io.Source
import kotlinx.io.Sink
import kotlinx.serialization.Serializable
import sk.ainet.nlp.tools.tokenizer.Tokenizer

/**
 * Reactive persistence interface for tokenizer save/load operations.
 * 
 * This interface separates I/O concerns from the tokenizer logic,
 * providing a clean, testable, and platform-agnostic way to handle
 * tokenizer persistence using kotlinx.io and Flow APIs.
 */
interface TokenizerPersistence {
    
    /**
     * Save a tokenizer to a sink as a reactive stream.
     * 
     * @param tokenizer The tokenizer to save
     * @param sink The output sink to write to
     * @return Flow that emits save progress events
     */
    fun save(tokenizer: Tokenizer, sink: Sink): Flow<SaveProgress>
    
    /**
     * Load a tokenizer from a source as a reactive stream.
     * 
     * @param source The input source to read from
     * @return Flow that emits load progress events and the final tokenizer
     */
    fun load(source: Source): Flow<LoadResult>
    
    /**
     * Save vocabulary file for human inspection.
     * 
     * @param tokenizer The tokenizer whose vocab to save
     * @param sink The output sink for the vocab file
     * @return Flow that emits save progress events
     */
    fun saveVocab(tokenizer: Tokenizer, sink: Sink): Flow<SaveProgress>
}

/**
 * Progress events emitted during save operations.
 */
@Serializable
sealed class SaveProgress {
    @Serializable
    data object Started : SaveProgress()
    
    @Serializable
    data class WritingModel(val bytesWritten: Long) : SaveProgress()
    
    @Serializable
    data class WritingVocab(val tokensWritten: Int, val totalTokens: Int) : SaveProgress()
    
    @Serializable
    data class Completed(val totalBytes: Long) : SaveProgress()
    
    @Serializable
    data class Error(val message: String, val cause: String? = null) : SaveProgress()
}

/**
 * Results emitted during load operations.
 */
@Serializable
sealed class LoadResult {
    @Serializable
    data object Started : LoadResult()
    
    @Serializable
    data class ReadingModel(val bytesRead: Long) : LoadResult()
    
    @Serializable
    data class ParsingModel(val stage: String) : LoadResult()
    
    @Serializable
    data class Completed(val tokenizer: TokenizerData) : LoadResult()
    
    @Serializable
    data class Error(val message: String, val cause: String? = null) : LoadResult()
}

/**
 * Serializable representation of tokenizer data for persistence.
 */
@Serializable
data class TokenizerData(
    val version: String,
    val type: String, // "basic", "regex", "gpt4"
    val pattern: String,
    val specialTokens: Map<String, Int>,
    val merges: List<Pair<Pair<Int, Int>, Int>>,
    val metadata: Map<String, String> = emptyMap()
) {
    fun validate() {
        require(version.startsWith("minbpe v")) { "Invalid version: $version" }
        require(type.isNotBlank()) { "Tokenizer type cannot be blank" }
        require(merges.all { (pair, id) -> pair.first >= 0 && pair.second >= 0 && id >= 256 }) {
            "Invalid merge data"
        }
    }
    
    fun getMergesMap(): Map<Pair<Int, Int>, Int> = merges.toMap()
}