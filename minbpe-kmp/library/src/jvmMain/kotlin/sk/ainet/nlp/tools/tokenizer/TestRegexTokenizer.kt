package sk.ainet.nlp.tools.tokenizer

fun main() {
    println("RegexTokenizer Verification")
    println("=".repeat(50))
    println()
    
    val results = RegexTokenizerVerification.runBasicVerification()
    println(results)
    
    println()
    println("=".repeat(50))
    println("Verification complete")
}