package it.plannershop.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import com.jayway.jsonpath.JsonPath;

import it.plannershop.AbstractTenantTest;

@TestPropertySource(properties = "app.demo-reset=true")
class DemoResetTests extends AbstractTenantTest {

    private static final String URL = "/api/demo-data";

    private String tenantName() throws Exception {
        return JsonPath.read(call(get("/api/me"), admin).andReturn().getResponse().getContentAsString(), "$.tenant.name");
    }

    /** Customers of the business in the database, also the soft-deleted ones. */
    private long customerRows(String tenantName) {
        return jdbc.queryForObject("SELECT count(*) FROM customer c JOIN tenant t ON t.id = c.tenant_id WHERE t.name = ?",
                Long.class, tenantName);
    }

    @Test
    void resetReplacesAllTheDataOfTheBusinessWithTheDemoData() throws Exception {
        call(get(URL), admin).andExpect(jsonPath("$.resettable").value(true));
        String json = create("/api/customers", admin, """
                {"firstName":"Vero","lastName":"Cliente"}""");
        call(delete("/api/customers/" + id(json)), admin).andExpect(status().isNoContent());
        create("/api/customers", admin, """
                {"firstName":"Altro","lastName":"Cliente"}""");
        create("/api/customers", otherTenantAdmin, """
                {"firstName":"Di","lastName":"Altri"}""");

        String name = tenantName();
        // Spaces and case do not matter.
        send(post(URL + "/reset"), admin, "{\"confirmName\":\" " + name.toUpperCase() + " \"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customers").value(36))
                .andExpect(jsonPath("$.products").value(10));

        // Also the soft-deleted rows are gone: only the demo data remain.
        assertThat(customerRows(name)).isEqualTo(36);
        call(get("/api/customers?q=Cliente"), admin).andExpect(jsonPath("$.total").value(0));
        // Other businesses are untouched.
        call(get("/api/customers"), otherTenantAdmin).andExpect(jsonPath("$.total").value(1));

        // It can be repeated.
        send(post(URL + "/reset"), admin, "{\"confirmName\":\"" + name + "\"}").andExpect(status().isOk());
        assertThat(customerRows(name)).isEqualTo(36);
    }

    @Test
    void wrongNameChangesNothing() throws Exception {
        create("/api/customers", admin, """
                {"firstName":"Vero","lastName":"Cliente"}""");
        send(post(URL + "/reset").header("Accept-Language", "it"), admin, "{\"confirmName\":\"Altro nome\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Il nome non corrisponde a quello dell'attività"));
        call(get("/api/customers"), admin).andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void onlyTheAdmin() throws Exception {
        send(post(URL + "/reset"), operator, "{\"confirmName\":\"x\"}").andExpect(status().isForbidden());
    }
}
