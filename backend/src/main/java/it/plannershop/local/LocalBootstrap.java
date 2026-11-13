package it.plannershop.local;

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
import it.plannershop.tenant.Tenant;
import it.plannershop.tenant.TenantRepository;
import it.plannershop.user.AppUser;
import it.plannershop.user.AppUserRepository;
import it.plannershop.user.Membership;
import it.plannershop.user.MembershipRepository;
import it.plannershop.user.Role;

/**
 * LOCAL mode: on first start-up (no users in the database) creates the single
 * tenant and its ADMIN, instead of the SUPERADMIN.
 *
 * <p>Business name, email and password come from ACTIVITY_NAME, ADMIN_EMAIL,
 * ADMIN_NAME, ADMIN_PASSWORD. The password is always temporary: whoever installs
 * knows it, so the customer changes it at the first sign-in.
 */
@Component
@ConditionalOnProperty(name = "app.mode", havingValue = "local")
class LocalBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LocalBootstrap.class);

    private final AppUserRepository users;
    private final TenantRepository tenants;
    private final MembershipRepository memberships;
    private final PasswordEncoder passwordEncoder;
    private final BootstrapProperties props;

    LocalBootstrap(AppUserRepository users, TenantRepository tenants, MembershipRepository memberships,
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
                log.warn("LOCAL mode, but the database contains more than one tenant: only one is expected");
            }
            return;
        }
        boolean generated = props.adminPassword() == null || props.adminPassword().isBlank();
        String pwd = generated ? RandomPassword.generate() : props.adminPassword();

        Tenant tenant = tenants.save(new Tenant(props.tenantName().trim()));
        AppUser admin = new AppUser(props.adminEmail(), props.adminName().trim(), passwordEncoder.encode(pwd));
        admin.changePassword(admin.getPasswordHash(), true);
        admin = users.save(admin);
        memberships.save(new Membership(admin, tenant, Role.ADMIN));

        if (generated) {
            log.warn("""

                    ==============================================================
                     Local installation: created the business "{}"
                       administrator: {}
                       password:      {}
                     The password is temporary: change it at first sign-in.
                    ==============================================================""",
                    tenant.getName(), admin.getEmail(), pwd);
        } else {
            log.info("Local installation: created the business \"{}\" with administrator {}",
                    tenant.getName(), admin.getEmail());
        }
    }
}
