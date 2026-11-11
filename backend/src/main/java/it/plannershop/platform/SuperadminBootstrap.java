package it.plannershop.platform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import it.plannershop.common.BootstrapProperties;
import it.plannershop.common.PasswordCasuale;
import it.plannershop.user.AppUser;
import it.plannershop.user.AppUserRepository;

/**
 * Modalità CLOUD: al primo avvio (nessun utente nel database) crea il SUPERADMIN.
 *
 * <p>Email e password si leggono da SUPERADMIN_EMAIL / SUPERADMIN_PASSWORD.
 * Se la password non è impostata ne genera una casuale e la scrive UNA volta
 * nel log: va cambiata al primo accesso.
 */
@Component
@ConditionalOnProperty(name = "app.modalita", havingValue = "cloud", matchIfMissing = true)
class SuperadminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SuperadminBootstrap.class);

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final BootstrapProperties props;

    SuperadminBootstrap(AppUserRepository users, PasswordEncoder passwordEncoder, BootstrapProperties props) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.props = props;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.count() > 0) {
            return;
        }
        String email = props.superadminEmail();
        boolean generata = props.superadminPassword() == null || props.superadminPassword().isBlank();
        String pwd = generata ? PasswordCasuale.genera() : props.superadminPassword();
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
}
