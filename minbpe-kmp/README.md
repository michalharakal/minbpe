# MinBPE Kotlin Multiplatform

A Kotlin Multiplatform port of [minbpe](https://github.com/karpathy/minbpe) - a minimal, clean implementation of the Byte Pair Encoding (BPE) algorithm commonly used in Large Language Model tokenization.

## Overview

This library provides a complete Kotlin Multiplatform implementation of BPE tokenization with full compatibility with the original Python minbpe. It maintains the same core algorithm while leveraging Kotlin's type safety, coroutines, and multiplatform capabilities.

### Key Features

- **🔄 Python Compatibility**: Read and write `.model` files created by the original Python minbpe
- **🌐 Multiplatform**: Runs on JVM, Android, iOS, macOS, Linux, JavaScript, and WebAssembly
- **⚡ Reactive**: Async operations with Flow-based progress monitoring
- **🛡️ Type Safe**: Strong typing with comprehensive validation
- **📦 Multiple Formats**: Support for both JSON and Python-compatible text formats
- **🧪 Well Tested**: Comprehensive test suite with property-based testing

## Supported Platforms

- **JVM** (Java 21+ with Vector API support)
- **Android** (API 24+, targeting JVM 11 bytecode)
- **iOS** (Arm64 & Simulator)
- **macOS** (Arm64)
- **Linux** (x64 & Arm64)
- **JavaScript** (Browser)
- **WebAssembly** (Browser)

## Architecture

### Core Components

- **`Tokenizer`** - Abstract base class defining the BPE interface
- **`BasicTokenizer`** - Core BPE implementation (equivalent to Python's BasicTokenizer)
- **`BpeUtils`** - Helper functions for statistics and merging operations
- **`TokenizerRepository`** - High-level API for save/load operations
- **`PythonCompatiblePersistence`** - Python `.model` file format support
- **`JsonTokenizerPersistence`** - Modern JSON serialization format

### Persistence Layer

The library separates tokenization logic from I/O operations:

```kotlin
// Python-compatible format (interoperable with original minbpe)
val repo = TokenizerRepository()
repo.savePythonCompatible(tokenizer, "my_tokenizer")
// Creates: my_tokenizer.model, my_tokenizer.vocab

// Load Python-created files
repo.loadPythonCompatible("python_tokenizer.model").collect { result ->
    if (result is RepositoryResult.TokenizerLoaded) {
        val tokenizer = result.tokenizer
        // Use tokenizer...
    }
}

// Auto-detect format
repo.loadAuto("some_tokenizer.model") // Detects Python vs JSON format
```

## Usage

### Basic Training and Encoding

```kotlin
import sk.ainet.nlp.tools.tokenizer.BasicTokenizer

// Create and train tokenizer
val tokenizer = BasicTokenizer()
val text = "Hello world! This is a sample text for BPE training."
tokenizer.train(text, vocabSize = 512, verbose = true)

// Encode text to token IDs
val tokens = tokenizer.encode("Hello world!")
println("Tokens: $tokens")

// Decode back to text
val decoded = tokenizer.decode(tokens)
println("Decoded: $decoded")
```

### Reactive Save/Load Operations

```kotlin
import sk.ainet.nlp.tools.tokenizer.persistence.TokenizerRepository
import kotlinx.coroutines.flow.collect

val repo = TokenizerRepository()

// Save with progress monitoring
repo.savePythonCompatible(tokenizer, "my_tokenizer").collect { result ->
    when (result) {
        is RepositoryResult.Started -> println("Starting save...")
        is RepositoryResult.Progress -> println("Progress: ${result.message}")
        is RepositoryResult.Completed -> println("Save completed: ${result.message}")
        is RepositoryResult.Error -> println("Error: ${result.message}")
    }
}

// Load with progress monitoring
repo.loadPythonCompatible("my_tokenizer.model").collect { result ->
    when (result) {
        is RepositoryResult.TokenizerLoaded -> {
            val loadedTokenizer = result.tokenizer
            println("Tokenizer loaded successfully!")
        }
        is RepositoryResult.Error -> println("Load failed: ${result.message}")
        else -> println("Loading progress: $result")
    }
}
```

## Python Interoperability

The library can seamlessly work with files created by the original Python minbpe:

### Python → Kotlin
```python
# Python code
import minbpe
tokenizer = minbpe.BasicTokenizer()
tokenizer.train(text, vocab_size=512)
tokenizer.save("python_tokenizer")  # Creates .model and .vocab files
```

```kotlin
// Kotlin code - load Python-created files
val repo = TokenizerRepository()
repo.loadPythonCompatible("python_tokenizer.model").collect { result ->
    if (result is RepositoryResult.TokenizerLoaded) {
        val tokenizer = result.tokenizer
        // Use the Python-trained tokenizer in Kotlin!
    }
}
```

### Kotlin → Python
```kotlin
// Kotlin code
val tokenizer = BasicTokenizer()
tokenizer.train(text, vocabSize = 512)
repo.savePythonCompatible(tokenizer, "kotlin_tokenizer")
```

```python
# Python code - load Kotlin-created files
import minbpe
tokenizer = minbpe.BasicTokenizer()
tokenizer.load("kotlin_tokenizer.model")  # Works seamlessly!
```

## File Formats

### Python-Compatible Format (.model)
```
minbpe v1
[pattern]
[num_special_tokens]
[special_token] [id]
...
[merge_token1] [merge_token2]
...
```

### JSON Format (.json)
```json
{
  "version": "minbpe v1",
  "type": "basic",
  "pattern": "",
  "specialTokens": {},
  "merges": [
    [[72, 101], 256],
    [[256, 108], 257]
  ],
  "metadata": {
    "vocabSize": "512",
    "mergeCount": "256"
  }
}
```

## Dependencies

- **kotlinx-serialization-json** 1.9.0 - JSON serialization
- **kotlinx-coroutines-core** 1.10.2 - Reactive operations
- **kotlinx-io-core** 0.8.2 - Cross-platform I/O

## Testing

The library includes comprehensive testing:

- **Unit Tests**: Core algorithm validation
- **Property-Based Tests**: Kotest property testing for edge cases
- **Integration Tests**: Python compatibility validation
- **Cross-Platform Tests**: Ensuring consistent behavior across platforms

```bash
# Run all tests
./gradlew allTests

# Run JVM tests only
./gradlew jvmTest

# Generate coverage report
./gradlew koverHtmlReport
```

## Building

```bash
# Build for all platforms
./gradlew assemble

# Build specific platform
./gradlew jvmJar
./gradlew compileKotlinJs
./gradlew linkDebugFrameworkIosArm64
```

## Installation

Add to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("sk.ainet.nlp.tools:minbpe-kmp:1.0.0")
}
```

## Algorithm Details

The implementation follows the exact BPE algorithm from the original paper and Python implementation:

1. **Initialization**: Start with UTF-8 bytes as base vocabulary (tokens 0-255)
2. **Statistics**: Count frequency of all consecutive token pairs
3. **Merge**: Replace most frequent pair with new token (256, 257, 258...)
4. **Repeat**: Continue until desired vocabulary size is reached
5. **Encoding**: Apply learned merges greedily in training order
6. **Decoding**: Concatenate token byte sequences and decode as UTF-8

## Contributing

This project follows Kotlin coding conventions and includes:
- Explicit API mode for public interfaces
- Comprehensive documentation
- Property-based testing for algorithm validation
- Cross-platform compatibility testing

## License

Licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Original [minbpe](https://github.com/karpathy/minbpe) by Andrej Karpathy
- Kotlin Multiplatform team for the excellent tooling
- JetBrains for kotlinx libraries
