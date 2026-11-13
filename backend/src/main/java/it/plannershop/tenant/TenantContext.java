package it.plannershop.tenant;

import java.util.function.Supplier;

/**
 * Runs code "as" a given tenant outside an authenticated HTTP request
 * (data import, scheduled jobs, tests).
 *
 * <p>In normal requests the tenant comes from the token and this class is not needed.
 */
public final class TenantContext {

    private static final ThreadLocal<Long> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    static Long current() {
        return CURRENT.get();
    }

    public static <T> T callAs(Long tenantId, Supplier<T> action) {
        Long previous = CURRENT.get();
        CURRENT.set(tenantId);
        try {
            return action.get();
        } finally {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }

    public static void runAs(Long tenantId, Runnable action) {
        callAs(tenantId, () -> {
            action.run();
            return null;
        });
    }
}
