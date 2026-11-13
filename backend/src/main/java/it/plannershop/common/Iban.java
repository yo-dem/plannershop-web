package it.plannershop.common;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

/**
 * Valid IBAN: country letters, check digits, 11 to 30 alphanumeric characters
 * and Mod-97 check (SPECIFICA §7.4). Normalize the value first with
 * {@link Ibans#normalize(String)}; null is valid.
 */
@Documented
@Constraint(validatedBy = Iban.Validator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT })
@Retention(RetentionPolicy.RUNTIME)
public @interface Iban {

    String message() default "{validation.iban}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<Iban, String> {
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return value == null || Ibans.isValid(value);
        }
    }
}
