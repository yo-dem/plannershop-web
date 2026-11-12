package it.plannershop;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;

import it.plannershop.tenant.Tenant;
import it.plannershop.tenant.TenantRepository;
import it.plannershop.user.AppUser;
import it.plannershop.user.AppUserRepository;
import it.plannershop.user.Membership;
import it.plannershop.user.MembershipRepository;
import it.plannershop.user.Ruolo;

/**
 * Base dei test sulle API di un'attività: prima di ogni test crea un'attività
 * nuova con un ADMIN e un OPERATORE, più un ADMIN di un'altra attività.
 */
public abstract class AbstractAttivitaTest extends AbstractIntegrationTest {

    protected static final String PASSWORD = "Password-123";

    @Autowired
    protected MockMvc mvc;
    @Autowired
    protected TenantRepository tenants;
    @Autowired
    protected AppUserRepository users;
    @Autowired
    protected MembershipRepository memberships;
    @Autowired
    protected PasswordEncoder passwordEncoder;
    @Autowired
    protected JdbcTemplate jdbc;

    /** Token di accesso. */
    protected String admin;
    protected String operatore;
    protected String adminAltraAttivita;

    @BeforeEach
    void preparaAttivita() throws Exception {
        Tenant t = tenants.save(new Tenant("Negozio " + UUID.randomUUID()));
        admin = token(utente(t, Ruolo.ADMIN));
        operatore = token(utente(t, Ruolo.OPERATORE));
        adminAltraAttivita = token(utente(tenants.save(new Tenant("Altro negozio")), Ruolo.ADMIN));
    }

    private String utente(Tenant t, Ruolo ruolo) {
        AppUser u = users.save(new AppUser(ruolo.name().toLowerCase() + "-" + UUID.randomUUID() + "@test.local",
                "Utente " + ruolo, passwordEncoder.encode(PASSWORD)));
        memberships.save(new Membership(u, t, ruolo));
        return u.getEmail();
    }

    protected String token(String email, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    private String token(String email) throws Exception {
        return token(email, PASSWORD);
    }

    protected ResultActions chiama(MockHttpServletRequestBuilder req, String token) throws Exception {
        return mvc.perform(req.header("Authorization", "Bearer " + token));
    }

    protected ResultActions invia(MockHttpServletRequestBuilder req, String token, String json) throws Exception {
        return chiama(req.contentType(MediaType.APPLICATION_JSON).content(json), token);
    }

    /** POST che deve rispondere 201: restituisce il JSON creato. */
    protected String crea(String url, String token, String json) throws Exception {
        return invia(post(url), token, json).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    protected static long id(String json) {
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }
}
