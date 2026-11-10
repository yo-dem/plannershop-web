package it.plannershop.tenant;

import java.util.function.Supplier;

/**
 * Permette di eseguire codice "come" un certo tenant fuori da una richiesta
 * HTTP autenticata (import dati, job pianificati, test).
 *
 * <p>Nelle richieste normali il tenant arriva dal token e questa classe non serve.
 */
public final class TenantContext {

    private static final ThreadLocal<Long> CORRENTE = new ThreadLocal<>();

    private TenantContext() {
    }

    static Long corrente() {
        return CORRENTE.get();
    }

    public static <T> T callAs(Long tenantId, Supplier<T> azione) {
        Long precedente = CORRENTE.get();
        CORRENTE.set(tenantId);
        try {
            return azione.get();
        } finally {
            if (precedente == null) {
                CORRENTE.remove();
            } else {
                CORRENTE.set(precedente);
            }
        }
    }

    public static void runAs(Long tenantId, Runnable azione) {
        callAs(tenantId, () -> {
            azione.run();
            return null;
        });
    }
}
