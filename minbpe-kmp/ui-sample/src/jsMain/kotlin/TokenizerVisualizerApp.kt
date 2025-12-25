import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.html.*
import kotlinx.html.dom.append
import kotlinx.html.js.onClickFunction
import kotlinx.html.js.onInputFunction
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLTextAreaElement
import sk.ainet.nlp.tools.tokenizer.BasicTokenizer

class TokenizerVisualizerApp {
    private val tokenizer = BasicTokenizer()
    private var isTokenizerTrained = false
    
    fun start() {
        document.body?.append {
            div("container") {
                h1 { +"MinBPE Tokenizer Visualizer" }
                
                div("github-link") {
                    style = "text-align: center; margin-bottom: 20px;"
                    a(href = "https://github.com/michalharakal/minbpe", target = "_blank") {
                        +"🔗 View on GitHub"
                        style = "color: #007bff; text-decoration: none; font-weight: bold;"
                    }
                }
                
                // Training section
                div("section") {
                    h2 { +"1. Train Tokenizer" }
                    textArea {
                        id = "training-text"
                        placeholder = "Enter training text here..."
                        rows = "4"
                        style = "width: 100%; margin-bottom: 10px;"
                        +"""Hello world! This is a sample text for training the tokenizer. 
The tokenizer will learn byte-pair encodings from this text."""
                    }
                    
                    div("controls") {
                        label {
                            +"Vocabulary Size: "
                            input(InputType.number) {
                                id = "vocab-size"
                                value = "300"
                                min = "256"
                                max = "1000"
                                style = "margin-right: 10px;"
                            }
                        }
                        
                        button {
                            id = "train-btn"
                            +"Train Tokenizer"
                            onClickFunction = { trainTokenizer() }
                        }
                    }
                    
                    div("status") {
                        id = "training-status"
                        style = "margin-top: 10px; font-weight: bold;"
                    }
                }
                
                // Tokenization section
                div("section") {
                    h2 { +"2. Tokenize Text" }
                    textArea {
                        id = "input-text"
                        placeholder = "Enter text to tokenize..."
                        rows = "3"
                        style = "width: 100%; margin-bottom: 10px;"
                        +"Hello world!"
                        onInputFunction = { tokenizeText() }
                    }
                    
                    button {
                        id = "tokenize-btn"
                        +"Tokenize"
                        onClickFunction = { tokenizeText() }
                    }
                }
                
                // Results section
                div("section") {
                    h2 { +"3. Visualization" }
                    
                    div("result-section") {
                        h3 { +"Token IDs:" }
                        div("token-ids") {
                            id = "token-ids"
                            style = "font-family: monospace; background: #f5f5f5; padding: 10px; border-radius: 4px; margin-bottom: 10px;"
                        }
                    }
                    
                    div("result-section") {
                        h3 { +"Token Visualization:" }
                        div("token-flow") {
                            id = "token-flow"
                            style = "display: flex; flex-wrap: wrap; gap: 5px; margin-bottom: 10px;"
                        }
                    }
                    
                    div("result-section") {
                        h3 { +"Special Characters & Mappings:" }
                        div("token-details") {
                            id = "token-details"
                            style = "font-family: monospace; background: #f9f9f9; padding: 10px; border-radius: 4px; max-height: 300px; overflow-y: auto;"
                        }
                    }
                    
                    div("result-section") {
                        h3 { +"Decoded Text:" }
                        div("decoded-text") {
                            id = "decoded-text"
                            style = "background: #e8f5e8; padding: 10px; border-radius: 4px; font-family: monospace;"
                        }
                    }
                }
            }
        }
        
        // Add CSS styles
        addStyles()
    }
    
    private fun trainTokenizer() {
        val trainingText = (document.getElementById("training-text") as HTMLTextAreaElement).value
        val vocabSize = (document.getElementById("vocab-size") as HTMLInputElement).value.toIntOrNull() ?: 300
        val statusDiv = document.getElementById("training-status") as HTMLElement
        
        if (trainingText.isBlank()) {
            statusDiv.innerHTML = "<span style='color: red;'>Please enter training text</span>"
            return
        }
        
        try {
            statusDiv.innerHTML = "<span style='color: blue;'>Training tokenizer...</span>"
            
            tokenizer.train(trainingText, vocabSize, verbose = true)
            isTokenizerTrained = true
            
            statusDiv.innerHTML = "<span style='color: green;'>✓ Tokenizer trained successfully! Vocabulary size: ${tokenizer.vocab.size}</span>"
            
            // Auto-tokenize current input if any
            tokenizeText()
            
        } catch (e: Exception) {
            statusDiv.innerHTML = "<span style='color: red;'>Training failed: ${e.message}</span>"
            console.error("Training error:", e)
        }
    }
    
    private fun tokenizeText() {
        if (!isTokenizerTrained) {
            updateResults(emptyList(), "Please train the tokenizer first")
            return
        }
        
        val inputText = (document.getElementById("input-text") as HTMLTextAreaElement).value
        
        if (inputText.isBlank()) {
            updateResults(emptyList(), "")
            return
        }
        
        try {
            val tokenIds = tokenizer.encode(inputText)
            val decodedText = tokenizer.decode(tokenIds)
            updateResults(tokenIds, decodedText)
        } catch (e: Exception) {
            updateResults(emptyList(), "Tokenization failed: ${e.message}")
            console.error("Tokenization error:", e)
        }
    }
    
