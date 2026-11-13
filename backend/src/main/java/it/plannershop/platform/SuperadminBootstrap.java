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
import it.plannershop.common.RandomPassword;
import it.plannershop.user.AppUser;
import it.plannershop.user.AppUserRepository;

/**
 * CLOUD mode: on first start-up (no users in the database) creates the SUPERADMIN.
 *
 * <p>Email, password and name come from SUPERADMIN_EMAIL / SUPERADMIN_PASSWORD /
 * SUPERADMIN_NAME. If the
 * password is not set, a random one is generated and logged ONCE: it must be
 * changed at the first sign-in.
 */
@Component
@ConditionalOnProperty(name = "app.mode", havingValue = "cloud", matchIfMissing = true)
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
        boolean generated = props.superadminPassword() == null || props.superadminPassword().isBlank();
        String pwd = generated ? RandomPassword.generate() : props.superadminPassword();
        AppUser admin = new AppUser(email, props.superadminName(), passwordEncoder.encode(pwd));
        admin.setSuperadmin(true);
        admin.changePassword(admin.getPasswordHash(), generated);
        users.save(admin);

        if (generated) {
            log.warn("""

                    ==============================================================
                     SUPERADMIN created
                       email:    {}
                       password: {}
                     The password is temporary: change it at first sign-in.
                    ==============================================================""", email, pwd);
        } else {
            log.info("SUPERADMIN created: {}", email);
        }
    }
}
