package it.plannershop.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;

import it.plannershop.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;

class AuthFlowTests extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;

    // ---------------------------------------------------------------- helpers

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder req, String body) {
        return req.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static String loginBody(String email, String password) {
        return """
                {"email":"%s","password":"%s"}""".formatted(email, password);
    }

    private MockHttpServletResponse login(String email, String password) throws Exception {
        return mvc.perform(json(post("/api/auth/login"), loginBody(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse();
    }

    private static String token(MockHttpServletResponse r) throws Exception {
        return JsonPath.read(r.getContentAsString(), "$.accessToken");
    }

    /** Creates a tenant with its admin (through the platform API) and returns the admin's email. */
    private String createTenant(String name) throws Exception {
        String superToken = token(login(SUPERADMIN_EMAIL, SUPERADMIN_PASSWORD));
        String email = "admin-" + UUID.randomUUID() + "@test.local";
        mvc.perform(json(post("/api/platform/tenants").header("Authorization", "Bearer " + superToken), """
                {"name":"%s","adminName":"Mario Rossi","adminEmail":"%s","adminPassword":"Temporary-1"}"""
                .formatted(name, email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.admins[0]").value(email));
        return email;
    }

    // ---------------------------------------------------------------- tests

    @Test
    void theSuperadminCreatedAtFirstStartCanSignIn() throws Exception {
        MockHttpServletResponse r = login(SUPERADMIN_EMAIL, SUPERADMIN_PASSWORD);

        assertThat(token(r)).isNotBlank();
        assertThat((String) JsonPath.read(r.getContentAsString(), "$.user.role")).isEqualTo("SUPERADMIN");
        Cookie refresh = r.getCookie(AuthController.REFRESH_COOKIE);
        assertThat(refresh).isNotNull();
        assertThat(refresh.isHttpOnly()).isTrue();
        assertThat(refresh.getPath()).isEqualTo("/api/auth");
    }

    @Test
    void theNewAdminSignsInToTheirTenantAndMustChangePassword() throws Exception {
        String email = createTenant("Beauty Center Test");
        String t = token(login(email, "Temporary-1"));

        mvc.perform(get("/api/me").header("Authorization", "Bearer " + t))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.tenant.name").value("Beauty Center Test"))
                .andExpect(jsonPath("$.mustChangePassword").value(true));

        mvc.perform(json(put("/api/auth/password").header("Authorization", "Bearer " + t),
                "{\"currentPassword\":\"Temporary-1\",\"newPassword\":\"Definitive-22\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/me").header("Authorization", "Bearer " + t))
                .andExpect(jsonPath("$.mustChangePassword").value(false));
        login(email, "Definitive-22");
    }

    @Test
    void anAdminCannotUseThePlatformApi() throws Exception {
        String t = token(login(createTenant("Shop"), "Temporary-1"));
        mvc.perform(get("/api/platform/tenants").header("Authorization", "Bearer " + t))
                .andExpect(status().isForbidden());
    }

    @Test
    void inCloudModeInfoReportsItAndTheSuperadminHasThePlatform() throws Exception {
        MockHttpServletResponse r = login(SUPERADMIN_EMAIL, SUPERADMIN_PASSWORD);
        assertThat((Boolean) JsonPath.read(r.getContentAsString(), "$.user.superadmin")).isTrue();
        mvc.perform(get("/api/info").header("Authorization", "Bearer " + token(r)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("CLOUD"));
    }

    @Test
    void withoutATokenTheApisAnswer401() throws Exception {
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/info")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/me").header("Authorization", "Bearer fake-token")).andExpect(status().isUnauthorized());
    }

    @Test
    void theOperatorRoleIsNotEnoughForThePlatform() throws Exception {
        mvc.perform(get("/api/platform/tenants").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERATOR"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void aWrongPasswordReturns401WithATranslatedMessage() throws Exception {
        mvc.perform(json(post("/api/auth/login"), loginBody(SUPERADMIN_EMAIL, "wrong"))
                        .with(r -> { r.setRemoteAddr("10.0.0.1"); return r; }))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Wrong email or password"));
        mvc.perform(json(post("/api/auth/login"), loginBody(SUPERADMIN_EMAIL, "wrong"))
                        .header("Accept-Language", "it-IT,it;q=0.9")
                        .with(r -> { r.setRemoteAddr("10.0.0.2"); return r; }))
                .andExpect(jsonPath("$.detail").value("Email o password non corretti"));
    }

    @Test
    void invalidLoginDataReturns400WithFieldErrors() throws Exception {
        mvc.perform(json(post("/api/auth/login"), "{\"email\":\"not-an-email\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Invalid data"))
                .andExpect(jsonPath("$.errors.email").value("Invalid email"))
                .andExpect(jsonPath("$.errors.password").value("Required"));
        mvc.perform(json(post("/api/auth/login"), "{\"email\":\"not-an-email\",\"password\":\"\"}")
                        .header("Accept-Language", "it"))
                .andExpect(jsonPath("$.detail").value("Dati non validi"))
                .andExpect(jsonPath("$.errors.email").value("Email non valida"))
                .andExpect(jsonPath("$.errors.password").value("Obbligatorio"));
    }

    @Test
    void tooManyAttemptsBlockTheSignIn() throws Exception {
        String email = "nobody-" + UUID.randomUUID() + "@test.local";
        for (int i = 0; i < 5; i++) {
            mvc.perform(json(post("/api/auth/login"), loginBody(email, "x"))
                            .with(r -> { r.setRemoteAddr("10.9.9.9"); return r; }))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(json(post("/api/auth/login"), loginBody(email, "x"))
                        .with(r -> { r.setRemoteAddr("10.9.9.9"); return r; }))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void refreshRotatesTheTokenAndAReusedTokenClosesAllSessions() throws Exception {
        Cookie first = login(SUPERADMIN_EMAIL, SUPERADMIN_PASSWORD).getCookie(AuthController.REFRESH_COOKIE);

        MockHttpServletResponse r = mvc.perform(post("/api/auth/refresh").cookie(first))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn().getResponse();
        Cookie second = r.getCookie(AuthController.REFRESH_COOKIE);
        assertThat(second.getValue()).isNotEqualTo(first.getValue());

        // Reuse of the first token (already rotated): possible theft → 401 and everything revoked.
        mvc.perform(post("/api/auth/refresh").cookie(first)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/refresh").cookie(second)).andExpect(status().isUnauthorized());
    }

    @Test
    void afterLogoutRefreshNoLongerWorks() throws Exception {
        Cookie c = login(SUPERADMIN_EMAIL, SUPERADMIN_PASSWORD).getCookie(AuthController.REFRESH_COOKIE);
        mvc.perform(post("/api/auth/logout").cookie(c))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge(AuthController.REFRESH_COOKIE, 0));
        mvc.perform(post("/api/auth/refresh").cookie(c)).andExpect(status().isUnauthorized());
    }

    @Test
    void aDeletedTenantNoLongerAllowsSignIn() throws Exception {
        String email = createTenant("To close");
        String superToken = token(login(SUPERADMIN_EMAIL, SUPERADMIN_PASSWORD));
        String list = mvc.perform(get("/api/platform/tenants").header("Authorization", "Bearer " + superToken))
                .andReturn().getResponse().getContentAsString();
        Integer id = JsonPath.<java.util.List<Integer>>read(list, "$[?(@.name == 'To close')].id").getFirst();

        mvc.perform(delete("/api/platform/tenants/" + id).header("Authorization", "Bearer " + superToken))
                .andExpect(status().isNoContent());
        mvc.perform(json(post("/api/auth/login"), loginBody(email, "Temporary-1")))
                .andExpect(status().isForbidden());
    }
}
