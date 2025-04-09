package com.brandy.core.model;

public class KeyCandidate {
    private final String originalKey;
    private final String encodedKey;

    public KeyCandidate(String originalKey, String encodedKey) {
        this.originalKey = originalKey;
        this.encodedKey = encodedKey;
    }

    public String getOriginalKey() { return originalKey; }
    public String getEncodedKey() { return encodedKey; }
}
