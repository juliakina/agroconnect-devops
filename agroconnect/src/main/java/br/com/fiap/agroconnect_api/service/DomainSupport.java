package br.com.fiap.agroconnect_api.service;

import br.com.fiap.agroconnect_api.model.Device;
import br.com.fiap.agroconnect_api.model.Field;
import br.com.fiap.agroconnect_api.model.Property;
import br.com.fiap.agroconnect_api.model.Sensor;
import br.com.fiap.agroconnect_api.model.User;
import br.com.fiap.agroconnect_api.repository.DeviceRepository;
import br.com.fiap.agroconnect_api.repository.FieldRepository;
import br.com.fiap.agroconnect_api.repository.PropertyRepository;
import br.com.fiap.agroconnect_api.repository.SensorRepository;
import br.com.fiap.agroconnect_api.repository.UserRepository;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class DomainSupport {

	@Autowired
	private Validator validator;
	@Autowired
	private UserRepository users;
	@Autowired
	private PropertyRepository properties;
	@Autowired
	private FieldRepository fields;
	@Autowired
	private DeviceRepository devices;
	@Autowired
	private SensorRepository sensors;

	public <T> T validate(T input) {
		var violations = validator.validate(input);
		if (!violations.isEmpty()) throw new ConstraintViolationException(violations);
		return input;
	}

	public ApiException missing(String resource) {
		return new ApiException(404, "NOT_FOUND", resource + " não encontrado(a).");
	}

	public ApiException invalid(String field, String message) {
		return new ApiException(400, "VALIDATION_ERROR", "Os dados enviados são inválidos.", Map.of(field, message));
	}

	public User user(UUID id) {
		return users.findById(id).orElseThrow(() -> new ApiException(401, "UNAUTHORIZED", "Usuário não encontrado."));
	}

	public Property property(UUID userId, UUID id) {
		return properties.findByIdAndUserId(id, userId).orElseThrow(() -> missing("Propriedade"));
	}

	public Field field(UUID userId, UUID id) {
		return fields.findByIdAndPropertyUserId(id, userId).orElseThrow(() -> missing("Talhão"));
	}

	public Device device(UUID userId, UUID id) {
		return devices.findByIdAndFieldPropertyUserId(id, userId).orElseThrow(() -> missing("Dispositivo"));
	}

	public Sensor sensor(UUID userId, UUID id) {
		return sensors.findByIdAndDeviceFieldPropertyUserId(id, userId).orElseThrow(() -> missing("Sensor"));
	}

	public Pageable page(int page, int size, String... properties) {
		return page(page, size, Sort.by(properties));
	}

	public Pageable page(int page, int size, Sort sort) {
		return new LimitedPage(Math.max(1, page) - 1, Math.max(1, Math.min(100, size)), sort);
	}

	private static class LimitedPage extends PageRequest {
		LimitedPage(int page, int size, Sort sort) { super(page, size, sort); }
		@Override
		public long getOffset() {
			return Math.min(Integer.MAX_VALUE, super.getOffset());
		}
	}
}
