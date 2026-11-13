package it.plannershop.platform;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Objects of the platform APIs (SUPERADMIN only). */
public final class PlatformDtos {

    private PlatformDtos() {
    }

    public record TenantDto(Long id, String name, boolean active, Instant createdAt, List<String> admins) {
    }

    public record NewTenantRequest(
            @NotBlank(message = "{validation.required}") String name,
            @NotBlank(message = "{validation.required}") String adminName,
            @NotBlank(message = "{validation.required}") @Email(message = "{validation.email}") String adminEmail,
            /**
             * Temporary password (the administrator will change it at the first sign-in).
             * Ignored if the email belongs to an existing user.
             */
            @Size(min = 8, max = 100, message = "{validation.passwordLength}") String adminPassword) {
    }

    public record UpdateTenantRequest(
            @NotBlank(message = "{validation.required}") String name,
            boolean active) {
    }
}
