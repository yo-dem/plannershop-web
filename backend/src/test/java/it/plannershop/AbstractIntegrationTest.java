package it.plannershop;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

/**
 * Base dei test d'integrazione: applicazione completa + Postgres in container.
 * Tutte le classi che la estendono condividono lo stesso contesto Spring
 * (e lo stesso database), quindi i test non devono dipendere da un DB vuoto.
 */
@SpringBootTest(properties = {
        "spring.flyway.locations=classpath:db/migration,classpath:db/testmigration",
        "app.security.jwt-secret=segreto-di-test-lungo-almeno-trentadue-caratteri",
        "app.bootstrap.superadmin-email=" + AbstractIntegrationTest.SUPERADMIN_EMAIL,
        "app.bootstrap.superadmin-password=" + AbstractIntegrationTest.SUPERADMIN_PASSWORD,
        "app.security.login-max-tentativi=5"
})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class AbstractIntegrationTest {

    public static final String SUPERADMIN_EMAIL = "root@test.local";
    public static final String SUPERADMIN_PASSWORD = "Superadmin-123";
}
