package it.plannershop.demo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import it.plannershop.AbstractTenantTest;

class DemoDataTests extends AbstractTenantTest {

    private static final String URL = "/api/demo-data";

    @Test
    void adminLoadsDemoDataOnceIntoAnEmptyBusiness() throws Exception {
        call(get(URL), admin).andExpect(status().isOk()).andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.resettable").value(false));

        call(post(URL), admin).andExpect(status().isCreated())
                .andExpect(jsonPath("$.customers").value(36))
                .andExpect(jsonPath("$.suppliers").value(4))
                .andExpect(jsonPath("$.products").value(10))
                .andExpect(jsonPath("$.services").value(10));

        call(get("/api/customers"), admin).andExpect(jsonPath("$.total").value(36));
        call(get("/api/products?sort=stockDate"), admin)
                .andExpect(jsonPath("$.items[0].brand").value("DERMALUXE"))
                // gross price calculated by the backend from the net price
                .andExpect(jsonPath("$.items[0].grossPrice").value(21.96));
        call(get("/api/suppliers"), admin).andExpect(jsonPath("$.total").value(4));
        call(get("/api/services"), admin).andExpect(jsonPath("$.total").value(10));

        // Not a second time: the business is no longer empty.
        call(get(URL), admin).andExpect(jsonPath("$.available").value(false));
        call(post(URL).header("Accept-Language", "it"), admin).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "I dati demo si possono caricare solo se l'attività non ha ancora clienti, fornitori, prodotti o servizi"));

        // Other businesses are untouched and can still load them.
        call(get("/api/customers"), otherTenantAdmin).andExpect(jsonPath("$.total").value(0));
        call(get(URL), otherTenantAdmin).andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void notAvailableWhenTheBusinessHasData() throws Exception {
        create("/api/services", admin, """
                {"name":"Taglio","description":"Taglio capelli","vatRate":22,"priceBase":"NET",
                 "netPrice":10,"salePrice":15}""");
        call(get(URL), admin).andExpect(jsonPath("$.available").value(false));
        call(post(URL), admin).andExpect(status().isConflict());
    }

    @Test
    void resetDisabledByDefault() throws Exception {
        send(post(URL + "/reset"), admin, "{\"confirmName\":\"x\"}").andExpect(status().isForbidden());
    }

    @Test
    void onlyTheAdmin() throws Exception {
        call(get(URL), operator).andExpect(status().isForbidden());
        call(post(URL), operator).andExpect(status().isForbidden());
    }
}
