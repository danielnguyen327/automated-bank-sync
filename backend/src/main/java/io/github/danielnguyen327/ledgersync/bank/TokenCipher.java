package io.github.danielnguyen327.ledgersync.bank;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Encrypts Plaid access tokens with AES-256-GCM before they're saved. GCM also detects tampering:
 * decrypting a changed value fails instead of returning garbage.
 */
@Component
public class TokenCipher {

    private static final String VERSION = "v1";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    public TokenCipher(@Value("${ledgersync.token-encryption-key}") String base64Key) {
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64Key.strip());
        } catch (IllegalArgumentException e) {
            bytes = new byte[0];
        }
        if (bytes.length != 32) {
            throw new IllegalStateException(
                "TOKEN_ENCRYPTION_KEY must be 32 random bytes, base64-encoded. Make one with: openssl rand -base64 32");
        }
        this.key = new SecretKeySpec(bytes, "AES");
    }

    /** Returns "v1:iv:ciphertext". A new random IV each time, so the same token never encrypts the same way twice. */
    public String encrypt(String plaintext) {
        byte[] iv = new byte[IV_BYTES];
        random.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            Base64.Encoder base64 = Base64.getEncoder();
            return VERSION + ":" + base64.encodeToString(iv) + ":" + base64.encodeToString(ciphertext);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Could not encrypt the token", e);
        }
    }

    public String decrypt(String stored) {
        String[] parts = stored.split(":");
        if (parts.length != 3 || !parts[0].equals(VERSION)) {
            throw new IllegalArgumentException("Not an encrypted token");
        }
        try {
            Base64.Decoder base64 = Base64.getDecoder();
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, base64.decode(parts[1])));
            return new String(cipher.doFinal(base64.decode(parts[2])), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Could not decrypt the token: wrong key, or the stored value was changed", e);
        }
    }
}
