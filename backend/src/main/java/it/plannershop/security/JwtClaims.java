package it.plannershop.security;

/** Nomi dei claim personalizzati nell'access token. */
public final class JwtClaims {

    /** Id dell'attività corrente (assente per il SUPERADMIN senza attività). */
    public static final String TENANT = "tenant";
    /** Ruoli: ADMIN | OPERATORE, più SUPERADMIN se l'utente lo è. */
    public static final String ROLES = "roles";
    public static final String NOME = "nome";
    public static final String EMAIL = "email";

    private JwtClaims() {
    }
}
