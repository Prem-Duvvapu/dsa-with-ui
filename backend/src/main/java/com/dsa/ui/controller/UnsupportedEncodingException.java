package com.dsa.ui.controller;

final class UnsupportedEncodingException extends RuntimeException {
    UnsupportedEncodingException(String encoding) {
        super("Unsupported trace encoding '" + encoding + "'. Use delta or full.");
    }
}
