package it.plannershop.platform;

import java.security.SecureRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import it.plannershop.user.AppUser;
import it.plannershop.user.AppUserRepository;

/**
 * Al primo avvio (nessun utente nel database) crea il SUPERADMIN.
 *
 * <p>Email e password si leggono da SUPERADMIN_EMAIL / SUPERADMIN_PASSWORD.
 * Se la password non è impostata ne genera una casuale e la scrive UNA volta
 * nel log: va cambiata al primo accesso.
 */
@Component
class SuperadminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SuperadminBootstrap.class);

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    SuperadminBootstrap(AppUserRepository users, PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap.superadmin-email}") String email,
            @Value("${app.bootstrap.superadmin-password:}") String password) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.count() > 0) {
            return;
        }
        boolean generata = password == null || password.isBlank();
        String pwd = generata ? generaPassword() : password;
        AppUser admin = new AppUser(email, "Amministratore piattaforma", passwordEncoder.encode(pwd));
        admin.setSuperadmin(true);
        admin.cambiaPassword(admin.getPasswordHash(), generata);
        users.save(admin);

        if (generata) {
            log.warn("""

                    ==============================================================
                     Creato il SUPERADMIN
                       email:    {}
                       password: {}
                     La password è provvisoria: cambiala al primo accesso.
                    ==============================================================""", email, pwd);
        } else {
            log.info("Creato il SUPERADMIN {}", email);
        }
    }

    private static String generaPassword() {
        String alfabeto = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            sb.append(alfabeto.charAt(random.nextInt(alfabeto.length())));
        }
        return sb.toString();
    }
}
