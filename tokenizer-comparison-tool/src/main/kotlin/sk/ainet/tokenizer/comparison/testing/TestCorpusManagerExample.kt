package sk.ainet.tokenizer.comparison.testing

import sk.ainet.tokenizer.comparison.model.*

/**
 * Example demonstrating how to use the TestCorpusManager.
 */
fun main() {
    val corpusManager = TestCorpusManager()
    val generator = TestSuiteGenerator(corpusManager)
    
    println("=== Test Corpus Manager Example ===")
    
    // Basic corpus
    println("\n1. Basic Corpus:")
    val basicCorpus = corpusManager.getBasicCorpus()
    println("   Found ${basicCorpus.size} basic test cases")
    basicCorpus.take(3).forEach { text ->
        println("   - \"${text.take(50)}${if (text.length > 50) "..." else ""}\"")
    }
    
    // Unicode corpus
    println("\n2. Unicode Corpus:")
    val unicodeCorpus = corpusManager.getUnicodeCorpus()
    println("   Found ${unicodeCorpus.size} unicode test cases")
    unicodeCorpus.take(2).forEach { text ->
        println("   - \"${text.take(50)}${if (text.length > 50) "..." else ""}\"")
    }
    
    // Edge cases
    println("\n3. Edge Cases:")
    val edgeCases = corpusManager.getEdgeCases()
    println("   Found ${edgeCases.size} edge cases")
    println("   - Empty string: \"${edgeCases.find { it.isEmpty() } ?: "Not found"}\"")
    println("   - Single space: \"${edgeCases.find { it == " " } ?: "Not found"}\"")
    println("   - Contains emoji: ${edgeCases.any { it.contains("🌍") }}")
    
    // Reference tests
    println("\n4. Reference Tests:")
    val referenceTests = corpusManager.getReferenceTests()
    println("   Found ${referenceTests.size} reference tests")
    referenceTests.forEach { test ->
        println("   - ${test.name}: \"${test.input.take(30)}${if (test.input.length > 30) "..." else ""}\"")
    }
    
    // Test cases by category
    println("\n5. Test Cases by Category:")
    TestCategory.values().forEach { category ->
        val cases = corpusManager.getTestCasesByCategory(category)
        println("   - ${category.name}: ${cases.size} cases")
    }
    
    // Test cases by complexity
    println("\n6. Test Cases by Complexity:")
    ComplexityLevel.values().forEach { complexity ->
        val cases = corpusManager.getTestCasesByComplexity(complexity)
        println("   - ${complexity.name}: ${cases.size} cases")
    }
    
    // Wikipedia BPE example
    println("\n7. Wikipedia BPE Example:")
    val wikipediaExample = corpusManager.getWikipediaBPEExample()
    println("   Length: ${wikipediaExample.length} characters")
    println("   Preview: \"${wikipediaExample.take(100)}...\"")
    
    // Generate test suite
    println("\n8. Generated Test Suite:")
    val testSuite = generator.generateCompatibilityTestSuite()
    println("   Suite name: ${testSuite.name}")
    println("   Total test cases: ${testSuite.size}")
    println("   Categories: ${testSuite.testCases.map { it.category }.toSet().joinToString(", ")}")
    println("   Tokenizer types: ${testSuite.testCases.map { it.config.type }.toSet().joinToString(", ")}")
    
    // Corpus validation
    println("\n9. Corpus Validation:")
    val allCorpora = corpusManager.getAllCorpora()
    val validation = TestCorpusUtils.validateCorpus(allCorpora)
    println("   Total texts: ${validation.totalTexts}")
    println("   Total size: ${validation.totalSize} characters")
    println("   Empty texts: ${validation.emptyTexts}")
    println("   Duplicate texts: ${validation.duplicateTexts}")
    println("   Valid: ${validation.isValid}")
    if (validation.issues.isNotEmpty()) {
        println("   Issues:")
        validation.issues.forEach { issue ->
            println("     - $issue")
        }
    }
    
    println("\n=== Example Complete ===")
}