package sk.ainet.nlp.tools.tokenizer.persistence

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.io.Source
import kotlinx.io.Sink
import kotlinx.io.readString
import kotlinx.io.writeString
import sk.ainet.nlp.tools.tokenizer.Tokenizer

/**
 * Python-compatible persistence implementation that can read/write the same
 * text-based .model format as the original Python minbpe implementation.
 * 
 * Format specification (from Python base.py):
 * Line 1: "minbpe v1"
 * Line 2: pattern (empty string for BasicTokenizer)
 * Line 3: number of special tokens
 * Next N lines: special_token token_id (one per line)
 * Remaining lines: merge_token1 merge_token2 (one pair per line)
 * 
 * This allows seamless interoperability between Python and Kotlin implementations.
 */
class PythonCompatiblePersistence : TokenizerPersistence {
    
    override fun save(tokenizer: Tokenizer, sink: Sink): Flow<SaveProgress> = flow {
        emit(SaveProgress.Started)
        
        try {
            val content = buildString {
                // Line 1: version
                appendLine("minbpe v1")
                
                // Line 2: pattern
                appendLine(tokenizer.pattern)
                
                // Line 3: number of special tokens
                appendLine(tokenizer.specialTokens.size.toString())
                
                // Special tokens (one per line: token id)
                tokenizer.specialTokens.forEach { (token, id) ->
                    appendLine("$token $id")
                }
                
                // Merges (one per line: token1 token2)
                // Note: Python iterates merges dict directly, so we need to maintain order
                tokenizer.merges.forEach { (pair, _) ->
                    val (token1, token2) = pair
                    appendLine("$token1 $token2")
                }
            }
            
            emit(SaveProgress.WritingModel(0))
            
            // Write to sink
            sink.writeString(content)
            
            emit(SaveProgress.WritingModel(content.length.toLong()))
            emit(SaveProgress.Completed(content.length.toLong()))
            
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
            // Read all content
            val content = source.readString()
            val lines = content.lines()
            
            emit(LoadResult.ReadingModel(content.length.toLong()))
            emit(LoadResult.ParsingModel("Reading version"))
            
            var lineIndex = 0
            
            // Line 1: version
            if (lineIndex >= lines.size) {
                throw IllegalArgumentException("Empty model file")
            }
            val version = lines[lineIndex++].trim()
            if (version != "minbpe v1") {
                throw IllegalArgumentException("Unsupported version: $version")
            }
            
            emit(LoadResult.ParsingModel("Reading pattern"))
            
            // Line 2: pattern
            if (lineIndex >= lines.size) {
                throw IllegalArgumentException("Missing pattern line")
            }
            val pattern = lines[lineIndex++].trim()
            
            emit(LoadResult.ParsingModel("Reading special tokens"))
            
            // Line 3: number of special tokens
            if (lineIndex >= lines.size) {
                throw IllegalArgumentException("Missing special tokens count")
            }
            val numSpecial = lines[lineIndex++].trim().toIntOrNull()
                ?: throw IllegalArgumentException("Invalid special tokens count")
            
            // Read special tokens
            val specialTokens = mutableMapOf<String, Int>()
            repeat(numSpecial) {
                if (lineIndex >= lines.size) {
                    throw IllegalArgumentException("Missing special token at line $lineIndex")
                }
                val parts = lines[lineIndex++].trim().split(" ", limit = 2)
                if (parts.size != 2) {
                    throw IllegalArgumentException("Invalid special token format: ${lines[lineIndex - 1]}")
                }
                val token = parts[0]
                val id = parts[1].toIntOrNull()
                    ?: throw IllegalArgumentException("Invalid special token ID: ${parts[1]}")
                specialTokens[token] = id
            }
            
            emit(LoadResult.ParsingModel("Reading merges"))
            
            // Read merges
            val merges = mutableListOf<Pair<Pair<Int, Int>, Int>>()
            var mergeId = 256
            
            while (lineIndex < lines.size) {
                val line = lines[lineIndex++].trim()
                if (line.isEmpty()) continue // Skip empty lines
                
                val parts = line.split(" ")
                if (parts.size != 2) {
                    throw IllegalArgumentException("Invalid merge format: $line")
                }
                
                val token1 = parts[0].toIntOrNull()
                    ?: throw IllegalArgumentException("Invalid merge token1: ${parts[0]}")
                val token2 = parts[1].toIntOrNull()
                    ?: throw IllegalArgumentException("Invalid merge token2: ${parts[1]}")
                
                merges.add(Pair(Pair(token1, token2), mergeId))
                mergeId++
            }
            
            emit(LoadResult.ParsingModel("Creating tokenizer data"))
            
            // Create TokenizerData
            val tokenizerData = TokenizerData(
                version = version,
                type = inferTokenizerType(pattern, specialTokens),
                pattern = pattern,
                specialTokens = specialTokens,
                merges = merges
            )
            
            tokenizerData.validate()
            
            emit(LoadResult.Completed(tokenizerData))
            
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
            
            // Build vocabulary content in Python-compatible format
            val vocabContent = buildString {
                // Find merge relationships for display
                val invertedMerges = tokenizer.merges.entries
                    .associate { (pair, id) -> id to pair }
                
                vocab.entries.sortedBy { it.key }.forEach { (id, bytes) ->
                    val tokenStr = renderToken(bytes)
                    
                    // Show merge information if available (like Python)
                    if (id in invertedMerges) {
                        val (id1, id2) = invertedMerges[id]!!
                        val token1Str = renderToken(tokenizer.vocab[id1] ?: byteArrayOf())
                        val token2Str = renderToken(tokenizer.vocab[id2] ?: byteArrayOf())
                        
                        appendLine("[$token1Str][$token2Str] -> [$tokenStr] $id")
                    } else {
                        // Base token (0-255)
                        appendLine("[$tokenStr] $id")
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
    
    /**
     * Render a token as a string, handling control characters like Python's render_token.
     * This mimics the Python implementation's character escaping.
     */
    private fun renderToken(bytes: ByteArray): String {
        return try {
            val s = bytes.decodeToString()
            // Replace control characters with escape sequences
            s.map { ch ->
                when {
                    ch.isISOControl() -> "\\u${ch.code.toString(16).padStart(4, '0')}"
                    else -> ch.toString()
                }
            }.joinToString("")
        } catch (e: Exception) {
            // Handle invalid UTF-8 sequences
            bytes.joinToString("") { byte ->
                val unsigned = byte.toInt() and 0xFF
                "\\u${unsigned.toString(16).padStart(4, '0')}"
            }
        }
    }
    
    /**
     * Infer tokenizer type from pattern and special tokens.
     */
    private fun inferTokenizerType(pattern: String, specialTokens: Map<String, Int>): String {
        return when {
            pattern.isEmpty() && specialTokens.isEmpty() -> "basic"
            pattern.isNotEmpty() && specialTokens.isEmpty() -> "regex"
            pattern.isNotEmpty() && specialTokens.isNotEmpty() -> "gpt4"
            else -> "unknown"
        }
    }
}