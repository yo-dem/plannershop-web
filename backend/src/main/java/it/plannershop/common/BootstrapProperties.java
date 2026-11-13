package it.plannershop.common;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Users created on first start-up ({@code app.bootstrap.*}), only if the
 * database has no users yet. An empty password is generated and logged.
 *
 * @param superadminEmail    CLOUD mode: SUPERADMIN email
 * @param superadminPassword CLOUD mode: SUPERADMIN password
 * @param superadminName     CLOUD mode: display name of the SUPERADMIN
 * @param tenantName         LOCAL mode: name of the business
 * @param adminEmail         LOCAL mode: email of the tenant ADMIN
 * @param adminName          LOCAL mode: display name of the ADMIN
 * @param adminPassword      LOCAL mode: temporary password of the ADMIN
 */
@ConfigurationProperties("app.bootstrap")
public record BootstrapProperties(
        @DefaultValue("admin@plannershop.local") String superadminEmail,
        String superadminPassword,
        @DefaultValue("Platform administrator") String superadminName,
        @DefaultValue("My business") String tenantName,
        @DefaultValue("admin@plannershop.local") String adminEmail,
        @DefaultValue("Administrator") String adminName,
        String adminPassword) {
}
