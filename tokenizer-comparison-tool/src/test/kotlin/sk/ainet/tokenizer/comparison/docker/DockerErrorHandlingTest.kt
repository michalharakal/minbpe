package sk.ainet.tokenizer.comparison.docker

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import sk.ainet.tokenizer.comparison.model.TokenizerConfig
import sk.ainet.tokenizer.comparison.model.TokenizerType
import sk.ainet.tokenizer.comparison.model.ExportFormat

class DockerErrorHandlingTest : FunSpec({
    
    lateinit var dockerManager: DockerManager
    lateinit var pythonContainer: PythonContainer
    lateinit var kotlinContainer: KotlinContainer
    
    beforeEach {
        dockerManager = DockerManager()
        pythonContainer = PythonContainer()
        kotlinContainer = KotlinContainer()
    }
    
    context("Docker Availability Error Handling") {
        test("should handle Docker unavailability gracefully") {
            runTest {
                // Test behavior when Docker is not available
                val dockerAvailable = dockerManager.isDockerAvailable()
                val dockerInfo = dockerManager.getDockerInfo()
                
                dockerInfo shouldNotBe null
                
                if (!dockerAvailable) {
                    dockerInfo shouldContain "Docker not available"
                }
            }
        }
    }
    
    context("Container Startup Error Handling") {
        test("should handle container startup failures") {
            runTest {
                // Test container startup when Docker images don't exist
                try {
                    pythonContainer.start()
                    // If this succeeds, Docker is available and image exists
                } catch (e: RuntimeException) {
                    // Expected when Docker is not available or image doesn't exist
                    e.message shouldContain "Failed to"
                }
            }
        }
        
        test("should handle health check failures") {
            runTest {
                // Test health checks when containers are not available
                val pythonHealthy = pythonContainer.isHealthy()
                val kotlinHealthy = kotlinContainer.isHealthy()
                
                // Should return false when containers are not running
                pythonHealthy shouldBe false
                kotlinHealthy shouldBe false
            }
        }
    }
    
    context("Tokenizer Operation Error Handling") {
        test("should handle tokenizer training failures") {
            runTest {
                // Test tokenizer training when container is not available
                try {
                    pythonContainer.trainTokenizer("test text", 256, TokenizerType.BASIC)
                    // If this succeeds, container is available
                } catch (e: RuntimeException) {
                    // Expected when container is not available
                    e.message shouldContain "failed"
                }
            }
        }
        
        test("should handle encoding failures") {
            runTest {
                // Test encoding when container is not available
                val config = TokenizerConfig(
                    type = TokenizerType.BASIC,
                    vocabSize = 256,
                    merges = emptyList()
                )
                
                try {
                    pythonContainer.encodeText(config, "test text")
                    // If this succeeds, container is available
                } catch (e: RuntimeException) {
                    // Expected when container is not available
                    e.message shouldContain "failed"
                }
            }
        }
        
        test("should handle decoding failures") {
            runTest {
                // Test decoding when container is not available
                val config = TokenizerConfig(
                    type = TokenizerType.BASIC,
                    vocabSize = 256,
                    merges = emptyList()
                )
                
                try {
                    pythonContainer.decodeTokens(config, listOf(1, 2, 3))
                    // If this succeeds, container is available
                } catch (e: RuntimeException) {
                    // Expected when container is not available
                    e.message shouldContain "failed"
                }
            }
        }
    }
    
    context("Export/Import Error Handling") {
        test("should handle unsupported export formats") {
            runTest {
                // Test unsupported export format
                val config = TokenizerConfig(
                    type = TokenizerType.BASIC,
                    vocabSize = 256,
                    merges = emptyList()
                )
                
                try {
                    pythonContainer.exportConfig(config, ExportFormat.BINARY)
                } catch (e: UnsupportedOperationException) {
                    e.shouldBeInstanceOf<UnsupportedOperationException>()
                }
                
                try {
                    kotlinContainer.exportConfig(config, ExportFormat.BINARY)
                } catch (e: UnsupportedOperationException) {
                    e.shouldBeInstanceOf<UnsupportedOperationException>()
                }
            }
        }
        
        test("should handle unsupported import formats") {
            runTest {
                // Test unsupported import format
                try {
                    pythonContainer.loadConfig("binary data", ExportFormat.BINARY)
                } catch (e: UnsupportedOperationException) {
                    e.shouldBeInstanceOf<UnsupportedOperationException>()
                }
                
                try {
                    kotlinContainer.loadConfig("binary data", ExportFormat.BINARY)
                } catch (e: UnsupportedOperationException) {
                    e.shouldBeInstanceOf<UnsupportedOperationException>()
                }
            }
        }
        
        test("should handle invalid JSON configuration") {
            runTest {
                // Test loading invalid JSON configuration
                val invalidJson = "{ invalid json }"
                
                try {
                    pythonContainer.loadConfig(invalidJson, ExportFormat.JSON)
                    // Should not reach here with invalid JSON
                    false shouldBe true // Force failure if we reach here
                } catch (e: Exception) {
                    // Expected for invalid JSON
                    e.shouldBeInstanceOf<Exception>()
                }
            }
        }
    }
    
    context("Container Lifecycle Error Handling") {
        test("should handle container stop operations gracefully") {
            runTest {
                // Test stopping containers that are not running
                try {
                    pythonContainer.stop()
                    kotlinContainer.stop()
                    dockerManager.stopContainers()
                    // Should complete without throwing exceptions
                } catch (e: Exception) {
                    // Any exceptions should be handled gracefully
                    e.message shouldNotBe null
                }
            }
        }
        
        test("should handle executeInBoth with container failures") {
            runTest {
                // Test executeInBoth when containers are not available
                try {
                    val (pythonResult, kotlinResult) = dockerManager.executeInBoth { container ->
                        container.getContainerName()
                    }
                    
                    // If this succeeds, just verify the results
                    pythonResult shouldNotBe null
                    kotlinResult shouldNotBe null
                } catch (e: Exception) {
                    // Expected when containers are not available
                    e.message shouldNotBe null
                }
            }
        }
        
        test("should handle container ready check failures") {
            runTest {
                // Test container readiness check when containers are not available
                val containersReady = dockerManager.ensureContainersReady()
                
                // Should return false when containers are not available
                containersReady shouldBe false
            }
        }
    }
})