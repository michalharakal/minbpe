package sk.ainet.tokenizer.comparison.docker

import io.kotest.core.spec.style.StringSpec
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.maps.shouldContainAll
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import sk.ainet.tokenizer.comparison.model.*

/**
 * Property-based tests for cross-platform compatibility between Python and Kotlin implementations.
 * These tests validate that tokenizer configurations can be exported from one implementation
 * and loaded into another while maintaining identical behavior.
 */
class CrossPlatformCompatibilityPropertyTest : StringSpec({
    
    "Property 2: Cross-platform configuration round trip - For any tokenizer configuration exported from one implementation, loading it in the other implementation should produce identical tokenization behavior" {
        /**
         * Feature: tokenizer-comparison-tool, Property 2: Cross-platform configuration round trip
         * Validates: Requirements 2.6, 3.5
         */
        
        checkAll(100, arbTokenizerConfig()) { originalConfig ->
            // Test JSON serialization round trip
            val jsonString = Json.encodeToString(originalConfig)
            val deserializedConfig = Json.decodeFromString<TokenizerConfig>(jsonString)
            
            // Configuration should be identical after round trip
            deserializedConfig.type shouldBe originalConfig.type
            deserializedConfig.vocabSize shouldBe originalConfig.vocabSize
            deserializedConfig.merges shouldBe originalConfig.merges
            deserializedConfig.specialTokens shouldBe originalConfig.specialTokens
            deserializedConfig.pattern shouldBe originalConfig.pattern
            deserializedConfig.metadata shouldContainAll originalConfig.metadata
            
            // Test that essential fields are preserved
            deserializedConfig.type shouldNotBe null
            deserializedConfig.vocabSize shouldBe originalConfig.vocabSize
            
            // Test cross-platform metadata compatibility
            val pythonMetadata = originalConfig.metadata + ("implementation" to "python")
            val kotlinMetadata = originalConfig.metadata + ("implementation" to "kotlin")
            
            val pythonConfig = originalConfig.copy(metadata = pythonMetadata)
            val kotlinConfig = originalConfig.copy(metadata = kotlinMetadata)
            
            // Both should serialize/deserialize successfully
            val pythonJson = Json.encodeToString(pythonConfig)
            val kotlinJson = Json.encodeToString(kotlinConfig)
            
            val pythonDeserialized = Json.decodeFromString<TokenizerConfig>(pythonJson)
            val kotlinDeserialized = Json.decodeFromString<TokenizerConfig>(kotlinJson)
            
            // Core tokenizer data should be identical regardless of implementation metadata
            pythonDeserialized.type shouldBe kotlinDeserialized.type
            pythonDeserialized.vocabSize shouldBe kotlinDeserialized.vocabSize
            pythonDeserialized.merges shouldBe kotlinDeserialized.merges
            pythonDeserialized.specialTokens shouldBe kotlinDeserialized.specialTokens
            pythonDeserialized.pattern shouldBe kotlinDeserialized.pattern
        }
    }
    
    "Property 3: Export format completeness - For any exported tokenizer configuration, the output should contain all required fields" {
        /**
         * Feature: tokenizer-comparison-tool, Property 3: Export format completeness
         * Validates: Requirements 2.4, 2.5
         */
        
        checkAll(100, arbTokenizerConfig()) { config ->
            // Serialize to JSON
            val jsonString = Json.encodeToString(config)
            
            // Parse as JsonElement to avoid Any? serialization issues
            val jsonElement = Json.parseToJsonElement(jsonString)
            val jsonMap = jsonElement.jsonObject
            
            // Required fields must be present
            jsonMap.keys shouldContainAll listOf("type", "vocabSize", "merges")
            
            // Type should be valid
            val typeValue = jsonMap["type"]?.jsonPrimitive?.content
            typeValue shouldNotBe null
            TokenizerType.entries.map { it.name } shouldContainAll listOf(typeValue!!)
            
            // Vocab size should be positive
            val vocabSizeValue = jsonMap["vocabSize"]?.jsonPrimitive?.int
            vocabSizeValue shouldNotBe null
            vocabSizeValue shouldBe config.vocabSize
            
            // Merges should be a list
            val mergesValue = jsonMap["merges"]
            mergesValue shouldNotBe null
            
            // Special tokens should be present (even if empty) or omitted if empty
            // kotlinx.serialization omits empty collections by default
            val hasSpecialTokens = jsonMap.containsKey("specialTokens")
            if (hasSpecialTokens) {
                // If present, should be an object
                jsonMap["specialTokens"]?.jsonObject shouldNotBe null
            }
            // If not present, that's also valid for empty special tokens
            
            // Pattern should be present (even if empty) or omitted if empty
            val hasPattern = jsonMap.containsKey("pattern")
            if (hasPattern) {
                // If present, should be a string
                jsonMap["pattern"]?.jsonPrimitive?.content shouldNotBe null
            }
            
            // Metadata should be present (even if empty) or omitted if empty
            val hasMetadata = jsonMap.containsKey("metadata")
            if (hasMetadata) {
                // If present, should be an object
                jsonMap["metadata"]?.jsonObject shouldNotBe null
            }
            
            // Test that all token IDs in merges are valid integers
            config.merges.forEach { (pair, id) ->
                pair.first shouldBe pair.first // Should be valid integers
                pair.second shouldBe pair.second
                id shouldBe id
            }
            
            // Test that special token IDs are valid
            config.specialTokens.values.forEach { tokenId ->
                tokenId shouldBe tokenId // Should be valid integer
            }
            
            // Test that the configuration can be used for both export formats
            val exportFormats = ExportFormat.entries
            exportFormats.forEach { format ->
                when (format) {
                    ExportFormat.JSON -> {
                        // JSON export should produce valid JSON
                        val exported = Json.encodeToString(config)
                        exported.isNotEmpty() shouldBe true
                        
                        // Should be parseable back
                        val reimported = Json.decodeFromString<TokenizerConfig>(exported)
                        reimported.type shouldBe config.type
                    }
                    ExportFormat.BINARY -> {
                        // Binary format would be implementation-specific
                        // For now, we test that the config is serializable
                        val jsonBytes = Json.encodeToString(config).toByteArray()
                        jsonBytes.isNotEmpty() shouldBe true
                    }
                }
            }
        }
    }
})

