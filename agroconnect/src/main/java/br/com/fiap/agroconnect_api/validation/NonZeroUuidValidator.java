package br.com.fiap.agroconnect_api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.UUID;

public class NonZeroUuidValidator implements ConstraintValidator<NonZeroUuid, UUID> {

	@Override
	public boolean isValid(UUID value, ConstraintValidatorContext context) {
		return value == null || !value.equals(new UUID(0, 0));
	}
}
