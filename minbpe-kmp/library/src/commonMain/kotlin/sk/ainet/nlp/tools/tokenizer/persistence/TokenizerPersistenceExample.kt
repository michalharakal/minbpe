package sk.ainet.nlp.tools.tokenizer.persistence

import kotlinx.coroutines.flow.collect
import sk.ainet.nlp.tools.tokenizer.BasicTokenizer

/**
 * Example usage of the reactive tokenizer persistence system.
 * 
 * This demonstrates the separation of concerns between tokenizer logic
 * and I/O operations using kotlinx.io and Flow APIs.
 */
object TokenizerPersistenceExample {
    
    /**
     * Example: Train and save a tokenizer with progress monitoring.
     */
    suspend fun trainAndSaveExample() {
        // Train a tokenizer
        val tokenizer = BasicTokenizer()
        val text = "aaabdaaabac"
        tokenizer.train(text, vocabSize = 260, verbose = true)
        
        // Save using the repository with progress monitoring
        val repository = TokenizerRepository()
        
        repository.save(tokenizer, "my_tokenizer").collect { result ->
            when (result) {
                is RepositoryResult.Started -> println("Started: ${result.message}")
                is RepositoryResult.Progress -> println("Progress: ${result.message}")
                is RepositoryResult.Completed -> println("Completed: ${result.message}")
                is RepositoryResult.Error -> println("Error: ${result.message}")
                else -> {}
            }
        }
    }
    
    /**
     * Example: Load a tokenizer with progress monitoring.
     */
    suspend fun loadExample() {
        val repository = TokenizerRepository()
        
        repository.load("my_tokenizer.model").collect { result ->
            when (result) {
                is RepositoryResult.Started -> println("Started: ${result.message}")
                is RepositoryResult.Progress -> println("Progress: ${result.message}")
                is RepositoryResult.TokenizerLoaded -> {
                    println("Loaded: ${result.message}")
                    val tokenizer = result.tokenizer
                    
                    // Test the loaded tokenizer
                    val text = "aaabdaaabac"
                    val encoded = tokenizer.encode(text)
                    val decoded = tokenizer.decode(encoded)
                    
                    println("Original: $text")
                    println("Encoded: $encoded")
                    println("Decoded: $decoded")
                    println("Round-trip successful: ${text == decoded}")
                }
                is RepositoryResult.Error -> println("Error: ${result.message}")
                else -> {}
            }
        }
    }
    
    /**
     * Example: Direct persistence layer usage for custom scenarios.
     */
    suspend fun directPersistenceExample() {
        val tokenizer = BasicTokenizer()
        tokenizer.train("hello world", vocabSize = 280)
        
        val persistence = JsonTokenizerPersistence()
        
        // Custom sink/source usage
        // This would typically be used with network streams, 
        // custom file formats, or other I/O scenarios
        
        // Note: This is a conceptual example - actual implementation
        // would depend on specific I/O requirements
        println("Direct persistence layer allows custom I/O scenarios")
        println("- Network streaming")
        println("- Custom file formats") 
        println("- Database storage")
        println("- Memory buffers")
    }
}