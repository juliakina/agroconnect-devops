package br.com.fiap.agroconnect_api.service;

import br.com.fiap.agroconnect_api.model.IdempotencyRecord;
import br.com.fiap.agroconnect_api.repository.IdempotencyRecordRepository;
import br.com.fiap.agroconnect_api.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.util.Collections;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Service
public class IdempotencyService {

	@Autowired
	private IdempotencyRecordRepository records;
	@Autowired
	private UserRepository users;
	@Autowired
	private JsonMapper mapper;
	@Autowired
	private Clock clock;

	@Transactional
	public <T> ResponseEntity<T> execute(UUID userId, HttpServletRequest request, Object input,
			Class<T> responseType, Supplier<ResponseEntity<T>> action) {
		var headers = Collections.list(request.getHeaders("Idempotency-Key"));
		if (headers.size() > 1 || (!headers.isEmpty() && (headers.get(0).isBlank() || headers.get(0).length() > 128)))
			throw new ApiException(400, "INVALID_IDEMPOTENCY_KEY", "Informe uma chave de idempotência de até 128 caracteres.");
		String path = request.getRequestURI().replaceAll("/+$", "").toLowerCase(Locale.ROOT);
		String hash = fingerprint(path + "\n" + mapper.writeValueAsString(input));
		boolean automatic = headers.isEmpty();
		String key = automatic ? "auto:" + hash : "client:" + headers.get(0);
		var user = users.lockById(userId).orElseThrow(() -> new ApiException(401, "UNAUTHORIZED", "Usuário não encontrado."));
		var record = records.findByUserIdAndKey(userId, key).orElse(null);
		var now = clock.instant();
		if (record != null && (!automatic || Duration.between(record.getCreatedAt(), now).compareTo(Duration.ofMinutes(5)) < 0)) {
			if (!hash.equals(record.getRequestHash()))
				throw new ApiException(409, "IDEMPOTENCY_CONFLICT", "A chave já foi utilizada com outro conteúdo.");
			return ResponseEntity.status(record.getStatusCode()).location(URI.create(record.getLocation()))
					.body(mapper.readValue(record.getResponseJson(), responseType));
		}
		ResponseEntity<T> response = action.get();
		if (response.getStatusCode().value() != 201) return response;
		if (record == null) {
			record = new IdempotencyRecord();
			record.setUser(user);
			record.setKey(key);
		}
		record.setRequestHash(hash);
		record.setStatusCode(201);
		record.setLocation(response.getHeaders().getLocation().toString());
		record.setResponseJson(mapper.writeValueAsString(response.getBody()));
		record.setCreatedAt(now);
		records.saveAndFlush(record);
		return response;
	}

	private String fingerprint(String value) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException error) {
			throw new IllegalStateException(error);
		}
	}
}
