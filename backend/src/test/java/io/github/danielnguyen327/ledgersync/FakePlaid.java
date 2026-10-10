package io.github.danielnguyen327.ledgersync;

import io.github.danielnguyen327.ledgersync.bank.PlaidException;
import io.github.danielnguyen327.ledgersync.bank.PlaidGateway;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * A pretend Plaid for tests. It never touches the network, and remembers every access token it gave
 * out and every bank it was asked to remove, so tests can check both.
 */
class FakePlaid implements PlaidGateway {

    /** Public tokens the fake accepts, and the bank each one connects. "public-broken" fails after the exchange. */
    private static final Map<String, String> BANKS = Map.of(
        "public-tartan", "Tartan Bank",
        "public-platypus", "First Platypus Bank",
        "public-broken", "Broken Bank");

    final List<String> issuedAccessTokens = new CopyOnWriteArrayList<>();
    final List<String> removedAccessTokens = new CopyOnWriteArrayList<>();
    private final Map<String, String> bankByAccessToken = new ConcurrentHashMap<>();

    void reset() {
        issuedAccessTokens.clear();
        removedAccessTokens.clear();
        bankByAccessToken.clear();
    }

    @Override
    public String createLinkToken(UUID userId) {
        return "link-sandbox-" + userId;
    }

    @Override
    public ExchangedItem exchangePublicToken(String publicToken) {
        String bank = BANKS.get(publicToken);
        if (bank == null) {
            throw new PlaidException("INVALID_PUBLIC_TOKEN", null, "unknown public token");
        }
        String accessToken = "access-sandbox-" + UUID.randomUUID();
        issuedAccessTokens.add(accessToken);
        bankByAccessToken.put(accessToken, bank);
        return new ExchangedItem("item-" + UUID.randomUUID(), accessToken);
    }

    @Override
    public ItemDetails getItemDetails(String accessToken) {
        String bank = bankByAccessToken.get(accessToken);
        if (bank.equals("Broken Bank")) {
            throw new PlaidException("INTERNAL_SERVER_ERROR", null, "Plaid had a problem");
        }
        return new ItemDetails("ins_" + bank.length(), bank, List.of(
            new PlaidAccount("acct-" + UUID.randomUUID(), "Plaid Checking", "Plaid Gold Standard 0% Interest Checking",
                "0000", "depository", "checking", 11000L, 10000L, "USD"),
            new PlaidAccount("acct-" + UUID.randomUUID(), "Plaid Credit Card", "Plaid Diamond 12.5% APR Interest Credit Card",
                "3333", "credit", "credit card", 41000L, null, "USD")));
    }

    @Override
    public void removeItem(String accessToken) {
        removedAccessTokens.add(accessToken);
    }

    /** Add with @Import to swap the real Plaid for this fake. */
    @TestConfiguration(proxyBeanMethods = false)
    static class Config {

        @Bean
        @Primary
        FakePlaid fakePlaid() {
            return new FakePlaid();
        }
    }
}
