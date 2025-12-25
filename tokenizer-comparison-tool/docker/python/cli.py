#!/usr/bin/env python3
"""
Python CLI for minbpe tokenizer operations.
This script provides a consistent interface for the comparison tool.
"""

import json
import sys
import argparse
import os
import logging
import traceback
from pathlib import Path
from typing import Dict, Any, List

# Add minbpe to path (will be mounted from host)
sys.path.insert(0, '/shared/minbpe')

try:
    from minbpe import BasicTokenizer, RegexTokenizer, GPT4Tokenizer
except ImportError as e:
    print(json.dumps({
        "status": "error",
        "error": f"Failed to import minbpe: {e}",
        "implementation": "python"
    }))
    sys.exit(1)

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

def create_tokenizer(tokenizer_type: str):
    """Create a tokenizer instance based on type."""
    if tokenizer_type == 'basic':
        return BasicTokenizer()
    elif tokenizer_type == 'regex':
        return RegexTokenizer()
    elif tokenizer_type == 'gpt4':
        return GPT4Tokenizer()
    else:
        raise ValueError(f"Unknown tokenizer type: {tokenizer_type}")

def export_tokenizer_config(tokenizer, tokenizer_type: str) -> Dict[str, Any]:
    """Export tokenizer configuration to JSON-compatible format."""
    config = {
        "type": tokenizer_type,
        "vocab_size": len(tokenizer.vocab),
        "merges": list(tokenizer.merges.items()) if hasattr(tokenizer, 'merges') else [],
        "special_tokens": dict(tokenizer.special_tokens) if hasattr(tokenizer, 'special_tokens') else {},
        "pattern": getattr(tokenizer, 'pattern', ''),
        "metadata": {
            "implementation": "python",
            "version": "1.0"
        }
    }
    
    # Handle GPT4 tokenizer special case
    if tokenizer_type == 'gpt4' and hasattr(tokenizer, 'byte_shuffle'):
        config["byte_shuffle"] = dict(tokenizer.byte_shuffle)
    
    return config

def load_tokenizer_from_config(config: Dict[str, Any]):
    """Load tokenizer from JSON configuration."""
    tokenizer_type = config["type"]
    tokenizer = create_tokenizer(tokenizer_type)
    
    # Set merges if available
    if "merges" in config and config["merges"]:
        tokenizer.merges = {tuple(pair): idx for pair, idx in config["merges"]}
        tokenizer.vocab = tokenizer._build_vocab()
    
    # Set special tokens if available
    if "special_tokens" in config and config["special_tokens"]:
        if hasattr(tokenizer, 'register_special_tokens'):
            tokenizer.register_special_tokens(config["special_tokens"])
        else:
            tokenizer.special_tokens = config["special_tokens"]
    
    # Set pattern if available
    if "pattern" in config and config["pattern"]:
        tokenizer.pattern = config["pattern"]
        if hasattr(tokenizer, 'compiled_pattern'):
            import regex as re
            tokenizer.compiled_pattern = re.compile(tokenizer.pattern)
    
    # Handle GPT4 tokenizer special case
    if tokenizer_type == 'gpt4' and "byte_shuffle" in config:
        tokenizer.byte_shuffle = config["byte_shuffle"]
        tokenizer.inverse_byte_shuffle = {v: k for k, v in tokenizer.byte_shuffle.items()}
    
    return tokenizer

def handle_train_command(args) -> Dict[str, Any]:
    """Handle the train command."""
    try:
        logger.info(f"Training {args.type} tokenizer with vocab size {args.vocab_size}")
        
        # Read training text
        if os.path.isfile(args.text):
            with open(args.text, 'r', encoding='utf-8') as f:
                text = f.read()
        else:
            text = args.text
        
        # Create and train tokenizer
        tokenizer = create_tokenizer(args.type)
        
        # GPT4 tokenizer is pretrained, cannot be trained
        if args.type == 'gpt4':
            logger.info("GPT4 tokenizer is pretrained, skipping training")
        else:
            tokenizer.train(text, args.vocab_size, verbose=False)
        
        # Export configuration
        config = export_tokenizer_config(tokenizer, args.type)
        
        # Save configuration
        with open(args.output, 'w', encoding='utf-8') as f:
            json.dump(config, f, indent=2)
        
        logger.info(f"Tokenizer configuration saved to {args.output}")
        
        return {
            "status": "success",
            "command": "train",
            "tokenizer_type": args.type,
            "vocab_size": len(tokenizer.vocab),
            "output_file": args.output,
            "implementation": "python"
        }
        
    except Exception as e:
        logger.error(f"Training failed: {e}")
        return {
            "status": "error",
            "command": "train",
            "error": str(e),
            "traceback": traceback.format_exc(),
            "implementation": "python"
        }

