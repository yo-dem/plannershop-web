package it.plannershop.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Se l'applicazione viene avviata con {@code --app.reset-password=<email>},
 * ripristina la password di quell'utente, la scrive a video e termina.
 * Vedi {@link it.plannershop.PlannershopApplication#main} e il README.
 */
@Component
@Order(Integer.MAX_VALUE)
class RipristinoPasswordRunner implements ApplicationRunner {

    static final String OPZIONE = "app.reset-password";

    private static final Logger log = LoggerFactory.getLogger(RipristinoPasswordRunner.class);

    private final RipristinoPassword ripristino;
    private final ConfigurableApplicationContext context;

    RipristinoPasswordRunner(RipristinoPassword ripristino, ConfigurableApplicationContext context) {
        this.ripristino = ripristino;
        this.context = context;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!args.containsOption(OPZIONE)) {
            return;
        }
        String email = args.getOptionValues(OPZIONE).stream().findFirst().orElse("");
        int codice = 0;
        try {
            String pwd = ripristino.ripristina(email);
            log.warn("""

                    ==============================================================
                     Password ripristinata
                       email:    {}
                       password: {}
                     La password è provvisoria: va cambiata al primo accesso.
                    ==============================================================""", email, pwd);
        } catch (RuntimeException e) {
            log.error("Ripristino password non riuscito: {}", e.getMessage());
            codice = 1;
        }
        int esito = codice;
        System.exit(SpringApplication.exit(context, () -> esito));
    }
}
