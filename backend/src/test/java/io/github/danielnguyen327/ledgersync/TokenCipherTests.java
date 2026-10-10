package io.github.danielnguyen327.ledgersync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.danielnguyen327.ledgersync.bank.TokenCipher;
import org.junit.jupiter.api.Test;

/** Plain unit tests: no Spring and no database, so they run in milliseconds. */
class TokenCipherTests {

    private static final String TOKEN = "access-sandbox-1234";
    private static final String OTHER_KEY = "Hx4dHBsaGRgXFhUUExIREA8ODQwLCgkIBwYFBAMCAQA=";

    private final TokenCipher cipher = new TokenCipher(TestcontainersConfiguration.TEST_TOKEN_KEY);

    @Test
    void decryptingGivesBackTheOriginalToken() {
        assertThat(cipher.decrypt(cipher.encrypt(TOKEN))).isEqualTo(TOKEN);
    }

    @Test
    void theSameTokenEncryptsDifferentlyEachTime() {
        assertThat(cipher.encrypt(TOKEN)).isNotEqualTo(cipher.encrypt(TOKEN)).doesNotContain(TOKEN);
    }

    @Test
    void aChangedValueIsRejected() {
        String[] parts = cipher.encrypt(TOKEN).split(":");
        char first = parts[2].charAt(0);
        String tampered = parts[0] + ":" + parts[1] + ":" + (first == 'A' ? 'B' : 'A') + parts[2].substring(1);

        assertThatThrownBy(() -> cipher.decrypt(tampered)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void theWrongKeyCannotDecrypt() {
        String stored = cipher.encrypt(TOKEN);

        assertThatThrownBy(() -> new TokenCipher(OTHER_KEY).decrypt(stored)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void aMissingOrShortKeyStopsTheAppFromStarting() {
        assertThatThrownBy(() -> new TokenCipher("")).hasMessageContaining("openssl rand -base64 32");
        assertThatThrownBy(() -> new TokenCipher("c2hvcnQ=")).hasMessageContaining("32 random bytes");
    }
}
