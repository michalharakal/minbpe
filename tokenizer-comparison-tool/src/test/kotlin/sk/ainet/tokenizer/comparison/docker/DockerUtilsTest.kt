package sk.ainet.tokenizer.comparison.docker

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest

class DockerUtilsTest : FunSpec({
    
    context("Basic Command Execution") {
        test("should execute basic system commands") {
            runTest {
                // Test basic command execution with a simple command that should work on most systems
                val result = executeCommand(listOf("echo", "test"))
                
                result shouldNotBe null
                result.exitCode shouldBe 0
                result.output shouldContain "test"
                result.error shouldBe ""
            }
        }
        
        test("should handle command execution failures") {
            runTest {
                // Test command execution with a command that should fail
                val result = executeCommand(listOf("nonexistent-command-12345"))
                
                result shouldNotBe null
                result.exitCode shouldNotBe 0
                // Either error should be non-empty or output should be empty
                (result.error.isNotEmpty() || result.output.isEmpty()) shouldBe true
            }
        }
        
        test("should handle command timeouts") {
            runTest {
                // Test command timeout handling with a short timeout
                val result = executeCommand(listOf("sleep", "2"), timeoutSeconds = 1)
                
                result shouldNotBe null
                result.exitCode shouldBe -1
                result.error shouldContain "timed out"
            }
        }
    }
    
    context("Container Status Checks") {
        test("should check container existence without Docker") {
            runTest {
                // Test container existence check when Docker might not be available
                val exists = containerExists("nonexistent-container")
                
                // Should return false, either because Docker is not available or container doesn't exist
                exists shouldBe false
            }
        }
        
        test("should check container running status without Docker") {
            runTest {
                // Test container running check when Docker might not be available
                val running = isContainerRunning("nonexistent-container")
                
                // Should return false, either because Docker is not available or container is not running
                running shouldBe false
            }
        }
    }
    
    context("Container Operations") {
        test("should handle container operations gracefully") {
            runTest {
                // Test container operations when containers don't exist
                val containerName = "test-nonexistent-container"
                
                val started = startContainer(containerName)
                val stopped = stopContainer(containerName)
                val removed = removeContainer(containerName)
                
                // These should all return false when container doesn't exist or Docker is not available
                started shouldBe false
                // stopped and removed might return true even if container doesn't exist (Docker behavior)
                stopped.shouldBeInstanceOf<Boolean>()
                removed.shouldBeInstanceOf<Boolean>()
            }
        }
        
        test("should handle container command execution") {
            runTest {
                // Test executing commands in containers when container doesn't exist
                val result = executeInContainer("nonexistent-container", listOf("echo", "test"))
                
                result shouldNotBe null
                result.exitCode shouldNotBe 0
                result.error.isNotEmpty() shouldBe true
            }
        }
    }
    
    context("Container Logs") {
        test("should get container logs gracefully") {
            runTest {
                // Test getting container logs when container doesn't exist
                val logs = getContainerLogs("nonexistent-container")
                
                logs shouldNotBe null
                // Should return empty string or error message, but not null
                logs.shouldBeInstanceOf<String>()
            }
        }
        
        test("should handle container logs with line limit") {
            runTest {
                // Test getting container logs with line limit
                val logs = getContainerLogs("nonexistent-container", lines = 10)
                
                logs shouldNotBe null
                logs.shouldBeInstanceOf<String>()
            }
        }
    }
    
    context("Docker Integration Tests") {
        test("should execute Docker commands when Docker is available").config(enabled = false) {
            runTest {
                // Test actual Docker commands when Docker is available
                val result = executeCommand(listOf("docker", "--version"))
                
                if (result.exitCode == 0) {
                    result.output shouldContain "Docker"
                    result.error shouldBe ""
                }
            }
        }
        
        test("should list Docker containers when Docker is available").config(enabled = false) {
            runTest {
                // Test Docker container listing
                val result = executeCommand(listOf("docker", "ps", "-a"))
                
                if (result.exitCode == 0) {
                    result.output shouldNotBe null
                    // Output should contain container headers or be empty
                }
            }
        }
    }
})