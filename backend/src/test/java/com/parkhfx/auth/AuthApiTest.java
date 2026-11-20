package com.parkhfx.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/** End-to-end tests of the auth API: real HTTP layer, real security filters, real PostGIS database. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AuthApiTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void clean() {
        jdbc.execute("TRUNCATE bookings, listings, users CASCADE");
    }

    @Test
    void registerReturnsATokenThatWorksOnMe() throws Exception {
        String token = tokenFrom(register("ada@example.com", "correct horse", "Ada"));

        mvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ada@example.com"))
                .andExpect(jsonPath("$.displayName").value("Ada"));
    }

    @Test
    void passwordIsNeverReturnedAndIsStoredHashed() throws Exception {
        String body = register("ada@example.com", "correct horse", "Ada");

        assertThat(body).doesNotContain("correct horse");
        assertThat(body).doesNotContain("password");

        String stored = jdbc.queryForObject(
                "SELECT password_hash FROM users WHERE email = 'ada@example.com'", String.class);
        assertThat(stored).startsWith("$2"); // BCrypt
    }

    @Test
    void duplicateEmailIsRejectedIgnoringCase() throws Exception {
        register("ada@example.com", "correct horse", "Ada");

        registerRequest("Ada@Example.com", "another horse", "Ada Two")
                .andExpect(status().isConflict());
    }

    @Test
    void loginWithTheRightPasswordWorksEvenIfEmailCaseDiffers() throws Exception {
        register("ada@example.com", "correct horse", "Ada");

        loginRequest("ADA@example.com", "correct horse")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void wrongPasswordAndUnknownEmailBothGetTheSame401() throws Exception {
        register("ada@example.com", "correct horse", "Ada");

        String wrongPassword = loginRequest("ada@example.com", "wrong password")
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        String unknownEmail = loginRequest("nobody@example.com", "whatever pass")
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertThat(json.readTree(wrongPassword).get("detail").asText())
                .isEqualTo(json.readTree(unknownEmail).get("detail").asText());
    }

    @Test
    void invalidRegistrationIsRejectedWithFieldErrors() throws Exception {
        registerRequest("not-an-email", "short", " ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists())
                .andExpect(jsonPath("$.errors.displayName").exists());
    }

    @Test
    void protectedEndpointsNeedAToken() throws Exception {
        mvc.perform(get("/api/v1/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void tamperedTokenIsRejected() throws Exception {
        String token = tokenFrom(register("ada@example.com", "correct horse", "Ada"));

        // Corrupt the first character of the signature (third part of header.payload.signature).
        String[] parts = token.split("\\.");
        char first = parts[2].charAt(0);
        parts[2] = (first == 'A' ? 'B' : 'A') + parts[2].substring(1);
        String tampered = String.join(".", parts);

        mvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void demoLoginWorksAndAlwaysReturnsTheSameAccount() throws Exception {
        JsonNode first = json.readTree(demoLogin());
        JsonNode second = json.readTree(demoLogin());

        assertThat(first.get("user").get("email").asText()).isEqualTo(AuthService.DEMO_EMAIL);
        assertThat(first.get("user").get("id").asText()).isEqualTo(second.get("user").get("id").asText());

        mvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + first.get("token").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(AuthService.DEMO_EMAIL));
    }

    // ---- helpers -----------------------------------------------------------

    private ResultActions registerRequest(String email, String password, String displayName) throws Exception {
        return mvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of(
                        "email", email, "password", password, "displayName", displayName))));
    }

    /** Registers an account, asserts 201, and returns the response body. */
    private String register(String email, String password, String displayName) throws Exception {
        return registerRequest(email, password, displayName)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private ResultActions loginRequest(String email, String password) throws Exception {
        return mvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "password", password))));
    }

    private String demoLogin() throws Exception {
        return mvc.perform(post("/api/v1/auth/demo"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private String tokenFrom(String responseBody) throws Exception {
        return json.readTree(responseBody).get("token").asText();
    }
}
