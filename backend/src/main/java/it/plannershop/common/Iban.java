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
 * IBAN valido: due lettere del paese, due cifre di controllo, da 11 a 30
 * caratteri alfanumerici e controllo Mod-97 uguale a 1 (SPECIFICA §7.4).
 * Il valore va prima normalizzato con {@link CodiceIban#normalizza(String)}; null è valido.
 */
@Documented
@Constraint(validatedBy = Iban.Validatore.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT })
@Retention(RetentionPolicy.RUNTIME)
public @interface Iban {

    String message() default "IBAN non valido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validatore implements ConstraintValidator<Iban, String> {
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return value == null || CodiceIban.valido(value);
        }
    }
}
