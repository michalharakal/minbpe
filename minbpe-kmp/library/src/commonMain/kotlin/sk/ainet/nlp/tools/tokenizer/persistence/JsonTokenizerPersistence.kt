package sk.ainet.nlp.tools.tokenizer.persistence

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.io.Buffer
import kotlinx.io.Source
import kotlinx.io.Sink
import kotlinx.io.readByteArray
import kotlinx.io.writeString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import sk.ainet.nlp.tools.tokenizer.Tokenizer

/**
 * JSON-based implementation of TokenizerPersistence using kotlinx.io.
 * 
 * This implementation provides reactive save/load operations with progress
 * monitoring and error handling. It uses JSON serialization for cross-platform
 * compatibility and kotlinx.io for efficient streaming I/O operations.
 */
class JsonTokenizerPersistence : TokenizerPersistence {
    
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    
    override fun save(tokenizer: Tokenizer, sink: Sink): Flow<SaveProgress> = flow {
        emit(SaveProgress.Started)
        
        try {
            // Create serializable model from tokenizer
            val model = TokenizerData(
                version = "minbpe v1",
                type = tokenizer::class.simpleName?.lowercase()?.removeSuffix("tokenizer") ?: "unknown",
                pattern = tokenizer.pattern,
                specialTokens = tokenizer.specialTokens,
                merges = tokenizer.merges.toList(),
                metadata = mapOf(
                    "vocabSize" to tokenizer.vocab.size.toString(),
                    "mergeCount" to tokenizer.merges.size.toString()
                )
            )
            
            // Serialize to JSON
            val jsonString = json.encodeToString(model)
            val jsonBytes = jsonString.encodeToByteArray()
            
            emit(SaveProgress.WritingModel(0))
            
            // Write to sink
            sink.writeString(jsonString)
            
            emit(SaveProgress.WritingModel(jsonString.length.toLong()))
            emit(SaveProgress.Completed(jsonString.length.toLong()))
            
        } catch (e: Exception) {
            emit(SaveProgress.Error(
                message = "Failed to save tokenizer: ${e.message}",
                cause = e.cause?.message
            ))
        }
    }
    
    override fun load(source: Source): Flow<LoadResult> = flow {
        emit(LoadResult.Started)
        
        try {
            // Read all data from source
            val jsonBytes = source.readByteArray()
            
            emit(LoadResult.ReadingModel(jsonBytes.size.toLong()))
            emit(LoadResult.ParsingModel("Reading JSON data"))
            
            // Convert to string
            val jsonString = jsonBytes.decodeToString()
            
            emit(LoadResult.ParsingModel("Deserializing tokenizer data"))
            
            // Deserialize from JSON
            val model = json.decodeFromString<TokenizerData>(jsonString)
            model.validate()
            
            emit(LoadResult.ParsingModel("Validating tokenizer data"))
            emit(LoadResult.Completed(model))
            
        } catch (e: Exception) {
            emit(LoadResult.Error(
                message = "Failed to load tokenizer: ${e.message}",
                cause = e.cause?.message
            ))
        }
    }
    
    override fun saveVocab(tokenizer: Tokenizer, sink: Sink): Flow<SaveProgress> = flow {
        emit(SaveProgress.Started)
        
        try {
            val vocab = tokenizer.vocab
            val totalTokens = vocab.size
            var tokensWritten = 0
            
            // Build human-readable vocabulary content
            val vocabContent = buildString {
                appendLine("minbpe v1")
                appendLine("Tokenizer Type: ${tokenizer::class.simpleName}")
                appendLine("Pattern: ${tokenizer.pattern}")
                appendLine("Special Tokens: ${tokenizer.specialTokens.size}")
                appendLine()
                
                // Special tokens section
                if (tokenizer.specialTokens.isNotEmpty()) {
                    appendLine("=== Special Tokens ===")
                    tokenizer.specialTokens.forEach { (token, id) ->
                        appendLine("$token -> $id")
                    }
                    appendLine()
                }
                
                // Vocabulary section
                appendLine("=== Vocabulary (${vocab.size} tokens) ===")
                
                // Find merge relationships for better display
                val invertedMerges = tokenizer.merges.entries
                    .associate { (pair, id) -> id to pair }
                
                vocab.entries.sortedBy { it.key }.forEach { (id, bytes) ->
                    val tokenStr = try {
                        bytes.decodeToString()
                            .replace("\n", "\\n")
                            .replace("\r", "\\r")
                            .replace("\t", "\\t")
                    } catch (e: Exception) {
                        // Handle invalid UTF-8 sequences
                        bytes.joinToString(" ") { byte -> 
                            val unsigned = byte.toInt() and 0xFF
                            "0x${unsigned.toString(16).padStart(2, '0')}"
                        }
                    }
                    
                    // Show merge information if available
                    if (id in invertedMerges) {
                        val (id1, id2) = invertedMerges[id]!!
                        val token1Str = try {
                            tokenizer.vocab[id1]?.decodeToString() ?: "?"
                        } catch (e: Exception) { "?" }
                        val token2Str = try {
                            tokenizer.vocab[id2]?.decodeToString() ?: "?"
                        } catch (e: Exception) { "?" }
                        
                        appendLine("$id: [$token1Str] + [$token2Str] -> [$tokenStr]")
                    } else {
                        // Base token (0-255)
                        appendLine("$id: [$tokenStr]")
                    }
                    
                    tokensWritten++
                    if (tokensWritten % 100 == 0) {
                        // Emit progress periodically
                        emit(SaveProgress.WritingVocab(tokensWritten, totalTokens))
                    }
                }
            }
            
            // Write vocabulary content
            sink.writeString(vocabContent)
            
            emit(SaveProgress.WritingVocab(totalTokens, totalTokens))
            emit(SaveProgress.Completed(vocabContent.length.toLong()))
            
        } catch (e: Exception) {
            emit(SaveProgress.Error(
                message = "Failed to save vocabulary: ${e.message}",
                cause = e.cause?.message
            ))
        }
    }
}