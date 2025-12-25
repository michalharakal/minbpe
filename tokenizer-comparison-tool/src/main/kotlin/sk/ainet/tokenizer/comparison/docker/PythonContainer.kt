package sk.ainet.tokenizer.comparison.docker

import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import sk.ainet.tokenizer.comparison.model.TokenizerConfig
import sk.ainet.tokenizer.comparison.model.TokenizerType
import sk.ainet.tokenizer.comparison.model.ExportFormat
import java.io.File
import java.util.UUID

/**
 * Docker container implementation for the Python minbpe tokenizer.
 * Provides access to the original Python implementation via containerized CLI.
 */
class PythonContainer : ContainerInterface {
    
    companion object {
        private const val CONTAINER_NAME = "minbpe-python"
        private const val IMAGE_NAME = "minbpe-python:latest"
        private const val SHARED_VOLUME = "/shared"
    }
    
    private val json = Json { 
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    
    override suspend fun trainTokenizer(
        text: String,
        vocabSize: Int,
        type: TokenizerType
    ): TokenizerConfig {
        val sessionId = UUID.randomUUID().toString()
        val inputFile = "input_${sessionId}.txt"
        val outputFile = "config_${sessionId}.json"
        
        try {
            // Write input text to shared volume
            writeToSharedVolume(inputFile, text)
            
            // Execute training command
            val command = listOf(
                "python", "/app/cli.py", "train",
                "--input", "$SHARED_VOLUME/$inputFile",
                "--output", "$SHARED_VOLUME/$outputFile",
                "--vocab-size", vocabSize.toString(),
                "--type", type.name.lowercase()
            )
            
            val result = executeInContainer(CONTAINER_NAME, command, timeoutSeconds = 300)
            
            if (result.exitCode != 0) {
                throw RuntimeException("Training failed: ${result.error}")
            }
            
            // Read the generated configuration
            val configJson = readFromSharedVolume(outputFile)
            return json.decodeFromString<TokenizerConfig>(configJson)
            
        } finally {
            // Clean up temporary files
            cleanupSharedFile(inputFile)
            cleanupSharedFile(outputFile)
        }
    }
    
    override suspend fun encodeText(
        config: TokenizerConfig,
        text: String
    ): List<Int> {
        val sessionId = UUID.randomUUID().toString()
        val configFile = "config_${sessionId}.json"
        val inputFile = "input_${sessionId}.txt"
        val outputFile = "tokens_${sessionId}.json"
        
        try {
            // Write configuration and input text
            writeToSharedVolume(configFile, json.encodeToString(config))
            writeToSharedVolume(inputFile, text)
            
            // Execute encoding command
            val command = listOf(
                "python", "/app/cli.py", "encode",
                "--config", "$SHARED_VOLUME/$configFile",
                "--input", "$SHARED_VOLUME/$inputFile",
                "--output", "$SHARED_VOLUME/$outputFile"
            )
            
            val result = executeInContainer(CONTAINER_NAME, command)
            
            if (result.exitCode != 0) {
                throw RuntimeException("Encoding failed: ${result.error}")
            }
            
            // Read the generated tokens
            val tokensJson = readFromSharedVolume(outputFile)
            return json.decodeFromString<List<Int>>(tokensJson)
            
        } finally {
            // Clean up temporary files
            cleanupSharedFile(configFile)
            cleanupSharedFile(inputFile)
            cleanupSharedFile(outputFile)
        }
    }
    
    override suspend fun decodeTokens(
        config: TokenizerConfig,
        tokens: List<Int>
    ): String {
        val sessionId = UUID.randomUUID().toString()
        val configFile = "config_${sessionId}.json"
        val tokensFile = "tokens_${sessionId}.json"
        val outputFile = "output_${sessionId}.txt"
        
        try {
            // Write configuration and tokens
            writeToSharedVolume(configFile, json.encodeToString(config))
            writeToSharedVolume(tokensFile, json.encodeToString(tokens))
            
            // Execute decoding command
            val command = listOf(
                "python", "/app/cli.py", "decode",
                "--config", "$SHARED_VOLUME/$configFile",
                "--tokens", "$SHARED_VOLUME/$tokensFile",
                "--output", "$SHARED_VOLUME/$outputFile"
            )
            
            val result = executeInContainer(CONTAINER_NAME, command)
            
            if (result.exitCode != 0) {
                throw RuntimeException("Decoding failed: ${result.error}")
            }
            
            // Read the decoded text
            return readFromSharedVolume(outputFile)
            
        } finally {
            // Clean up temporary files
            cleanupSharedFile(configFile)
            cleanupSharedFile(tokensFile)
            cleanupSharedFile(outputFile)
        }
    }
    
    override suspend fun exportConfig(
        config: TokenizerConfig,
        format: ExportFormat
    ): String {
        return when (format) {
            ExportFormat.JSON -> json.encodeToString(config)
            ExportFormat.BINARY -> throw UnsupportedOperationException("Binary export not yet implemented")
        }
    }
    
    override suspend fun loadConfig(
        configData: String,
        format: ExportFormat
    ): TokenizerConfig {
        return when (format) {
            ExportFormat.JSON -> json.decodeFromString<TokenizerConfig>(configData)
            ExportFormat.BINARY -> throw UnsupportedOperationException("Binary import not yet implemented")
        }
    }
    
    override suspend fun isHealthy(): Boolean {
        return try {
            if (!isContainerRunning(CONTAINER_NAME)) {
                false
            } else {
                // Test basic CLI functionality
                val result = executeInContainer(CONTAINER_NAME, listOf("python", "/app/cli.py", "--help"))
                result.exitCode == 0
            }
        } catch (e: Exception) {
            false
        }
    }
    
    override fun getContainerName(): String = CONTAINER_NAME
    
    override suspend fun start() {
        if (!containerExists(CONTAINER_NAME)) {
            // Create and start container if it doesn't exist
            val createCommand = listOf(
                "docker", "run", "-d",
                "--name", CONTAINER_NAME,
                "-v", "${getSharedVolumeHost()}:$SHARED_VOLUME",
                IMAGE_NAME,
                "tail", "-f", "/dev/null"  // Keep container running
            )
            
            val result = executeCommand(createCommand)
            if (result.exitCode != 0) {
                throw RuntimeException("Failed to create Python container: ${result.error}")
            }
        } else if (!isContainerRunning(CONTAINER_NAME)) {
            // Start existing container
            if (!startContainer(CONTAINER_NAME)) {
                throw RuntimeException("Failed to start Python container")
            }
        }
    }
    
    override suspend fun stop() {
        if (isContainerRunning(CONTAINER_NAME)) {
            stopContainer(CONTAINER_NAME)
        }
    }
    
    private fun getSharedVolumeHost(): String {
        // Use a temporary directory on the host for shared volume
        val tempDir = System.getProperty("java.io.tmpdir")
        val sharedDir = File(tempDir, "tokenizer-comparison-shared")
        if (!sharedDir.exists()) {
            sharedDir.mkdirs()
        }
        return sharedDir.absolutePath
    }
    
    private suspend fun writeToSharedVolume(filename: String, content: String) {
        val sharedDir = File(getSharedVolumeHost())
        val file = File(sharedDir, filename)
        file.writeText(content)
    }
    
    private suspend fun readFromSharedVolume(filename: String): String {
        val sharedDir = File(getSharedVolumeHost())
        val file = File(sharedDir, filename)
        return file.readText()
    }
    
    private suspend fun cleanupSharedFile(filename: String) {
        try {
            val sharedDir = File(getSharedVolumeHost())
            val file = File(sharedDir, filename)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }
}