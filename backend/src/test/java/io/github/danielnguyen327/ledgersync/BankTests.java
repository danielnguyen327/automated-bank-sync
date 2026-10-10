package io.github.danielnguyen327.ledgersync;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.danielnguyen327.ledgersync.bank.TokenCipher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({TestcontainersConfiguration.class, FakePlaid.Config.class})
class BankTests {

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private JsonMapper json;

    @Autowired
    private FakePlaid plaid;

    @Autowired
    private TokenCipher cipher;

    @BeforeEach
    void startEmpty() {
        jdbc.update("delete from spring_session");
        jdbc.update("delete from app_user");
        plaid.reset();
    }

    @Test
    void oneUserConnectsTwoBanks() {
        Browser sam = signedUp("sam@example.com");

        Browser.Response linkToken = sam.post("/api/banks/link-token", "");
        assertThat(linkToken.status()).isEqualTo(200);
        assertThat(body(linkToken).get("linkToken").asString()).startsWith("link-sandbox-");

        assertThat(sam.post("/api/banks", connect("public-tartan")).status()).isEqualTo(201);
        assertThat(sam.post("/api/banks", connect("public-platypus")).status()).isEqualTo(201);

        JsonNode banks = body(sam.get("/api/banks"));
        assertThat(banks.size()).isEqualTo(2);
        assertThat(banks.get(0).get("institutionName").asString()).isEqualTo("Tartan Bank");
        assertThat(banks.get(1).get("institutionName").asString()).isEqualTo("First Platypus Bank");
        JsonNode card = banks.get(0).get("accounts").get(1);
        assertThat(card.get("name").asString()).isEqualTo("Plaid Credit Card");
        assertThat(card.get("currentBalanceCents").asLong()).isEqualTo(41000);
    }

    @Test
    void accessTokensNeverReachTheBrowserAndAreStoredEncrypted() {
        Browser sam = signedUp("sam@example.com");
        Browser.Response connected = sam.post("/api/banks", connect("public-tartan"));
        Browser.Response listed = sam.get("/api/banks");
        String accessToken = plaid.issuedAccessTokens.getFirst();

        assertThat(connected.body()).doesNotContain(accessToken).doesNotContain("access");
        assertThat(listed.body()).doesNotContain(accessToken).doesNotContain("access");
        String stored = jdbc.queryForObject("select access_token_encrypted from plaid_item", String.class);
        assertThat(stored).startsWith("v1:").doesNotContain(accessToken);
        assertThat(cipher.decrypt(stored)).isEqualTo(accessToken);
    }

    @Test
    void usersCanOnlySeeAndDisconnectTheirOwnBanks() {
        Browser sam = signedUp("sam@example.com");
        Browser alex = signedUp("alex@example.com");
        String samsBank = body(sam.post("/api/banks", connect("public-tartan"))).get("id").asString();

        assertThat(body(alex.get("/api/banks")).size()).isZero();
        assertThat(alex.delete("/api/banks/" + samsBank).status()).isEqualTo(404);
        assertThat(body(sam.get("/api/banks")).size()).isEqualTo(1);
        assertThat(plaid.removedAccessTokens).isEmpty();
    }

    @Test
    void disconnectingRemovesTheBankAtPlaidAndHere() {
        Browser sam = signedUp("sam@example.com");
        String bank = body(sam.post("/api/banks", connect("public-tartan"))).get("id").asString();

        assertThat(sam.delete("/api/banks/" + bank).status()).isEqualTo(204);

        assertThat(plaid.removedAccessTokens).containsExactlyElementsOf(plaid.issuedAccessTokens);
        assertThat(body(sam.get("/api/banks")).size()).isZero();
        assertThat(jdbc.queryForObject("select count(*) from financial_account", Integer.class)).isZero();
    }

    @Test
    void aConnectionThatFailsHalfwayLeavesNothingBehind() {
        Browser sam = signedUp("sam@example.com");

        Browser.Response response = sam.post("/api/banks", connect("public-broken"));

        assertThat(response.status()).isEqualTo(502);
        assertThat(body(response).get("detail").asString()).isEqualTo("Plaid couldn't finish that. Try again in a minute.");
        assertThat(plaid.removedAccessTokens).containsExactlyElementsOf(plaid.issuedAccessTokens);
        assertThat(jdbc.queryForObject("select count(*) from plaid_item", Integer.class)).isZero();
    }

    @Test
    void deletingTheAccountDisconnectsEveryBankAndSignsOut() {
        Browser sam = signedUp("sam@example.com");
        sam.post("/api/banks", connect("public-tartan"));
        sam.post("/api/banks", connect("public-platypus"));

        assertThat(sam.delete("/api/me").status()).isEqualTo(204);

        assertThat(plaid.removedAccessTokens).containsExactlyInAnyOrderElementsOf(plaid.issuedAccessTokens);
        assertThat(jdbc.queryForObject("select count(*) from app_user", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from plaid_item", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from spring_session", Integer.class)).isZero();
        assertThat(sam.get("/api/me").status()).isEqualTo(401);
    }

    @Test
    void banksNeedYouToBeSignedIn() {
        assertThat(new Browser(port).get("/api/banks").status()).isEqualTo(401);
    }

    private Browser signedUp(String email) {
        Browser browser = new Browser(port);
        browser.post("/api/auth/register", """
            {"name": "Test User", "email": "%s", "password": "correct horse battery"}
            """.formatted(email));
        return browser;
    }

    private static String connect(String publicToken) {
        return """
            {"publicToken": "%s"}
            """.formatted(publicToken);
    }

    private JsonNode body(Browser.Response response) {
        return json.readTree(response.body());
    }
}
