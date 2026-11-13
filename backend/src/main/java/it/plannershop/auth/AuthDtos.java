package it.plannershop.auth;

import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Objects exchanged by the authentication APIs (JSON). */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank(message = "{validation.required}") @Email(message = "{validation.email}") String email,
            @NotBlank(message = "{validation.required}") String password,
            /** Optional: chosen tenant, if the user belongs to more than one. */
            Long tenantId) {
    }

    /**
     * Result of the sign-in. Normally contains the token and the user; if the user
     * belongs to several tenants and has not chosen one, it contains only
     * {@code tenantChoices} and the client must repeat the sign-in with {@code tenantId}.
     */
    public record LoginResponse(
            String accessToken,
            Long expiresInSeconds,
            CurrentUserDto user,
            List<TenantOption> tenantChoices) {

        static LoginResponse of(Session s) {
            return new LoginResponse(s.accessToken(), s.expiresInSeconds(), s.user(), null);
        }

        static LoginResponse choice(List<TenantOption> options) {
            return new LoginResponse(null, null, null, options);
        }
    }

    public record TenantOption(Long id, String name) {
    }

    /** Data of the signed-in user, as the UI sees it. */
    public record CurrentUserDto(
            Long id,
            String email,
            String name,
            /** ADMIN | OPERATOR | SUPERADMIN */
            String role,
            boolean superadmin,
            TenantOption tenant,
            boolean mustChangePassword) {
    }

    public record ChangePasswordRequest(
            @NotBlank(message = "{validation.required}") String currentPassword,
            @NotBlank(message = "{validation.required}")
            @Size(min = 8, max = 100, message = "{validation.passwordLength}") String newPassword) {
    }

    /** Internal use: tokens issued by login/refresh. */
    record Session(String accessToken, long expiresInSeconds, String refreshToken, CurrentUserDto user) {
    }
}
