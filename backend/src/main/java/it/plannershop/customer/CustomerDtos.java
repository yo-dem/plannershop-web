package it.plannershop.customer;

import static it.plannershop.common.TextUtils.clean;

import java.time.Instant;
import java.time.Month;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class CustomerDtos {

    private CustomerDtos() {
    }

    public record CustomerDto(
            Long id,
            String firstName,
            String lastName,
            Short birthDay,
            Short birthMonth,
            String phone,
            String email,
            String address,
            String notes,
            long version,
            Instant createdAt,
            Instant updatedAt) {

        static CustomerDto of(Customer c) {
            return new CustomerDto(c.getId(), c.getFirstName(), c.getLastName(), c.getBirthDay(), c.getBirthMonth(),
                    c.getPhone(), c.getEmail(), c.getAddress(), c.getNotes(), c.getVersion(), c.getCreatedAt(),
                    c.getUpdatedAt());
        }
    }

    /**
     * Data of a customer to create or update.
     *
     * @param version required on update: the version that was read, so that
     *                changes made meanwhile by others are not overwritten
     */
    public record CustomerRequest(
            @NotBlank(message = "{validation.required}") @Size(max = 100, message = "{validation.maxLength}") String firstName,
            @NotBlank(message = "{validation.required}") @Size(max = 100, message = "{validation.maxLength}") String lastName,
            @Min(value = 1, message = "{validation.day}") @Max(value = 31, message = "{validation.day}") Short birthDay,
            @Min(value = 1, message = "{validation.month}") @Max(value = 12, message = "{validation.month}") Short birthMonth,
            @Size(max = 50, message = "{validation.maxLength}") String phone,
            @Email(message = "{validation.email}") @Size(max = 254, message = "{validation.maxLength}") String email,
            @Size(max = 300, message = "{validation.maxLength}") String address,
            @Size(max = 4000, message = "{validation.maxLength}") String notes,
            Long version) {

        /** Leading and trailing spaces removed before validation; empty text = missing field. */
        public CustomerRequest {
            firstName = clean(firstName);
            lastName = clean(lastName);
            phone = clean(phone);
            email = clean(email);
            address = clean(address);
            notes = clean(notes);
        }

        /** Day and month together, and the day must exist in that month (29 February allowed). */
        @AssertTrue(message = "{validation.birthday}")
        public boolean isBirthday() {
            if (birthDay == null && birthMonth == null) {
                return true;
            }
            if (birthDay == null || birthMonth == null || birthMonth < 1 || birthMonth > 12) {
                return false;
            }
            return Month.of(birthMonth).maxLength() >= birthDay;
        }
    }
}
