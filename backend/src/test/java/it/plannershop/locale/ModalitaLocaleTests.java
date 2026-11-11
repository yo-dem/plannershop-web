package it.plannershop.locale;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import it.plannershop.AbstractIntegrationTest;
import it.plannershop.tenant.TenantRepository;
import it.plannershop.user.AppUser;
import it.plannershop.user.AppUserRepository;
import it.plannershop.user.Membership;
import it.plannershop.user.MembershipRepository;
import it.plannershop.user.RipristinoPassword;
import it.plannershop.user.Ruolo;

/**
 * Modalità LOCALE: contesto Spring separato, con un proprio database vuoto,
 * così si verifica anche cosa succede al primo avvio.
 */
@TestPropertySource(properties = {
        "app.modalita=locale",
        "app.bootstrap.attivita-nome=Centro Locale",
        "app.bootstrap.admin-email=" + ModalitaLocaleTests.ADMIN_EMAIL,
        "app.bootstrap.admin-nome=Titolare",
        "app.bootstrap.admin-password=" + ModalitaLocaleTests.ADMIN_PASSWORD
})
class ModalitaLocaleTests extends AbstractIntegrationTest {

    static final String ADMIN_EMAIL = "titolare@locale.test";
    static final String ADMIN_PASSWORD = "Iniziale-123";

    @Autowired
    MockMvc mvc;
    @Autowired
    AppUserRepository users;
    @Autowired
    TenantRepository tenants;
    @Autowired
    MembershipRepository memberships;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    RipristinoPassword ripristino;

    private ResultActions login(String email, String password) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s"}""".formatted(email, password)));
    }

    private String token(String email, String password) throws Exception {
        String body = login(email, password).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    /** Crea un utente direttamente nel database, opzionalmente come operatore dell'attività. */
    private AppUser creaUtente(String password, boolean superadmin, boolean membro) {
        AppUser u = new AppUser("utente-" + UUID.randomUUID() + "@locale.test", "Prova", passwordEncoder.encode(password));
        u.setSuperadmin(superadmin);
        u = users.save(u);
        if (membro) {
            memberships.save(new Membership(u, tenants.findAll().getFirst(), Ruolo.OPERATORE));
        }
        return u;
    }

    @Test
    void alPrimoAvvioCreaLAttivitaEIlSuoAdminInveceDelSuperadmin() throws Exception {
        login(ADMIN_EMAIL, ADMIN_PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.utente.ruolo").value("ADMIN"))
                .andExpect(jsonPath("$.utente.superadmin").value(false))
                .andExpect(jsonPath("$.utente.tenant.nome").value("Centro Locale"))
                .andExpect(jsonPath("$.utente.deveCambiarePassword").value(true));

        // Il SUPERADMIN configurato nei test non viene creato.
        login(SUPERADMIN_EMAIL, SUPERADMIN_PASSWORD).andExpect(status().isUnauthorized());
    }

    @Test
    void infoRiportaLaModalitaLocale() throws Exception {
        mvc.perform(get("/api/info").header("Authorization", "Bearer " + token(ADMIN_EMAIL, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modalita").value("LOCALE"));
    }

    @Test
    void leApiDiPiattaformaNonEsistono() throws Exception {
        mvc.perform(get("/api/platform/tenants").header("Authorization", "Bearer " + token(ADMIN_EMAIL, ADMIN_PASSWORD)))
                .andExpect(status().isForbidden());
        // Anche con un token (falsificato) da SUPERADMIN l'endpoint non c'è.
        mvc.perform(get("/api/platform/tenants").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPERADMIN"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void unSuperadminPresenteNelDatabaseNonPuoUsareLaPiattaforma() throws Exception {
        // Senza attività: non può accedere affatto.
        AppUser solo = creaUtente("Password-123", true, false);
        login(solo.getEmail(), "Password-123").andExpect(status().isForbidden());

        // Membro dell'attività: entra, ma senza il ruolo SUPERADMIN.
        AppUser membro = creaUtente("Password-123", true, true);
        login(membro.getEmail(), "Password-123")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.utente.ruolo").value("OPERATORE"))
                .andExpect(jsonPath("$.utente.superadmin").value(false));
        String t = token(membro.getEmail(), "Password-123");
        mvc.perform(get("/api/platform/tenants").header("Authorization", "Bearer " + t))
                .andExpect(status().isForbidden());
    }

    @Test
    void ilRipristinoAssegnaUnaPasswordProvvisoriaNuova() throws Exception {
        AppUser u = creaUtente("Dimenticata-123", false, true);

        String nuova = ripristino.ripristina(u.getEmail().toUpperCase());

        login(u.getEmail(), "Dimenticata-123").andExpect(status().isUnauthorized());
        login(u.getEmail(), nuova)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.utente.deveCambiarePassword").value(true));
    }
}
