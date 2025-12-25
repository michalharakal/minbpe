package sk.ainet.tokenizer.comparison.model

import kotlinx.serialization.Serializable

/**
 * Complete validation report containing all test and benchmark results.
 */
@Serializable
data class ValidationReport(
    val timestamp: String,
    val environment: EnvironmentInfo,
    val compatibilityResults: CompatibilityResults,
    val benchmarkResults: BenchmarkResults,
    val summary: ReportSummary
)

/**
 * Environment information for the test execution.
 */
@Serializable
data class EnvironmentInfo(
    val hostOS: String,
    val jvmVersion: String,
    val kotlinVersion: String,
    val dockerVersion: String,
    val pythonVersion: String,
    val timestamp: String,
    val additionalInfo: Map<String, String> = emptyMap()
)

/**
 * Results of compatibility testing.
 */
@Serializable
data class CompatibilityResults(
    val totalTests: Int,
    val passedTests: Int,
    val failedTests: Int,
    val testResults: List<TestCaseResult>,
    val failuresByCategory: Map<TestCategory, Int>
)

/**
 * Results of benchmark testing.
 */
@Serializable
data class BenchmarkResults(
    val trainingBenchmarks: Map<BenchmarkKey, BenchmarkMetrics>,
    val encodingBenchmarks: Map<BenchmarkKey, BenchmarkMetrics>,
    val decodingBenchmarks: Map<BenchmarkKey, BenchmarkMetrics>,
    val overallSummary: BenchmarkSummary
)

/**
 * Summary of the validation report.
 */
@Serializable
data class ReportSummary(
    val overallCompatibility: Double, // Percentage
    val criticalFailures: List<String>,
    val performanceHighlights: List<String>,
    val recommendations: List<String>
)