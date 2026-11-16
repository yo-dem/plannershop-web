package it.plannershop.common;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * General settings ({@code app.*} in application.yml).
 *
 * @param mode     how the application is installed (APP_MODE variable)
 * @param demoData whether the ADMIN of an empty business may load the demo data
 *                 (DEMO_DATA variable)
 */
@ConfigurationProperties("app")
public record AppProperties(@DefaultValue("CLOUD") Mode mode, @DefaultValue("true") boolean demoData) {

    /**
     * <ul>
     * <li>{@code CLOUD}: platform with many tenants, managed by the SUPERADMIN.</li>
     * <li>{@code LOCAL}: installed on the PC of a single business. The SUPERADMIN
     * and the platform APIs do not exist; on first start-up the tenant and its
     * ADMIN are created directly.</li>
     * </ul>
     */
    public enum Mode {
        CLOUD, LOCAL
    }

    public boolean isLocal() {
        return mode == Mode.LOCAL;
    }
}
