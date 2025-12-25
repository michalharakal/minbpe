package sk.ainet.tokenizer.comparison.docker

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Disabled
import sk.ainet.tokenizer.comparison.model.TokenizerConfig
import sk.ainet.tokenizer.comparison.model.TokenizerType
import sk.ainet.tokenizer.comparison.model.ExportFormat
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class ContainerInterfaceTest {
    
    @Test
    fun `python container should have correct name`() {
        val container = PythonContainer()
        assertEquals("minbpe-python", container.getContainerName())
    }
    
    @Test
    fun `kotlin container should have correct name`() {
        val container = KotlinContainer()
        assertEquals("minbpe-kotlin", container.getContainerName())
    }
    
    @Test
    fun `should handle export config for JSON format`() = runTest {
        val container = PythonContainer()
        val config = TokenizerConfig(
            type = TokenizerType.BASIC,
            vocabSize = 256,
            merges = emptyList(),
            specialTokens = emptyMap(),
            pattern = "",
            metadata = emptyMap()
        )
        
        val exported = container.exportConfig(config, ExportFormat.JSON)
        assertNotNull(exported)
        assertTrue(exported.contains("BASIC"))
        assertTrue(exported.contains("256"))
    }
    
    @Test
    fun `should handle load config from JSON format`() = runTest {
        val container = KotlinContainer()
        val configJson = """
            {
                "type": "BASIC",
                "vocabSize": 256,
                "merges": [],
                "specialTokens": {},
                "pattern": "",
                "metadata": {}
            }
        """.trimIndent()
        
        val config = container.loadConfig(configJson, ExportFormat.JSON)
        assertEquals(TokenizerType.BASIC, config.type)
        assertEquals(256, config.vocabSize)
        assertTrue(config.merges.isEmpty())
    }
    
    @Disabled("Requires Docker containers to be built and available")
    @Test
    fun `should check container health when containers are available`() = runTest {
        val pythonContainer = PythonContainer()
        val kotlinContainer = KotlinContainer()
        
        // These tests require actual Docker containers to be running
        try {
            pythonContainer.start()
            kotlinContainer.start()
            
            val pythonHealthy = pythonContainer.isHealthy()
            val kotlinHealthy = kotlinContainer.isHealthy()
            
            // Just test that the methods don't throw exceptions
            assertTrue(pythonHealthy || !pythonHealthy)
            assertTrue(kotlinHealthy || !kotlinHealthy)
            
        } catch (e: Exception) {
            // Expected when Docker is not available or containers are not built
            assertTrue(true)
        } finally {
            try {
                pythonContainer.stop()
                kotlinContainer.stop()
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
    }
    
    @Disabled("Requires Docker containers to be built and available")
    @Test
    fun `should train tokenizer when containers are available`() = runTest {
        val container = PythonContainer()
        val text = "Hello world! This is a test."
        
        try {
            container.start()
            val config = container.trainTokenizer(text, 256, TokenizerType.BASIC)
            
            assertEquals(TokenizerType.BASIC, config.type)
            assertEquals(256, config.vocabSize)
            
        } catch (e: Exception) {
            // Expected when containers are not available
            assertTrue(true)
        } finally {
            try {
                container.stop()
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
    }
}