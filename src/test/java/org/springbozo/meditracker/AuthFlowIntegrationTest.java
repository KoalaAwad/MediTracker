package org.springbozo.meditracker;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springbozo.meditracker.service.MailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.io.IOException;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs the auth flows (register, verify, login, forgot/reset password, admin bootstrap)
 * against a real embedded Postgres with all Flyway migrations applied.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowIntegrationTest {

    static final EmbeddedPostgres pg;

    static {
        try {
            pg = EmbeddedPostgres.builder().start();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", () -> pg.getJdbcUrl("postgres", "postgres"));
        r.add("spring.datasource.username", () -> "postgres");
        r.add("spring.datasource.password", () -> "");
        r.add("jwt.secret", () -> "test-secret-test-secret-test-secret-0123456789");
        r.add("app.admin.email", () -> "Boss@Example.com");
        r.add("app.admin.password", () -> "admin-pass-123");
    }

    @AfterAll
    static void stop() throws IOException {
        pg.close();
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @MockitoBean
    MailService mailService;

    private ResultActions postJson(String url, Map<String, String> body) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private String lastVerifyToken(String email) {
        var captor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(mailService, atLeastOnce()).sendVerification(eq(email), anyString(), captor.capture());
        return captor.getValue();
    }

    private String lastResetToken(String email) {
        var captor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(mailService, atLeastOnce()).sendPasswordReset(eq(email), anyString(), captor.capture());
        return captor.getValue();
    }

    @Test
    void publicDefaultAdminIsGone() throws Exception {
        postJson("/api/auth/login", Map.of("email", "admin@meditracker.com", "password", "password"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminIsBootstrappedFromEnv() throws Exception {
        String body = postJson("/api/auth/login", Map.of("email", "boss@example.com", "password", "admin-pass-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value(org.hamcrest.Matchers.containsString("ADMIN")))
                .andReturn().getResponse().getContentAsString();
        String token = json.readTree(body).get("token").asText();
        mvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void registerVerifyLoginFlow() throws Exception {
        String email = "flow@example.com";

        postJson("/api/auth/register", Map.of("email", email, "password", "short", "name", "Flow"))
                .andExpect(status().isBadRequest());
        postJson("/api/auth/register", Map.of("email", "not-an-email", "password", "long-enough-1", "name", "Flow"))
                .andExpect(status().isBadRequest());

        postJson("/api/auth/register", Map.of("email", "Flow@Example.com", "password", "long-enough-1", "name", "Flow"))
                .andExpect(status().isCreated());
        postJson("/api/auth/register", Map.of("email", email, "password", "long-enough-1", "name", "Flow"))
                .andExpect(status().isConflict());

        // wrong password must not reveal the account is unverified
        postJson("/api/auth/login", Map.of("email", email, "password", "wrong-password"))
                .andExpect(status().isUnauthorized());
        postJson("/api/auth/login", Map.of("email", email, "password", "long-enough-1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));

        // resend inside the cooldown sends nothing new
        postJson("/api/auth/resend-verification", Map.of("email", email)).andExpect(status().isOk());
        verify(mailService, times(1)).sendVerification(eq(email), anyString(), anyString());

        String token = lastVerifyToken(email);
        postJson("/api/auth/verify-email", Map.of("token", "bogus")).andExpect(status().isBadRequest());
        postJson("/api/auth/verify-email", Map.of("token", token)).andExpect(status().isOk());
        postJson("/api/auth/verify-email", Map.of("token", token)).andExpect(status().isBadRequest());

        postJson("/api/auth/login", Map.of("email", email, "password", "long-enough-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());

        // verified accounts don't get more verification emails
        postJson("/api/auth/resend-verification", Map.of("email", email)).andExpect(status().isOk());
        verify(mailService, times(1)).sendVerification(eq(email), anyString(), anyString());
    }

    @Test
    void forgotAndResetPasswordFlow() throws Exception {
        String email = "reset@example.com";
        postJson("/api/auth/register", Map.of("email", email, "password", "original-pass", "name", "Reset"))
                .andExpect(status().isCreated());

        // unknown email gets the same answer and no mail
        postJson("/api/auth/forgot-password", Map.of("email", "nobody@example.com"))
                .andExpect(status().isOk());
        verify(mailService, never()).sendPasswordReset(eq("nobody@example.com"), anyString(), anyString());

        postJson("/api/auth/forgot-password", Map.of("email", email)).andExpect(status().isOk());
        postJson("/api/auth/forgot-password", Map.of("email", email)).andExpect(status().isOk());
        verify(mailService, times(1)).sendPasswordReset(eq(email), anyString(), anyString());
        String token = lastResetToken(email);

        postJson("/api/auth/reset-password", Map.of("token", token, "password", "short"))
                .andExpect(status().isBadRequest());
        postJson("/api/auth/reset-password", Map.of("token", token, "password", "brand-new-pass"))
                .andExpect(status().isOk());
        postJson("/api/auth/reset-password", Map.of("token", token, "password", "another-pass-1"))
                .andExpect(status().isBadRequest());

        postJson("/api/auth/login", Map.of("email", email, "password", "original-pass"))
                .andExpect(status().isUnauthorized());
        // the reset link also proved the inbox, so the account is verified now
        postJson("/api/auth/login", Map.of("email", email, "password", "brand-new-pass"))
                .andExpect(status().isOk());
    }
}
