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
import it.plannershop.user.Role;

/**
 * Base of the tests on the APIs of a tenant: before each test it creates a new
 * tenant with an ADMIN and an OPERATOR, plus an ADMIN of another tenant.
 */
public abstract class AbstractTenantTest extends AbstractIntegrationTest {

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

    /** Access tokens. */
    protected String admin;
    protected String operator;
    protected String otherTenantAdmin;

    @BeforeEach
    void createTenant() throws Exception {
        Tenant t = tenants.save(new Tenant("Shop " + UUID.randomUUID()));
        admin = token(user(t, Role.ADMIN));
        operator = token(user(t, Role.OPERATOR));
        otherTenantAdmin = token(user(tenants.save(new Tenant("Other shop")), Role.ADMIN));
    }

    private String user(Tenant t, Role role) {
        AppUser u = users.save(new AppUser(role.name().toLowerCase() + "-" + UUID.randomUUID() + "@test.local",
                "User " + role, passwordEncoder.encode(PASSWORD)));
        memberships.save(new Membership(u, t, role));
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

    protected ResultActions call(MockHttpServletRequestBuilder req, String token) throws Exception {
        return mvc.perform(req.header("Authorization", "Bearer " + token));
    }

    protected ResultActions send(MockHttpServletRequestBuilder req, String token, String json) throws Exception {
        return call(req.contentType(MediaType.APPLICATION_JSON).content(json), token);
    }

    /** POST that must answer 201: returns the created JSON. */
    protected String create(String url, String token, String json) throws Exception {
        return send(post(url), token, json).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    protected static long id(String json) {
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }
}
