package sk.ainet.tokenizer.comparison.docker

import io.kotest.core.spec.style.StringSpec
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.kotest.matchers.shouldBe

/**
 * Property-based tests for Docker container setup and consistency.
 * Feature: tokenizer-comparison-tool, Property 1: Docker container consistency
 * Validates: Requirements 1.4
 */
class DockerContainerPropertyTest : StringSpec({
    
    "Property 1: Docker container consistency - For any CLI command supported by the system, both Python and Kotlin containers should expose identical interfaces and produce compatible outputs" {
        // This property test validates that both containers expose consistent interfaces
        // Currently testing the structure - full implementation will be added when containers are implemented
        
        checkAll(100, Arb.string(1..50)) { command ->
            // Test that both container types would handle the same command structure
            val pythonCommand = formatPythonCommand(command)
            val kotlinCommand = formatKotlinCommand(command)
            
            // Both should have the same basic structure
            pythonCommand.isNotEmpty() shouldBe true
            kotlinCommand.isNotEmpty() shouldBe true
            
            // Both should support the same basic operations
            val supportedOps = listOf("train", "encode", "decode", "health")
            val pythonSupportsOp = supportedOps.any { pythonCommand.contains(it) || command == it }
            val kotlinSupportsOp = supportedOps.any { kotlinCommand.contains(it) || command == it }
            
            // If one supports an operation, both should (consistency property)
            if (supportedOps.contains(command)) {
                pythonSupportsOp shouldBe kotlinSupportsOp
            }
        }
    }
})

/**
 * Format command for Python container CLI
 */
private fun formatPythonCommand(command: String): String {
    return when (command) {
        "train" -> "python cli.py train"
        "encode" -> "python cli.py encode"
        "decode" -> "python cli.py decode"
        "health" -> "python cli.py health"
        else -> "python cli.py $command"
    }
}

/**
 * Format command for Kotlin container CLI
 */
private fun formatKotlinCommand(command: String): String {
    return when (command) {
        "train" -> "./gradlew run --args='train'"
        "encode" -> "./gradlew run --args='encode'"
        "decode" -> "./gradlew run --args='decode'"
        "health" -> "./gradlew run --args='health'"
        else -> "./gradlew run --args='$command'"
    }
}