package io.github.danielnguyen327.ledgersync;

import static org.assertj.core.api.Assertions.assertThat;

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
@Import(TestcontainersConfiguration.class)
class AuthTests {

    private static final String PASSWORD = "correct horse battery";

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private JsonMapper json;

    @BeforeEach
    void startEmpty() {
        jdbc.update("delete from spring_session");
        jdbc.update("delete from app_user");
    }

    @Test
    void twoUsersEachSeeOnlyTheirOwnAccount() {
        Browser sam = new Browser(port);
        Browser alex = new Browser(port);
        assertThat(sam.post("/api/auth/register", registration("Sam Rivera", "sam@example.com")).status())
            .isEqualTo(201);
        assertThat(alex.post("/api/auth/register", registration("Alex Kim", "alex@example.com")).status())
            .isEqualTo(201);

        assertThat(field(sam.get("/api/me"), "email")).isEqualTo("sam@example.com");
        assertThat(field(alex.get("/api/me"), "email")).isEqualTo("alex@example.com");
    }

    @Test
    void signingUpSignsYouInWithASessionStoredInPostgres() {
        Browser sam = new Browser(port);
        Browser.Response signUp = sam.post("/api/auth/register", registration("Sam Rivera", "sam@example.com"));

        assertThat(signUp.status()).isEqualTo(201);
        assertThat(field(signUp, "name")).isEqualTo("Sam Rivera");
        assertThat(signUp.body()).doesNotContain("password");
        assertThat(field(sam.get("/api/me"), "name")).isEqualTo("Sam Rivera");
        assertThat(jdbc.queryForObject("select password_hash from app_user", String.class))
            .startsWith("{bcrypt}$2a$")
            .doesNotContain(PASSWORD);
        assertThat(jdbc.queryForList("select principal_name from spring_session", String.class))
            .containsExactly("sam@example.com");
    }

    @Test
    void signingInChangesTheSessionId() {
        Browser sam = new Browser(port);
        sam.post("/api/auth/register", registration("Sam Rivera", "sam@example.com"));
        String sessionBefore = sam.cookie("SESSION");

        sam.post("/api/auth/login", login("sam@example.com", PASSWORD));

        assertThat(sam.cookie("SESSION")).isNotNull().isNotEqualTo(sessionBefore);
        assertThat(sam.get("/api/me").status()).isEqualTo(200);
    }

    @Test
    void emailsIgnoreCase() {
        Browser sam = new Browser(port);
        sam.post("/api/auth/register", registration("Sam Rivera", "Sam@Example.com"));

        assertThat(field(sam.get("/api/me"), "email")).isEqualTo("sam@example.com");
        assertThat(new Browser(port).post("/api/auth/login", login("SAM@example.com", PASSWORD)).status())
            .isEqualTo(200);
        Browser.Response again = new Browser(port).post("/api/auth/register", registration("Sam", "sam@example.com"));
        assertThat(again.status()).isEqualTo(409);
        assertThat(field(again, "detail")).isEqualTo("An account with this email already exists.");
    }

    @Test
    void aWrongPasswordAndAnUnknownEmailGetTheSameAnswer() {
        new Browser(port).post("/api/auth/register", registration("Sam Rivera", "sam@example.com"));

        Browser.Response wrongPassword = new Browser(port).post("/api/auth/login", login("sam@example.com", "nope"));
        Browser.Response unknownEmail = new Browser(port).post("/api/auth/login", login("who@example.com", PASSWORD));

        assertThat(wrongPassword.status()).isEqualTo(401);
        assertThat(unknownEmail.status()).isEqualTo(401);
        assertThat(field(wrongPassword, "detail"))
            .isEqualTo(field(unknownEmail, "detail"))
            .isEqualTo("Email or password is incorrect.");
    }

    @Test
    void signingOutEndsTheSession() {
        Browser sam = new Browser(port);
        sam.post("/api/auth/register", registration("Sam Rivera", "sam@example.com"));

        assertThat(sam.post("/api/auth/logout", "").status()).isEqualTo(204);

        assertThat(sam.get("/api/me").status()).isEqualTo(401);
        assertThat(jdbc.queryForObject("select count(*) from spring_session", Integer.class)).isZero();
    }

    @Test
    void meNeedsYouToBeSignedIn() {
        assertThat(new Browser(port).get("/api/me").status()).isEqualTo(401);
    }

    @Test
    void postsWithoutTheCsrfTokenAreRejected() {
        Browser.Response response = new Browser(port)
            .postWithoutCsrfToken("/api/auth/register", registration("Sam Rivera", "sam@example.com"));

        assertThat(response.status()).isEqualTo(403);
        assertThat(jdbc.queryForObject("select count(*) from app_user", Integer.class)).isZero();
    }

    @Test
    void shortPasswordsAreRejected() {
        Browser.Response response = new Browser(port).post("/api/auth/register", """
            {"name": "Sam Rivera", "email": "sam@example.com", "password": "short"}
            """);

        assertThat(response.status()).isEqualTo(400);
        assertThat(field(response, "detail")).isEqualTo("Use a password of 12 to 72 characters.");
    }

    private static String registration(String name, String email) {
        return """
            {"name": "%s", "email": "%s", "password": "%s"}
            """.formatted(name, email, PASSWORD);
    }

    private static String login(String email, String password) {
        return """
            {"email": "%s", "password": "%s"}
            """.formatted(email, password);
    }

    private String field(Browser.Response response, String name) {
        JsonNode body = json.readTree(response.body());
        return body.get(name).asString();
    }
}