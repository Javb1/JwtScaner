package com.brandy.core.model;

import java.nio.file.Path;

public class AttackConfig {
    private final String jwtToken;
    private final String algorithm;
    private final Path dictionaryPath;
    private final int threadCount;
    private final boolean useBuiltinDictionary;

    public AttackConfig(String jwtToken, String algorithm, Path dictionaryPath, int threadCount) {
        this.jwtToken = jwtToken;
        this.algorithm = algorithm;
        this.dictionaryPath = dictionaryPath;
        this.threadCount = threadCount;
        this.useBuiltinDictionary = false;
    }
    
    public AttackConfig(String jwtToken, String algorithm, int threadCount) {
        this.jwtToken = jwtToken;
        this.algorithm = algorithm;
        this.dictionaryPath = null;
        this.threadCount = threadCount;
        this.useBuiltinDictionary = true;
    }

    public String getJwtToken() {
        return jwtToken;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public Path getDictionaryPath() {
        return dictionaryPath;
    }

    public int getThreadCount() {
        return threadCount;
    }
    
    public boolean isUseBuiltinDictionary() {
        return useBuiltinDictionary;
    }
}
