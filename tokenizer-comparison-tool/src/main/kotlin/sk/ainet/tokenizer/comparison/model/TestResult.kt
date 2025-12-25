package sk.ainet.tokenizer.comparison.model

import kotlinx.serialization.Serializable

/**
 * Result of a test execution.
 */
@Serializable
data class TestResult(
    val success: Boolean,
    val tokens: List<Int> = emptyList(),
    val text: String = "",
    val executionTime: Long = 0L,
    val memoryUsage: Long = 0L,
    val errorMessage: String? = null
)

/**
 * Result of a test case execution comparing both implementations.
 */
@Serializable
data class TestCaseResult(
    val testCase: TestCase,
    val pythonResult: TestResult,
    val kotlinResult: TestResult,
    val compatible: Boolean,
    val executionTime: Pair<Long, Long>, // Python, Kotlin
    val errorMessage: String? = null
)

/**
 * Test case definition.
 */
@Serializable
data class TestCase(
    val name: String,
    val input: String,
    val config: TokenizerConfig,
    val expectedBehavior: TestBehavior,
    val category: TestCategory = TestCategory.BASIC
)