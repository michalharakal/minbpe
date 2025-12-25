package sk.ainet.nlp.tools.tokenizer.persistence

import sk.ainet.nlp.tools.tokenizer.Tokenizer

/**
 * Results emitted by repository operations.
 */
sealed class RepositoryResult {
    data class Started(val message: String) : RepositoryResult()
    data class Progress(val message: String) : RepositoryResult()
    data class Completed(val message: String) : RepositoryResult()
    data class TokenizerLoaded(val tokenizer: Tokenizer, val message: String) : RepositoryResult()
    data class Error(val message: String) : RepositoryResult()
}

/**
 * Information about a tokenizer file.
 */
data class FileInfo(
    val path: String,
    val size: Long,
    val format: String,
    val exists: Boolean
)