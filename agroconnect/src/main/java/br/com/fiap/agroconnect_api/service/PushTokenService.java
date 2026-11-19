package br.com.fiap.agroconnect_api.service;

import br.com.fiap.agroconnect_api.model.PushToken;
import br.com.fiap.agroconnect_api.record.PushTokenInput;
import br.com.fiap.agroconnect_api.repository.PushTokenRepository;
import br.com.fiap.agroconnect_api.repository.SessionRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PushTokenService {

	@Autowired
	private DomainSupport support;
	@Autowired
	private PushTokenRepository tokens;
	@Autowired
	private SessionRepository sessions;

	@Transactional
	public void register(UUID userId, UUID sessionId, PushTokenInput input) {
		support.validate(input);
		if (sessionId == null) throw new ApiException(401, "UNAUTHORIZED", "Sessão autenticada necessária.");
		var session = sessions.findByIdAndUserId(sessionId, userId)
				.filter(item -> item.getExpiresAt().isAfter(Instant.now()))
				.orElseThrow(() -> new ApiException(401, "UNAUTHORIZED", "Sessão inválida ou expirada."));
		PushToken item = tokens.findByToken(input.token()).orElseGet(PushToken::new);
		item.setUser(support.user(userId));
		item.setSession(session);
		item.setToken(input.token());
		item.setPlatform(input.platform());
		item.setUpdatedAt(Instant.now());
		tokens.saveAndFlush(item);
	}
}
