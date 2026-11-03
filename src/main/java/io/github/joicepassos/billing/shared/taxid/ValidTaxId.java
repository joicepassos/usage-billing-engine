package io.github.joicepassos.billing.shared.taxid;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** A valid CPF or CNPJ (numeric or alphanumeric). Null is valid; combine with {@code @NotBlank} when required. */
@Documented
@Constraint(validatedBy = ValidTaxId.Validator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidTaxId {

    String message() default "must be a valid CPF or CNPJ";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<ValidTaxId, String> {

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return value == null || TaxId.isValid(value);
        }
    }
}