def handle_encode_command(args) -> Dict[str, Any]:
    """Handle the encode command."""
    try:
        logger.info(f"Encoding text using config from {args.config}")
        
        # Load tokenizer configuration
        with open(args.config, 'r', encoding='utf-8') as f:
            config = json.load(f)
        
        tokenizer = load_tokenizer_from_config(config)
        
        # Read input text
        if os.path.isfile(args.text):
            with open(args.text, 'r', encoding='utf-8') as f:
                text = f.read()
        else:
            text = args.text
        
        # Encode text
        tokens = tokenizer.encode(text)
        
        # Save tokens
        with open(args.output, 'w', encoding='utf-8') as f:
            json.dump(tokens, f)
        
        logger.info(f"Encoded {len(text)} characters to {len(tokens)} tokens")
        
        return {
            "status": "success",
            "command": "encode",
            "input_length": len(text),
            "token_count": len(tokens),
            "tokens": tokens,
            "output_file": args.output,
            "implementation": "python"
        }
        
    except Exception as e:
        logger.error(f"Encoding failed: {e}")
        return {
            "status": "error",
            "command": "encode",
            "error": str(e),
            "traceback": traceback.format_exc(),
            "implementation": "python"
        }

def handle_decode_command(args) -> Dict[str, Any]:
    """Handle the decode command."""
    try:
        logger.info(f"Decoding tokens using config from {args.config}")
        
        # Load tokenizer configuration
        with open(args.config, 'r', encoding='utf-8') as f:
            config = json.load(f)
        
        tokenizer = load_tokenizer_from_config(config)
        
        # Read tokens
        if os.path.isfile(args.tokens):
            with open(args.tokens, 'r', encoding='utf-8') as f:
                tokens = json.load(f)
        else:
            tokens = json.loads(args.tokens)
        
        # Decode tokens
        text = tokenizer.decode(tokens)
        
        # Save decoded text
        with open(args.output, 'w', encoding='utf-8') as f:
            f.write(text)
        
        logger.info(f"Decoded {len(tokens)} tokens to {len(text)} characters")
        
        return {
            "status": "success",
            "command": "decode",
            "token_count": len(tokens),
            "output_length": len(text),
            "text": text,
            "output_file": args.output,
            "implementation": "python"
        }
        
    except Exception as e:
        logger.error(f"Decoding failed: {e}")
        return {
            "status": "error",
            "command": "decode",
            "error": str(e),
            "traceback": traceback.format_exc(),
            "implementation": "python"
        }

def handle_export_command(args) -> Dict[str, Any]:
    """Handle the export command."""
    try:
        logger.info(f"Exporting tokenizer config from {args.config}")
        
        # Load tokenizer configuration
        with open(args.config, 'r', encoding='utf-8') as f:
            config = json.load(f)
        
        tokenizer = load_tokenizer_from_config(config)
        
        # Export vocabulary if requested
        if hasattr(args, 'vocab') and args.vocab:
            vocab_data = {}
            for idx, token_bytes in tokenizer.vocab.items():
                try:
                    token_str = token_bytes.decode('utf-8', errors='replace')
                except:
                    token_str = str(token_bytes)
                vocab_data[idx] = token_str
            
            vocab_file = args.output.replace('.json', '_vocab.json')
            with open(vocab_file, 'w', encoding='utf-8') as f:
                json.dump(vocab_data, f, indent=2, ensure_ascii=False)
        
        # Save enhanced configuration
        enhanced_config = export_tokenizer_config(tokenizer, config["type"])
        with open(args.output, 'w', encoding='utf-8') as f:
            json.dump(enhanced_config, f, indent=2)
        
        logger.info(f"Configuration exported to {args.output}")
        
        return {
            "status": "success",
            "command": "export",
            "output_file": args.output,
            "vocab_size": len(tokenizer.vocab),
            "implementation": "python"
        }
        
    except Exception as e:
        logger.error(f"Export failed: {e}")
        return {
            "status": "error",
            "command": "export",
            "error": str(e),
            "traceback": traceback.format_exc(),
            "implementation": "python"
        }

