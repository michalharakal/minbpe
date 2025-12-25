package sk.ainet.tokenizer.kotlin.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.types.int
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import sk.ainet.nlp.tools.tokenizer.*
import sk.ainet.nlp.tools.tokenizer.persistence.TokenizerData
import java.io.File
import kotlin.system.exitProcess

@Serializable
data class CLIResult(
    val status: String,
    val command: String,
    val implementation: String = "kotlin",
    val error: String? = null,
    val traceback: String? = null,
    val tokenizer_type: String? = null,
    val vocab_size: Int? = null,
    val input_length: Int? = null,
    val token_count: Int? = null,
    val tokens: List<Int>? = null,
    val text: String? = null,
    val output_file: String? = null,
    val config_file: String? = null,
    val round_trip_test: Boolean? = null,
    val test_tokens: Int? = null,
    val available_tokenizers: List<String>? = null
)

class KotlinCLI : CliktCommand(
    name = "kotlin-tokenizer-cli",
    help = "Kotlin minbpe CLI for comparison tool"
) {
    override fun run() {
        // Main CLI entry point - subcommands handle actual work
    }
}

class TrainCommand : CliktCommand(name = "train", help = "Train a tokenizer") {
    private val text by option("--text", help = "Training text or file path").required()
    private val vocabSize by option("--vocab-size", help = "Vocabulary size").int().required()
    private val type by option("--type", help = "Tokenizer type").required()
    private val output by option("--output", help = "Output config file").required()
    
    override fun run() {
        try {
            echo("Training $type tokenizer with vocab size $vocabSize", err = true)
            
            // Read training text
            val trainingText = if (File(text).exists()) {
                File(text).readText()
            } else {
                text
            }
            
            // Create tokenizer
            val tokenizer = createTokenizer(type)
            
            // GPT4 tokenizer is pretrained, cannot be trained
            if (type == "gpt4") {
                echo("GPT4 tokenizer is pretrained, skipping training", err = true)
            } else {
                tokenizer.train(trainingText, vocabSize, verbose = false)
            }
            
            // Export configuration
            val config = exportTokenizerConfig(tokenizer, type)
            
            // Save configuration
            File(output).writeText(Json.encodeToString(config))
            
            echo("Tokenizer configuration saved to $output", err = true)
            
            val result = CLIResult(
                status = "success",
                command = "train",
                tokenizer_type = type,
                vocab_size = tokenizer.vocab.size,
                output_file = output
            )
            
            println(Json.encodeToString(result))
            
        } catch (e: Exception) {
            val result = CLIResult(
                status = "error",
                command = "train",
                error = e.message ?: "Unknown error",
                traceback = e.stackTraceToString()
            )
            
            println(Json.encodeToString(result))
            exitProcess(1)
        }
    }
}

class EncodeCommand : CliktCommand(name = "encode", help = "Encode text") {
    private val config by option("--config", help = "Tokenizer config file").required()
    private val text by option("--text", help = "Text to encode or file path").required()
    private val output by option("--output", help = "Output tokens file").required()
    
    override fun run() {
        try {
            echo("Encoding text using config from $config", err = true)
            
            // Load tokenizer configuration
            val configData = Json.decodeFromString<TokenizerConfigData>(File(config).readText())
            val tokenizer = loadTokenizerFromConfig(configData)
            
            // Read input text
            val inputText = if (File(text).exists()) {
                File(text).readText()
            } else {
                text
            }
            
            // Encode text
            val tokens = tokenizer.encode(inputText)
            
            // Save tokens
            File(output).writeText(Json.encodeToString(tokens))
            
            echo("Encoded ${inputText.length} characters to ${tokens.size} tokens", err = true)
            
            val result = CLIResult(
                status = "success",
                command = "encode",
                input_length = inputText.length,
                token_count = tokens.size,
                tokens = tokens,
                output_file = output
            )
            
            println(Json.encodeToString(result))
            
        } catch (e: Exception) {
            val result = CLIResult(
                status = "error",
                command = "encode",
                error = e.message ?: "Unknown error",
                traceback = e.stackTraceToString()
            )
            
            println(Json.encodeToString(result))
            exitProcess(1)
        }
    }
}

class DecodeCommand : CliktCommand(name = "decode", help = "Decode tokens") {
    private val config by option("--config", help = "Tokenizer config file").required()
    private val tokens by option("--tokens", help = "Tokens JSON or file path").required()
    private val output by option("--output", help = "Output text file").required()
    
    override fun run() {
        try {
            echo("Decoding tokens using config from $config", err = true)
            
            // Load tokenizer configuration
            val configData = Json.decodeFromString<TokenizerConfigData>(File(config).readText())
            val tokenizer = loadTokenizerFromConfig(configData)
            
            // Read tokens
            val tokenList = if (File(tokens).exists()) {
                Json.decodeFromString<List<Int>>(File(tokens).readText())
            } else {
                Json.decodeFromString<List<Int>>(tokens)
            }
            
            // Decode tokens
            val decodedText = tokenizer.decode(tokenList)
            
            // Save decoded text
            File(output).writeText(decodedText)
            
            echo("Decoded ${tokenList.size} tokens to ${decodedText.length} characters", err = true)
            
            val result = CLIResult(
                status = "success",
                command = "decode",
                token_count = tokenList.size,
                output_length = decodedText.length,
                text = decodedText,
                output_file = output
            )
            
            println(Json.encodeToString(result))
            
        } catch (e: Exception) {
            val result = CLIResult(
                status = "error",
                command = "decode",
                error = e.message ?: "Unknown error",
                traceback = e.stackTraceToString()
            )
            
            println(Json.encodeToString(result))
            exitProcess(1)
        }
    }
}

