package sk.ainet.tokenizer.comparison.docker

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import sk.ainet.tokenizer.comparison.model.TestResult

/**
 * Manages Docker containers for both Python and Kotlin tokenizer implementations.
 * Provides unified access and orchestration capabilities for cross-implementation testing.
 */
class DockerManager {
    private val pythonContainer = PythonContainer()
    private val kotlinContainer = KotlinContainer()
    
    /**
     * Ensure both containers are ready for operation.
     * 
     * @return true if both containers are healthy, false otherwise
     */
    suspend fun ensureContainersReady(): Boolean {
        return coroutineScope {
            val pythonHealthy = async { pythonContainer.isHealthy() }
            val kotlinHealthy = async { kotlinContainer.isHealthy() }
            
            pythonHealthy.await() && kotlinHealthy.await()
        }
    }
    
    /**
     * Start both containers.
     */
    suspend fun startContainers() {
        coroutineScope {
            val pythonStart = async { pythonContainer.start() }
            val kotlinStart = async { kotlinContainer.start() }
            
            pythonStart.await()
            kotlinStart.await()
        }
    }
    
    /**
     * Stop both containers and clean up resources.
     */
    suspend fun stopContainers() {
        coroutineScope {
            val pythonStop = async { pythonContainer.stop() }
            val kotlinStop = async { kotlinContainer.stop() }
            
            pythonStop.await()
            kotlinStop.await()
        }
    }
    
    /**
     * Execute an operation on both containers and return the results.
     * 
     * @param operation Operation to execute on each container
     * @return Pair of results (Python result, Kotlin result)
     */
    suspend fun <T> executeInBoth(
        operation: suspend (ContainerInterface) -> T
    ): Pair<T, T> {
        return coroutineScope {
            val pythonResult = async { operation(pythonContainer) }
            val kotlinResult = async { operation(kotlinContainer) }
            
            pythonResult.await() to kotlinResult.await()
        }
    }
    
    /**
     * Get the Python container instance.
     */
    fun getPythonContainer(): ContainerInterface = pythonContainer
    
    /**
     * Get the Kotlin container instance.
     */
    fun getKotlinContainer(): ContainerInterface = kotlinContainer
    
    /**
     * Check if Docker is available on the system.
     * 
     * @return true if Docker is available, false otherwise
     */
    suspend fun isDockerAvailable(): Boolean {
        return try {
            val result = executeCommand(listOf("docker", "--version"))
            result.exitCode == 0
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get Docker system information for diagnostics.
     * 
     * @return Docker system info or error message
     */
    suspend fun getDockerInfo(): String {
        return try {
            val result = executeCommand(listOf("docker", "system", "info"))
            if (result.exitCode == 0) {
                result.output
            } else {
                "Docker system info failed: ${result.error}"
            }
        } catch (e: Exception) {
            "Docker not available: ${e.message}"
        }
    }
}