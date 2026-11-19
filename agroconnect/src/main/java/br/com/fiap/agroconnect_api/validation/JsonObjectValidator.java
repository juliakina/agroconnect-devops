package br.com.fiap.agroconnect_api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

public class JsonObjectValidator implements ConstraintValidator<JsonObject, String> {

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		if (value == null) return true;
		try {
			return JsonMapper.builder().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
					.build().readTree(value).isObject();
		} catch (RuntimeException exception) {
			return false;
		}
	}
}
