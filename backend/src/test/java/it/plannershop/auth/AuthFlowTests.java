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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;

import it.plannershop.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;

class AuthFlowTests extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;

    // ---------------------------------------------------------------- helper

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

    /** Crea un'attività con il suo admin (via API di piattaforma) e restituisce l'email dell'admin. */
    private String creaAttivita(String nome) throws Exception {
        String superToken = token(login(SUPERADMIN_EMAIL, SUPERADMIN_PASSWORD));
        String email = "admin-" + UUID.randomUUID() + "@test.local";
        mvc.perform(json(post("/api/platform/tenants").header("Authorization", "Bearer " + superToken), """
                {"nome":"%s","adminNome":"Mario Rossi","adminEmail":"%s","adminPassword":"Provvisoria-1"}"""
                .formatted(nome, email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value(nome))
                .andExpect(jsonPath("$.amministratori[0]").value(email));
        return email;
    }

    // ---------------------------------------------------------------- test

    @Test
    void ilSuperadminCreatoAlPrimoAvvioPuoAccedere() throws Exception {
        MockHttpServletResponse r = login(SUPERADMIN_EMAIL, SUPERADMIN_PASSWORD);

        assertThat(token(r)).isNotBlank();
        assertThat((String) JsonPath.read(r.getContentAsString(), "$.utente.ruolo")).isEqualTo("SUPERADMIN");
        Cookie refresh = r.getCookie(AuthController.COOKIE_REFRESH);
        assertThat(refresh).isNotNull();
        assertThat(refresh.isHttpOnly()).isTrue();
        assertThat(refresh.getPath()).isEqualTo("/api/auth");
    }

    @Test
    void ilNuovoAdminAccedeAllaSuaAttivitaEDeveCambiarePassword() throws Exception {
        String email = creaAttivita("Centro Estetico Test");
        String t = token(login(email, "Provvisoria-1"));

        mvc.perform(get("/api/me").header("Authorization", "Bearer " + t))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruolo").value("ADMIN"))
                .andExpect(jsonPath("$.tenant.nome").value("Centro Estetico Test"))
                .andExpect(jsonPath("$.deveCambiarePassword").value(true));

        mvc.perform(json(put("/api/auth/password").header("Authorization", "Bearer " + t),
                "{\"passwordAttuale\":\"Provvisoria-1\",\"nuovaPassword\":\"Definitiva-22\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/me").header("Authorization", "Bearer " + t))
                .andExpect(jsonPath("$.deveCambiarePassword").value(false));
        login(email, "Definitiva-22");
    }

    @Test
    void unAdminNonAccedeAlleApiDiPiattaforma() throws Exception {
        String t = token(login(creaAttivita("Negozio"), "Provvisoria-1"));
        mvc.perform(get("/api/platform/tenants").header("Authorization", "Bearer " + t))
                .andExpect(status().isForbidden());
    }

    @Test
    void senzaTokenLeApiRispondono401() throws Exception {
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/info")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/me").header("Authorization", "Bearer token-falso")).andExpect(status().isUnauthorized());
    }

    @Test
    void ilRuoloOperatoreNonBastaPerLaPiattaforma() throws Exception {
        mvc.perform(get("/api/platform/tenants").with(jwt().authorities(
                        new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_OPERATORE"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void passwordErrataRestituisce401ConMessaggio() throws Exception {
        mvc.perform(json(post("/api/auth/login"), loginBody(SUPERADMIN_EMAIL, "sbagliata"))
                        .with(r -> { r.setRemoteAddr("10.0.0.1"); return r; }))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Email o password non corretti"));
    }

    @Test
    void datiDiLoginNonValidiRestituiscono400ConIErroriPerCampo() throws Exception {
        mvc.perform(json(post("/api/auth/login"), "{\"email\":\"non-una-email\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errori.email").value("Email non valida"))
                .andExpect(jsonPath("$.errori.password").value("Inserisci la password"));
    }

    @Test
    void troppiTentativiBloccanoIlLogin() throws Exception {
        String email = "nessuno-" + UUID.randomUUID() + "@test.local";
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
    void ilRefreshRuotaIlTokenEUnTokenRiusatoChiudeTutteLeSessioni() throws Exception {
        Cookie primo = login(SUPERADMIN_EMAIL, SUPERADMIN_PASSWORD).getCookie(AuthController.COOKIE_REFRESH);

        MockHttpServletResponse r = mvc.perform(post("/api/auth/refresh").cookie(primo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn().getResponse();
        Cookie secondo = r.getCookie(AuthController.COOKIE_REFRESH);
        assertThat(secondo.getValue()).isNotEqualTo(primo.getValue());

        // Riutilizzo del primo token (già ruotato): sospetto furto → 401 e revoca di tutto.
        mvc.perform(post("/api/auth/refresh").cookie(primo)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/refresh").cookie(secondo)).andExpect(status().isUnauthorized());
    }

    @Test
    void dopoIlLogoutIlRefreshNonFunzionaPiu() throws Exception {
        Cookie c = login(SUPERADMIN_EMAIL, SUPERADMIN_PASSWORD).getCookie(AuthController.COOKIE_REFRESH);
        mvc.perform(post("/api/auth/logout").cookie(c))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge(AuthController.COOKIE_REFRESH, 0));
        mvc.perform(post("/api/auth/refresh").cookie(c)).andExpect(status().isUnauthorized());
    }

    @Test
    void unaAttivitaEliminataNonPermettePiuLAccesso() throws Exception {
        String email = creaAttivita("Da chiudere");
        String superToken = token(login(SUPERADMIN_EMAIL, SUPERADMIN_PASSWORD));
        String elenco = mvc.perform(get("/api/platform/tenants").header("Authorization", "Bearer " + superToken))
                .andReturn().getResponse().getContentAsString();
        Integer id = JsonPath.<java.util.List<Integer>>read(elenco, "$[?(@.nome == 'Da chiudere')].id").getFirst();

        mvc.perform(delete("/api/platform/tenants/" + id).header("Authorization", "Bearer " + superToken))
                .andExpect(status().isNoContent());
        mvc.perform(json(post("/api/auth/login"), loginBody(email, "Provvisoria-1")))
                .andExpect(status().isForbidden());
    }
}
