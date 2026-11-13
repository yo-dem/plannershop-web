package it.plannershop.security;

/** Names of the custom claims in the access token. */
public final class JwtClaims {

    /** Id of the current tenant (absent for the SUPERADMIN without a tenant). */
    public static final String TENANT = "tenant";
    /** Roles: ADMIN | OPERATOR, plus SUPERADMIN if the user is one. */
    public static final String ROLES = "roles";
    public static final String NAME = "name";
    public static final String EMAIL = "email";

    private JwtClaims() {
    }
}
