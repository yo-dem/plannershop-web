package it.plannershop.auth;

import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Oggetti scambiati dalle API di autenticazione (JSON). */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank(message = "Inserisci l'email") @Email(message = "Email non valida") String email,
            @NotBlank(message = "Inserisci la password") String password,
            /** Facoltativo: attività scelta, se l'utente ne ha più di una. */
            Long tenantId) {
    }

    /**
     * Esito del login. Normalmente contiene il token e l'utente; se l'utente
     * appartiene a più attività e non ne ha scelta una, contiene solo
     * {@code sceltaTenant} e il client deve ripetere il login con {@code tenantId}.
     */
    public record LoginResponse(
            String accessToken,
            Long scadeTraSecondi,
            UtenteCorrente utente,
            List<TenantOpzione> sceltaTenant) {

        static LoginResponse di(Sessione s) {
            return new LoginResponse(s.accessToken(), s.scadeTraSecondi(), s.utente(), null);
        }

        static LoginResponse scelta(List<TenantOpzione> opzioni) {
            return new LoginResponse(null, null, null, opzioni);
        }
    }

    public record TenantOpzione(Long id, String nome) {
    }

    /** Dati dell'utente collegato, come li vede l'interfaccia. */
    public record UtenteCorrente(
            Long id,
            String email,
            String nome,
            /** ADMIN | OPERATORE | SUPERADMIN */
            String ruolo,
            boolean superadmin,
            TenantOpzione tenant,
            boolean deveCambiarePassword) {
    }

    public record CambioPasswordRequest(
            @NotBlank(message = "Inserisci la password attuale") String passwordAttuale,
            @NotBlank(message = "Inserisci la nuova password")
            @Size(min = 8, max = 100, message = "La password deve avere almeno 8 caratteri") String nuovaPassword) {
    }

    /** Uso interno: token emessi da login/refresh. */
    record Sessione(String accessToken, long scadeTraSecondi, String refreshToken, UtenteCorrente utente) {
    }
}
