package com.victorqueiroga.fastnotify.origem;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;

public final class Crypto {
    private static final int IV_LEN = 12;
    private static final int TAG_BITS = 128;
    private static final byte[] SALT =
            "FastNotify/AES-GCM/PSK/v1".getBytes(StandardCharsets.UTF_8);
    private static final int PBKDF2_ITERATIONS = 120_000;
    private static final SecureRandom RANDOM = new SecureRandom();

    private static volatile String cachedPsk;
    private static volatile SecretKey cachedKey;

    private Crypto() {
    }

    public static boolean isEncrypted(String psk) {
        return psk != null && !psk.isEmpty();
    }

    public static byte[] encrypt(String psk, byte[] plain) throws IOException {
        if (!isEncrypted(psk)) {
            throw new IOException("PSK vazia — cifra indisponível");
        }
        try {
            byte[] iv = new byte[IV_LEN];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keyFor(psk),
                    new GCMParameterSpec(TAG_BITS, iv));
            byte[] ct = cipher.doFinal(plain);
            byte[] out = new byte[IV_LEN + ct.length];
            System.arraycopy(iv, 0, out, 0, IV_LEN);
            System.arraycopy(ct, 0, out, IV_LEN, ct.length);
            return out;
        } catch (GeneralSecurityException e) {
            throw new IOException("Falha ao cifrar: " + e.getMessage(), e);
        }
    }

    public static byte[] decrypt(String psk, byte[] blob) throws IOException {
        if (!isEncrypted(psk)) {
            throw new IOException("PSK vazia — não é possível decifrar");
        }
        if (blob == null || blob.length <= IV_LEN) {
            throw new IOException("Payload cifrado inválido");
        }
        try {
            byte[] iv = Arrays.copyOfRange(blob, 0, IV_LEN);
            byte[] ct = Arrays.copyOfRange(blob, IV_LEN, blob.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keyFor(psk),
                    new GCMParameterSpec(TAG_BITS, iv));
            return cipher.doFinal(ct);
        } catch (GeneralSecurityException e) {
            throw new IOException(
                    "Decifragem falhou — PSK incorreta ou payload alterado", e);
        }
    }

    private static SecretKey keyFor(String psk) {
        String value = psk == null ? "" : psk;
        SecretKey key = cachedKey;
        if (key != null && value.equals(cachedPsk)) {
            return key;
        }
        synchronized (Crypto.class) {
            if (cachedKey != null && value.equals(cachedPsk)) {
                return cachedKey;
            }
            try {
                SecretKeyFactory factory =
                        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
                PBEKeySpec spec =
                        new PBEKeySpec(value.toCharArray(), SALT, PBKDF2_ITERATIONS, 256);
                byte[] raw = factory.generateSecret(spec).getEncoded();
                spec.clearPassword();
                SecretKey derived = new SecretKeySpec(raw, "AES");
                Arrays.fill(raw, (byte) 0);
                cachedPsk = value;
                cachedKey = derived;
                return derived;
            } catch (GeneralSecurityException e) {
                throw new IllegalStateException("Derivação de chave PSK indisponível", e);
            }
        }
    }
}
