# Tokenizer Comparison Tool

A comprehensive comparison and validation tool for testing compatibility between Python and Kotlin BPE tokenizer implementations.

## 📋 Architecture Documentation

**[📖 Complete Architecture Guide](../TOKENIZER_COMPARISON_ARCHITECTURE.md)** - Detailed system architecture, data flow diagrams, and component specifications with Mermaid diagrams.

## Overview

This tool provides Docker containerization, CLI utilities, automated testing, and detailed reporting to ensure compatibility and validate performance characteristics between the original Python minbpe implementation and the Kotlin Multiplatform port.

## Quick Start

### Prerequisites

- Docker and Docker Compose
- Java 21+ (for local development)
- Gradle 8.11.1+ (for local development)

### Building and Running

1. **Build all containers:**
   ```bash
   docker-compose build
   ```

2. **Run health checks:**
   ```bash
   docker-compose run --rm python-tokenizer health
   docker-compose run --rm kotlin-tokenizer health
   ```

3. **Run the comparison tool:**
   ```bash
   docker-compose run --rm comparison-tool --help
   ```

### Local Development

1. **Build the project:**
   ```bash
   ./gradlew build
   ```

2. **Run the CLI:**
   ```bash
   ./gradlew run --args="--help"
   ```

## Project Structure

```
tokenizer-comparison-tool/
├── docker/                     # Docker configurations
│   ├── python/                # Python container setup
│   │   ├── Dockerfile
│   │   ├── requirements.txt
│   │   └── cli.py
│   └── kotlin/                # Kotlin container setup
│       ├── Dockerfile
│       ├── build.gradle.kts
│       └── src/
├── src/                       # Main application source
│   ├── main/kotlin/
│   │   ├── cli/              # CLI interface
│   │   ├── docker/           # Docker management
│   │   ├── testing/          # Test orchestration
│   │   ├── reporting/        # Report generation
│   │   └── model/            # Data models
│   └── test/                 # Test files
├── test-corpus/              # Test data
├── reports/                  # Generated reports
├── docker-compose.yml        # Container orchestration
└── README.md
```

## Features

- **Docker Containerization**: Isolated, reproducible environments for both implementations
- **CLI Interface**: Unified command-line tool for all operations
- **Compatibility Testing**: Automated validation of identical behavior
- **Performance Benchmarking**: Statistical analysis of execution characteristics
- **Comprehensive Reporting**: HTML, JSON, and CSV output formats
- **CI/CD Integration**: Headless operation with machine-readable outputs

## Usage

The tool will support the following commands (implementation in progress):

- `compare` - Run compatibility tests between implementations
- `benchmark` - Execute performance benchmarks
- `export` - Export tokenizer configurations
- `validate` - Validate cross-platform compatibility

## Development Status

This project is currently under development. The basic project structure and Docker foundation have been established. Implementation of core functionality is in progress according to the specification.

**For detailed architecture information, see the [Architecture Documentation](../TOKENIZER_COMPARISON_ARCHITECTURE.md).**

## Requirements

See the requirements document in `.kiro/specs/tokenizer-comparison-tool/requirements.md` for detailed functional requirements.

## Design

See the design document in `.kiro/specs/tokenizer-comparison-tool/design.md` for architectural details and component specifications.