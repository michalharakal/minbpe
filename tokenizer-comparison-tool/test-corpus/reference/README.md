# Reference Test Cases

This directory contains reference test cases with known expected outputs for validation purposes.

## Structure

- `basic/` - Simple text samples for basic tokenizer testing
- `unicode/` - Unicode character testing including emojis and non-Latin scripts
- `edge-cases/` - Edge cases like empty strings, whitespace, and special characters
- `reference/` - Reference implementations with expected outputs

## Usage

These test cases are automatically loaded by the TestCorpusManager and used for compatibility testing between Python and Kotlin implementations.