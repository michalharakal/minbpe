/**
 * Kotlin Multiplatform implementation of minbpe (minimal Byte Pair Encoding) tokenizer.
 * 
 * This package provides a clean, efficient implementation of BPE tokenization algorithms
 * commonly used in Large Language Models, with full compatibility across JVM, Android,
 * iOS, macOS, Linux, JavaScript, and WebAssembly platforms.
 * 
 * The main components include:
 * - [BasicTokenizer]: Simple BPE implementation for basic tokenization
 * - [RegexTokenizer]: Production-grade tokenization with regex splitting and special tokens  
 * - [GPT4Tokenizer]: Exact tiktoken compatibility for GPT-4 models
 * - [utils]: Core utilities for BPE operations and text processing
 * 
 * @since 1.0.0
 */
package sk.ainet.nlp.tools.tokenizer