package it.plannershop.locale;

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
import it.plannershop.tenant.Tenant;
import it.plannershop.tenant.TenantRepository;
import it.plannershop.user.AppUser;
import it.plannershop.user.AppUserRepository;
import it.plannershop.user.Membership;
import it.plannershop.user.MembershipRepository;
import it.plannershop.user.Ruolo;

/**
 * Modalità LOCALE: al primo avvio (nessun utente nel database) crea l'unica
 * attività e il suo ADMIN, al posto del SUPERADMIN.
 *
 * <p>Nome dell'attività, email e password si leggono da ACTIVITY_NAME,
 * ADMIN_EMAIL, ADMIN_NAME, ADMIN_PASSWORD. La password è sempre provvisoria:
 * chi installa la conosce, quindi il cliente la cambia al primo accesso.
 */
@Component
@ConditionalOnProperty(name = "app.modalita", havingValue = "locale")
class LocaleBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LocaleBootstrap.class);

    private final AppUserRepository users;
    private final TenantRepository tenants;
    private final MembershipRepository memberships;
    private final PasswordEncoder passwordEncoder;
    private final BootstrapProperties props;

    LocaleBootstrap(AppUserRepository users, TenantRepository tenants, MembershipRepository memberships,
            PasswordEncoder passwordEncoder, BootstrapProperties props) {
        this.users = users;
        this.tenants = tenants;
        this.memberships = memberships;
        this.passwordEncoder = passwordEncoder;
        this.props = props;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.count() > 0) {
            if (tenants.count() > 1) {
                log.warn("Modalità LOCALE ma il database contiene più attività: è previsto che ce ne sia una sola");
            }
            return;
        }
        boolean generata = props.adminPassword() == null || props.adminPassword().isBlank();
        String pwd = generata ? PasswordCasuale.genera() : props.adminPassword();

        Tenant tenant = tenants.save(new Tenant(props.attivitaNome().trim()));
        AppUser admin = new AppUser(props.adminEmail(), props.adminNome().trim(), passwordEncoder.encode(pwd));
        admin.cambiaPassword(admin.getPasswordHash(), true);
        admin = users.save(admin);
        memberships.save(new Membership(admin, tenant, Ruolo.ADMIN));

        if (generata) {
            log.warn("""

                    ==============================================================
                     Installazione locale: creata l'attività "{}"
                       amministratore: {}
                       password:       {}
                     La password è provvisoria: cambiala al primo accesso.
                    ==============================================================""",
                    tenant.getNome(), admin.getEmail(), pwd);
        } else {
            log.info("Installazione locale: creata l'attività \"{}\" con amministratore {}",
                    tenant.getNome(), admin.getEmail());
        }
    }
}
