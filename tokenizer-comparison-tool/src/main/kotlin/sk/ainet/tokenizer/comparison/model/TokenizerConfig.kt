package sk.ainet.tokenizer.comparison.model

import kotlinx.serialization.Serializable

@Serializable
data class TokenizerConfig(
    val type: TokenizerType,
    val vocabSize: Int,
    val merges: List<Pair<Pair<Int, Int>, Int>>,
    val specialTokens: Map<String, Int> = emptyMap(),
    val pattern: String = "",
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
enum class TokenizerType {
    BASIC, REGEX, GPT4
}

@Serializable
enum class TestBehavior {
    IDENTICAL_TOKENS,
    IDENTICAL_ROUNDTRIP,
    ERROR_HANDLING
}

@Serializable
enum class TestCategory {
    BASIC, UNICODE, EDGE_CASE, REFERENCE, PERFORMANCE
}