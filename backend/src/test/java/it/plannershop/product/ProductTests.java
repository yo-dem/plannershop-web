package it.plannershop.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

import org.junit.jupiter.api.Test;

import it.plannershop.AbstractTenantTest;

class ProductTests extends AbstractTenantTest {

    private static final String URL = "/api/products";

    /** Product with net price 10, VAT 22%, sale price 15. */
    private static String product(String brand, String description, int quantity) {
        return """
                {"brand":"%s","description":"%s","quantity":%d,"vatRate":22,
                 "priceBase":"NET","netPrice":10,"salePrice":15}""".formatted(brand, description, quantity);
    }

    @Test
    void createWithGrossPriceCalculationThenReadAndUpdate() throws Exception {
        String created = send(post(URL), operator, """
                {"brand":" Davines ","description":"Shampoo 250 ml","quantity":12,"vatRate":22,
                 "priceBase":"NET","netPrice":10,"grossPrice":999,"salePrice":18.50,"notes":""}""")
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.brand").value("Davines"))
                .andExpect(jsonPath("$.netPrice").value(10.00))
                .andExpect(jsonPath("$.grossPrice").value(12.20)) // recalculated: the 999 sent is ignored
                .andExpect(jsonPath("$.salePrice").value(18.50))
                .andExpect(jsonPath("$.notes").doesNotExist())
                .andExpect(jsonPath("$.version").value(0))
                .andReturn().getResponse().getContentAsString();
        long id = id(created);

        // Without a stock date: today, in the tenant's time zone.
        call(get(URL + "/" + id), admin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockDate").value(LocalDate.now(ZoneId.of("Europe/Rome")).toString()))
                .andExpect(jsonPath("$.quantity").value(12));

        // Update starting from the gross price: the net price is calculated.
        send(put(URL + "/" + id), operator, """
                {"stockDate":"2026-03-15","brand":"Davines","description":"Shampoo 250 ml","quantity":0,
                 "vatRate":22,"priceBase":"GROSS","grossPrice":18,"salePrice":20,"version":0}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockDate").value("2026-03-15"))
                .andExpect(jsonPath("$.netPrice").value(14.76))
                .andExpect(jsonPath("$.grossPrice").value(18.00))
                .andExpect(jsonPath("$.quantity").value(0))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    void invalidDataReturnsFieldErrors() throws Exception {
        send(post(URL), admin, """
                {"brand":" ","quantity":-1,"vatRate":101,"priceBase":"NET","grossPrice":10,
                 "salePrice":12.345}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.brand").value("Required"))
                .andExpect(jsonPath("$.errors.description").value("Required"))
                .andExpect(jsonPath("$.errors.quantity").value("Cannot be negative"))
                .andExpect(jsonPath("$.errors.vatRate").value("Invalid VAT rate"))
                .andExpect(jsonPath("$.errors.price").value("Enter the price")) // net price missing
                .andExpect(jsonPath("$.errors.salePrice").value("Invalid amount"));

        send(post(URL), admin, """
                {"brand":"A","description":"B","quantity":1,"vatRate":22,"netPrice":10,"salePrice":-1}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.priceBase").exists())
                .andExpect(jsonPath("$.errors.salePrice").value("Cannot be negative"));
    }

    @Test
    void searchAvailabilityAndSorting() throws Exception {
        create(URL, admin, product("Davines", "Nourishing shampoo", 5));
        create(URL, admin, product("Alfaparf", "Conditioner", 0));
        create(URL, admin, product("Kérastase", "Shampoo 100% natural", 30));
        create(URL, admin, """
                {"brand":"Wella","description":"Hairspray","quantity":2,"vatRate":22,"priceBase":"GROSS",
                 "grossPrice":30,"salePrice":35,"notes":"Only on order"}""");

        // Brand and description in both orders, notes.
        call(get(URL).param("q", "SHAMPOO"), admin).andExpect(jsonPath("$.total").value(2));
        call(get(URL).param("q", "davines nourishing"), admin).andExpect(jsonPath("$.total").value(1));
        call(get(URL).param("q", "conditioner alfaparf"), admin).andExpect(jsonPath("$.total").value(1));
        call(get(URL).param("q", "order"), admin).andExpect(jsonPath("$.items[0].brand").value("Wella"));
        call(get(URL).param("q", "100%"), admin).andExpect(jsonPath("$.total").value(1));

        // Default by brand; available only excludes quantity 0.
        call(get(URL), operator)
                .andExpect(jsonPath("$.total").value(4))
                .andExpect(jsonPath("$.items[0].brand").value("Alfaparf"));
        call(get(URL).param("available", "true"), operator)
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.items[*].brand", not(hasItem("Alfaparf"))));
        call(get(URL).param("sort", "quantity,desc"), admin)
                .andExpect(jsonPath("$.items[0].brand").value("Kérastase"));
        call(get(URL).param("sort", "salePrice,desc"), admin)
                .andExpect(jsonPath("$.items[0].brand").value("Wella"));
        call(get(URL).param("sort", "notes,asc"), admin).andExpect(status().isOk());
    }

    @Test
    void theOperatorDeletesTooAndDeletionIsSoft() throws Exception {
        long id = id(create(URL, admin, product("Davines", "Mask", 3)));

        call(delete(URL + "/" + id), operator).andExpect(status().isNoContent());
        call(get(URL + "/" + id), admin).andExpect(status().isNotFound());
        call(get(URL), admin).andExpect(jsonPath("$.items[*].id", not(hasItem((int) id))));

        Map<String, Object> row = jdbc.queryForMap("select deleted_at, deleted_by from product where id = ?", id);
        assertThat(row.get("deleted_at")).isNotNull();
        assertThat(row.get("deleted_by")).isNotNull();
    }

    @Test
    void anotherTenantDoesNotSeeTheProducts() throws Exception {
        long id = id(create(URL, admin, product("Davines", "Oil", 1)));

        call(get(URL + "/" + id), otherTenantAdmin).andExpect(status().isNotFound());
        send(put(URL + "/" + id), otherTenantAdmin, product("X", "Y", 1).replace("}", ",\"version\":0}"))
                .andExpect(status().isNotFound());
        call(delete(URL + "/" + id), otherTenantAdmin).andExpect(status().isNotFound());
        call(get(URL), otherTenantAdmin).andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void anUpdateBasedOnStaleDataIsRejected() throws Exception {
        long id = id(create(URL, admin, product("Davines", "Serum", 4)));
        String v0 = product("Davines", "Serum", 2).replace("}", ",\"version\":0}");
        send(put(URL + "/" + id), admin, v0).andExpect(status().isOk());
        send(put(URL + "/" + id), operator, v0).andExpect(status().isConflict());
        send(put(URL + "/" + id), operator, product("Davines", "Serum", 2)).andExpect(status().isBadRequest());
    }

    @Test
    void theSuperadminWithoutATenantCannotAccessProducts() throws Exception {
        call(get(URL), token(SUPERADMIN_EMAIL, SUPERADMIN_PASSWORD)).andExpect(status().isForbidden());
        mvc.perform(get(URL)).andExpect(status().isUnauthorized());
    }
}