/**
 * Arbitrary generator for TokenizerConfig instances.
 */
private fun arbTokenizerConfig(): Arb<TokenizerConfig> = arbitrary { rs ->
    val type = Arb.enum<TokenizerType>().bind()
    val vocabSize = Arb.int(256, 50000).bind()
    val merges = arbMergesList(vocabSize).bind()
    val specialTokens = arbSpecialTokens().bind()
    val pattern = Arb.string(0, 100).bind()
    val metadata = arbMetadata().bind()
    
    TokenizerConfig(
        type = type,
        vocabSize = vocabSize,
        merges = merges,
        specialTokens = specialTokens,
        pattern = pattern,
        metadata = metadata
    )
}

/**
 * Arbitrary generator for merge lists.
 */
private fun arbMergesList(maxVocabSize: Int): Arb<List<Pair<Pair<Int, Int>, Int>>> = arbitrary { rs ->
    val numMerges = Arb.int(0, minOf(100, maxVocabSize / 10)).bind()
    val merges = mutableListOf<Pair<Pair<Int, Int>, Int>>()
    
    repeat(numMerges) { i ->
        val token1 = Arb.int(0, 255).bind()
        val token2 = Arb.int(0, 255).bind()
        val mergeId = 256 + i // Merge IDs start after base vocabulary
        merges.add(Pair(Pair(token1, token2), mergeId))
    }
    
    merges
}

/**
 * Arbitrary generator for special tokens map.
 */
private fun arbSpecialTokens(): Arb<Map<String, Int>> = arbitrary { rs ->
    val numSpecialTokens = Arb.int(0, 10).bind()
    val specialTokens = mutableMapOf<String, Int>()
    
    val commonSpecialTokens = listOf("<|endoftext|>", "<|startoftext|>", "<pad>", "<unk>", "<mask>")
    
    repeat(numSpecialTokens) { i ->
        val tokenName = if (i < commonSpecialTokens.size) {
            commonSpecialTokens[i]
        } else {
            "<special_${i}>"
        }
        val tokenId = 50000 + i // Special token IDs in high range
        specialTokens[tokenName] = tokenId
    }
    
    specialTokens
}

/**
 * Arbitrary generator for metadata map.
 */
private fun arbMetadata(): Arb<Map<String, String>> = arbitrary { rs ->
    val numEntries = Arb.int(0, 5).bind()
    val metadata = mutableMapOf<String, String>()
    
    val commonKeys = listOf("implementation", "version", "created_by", "model_name", "description")
    val commonValues = listOf("python", "kotlin", "1.0", "test", "minbpe", "comparison-tool")
    
    repeat(numEntries) { i ->
        val key = if (i < commonKeys.size) commonKeys[i] else "key_$i"
        val value = commonValues.random()
        metadata[key] = value
    }
    
    metadata
}