class ExportCommand : CliktCommand(name = "export", help = "Export tokenizer configuration") {
    private val config by option("--config", help = "Tokenizer config file").required()
    private val output by option("--output", help = "Output config file").required()
    private val vocab by option("--vocab", help = "Also export vocabulary").flag()
    
    override fun run() {
        try {
            echo("Exporting tokenizer config from $config", err = true)
            
            // Load tokenizer configuration
            val configData = Json.decodeFromString<TokenizerConfigData>(File(config).readText())
            val tokenizer = loadTokenizerFromConfig(configData)
            
            // Export vocabulary if requested
            if (vocab) {
                val vocabData = mutableMapOf<Int, String>()
                for ((id, bytes) in tokenizer.vocab) {
                    try {
                        vocabData[id] = bytes.decodeToString()
                    } catch (e: Exception) {
                        vocabData[id] = bytes.contentToString()
                    }
                }
                
                val vocabFile = output.replace(".json", "_vocab.json")
                File(vocabFile).writeText(Json.encodeToString(vocabData))
            }
            
            // Save enhanced configuration
            val enhancedConfig = exportTokenizerConfig(tokenizer, configData.type)
            File(output).writeText(Json.encodeToString(enhancedConfig))
            
            echo("Configuration exported to $output", err = true)
            
            val result = CLIResult(
                status = "success",
                command = "export",
                output_file = output,
                vocab_size = tokenizer.vocab.size
            )
            
            println(Json.encodeToString(result))
            
        } catch (e: Exception) {
            val result = CLIResult(
                status = "error",
                command = "export",
                error = e.message ?: "Unknown error",
                traceback = e.stackTraceToString()
            )
            
            println(Json.encodeToString(result))
            exitProcess(1)
        }
    }
}

class LoadCommand : CliktCommand(name = "load", help = "Load and validate tokenizer configuration") {
    private val config by option("--config", help = "Tokenizer config file").required()
    
    override fun run() {
        try {
            echo("Loading and validating tokenizer config from $config", err = true)
            
            // Load and validate configuration
            val configData = Json.decodeFromString<TokenizerConfigData>(File(config).readText())
            val tokenizer = loadTokenizerFromConfig(configData)
            
            // Test basic functionality
            val testText = "Hello, world! This is a test."
            val tokens = tokenizer.encode(testText)
            val decodedText = tokenizer.decode(tokens)
            
            val roundTripSuccess = testText == decodedText
            
            val result = CLIResult(
                status = "success",
                command = "load",
                config_file = config,
                tokenizer_type = configData.type,
                vocab_size = tokenizer.vocab.size,
                round_trip_test = roundTripSuccess,
                test_tokens = tokens.size
            )
            
            println(Json.encodeToString(result))
            
        } catch (e: Exception) {
            val result = CLIResult(
                status = "error",
                command = "load",
                error = e.message ?: "Unknown error",
                traceback = e.stackTraceToString()
            )
            
            println(Json.encodeToString(result))
            exitProcess(1)
        }
    }
}

class HealthCommand : CliktCommand(name = "health", help = "Health check") {
    override fun run() {
        val result = CLIResult(
            status = "healthy",
            command = "health",
            available_tokenizers = listOf("basic", "regex", "gpt4")
        )
        
        println(Json.encodeToString(result))
    }
}

@Serializable
data class TokenizerConfigData(
    val type: String,
    val vocab_size: Int,
    val merges: List<Pair<Pair<Int, Int>, Int>>,
    val special_tokens: Map<String, Int> = emptyMap(),
    val pattern: String = "",
    val metadata: Map<String, String> = emptyMap(),
    val byte_shuffle: Map<String, Int>? = null
)

fun createTokenizer(tokenizerType: String): Tokenizer {
    return when (tokenizerType) {
        "basic" -> BasicTokenizer()
        "regex" -> RegexTokenizer()
        "gpt4" -> GPT4Tokenizer()
        else -> throw IllegalArgumentException("Unknown tokenizer type: $tokenizerType")
    }
}

fun exportTokenizerConfig(tokenizer: Tokenizer, tokenizerType: String): TokenizerConfigData {
    val mergesList = tokenizer.merges.entries.map { (pair, id) -> Pair(pair, id) }
    
    return TokenizerConfigData(
        type = tokenizerType,
        vocab_size = tokenizer.vocab.size,
        merges = mergesList,
        special_tokens = tokenizer.specialTokens,
        pattern = tokenizer.pattern,
        metadata = mapOf(
            "implementation" to "kotlin",
            "version" to "1.0"
        )
    )
}

fun loadTokenizerFromConfig(config: TokenizerConfigData): Tokenizer {
    val tokenizer = createTokenizer(config.type)
    
    // Create TokenizerData for loading
    val tokenizerData = TokenizerData(
        version = "minbpe v1",
        type = config.type,
        pattern = config.pattern,
        specialTokens = config.special_tokens,
        merges = config.merges
    )
    
    // Load the tokenizer state
    tokenizer.loadFromData(tokenizerData)
    
    return tokenizer
}

fun main(args: Array<String>) = KotlinCLI()
    .subcommands(TrainCommand(), EncodeCommand(), DecodeCommand(), ExportCommand(), LoadCommand(), HealthCommand())
    .main(args)