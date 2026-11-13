package it.plannershop;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import it.plannershop.info.InfoController;

class PlannershopApplicationTests extends AbstractIntegrationTest {

    @Autowired
    InfoController infoController;

    @Test
    void infoReadsTheDatabaseVersion() {
        assertThat(infoController.info().database()).startsWith("PostgreSQL 18");
    }
}
