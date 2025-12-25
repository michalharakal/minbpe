package sk.ainet.tokenizer.comparison.model

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertEquals

class ValidationUtilsTest {
    
    @Test
    fun `validateTokenizerConfig accepts valid configuration`() {
        val validConfig = TokenizerConfig(
            type = TokenizerType.BASIC,
            vocabSize = 512,
            merges = listOf(Pair(Pair(1, 2), 256)),
            specialTokens = mapOf("<pad>" to 0),
            pattern = "\\w+"
        )
        
        val result = ValidationUtils.validateTokenizerConfig(validConfig)
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun `validateTokenizerConfig rejects negative vocabulary size`() {
        val invalidConfig = TokenizerConfig(
            type = TokenizerType.BASIC,
            vocabSize = -1,
            merges = emptyList()
        )
        
        val result = ValidationUtils.validateTokenizerConfig(invalidConfig)
        assertTrue(result.isFailure)
        assertTrue((result as ValidationResult.Failure).errors.any { 
            it.contains("Vocabulary size must be positive") 
        })
    }
    
    @Test
    fun `validateTokenizerConfig rejects negative merge IDs`() {
        val invalidConfig = TokenizerConfig(
            type = TokenizerType.REGEX,
            vocabSize = 512,
            merges = listOf(Pair(Pair(-1, 2), 256))
        )
        
        val result = ValidationUtils.validateTokenizerConfig(invalidConfig)
        assertTrue(result.isFailure)
        assertTrue((result as ValidationResult.Failure).errors.any { 
            it.contains("negative token ID") 
        })
    }
    
    @Test
    fun `validateTokenizerConfig rejects empty special tokens`() {
        val invalidConfig = TokenizerConfig(
            type = TokenizerType.BASIC,
            vocabSize = 512,
            merges = emptyList(),
            specialTokens = mapOf("" to 0)
        )
        
        val result = ValidationUtils.validateTokenizerConfig(invalidConfig)
        assertTrue(result.isFailure)
        assertTrue((result as ValidationResult.Failure).errors.any { 
            it.contains("Special token cannot be empty") 
        })
    }
    
    @Test
    fun `validateCrossPlatformCompatibility accepts compatible configuration`() {
        val compatibleConfig = TokenizerConfig(
            type = TokenizerType.BASIC,
            vocabSize = 512,
            merges = emptyList(),
            metadata = mapOf("version" to "1.0", "model" to "basic")
        )
        
        val result = ValidationUtils.validateCrossPlatformCompatibility(compatibleConfig)
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun `validateCrossPlatformCompatibility warns about unicode properties`() {
        val problematicConfig = TokenizerConfig(
            type = TokenizerType.REGEX,
            vocabSize = 512,
            merges = emptyList(),
            pattern = "\\p{L}+"
        )
        
        val result = ValidationUtils.validateCrossPlatformCompatibility(problematicConfig)
        assertTrue(result.isFailure)
        assertTrue((result as ValidationResult.Failure).errors.any { 
            it.contains("Unicode property classes") 
        })
    }
    
    @Test
    fun `serializeTokenizerConfig produces valid JSON`() {
        val config = TokenizerConfig(
            type = TokenizerType.BASIC,
            vocabSize = 256,
            merges = listOf(Pair(Pair(1, 2), 256))
        )
        
        val jsonString = ValidationUtils.serializeTokenizerConfig(config)
        assertTrue(jsonString.contains("\"type\""))
        assertTrue(jsonString.contains("\"vocabSize\""))
        assertTrue(jsonString.contains("\"merges\""))
    }
    
    @Test
    fun `deserializeTokenizerConfig handles valid JSON`() {
        val config = TokenizerConfig(
            type = TokenizerType.BASIC,
            vocabSize = 256,
            merges = listOf(Pair(Pair(1, 2), 256)),
            specialTokens = emptyMap(),
            pattern = "",
            metadata = emptyMap()
        )
        
        // First serialize a known good config
        val jsonString = ValidationUtils.serializeTokenizerConfig(config)
        println("Generated JSON: $jsonString")
        
        val result = ValidationUtils.deserializeTokenizerConfig(jsonString)
        assertTrue(result.isSuccess, "Deserialization should succeed, but got: ${result.exceptionOrNull()}")
        
        val deserializedConfig = result.getOrThrow()
        assertEquals(TokenizerType.BASIC, deserializedConfig.type)
        assertEquals(256, deserializedConfig.vocabSize)
        assertEquals(1, deserializedConfig.merges.size)
    }
    
    @Test
    fun `deserializeTokenizerConfig handles invalid JSON`() {
        val invalidJson = "{ invalid json }"
        
        val result = ValidationUtils.deserializeTokenizerConfig(invalidJson)
        assertTrue(result.isFailure)
    }
}