def handle_load_command(args) -> Dict[str, Any]:
    """Handle the load command."""
    try:
        logger.info(f"Loading and validating tokenizer config from {args.config}")
        
        # Load and validate configuration
        with open(args.config, 'r', encoding='utf-8') as f:
            config = json.load(f)
        
        tokenizer = load_tokenizer_from_config(config)
        
        # Test basic functionality
        test_text = "Hello, world! This is a test."
        tokens = tokenizer.encode(test_text)
        decoded_text = tokenizer.decode(tokens)
        
        round_trip_success = test_text == decoded_text
        
        return {
            "status": "success",
            "command": "load",
            "config_file": args.config,
            "tokenizer_type": config["type"],
            "vocab_size": len(tokenizer.vocab),
            "round_trip_test": round_trip_success,
            "test_tokens": len(tokens),
            "implementation": "python"
        }
        
    except Exception as e:
        logger.error(f"Load failed: {e}")
        return {
            "status": "error",
            "command": "load",
            "error": str(e),
            "traceback": traceback.format_exc(),
            "implementation": "python"
        }

def main():
    parser = argparse.ArgumentParser(description='Python minbpe CLI')
    subparsers = parser.add_subparsers(dest='command', help='Available commands')
    
    # Train command
    train_parser = subparsers.add_parser('train', help='Train a tokenizer')
    train_parser.add_argument('--text', required=True, help='Training text or file path')
    train_parser.add_argument('--vocab-size', type=int, required=True, help='Vocabulary size')
    train_parser.add_argument('--type', choices=['basic', 'regex', 'gpt4'], required=True, help='Tokenizer type')
    train_parser.add_argument('--output', required=True, help='Output config file')
    
    # Encode command
    encode_parser = subparsers.add_parser('encode', help='Encode text')
    encode_parser.add_argument('--config', required=True, help='Tokenizer config file')
    encode_parser.add_argument('--text', required=True, help='Text to encode or file path')
    encode_parser.add_argument('--output', required=True, help='Output tokens file')
    
    # Decode command
    decode_parser = subparsers.add_parser('decode', help='Decode tokens')
    decode_parser.add_argument('--config', required=True, help='Tokenizer config file')
    decode_parser.add_argument('--tokens', required=True, help='Tokens JSON or file path')
    decode_parser.add_argument('--output', required=True, help='Output text file')
    
    # Export command
    export_parser = subparsers.add_parser('export', help='Export tokenizer configuration')
    export_parser.add_argument('--config', required=True, help='Tokenizer config file')
    export_parser.add_argument('--output', required=True, help='Output config file')
    export_parser.add_argument('--vocab', action='store_true', help='Also export vocabulary')
    
    # Load command
    load_parser = subparsers.add_parser('load', help='Load and validate tokenizer configuration')
    load_parser.add_argument('--config', required=True, help='Tokenizer config file')
    
    # Health check command
    health_parser = subparsers.add_parser('health', help='Health check')
    
    args = parser.parse_args()
    
    if not args.command:
        parser.print_help()
        return
    
    try:
        if args.command == 'health':
            result = {
                "status": "healthy",
                "implementation": "python",
                "available_tokenizers": ["basic", "regex", "gpt4"]
            }
        elif args.command == 'train':
            result = handle_train_command(args)
        elif args.command == 'encode':
            result = handle_encode_command(args)
        elif args.command == 'decode':
            result = handle_decode_command(args)
        elif args.command == 'export':
            result = handle_export_command(args)
        elif args.command == 'load':
            result = handle_load_command(args)
        else:
            result = {
                "status": "error",
                "error": f"Unknown command: {args.command}",
                "implementation": "python"
            }
        
        print(json.dumps(result, indent=2))
        
        # Exit with appropriate code
        if result.get("status") == "error":
            sys.exit(1)
            
    except Exception as e:
        error_result = {
            "status": "error",
            "error": str(e),
            "traceback": traceback.format_exc(),
            "implementation": "python"
        }
        print(json.dumps(error_result, indent=2))
        sys.exit(1)

if __name__ == '__main__':
    main()