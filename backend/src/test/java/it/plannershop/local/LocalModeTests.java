package it.plannershop.local;

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
import it.plannershop.user.PasswordReset;
import it.plannershop.user.Role;

/**
 * LOCAL mode: separate Spring context, with its own empty database, so what
 * happens on first start-up is verified too.
 */
@TestPropertySource(properties = {
        "app.mode=local",
        "app.bootstrap.tenant-name=Local Center",
        "app.bootstrap.admin-email=" + LocalModeTests.ADMIN_EMAIL,
        "app.bootstrap.admin-name=Owner",
        "app.bootstrap.admin-password=" + LocalModeTests.ADMIN_PASSWORD
})
class LocalModeTests extends AbstractIntegrationTest {

    static final String ADMIN_EMAIL = "owner@local.test";
    static final String ADMIN_PASSWORD = "Initial-123";

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
    PasswordReset passwordReset;

    private ResultActions login(String email, String password) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s"}""".formatted(email, password)));
    }

    private String token(String email, String password) throws Exception {
        String body = login(email, password).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    /** Creates a user directly in the database, optionally as operator of the tenant. */
    private AppUser createUser(String password, boolean superadmin, boolean member) {
        AppUser u = new AppUser("user-" + UUID.randomUUID() + "@local.test", "Test", passwordEncoder.encode(password));
        u.setSuperadmin(superadmin);
        u = users.save(u);
        if (member) {
            memberships.save(new Membership(u, tenants.findAll().getFirst(), Role.OPERATOR));
        }
        return u;
    }

    @Test
    void atFirstStartItCreatesTheTenantAndItsAdminInsteadOfTheSuperadmin() throws Exception {
        login(ADMIN_EMAIL, ADMIN_PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("ADMIN"))
                .andExpect(jsonPath("$.user.superadmin").value(false))
                .andExpect(jsonPath("$.user.tenant.name").value("Local Center"))
                .andExpect(jsonPath("$.user.mustChangePassword").value(true));

        // The SUPERADMIN configured for the tests is not created.
        login(SUPERADMIN_EMAIL, SUPERADMIN_PASSWORD).andExpect(status().isUnauthorized());
    }

    @Test
    void infoReportsTheLocalMode() throws Exception {
        mvc.perform(get("/api/info").header("Authorization", "Bearer " + token(ADMIN_EMAIL, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("LOCAL"));
    }

    @Test
    void thePlatformApisDoNotExist() throws Exception {
        mvc.perform(get("/api/platform/tenants").header("Authorization", "Bearer " + token(ADMIN_EMAIL, ADMIN_PASSWORD)))
                .andExpect(status().isForbidden());
        // Even with a (forged) SUPERADMIN token the endpoint is not there.
        mvc.perform(get("/api/platform/tenants").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPERADMIN"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void aSuperadminInTheDatabaseCannotUseThePlatform() throws Exception {
        // Without a tenant: cannot sign in at all.
        AppUser alone = createUser("Password-123", true, false);
        login(alone.getEmail(), "Password-123").andExpect(status().isForbidden());

        // Member of the tenant: signs in, but without the SUPERADMIN role.
        AppUser member = createUser("Password-123", true, true);
        login(member.getEmail(), "Password-123")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("OPERATOR"))
                .andExpect(jsonPath("$.user.superadmin").value(false));
        String t = token(member.getEmail(), "Password-123");
        mvc.perform(get("/api/platform/tenants").header("Authorization", "Bearer " + t))
                .andExpect(status().isForbidden());
    }

    @Test
    void passwordResetAssignsANewTemporaryPassword() throws Exception {
        AppUser u = createUser("Forgotten-123", false, true);

        String newPassword = passwordReset.reset(u.getEmail().toUpperCase());

        login(u.getEmail(), "Forgotten-123").andExpect(status().isUnauthorized());
        login(u.getEmail(), newPassword)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.mustChangePassword").value(true));
    }
}
