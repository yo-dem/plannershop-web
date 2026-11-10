package it.plannershop.platform;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Oggetti delle API di piattaforma (solo SUPERADMIN). */
public final class PlatformDtos {

    private PlatformDtos() {
    }

    public record TenantDto(Long id, String nome, boolean attivo, Instant createdAt, List<String> amministratori) {
    }

    public record NuovoTenantRequest(
            @NotBlank(message = "Inserisci il nome dell'attività") String nome,
            @NotBlank(message = "Inserisci il nome dell'amministratore") String adminNome,
            @NotBlank(message = "Inserisci l'email dell'amministratore")
            @Email(message = "Email non valida") String adminEmail,
            /**
             * Password provvisoria (l'amministratore la cambierà al primo accesso).
             * Ignorata se l'email appartiene a un utente già esistente.
             */
            @Size(min = 8, max = 100, message = "La password deve avere almeno 8 caratteri") String adminPassword) {
    }

    public record ModificaTenantRequest(
            @NotBlank(message = "Inserisci il nome dell'attività") String nome,
            boolean attivo) {
    }
}
