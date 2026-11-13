package it.plannershop.customer;

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

class CustomerTests extends AbstractTenantTest {

    private static final String URL = "/api/customers";

    private static String customer(String firstName, String lastName) {
        return "{\"firstName\":\"%s\",\"lastName\":\"%s\"}".formatted(firstName, lastName);
    }

    @Test
    void createReadAndUpdate() throws Exception {
        String created = send(post(URL), operator, """
                {"firstName":"  Mario ","lastName":"Rossi","birthDay":29,"birthMonth":2,
                 "phone":" 333 1234567 ","email":" Mario.Rossi@Example.COM","address":"","notes":"Long-time customer"}""")
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.firstName").value("Mario"))
                .andExpect(jsonPath("$.phone").value("333 1234567"))
                .andExpect(jsonPath("$.email").value("mario.rossi@example.com"))
                .andExpect(jsonPath("$.address").doesNotExist())
                .andExpect(jsonPath("$.version").value(0))
                .andReturn().getResponse().getContentAsString();
        long id = id(created);

        call(get(URL + "/" + id), admin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.birthDay").value(29))
                .andExpect(jsonPath("$.birthMonth").value(2));

        send(put(URL + "/" + id), operator, """
                {"firstName":"Mario","lastName":"Rossi Bianchi","version":0}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName").value("Rossi Bianchi"))
                .andExpect(jsonPath("$.birthDay").doesNotExist())
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    void invalidDataReturnsFieldErrors() throws Exception {
        send(post(URL), admin, """
                {"firstName":" ","email":"not-an-email","birthDay":31,"birthMonth":2}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.firstName").value("Required"))
                .andExpect(jsonPath("$.errors.lastName").value("Required"))
                .andExpect(jsonPath("$.errors.email").value("Invalid email"))
                .andExpect(jsonPath("$.errors.birthday").exists());

        send(post(URL), admin, """
                {"firstName":"A","lastName":"B","birthDay":5}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.birthday").exists());

        // Parameters of the messages are filled in, in the requested language.
        call(post(URL).header("Accept-Language", "it").contentType("application/json")
                .content("{\"firstName\":\"%s\",\"lastName\":\"B\"}".formatted("x".repeat(101))), admin)
                .andExpect(jsonPath("$.errors.firstName").value("Massimo 100 caratteri"));
    }

    @Test
    void searchPaginationAndSorting() throws Exception {
        create(URL, admin, customer("Mario", "Rossi"));
        create(URL, admin, customer("Anna", "Verdi"));
        create(URL, admin, customer("Luca", "Bianchi"));
        create(URL, admin, """
                {"firstName":"Giulia","lastName":"Neri","phone":"347 9876543","email":"giulia@example.com"}""");
        create(URL, admin, customer("Paolo", "Rossini"));

        // First and last name in both orders, case-insensitive.
        call(get(URL).param("q", "MARIO rossi"), admin)
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].lastName").value("Rossi"));
        call(get(URL).param("q", "rossi mario"), admin).andExpect(jsonPath("$.total").value(1));
        call(get(URL).param("q", "ross"), admin).andExpect(jsonPath("$.total").value(2));
        // Phone and email.
        call(get(URL).param("q", "9876"), admin).andExpect(jsonPath("$.items[0].firstName").value("Giulia"));
        call(get(URL).param("q", "giulia@"), admin).andExpect(jsonPath("$.total").value(1));
        // LIKE wildcards are searched as plain text.
        call(get(URL).param("q", "%"), admin).andExpect(jsonPath("$.total").value(0));

        // Default: by last name.
        call(get(URL).param("size", "2"), admin)
                .andExpect(jsonPath("$.total").value(5))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].lastName").value("Bianchi"));
        call(get(URL).param("page", "2").param("size", "2"), admin)
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].lastName").value("Verdi"));
        call(get(URL).param("sort", "firstName,desc"), admin)
                .andExpect(jsonPath("$.items[0].firstName").value("Paolo"));
        // A sort key that is not allowed is ignored, without errors.
        call(get(URL).param("sort", "passwordHash,asc"), admin).andExpect(status().isOk());
    }

    @Test
    void onlyTheAdminDeletesAndDeletionIsSoft() throws Exception {
        long id = id(create(URL, operator, customer("Elena", "Gialli")));

        call(delete(URL + "/" + id), operator).andExpect(status().isForbidden());
        call(delete(URL + "/" + id), admin).andExpect(status().isNoContent());

        call(get(URL + "/" + id), admin).andExpect(status().isNotFound());
        call(get(URL), admin).andExpect(jsonPath("$.items[*].id", not(hasItem((int) id))));

        Map<String, Object> row = jdbc.queryForMap("select deleted_at, deleted_by from customer where id = ?", id);
        assertThat(row.get("deleted_at")).isNotNull();
        assertThat(row.get("deleted_by")).isNotNull();
    }

    @Test
    void anotherTenantNeitherSeesNorChangesTheCustomers() throws Exception {
        long id = id(create(URL, admin, customer("Sara", "Blu")));

        call(get(URL + "/" + id), otherTenantAdmin).andExpect(status().isNotFound());
        send(put(URL + "/" + id), otherTenantAdmin, """
                {"firstName":"X","lastName":"Y","version":0}""").andExpect(status().isNotFound());
        call(delete(URL + "/" + id), otherTenantAdmin).andExpect(status().isNotFound());
        call(get(URL), otherTenantAdmin).andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void anUpdateBasedOnStaleDataIsRejected() throws Exception {
        long id = id(create(URL, admin, customer("Marco", "Viola")));
        send(put(URL + "/" + id), admin, "{\"firstName\":\"Marco\",\"lastName\":\"Viola\",\"phone\":\"123\",\"version\":0}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1));

        send(put(URL + "/" + id), operator, "{\"firstName\":\"Marco\",\"lastName\":\"Rosa\",\"version\":0}")
                .andExpect(status().isConflict());
        send(put(URL + "/" + id), operator, "{\"firstName\":\"Marco\",\"lastName\":\"Rosa\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void birthdaysOfTheMonthSortedByDay() throws Exception {
        create(URL, admin, "{\"firstName\":\"C\",\"lastName\":\"Three\",\"birthDay\":20,\"birthMonth\":3}");
        create(URL, admin, "{\"firstName\":\"A\",\"lastName\":\"One\",\"birthDay\":2,\"birthMonth\":3}");
        create(URL, admin, "{\"firstName\":\"B\",\"lastName\":\"Two\",\"birthDay\":2,\"birthMonth\":4}");
        create(URL, admin, customer("No", "Birthday"));

        call(get(URL + "/birthdays").param("month", "3"), operator)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].lastName").value("One"))
                .andExpect(jsonPath("$[1].lastName").value("Three"));
        call(get(URL + "/birthdays").param("month", "13"), admin).andExpect(status().isBadRequest());

        // Without a month: the current month in the tenant's time zone.
        int currentMonth = LocalDate.now(ZoneId.of("Europe/Rome")).getMonthValue();
        create(URL, admin, "{\"firstName\":\"D\",\"lastName\":\"Today\",\"birthDay\":1,\"birthMonth\":%d}".formatted(currentMonth));
        call(get(URL + "/birthdays"), admin)
                .andExpect(jsonPath("$[*].lastName", hasItem("Today")));
    }

    @Test
    void theSuperadminWithoutATenantCannotAccessCustomers() throws Exception {
        call(get(URL), token(SUPERADMIN_EMAIL, SUPERADMIN_PASSWORD)).andExpect(status().isForbidden());
        mvc.perform(get(URL)).andExpect(status().isUnauthorized());
    }
}
