package it.plannershop.settings;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import it.plannershop.AbstractTenantTest;

class SettingsTests extends AbstractTenantTest {

    private static final String URL = "/api/settings";

    @Test
    void defaultsUntilTheFirstSave() throws Exception {
        call(get(URL), operator).andExpect(status().isOk())
                .andExpect(jsonPath("$.outOfStockThreshold").value(0))
                .andExpect(jsonPath("$.lowStockThreshold").value(10))
                .andExpect(jsonPath("$.version").doesNotExist());
    }

    @Test
    void adminChangesTheThresholdsOfItsOwnBusiness() throws Exception {
        send(put(URL), admin, """
                {"outOfStockThreshold":2,"lowStockThreshold":5}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outOfStockThreshold").value(2))
                .andExpect(jsonPath("$.lowStockThreshold").value(5))
                .andExpect(jsonPath("$.version").value(0));
        call(get(URL), operator).andExpect(jsonPath("$.lowStockThreshold").value(5));

        // Every business has its own settings.
        call(get(URL), otherTenantAdmin).andExpect(jsonPath("$.lowStockThreshold").value(10));

        // Later saves need the version read before.
        send(put(URL), admin, """
                {"outOfStockThreshold":1,"lowStockThreshold":20}""").andExpect(status().isBadRequest());
        send(put(URL), admin, """
                {"outOfStockThreshold":1,"lowStockThreshold":20,"version":7}""").andExpect(status().isConflict());
        send(put(URL), admin, """
                {"outOfStockThreshold":1,"lowStockThreshold":20,"version":0}""")
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1));
    }

    @Test
    void validation() throws Exception {
        send(put(URL).header("Accept-Language", "it"), admin, """
                {"outOfStockThreshold":5,"lowStockThreshold":3}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.lowStockThreshold").value("Non può essere inferiore alla soglia di esaurimento"));
        send(put(URL), admin, """
                {"outOfStockThreshold":-1,"lowStockThreshold":null}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.outOfStockThreshold").exists())
                .andExpect(jsonPath("$.errors.lowStockThreshold").exists());
        // Equal thresholds are allowed: no orange.
        send(put(URL), admin, """
                {"outOfStockThreshold":3,"lowStockThreshold":3}""").andExpect(status().isOk());
    }

    @Test
    void onlyTheAdminChangesThem() throws Exception {
        send(put(URL), operator, """
                {"outOfStockThreshold":0,"lowStockThreshold":10}""").andExpect(status().isForbidden());
    }
}
