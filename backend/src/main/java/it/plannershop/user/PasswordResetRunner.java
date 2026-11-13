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
 * If the application is started with {@code --app.reset-password=<email>}, it
 * resets that user's password, prints it and exits.
 * See {@link it.plannershop.PlannershopApplication#main} and the README.
 */
@Component
@Order(Integer.MAX_VALUE)
class PasswordResetRunner implements ApplicationRunner {

    static final String OPTION = "app.reset-password";

    private static final Logger log = LoggerFactory.getLogger(PasswordResetRunner.class);

    private final PasswordReset passwordReset;
    private final ConfigurableApplicationContext context;

    PasswordResetRunner(PasswordReset passwordReset, ConfigurableApplicationContext context) {
        this.passwordReset = passwordReset;
        this.context = context;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!args.containsOption(OPTION)) {
            return;
        }
        String email = args.getOptionValues(OPTION).stream().findFirst().orElse("");
        int code = 0;
        try {
            String pwd = passwordReset.reset(email);
            log.warn("""

                    ==============================================================
                     Password reset
                       email:    {}
                       password: {}
                     The password is temporary: it must be changed at first sign-in.
                    ==============================================================""", email, pwd);
        } catch (RuntimeException e) {
            log.error("Password reset failed: no user with email {}", email);
            code = 1;
        }
        int exitCode = code;
        System.exit(SpringApplication.exit(context, () -> exitCode));
    }
}
