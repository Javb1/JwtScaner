// com.brandy.core.utils.JWTUtils
package com.brandy.core.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.util.Base64;

public class JWTUtils {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final ObjectWriter prettyWriter = mapper.writerWithDefaultPrettyPrinter();

    public static String parseAndFormat(String jwt) {
        try {
            String[] parts = parseJWT(jwt);
            String header = decodeAndFormat(parts[0]);
            String payload = decodeAndFormat(parts[1]);
            return "Header:\n" + header + "\n\nPayload:\n" + payload;
        } catch (Exception e) {
            return "JWT Parse Error: " + e.getMessage();
        }
    }

    public static String[] parseJWT(String jwt) throws IllegalArgumentException {
        String[] parts = jwt.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid JWT format");
        }
        return parts;
    }

    public static String decodeAndFormat(String part) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(part));
            return formatJSON(decoded);
        } catch (Exception e) {
            return "Decode error: " + e.getMessage();
        }
    }

    private static String formatJSON(String raw) {
        try {
            return prettyWriter.writeValueAsString(mapper.readTree(raw));
        } catch (JsonProcessingException e) {
            return raw;
        } catch (Exception e) {
            return "Invalid JSON: " + raw;
        }
    }
}