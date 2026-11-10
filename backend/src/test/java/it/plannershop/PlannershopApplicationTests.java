package it.plannershop;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import it.plannershop.info.InfoController;

/**
 * Avvia l'intera applicazione contro un Postgres vero in un container
 * (Testcontainers): richiede Docker Desktop in esecuzione.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class PlannershopApplicationTests {

    @Autowired
    InfoController infoController;

    @Test
    void contextLoads() {
    }

    @Test
    void infoLeggeLaVersioneDelDatabase() {
        var info = infoController.info();
        assertThat(info.database()).startsWith("PostgreSQL 18");
    }
}
