package sk.ainet.tokenizer.comparison.model

import kotlinx.serialization.Serializable

/**
 * Benchmark scenario configuration.
 */
@Serializable
data class BenchmarkScenario(
    val name: String,
    val operation: BenchmarkOperation,
    val text: String,
    val vocabSize: Int,
    val tokenizerType: TokenizerType,
    val config: TokenizerConfig? = null,
    val tokens: List<Int> = emptyList()
)

/**
 * Result of a benchmark execution.
 */
@Serializable
data class BenchmarkResult(
    val scenario: BenchmarkScenario,
    val pythonMetrics: PerformanceStats,
    val kotlinMetrics: PerformanceStats,
    val speedupRatio: Double,
    val memoryRatio: Double
)

/**
 * Performance statistics for a benchmark.
 */
@Serializable
data class PerformanceStats(
    val meanTime: Double,
    val stdDevTime: Double,
    val meanMemory: Double,
    val stdDevMemory: Double,
    val throughput: Double? = null // tokens/second for encoding/decoding
)

/**
 * Key for benchmark result mapping.
 */
@Serializable
data class BenchmarkKey(
    val operation: String,
    val textSize: Int,
    val vocabSize: Int
)

/**
 * Metrics for benchmark comparison.
 */
@Serializable
data class BenchmarkMetrics(
    val pythonStats: PerformanceStats,
    val kotlinStats: PerformanceStats,
    val speedupRatio: Double
)

/**
 * Summary of all benchmark results.
 */
@Serializable
data class BenchmarkSummary(
    val totalBenchmarks: Int,
    val averageSpeedup: Double,
    val averageMemoryRatio: Double,
    val fastestOperation: String,
    val slowestOperation: String
)

/**
 * Types of benchmark operations.
 */
@Serializable
enum class BenchmarkOperation {
    TRAIN, ENCODE, DECODE
}