    private fun updateResults(tokenIds: List<Int>, decodedText: String) {
        // Update token IDs display
        val tokenIdsDiv = document.getElementById("token-ids") as HTMLElement
        tokenIdsDiv.textContent = tokenIds.joinToString(", ")
        
        // Update token flow visualization
        val tokenFlowDiv = document.getElementById("token-flow") as HTMLElement
        tokenFlowDiv.innerHTML = ""
        
        tokenIds.forEach { tokenId ->
            val tokenBytes = tokenizer.vocab[tokenId]
            val tokenText = tokenBytes?.let { bytes ->
                try {
                    bytes.decodeToString()
                } catch (e: Exception) {
                    // Show hex for non-printable bytes
                    bytes.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0').uppercase() }
                }
            } ?: "?"
            
            tokenFlowDiv.append {
                span("token-box") {
                    title = "Token ID: $tokenId"
                    +tokenText
                    attributes["data-token-id"] = tokenId.toString()
                }
            }
        }
        
        // Update token details
        val tokenDetailsDiv = document.getElementById("token-details") as HTMLElement
        val details = buildString {
            appendLine("Total tokens: ${tokenIds.size}")
            appendLine("Unique tokens: ${tokenIds.toSet().size}")
            appendLine()
            appendLine("Token mappings:")
            
            tokenIds.forEachIndexed { index, tokenId ->
                val tokenBytes = tokenizer.vocab[tokenId]
                val tokenText = tokenBytes?.let { bytes ->
                    try {
                        val text = bytes.decodeToString()
                        if (text.any { it.isISOControl() }) {
                            // Show both text and hex for control characters
                            "$text [${bytes.joinToString(" ") { (it.toInt() and 0xFF).toString(16).padStart(2, '0').uppercase() }}]"
                        } else {
                            text
                        }
                    } catch (e: Exception) {
                        "[${bytes.joinToString(" ") { (it.toInt() and 0xFF).toString(16).padStart(2, '0').uppercase() }}]"
                    }
                } ?: "UNKNOWN"
                
                appendLine("[$index] ID:$tokenId → \"$tokenText\"")
            }
        }
        tokenDetailsDiv.textContent = details
        
        // Update decoded text
        val decodedDiv = document.getElementById("decoded-text") as HTMLElement
        decodedDiv.textContent = decodedText
    }
    
    private fun addStyles() {
        document.head?.append {
            style {
                unsafe {
                    +"""
                .container {
                    max-width: 1200px;
                    margin: 0 auto;
                    padding: 20px;
                    font-family: Arial, sans-serif;
                }
                
                .section {
                    margin-bottom: 30px;
                    padding: 20px;
                    border: 1px solid #ddd;
                    border-radius: 8px;
                    background: #fafafa;
                }
                
                .controls {
                    margin-bottom: 10px;
                }
                
                .controls label {
                    margin-right: 15px;
                }
                
                button {
                    background: #007bff;
                    color: white;
                    border: none;
                    padding: 8px 16px;
                    border-radius: 4px;
                    cursor: pointer;
                    font-size: 14px;
                }
                
                button:hover {
                    background: #0056b3;
                }
                
                .github-link a:hover {
                    color: #0056b3;
                    text-decoration: underline;
                }
                
                .token-box {
                    display: inline-block;
                    background: #e3f2fd;
                    border: 1px solid #2196f3;
                    border-radius: 4px;
                    padding: 4px 8px;
                    margin: 2px;
                    font-family: monospace;
                    font-size: 12px;
                    cursor: pointer;
                    position: relative;
                }
                
                .token-box:hover {
                    background: #bbdefb;
                    transform: scale(1.05);
                }
                
                .token-box:hover::after {
                    content: "ID: " attr(data-token-id);
                    position: absolute;
                    bottom: 100%;
                    left: 50%;
                    transform: translateX(-50%);
                    background: #333;
                    color: white;
                    padding: 4px 8px;
                    border-radius: 4px;
                    font-size: 10px;
                    white-space: nowrap;
                    z-index: 1000;
                }
                
                .result-section {
                    margin-bottom: 20px;
                }
                
                h1 {
                    color: #333;
                    text-align: center;
                }
                
                h2 {
                    color: #555;
                    border-bottom: 2px solid #007bff;
                    padding-bottom: 5px;
                }
                
                h3 {
                    color: #666;
                    margin-bottom: 10px;
                }
                
                textarea, input {
                    border: 1px solid #ccc;
                    border-radius: 4px;
                    padding: 8px;
                    font-size: 14px;
                }
                
                textarea:focus, input:focus {
                    outline: none;
                    border-color: #007bff;
                    box-shadow: 0 0 5px rgba(0,123,255,0.3);
                }
                """.trimIndent()
                }
            }
        }
    }
}

fun main() {
    window.onload = {
        TokenizerVisualizerApp().start()
    }
}