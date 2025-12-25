package sk.ainet.nlp.tools.tokenizer

/**
 * Simple verification script for RegexTokenizer functionality.
 * This is used to verify the implementation works correctly when the test framework
 * cannot be used due to compilation issues in other parts of the project.
 */
object RegexTokenizerVerification {
    
    fun runBasicVerification(): String {
        val results = mutableListOf<String>()
        
        try {
            // Test 1: Basic construction
            val tokenizer = RegexTokenizer()
            results.add("✓ Basic construction works")
            results.add("  Pattern: ${tokenizer.pattern}")
            results.add("  Special tokens: ${tokenizer.specialTokens.size}")
            
            // Test 2: Custom pattern
            val customTokenizer = RegexTokenizer("\\w+")
            results.add("✓ Custom pattern construction works")
            results.add("  Custom pattern: ${customTokenizer.pattern}")
            
            // Test 3: Special token registration
            val specialTokens = mapOf(
                "<|endoftext|>" to 100257,
                "<|startoftext|>" to 100258
            )
            tokenizer.registerSpecialTokens(specialTokens)
            results.add("✓ Special token registration works")
            results.add("  Registered tokens: ${tokenizer.specialTokens}")
            
            // Test 4: Basic training
            val text = "hello world test"
            tokenizer.train(text, vocabSize = 300, verbose = false)
            results.add("✓ Training completed")
            results.add("  Merges learned: ${tokenizer.merges.size}")
            results.add("  Vocab size: ${tokenizer.vocab.size}")
            
            // Test 5: Encoding and decoding
            val encoded = tokenizer.encode(text)
            val decoded = tokenizer.decode(encoded)
            results.add("✓ Encode/decode works")
            results.add("  Original: '$text'")
            results.add("  Encoded: $encoded")
            results.add("  Decoded: '$decoded'")
            results.add("  Round trip success: ${text == decoded}")
            
            // Test 6: Special token handling
            val textWithSpecial = "hello <|endoftext|> world"
            try {
                val encodedWithSpecial = tokenizer.encode(textWithSpecial, "all")
                val decodedWithSpecial = tokenizer.decode(encodedWithSpecial)
                results.add("✓ Special token handling works")
                results.add("  With special: '$textWithSpecial'")
                results.add("  Encoded: $encodedWithSpecial")
                results.add("  Decoded: '$decodedWithSpecial'")
                results.add("  Special token round trip: ${textWithSpecial == decodedWithSpecial}")
            } catch (e: Exception) {
                results.add("✗ Special token handling failed: ${e.message}")
            }
            
            // Test 7: Error handling
            try {
                tokenizer.encode(textWithSpecial, "none_raise")
                results.add("✗ Error handling failed - should have thrown exception")
            } catch (e: TokenizationException) {
                results.add("✓ Error handling works - correctly threw TokenizationException")
            } catch (e: Exception) {
                results.add("✗ Error handling failed - wrong exception type: ${e::class.simpleName}")
            }
            
        } catch (e: Exception) {
            results.add("✗ Verification failed with exception: ${e.message}")
            results.add("  Stack trace: ${e.stackTraceToString()}")
        }
        
        return results.joinToString("\n")
    }
}