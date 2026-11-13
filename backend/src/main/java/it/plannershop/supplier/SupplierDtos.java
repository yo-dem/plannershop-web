package it.plannershop.supplier;

import static it.plannershop.common.TextUtils.clean;

import java.time.Instant;

import it.plannershop.common.Iban;
import it.plannershop.common.Ibans;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class SupplierDtos {

    private SupplierDtos() {
    }

    public record SupplierDto(
            Long id,
            String name,
            String address,
            String phone,
            String email,
            String iban,
            String notes,
            long version,
            Instant createdAt,
            Instant updatedAt) {

        static SupplierDto of(Supplier s) {
            return new SupplierDto(s.getId(), s.getName(), s.getAddress(), s.getPhone(), s.getEmail(), s.getIban(),
                    s.getNotes(), s.getVersion(), s.getCreatedAt(), s.getUpdatedAt());
        }
    }

    /**
     * Data of a supplier to create or update.
     *
     * @param iban    accepted with spaces and lower case: normalized before validation
     * @param version required on update: the version that was read
     */
    public record SupplierRequest(
            @NotBlank(message = "{validation.required}") @Size(max = 150, message = "{validation.maxLength}") String name,
            @NotBlank(message = "{validation.required}") @Size(max = 300, message = "{validation.maxLength}") String address,
            @NotBlank(message = "{validation.required}") @Size(max = 50, message = "{validation.maxLength}") String phone,
            @Email(message = "{validation.email}") @Size(max = 254, message = "{validation.maxLength}") String email,
            @Iban String iban,
            @Size(max = 4000, message = "{validation.maxLength}") String notes,
            Long version) {

        public SupplierRequest {
            name = clean(name);
            address = clean(address);
            phone = clean(phone);
            email = clean(email);
            iban = Ibans.normalize(iban);
            notes = clean(notes);
        }
    }
}
