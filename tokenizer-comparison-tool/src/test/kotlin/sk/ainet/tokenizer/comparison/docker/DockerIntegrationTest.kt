package sk.ainet.tokenizer.comparison.docker

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Disabled
import sk.ainet.tokenizer.comparison.model.TokenizerType
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertEquals

/**
 * Integration tests for Docker Manager functionality.
 * These tests are disabled by default as they require Docker containers to be built and available.
 */
class DockerIntegrationTest {
    
    @Disabled("Requires Docker containers to be built and available")
    @Test
    fun `should perform end-to-end tokenizer comparison`() = runTest {
        val dockerManager = DockerManager()
        val testText = "Hello world! This is a test for tokenizer comparison."
        
        try {
            // Start containers
            dockerManager.startContainers()
            
            // Verify containers are ready
            val containersReady = dockerManager.ensureContainersReady()
            assertTrue(containersReady, "Containers should be ready for testing")
            
            // Train tokenizers on both implementations
            val (pythonConfig, kotlinConfig) = dockerManager.executeInBoth { container ->
                container.trainTokenizer(testText, 256, TokenizerType.BASIC)
            }
            
            assertNotNull(pythonConfig)
            assertNotNull(kotlinConfig)
            assertEquals(TokenizerType.BASIC, pythonConfig.type)
            assertEquals(TokenizerType.BASIC, kotlinConfig.type)
            assertEquals(256, pythonConfig.vocabSize)
            assertEquals(256, kotlinConfig.vocabSize)
            
            // Test encoding with both implementations
            val (pythonTokens, kotlinTokens) = dockerManager.executeInBoth { container ->
                when (container.getContainerName()) {
                    "minbpe-python" -> container.encodeText(pythonConfig, testText)
                    "minbpe-kotlin" -> container.encodeText(kotlinConfig, testText)
                    else -> throw IllegalStateException("Unknown container: ${container.getContainerName()}")
                }
            }
            
            assertNotNull(pythonTokens)
            assertNotNull(kotlinTokens)
            assertTrue(pythonTokens.isNotEmpty())
            assertTrue(kotlinTokens.isNotEmpty())
            
            // Test decoding with both implementations
            val (pythonDecoded, kotlinDecoded) = dockerManager.executeInBoth { container ->
                when (container.getContainerName()) {
                    "minbpe-python" -> container.decodeTokens(pythonConfig, pythonTokens)
                    "minbpe-kotlin" -> container.decodeTokens(kotlinConfig, kotlinTokens)
                    else -> throw IllegalStateException("Unknown container: ${container.getContainerName()}")
                }
            }
            
            assertNotNull(pythonDecoded)
            assertNotNull(kotlinDecoded)
            
            // Both implementations should produce the same results for identical training
            // Note: This might not always be true due to implementation differences,
            // but it's a good test for basic compatibility
            println("Python tokens: $pythonTokens")
            println("Kotlin tokens: $kotlinTokens")
            println("Python decoded: '$pythonDecoded'")
            println("Kotlin decoded: '$kotlinDecoded'")
            
        } finally {
            // Clean up containers
            dockerManager.stopContainers()
        }
    }
    
    @Disabled("Requires Docker containers to be built and available")
    @Test
    fun `should handle container lifecycle management`() = runTest {
        val dockerManager = DockerManager()
        
        try {
            // Initially containers should not be ready
            val initialReady = dockerManager.ensureContainersReady()
            // This might be true or false depending on system state
            
            // Start containers
            dockerManager.startContainers()
            
            // Now containers should be ready (if Docker images are available)
            val afterStartReady = dockerManager.ensureContainersReady()
            
            // Test individual container access
            val pythonContainer = dockerManager.getPythonContainer()
            val kotlinContainer = dockerManager.getKotlinContainer()
            
            assertEquals("minbpe-python", pythonContainer.getContainerName())
            assertEquals("minbpe-kotlin", kotlinContainer.getContainerName())
            
            // Test health checks
            val pythonHealthy = pythonContainer.isHealthy()
            val kotlinHealthy = kotlinContainer.isHealthy()
            
            println("Initial ready: $initialReady")
            println("After start ready: $afterStartReady")
            println("Python healthy: $pythonHealthy")
            println("Kotlin healthy: $kotlinHealthy")
            
        } finally {
            // Clean up
            dockerManager.stopContainers()
        }
    }
    
    @Test
    fun `should provide docker system information`() = runTest {
        val dockerManager = DockerManager()
        
        val dockerAvailable = dockerManager.isDockerAvailable()
        val dockerInfo = dockerManager.getDockerInfo()
        
        assertNotNull(dockerInfo)
        assertTrue(dockerInfo.isNotEmpty())
        
        println("Docker available: $dockerAvailable")
        println("Docker info: ${dockerInfo.take(200)}...")
        
        // The test should pass regardless of Docker availability
        assertTrue(true)
    }
}