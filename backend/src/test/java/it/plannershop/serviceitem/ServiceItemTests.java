package it.plannershop.serviceitem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import it.plannershop.AbstractTenantTest;

class ServiceItemTests extends AbstractTenantTest {

    private static final String URL = "/api/services";

    private static String service(String name, String description, String salePrice) {
        return """
                {"name":"%s","description":"%s","vatRate":22,"priceBase":"GROSS","grossPrice":30,
                 "salePrice":%s}""".formatted(name, description, salePrice);
    }

    @Test
    void createUpdateAndDelete() throws Exception {
        long id = id(send(post(URL), operator, """
                {"name":" Blow-dry ","description":"Short hair","vatRate":22,"priceBase":"NET",
                 "netPrice":20,"salePrice":25}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Blow-dry"))
                .andExpect(jsonPath("$.grossPrice").value(24.40))
                .andExpect(jsonPath("$.quantity").doesNotExist())
                .andReturn().getResponse().getContentAsString());

        send(put(URL + "/" + id), operator, """
                {"name":"Blow-dry","description":"Long hair","vatRate":10,"priceBase":"GROSS",
                 "grossPrice":33,"salePrice":33,"version":0}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.netPrice").value(30.00))
                .andExpect(jsonPath("$.vatRate").value(10))
                .andExpect(jsonPath("$.version").value(1));
        send(put(URL + "/" + id), admin, service("Blow-dry", "X", "1").replace("}", ",\"version\":0}"))
                .andExpect(status().isConflict());

        call(delete(URL + "/" + id), operator).andExpect(status().isNoContent());
        call(get(URL + "/" + id), admin).andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("select deleted_by from service_item where id = ?", Long.class, id)).isNotNull();
    }

    @Test
    void invalidData() throws Exception {
        send(post(URL), admin, """
                {"name":"","vatRate":-1,"priceBase":"GROSS","netPrice":10}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").value("Required"))
                .andExpect(jsonPath("$.errors.description").value("Required"))
                .andExpect(jsonPath("$.errors.vatRate").value("Invalid VAT rate"))
                .andExpect(jsonPath("$.errors.price").value("Enter the price"))
                .andExpect(jsonPath("$.errors.salePrice").value("Required"));
    }

    @Test
    void searchSortingAndIsolation() throws Exception {
        create(URL, admin, service("Haircut", "Men", "15"));
        create(URL, admin, service("Colour", "Full colour", "45"));
        create(URL, admin, service("Blow-dry", "With hairdryer", "20").replace("}", ",\"notes\":\"Haircut not included\"}"));

        call(get(URL).param("q", "haircut"), operator).andExpect(jsonPath("$.total").value(2));
        call(get(URL).param("q", "FULL"), operator).andExpect(jsonPath("$.items[0].name").value("Colour"));
        call(get(URL), operator).andExpect(jsonPath("$.items[0].name").value("Blow-dry"));
        call(get(URL).param("sort", "salePrice,asc"), operator)
                .andExpect(jsonPath("$.items[0].name").value("Haircut"));

        call(get(URL), otherTenantAdmin).andExpect(jsonPath("$.total").value(0));
    }
}
