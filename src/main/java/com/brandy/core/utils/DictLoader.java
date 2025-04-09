package com.brandy.core.utils;

import com.brandy.core.model.KeyCandidate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Stream;
import java.util.HexFormat;


public class DictLoader {
    private static final String BUILTIN_DICTIONARY_PATH = "builtin_dictionary.txt";
    
    /**
     * 从文件路径加载字典
     * @param path 字典文件路径
     * @return 密钥候选流
     * @throws IOException 如果文件读取失败
     */
    public static Stream<KeyCandidate> loadDictionary(Path path) throws IOException {
        return Files.lines(path)
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .flatMap(DictLoader::generateKeyVariants);
    }
    
    /**
     * 加载内置字典
     * @return 密钥候选流
     * @throws IOException 如果资源文件读取失败
     */
    public static Stream<KeyCandidate> loadBuiltinDictionary() throws IOException {
        // 先读取所有行到内存中，避免流关闭问题
        List<String> lines = new ArrayList<>();
        
        // 尝试从类加载器加载资源
        InputStream is = DictLoader.class.getClassLoader().getResourceAsStream(BUILTIN_DICTIONARY_PATH);
        if (is == null) {
            // 如果从类加载器加载失败，尝试从当前类加载
            is = DictLoader.class.getResourceAsStream("/" + BUILTIN_DICTIONARY_PATH);
        }
        
        if (is == null) {
            throw new IOException("无法找到内置字典文件: " + BUILTIN_DICTIONARY_PATH);
        }
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line.trim());
            }
        }
        
        // 过滤空行并生成变体
        return lines.stream()
                .filter(line -> !line.isEmpty())
                .flatMap(DictLoader::generateKeyVariants);
    }

    private static Stream<KeyCandidate> generateKeyVariants(String originalKey) {
        List<KeyCandidate> variants = new ArrayList<>();

        // 原始密钥
        variants.add(new KeyCandidate(originalKey, originalKey));

        // Base64编码变体
        String base64Key = Base64.getEncoder().encodeToString(originalKey.getBytes());
        variants.add(new KeyCandidate(originalKey, base64Key));

        // Hex编码变体
        String hexKey = HexFormat.of().formatHex(originalKey.getBytes());
        variants.add(new KeyCandidate(originalKey, hexKey));

        return variants.stream();
    }
}
