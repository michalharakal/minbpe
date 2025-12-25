package sk.ainet.tokenizer.comparison.docker

import sk.ainet.tokenizer.comparison.model.TokenizerConfig
import sk.ainet.tokenizer.comparison.model.TokenizerType
import sk.ainet.tokenizer.comparison.model.ExportFormat

/**
 * Common interface for tokenizer operations across different container implementations.
 * Provides unified access to Python and Kotlin tokenizer implementations via Docker containers.
 */
interface ContainerInterface {
    
    /**
     * Train a tokenizer with the given text and parameters.
     * 
     * @param text Training text corpus
     * @param vocabSize Target vocabulary size
     * @param type Type of tokenizer to train (BASIC, REGEX, GPT4)
     * @return Trained tokenizer configuration
     */
    suspend fun trainTokenizer(
        text: String,
        vocabSize: Int,
        type: TokenizerType
    ): TokenizerConfig
    
    /**
     * Encode text using the provided tokenizer configuration.
     * 
     * @param config Tokenizer configuration to use
     * @param text Text to encode
     * @return List of token IDs
     */
    suspend fun encodeText(
        config: TokenizerConfig,
        text: String
    ): List<Int>
    
    /**
     * Decode token IDs back to text using the provided tokenizer configuration.
     * 
     * @param config Tokenizer configuration to use
     * @param tokens List of token IDs to decode
     * @return Decoded text
     */
    suspend fun decodeTokens(
        config: TokenizerConfig,
        tokens: List<Int>
    ): String
    
    /**
     * Export tokenizer configuration to the specified format.
     * 
     * @param config Tokenizer configuration to export
     * @param format Export format (JSON, BINARY)
     * @return Serialized configuration data
     */
    suspend fun exportConfig(
        config: TokenizerConfig,
        format: ExportFormat
    ): String
    
    /**
     * Load tokenizer configuration from serialized data.
     * 
     * @param configData Serialized configuration data
     * @param format Format of the configuration data
     * @return Loaded tokenizer configuration
     */
    suspend fun loadConfig(
        configData: String,
        format: ExportFormat
    ): TokenizerConfig
    
    /**
     * Check if the container is healthy and ready to process requests.
     * 
     * @return true if container is healthy, false otherwise
     */
    suspend fun isHealthy(): Boolean
    
    /**
     * Get the container name for identification purposes.
     * 
     * @return Container name
     */
    fun getContainerName(): String
    
    /**
     * Start the container if it's not already running.
     */
    suspend fun start()
    
    /**
     * Stop the container and clean up resources.
     */
    suspend fun stop()
}