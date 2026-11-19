package br.com.fiap.agroconnect_api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DecimalPrecisionValidator.class)
public @interface DecimalPrecision {

	String message() default "Informe no máximo 38 algarismos significativos.";
	Class<?>[] groups() default {};
	Class<? extends Payload>[] payload() default {};
}
