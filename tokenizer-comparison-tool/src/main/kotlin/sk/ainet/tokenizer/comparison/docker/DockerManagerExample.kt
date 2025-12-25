package sk.ainet.tokenizer.comparison.docker

import kotlinx.coroutines.runBlocking
import sk.ainet.tokenizer.comparison.model.TokenizerType
import sk.ainet.tokenizer.comparison.model.ExportFormat

/**
 * Example demonstrating how to use the Docker Manager for tokenizer comparison.
 * This example shows the basic workflow for comparing Python and Kotlin implementations.
 */
fun main() = runBlocking {
    val dockerManager = DockerManager()
    
    println("=== Tokenizer Comparison Tool - Docker Manager Example ===")
    
    // Check Docker availability
    println("\n1. Checking Docker availability...")
    val dockerAvailable = dockerManager.isDockerAvailable()
    println("Docker available: $dockerAvailable")
    
    if (!dockerAvailable) {
        println("Docker is not available. Please install Docker and try again.")
        return@runBlocking
    }
    
    // Get Docker system information
    println("\n2. Docker system information:")
    val dockerInfo = dockerManager.getDockerInfo()
    println(dockerInfo.lines().take(5).joinToString("\n"))
    
    // Example workflow (would require actual containers to be built)
    println("\n3. Example workflow (requires containers to be built):")
    
    try {
        // Start containers
        println("Starting containers...")
        dockerManager.startContainers()
        
        // Check if containers are ready
        val containersReady = dockerManager.ensureContainersReady()
        println("Containers ready: $containersReady")
        
        if (containersReady) {
            // Example: Train tokenizers on both implementations
            val testText = "Hello world! This is a test for tokenizer comparison."
            println("Training tokenizers with text: '$testText'")
            
            val (pythonConfig, kotlinConfig) = dockerManager.executeInBoth { container ->
                println("Training on ${container.getContainerName()}...")
                container.trainTokenizer(testText, 256, TokenizerType.BASIC)
            }
            
            println("Python config: ${pythonConfig.type}, vocab size: ${pythonConfig.vocabSize}")
            println("Kotlin config: ${kotlinConfig.type}, vocab size: ${kotlinConfig.vocabSize}")
            
            // Example: Export configurations
            val pythonExport = dockerManager.getPythonContainer().exportConfig(pythonConfig, ExportFormat.JSON)
            val kotlinExport = dockerManager.getKotlinContainer().exportConfig(kotlinConfig, ExportFormat.JSON)
            
            println("Configurations exported successfully")
            println("Python export size: ${pythonExport.length} characters")
            println("Kotlin export size: ${kotlinExport.length} characters")
            
        } else {
            println("Containers are not ready. This is expected if Docker images are not built.")
            println("To build the containers, run:")
            println("  docker build -t minbpe-python:latest docker/python/")
            println("  docker build -t minbpe-kotlin:latest docker/kotlin/")
        }
        
    } catch (e: Exception) {
        println("Error during container operations: ${e.message}")
        println("This is expected if Docker containers are not built and available.")
    } finally {
        // Clean up
        println("\nCleaning up containers...")
        dockerManager.stopContainers()
    }
    
    println("\n=== Example completed ===")
}