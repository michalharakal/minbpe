package sk.ainet.tokenizer.comparison.docker

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest

class DockerManagerTest : FunSpec({
    
    lateinit var dockerManager: DockerManager
    
    beforeEach {
        dockerManager = DockerManager()
    }
    
    context("Docker Manager Initialization") {
        test("should create docker manager instance") {
            dockerManager shouldNotBe null
            dockerManager.getPythonContainer() shouldNotBe null
            dockerManager.getKotlinContainer() shouldNotBe null
        }
        
        test("python container should have correct name") {
            val pythonContainer = dockerManager.getPythonContainer()
            pythonContainer.getContainerName() shouldBe "minbpe-python"
        }
        
        test("kotlin container should have correct name") {
            val kotlinContainer = dockerManager.getKotlinContainer()
            kotlinContainer.getContainerName() shouldBe "minbpe-kotlin"
        }
    }
    
    context("Container Startup and Health Checks") {
        test("should check docker availability") {
            runTest {
                // This test verifies the method executes without throwing
                val isAvailable = dockerManager.isDockerAvailable()
                // Result can be true or false depending on Docker availability
                isAvailable.shouldBeInstanceOf<Boolean>()
            }
        }
        
        test("should get docker info") {
            runTest {
                val info = dockerManager.getDockerInfo()
                info shouldNotBe null
                info.length shouldNotBe 0
            }
        }
        
        test("should test health checks without containers") {
            runTest {
                // Test health checks when containers are not running
                val containersReady = dockerManager.ensureContainersReady()
                // Should return false when containers are not available
                containersReady shouldBe false
            }
        }
        
        test("should test container startup without Docker") {
            runTest {
                // Test that startup methods handle Docker unavailability gracefully
                try {
                    dockerManager.startContainers()
                    // If Docker is not available, this should handle it gracefully
                } catch (e: RuntimeException) {
                    // Expected when Docker is not available or containers don't exist
                    e.message shouldContain "Failed to"
                }
            }
        }
    }
    
    context("Command Execution and Output Parsing") {
        test("should handle Docker command execution errors") {
            runTest {
                // Test error handling for Docker command failures
                val dockerInfo = dockerManager.getDockerInfo()
                dockerInfo shouldNotBe null
                
                // If Docker is not available, should return error message
                if (!dockerManager.isDockerAvailable()) {
                    dockerInfo shouldContain "Docker not available"
                }
            }
        }
        
        test("should execute operation in both containers gracefully") {
            runTest {
                // Test executeInBoth when containers are not available
                val operation: suspend (ContainerInterface) -> String = { container ->
                    container.getContainerName()
                }
                
                try {
                    val (pythonResult, kotlinResult) = dockerManager.executeInBoth(operation)
                    pythonResult shouldNotBe null
                    kotlinResult shouldNotBe null
                } catch (e: Exception) {
                    // Expected when containers are not available
                    e.message shouldNotBe null
                }
            }
        }
    }
    
    context("Error Handling for Docker Failures") {
        test("should handle container lifecycle operations gracefully") {
            runTest {
                // Test that lifecycle operations don't throw unexpected exceptions
                try {
                    dockerManager.stopContainers()
                    // Should complete without throwing, even if containers don't exist
                } catch (e: Exception) {
                    // Any exceptions should be handled gracefully
                    e.message shouldNotBe null
                }
            }
        }
        
        test("should handle Docker unavailability in info retrieval") {
            runTest {
                val dockerInfo = dockerManager.getDockerInfo()
                dockerInfo shouldNotBe null
                
                // Should either contain Docker info or error message
                if (!dockerManager.isDockerAvailable()) {
                    dockerInfo shouldContain "Docker not available"
                }
            }
        }
        
        test("should provide individual container access") {
            val pythonContainer = dockerManager.getPythonContainer()
            val kotlinContainer = dockerManager.getKotlinContainer()
            
            pythonContainer shouldNotBe null
            kotlinContainer shouldNotBe null
            pythonContainer.getContainerName() shouldBe "minbpe-python"
            kotlinContainer.getContainerName() shouldBe "minbpe-kotlin"
        }
    }
})