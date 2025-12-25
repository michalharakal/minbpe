package sk.ainet.tokenizer.comparison.docker

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Result of executing a command.
 */
data class CommandResult(
    val exitCode: Int,
    val output: String,
    val error: String
)

/**
 * Execute a system command and return the result.
 * 
 * @param command Command and arguments to execute
 * @param workingDir Working directory for the command (optional)
 * @param timeoutSeconds Timeout in seconds (default: 60)
 * @return Command execution result
 */
suspend fun executeCommand(
    command: List<String>,
    workingDir: File? = null,
    timeoutSeconds: Long = 60
): CommandResult = withContext(Dispatchers.IO) {
    try {
        val processBuilder = ProcessBuilder(command)
        if (workingDir != null) {
            processBuilder.directory(workingDir)
        }
        
        val process = processBuilder.start()
        val completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
        
        if (!completed) {
            process.destroyForcibly()
            return@withContext CommandResult(
                exitCode = -1,
                output = "",
                error = "Command timed out after $timeoutSeconds seconds"
            )
        }
        
        val output = process.inputStream.bufferedReader().readText()
        val error = process.errorStream.bufferedReader().readText()
        
        CommandResult(
            exitCode = process.exitValue(),
            output = output,
            error = error
        )
    } catch (e: Exception) {
        CommandResult(
            exitCode = -1,
            output = "",
            error = "Command execution failed: ${e.message}"
        )
    }
}

/**
 * Check if a Docker container is running.
 * 
 * @param containerName Name of the container to check
 * @return true if container is running, false otherwise
 */
suspend fun isContainerRunning(containerName: String): Boolean {
    val result = executeCommand(listOf("docker", "ps", "--filter", "name=$containerName", "--format", "{{.Names}}"))
    return result.exitCode == 0 && result.output.trim().contains(containerName)
}

/**
 * Check if a Docker container exists (running or stopped).
 * 
 * @param containerName Name of the container to check
 * @return true if container exists, false otherwise
 */
suspend fun containerExists(containerName: String): Boolean {
    val result = executeCommand(listOf("docker", "ps", "-a", "--filter", "name=$containerName", "--format", "{{.Names}}"))
    return result.exitCode == 0 && result.output.trim().contains(containerName)
}

/**
 * Start a Docker container.
 * 
 * @param containerName Name of the container to start
 * @return true if started successfully, false otherwise
 */
suspend fun startContainer(containerName: String): Boolean {
    val result = executeCommand(listOf("docker", "start", containerName))
    return result.exitCode == 0
}

/**
 * Stop a Docker container.
 * 
 * @param containerName Name of the container to stop
 * @return true if stopped successfully, false otherwise
 */
suspend fun stopContainer(containerName: String): Boolean {
    val result = executeCommand(listOf("docker", "stop", containerName))
    return result.exitCode == 0
}

/**
 * Remove a Docker container.
 * 
 * @param containerName Name of the container to remove
 * @return true if removed successfully, false otherwise
 */
suspend fun removeContainer(containerName: String): Boolean {
    val result = executeCommand(listOf("docker", "rm", containerName))
    return result.exitCode == 0
}

/**
 * Execute a command inside a Docker container.
 * 
 * @param containerName Name of the container
 * @param command Command to execute inside the container
 * @param timeoutSeconds Timeout in seconds
 * @return Command execution result
 */
suspend fun executeInContainer(
    containerName: String,
    command: List<String>,
    timeoutSeconds: Long = 60
): CommandResult {
    val dockerCommand = listOf("docker", "exec", containerName) + command
    return executeCommand(dockerCommand, timeoutSeconds = timeoutSeconds)
}

/**
 * Get container logs.
 * 
 * @param containerName Name of the container
 * @param lines Number of lines to retrieve (optional)
 * @return Container logs
 */
suspend fun getContainerLogs(containerName: String, lines: Int? = null): String {
    val command = mutableListOf("docker", "logs")
    if (lines != null) {
        command.addAll(listOf("--tail", lines.toString()))
    }
    command.add(containerName)
    
    val result = executeCommand(command)
    return if (result.exitCode == 0) result.output else result.error
}