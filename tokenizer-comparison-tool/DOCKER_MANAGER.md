# Docker Manager Implementation

This document describes the Docker Manager implementation for the Tokenizer Comparison Tool.

## Overview

The Docker Manager provides a unified interface for managing and orchestrating Docker containers that run both Python and Kotlin tokenizer implementations. It enables systematic comparison and validation between the two implementations in isolated, reproducible environments.

## Architecture

### Core Components

1. **ContainerInterface** - Common interface for tokenizer operations
2. **DockerManager** - Orchestrates both containers and provides unified access
3. **PythonContainer** - Implementation for Python minbpe tokenizer
4. **KotlinContainer** - Implementation for Kotlin minbpe tokenizer
5. **DockerUtils** - Utility functions for Docker command execution

### Key Features

- **Container Lifecycle Management**: Start, stop, and health check containers
- **Unified Operations**: Execute operations on both implementations simultaneously
- **Shared Volume Management**: Automatic file sharing between host and containers
- **Error Handling**: Comprehensive error handling with detailed diagnostics
- **Resource Cleanup**: Automatic cleanup of temporary files and resources

## Usage

### Basic Usage

```kotlin
import sk.ainet.tokenizer.comparison.docker.DockerManager
import sk.ainet.tokenizer.comparison.model.TokenizerType

val dockerManager = DockerManager()

// Check Docker availability
val dockerAvailable = dockerManager.isDockerAvailable()

// Start containers
dockerManager.startContainers()

// Verify containers are ready
val containersReady = dockerManager.ensureContainersReady()

// Execute operation on both implementations
val (pythonResult, kotlinResult) = dockerManager.executeInBoth { container ->
    container.trainTokenizer("Hello world!", 256, TokenizerType.BASIC)
}

// Clean up
dockerManager.stopContainers()
```

### Container Operations

Each container supports the following operations:

- **trainTokenizer()** - Train a tokenizer with given text and parameters
- **encodeText()** - Encode text using a tokenizer configuration
- **decodeTokens()** - Decode token IDs back to text
- **exportConfig()** - Export tokenizer configuration to JSON/binary format
- **loadConfig()** - Load tokenizer configuration from serialized data
- **isHealthy()** - Check container health status

### Configuration Export/Import

```kotlin
// Export configuration
val config = container.trainTokenizer(text, vocabSize, type)
val exportedJson = container.exportConfig(config, ExportFormat.JSON)

// Import configuration
val importedConfig = container.loadConfig(exportedJson, ExportFormat.JSON)
```

## Container Requirements

### Python Container

- **Image Name**: `minbpe-python:latest`
- **Container Name**: `minbpe-python`
- **CLI Path**: `/app/cli.py`
- **Shared Volume**: `/shared`

### Kotlin Container

- **Image Name**: `minbpe-kotlin:latest`
- **Container Name**: `minbpe-kotlin`
- **CLI Path**: `/app/cli.jar`
- **Shared Volume**: `/shared`

## File Management

The Docker Manager uses a shared volume approach for data exchange:

1. **Host Directory**: `${java.io.tmpdir}/tokenizer-comparison-shared`
2. **Container Mount**: `/shared`
3. **Automatic Cleanup**: Temporary files are automatically cleaned up after operations

## Error Handling

The implementation provides comprehensive error handling:

- **Docker Availability**: Checks if Docker is installed and running
- **Container Health**: Validates container readiness before operations
- **Command Execution**: Handles timeouts and execution failures
- **Resource Management**: Ensures proper cleanup even on failures

## Testing

The implementation includes comprehensive tests:

- **Unit Tests**: Test individual components and methods
- **Integration Tests**: Test end-to-end workflows (disabled by default)
- **Container Tests**: Test container-specific functionality

### Running Tests

```bash
# Run all tests
./gradlew test

# Run Docker-specific tests
./gradlew test --tests "sk.ainet.tokenizer.comparison.docker.*"

# Run with Docker containers (requires containers to be built)
./gradlew test -Dtest.docker.enabled=true
```

## Prerequisites

1. **Docker Installation**: Docker must be installed and running
2. **Container Images**: Both Python and Kotlin container images must be built
3. **Permissions**: User must have permissions to run Docker commands

### Building Container Images

```bash
# Build Python container
docker build -t minbpe-python:latest docker/python/

# Build Kotlin container
docker build -t minbpe-kotlin:latest docker/kotlin/
```

## Performance Considerations

- **Parallel Execution**: Operations on both containers run in parallel
- **Resource Monitoring**: Memory and CPU usage can be monitored
- **Timeout Management**: Configurable timeouts prevent hanging operations
- **Volume Optimization**: Shared volumes minimize data transfer overhead

## Troubleshooting

### Common Issues

1. **Docker Not Available**
   - Ensure Docker is installed and running
   - Check user permissions for Docker commands

2. **Container Images Not Found**
   - Build the required container images
   - Verify image names match expected values

3. **Container Health Check Failures**
   - Check container logs: `docker logs <container-name>`
   - Verify CLI applications are properly installed in containers

4. **Shared Volume Issues**
   - Check file permissions in shared directory
   - Ensure sufficient disk space for temporary files

### Diagnostic Commands

```kotlin
// Check Docker availability
val available = dockerManager.isDockerAvailable()

// Get Docker system information
val info = dockerManager.getDockerInfo()

// Check container health
val pythonHealthy = dockerManager.getPythonContainer().isHealthy()
val kotlinHealthy = dockerManager.getKotlinContainer().isHealthy()
```

## Future Enhancements

- **Binary Format Support**: Add support for binary configuration formats
- **Performance Monitoring**: Real-time resource usage monitoring
- **Container Scaling**: Support for multiple container instances
- **Custom Images**: Support for custom container images and configurations
- **Network Optimization**: Optimize container communication for large datasets