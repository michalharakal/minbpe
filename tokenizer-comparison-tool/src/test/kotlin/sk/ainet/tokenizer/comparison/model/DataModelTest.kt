package sk.ainet.tokenizer.comparison.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class DataModelTest {
    
    val json = Json { 
        prettyPrint = true 
        allowStructuredMapKeys = true
    }
    
    @Test
    fun `TokenizerConfig serialization round trip`() {
        val config = TokenizerConfig(
            type = TokenizerType.BASIC,
            vocabSize = 512,
            merges = listOf(Pair(Pair(1, 2), 256), Pair(Pair(3, 4), 257)),
            specialTokens = mapOf("<pad>" to 0, "<unk>" to 1),
            pattern = "\\w+",
            metadata = mapOf("version" to "1.0", "author" to "test")
        )
        
        val jsonString = json.encodeToString(config)
        val deserializedConfig = json.decodeFromString<TokenizerConfig>(jsonString)
        
        assertEquals(config, deserializedConfig)
    }
    
    @Test
    fun `TestCase serialization round trip`() {
        val config = TokenizerConfig(
            type = TokenizerType.REGEX,
            vocabSize = 256,
            merges = emptyList()
        )
        
        val testCase = TestCase(
            name = "basic_test",
            input = "Hello world",
            config = config,
            expectedBehavior = TestBehavior.IDENTICAL_TOKENS,
            category = TestCategory.BASIC
        )
        
        val jsonString = json.encodeToString(testCase)
        val deserializedTestCase = json.decodeFromString<TestCase>(jsonString)
        
        assertEquals(testCase, deserializedTestCase)
    }
    
    @Test
    fun `BenchmarkResult serialization round trip`() {
        val scenario = BenchmarkScenario(
            name = "training_benchmark",
            operation = BenchmarkOperation.TRAIN,
            text = "Sample text for training",
            vocabSize = 512,
            tokenizerType = TokenizerType.BASIC
        )
        
        val pythonStats = PerformanceStats(
            meanTime = 100.0,
            stdDevTime = 5.0,
            meanMemory = 1024.0,
            stdDevMemory = 50.0,
            throughput = 1000.0
        )
        
        val kotlinStats = PerformanceStats(
            meanTime = 80.0,
            stdDevTime = 4.0,
            meanMemory = 900.0,
            stdDevMemory = 40.0,
            throughput = 1200.0
        )
        
        val benchmarkResult = BenchmarkResult(
            scenario = scenario,
            pythonMetrics = pythonStats,
            kotlinMetrics = kotlinStats,
            speedupRatio = 1.25,
            memoryRatio = 0.88
        )
        
        val jsonString = json.encodeToString(benchmarkResult)
        val deserializedResult = json.decodeFromString<BenchmarkResult>(jsonString)
        
        assertEquals(benchmarkResult, deserializedResult)
    }
    
    @Test
    fun `ValidationReport serialization round trip`() {
        val environment = EnvironmentInfo(
            hostOS = "macOS",
            jvmVersion = "21.0.1",
            kotlinVersion = "2.2.21",
            dockerVersion = "24.0.0",
            pythonVersion = "3.11.0",
            timestamp = "2024-01-01T00:00:00Z"
        )
        
        // Test EnvironmentInfo serialization first
        val envJson = json.encodeToString(environment)
        val deserializedEnv = json.decodeFromString<EnvironmentInfo>(envJson)
        assertEquals(environment, deserializedEnv)
        
        val compatibilityResults = CompatibilityResults(
            totalTests = 10,
            passedTests = 9,
            failedTests = 1,
            testResults = emptyList(),
            failuresByCategory = emptyMap()
        )
        
        // Test CompatibilityResults serialization
        val compatJson = json.encodeToString(compatibilityResults)
        val deserializedCompat = json.decodeFromString<CompatibilityResults>(compatJson)
        assertEquals(compatibilityResults, deserializedCompat)
        
        val benchmarkSummary = BenchmarkSummary(
            totalBenchmarks = 5,
            averageSpeedup = 1.2,
            averageMemoryRatio = 0.9,
            fastestOperation = "encoding",
            slowestOperation = "training"
        )
        
        val benchmarkResults = BenchmarkResults(
            trainingBenchmarks = emptyMap(),
            encodingBenchmarks = emptyMap(),
            decodingBenchmarks = emptyMap(),
            overallSummary = benchmarkSummary
        )
        
        // Test BenchmarkResults serialization
        val benchJson = json.encodeToString(benchmarkResults)
        val deserializedBench = json.decodeFromString<BenchmarkResults>(benchJson)
        assertEquals(benchmarkResults, deserializedBench)
        
        val summary = ReportSummary(
            overallCompatibility = 90.0,
            criticalFailures = listOf("Unicode handling mismatch"),
            performanceHighlights = listOf("Kotlin 20% faster on average"),
            recommendations = listOf("Review Unicode normalization")
        )
        
        // Test ReportSummary serialization
        val summaryJson = json.encodeToString(summary)
        val deserializedSummary = json.decodeFromString<ReportSummary>(summaryJson)
        assertEquals(summary, deserializedSummary)
        
        val report = ValidationReport(
            timestamp = "2024-01-01T00:00:00Z",
            environment = environment,
            compatibilityResults = compatibilityResults,
            benchmarkResults = benchmarkResults,
            summary = summary
        )
        
        try {
            val jsonString = json.encodeToString(report)
            val deserializedReport = json.decodeFromString<ValidationReport>(jsonString)
            assertEquals(report, deserializedReport)
        } catch (e: Exception) {
            println("Serialization failed: ${e.message}")
            println("Exception: $e")
            throw e
        }
    }
}