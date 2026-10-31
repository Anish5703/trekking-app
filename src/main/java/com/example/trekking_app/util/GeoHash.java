package com.example.trekking_app.util;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
public final class GeoHash {
    private GeoHash() {}
    public static String sha256(byte[] bytes) { return HexFormat.of().formatHex(digest(bytes)); }
    public static String sha256(String text) { return sha256(text.getBytes(StandardCharsets.UTF_8)); }
    private static byte[] digest(byte[] bytes) {
        try { return MessageDigest.getInstance("SHA-256").digest(bytes); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException("SHA-256 is required by the JVM", e); }
    }
}
