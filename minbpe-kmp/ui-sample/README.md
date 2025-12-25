# MinBPE Tokenizer Visualizer

A minimal web-based UI for visualizing the MinBPE tokenization process.

## Features

- **Interactive Training**: Train a BPE tokenizer on custom text with configurable vocabulary size
- **Real-time Tokenization**: See how text gets broken down into tokens
- **Visual Token Flow**: Each token is displayed as a colored box with hover tooltips showing token IDs
- **Special Character Mapping**: View detailed mappings including hex representations for special characters
- **Decode Verification**: Verify that tokenization is reversible by showing the decoded output

## How to Run

### Development Server
```bash
cd minbpe-kmp
./gradlew :ui-sample:jsBrowserDevelopmentRun
```

This will start a development server and automatically open your browser to `http://localhost:8080`.

### Build for Production
```bash
./gradlew :ui-sample:jsBrowserProductionWebpack
```

The built files will be in `ui-sample/build/dist/js/productionExecutable/`.

## Usage

1. **Train the Tokenizer**:
   - Enter training text in the first text area
   - Set desired vocabulary size (256-1000)
   - Click "Train Tokenizer"

2. **Tokenize Text**:
   - Enter text to tokenize in the second text area
   - The visualization updates automatically
   - Hover over token boxes to see token IDs

3. **Analyze Results**:
   - **Token IDs**: Raw list of token identifiers
   - **Token Visualization**: Visual flow of tokens with hover details
   - **Special Characters & Mappings**: Detailed breakdown including hex for special chars
   - **Decoded Text**: Verification that tokenization is reversible

## Example

Try training with:
```
Hello world! This is a sample text for training the tokenizer. 
The tokenizer will learn byte-pair encodings from this text.
```

Then tokenize: `"Hello world!"`

You'll see how the tokenizer breaks down the text into learned subword units.