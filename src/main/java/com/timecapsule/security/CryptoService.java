package com.timecapsule.security;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;

/**
 * Cryptography (Module: Cryptography & Security).
 * Passwords are stretched with PBKDF2-HMAC-SHA256 into a 256-bit key, then
 * memory bodies are sealed with AES-256-GCM (fresh random IV per entry, so
 * identical entries encrypt differently; tampering fails authentication).
 */
public final class CryptoService {

    private static final String KDF = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 210_000;
    private static final int KEY_BITS = 256;
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_BYTES = 12;
    private static final int SALT_BYTES = 16;

    private static final SecureRandom RANDOM = new SecureRandom();

    /** Sealed payload: random IV plus ciphertext (tag included). */
    public record Sealed(byte[] iv, byte[] cipher) {
    }

    private CryptoService() {
    }

    public static byte[] newSalt() {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        return salt;
    }

    public static SecretKey deriveKey(char[] password, byte[] salt) throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_BITS);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(KDF);
            return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
        } finally {
            spec.clearPassword();
        }
    }

    public static Sealed encrypt(byte[] plain, SecretKey key) throws GeneralSecurityException {
        byte[] iv = new byte[IV_BYTES];
        RANDOM.nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
        return new Sealed(iv, cipher.doFinal(plain));
    }

    public static byte[] decrypt(Sealed sealed, SecretKey key) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, sealed.iv()));
        return cipher.doFinal(sealed.cipher());
    }

    public static Sealed encryptText(String text, SecretKey key) throws GeneralSecurityException {
        return encrypt(text.getBytes(StandardCharsets.UTF_8), key);
    }

    public static String decryptText(Sealed sealed, SecretKey key) throws GeneralSecurityException {
        return new String(decrypt(sealed, key), StandardCharsets.UTF_8);
    }
}
