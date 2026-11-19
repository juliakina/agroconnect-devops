package br.com.fiap.agroconnect_api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.math.BigDecimal;

public class DecimalPrecisionValidator implements ConstraintValidator<DecimalPrecision, BigDecimal> {

	@Override
	public boolean isValid(BigDecimal value, ConstraintValidatorContext context) {
		return value == null || value.stripTrailingZeros().precision() <= 38;
	}
}
