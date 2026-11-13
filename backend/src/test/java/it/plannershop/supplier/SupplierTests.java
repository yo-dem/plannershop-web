package it.plannershop.supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import it.plannershop.AbstractTenantTest;

class SupplierTests extends AbstractTenantTest {

    private static final String URL = "/api/suppliers";

    private static String supplier(String name) {
        return """
                {"name":"%s","address":"Via Roma 1","phone":"095 123456"}""".formatted(name);
    }

    @Test
    void createWithNormalizedIbanAndUpdate() throws Exception {
        long id = id(send(post(URL), operator, """
                {"name":" Maxico srl ","address":"Via Postiglione 46","phone":"011 3473989",
                 "email":"Accounts@Maxico.com","iban":" it60 x054 2811 1010 0000 0123 456","notes":""}""")
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.name").value("Maxico srl"))
                .andExpect(jsonPath("$.email").value("accounts@maxico.com"))
                .andExpect(jsonPath("$.iban").value("IT60X0542811101000000123456"))
                .andExpect(jsonPath("$.notes").doesNotExist())
                .andReturn().getResponse().getContentAsString());

        // IBAN and email are optional: they can be removed.
        send(put(URL + "/" + id), operator, supplier("Maxico srl").replace("}", ",\"iban\":\"\",\"version\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.iban").doesNotExist())
                .andExpect(jsonPath("$.version").value(1));
        send(put(URL + "/" + id), admin, supplier("X").replace("}", ",\"version\":0}"))
                .andExpect(status().isConflict());
    }

    @Test
    void invalidData() throws Exception {
        send(post(URL), admin, """
                {"name":" ","email":"not-valid","iban":"IT61X0542811101000000123456"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").value("Required"))
                .andExpect(jsonPath("$.errors.address").value("Required"))
                .andExpect(jsonPath("$.errors.phone").value("Required"))
                .andExpect(jsonPath("$.errors.email").value("Invalid email"))
                .andExpect(jsonPath("$.errors.iban").value("Invalid IBAN"));
    }

    @Test
    void searchAndSorting() throws Exception {
        create(URL, admin, supplier("Thecno fire"));
        create(URL, admin, supplier("aestetic project").replace("}", ",\"email\":\"info@aesteticproject.it\"}"));
        create(URL, admin, supplier("Massimo Rizzi").replace("}", ",\"iban\":\"DE89370400440532013000\"}"));

        call(get(URL), operator)
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.items[0].name").value("aestetic project"));
        call(get(URL).param("q", "AESTETICPROJECT.IT"), operator).andExpect(jsonPath("$.total").value(1));
        call(get(URL).param("q", "de8937"), operator).andExpect(jsonPath("$.items[0].name").value("Massimo Rizzi"));
        call(get(URL).param("q", "DE89 3704 0044"), operator).andExpect(jsonPath("$.total").value(1));
        call(get(URL).param("q", "roma"), operator).andExpect(jsonPath("$.total").value(3));
        call(get(URL).param("sort", "name,desc"), operator)
                .andExpect(jsonPath("$.items[0].name").value("Thecno fire"));
    }

    @Test
    void softDeleteAndIsolation() throws Exception {
        long id = id(create(URL, admin, supplier("Supplier to delete")));

        call(get(URL + "/" + id), otherTenantAdmin).andExpect(status().isNotFound());
        call(delete(URL + "/" + id), otherTenantAdmin).andExpect(status().isNotFound());
        call(get(URL), otherTenantAdmin).andExpect(jsonPath("$.total").value(0));

        call(delete(URL + "/" + id), operator).andExpect(status().isNoContent());
        call(get(URL + "/" + id), admin).andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("select deleted_by from supplier where id = ?", Long.class, id)).isNotNull();
    }
}
