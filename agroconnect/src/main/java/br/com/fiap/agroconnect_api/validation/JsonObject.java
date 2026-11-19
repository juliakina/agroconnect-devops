package br.com.fiap.agroconnect_api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = JsonObjectValidator.class)
public @interface JsonObject {
	String message() default "Informe um objeto JSON válido.";
	Class<?>[] groups() default {};
	Class<? extends Payload>[] payload() default {};
}
