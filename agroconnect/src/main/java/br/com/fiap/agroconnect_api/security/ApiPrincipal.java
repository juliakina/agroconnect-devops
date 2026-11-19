package br.com.fiap.agroconnect_api.security;

import br.com.fiap.agroconnect_api.model.UserRole;
import java.security.Principal;
import java.util.UUID;

public record ApiPrincipal(UUID userId, UUID sessionId, UserRole role) implements Principal {

	@Override
	public String getName() {
		return userId.toString();
	}
}
