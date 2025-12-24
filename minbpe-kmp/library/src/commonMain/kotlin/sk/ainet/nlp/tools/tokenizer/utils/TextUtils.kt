package sk.ainet.nlp.tools.tokenizer.utils

/**
 * Text processing utilities for tokenization.
 * 
 * This object provides utilities for handling text processing tasks related
 * to tokenization, including control character escaping and token rendering
 * for human-readable display.
 */
object TextUtils {
    
    /**
     * Replace control characters with escaped representations for display.
     * 
     * This function converts control characters (like newlines, tabs, etc.)
     * into their escaped string representations so they can be safely displayed
     * in logs, debug output, or user interfaces.
     * 
     * @param text The input text that may contain control characters
     * @return Text with control characters replaced by escape sequences
     * 
     * Requirements: 6.4 - Provide utilities for control character escaping
     */
    fun replaceControlCharacters(text: String): String {
        return text
            .replace("\\", "\\\\")     // Backslash itself (must be first)
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
            .replace("\b", "\\b")
            .replace("\u000C", "\\f")  // Form feed
            .replace("\u0007", "\\a")  // Bell
            .replace("\u000B", "\\v")  // Vertical tab
            .replace("\"", "\\\"")     // Double quote
            .replace("\'", "\\\'")     // Single quote
    }
    
    /**
     * Render a token (byte array) as a human-readable string.
     * 
     * This function converts a token's byte representation into a string
     * suitable for display, handling both printable characters and control
     * characters appropriately. Non-UTF-8 sequences are handled gracefully.
     * 
     * @param token The token as a byte array
     * @return Human-readable string representation of the token
     * 
     * Requirements: 6.4 - Provide token rendering with control character handling
     */
    fun renderToken(token: ByteArray): String {
        return try {
            // Try to decode as UTF-8
            val text = token.decodeToString()
            // Replace control characters with escape sequences
            replaceControlCharacters(text)
        } catch (e: Exception) {
            // If UTF-8 decoding fails, show as hex bytes
            token.joinToString("") { byte ->
                val hex = (byte.toInt() and 0xFF).toString(16).padStart(2, '0')
                "\\x$hex"
            }
        }
    }
    
    /**
     * Convert a string to its UTF-8 byte representation.
     * 
     * This is a utility function to convert strings to byte arrays using UTF-8 encoding,
     * which is the standard encoding used throughout the BPE tokenization process.
     * 
     * @param text The input string
     * @return UTF-8 encoded byte array
     * 
     * Requirements: 6.3 - Handle UTF-8 encoding correctly
     */
    fun stringToBytes(text: String): ByteArray {
        return text.encodeToByteArray()
    }
    
    /**
     * Convert UTF-8 bytes back to a string.
     * 
     * This is a utility function to convert byte arrays back to strings using UTF-8 decoding.
     * Invalid UTF-8 sequences are replaced with the Unicode replacement character.
     * 
     * @param bytes The UTF-8 encoded byte array
     * @return Decoded string
     * 
     * Requirements: 6.3 - Handle UTF-8 decoding correctly
     */
    fun bytesToString(bytes: ByteArray): String {
        return bytes.decodeToString()
    }
}