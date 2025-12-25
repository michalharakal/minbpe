package sk.ainet.tokenizer.comparison.model

import kotlinx.serialization.Serializable

/**
 * Supported export formats for tokenizer configurations.
 */
@Serializable
enum class ExportFormat {
    /**
     * JSON format for cross-platform compatibility.
     */
    JSON,
    
    /**
     * Binary format for efficient storage.
     */
    BINARY
}