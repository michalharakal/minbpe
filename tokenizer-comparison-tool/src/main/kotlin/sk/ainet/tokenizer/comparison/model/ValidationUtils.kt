package sk.ainet.tokenizer.comparison.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/**
 * Utility functions for validating and serializing data models.
 */
object ValidationUtils {
    
    val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
        allowStructuredMapKeys = true
    }
    
    /**
     * Validates that a TokenizerConfig has all required fields and valid values.
     */
    fun validateTokenizerConfig(config: TokenizerConfig): ValidationResult {
        val errors = mutableListOf<String>()
        
        if (config.vocabSize <= 0) {
            errors.add("Vocabulary size must be positive, got: ${config.vocabSize}")
        }
        
        if (config.merges.isEmpty() && config.type != TokenizerType.BASIC) {
            errors.add("Non-basic tokenizers must have merge rules")
        }
        
        // Validate merge format
        config.merges.forEach { (pair, id) ->
            if (pair.first < 0 || pair.second < 0) {
                errors.add("Merge pair contains negative token ID: $pair -> $id")
            }
            if (id < 0) {
                errors.add("Merge result ID must be non-negative: $pair -> $id")
            }
        }
        
        // Validate special tokens
        config.specialTokens.forEach { (token, id) ->
            if (token.isEmpty()) {
                errors.add("Special token cannot be empty string")
            }
            if (id < 0) {
                errors.add("Special token ID must be non-negative: '$token' -> $id")
            }
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Failure(errors)
        }
    }
    
    /**
     * Validates cross-platform compatibility of a tokenizer configuration.
     */
    fun validateCrossPlatformCompatibility(config: TokenizerConfig): ValidationResult {
        val errors = mutableListOf<String>()
        
        // Check for platform-specific patterns that might not work across implementations
        if (config.pattern.contains("\\p{") && config.type == TokenizerType.REGEX) {
            errors.add("Unicode property classes in regex patterns may not be compatible across platforms")
        }
        
        // Validate metadata doesn't contain platform-specific paths
        config.metadata.forEach { (key, value) ->
            if (value.contains("\\") || value.contains("/")) {
                errors.add("Metadata value '$key' contains path separators that may not be cross-platform compatible")
            }
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Failure(errors)
        }
    }
    
    /**
     * Serializes a TokenizerConfig to JSON string.
     */
    fun serializeTokenizerConfig(config: TokenizerConfig): String {
        return json.encodeToString(config)
    }
    
    /**
     * Deserializes a TokenizerConfig from JSON string.
     */
    fun deserializeTokenizerConfig(jsonString: String): Result<TokenizerConfig> {
        return try {
            val config = json.decodeFromString<TokenizerConfig>(jsonString)
            Result.success(config)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Serializes any serializable model to JSON string.
     */
    inline fun <reified T> serialize(obj: T): String {
        return json.encodeToString(obj)
    }
    
    /**
     * Deserializes any serializable model from JSON string.
     */
    inline fun <reified T> deserialize(jsonString: String): Result<T> {
        return try {
            val obj = json.decodeFromString<T>(jsonString)
            Result.success(obj)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Result of a validation operation.
 */
sealed class ValidationResult {
    object Success : ValidationResult()
    data class Failure(val errors: List<String>) : ValidationResult()
    
    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure
}